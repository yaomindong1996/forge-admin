import { describe, expect, it } from 'vitest'
import { formatBusinessBindings, getModelSecondaryAction, mergeModelBusinessBindings, publishedVersionLabel } from '../modelCatalog'
import { parseBusinessFormReference, resolveApplicationIdFromFormKey } from '../modelFormReference'

describe('模型卡片展示契约', () => {
  it('拆出的表单引用解析保留当前应用与长 ID 字符串', () => {
    const id = '190000000000000001'
    const reference = parseBusinessFormReference(JSON.stringify({
      objectCode: 'orders',
      formRef: { applicationId: id, formKey: `app_${id}_page_detail_form_main` },
    }))
    expect(reference.applicationId).toBe(id)
    expect(reference.objectCode).toBe('orders')
    expect(resolveApplicationIdFromFormKey(reference.formKey)).toBe(id)
  })
  it('旧的空或非法表单引用不制造业务应用', () => {
    expect(parseBusinessFormReference('not-json')).toEqual({})
    expect(parseBusinessFormReference('[]')).toEqual({})
    expect(parseBusinessFormReference(null)).toEqual({})
  })
  it('不拿初始 version=1 或生命周期状态冒充发布版本', () => {
    expect(publishedVersionLabel({ version: 1, status: 0 })).not.toContain('v1')
    expect(publishedVersionLabel({ version: 3, status: 1 })).not.toContain('v3')
    expect(publishedVersionLabel({ publishedVersion: 3 })).toBe('发布版本 v3')
  })
  it('发布更新只认后端差异，挂起仍进入运行记录', () => {
    expect(getModelSecondaryAction({ status: 0 }).label).toBe('发布流程')
    expect(getModelSecondaryAction({ status: 1, publicationState: 'CHANGED' }).label).toBe('发布更新')
    expect(getModelSecondaryAction({ status: 1, publicationState: 'UNKNOWN' }).label).toBe('运行记录')
    expect(getModelSecondaryAction({ status: 2, publicationState: 'CHANGED' }).key).toBe('instances')
  })
  it('绑定失败不等于未绑定，明确表单应用不被共享对象主应用覆盖', () => {
    expect(formatBusinessBindings({ bindingLoadError: true })).toBe('业务绑定暂不可用')
    expect(formatBusinessBindings({ businessBindings: [] })).toBe('未绑定业务应用')
    const configured = { applicationId: '190000000000000001', applicationName: '当前应用', objectCode: 'orders' }
    expect(mergeModelBusinessBindings([{ applicationName: '其他主应用', objectCode: 'orders' }], [configured]))
      .toEqual([configured])
  })
})
