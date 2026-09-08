import { describe, expect, it } from 'vitest'
import {
  buildHttpSendExample,
  buildJavaSendExample,
  describeJumpResult,
  isBuiltInFlowTodo,
  normalizeBizTypeCode,
  previewJumpUrl,
  resolveJumpPreset,
} from '../biz-type-usage'

describe('message biz type usage helpers', () => {
  it('把跳转模板换成业务人员能看懂的实际地址', () => {
    const keySlot = '$' + '{bizKey}'
    expect(previewJumpUrl(`/order/detail?id=${keySlot}`, 'PO88')).toBe('/order/detail?id=PO88')
    expect(resolveJumpPreset(`/flow/todo?taskId=${keySlot}`)).toBe('todo')
    expect(resolveJumpPreset('')).toBe('none')
    expect(resolveJumpPreset(`/leave/list?id=${keySlot}`)).toBe('custom')
    expect(describeJumpResult('', '_self', 'PO88')).toContain('不会跳到别的页面')
    expect(describeJumpResult(`/order/detail?id=${keySlot}`, '_blank', 'PO88')).toContain('新窗口打开')
  })

  it('代码示例带上当前业务编码', () => {
    expect(normalizeBizTypeCode(' purchase order ')).toBe('PURCHASE_ORDER')
    expect(isBuiltInFlowTodo('flow_todo')).toBe(true)
    const java = buildJavaSendExample({ bizType: 'PURCHASE_ORDER', bizName: '采购审批', sampleKey: 'PO88' })
    expect(java).toContain('req.setBizType("PURCHASE_ORDER")')
    expect(java).toContain('markWebReadByBiz("PURCHASE_ORDER", "PO88")')
    const http = buildHttpSendExample({ bizType: 'PURCHASE_ORDER', sampleKey: 'PO88' })
    expect(http).toContain('"bizType": "PURCHASE_ORDER"')
    expect(http).toContain('"bizKey": "PO88"')
  })
})
