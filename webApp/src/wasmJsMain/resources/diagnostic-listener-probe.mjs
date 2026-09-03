const trackedDocumentEvents = new Set(["fullscreenchange", "visibilitychange", "focusin", "keydown"]);
const registrations = new Map();
let installed = false;
let activeCount = 0;

export function install() {
  if (installed) {
    return;
  }
  installed = true;
  const originalAdd = EventTarget.prototype.addEventListener;
  const originalRemove = EventTarget.prototype.removeEventListener;

  EventTarget.prototype.addEventListener = function(type, listener, options) {
    originalAdd.call(this, type, listener, options);
    if (!listener || !shouldTrack(this, type)) {
      return;
    }
    const capture = captureValue(options);
    const byType = registrations.get(this) ?? new Map();
    const byListener = byType.get(type) ?? new Map();
    const captures = byListener.get(listener) ?? new Set();
    if (!captures.has(capture)) {
      captures.add(capture);
      activeCount += 1;
    }
    byListener.set(listener, captures);
    byType.set(type, byListener);
    registrations.set(this, byType);
  };

  EventTarget.prototype.removeEventListener = function(type, listener, options) {
    originalRemove.call(this, type, listener, options);
    if (!listener || !shouldTrack(this, type)) {
      return;
    }
    const capture = captureValue(options);
    const byType = registrations.get(this);
    const byListener = byType?.get(type);
    const captures = byListener?.get(listener);
    if (!captures?.delete(capture)) {
      return;
    }
    activeCount = Math.max(0, activeCount - 1);
    if (captures.size === 0) {
      byListener.delete(listener);
    }
    if (byListener.size === 0) {
      byType.delete(type);
    }
    if (byType.size === 0) {
      registrations.delete(this);
    }
  };
}

export function count() {
  return activeCount;
}

export function focusedAction() {
  let active = document.activeElement;
  while (active?.shadowRoot?.activeElement) {
    active = active.shadowRoot.activeElement;
  }
  return active?.id === "web-player:fullscreen" ? "fullscreen" : "";
}

function shouldTrack(target, type) {
  if (target === document) {
    return trackedDocumentEvents.has(type);
  }
  return target instanceof HTMLVideoElement &&
    (target.dataset.testid === "player:video" || target.dataset.testid === "playback-video");
}

function captureValue(options) {
  if (typeof options === "boolean") {
    return options;
  }
  return options?.capture === true;
}
