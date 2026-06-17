/**
 * 多实例（会签）completionCondition 表达式构造器。
 *
 * 与 user-task-parser.js#parseCompletionExpression 对偶：
 *   all   → ${nrOfCompletedInstances/nrOfInstances == 1}
 *   any   → ${nrOfCompletedInstances >= 1}
 *   ratio → ${nrOfCompletedInstances/nrOfInstances >= 0.N}
 *
 * 默认（未知 condition）按 all 输出，与现有 NodePropertiesPanel 兼容。
 */

const DOLLAR = '$'

export function buildCompletionExpression(condition, passRate) {
  const c = String(condition || 'all').toLowerCase()
  if (c === 'any')
    return `${DOLLAR}{nrOfCompletedInstances >= 1}`

  if (c === 'ratio') {
    const n = Number(passRate)
    if (!Number.isFinite(n) || n <= 0 || n >= 100)
      return `${DOLLAR}{nrOfCompletedInstances/nrOfInstances == 1}` // 兜底 all
    const decimal = (n / 100).toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
    return `${DOLLAR}{nrOfCompletedInstances/nrOfInstances >= ${decimal}}`
  }

  // all / 默认
  return `${DOLLAR}{nrOfCompletedInstances/nrOfInstances == 1}`
}
