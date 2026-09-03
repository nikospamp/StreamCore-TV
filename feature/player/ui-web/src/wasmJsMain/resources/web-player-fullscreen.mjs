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

export function focusProjectedElement(elementId) {
  let attempts = 0;
  const focusWhenReady = () => {
    const element = findElementById(document, elementId);
    if (element instanceof HTMLElement) {
      element.tabIndex = 0;
      element.focus({ preventScroll: true });
      return;
    }
    attempts += 1;
    if (attempts < 6) {
      requestAnimationFrame(focusWhenReady);
    }
  };
  requestAnimationFrame(focusWhenReady);
}

function findElementById(root, elementId) {
  const direct = Array.from(root.querySelectorAll?.("[id]") ?? [])
    .find((element) => element.id === elementId);
  if (direct) {
    return direct;
  }
  for (const element of root.querySelectorAll?.("*") ?? []) {
    if (element.shadowRoot) {
      const nested = findElementById(element.shadowRoot, elementId);
      if (nested) {
        return nested;
      }
    }
  }
  return null;
}
