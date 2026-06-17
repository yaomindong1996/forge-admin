import { describe, expect, it } from 'vitest'
import { buildCompletionExpression } from '../completion-condition.js'
import { parseCompletionExpression } from '../user-task-parser.js'

const DOLLAR = '$'

describe('buildCompletionExpression', () => {
  it('all → ${nrOfCompletedInstances/nrOfInstances == 1}', () => {
    expect(buildCompletionExpression('all'))
      .toBe(`${DOLLAR}{nrOfCompletedInstances/nrOfInstances == 1}`)
  })
  it('any → ${nrOfCompletedInstances >= 1}', () => {
    expect(buildCompletionExpression('any'))
      .toBe(`${DOLLAR}{nrOfCompletedInstances >= 1}`)
  })
  it('ratio 60% → 0.6', () => {
    expect(buildCompletionExpression('ratio', 60))
      .toBe(`${DOLLAR}{nrOfCompletedInstances/nrOfInstances >= 0.6}`)
  })
  it('ratio 75% → 0.75', () => {
    expect(buildCompletionExpression('ratio', 75))
      .toBe(`${DOLLAR}{nrOfCompletedInstances/nrOfInstances >= 0.75}`)
  })
  it('ratio 越界 → 兜底 all', () => {
    expect(buildCompletionExpression('ratio', 0))
      .toBe(`${DOLLAR}{nrOfCompletedInstances/nrOfInstances == 1}`)
    expect(buildCompletionExpression('ratio', 100))
      .toBe(`${DOLLAR}{nrOfCompletedInstances/nrOfInstances == 1}`)
  })
})

describe('completion 双向往返', () => {
  it('all/any/ratio 60/ratio 75 解析后再生成结果一致', () => {
    const cases = [
      { c: 'all', r: 100 },
      { c: 'any', r: null },
      { c: 'ratio', r: 60 },
      { c: 'ratio', r: 75 },
    ]
    for (const { c, r } of cases) {
      const expr = buildCompletionExpression(c, r)
      const parsed = parseCompletionExpression(expr)
      expect(parsed.condition).toBe(c)
      if (r != null)
        expect(parsed.passRate).toBe(r)
    }
  })
})
