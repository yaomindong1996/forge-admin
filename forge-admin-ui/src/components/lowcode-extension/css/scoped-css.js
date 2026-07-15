import { generate, parse, walk } from 'css-tree'

const SAFE_AT_RULES = new Set(['media', 'supports', 'container', 'layer'])
const PLATFORM_SELECTOR_PATTERN = /forge[-_]?layout|layout[-_]?(?:sider|sidebar)|app[-_]?(?:sider|sidebar)|#app\b|\.n-layout-sider\b/i
const GLOBAL_SELECTOR_PATTERN = /(?:^|[\s>+~,(])(?:html|body|:root)(?=$|[\s>+~,.#:[\])])/i
const ESCAPE_SELECTOR_PATTERN = /:(?:global|host)|::(?:part|slotted)|\[data-forge-(?:app|page)/i
const SAFE_SCOPE_CODE = /^[\w-]{1,128}$/

export function processScopedCss(source, scope) {
  const css = String(source || '')
  const scopeSelector = createScopeSelector(scope)
  let ast
  try {
    ast = parse(css, {
      context: 'stylesheet',
      positions: false,
      parseCustomProperty: true,
    })
  }
  catch (error) {
    throw new Error(`CSS 语法错误: ${error.message}`)
  }

  let selectorCount = 0
  walk(ast, {
    enter(node) {
      if (node.type === 'Atrule') {
        const name = String(node.name || '').toLowerCase()
        if (!SAFE_AT_RULES.has(name))
          throw new Error(`不允许使用 @${name || 'unknown'} 规则`)
      }
      if (node.type === 'Url')
        throw new Error('不允许在扩展样式中使用外部或内联 URL')
      if (node.type === 'Function' && ['expression', 'url'].includes(String(node.name || '').toLowerCase()))
        throw new Error(`不允许使用 CSS ${node.name}()`)
      if (node.type === 'Declaration' && ['behavior', '-moz-binding'].includes(String(node.property || '').toLowerCase()))
        throw new Error(`不允许使用 CSS 属性 ${node.property}`)
      if (node.type === 'Declaration' && String(node.property || '').toLowerCase() === 'position'
        && generate(node.value).trim().toLowerCase() === 'fixed') {
        throw new Error('不允许使用 position: fixed 覆盖平台界面')
      }
      if (node.type === 'Declaration' && String(node.property || '').toLowerCase() === 'z-index') {
        const zIndex = Number(generate(node.value).trim())
        if (Number.isFinite(zIndex) && zIndex > 100) {
          throw new Error('扩展样式 z-index 不能超过100')
        }
      }
      if (node.type !== 'Rule' || node.prelude?.type !== 'SelectorList')
        return

      const scopedSelectors = []
      node.prelude.children.forEach((selectorNode) => {
        const selector = generate(selectorNode)
        validateSelector(selector)
        scopedSelectors.push(`${scopeSelector} ${selector}`)
        selectorCount += 1
      })
      node.prelude = parse(scopedSelectors.join(','), { context: 'selectorList', positions: false })
    },
  })

  if (selectorCount === 0 && css.trim())
    throw new Error('扩展样式至少需要一个普通选择器')

  return {
    css: generate(ast),
    scopeSelector,
    selectorCount,
  }
}

function createScopeSelector(scope = {}) {
  const applicationCode = String(scope.applicationCode || '').trim()
  const pageCode = String(scope.pageCode || '').trim()
  if (!SAFE_SCOPE_CODE.test(applicationCode) || !SAFE_SCOPE_CODE.test(pageCode))
    throw new Error('应用编码和页面编码只能包含字母、数字、下划线或短横线')
  return `[data-forge-app="${applicationCode}"][data-forge-page="${pageCode}"]`
}

function validateSelector(selector) {
  if (GLOBAL_SELECTOR_PATTERN.test(selector))
    throw new Error(`不允许覆盖 html/body/:root: ${selector}`)
  if (PLATFORM_SELECTOR_PATTERN.test(selector))
    throw new Error(`不允许覆盖 Forge 布局或侧边栏: ${selector}`)
  if (ESCAPE_SELECTOR_PATTERN.test(selector))
    throw new Error(`选择器试图逃逸扩展作用域: ${selector}`)
}
