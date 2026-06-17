/**
 * 钉钉样式流程设计器 - BPMN XML 解析与查询工具
 *
 * 仅依赖浏览器内置 DOMParser / XMLSerializer，jsdom 环境下也能直接运行。
 * 不再依赖 bpmn-js / moddle，转换层由本工具集驱动。
 *
 * 命名空间常量：
 *   BPMN     http://www.omg.org/spec/BPMN/20100524/MODEL
 *   BPMNDI   http://www.omg.org/spec/BPMN/20100524/DI
 *   DC       http://www.omg.org/spec/DD/20100524/DC
 *   DI       http://www.omg.org/spec/DD/20100524/DI
 *   FLOWABLE http://flowable.org/bpmn
 *   XSI      http://www.w3.org/2001/XMLSchema-instance
 *
 * 设计原则：
 * - 所有读取函数对 null/缺失返回稳定值（null / [] / ''），避免调用方做大量空判断
 * - localName 比较优先于带前缀的 nodeName，兼容不同前缀（bpmn / bpmn2 / 默认命名空间）
 * - flowable:* 属性同时尝试 getAttributeNS、getAttribute('flowable:xxx')、getAttribute('xxx')
 *   三种路径，匹配 Flowable 实际 XML 输出可能的多种形态
 */

export const BPMN_NS = 'http://www.omg.org/spec/BPMN/20100524/MODEL'
export const BPMNDI_NS = 'http://www.omg.org/spec/BPMN/20100524/DI'
export const DC_NS = 'http://www.omg.org/spec/DD/20100524/DC'
export const DI_NS = 'http://www.omg.org/spec/DD/20100524/DI'
export const FLOWABLE_NS = 'http://flowable.org/bpmn'
export const XSI_NS = 'http://www.w3.org/2001/XMLSchema-instance'

/**
 * 解析 BPMN XML 字符串为 DOM Document。
 *
 * @param {string} xmlString
 * @returns {Document}
 * @throws {Error} XML 解析失败抛出，message 含 parsererror 内容
 */
export function parseBpmnXml(xmlString) {
  if (typeof xmlString !== 'string' || xmlString.trim() === '')
    throw new Error('parseBpmnXml: xmlString must be a non-empty string')

  const parser = new DOMParser()
  const doc = parser.parseFromString(xmlString, 'application/xml')

  // DOMParser 解析失败时会返回包含 <parsererror> 的文档（jsdom / 浏览器行为一致）
  const errorNode = doc.querySelector?.('parsererror')
  if (errorNode)
    throw new Error(`BPMN XML parse error: ${errorNode.textContent?.trim() || 'unknown'}`)

  return doc
}

/**
 * 序列化 DOM Document 回 XML 字符串。
 * 不强制添加 XML 声明，保持调用方对原始 XML 头的控制。
 */
export function serializeXml(doc) {
  if (!doc)
    return ''
  const serializer = new XMLSerializer()
  return serializer.serializeToString(doc)
}

/**
 * 取节点的 localName（去掉命名空间前缀）。
 * - DOM 在 XML 模式下 element.localName === 'userTask'
 * - 文本节点等没有 localName 时返回 ''
 */
export function getLocalName(node) {
  if (!node)
    return ''
  if (node.localName)
    return node.localName
  const name = node.nodeName || ''
  const idx = name.indexOf(':')
  return idx >= 0 ? name.slice(idx + 1) : name
}

/**
 * 在文档中找到根 process 元素。
 *
 * 策略：
 * 1. 优先查找 bpmn:process（按命名空间）
 * 2. 兜底按 localName 'process' 查找（兼容默认命名空间或非 bpmn 前缀）
 * 3. 如果 collaboration + 多 process，返回第一个 isExecutable=true 的 process；都没有则返回第一个
 *
 * @param {Document} doc
 * @returns {Element | null}
 */
export function getRootProcess(doc) {
  if (!doc)
    return null

  const processes = findElementsByLocalName(doc.documentElement || doc, 'process')
  if (processes.length === 0)
    return null
  if (processes.length === 1)
    return processes[0]

  const executable = processes.find(p => p.getAttribute('isExecutable') === 'true')
  return executable || processes[0]
}

/**
 * 查找指定根下的所有 localName 匹配元素（深度优先）。
 * 不区分命名空间前缀，兼容 bpmn:process / bpmn2:process / process 等不同输出。
 *
 * @param {Element | Document} root
 * @param {string} localName 不带前缀的标签名，例如 'sequenceFlow'
 * @returns {Element[]}
 */
export function findElementsByLocalName(root, localName) {
  if (!root || !localName)
    return []

  const result = []
  // 优先用 getElementsByTagNameNS('*', localName)，jsdom + 浏览器都支持，且复杂度 O(n)
  const fn = root.getElementsByTagNameNS?.bind(root)
  if (fn) {
    const list = fn('*', localName)
    for (let i = 0; i < list.length; i += 1)
      result.push(list[i])
    return result
  }

  // 兜底深度遍历
  const stack = [root]
  while (stack.length > 0) {
    const node = stack.pop()
    if (!node || node.nodeType !== 1)
      continue
    if (getLocalName(node) === localName)
      result.push(node)
    if (node.childNodes) {
      for (let i = node.childNodes.length - 1; i >= 0; i -= 1)
        stack.push(node.childNodes[i])
    }
  }
  return result
}

/**
 * 取一个元素的直接子元素（按 localName 过滤）。
 * 与 findElementsByLocalName 的区别：仅一层。
 *
 * @param {Element} element
 * @param {string} localName
 * @returns {Element[]}
 */
export function getChildren(element, localName) {
  if (!element || !element.childNodes)
    return []
  const out = []
  for (let i = 0; i < element.childNodes.length; i += 1) {
    const child = element.childNodes[i]
    if (child.nodeType === 1 && (!localName || getLocalName(child) === localName))
      out.push(child)
  }
  return out
}

/**
 * 取元素的第一个直接子元素（按 localName）。
 */
export function getChild(element, localName) {
  const list = getChildren(element, localName)
  return list[0] || null
}

/**
 * 读取普通属性（无命名空间）。空字符串与缺失统一返回 null。
 */
export function getAttr(element, name) {
  if (!element || typeof element.getAttribute !== 'function')
    return null
  if (!element.hasAttribute?.(name))
    return null
  const v = element.getAttribute(name)
  return v == null || v === '' ? null : v
}

/**
 * 读取 flowable:* 命名空间属性。
 *
 * 兼容三种路径，按顺序回退：
 * 1. getAttributeNS(FLOWABLE_NS, name)        — 标准命名空间感知
 * 2. getAttribute('flowable:' + name)         — 字面前缀（jsdom 在 XML 模式下也常见）
 * 3. getAttribute(name)                       — 极少数 XML 命名空间被默认化场景
 *
 * @returns {string | null}
 */
export function getFlowableAttr(element, name) {
  if (!element || !name)
    return null

  if (typeof element.getAttributeNS === 'function') {
    const ns = element.getAttributeNS(FLOWABLE_NS, name)
    if (ns != null && ns !== '')
      return ns
  }
  const prefixed = element.getAttribute?.(`flowable:${name}`)
  if (prefixed != null && prefixed !== '')
    return prefixed

  // 极少数情况下命名空间被默认化（xmlns="http://flowable.org/bpmn"），裸属性也算
  const bare = element.getAttribute?.(name)
  if (bare != null && bare !== '')
    return bare

  return null
}

/**
 * 把 flowable 属性当成布尔值读取。'true' / 'TRUE' / true → true；缺失或其他 → false。
 */
export function getFlowableBoolAttr(element, name) {
  const v = getFlowableAttr(element, name)
  if (v == null)
    return false
  return String(v).toLowerCase() === 'true'
}

/**
 * 在元素的 <extensionElements> 子元素下查找指定 localName 的第一个扩展元素。
 *
 * @param {Element} element
 * @param {string} localName 不带前缀的扩展元素名，例如 'taskListener' / 'executionListener'
 * @returns {Element | null}
 */
export function getExtensionElement(element, localName) {
  const list = getExtensionElements(element, localName)
  return list[0] || null
}

/**
 * 列出元素的所有扩展元素（可按 localName 过滤）。
 *
 * @param {Element} element
 * @param {string} [localName] 可选，只返回该 localName 的扩展子节点
 * @returns {Element[]}
 */
export function getExtensionElements(element, localName) {
  if (!element)
    return []
  const ext = getChild(element, 'extensionElements')
  if (!ext)
    return []
  return getChildren(ext, localName)
}

/**
 * 读取元素的文本子节点（去除两端空白）。
 * 用于解析 <bpmn:completionCondition>${...}</bpmn:completionCondition> 这类内容。
 */
export function getTextContent(element) {
  if (!element)
    return ''
  return (element.textContent || '').trim()
}

/**
 * 读取元素的 documentation 文本（BPMN 标准的 bpmn:documentation 子节点）。
 */
export function getDocumentation(element) {
  const doc = getChild(element, 'documentation')
  return doc ? getTextContent(doc) : ''
}
