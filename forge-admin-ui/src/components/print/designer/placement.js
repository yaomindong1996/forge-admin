/** Default inset when the canvas is empty (mm). */
export const DEFAULT_ORIGIN_MM = 3
/** Gap between auto-packed elements (mm). */
export const PLACEMENT_GAP_MM = 2

/** Bottom edge of existing content on a free-canvas surface. */
export function contentBottomMm(elements = []) {
  if (!elements.length)
    return 0
  return elements.reduce((max, element) => Math.max(max, (Number(element.yMm) || 0) + (Number(element.heightMm) || 0)), 0)
}

function overlaps(a, b, gap = 1) {
  return !(a.xMm + a.widthMm + gap <= b.xMm
    || b.xMm + b.widthMm + gap <= a.xMm
    || a.yMm + a.heightMm + gap <= b.yMm
    || b.yMm + b.heightMm + gap <= a.yMm)
}

/**
 * Resolve insert position for palette clicks vs canvas drops.
 * Explicit drop coords keep the requested point; omitted position packs below/right of existing elements.
 */
export function resolvePlacement(surface, widthMm, heightMm, contentWidthMm, position) {
  const maxX = Math.max(0, contentWidthMm - widthMm)
  const hasExplicit = position
    && (Number.isFinite(position.xMm) || Number.isFinite(position.yMm))
  if (hasExplicit) {
    return {
      xMm: Math.max(0, Math.min(Number.isFinite(position.xMm) ? position.xMm : DEFAULT_ORIGIN_MM, maxX)),
      yMm: Math.max(0, Number.isFinite(position.yMm) ? position.yMm : DEFAULT_ORIGIN_MM),
    }
  }

  const elements = surface?.elements || []
  if (!elements.length) {
    return {
      xMm: Math.min(DEFAULT_ORIGIN_MM, maxX),
      yMm: DEFAULT_ORIGIN_MM,
    }
  }

  // Prefer same row to the right of the last element when it fits without overlap.
  const last = elements[elements.length - 1]
  const right = {
    xMm: (Number(last.xMm) || 0) + (Number(last.widthMm) || 0) + PLACEMENT_GAP_MM,
    yMm: Number(last.yMm) || 0,
    widthMm,
    heightMm,
  }
  if (right.xMm + widthMm <= contentWidthMm + 0.001 && !elements.some(element => overlaps(right, element))) {
    return {
      xMm: Number(right.xMm.toFixed(3)),
      yMm: Number(right.yMm.toFixed(3)),
    }
  }

  return {
    xMm: Math.min(DEFAULT_ORIGIN_MM, maxX),
    yMm: Number((contentBottomMm(elements) + PLACEMENT_GAP_MM).toFixed(3)),
  }
}

/** Grow FIXED / header / footer height so the new bottom stays inside the band. */
export function ensureSurfaceFits(surface, bottomMm) {
  if (!surface || !Number.isFinite(surface.heightMm) || !Number.isFinite(bottomMm))
    return
  surface.heightMm = Number(Math.max(surface.heightMm, bottomMm).toFixed(3))
}
