import { create as svgCreate } from 'tiny-svg';
/**
 * CanvasBackground - custom bpmn-js background with dot-grid and gradient.
 * Exports default function CanvasBackground(config, eventBus, canvas)
 */
function CanvasBackground(config, eventBus, canvas) {
  let svgRoot;
  let bgLayer;
  let dotsGroup;
  let gradientRect;
  let defs;

  function ensureSvgLayer() {
    var container = (canvas && canvas.getContainer) ? canvas.getContainer() : null;
    if (!container) return;
    svgRoot = container.querySelector('svg');
    if (!svgRoot) return;
    bgLayer = svgRoot.querySelector('#forge-bg-layer');
    if (!bgLayer) {
      bgLayer = svgCreate('g');
      bgLayer.setAttribute('id', 'forge-bg-layer');
      if (svgRoot.firstChild) {
        svgRoot.insertBefore(bgLayer, svgRoot.firstChild);
      } else {
        svgRoot.appendChild(bgLayer);
      }
    }
    defs = svgRoot.querySelector('defs');
    if (!defs) {
      defs = svgCreate('defs');
      svgRoot.insertBefore(defs, svgRoot.firstChild);
    }
    var grad = svgRoot.querySelector('#forge-bg-grad');
    if (!grad) {
      grad = svgCreate('linearGradient');
      grad.setAttribute('id', 'forge-bg-grad');
      var s1 = svgCreate('stop');
      s1.setAttribute('offset', '0%');
      s1.setAttribute('stop-color', '#f8fafc');
      var s2 = svgCreate('stop');
      s2.setAttribute('offset', '100%');
      s2.setAttribute('stop-color', '#f1f5f9');
      grad.appendChild(s1);
      grad.appendChild(s2);
      defs.appendChild(grad);
    }
  }

  function ensureBackgroundRect() {
    gradientRect = svgRoot.querySelector('#forge-bg-rect');
    if (!gradientRect) {
      gradientRect = svgCreate('rect');
      gradientRect.setAttribute('id', 'forge-bg-rect');
      gradientRect.setAttribute('fill', 'url(#forge-bg-grad)');
      bgLayer.appendChild(gradientRect);
    }
  }

  function ensureDotsGroup() {
    dotsGroup = svgRoot.querySelector('#forge-bg-dots');
    if (!dotsGroup) {
      dotsGroup = svgCreate('g');
      dotsGroup.setAttribute('id', 'forge-bg-dots');
      bgLayer.appendChild(dotsGroup);
    }
  }

  function getViewBox() {
    if (!svgRoot) return { x: 0, y: 0, width: 1000, height: 800 };
    var vbAttr = svgRoot.getAttribute('viewBox');
    if (vbAttr) {
      var parts = vbAttr.trim().split(/\s+/).map(Number);
      if (parts.length === 4 && parts.every(function(n){ return typeof n === 'number' && !isNaN(n); })) {
        return { x: parts[0], y: parts[1], width: parts[2], height: parts[3] };
      }
    }
    var r = svgRoot.getBoundingClientRect();
    return { x: 0, y: 0, width: r.width, height: r.height };
  }

  function clearDots() {
    if (!dotsGroup) return;
    while (dotsGroup.firstChild) {
      dotsGroup.removeChild(dotsGroup.firstChild);
    }
  }

  function drawDots(vb) {
    if (!dotsGroup) return;
    clearDots();
    var spacing = 20;
    var startX = Math.floor(vb.x / spacing) * spacing;
    var endX = Math.ceil((vb.x + vb.width) / spacing) * spacing;
    var startY = Math.floor(vb.y / spacing) * spacing;
    var endY = Math.ceil((vb.y + vb.height) / spacing) * spacing;
    for (var x = startX; x <= endX; x += spacing) {
      for (var y = startY; y <= endY; y += spacing) {
        if (x % 100 === 0 && y % 100 === 0) {
          var cMajor = svgCreate('circle');
          cMajor.setAttribute('cx', x);
          cMajor.setAttribute('cy', y);
          cMajor.setAttribute('r', 2.5);
          cMajor.setAttribute('fill', '#94a3b8');
          cMajor.setAttribute('opacity', '0.5');
          dotsGroup.appendChild(cMajor);
        } else if (x % 20 === 0 && y % 20 === 0) {
          var c = svgCreate('circle');
          c.setAttribute('cx', x);
          c.setAttribute('cy', y);
          c.setAttribute('r', 1.5);
          c.setAttribute('fill', '#cbd5e1');
          c.setAttribute('opacity', '0.4');
          dotsGroup.appendChild(c);
        }
      }
    }
  }

  function redraw() {
    if (!svgRoot) return;
    var vb = getViewBox();
    if (gradientRect) {
      gradientRect.setAttribute('x', vb.x);
      gradientRect.setAttribute('y', vb.y);
      gradientRect.setAttribute('width', vb.width);
      gradientRect.setAttribute('height', vb.height);
    }
    drawDots(vb);
  }

  function onViewBoxChanged() {
    redraw();
  }

  function onDestroy() {
    if (dotsGroup && dotsGroup.parentNode) dotsGroup.parentNode.removeChild(dotsGroup);
    if (gradientRect && gradientRect.parentNode) gradientRect.parentNode.removeChild(gradientRect);
    if (bgLayer && bgLayer.parentNode) bgLayer.parentNode.removeChild(bgLayer);
    if (defs && defs.parentNode) defs.parentNode.removeChild(defs);
    svgRoot = null;
    eventBus.off('canvas.viewbox.changed', onViewBoxChanged);
    eventBus.off('canvas.destroy', onDestroy);
    eventBus.off('canvas.init', onCanvasInit);
  }

  // Initialize when canvas is ready; also listen for canvas.init to be robust
  ensureSvgLayer();
  if (!svgRoot) return;
  ensureBackgroundRect();
  ensureDotsGroup();
  redraw();
  eventBus.on('canvas.viewbox.changed', onViewBoxChanged);
  eventBus.on('canvas.destroy', onDestroy);
  // ensure we also handle explicit canvas.init
  function onCanvasInit() {
    ensureSvgLayer();
    if (svgRoot) {
      ensureBackgroundRect();
      ensureDotsGroup();
      redraw();
    }
  }
  eventBus.on('canvas.init', onCanvasInit);
}

CanvasBackground.$inject = ['config', 'eventBus', 'canvas'];
export default CanvasBackground;
