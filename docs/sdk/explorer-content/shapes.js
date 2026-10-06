/* Semantic diagram shapes. These are graph geometry, not decorative icons. */
(() => {
  'use strict';

  const kinds = Object.freeze({
    function: 'Function',
    constructor: 'Constructor',
    parameter: 'Parameter',
    variable: 'Variable',
    property: 'Property',
    contract: 'Contract',
    external: 'External',
  });

  function shapeKey(nodeKind, isConstructor = false) {
    if (nodeKind === 'callable') return isConstructor ? 'constructor' : 'function';
    if (nodeKind === 'value') return 'variable';
    return Object.hasOwn(kinds, nodeKind) ? nodeKind : 'external';
  }

  function dimension(value, fallback) {
    const numeric = Number(value);
    return Number.isFinite(numeric) && numeric > 0 ? Math.round(numeric * 1000) / 1000 : fallback;
  }

  function markup(key, width = 232, height = 86) {
    const shape = Object.hasOwn(kinds, key) ? key : 'external';
    const w = dimension(width, 232);
    const h = dimension(height, 86);
    // Insets stay within the existing x=13 text margin at the canonical size.
    const inset = Math.min(12, w * 0.2, h * 0.3);
    const boxClass = `node-box node-shape-${shape}`;

    switch (shape) {
      case 'function': {
        const side = Math.min(6, w * 0.12);
        return `<rect class="${boxClass}" width="${w}" height="${h}"/><path class="node-detail-line" d="M${side},0 V${h} M${w-side},0 V${h}"/>`;
      }
      case 'constructor':
        return `<path class="${boxClass}" d="M${inset},0 H${w-inset} L${w},${inset} V${h-inset} L${w-inset},${h} H${inset} L0,${h-inset} V${inset} Z"/>`;
      case 'parameter':
        return `<path class="${boxClass}" d="M${inset},0 H${w} L${w-inset},${h} H0 Z"/>`;
      case 'variable':
        return `<rect class="${boxClass}" width="${w}" height="${h}" rx="${h/2}" ry="${h/2}"/>`;
      case 'property':
        return `<path class="${boxClass}" d="M${inset},0 H${w-inset} L${w},${h/2} L${w-inset},${h} H${inset} L0,${h/2} Z"/>`;
      case 'contract':
        return `<path class="${boxClass}" d="M0,0 H${w-inset} L${w},${inset} V${h} H0 Z"/><path class="node-detail-line" d="M${w-inset},0 V${inset} H${w}"/>`;
      default:
        return `<rect class="${boxClass}" width="${w}" height="${h}"/>`;
    }
  }

  function legendMarkup() {
    return `<div class="node-shape-legend" role="group" aria-label="Node shapes">${Object.entries(kinds).map(([key,label]) => `<span class="node-shape-legend-item" data-shape="${key}"><svg viewBox="-2 -2 52 28" width="48" height="26" aria-hidden="true" focusable="false">${markup(key,48,24)}</svg><span>${label}</span></span>`).join('')}</div>`;
  }

  function connectionX(key, side, y, width = 232, height = 86) {
    const shape = Object.hasOwn(kinds, key) ? key : 'external';
    const w = dimension(width, 232);
    const h = dimension(height, 86);
    const inset = Math.min(12, w * 0.2, h * 0.3);
    const numericY = Number(y);
    const ordinate = Number.isFinite(numericY) ? Math.min(h, Math.max(0, numericY)) : h / 2;
    let left = 0;
    let right = w;

    switch (shape) {
      case 'parameter':
        left = inset * (1 - ordinate / h);
        right = w - inset * ordinate / h;
        break;
      case 'variable': {
        const radiusY = h / 2;
        const radiusX = Math.min(radiusY, w / 2);
        const relativeY = (ordinate - radiusY) / radiusY;
        left = radiusX * (1 - Math.sqrt(Math.max(0, 1 - relativeY * relativeY)));
        right = w - left;
        break;
      }
      case 'property':
        left = inset * Math.abs(1 - 2 * ordinate / h);
        right = w - left;
        break;
      case 'constructor':
        left = Math.max(0, inset - ordinate, ordinate - (h - inset));
        right = w - left;
        break;
      case 'contract':
        right = w - Math.max(0, inset - ordinate);
        break;
    }
    return side === 'right' ? right : left;
  }

  window.SDK_NODE_SHAPES = Object.freeze({shapeKey, markup, legendMarkup, connectionX});
})();
