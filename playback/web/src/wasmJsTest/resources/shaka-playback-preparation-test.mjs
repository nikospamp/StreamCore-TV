import * as adapter from "./shaka-playback-adapter.mjs";

const probes = new Map();
let nextProbeId = 1;

export function start(scenario) {
    const id = nextProbeId++;
    const probe = { complete: false, failure: "" };
    probes.set(id, probe);
    void runScenario(scenario).then(
        () => { probe.complete = true; },
        (error) => {
            probe.failure = error.message;
            probe.complete = true;
        },
    );
    return id;
}

export function isComplete(id) {
    return probes.get(id)?.complete ?? false;
}

export function failure(id) {
    return probes.get(id)?.failure ?? "Missing preparation probe";
}

export function release(id) {
    probes.delete(id);
}

async function runScenario(scenario) {
    const fixture = createFixture();
    const handle = fixture.handle;
    try {
        adapter.load(handle, 1, "https://media.example/first.mpd", "application/dash+xml", 12);
        if (scenario === "pause-during-unload") {
            adapter.pause(handle);
        }
        fixture.unloads[0].resolve();
        await Promise.resolve();
        check(fixture.loads.length === 1, "The real runLoad continuation must reach Shaka.load");
        check(fixture.loads[0].startSeconds === 12, "Resume position must reach Shaka.load");

        switch (scenario) {
            case "autoplay":
                await completeLoad(fixture, 0);
                check(fixture.playCount === 1, "Visible playback must request autoplay");
                check(adapter.phase(handle) === "ready", "Loaded media must become ready");
                break;
            case "pause-during-unload":
            case "pause-during-load":
                if (scenario === "pause-during-load") {
                    adapter.pause(handle);
                }
                await completeLoad(fixture, 0);
                check(fixture.playCount === 0, "A pending load must retain the pause intent");
                check(adapter.phase(handle) === "ready", "Paused loaded media must become ready");
                adapter.play(handle);
                check(fixture.playCount === 1, "An explicit play must still work after preparation");
                break;
            case "play-during-load":
                adapter.pause(handle);
                adapter.play(handle);
                await completeLoad(fixture, 0);
                check(fixture.playCount === 1, "The most recent explicit play must restore autoplay");
                break;
            case "close-during-load":
                adapter.close(handle);
                adapter.close(handle);
                await completeLoad(fixture, 0);
                check(fixture.playCount === 0, "Closed media must never autoplay");
                check(!handle.loaded, "Late load must not revive a closed handle");
                check(fixture.destroyCount === 1, "Close must destroy exactly once");
                break;
            case "replace-during-load":
            case "replace-rejected-load":
                adapter.load(handle, 2, "https://media.example/second.mpd", "application/dash+xml", 24);
                fixture.unloads[1].resolve();
                await Promise.resolve();
                check(fixture.loads.length === 2, "Replacement must start a second Shaka load");
                adapter.pause(handle);
                await completeLoad(fixture, 1);
                if (scenario === "replace-rejected-load") {
                    fixture.loads[0].reject(new Error("obsolete load"));
                    await Promise.resolve();
                } else {
                    await completeLoad(fixture, 0);
                }
                check(adapter.generation(handle) === 2, "Replacement generation must remain current");
                check(adapter.phase(handle) === "ready", "Old load cannot overwrite replacement phase");
                check(fixture.playCount === 0, "Old load cannot play paused replacement media");
                check(!adapter.hasFailure(handle), "Old failure cannot fail replacement media");
                break;
            case "autoplay-rejected":
                fixture.rejectPlay = true;
                await completeLoad(fixture, 0);
                await Promise.resolve();
                check(fixture.playCount === 1, "Autoplay should be attempted once");
                check(adapter.phase(handle) === "ready", "Autoplay rejection must leave controls ready");
                check(!adapter.hasFailure(handle), "Browser autoplay rejection is not a media failure");
                break;
            case "stale-play-rejected":
                adapter.pause(handle);
                await completeLoad(fixture, 0);
                fixture.pendingPlay = deferred();
                adapter.play(handle);
                adapter.load(handle, 2, "https://media.example/second.mpd", "application/dash+xml", 0);
                fixture.pendingPlay.reject(new Error("obsolete play"));
                await Promise.resolve();
                check(adapter.phase(handle) === "preparing", "Old play rejection must not make replacement ready");
                break;
            default:
                throw new Error(`Unknown scenario: ${scenario}`);
        }
    } finally {
        adapter.close(handle);
        fixture.unloads.forEach((request) => request.resolve());
        fixture.loads.forEach((request) => request.resolve());
    }
}

async function completeLoad(fixture, index) {
    fixture.loads[index].resolve();
    await Promise.resolve();
}

function createFixture() {
    // Invoke the production adapter exports and runLoad with controlled Shaka promises.
    // Media decoding/autoplay policy are separate browser concerns; these spies count requests.
    const video = document.createElement("video");
    const fixture = {
        loads: [],
        unloads: [],
        playCount: 0,
        pauseCount: 0,
        destroyCount: 0,
        rejectPlay: false,
        pendingPlay: null,
    };
    Object.defineProperty(video, "play", { value() {
        fixture.playCount += 1;
        if (fixture.pendingPlay) {
            return fixture.pendingPlay.promise;
        }
        return fixture.rejectPlay ? Promise.reject(new Error("autoplay denied")) : Promise.resolve();
    } });
    Object.defineProperty(video, "pause", { value() { fixture.pauseCount += 1; } });
    Object.defineProperty(video, "load", { value() {} });
    fixture.handle = {
        video,
        player: {
            unload() {
                const request = deferred();
                fixture.unloads.push(request);
                return request.promise;
            },
            load(uri, startSeconds) {
                const request = { ...deferred(), uri, startSeconds };
                fixture.loads.push(request);
                return request.promise;
            },
            getVariantTracks() { return []; },
            getTextTracks() { return []; },
            destroy() {
                fixture.destroyCount += 1;
                return Promise.resolve();
            },
        },
        generation: 0,
        closed: false,
        destroyStarted: false,
        loaded: false,
        playWhenReady: true,
        tracksRevision: 0,
        filmstripRequests: new Map(),
        videoListeners: [],
        shakaListeners: [],
        cleanup: { destroyStartCount: 0, destroySettled: false, destroyRejectionCaught: false },
    };
    return fixture;
}

function deferred() {
    let resolve;
    let reject;
    const promise = new Promise((onResolve, onReject) => {
        resolve = onResolve;
        reject = onReject;
    });
    return { promise, resolve, reject };
}

function check(condition, message) {
    if (!condition) {
        throw new Error(message);
    }
}
