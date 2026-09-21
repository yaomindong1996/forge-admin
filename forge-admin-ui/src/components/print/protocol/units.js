const CSS_PIXELS_PER_INCH = 96
const MILLIMETRES_PER_INCH = 25.4
const POINTS_PER_INCH = 72

export function mmToPx(mm) {
  return mm * CSS_PIXELS_PER_INCH / MILLIMETRES_PER_INCH
}

export function pxToMm(px) {
  return px * MILLIMETRES_PER_INCH / CSS_PIXELS_PER_INCH
}

export function ptToMm(pt) {
  return pt * MILLIMETRES_PER_INCH / POINTS_PER_INCH
}

export function screenDeltaToMm(px, zoom = 1) {
  if (!Number.isFinite(px) || !Number.isFinite(zoom) || zoom <= 0) {
    throw new TypeError('移动距离与缩放比例必须是有效数字，缩放比例必须大于零')
  }
  return pxToMm(px / zoom)
}

/** Dimensions are stored portrait-first; orientation resolves the physical sides once. */
export function paperGeometry(document) {
  const { paper, header, footer } = document
  const shorter = Math.min(paper.widthMm, paper.heightMm)
  const longer = Math.max(paper.widthMm, paper.heightMm)
  const landscape = paper.orientation === 'LANDSCAPE'
  const widthMm = landscape ? longer : shorter
  const heightMm = landscape ? shorter : longer
  const margin = paper.marginMm
  return {
    widthMm,
    heightMm,
    contentWidthMm: widthMm - margin.left - margin.right,
    contentHeightMm: heightMm - margin.top - margin.bottom - header.heightMm - footer.heightMm,
    bodyTopMm: margin.top + header.heightMm,
    bodyLeftMm: margin.left,
    footerTopMm: heightMm - margin.bottom - footer.heightMm,
  }
}
