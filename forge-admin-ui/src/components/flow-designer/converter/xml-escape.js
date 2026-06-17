/**
 * XML 字符串转义工具。
 * 仅转义 BPMN 输出场景需要的字符，非追求完整规范。
 */

/**
 * 转义属性值（双引号场景）。
 */
export function escapeXmlAttr(s) {
  if (s == null)
    return ''
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/\n/g, '&#10;')
    .replace(/\r/g, '&#13;')
    .replace(/\t/g, '&#9;')
}

/**
 * 转义文本节点内容（不需要处理引号）。
 */
export function escapeXmlText(s) {
  if (s == null)
    return ''
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
