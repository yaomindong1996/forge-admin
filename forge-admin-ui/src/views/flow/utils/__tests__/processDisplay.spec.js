import { describe, expect, it } from 'vitest'
import {
  buildTaskBusinessHeadline,
  buildTaskDisplayFields,
  getProcessDisplayName,
  getProcessStartTime,
  getRowDisplayTitle,
  getTaskHandlerName,
  hasTaskBusinessSummary,
} from '../processDisplay'

describe('buildTaskDisplayFields', () => {
  it('优先读取 displayExtensions.fields', () => {
    expect(buildTaskDisplayFields({
      businessParams: { displayFields: [{ label: '旧', value: '不该出现' }] },
      displayExtensions: {
        fields: [
          { label: '车间', value: '一车间' },
          { name: '金额', value: '1200' },
        ],
      },
    })).toEqual([
      { key: '车间', label: '车间', value: '一车间' },
      { key: '金额', label: '金额', value: '1200' },
    ])
  })

  it('兼容 displayExtensions 直接用键值对象', () => {
    expect(buildTaskDisplayFields({
      displayExtensions: { 车间: '二车间', recordId: 99, processInstanceId: 'p1' },
    })).toEqual([
      { key: '车间', label: '车间', value: '二车间' },
    ])
  })

  it('没有 displayExtensions 时回退 businessParams.displayFields', () => {
    expect(buildTaskDisplayFields({
      businessParams: {
        displayFields: [{ title: '申请人', text: '张三' }],
      },
    })).toEqual([
      { key: '申请人', label: '申请人', value: '张三' },
    ])
  })

  it('不会把整份业务记录摊成待办字段', () => {
    expect(buildTaskDisplayFields({
      businessParams: {
        id: 1,
        tenantId: 1,
        orderNo: 'PO-1',
        amount: 99,
      },
    })).toEqual([])
  })
})

describe('buildTaskBusinessHeadline', () => {
  it('拼接对象名和摘要', () => {
    expect(buildTaskBusinessHeadline({
      businessObjectName: '采购申请',
      businessSummary: 'PO-001',
    })).toBe('采购申请：PO-001')
  })

  it('有展示字段时也算有摘要', () => {
    expect(hasTaskBusinessSummary({
      displayExtensions: { fields: [{ label: '车间', value: '一车间' }] },
    })).toBe(true)
  })
})

describe('getTaskHandlerName', () => {
  it('优先显示当前处理人姓名', () => {
    expect(getTaskHandlerName({ assignee: '1', assigneeName: '张三' })).toBe('张三')
  })

  it('未签收时显示待认领', () => {
    expect(getTaskHandlerName({ candidateUsers: '1,2' })).toBe('待认领')
  })
})

describe('getRowDisplayTitle', () => {
  it('优先显示流程审批标题，不被业务摘要覆盖', () => {
    expect(getRowDisplayTitle({
      title: '测试-5.0',
      businessObjectName: '测试',
      businessSummary: '85',
    })).toBe('测试-5.0')
  })

  it('列表标题不回退到业务主键或模型 Key', () => {
    expect(getRowDisplayTitle({
      processName: '采购申请',
      businessKey: 'sample_purchase_order:88',
      processDefKey: 'sample_purchase_order',
      processInstanceId: 'proc-1',
    })).toBe('采购申请')
    expect(getRowDisplayTitle({
      businessKey: 'sample_purchase_order:88',
      processDefKey: 'sample_purchase_order',
      processInstanceId: 'proc-1',
    })).toBe('-')
  })
})

describe('getProcessDisplayName', () => {
  it('用模型名称，不用流程编码兜底', () => {
    expect(getProcessDisplayName({
      processName: '测试页面审批',
      businessType: 'business_object_xvcy_approval_mu7walur',
      processDefKey: 'b7a0c23b-b3e3-11f1-95ed-b6a705e6d871',
    })).toBe('测试页面审批')
  })

  it('名称缺失时不把流程编码显示成流程名称', () => {
    expect(getProcessDisplayName({
      businessType: 'business_object_xvcy_approval_mu7walur',
      processDefKey: 'business_object_xvcy_approval_mu7walur',
    })).toBe('-')
  })
})

describe('getProcessStartTime', () => {
  it('取审批时间轴 start 记录时间，而不是当前任务到达时间', () => {
    expect(getProcessStartTime([
      { action: 'start', createTime: '2026-11-14T14:29:39' },
      { action: 'approve', createTime: '2026-11-14T15:59:08' },
    ])).toBe('2026-11-14 14:29:39')
  })

  it('时间轴未加载或没有 start 记录时返回占位', () => {
    expect(getProcessStartTime([])).toBe('-')
    expect(getProcessStartTime(undefined, '')).toBe('')
    expect(getProcessStartTime([{ action: 'approve', createTime: '2026-11-14 15:59:08' }])).toBe('-')
  })
})
