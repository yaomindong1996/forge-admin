import { amountToChinese } from './amountChinese'
import { PrintError } from './types'

export const EXPRESSION_LIMITS = Object.freeze({
  length: 500,
  nodes: 200,
  depth: 24,
})

const FUNCTIONS = new Set(['SUM', 'AVG', 'COUNT', 'MIN', 'MAX', 'ABS', 'ROUND', 'IF', 'CONCAT', 'TEXT', 'MONEY', 'UPPER', 'RMB'])
const AGGREGATES = new Set(['SUM', 'AVG', 'COUNT', 'MIN', 'MAX'])
const IDENTIFIER = /^[a-z_]\w*$/i
const FIELD_PATH = /^[a-z_$][\w$]*(?:\.[a-z_$][\w$]*)+$/i
const FORBIDDEN_NAMES = new Set(['eval', 'Function', 'constructor', 'prototype', '__proto__', 'window', 'document', 'globalThis', 'this', 'import', 'export', 'class', 'new', 'with'])

function fail(message, path = '') {
  throw new PrintError('INVALID_EXPRESSION', message, path)
}

function tokenize(source) {
  const tokens = []
  let i = 0
  const push = (type, value) => tokens.push({ type, value })
  while (i < source.length) {
    const ch = source[i]
    if (/\s/.test(ch)) {
      i++
      continue
    }
    if ('+-*/%(),'.includes(ch)) {
      push(ch, ch)
      i++
      continue
    }
    if (ch === '{' || ch === '}') {
      push(ch, ch)
      i++
      continue
    }
    if (ch === '"' || ch === '\'') {
      const quote = ch
      let text = ''
      i++
      while (i < source.length && source[i] !== quote) {
        if (source[i] === '\\' || source[i] === '\n')
          fail('表达式字符串不能转义或换行')
        text += source[i++]
        if (text.length > 200)
          fail('表达式字符串过长')
      }
      if (source[i] !== quote)
        fail('表达式字符串未闭合')
      i++
      push('STRING', text)
      continue
    }
    const two = source.slice(i, i + 2)
    if (['==', '!=', '>=', '<=', '&&', '||'].includes(two)) {
      push(two, two)
      i += 2
      continue
    }
    if ('<>=!'.includes(ch)) {
      push(ch, ch)
      i++
      continue
    }
    if (/\d/.test(ch)) {
      const match = /^\d+(?:\.\d+)?/.exec(source.slice(i))
      if (!match || /e/i.test(match[0]))
        fail('表达式数字必须是普通十进制')
      push('NUMBER', match[0])
      i += match[0].length
      continue
    }
    if (/[a-z_]/i.test(ch)) {
      const match = /^[a-z_]\w*(?:\.[a-z_]\w*)*/i.exec(source.slice(i))
      push('IDENT', match[0])
      i += match[0].length
      continue
    }
    fail('表达式含有不支持的字符')
  }
  push('EOF', '')
  return tokens
}

function parseTokens(tokens) {
  let index = 0
  let nodes = 0
  const peek = () => tokens[index]
  const eat = (type) => {
    if (peek().type !== type)
      fail('表达式语法无效')
    return tokens[index++]
  }
  const node = (value) => {
    nodes++
    if (nodes > EXPRESSION_LIMITS.nodes)
      fail('表达式过于复杂')
    return value
  }
  function expression(depth) {
    if (depth > EXPRESSION_LIMITS.depth)
      fail('表达式嵌套过深')
    return or(depth)
  }
  function binary(next, depth, operators) {
    let left = next(depth)
    while (operators.includes(peek().type)) {
      const op = eat(peek().type).value
      left = node({ type: 'binary', op, left, right: next(depth) })
    }
    return left
  }
  function or(depth) {
    return binary(and, depth, ['||'])
  }
  function and(depth) {
    return binary(compare, depth, ['&&'])
  }
  function compare(depth) {
    return binary(add, depth, ['==', '!=', '>=', '<=', '>', '<'])
  }
  function add(depth) {
    return binary(mul, depth, ['+', '-'])
  }
  function mul(depth) {
    return binary(unary, depth, ['*', '/', '%'])
  }
  function unary(depth) {
    if (peek().type === '-') {
      eat('-')
      return node({ type: 'unary', op: '-', value: unary(depth) })
    }
    return primary(depth)
  }
  function primary(depth) {
    const token = peek()
    if (token.type === 'NUMBER') {
      eat('NUMBER')
      return node({ type: 'number', value: token.value })
    }
    if (token.type === 'STRING') {
      eat('STRING')
      return node({ type: 'string', value: token.value })
    }
    if (token.type === '(') {
      eat('(')
      const value = expression(depth + 1)
      eat(')')
      return value
    }
    if (token.type === 'IDENT') {
      const name = eat('IDENT').value
      if (peek().type === '(') {
        const fn = name.toUpperCase()
        if (!FUNCTIONS.has(fn) || name !== fn)
          fail(`不支持函数 ${name}`)
        eat('(')
        const args = []
        if (peek().type !== ')') {
          args.push(expression(depth + 1))
          while (peek().type === ',') {
            eat(',')
            args.push(expression(depth + 1))
          }
        }
        eat(')')
        return node({ type: 'call', name: fn, args })
      }
      if (FORBIDDEN_NAMES.has(name.split('.')[0]) || FORBIDDEN_NAMES.has(name.split('.').at(-1)))
        fail('表达式不能访问保留名')
      if (name.includes('.') && !FIELD_PATH.test(name))
        fail('字段路径无效')
      if (!name.includes('.') && !IDENTIFIER.test(name))
        fail('标识符无效')
      return node({ type: 'path', path: name })
    }
    fail('表达式语法无效')
  }
  const ast = expression(0)
  return ast
}

export function parseExpression(source) {
  if (typeof source !== 'string' || !source.trim() || source.length > EXPRESSION_LIMITS.length)
    fail('表达式必须是不超过 500 字的文本')
  for (let i = 0; i < source.length; i++) {
    const code = source.charCodeAt(i)
    if (code < 32 && code !== 9 && code !== 10 && code !== 13)
      fail('表达式含有非法控制字符')
  }
  const ast = parseTokens(tokenize(source))
  return ast
}

export function parseExpressionTemplate(source) {
  if (typeof source !== 'string' || source.length > EXPRESSION_LIMITS.length)
    fail('表达式必须是不超过 500 字的文本')
  if (!source.includes('{'))
    return { type: 'expr', ast: parseExpression(source) }
  const parts = []
  let last = 0
  for (const match of source.matchAll(/\{([^{}]+)\}/g)) {
    if (match.index > last)
      parts.push({ type: 'text', value: source.slice(last, match.index) })
    parts.push({ type: 'expr', ast: parseExpression(match[1]) })
    last = match.index + match[0].length
  }
  if (last < source.length)
    parts.push({ type: 'text', value: source.slice(last) })
  if (!parts.length)
    fail('表达式为空')
  return { type: 'template', parts }
}

function asNumber(value, message = '表达式期望数字') {
  if (value === null || value === undefined || value === '')
    return 0
  if (typeof value === 'boolean')
    return value ? 1 : 0
  if (typeof value === 'number') {
    if (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER)
      throw new PrintError('UNSAFE_NUMBER', '表达式数字超出安全范围')
    if (Number.isInteger(value) && !Number.isSafeInteger(value))
      throw new PrintError('UNSAFE_NUMBER', '表达式数字超出安全范围')
    return value
  }
  const match = /^-?\d+(?:\.\d+)?$/.exec(String(value).trim())
  if (!match)
    throw new PrintError('INVALID_NUMBER', message)
  const number = Number(match[0])
  if (!Number.isFinite(number))
    throw new PrintError('UNSAFE_NUMBER', '表达式数字超出安全范围')
  return number
}

function asText(value) {
  if (value === null || value === undefined)
    return ''
  if (['string', 'number', 'boolean'].includes(typeof value))
    return String(value)
  throw new PrintError('EXPECTED_SCALAR', '表达式只能产生文本或数字')
}

function truthy(value) {
  return !(value === null || value === undefined || value === '' || value === 0 || value === false)
}

function readPath(context, path) {
  return path.split('.').reduce((current, key) => {
    if (!current || typeof current !== 'object' || Array.isArray(current) || !Object.hasOwn(current, key))
      return null
    const descriptor = Object.getOwnPropertyDescriptor(current, key)
    if (descriptor.get || descriptor.set)
      throw new PrintError('INVALID_CONTEXT', '打印数据必须是普通 JSON', path)
    return descriptor.value ?? null
  }, context)
}

function rowsOf(context) {
  const rows = context?.rows
  if (!Array.isArray(rows))
    return []
  return rows.filter(row => row && typeof row === 'object' && !Array.isArray(row))
}

function decimalMoney(cents) {
  const amount = asNumber(cents, '金额须为数字')
  if (!Number.isInteger(amount))
    throw new PrintError('INVALID_NUMBER', '金额须为整数分')
  const sign = amount < 0 ? '-' : ''
  const abs = Math.abs(amount)
  const yuan = String(Math.floor(abs / 100))
  const fraction = String(abs % 100).padStart(2, '0')
  return `${sign}${yuan}.${fraction}`
}

function evaluateAst(ast, context) {
  switch (ast.type) {
    case 'number':
      return asNumber(ast.value)
    case 'string':
      return ast.value
    case 'path':
      return readPath(context, ast.path)
    case 'unary':
      return -asNumber(evaluateAst(ast.value, context))
    case 'binary': {
      if (ast.op === '&&')
        return truthy(evaluateAst(ast.left, context)) ? evaluateAst(ast.right, context) : false
      if (ast.op === '||') {
        const left = evaluateAst(ast.left, context)
        return truthy(left) ? left : evaluateAst(ast.right, context)
      }
      const left = evaluateAst(ast.left, context)
      const right = evaluateAst(ast.right, context)
      if (ast.op === '+') {
        if (typeof left === 'string' || typeof right === 'string')
          return asText(left) + asText(right)
        return asNumber(left) + asNumber(right)
      }
      const a = asNumber(left)
      const b = asNumber(right)
      switch (ast.op) {
        case '-': return a - b
        case '*': return a * b
        case '/':
          if (b === 0)
            throw new PrintError('INVALID_NUMBER', '表达式除数不能为 0')
          return a / b
        case '%':
          if (b === 0)
            throw new PrintError('INVALID_NUMBER', '表达式除数不能为 0')
          return a % b
        case '==': return Object.is(left, right) || asText(left) === asText(right)
        case '!=': return !(Object.is(left, right) || asText(left) === asText(right))
        case '>': return a > b
        case '<': return a < b
        case '>=': return a >= b
        case '<=': return a <= b
        default: fail('不支持此运算符')
      }
    }
    case 'call':
      return evaluateCall(ast, context)
    default:
      fail('表达式语法无效')
  }
}

function rowValues(ast, context) {
  return rowsOf(context).map((row) => {
    const scoped = Object.assign(Object.create(null), context, row, { row })
    return evaluateAst(ast, scoped)
  })
}

function evaluateCall(ast, context) {
  const args = ast.args
  if (AGGREGATES.has(ast.name)) {
    if (ast.name === 'COUNT' && args.length === 0)
      return rowsOf(context).length
    if (args.length !== 1)
      fail(`${ast.name} 只接受一个参数`)
    const values = rowValues(args[0], context).filter(value => value !== null && value !== undefined && value !== '')
    if (ast.name === 'COUNT')
      return values.length
    const numbers = values.map(value => asNumber(value))
    if (!numbers.length)
      return 0
    if (ast.name === 'SUM')
      return numbers.reduce((sum, value) => sum + value, 0)
    if (ast.name === 'AVG')
      return numbers.reduce((sum, value) => sum + value, 0) / numbers.length
    if (ast.name === 'MIN')
      return Math.min(...numbers)
    return Math.max(...numbers)
  }
  const values = args.map(arg => evaluateAst(arg, context))
  switch (ast.name) {
    case 'ABS':
      return Math.abs(asNumber(values[0]))
    case 'ROUND': {
      const digits = args[1] === undefined ? 0 : asNumber(values[1])
      if (!Number.isInteger(digits) || digits < 0 || digits > 6)
        fail('ROUND 精度须为 0 至 6 的整数')
      const factor = 10 ** digits
      return Math.round(asNumber(values[0]) * factor) / factor
    }
    case 'IF':
      if (args.length !== 3)
        fail('IF 需要三个参数')
      return truthy(values[0]) ? values[1] : values[2]
    case 'CONCAT':
      return values.map(asText).join('')
    case 'TEXT':
      return asText(values[0])
    case 'MONEY':
      return decimalMoney(values[0])
    case 'UPPER':
      return amountToChinese(asNumber(values[0]))
    case 'RMB':
      return amountToChinese(decimalMoney(values[0]))
    default:
      fail(`不支持函数 ${ast.name}`)
  }
}

export function evaluateExpression(source, context) {
  const parsed = parseExpressionTemplate(source)
  if (parsed.type === 'expr')
    return evaluateAst(parsed.ast, context)
  return parsed.parts.map(part => part.type === 'text' ? part.value : asText(evaluateAst(part.ast, context))).join('')
}

export function collectExpressionFieldPaths(source) {
  const paths = new Set()
  function walk(ast) {
    if (!ast || typeof ast !== 'object')
      return
    if (ast.type === 'path' && ast.path.includes('.'))
      paths.add(ast.path)
    if (ast.type === 'call')
      ast.args.forEach(walk)
    if (ast.left)
      walk(ast.left)
    if (ast.right)
      walk(ast.right)
    if (ast.value && typeof ast.value === 'object')
      walk(ast.value)
  }
  const parsed = parseExpressionTemplate(source)
  if (parsed.type === 'expr')
    walk(parsed.ast)
  else
    parsed.parts.forEach(part => part.ast && walk(part.ast))
  return [...paths]
}
