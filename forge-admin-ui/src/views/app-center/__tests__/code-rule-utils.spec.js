import { describe, expect, it } from 'vitest'
import {
  applyLowCodeVariableMapping,
  buildCodeRulePreviewPayload,
  changeCodeRuleSegmentType,
  changeCodeRuleVariableSource,
  createCodeRuleSegment,
  createLatestRequestGuard,
  hasCodeRulePermission,
  normalizeCodeRuleSegments,
  validateCodeRuleDraft,
} from '../code-rule-utils'

describe('code rule utilities', () => {
  it('creates fixed-width sequence defaults', () => {
    const segment = createCodeRuleSegment('SEQ', 2)
    expect(segment).toMatchObject({
      segmentOrder: 2,
      segmentType: 'SEQ',
      segmentLength: 4,
      radixType: 'DECIMAL',
      resetPolicy: 'DAY',
      startValue: 1,
      padDirection: 'LEFT',
    })
  })

  it('keeps the stable key while clearing sequence-only properties', () => {
    const sequence = createCodeRuleSegment('SEQ', 1)
    const fixed = changeCodeRuleSegmentType(sequence, 'FIXED')
    expect(fixed.segmentKey).toBe(sequence.segmentKey)
    expect(fixed.segmentType).toBe('FIXED')
    expect(fixed.radixType).toBeNull()
    expect(fixed.resetEnabled).toBe(0)
  })

  it('normalizes display order without changing stable keys', () => {
    const first = createCodeRuleSegment('FIXED', 4)
    const second = createCodeRuleSegment('DATE', 2)
    const result = normalizeCodeRuleSegments([first, second])
    expect(result.map(item => item.segmentKey)).toEqual([second.segmentKey, first.segmentKey])
    expect(result.map(item => item.segmentOrder)).toEqual([1, 2])
  })

  it('rejects multiple sequence segments and overlong output', () => {
    const result = validateCodeRuleDraft({
      ruleCode: 'order_no',
      ruleName: '订单编号',
      category: 'DOCUMENT',
      segments: [
        { ...createCodeRuleSegment('SEQ', 1), segmentLength: 60 },
        { ...createCodeRuleSegment('SEQ', 2), segmentLength: 60 },
      ],
    })
    expect(result.valid).toBe(false)
    expect(result.errors).toContain('一条规则最多只能包含一个流水号段')
    expect(result.errors).toContain('编码声明总长度不能超过 96 个字符')
  })

  it('builds preview payload with normalized fields and segments', () => {
    const segment = createCodeRuleSegment('VARIABLE', 3)
    segment.segmentValue = 'warehouseCode'
    const payload = buildCodeRulePreviewPayload({
      ruleCode: 'warehouse_no',
      ruleName: '仓库编号',
      category: 'WAREHOUSE',
      scene: 'COMMON',
      sourceObjectId: '10001',
      sampleSequence: 12,
      segments: [segment],
    }, { warehouseCode: 'WH1' })
    expect(payload.sequence).toBe(12)
    expect(payload.fields).toEqual({ warehouseCode: 'WH1' })
    expect(payload.segments[0].segmentOrder).toBe(1)
  })

  it('accepts a single-letter rule code', () => {
    const result = validateCodeRuleDraft({
      ruleCode: 'A',
      ruleName: '单字符规则编码',
      category: 'COMMON',
      scene: 'COMMON',
      segments: [createCodeRuleSegment('SEQ', 1)],
    })
    expect(result.valid).toBe(true)
  })

  it('requires a low-code business object for variable segments', () => {
    const variable = createCodeRuleSegment('VARIABLE', 1)
    variable.variableSource = 'LOWCODE'
    variable.segmentValue = 'warehouseCode'
    const result = validateCodeRuleDraft({
      ruleCode: 'warehouse_code',
      ruleName: '仓库编码',
      category: 'WAREHOUSE',
      scene: 'COMMON',
      segments: [variable],
    })

    expect(result.valid).toBe(false)
    expect(result.errors).toContain('业务变量段必须选择字段来源业务对象')
  })

  it('accepts a custom variable without a low-code object', () => {
    const variable = createCodeRuleSegment('VARIABLE', 1)
    variable.segmentValue = 'customerType'
    const result = validateCodeRuleDraft({
      ruleCode: 'customer_code',
      ruleName: '客户编码',
      category: 'CUSTOMER',
      scene: 'COMMON',
      segments: [variable],
    })

    expect(variable.variableSource).toBe('CUSTOM')
    expect(result.valid).toBe(true)
  })

  it('clears an incompatible variable value when switching its source', () => {
    const custom = createCodeRuleSegment('VARIABLE', 1)
    custom.segmentValue = 'customerType'
    const lowCode = changeCodeRuleVariableSource(custom, 'LOWCODE')

    expect(lowCode.variableSource).toBe('LOWCODE')
    expect(lowCode.segmentValue).toBeNull()
    expect(changeCodeRuleVariableSource(lowCode, 'CUSTOM')).toMatchObject({
      variableSource: 'CUSTOM',
      segmentValue: null,
    })
  })

  it('updates only the target mapping when the low-code object is unchanged', () => {
    const target = { ...createCodeRuleSegment('VARIABLE', 1), variableSource: 'LOWCODE', segmentValue: 'oldCode' }
    const sibling = { ...createCodeRuleSegment('VARIABLE', 2), variableSource: 'LOWCODE', segmentValue: 'warehouseType' }

    const result = applyLowCodeVariableMapping(
      [target, sibling],
      target.segmentKey,
      { sourceObjectId: '10001', fieldCode: 'warehouseCode' },
      '10001',
    )

    expect(result.objectChanged).toBe(false)
    expect(result.clearedSegmentKeys).toEqual([])
    expect(result.segments.map(segment => segment.segmentValue)).toEqual(['warehouseCode', 'warehouseType'])
  })

  it('clears other low-code mappings when changing object and preserves custom variables', () => {
    const target = { ...createCodeRuleSegment('VARIABLE', 1), segmentValue: 'externalType' }
    const sibling = { ...createCodeRuleSegment('VARIABLE', 2), variableSource: 'LOWCODE', segmentValue: 'warehouseType' }
    const custom = { ...createCodeRuleSegment('VARIABLE', 3), segmentValue: 'customerLevel' }

    const result = applyLowCodeVariableMapping(
      [target, sibling, custom],
      target.segmentKey,
      { sourceObjectId: 20002, fieldCode: 'customerCode' },
      '10001',
    )

    expect(result.sourceObjectId).toBe('20002')
    expect(result.objectChanged).toBe(true)
    expect(result.clearedSegmentKeys).toEqual([sibling.segmentKey])
    expect(result.segments).toEqual(expect.arrayContaining([
      expect.objectContaining({ segmentKey: target.segmentKey, variableSource: 'LOWCODE', segmentValue: 'customerCode' }),
      expect.objectContaining({ segmentKey: sibling.segmentKey, variableSource: 'LOWCODE', segmentValue: null }),
      expect.objectContaining({ segmentKey: custom.segmentKey, variableSource: 'CUSTOM', segmentValue: 'customerLevel' }),
    ]))
  })

  it('ignores expired asynchronous responses', () => {
    const guard = createLatestRequestGuard()
    const first = guard.begin()
    const second = guard.begin()

    expect(guard.isLatest(first)).toBe(false)
    expect(guard.isLatest(second)).toBe(true)
    guard.invalidate()
    expect(guard.isLatest(second)).toBe(false)
  })

  it('checks independent code-rule permissions and wildcard grants', () => {
    expect(hasCodeRulePermission({ permissions: ['system:codeRule:add'] }, 'system:codeRule:add')).toBe(true)
    expect(hasCodeRulePermission({ permissions: ['system:codeRule:list'] }, 'system:codeRule:add')).toBe(false)
    expect(hasCodeRulePermission({ apiPermissions: ['*:*:*'] }, 'system:codeRule:remove')).toBe(true)
    expect(hasCodeRulePermission({ isAdmin: true }, 'system:codeRule:edit')).toBe(true)
  })
})
