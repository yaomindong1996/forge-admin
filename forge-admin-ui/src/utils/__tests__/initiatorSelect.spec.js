import { describe, expect, it } from 'vitest'
import { collectInitiatorSelectSelections, normalizeInitiatorSelectIds } from '../initiatorSelect.js'

describe('normalizeInitiatorSelectIds', () => {
  it('单选字符串转成数组', () => {
    expect(normalizeInitiatorSelectIds('1001')).toEqual(['1001'])
  })

  it('会签数组去重', () => {
    expect(normalizeInitiatorSelectIds(['1001', '1001', '1002'])).toEqual(['1001', '1002'])
  })

  it('多选人员选择器的逗号拼接值拆成多个审批人', () => {
    expect(normalizeInitiatorSelectIds('1,70')).toEqual(['1', '70'])
    expect(normalizeInitiatorSelectIds(' 1, ,70,1 ')).toEqual(['1', '70'])
  })
})

describe('collectInitiatorSelectSelections', () => {
  it('不会签节点只保留第一人', () => {
    expect(collectInitiatorSelectSelections(
      [{ nodeKey: 'Node_2', nodeName: '审批人2', multiple: false }],
      { Node_2: '2090' },
    )).toEqual({ Node_2: ['2090'] })
  })

  it('未选择时抛出可读错误', () => {
    expect(() => collectInitiatorSelectSelections(
      [{ nodeKey: 'Node_2', nodeName: '审批人2', multiple: false }],
      { Node_2: '' },
    )).toThrow('请选择「审批人2」的审批人')
  })
})
