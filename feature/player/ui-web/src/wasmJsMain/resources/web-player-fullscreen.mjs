export function isFullscreen() {
  return document.fullscreenElement !== null;
}

export function isDocumentVisible() {
  return document.visibilityState === "visible";
}

export function toggleFullscreen() {
  if (document.fullscreenElement !== null) {
    void document.exitFullscreen().catch(() => {});
    return;
  }

  const target = document.documentElement;
  if (target?.requestFullscreen) {
    void target.requestFullscreen().catch(() => {});
  }
}

export function exitFullscreen() {
  if (document.fullscreenElement !== null) {
    void document.exitFullscreen().catch(() => {});
  }
}
