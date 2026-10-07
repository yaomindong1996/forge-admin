import { requireCondition } from './paths.mjs'

const tokenPattern = /<!--[\s\S]*?-->|<\?[\s\S]*?\?>|<\/?[A-Za-z_][\w.-]*(?:\s+[^<>]*?)?\s*\/?>|[^<]+/g

export function parsePom(text) {
  requireCondition(typeof text === 'string' && Buffer.byteLength(text) <= 2 * 1024 * 1024, 'POM 超过限制')
  requireCondition(!/<!DOCTYPE|<!ENTITY|<!\[CDATA\[/i.test(text), 'POM 不接受 DTD/实体/CDATA')
  const tokens = [...text.matchAll(tokenPattern)]
  requireCondition(tokens.map(match => match[0]).join('') === text, 'POM XML 语法不支持或损坏')
  const document = { name: '#document', children: [], text: '', comments: [] }
  const stack = [document]
  for (const token of tokens) {
    readToken(token, stack)
    requireCondition(stack.length <= 64, 'POM 嵌套超过限制')
  }
  requireCondition(stack.length === 1 && document.children.length === 1, 'POM XML 未闭合或根节点重复')
  const root = document.children[0]
  requireCondition(root.name === 'project' && !document.text.trim(), 'POM 根节点必须是 project')
  return root
}

function readToken(match, stack) {
  const token = match[0]
  const parent = stack.at(-1)
  if (token.startsWith('<!--')) {
    requireCondition(!token.slice(4, -3).includes('--'), 'POM 注释非法')
    parent.comments.push({ text: token.slice(4, -3).trim(), start: match.index,
      end: match.index + token.length })
  }
  else if (token.startsWith('<?')) {
    requireCondition(parent.name === '#document' && token.startsWith('<?xml '), 'POM 处理指令非法')
  }
  else if (token.startsWith('</')) {
    const name = token.slice(2, -1).trim()
    requireCondition(stack.length > 1 && parent.name === name, 'POM XML 标签不匹配')
    parent.closeStart = match.index
    parent.end = match.index + token.length
    stack.pop()
  }
  else if (token.startsWith('<')) {
    const name = token.match(/^<([\w.-]+)/)[1]
    const attributes = token.slice(name.length + 1).replace(/\/?>(?:\s*)$/, '')
    requireCondition(!attributes.replace(/\s+[\w:.-]+\s*=\s*(?:"[^"<]*"|'[^'<]*')/g, '').trim(),
      'POM 属性语法非法')
    const node = { name, children: [], text: '', comments: [], start: match.index,
      openEnd: match.index + token.length, end: match.index + token.length }
    parent.children.push(node)
    if (!token.endsWith('/>')) {
      stack.push(node)
    }
  }
  else {
    requireCondition(!/&(?!(?:amp|lt|gt|apos|quot|#\d+|#x[\da-f]+);)/i.test(token), 'POM 实体非法')
    parent.text += token
  }
}

export function child(node, name, required = true) {
  const values = node.children.filter(item => item.name === name)
  requireCondition(values.length <= 1 && (!required || values.length === 1), `POM ${name} 缺失或重复`)
  return values[0]
}

export function field(node, name, required = true) {
  const value = child(node, name, required)
  requireCondition(!value || value.children.length === 0, `POM ${name} 不是文本字段`)
  return value?.text.trim()
}

export function replaceNodeText(text, node, value) {
  requireCondition(node.closeStart !== undefined && !node.children.length, 'POM 文本节点非法')
  return text.slice(0, node.openEnd) + value + text.slice(node.closeStart)
}
