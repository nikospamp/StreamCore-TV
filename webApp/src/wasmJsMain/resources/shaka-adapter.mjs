import shakaPackage from "shaka-player/dist/shaka-player.compiled.js";

const shaka = shakaPackage?.default ?? shakaPackage ?? globalThis.shaka;

export function createAndDestroyPlayer(videoElement) {
    if (!shaka?.Player) {
        throw new Error("Shaka Player 5.2.3 did not expose Player through the validated ESM adapter.");
    }
    const player = new shaka.Player(videoElement);
    void player.destroy();
    return true;
}
