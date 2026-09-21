export function printStyle(style = {}) {
  const verticalAlign = style.verticalAlign || 'top'
  const justifyContent = verticalAlign === 'middle'
    ? 'center'
    : verticalAlign === 'bottom'
      ? 'flex-end'
      : 'flex-start'
  return {
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    justifyContent,
    width: '100%',
    height: '100%',
    fontFamily: style.fontFamily || 'Microsoft YaHei, PingFang SC, sans-serif',
    fontSize: `${style.fontSizePt ?? 10}pt`,
    fontWeight: style.fontWeight ?? 400,
    fontStyle: style.fontStyle || 'normal',
    lineHeight: style.lineHeight ?? 1.4,
    textAlign: style.textAlign || 'left',
    textDecoration: style.textDecoration || 'none',
    color: style.color || '#000000',
    backgroundColor: style.backgroundColor || 'transparent',
    border: `${style.borderWidthMm ?? 0}mm ${style.borderStyle || 'solid'} ${style.borderColor || '#000000'}`,
    borderRadius: `${style.borderRadiusMm ?? 0}mm`,
    padding: `${style.paddingMm ?? 0}mm`,
    margin: 0,
    whiteSpace: 'pre-wrap',
    overflowWrap: 'anywhere',
    wordBreak: 'normal',
  }
}

export function elementStyle(element) {
  return {
    position: 'absolute',
    left: `${element.xMm}mm`,
    top: `${element.yMm}mm`,
    width: `${element.widthMm}mm`,
    height: `${element.heightMm}mm`,
    transform: `rotate(${element.rotationDeg || 0}deg) scaleX(${element.flipX ? -1 : 1}) scaleY(${element.flipY ? -1 : 1})`,
    transformOrigin: 'center center',
    ...printStyle(element.style),
  }
}

export function cellStyle(style = {}) {
  const textAlign = style.textAlign || 'left'
  const verticalAlign = style.verticalAlign || 'middle'
  const base = printStyle({ paddingMm: 1, borderWidthMm: 0.15, verticalAlign, ...style })
  return {
    ...base,
    display: 'flex',
    flexDirection: 'row',
    alignItems: verticalAlign === 'top' ? 'flex-start' : verticalAlign === 'bottom' ? 'flex-end' : 'center',
    justifyContent: textAlign === 'center' ? 'center' : textAlign === 'right' ? 'flex-end' : 'flex-start',
    textAlign,
  }
}

/** 表格外框不画占宽的 border，避免贴满纸张时右/下边被 overflow 裁切。外框由单元格 border 承担。 */
export function tableFrameStyle(style = {}) {
  return {
    boxSizing: 'border-box',
    border: 'none',
    backgroundColor: style.backgroundColor || '#fff',
    overflow: 'visible',
  }
}

/**
 * 表格单元格：四面都用同一条 CSS border，避免上/左用渐变、右/下用 border 打印时粗细不一。
 * 只给首行补上边、首列补左边，内线不叠加。底色 clip 到 padding，不会画进边框里把线“吃细”。
 */
export function tableCellStyle(style = {}, edges = {}) {
  const width = style.borderWidthMm ?? 0.15
  const color = style.borderColor || '#000000'
  const borderStyle = style.borderStyle || 'solid'
  const line = width > 0 ? `${width}mm ${borderStyle} ${color}` : 'none'
  const base = { ...cellStyle({ ...style, borderWidthMm: 0 }) }
  delete base.border
  return {
    ...base,
    borderTop: edges.top ? line : 'none',
    borderLeft: edges.left ? line : 'none',
    borderRight: line,
    borderBottom: line,
    backgroundClip: 'padding-box',
  }
}
