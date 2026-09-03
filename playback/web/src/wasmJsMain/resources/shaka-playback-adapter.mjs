import shakaPackage from "shaka-player/dist/shaka-player.compiled.js";

const shaka = shakaPackage?.default ?? shakaPackage;

if (!shaka?.Player) {
    throw new Error("Shaka Player 5.2.3 is unavailable through the module adapter.");
}

const VIDEO_EVENTS = [
    "canplay",
    "durationchange",
    "ended",
    "loadedmetadata",
    "pause",
    "playing",
    "progress",
    "ratechange",
    "resize",
    "seeked",
    "seeking",
    "stalled",
    "timeupdate",
    "waiting",
];

export function create(videoElement) {
    let player = null;
    try {
        player = new shaka.Player(videoElement);
    } catch (_) {
        // The Kotlin boundary receives only the fixed failure snapshot below.
    }
    const handle = {
        video: videoElement,
        player,
        generation: 0,
        closed: false,
        destroyStarted: false,
        loaded: false,
        phase: "idle",
        failure: false,
        recoverable: true,
        resizeMode: "fit",
        selectedVideoTrackId: "",
        selectedAudioTrackId: "",
        selectedTextTrackId: "",
        videoTrackOverrideId: "",
        audioTrackOverrideId: "",
        videoTracks: [],
        audioTracks: [],
        textTracks: [],
        tracksRevision: 0,
        filmstripRequests: new Map(),
        cleanup: {
            destroyStartCount: 0,
            destroySettled: false,
            destroyRejectionCaught: false,
        },
        videoListeners: [],
        shakaListeners: [],
    };

    safely(handle, () => configureVideoElement(videoElement));
    if (player) {
        safely(handle, () => {
            installVideoListeners(handle);
            installShakaListeners(handle);
        });
    } else {
        setFailure(handle, false);
    }
    return handle;
}

export function load(handle, generation, uri, mimeType, startSeconds) {
    if (handle.closed) {
        return;
    }
    handle.generation = generation;
    handle.loaded = false;
    handle.phase = "preparing";
    handle.failure = false;
    handle.recoverable = true;
    handle.selectedVideoTrackId = "";
    handle.selectedAudioTrackId = "";
    handle.selectedTextTrackId = "";
    handle.videoTrackOverrideId = "";
    handle.audioTrackOverrideId = "";
    cancelAllFilmstripRequests(handle);
    clearTracks(handle);

    if (!handle.player) {
        setFailure(handle, false);
        return;
    }

    void runLoad(handle, generation, uri, mimeType, Math.max(0, startSeconds));
}

export function reset(handle, generation) {
    if (handle.closed) {
        return;
    }
    handle.generation = generation;
    handle.loaded = false;
    handle.phase = "idle";
    handle.failure = false;
    handle.recoverable = true;
    handle.selectedVideoTrackId = "";
    handle.selectedAudioTrackId = "";
    handle.selectedTextTrackId = "";
    handle.videoTrackOverrideId = "";
    handle.audioTrackOverrideId = "";
    cancelAllFilmstripRequests(handle);
    clearTracks(handle);
    try {
        handle.video.pause();
    } catch (_) {
        // Continue invalidating the old generation.
    }
    if (handle.player) {
        try {
            const unloadResult = handle.player.unload();
            void Promise.resolve(unloadResult).catch(() => {});
        } catch (_) {
            // Reset is a terminal operation for the invalidated generation.
        }
    }
}

async function runLoad(handle, generation, uri, mimeType, startSeconds) {
    try {
        await handle.player.unload();
        if (!isCurrent(handle, generation)) {
            return;
        }
        await handle.player.load(uri, startSeconds, mimeType || undefined);
        if (!isCurrent(handle, generation)) {
            return;
        }
        handle.loaded = true;
        handle.phase = handle.video.ended ? "ended" : "ready";
        refreshTracks(handle);
        try {
            await Promise.resolve(handle.video.play());
        } catch (_) {
            if (isCurrent(handle, generation)) {
                handle.phase = "ready";
            }
        }
    } catch (_) {
        if (isCurrent(handle, generation)) {
            setFailure(handle, true);
        }
    }
}

export function play(handle) {
    if (handle.closed || !handle.loaded) {
        return;
    }
    try {
        if (handle.video.ended || finiteDuration(handle.video.duration) <= handle.video.currentTime) {
            handle.video.currentTime = 0;
        }
        const playResult = handle.video.play();
        void Promise.resolve(playResult).catch(() => {
            if (!handle.closed) {
                handle.phase = "ready";
            }
        });
    } catch (_) {
        handle.phase = "ready";
    }
}

export function pause(handle) {
    if (handle.closed) {
        return;
    }
    safely(handle, () => {
        handle.video.pause();
        if (handle.loaded && handle.phase !== "ended" && handle.phase !== "error") {
            handle.phase = "ready";
        }
    });
}

export function seekTo(handle, positionSeconds) {
    if (handle.closed || !handle.loaded) {
        return;
    }
    safely(handle, () => {
        const duration = finiteDuration(handle.video.duration);
        handle.video.currentTime = clamp(positionSeconds, 0, duration > 0 ? duration : positionSeconds);
    });
}

export function setSpeed(handle, speed) {
    if (handle.closed) {
        return;
    }
    safely(handle, () => {
        handle.video.playbackRate = clamp(speed, 0.5, 2.0);
    });
}

export function setResizeMode(handle, resizeMode) {
    if (handle.closed) {
        return;
    }
    safely(handle, () => {
        handle.resizeMode = resizeMode === "fill" ? "fill" : "fit";
        handle.video.style.objectFit = handle.resizeMode === "fill" ? "cover" : "contain";
    });
}

export function selectVideoTrack(handle, trackId) {
    if (handle.closed) {
        return;
    }
    safely(handle, () => {
        handle.videoTrackOverrideId = hasTrack(handle.videoTracks, trackId) ? trackId : "";
        applyVariantSelection(handle, "video");
    });
}

export function selectAudioTrack(handle, trackId) {
    if (handle.closed) {
        return;
    }
    safely(handle, () => {
        handle.audioTrackOverrideId = hasTrack(handle.audioTracks, trackId) ? trackId : "";
        applyVariantSelection(handle, "audio");
    });
}

export function selectTextTrack(handle, trackId) {
    if (handle.closed) {
        return;
    }
    safely(handle, () => {
        if (!trackId) {
            handle.selectedTextTrackId = "";
            handle.player.selectTextTrack(null);
            setTextVisibility(handle, false, null);
            handle.tracksRevision += 1;
            return;
        }
        const textTrack = handle.player.getTextTracks().find((track) => textTrackId(track) === trackId);
        if (!textTrack) {
            return;
        }
        handle.player.selectTextTrack(textTrack);
        setTextVisibility(handle, true, textTrack);
        handle.selectedTextTrackId = trackId;
        handle.tracksRevision += 1;
    });
}

function applyVariantSelection(handle, preferredType) {
    let requestedVideo = handle.videoTrackOverrideId;
    let requestedAudio = handle.audioTrackOverrideId;
    if (!requestedVideo) {
        if (requestedAudio) {
            const audioVariants = handle.player.getVariantTracks().filter((track) => {
                return audioTrackId(track) === requestedAudio && isSupported(track);
            });
            const selectedAudioVariant = audioVariants.find((track) => track.active) ?? audioVariants[0];
            if (selectedAudioVariant && !selectedAudioVariant.active) {
                handle.player.selectVariantTrack(selectedAudioVariant, true);
            }
        }
        handle.player.configure("abr.enabled", true);
        refreshTracks(handle);
        return;
    }

    let variants = handle.player.getVariantTracks().filter((track) => {
        const videoMatches = !requestedVideo || videoTrackId(track) === requestedVideo;
        const audioMatches = !requestedAudio || audioTrackId(track) === requestedAudio;
        return videoMatches && audioMatches && isSupported(track);
    });
    if (variants.length === 0 && requestedVideo && requestedAudio) {
        if (preferredType === "video") {
            handle.audioTrackOverrideId = "";
            requestedAudio = "";
        } else {
            handle.videoTrackOverrideId = "";
            requestedVideo = "";
            applyVariantSelection(handle, preferredType);
            return;
        }
        variants = handle.player.getVariantTracks().filter((track) => {
            const videoMatches = !requestedVideo || videoTrackId(track) === requestedVideo;
            const audioMatches = !requestedAudio || audioTrackId(track) === requestedAudio;
            return videoMatches && audioMatches && isSupported(track);
        });
    }
    const selected = variants.find((track) => track.active) ?? variants[0];
    if (!selected) {
        return;
    }
    handle.player.configure("abr.enabled", false);
    handle.player.selectVariantTrack(selected, true);
    refreshTracks(handle);
}

export function requestFilmstrip(handle, generation, requestId, positionsMillisCsv) {
    cancelFilmstrip(handle, requestId);
    const request = {
        cancelled: false,
        complete: false,
        controllers: new Set(),
        results: [],
    };
    handle.filmstripRequests.set(requestId, request);
    if (handle.closed || !handle.loaded || handle.generation !== generation || !handle.player) {
        request.complete = true;
        return;
    }
    const imageTrack = handle.player.getImageTracks()
        .filter((track) => track?.id != null)
        .sort((left, right) => (right.height ?? 0) - (left.height ?? 0))[0];
    if (!imageTrack) {
        request.complete = true;
        return;
    }
    const positionsMillis = positionsMillisCsv
        .split(",")
        .map((value) => Number(value))
        .filter((value) => Number.isFinite(value) && value >= 0);
    void runFilmstripRequest(handle, generation, request, imageTrack.id, positionsMillis);
}

async function runFilmstripRequest(handle, generation, request, imageTrackId, positionsMillis) {
    try {
        for (const positionMillis of positionsMillis) {
            if (!isFilmstripRequestCurrent(handle, generation, request)) {
                return;
            }
            const thumbnail = await handle.player.getThumbnails(imageTrackId, positionMillis / 1000);
            if (!thumbnail || !isFilmstripRequestCurrent(handle, generation, request)) {
                continue;
            }
            const encodedPng = await renderManifestThumbnail(request, thumbnail);
            if (encodedPng && isFilmstripRequestCurrent(handle, generation, request)) {
                request.results.push({ positionMillis, encodedPng });
            }
        }
    } catch (_) {
        // Filmstrip failure is non-fatal; successfully decoded manifest frames remain available.
    } finally {
        request.complete = true;
        abortFilmstripFetches(request);
    }
}

async function renderManifestThumbnail(request, thumbnail) {
    const uri = thumbnail.uris?.[0];
    if (!uri || request.cancelled) {
        return "";
    }
    const controller = new AbortController();
    request.controllers.add(controller);
    try {
        const headers = new Headers();
        if (thumbnail.startByte != null && thumbnail.endByte != null) {
            headers.set("Range", `bytes=${thumbnail.startByte}-${thumbnail.endByte}`);
        }
        const response = await fetch(uri, {
            headers,
            signal: controller.signal,
        });
        if (!response.ok || request.cancelled) {
            return "";
        }
        const blob = await response.blob();
        const sprite = await createImageBitmap(blob);
        try {
            if (request.cancelled) {
                return "";
            }
            const width = positiveDimension(thumbnail.width, sprite.width);
            const height = positiveDimension(thumbnail.height, sprite.height);
            const canvas = document.createElement("canvas");
            canvas.width = width;
            canvas.height = height;
            try {
                const context = canvas.getContext("2d");
                if (!context) {
                    return "";
                }
                context.drawImage(
                    sprite,
                    thumbnail.positionX ?? 0,
                    thumbnail.positionY ?? 0,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                );
                const encoded = canvas.toDataURL("image/png");
                return encoded.substring(encoded.indexOf(",") + 1);
            } finally {
                canvas.width = 0;
                canvas.height = 0;
            }
        } finally {
            sprite.close?.();
        }
    } catch (_) {
        return "";
    } finally {
        request.controllers.delete(controller);
    }
}

export function cancelFilmstrip(handle, requestId) {
    const request = handle.filmstripRequests.get(requestId);
    if (!request) {
        return;
    }
    request.cancelled = true;
    abortFilmstripFetches(request);
    handle.filmstripRequests.delete(requestId);
}

export function filmstripResultCount(handle, requestId) {
    return handle.filmstripRequests.get(requestId)?.results.length ?? 0;
}

export function filmstripResultPositionMillis(handle, requestId, index) {
    return handle.filmstripRequests.get(requestId)?.results[index]?.positionMillis ?? 0;
}

export function filmstripResultEncodedPng(handle, requestId, index) {
    return handle.filmstripRequests.get(requestId)?.results[index]?.encodedPng ?? "";
}

export function filmstripIsComplete(handle, requestId) {
    return handle.filmstripRequests.get(requestId)?.complete ?? true;
}

export function releaseFilmstrip(handle, requestId) {
    cancelFilmstrip(handle, requestId);
}

export function close(handle) {
    if (handle.closed) {
        return;
    }
    handle.closed = true;
    handle.generation += 1;
    cancelAllFilmstripRequests(handle);
    handle.videoListeners.forEach(({ type, listener }) => {
        try {
            handle.video.removeEventListener(type, listener);
        } catch (_) {
            // Continue terminal cleanup.
        }
    });
    handle.videoListeners = [];
    handle.shakaListeners.forEach(({ type, listener }) => {
        try {
            handle.player?.removeEventListener(type, listener);
        } catch (_) {
            // Continue terminal cleanup.
        }
    });
    handle.shakaListeners = [];
    try {
        handle.video.pause();
        handle.video.removeAttribute("src");
        handle.video.load();
        handle.video.remove();
    } catch (_) {
        try {
            handle.video.remove();
        } catch (_) {
            // DOM release is best effort after the handle is synchronously quiesced.
        }
    }
    clearTracks(handle);

    if (!handle.destroyStarted && handle.player) {
        handle.destroyStarted = true;
        handle.cleanup.destroyStartCount += 1;
        try {
            const destroyResult = handle.player.destroy();
            void Promise.resolve(destroyResult).then(
                () => {
                    handle.cleanup.destroySettled = true;
                },
                () => {
                    handle.cleanup.destroySettled = true;
                    handle.cleanup.destroyRejectionCaught = true;
                },
            );
        } catch (_) {
            handle.cleanup.destroySettled = true;
            handle.cleanup.destroyRejectionCaught = true;
        }
    }
}

export function generation(handle) {
    return handle.generation;
}

export function phase(handle) {
    return handle.phase;
}

export function isPlaying(handle) {
    return handle.loaded && !handle.failure && !handle.video.paused && !handle.video.ended;
}

export function positionMillis(handle) {
    return handle.loaded ? secondsToMillis(handle.video.currentTime) : 0;
}

export function durationMillis(handle) {
    return handle.loaded ? secondsToMillis(handle.video.duration) : 0;
}

export function bufferedPositionMillis(handle) {
    if (!handle.loaded) {
        return 0;
    }
    const ranges = handle.video.buffered;
    if (!ranges || ranges.length === 0) {
        return 0;
    }
    const position = handle.video.currentTime;
    for (let index = 0; index < ranges.length; index += 1) {
        if (position >= ranges.start(index) && position <= ranges.end(index)) {
            return secondsToMillis(ranges.end(index));
        }
    }
    return secondsToMillis(ranges.end(ranges.length - 1));
}

export function aspectRatio(handle) {
    if (!handle.loaded) {
        return 0;
    }
    const width = handle.video.videoWidth;
    const height = handle.video.videoHeight;
    return width > 0 && height > 0 ? width / height : 0;
}

export function speed(handle) {
    return handle.video.playbackRate;
}

export function resizeMode(handle) {
    return handle.resizeMode;
}

export function hasFailure(handle) {
    return handle.failure;
}

export function isFailureRecoverable(handle) {
    return handle.recoverable;
}

export function trackCount(handle, type) {
    return tracks(handle, type).length;
}

export function tracksRevision(handle) {
    return handle.tracksRevision;
}

export function trackId(handle, type, index) {
    return tracks(handle, type)[index]?.id ?? "";
}

export function trackLabel(handle, type, index) {
    return tracks(handle, type)[index]?.label ?? "";
}

export function trackLanguage(handle, type, index) {
    return tracks(handle, type)[index]?.language ?? "";
}

export function trackVideoHeight(handle, type, index) {
    return tracks(handle, type)[index]?.videoHeight ?? -1;
}

export function trackIsSupported(handle, type, index) {
    return tracks(handle, type)[index]?.isSupported ?? false;
}

export function selectedTrackId(handle, type) {
    if (type === "video") {
        return handle.selectedVideoTrackId;
    }
    if (type === "audio") {
        return handle.selectedAudioTrackId;
    }
    return handle.selectedTextTrackId;
}

function configureVideoElement(video) {
    video.controls = false;
    video.autoplay = false;
    video.preload = "metadata";
    video.playsInline = true;
    video.setAttribute("playsinline", "true");
    video.setAttribute("aria-label", "Video playback");
    video.setAttribute("data-testid", "playback-video");
    video.style.cssText = "display:block;width:100%;height:100%;background:#000;object-fit:contain;";
}

function installVideoListeners(handle) {
    VIDEO_EVENTS.forEach((type) => {
        const listener = () => onVideoEvent(handle, type);
        handle.video.addEventListener(type, listener);
        handle.videoListeners.push({ type, listener });
    });
    const errorListener = () => {
        if (handle.loaded) {
            setFailure(handle, true);
        }
    };
    handle.video.addEventListener("error", errorListener);
    handle.videoListeners.push({ type: "error", listener: errorListener });
}

function onVideoEvent(handle, type) {
    if (handle.closed || handle.failure || !handle.loaded) {
        return;
    }
    if (type === "ended") {
        handle.phase = "ended";
    } else if (type === "waiting" || type === "stalled" || type === "seeking") {
        handle.phase = "buffering";
    } else if (type === "canplay" || type === "playing" || type === "seeked") {
        handle.phase = handle.video.ended ? "ended" : "ready";
    }
}

function installShakaListeners(handle) {
    addShakaListener(handle, "buffering", (event) => {
        if (!handle.closed && handle.loaded && !handle.failure) {
            handle.phase = event.buffering ? "buffering" : (handle.video.ended ? "ended" : "ready");
        }
    });
    addShakaListener(handle, "error", (event) => {
        if (!handle.loaded) {
            return;
        }
        if (event?.detail?.code === shaka.util.Error.Code.LOAD_INTERRUPTED) {
            return;
        }
        const recoverable = event?.detail?.severity !== shaka.util.Error.Severity.CRITICAL;
        setFailure(handle, recoverable);
    });
    ["adaptation", "trackschanged", "variantchanged", "textchanged"].forEach((type) => {
        addShakaListener(handle, type, () => refreshTracks(handle));
    });
}

function addShakaListener(handle, type, listener) {
    handle.player.addEventListener(type, listener);
    handle.shakaListeners.push({ type, listener });
}

function refreshTracks(handle) {
    if (handle.closed || !handle.loaded) {
        clearTracks(handle);
        return;
    }
    const variants = handle.player.getVariantTracks();
    updateActiveVariantSelection(handle, variants);
    handle.videoTracks = uniqueTracks(variants.filter(hasVideo).map((track, index) => ({
        id: videoTrackId(track),
        label: track.height > 0 ? `${track.height}p` : (track.videoLabel || track.label || `Video ${index + 1}`),
        language: track.videoLanguage || "",
        videoHeight: track.height > 0 ? track.height : -1,
        isSupported: isSupported(track),
    })));
    handle.audioTracks = uniqueTracks(variants.filter(hasAudio).map((track, index) => ({
        id: audioTrackId(track),
        label: track.audioLabel || track.label || track.language || `Audio ${index + 1}`,
        language: track.audioLanguage || track.language || "",
        videoHeight: -1,
        isSupported: isSupported(track),
    })));
    handle.textTracks = uniqueTracks(handle.player.getTextTracks().map((track, index) => ({
        id: textTrackId(track),
        label: track.label || track.language || `Text ${index + 1}`,
        language: track.language || "",
        videoHeight: -1,
        isSupported: isSupported(track),
    })));
    const activeTextTrack = handle.player.getTextTracks().find((track) => track.active);
    handle.selectedTextTrackId = activeTextTrack ? textTrackId(activeTextTrack) : "";
    setTextVisibility(handle, activeTextTrack != null, activeTextTrack ?? null);
    handle.tracksRevision += 1;
}

function updateActiveVariantSelection(handle, variants) {
    const activeVariant = variants.find((track) => track.active);
    handle.selectedVideoTrackId = handle.videoTrackOverrideId && activeVariant && hasVideo(activeVariant)
        ? videoTrackId(activeVariant)
        : "";
    handle.selectedAudioTrackId = activeVariant && hasAudio(activeVariant)
        ? audioTrackId(activeVariant)
        : "";
}

function setTextVisibility(handle, visible, selectedTrack) {
    if (typeof handle.player?.setTextTrackVisibility === "function") {
        handle.player.setTextTrackVisibility(visible);
    }
    const nativeTracks = handle.video?.textTracks;
    if (!nativeTracks) {
        return;
    }
    let matchingIndex = -1;
    if (visible && selectedTrack) {
        for (let index = 0; index < nativeTracks.length; index += 1) {
            const nativeTrack = nativeTracks[index];
            if ((selectedTrack.language && nativeTrack.language === selectedTrack.language) ||
                (selectedTrack.label && nativeTrack.label === selectedTrack.label)) {
                matchingIndex = index;
                break;
            }
        }
        if (matchingIndex < 0 && nativeTracks.length > 0) {
            matchingIndex = 0;
        }
    }
    for (let index = 0; index < nativeTracks.length; index += 1) {
        nativeTracks[index].mode = visible && index === matchingIndex ? "showing" : "disabled";
    }
}

function clearTracks(handle) {
    handle.videoTracks = [];
    handle.audioTracks = [];
    handle.textTracks = [];
    handle.tracksRevision += 1;
}

function cancelAllFilmstripRequests(handle) {
    Array.from(handle.filmstripRequests.keys()).forEach((requestId) => {
        cancelFilmstrip(handle, requestId);
    });
}

function abortFilmstripFetches(request) {
    request.controllers.forEach((controller) => controller.abort());
    request.controllers.clear();
}

function isFilmstripRequestCurrent(handle, generation, request) {
    return !handle.closed &&
        handle.loaded &&
        handle.generation === generation &&
        !request.cancelled;
}

function positiveDimension(value, fallback) {
    return Number.isFinite(value) && value > 0 ? Math.round(value) : Math.max(1, Math.round(fallback));
}

function tracks(handle, type) {
    if (type === "video") {
        return handle.videoTracks;
    }
    if (type === "audio") {
        return handle.audioTracks;
    }
    return handle.textTracks;
}

function uniqueTracks(items) {
    const byId = new Map();
    items.forEach((item) => {
        if (item.id && !byId.has(item.id)) {
            byId.set(item.id, item);
        }
    });
    return Array.from(byId.values());
}

function hasTrack(items, trackId) {
    return !!trackId && items.some((track) => track.id === trackId && track.isSupported);
}

function hasVideo(track) {
    return track.videoId != null || track.originalVideoId != null;
}

function hasAudio(track) {
    return track.audioId != null || track.originalAudioId != null;
}

function videoTrackId(track) {
    return `video:${track.videoId ?? track.originalVideoId ?? track.id}`;
}

function audioTrackId(track) {
    return `audio:${track.audioId ?? track.originalAudioId ?? track.id}`;
}

function textTrackId(track) {
    return `text:${track.originalTextId ?? track.id}`;
}

function isSupported(track) {
    return track.allowedByApplication !== false && track.allowedByKeySystem !== false;
}

function setFailure(handle, recoverable) {
    if (handle.closed) {
        return;
    }
    handle.failure = true;
    handle.recoverable = !!recoverable;
    handle.phase = "error";
    try {
        handle.video.pause();
    } catch (_) {
        // Failure state is still authoritative when the media element cannot pause.
    }
}

function safely(handle, block) {
    try {
        block();
    } catch (_) {
        setFailure(handle, true);
    }
}

function isCurrent(handle, generation) {
    return !handle.closed && handle.generation === generation;
}

function secondsToMillis(value) {
    return Math.round(finiteDuration(value) * 1000);
}

function finiteDuration(value) {
    return Number.isFinite(value) && value >= 0 ? value : 0;
}

function clamp(value, minimum, maximum) {
    return Math.min(Math.max(value, minimum), maximum);
}

const cleanupProbes = new Map();
let nextCleanupProbeId = 1;

export function runCleanupProbe(rejectDestroy) {
    const video = document.createElement("video");
    const handle = {
        video,
        player: {
            removeEventListener() {},
            destroy() {
                return rejectDestroy ? Promise.reject(new Error("expected probe rejection")) : Promise.resolve();
            },
        },
        generation: 0,
        closed: false,
        destroyStarted: false,
        loaded: false,
        phase: "idle",
        failure: false,
        recoverable: true,
        resizeMode: "fit",
        selectedVideoTrackId: "",
        selectedAudioTrackId: "",
        selectedTextTrackId: "",
        videoTrackOverrideId: "",
        audioTrackOverrideId: "",
        videoTracks: [],
        audioTracks: [],
        textTracks: [],
        tracksRevision: 0,
        filmstripRequests: new Map(),
        cleanup: {
            destroyStartCount: 0,
            destroySettled: false,
            destroyRejectionCaught: false,
        },
        videoListeners: [],
        shakaListeners: [],
    };
    close(handle);
    close(handle);
    const probeId = nextCleanupProbeId;
    nextCleanupProbeId += 1;
    cleanupProbes.set(probeId, handle.cleanup);
    return probeId;
}

export function cleanupProbeDestroyStartCount(probeId) {
    return cleanupProbes.get(probeId)?.destroyStartCount ?? 0;
}

export function cleanupProbeDestroySettled(probeId) {
    return cleanupProbes.get(probeId)?.destroySettled ?? false;
}

export function cleanupProbeRejectionCaught(probeId) {
    return cleanupProbes.get(probeId)?.destroyRejectionCaught ?? false;
}

export function releaseCleanupProbe(probeId) {
    cleanupProbes.delete(probeId);
}

export function verifyTrackAndTextContract() {
    const textTracks = [
        { id: 1, originalTextId: "en", active: false, language: "en", label: "English" },
        { id: 2, originalTextId: "el", active: false, language: "el", label: "Greek" },
    ];
    const handle = {
        closed: false,
        tracksRevision: 0,
        selectedVideoTrackId: "",
        selectedAudioTrackId: "",
        selectedTextTrackId: "",
        videoTrackOverrideId: "video:2",
        audioTrackOverrideId: "audio:20",
        video: {
            textTracks: [
                { language: "en", label: "English", mode: "disabled" },
                { language: "el", label: "Greek", mode: "disabled" },
            ],
        },
        player: {
            getTextTracks() {
                return textTracks;
            },
            selectTextTrack(selectedTrack) {
                textTracks.forEach((track) => {
                    track.active = track === selectedTrack;
                });
            },
        },
    };
    updateActiveVariantSelection(handle, [
        { active: false, videoId: 1, audioId: 10 },
        { active: true, videoId: 2, audioId: 20 },
    ]);
    selectTextTrack(handle, "text:el");
    const visible = handle.video.textTracks[0].mode === "disabled" &&
        handle.video.textTracks[1].mode === "showing" &&
        handle.selectedTextTrackId === "text:el" &&
        textTracks[1].active;
    selectTextTrack(handle, "");
    const hidden = handle.video.textTracks.every((track) => track.mode === "disabled");
    return handle.selectedVideoTrackId === "video:2" &&
        handle.selectedAudioTrackId === "audio:20" &&
        handle.selectedTextTrackId === "" &&
        textTracks.every((track) => !track.active) &&
        visible &&
        hidden;
}

export function verifyVideoAutoWithAudioOverrideContract() {
    const variants = [
        {
            active: true,
            id: 1,
            videoId: 1,
            audioId: 20,
            height: 720,
            language: "el",
            audioLanguage: "el",
        },
        {
            active: false,
            id: 2,
            videoId: 2,
            audioId: 20,
            height: 1080,
            language: "el",
            audioLanguage: "el",
        },
    ];
    let abrEnabled = false;
    const handle = {
        closed: false,
        loaded: true,
        tracksRevision: 0,
        selectedVideoTrackId: "video:1",
        selectedAudioTrackId: "audio:20",
        selectedTextTrackId: "",
        videoTrackOverrideId: "video:1",
        audioTrackOverrideId: "audio:20",
        videoTracks: [
            { id: "video:1", isSupported: true },
            { id: "video:2", isSupported: true },
        ],
        audioTracks: [{ id: "audio:20", isSupported: true }],
        textTracks: [],
        video: { textTracks: [] },
        player: {
            configure(path, value) {
                if (path === "abr.enabled") {
                    abrEnabled = value;
                }
            },
            getVariantTracks() {
                return variants;
            },
            selectVariantTrack(selectedTrack) {
                variants.forEach((track) => {
                    track.active = track === selectedTrack;
                });
            },
            getTextTracks() {
                return [];
            },
        },
    };

    selectVideoTrack(handle, "");

    return abrEnabled &&
        handle.videoTrackOverrideId === "" &&
        handle.audioTrackOverrideId === "audio:20" &&
        handle.selectedVideoTrackId === "" &&
        handle.selectedAudioTrackId === "audio:20";
}
