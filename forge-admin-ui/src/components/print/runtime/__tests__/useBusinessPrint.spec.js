import { describe, expect, it } from 'vitest'
import { createBusinessPrintRecord, resolveBusinessPrintRecord, useBusinessPrint } from '../useBusinessPrint'

const source = {
  businessSourceId: '9007199254740995',
  sourceCode: 'purchase_order',
  sourceType: 'SERVICE',
  objectCode: 'purchase_order',
}

describe('business print entry', () => {
  it('builds a standalone record without leaking template or page data', () => {
    expect(createBusinessPrintRecord({
      ...source,
      recordId: 42,
      params: { version: 7, includeDetail: true },
    })).toEqual({
      source,
      recordId: '42',
      scene: 'DETAIL',
      params: { version: 7, includeDetail: true },
    })
  })

  it('supports per-row overrides while keeping a reusable source configuration', async () => {
    const print = useBusinessPrint({ ...source, scene: 'LIST' })
    const record = await print.open({ recordId: 'row_9', params: { version: 2 } })
    expect(record.recordId).toBe('row_9')
    expect(record.params).toEqual({ version: 2 })
    expect(print.visible.value).toBe(true)
    print.close()
    expect(print.record.value).toBeNull()
  })

  it('resolves server-owned source identity when a business page only knows sourceCode', async () => {
    const resolver = async code => ({
      data: {
        id: '9007199254740995',
        sourceCode: code,
        sourceType: 'SERVICE',
        objectCode: 'purchase_order',
      },
    })
    const record = await resolveBusinessPrintRecord({
      sourceCode: 'purchase_order',
      recordId: 'PO-9',
      params: { version: 3 },
    }, resolver)
    expect(record.source).toEqual(source)
    expect(record.params).toEqual({ version: 3 })
  })

  it.each([
    [{ ...source, recordId: '' }, '打印记录标识无效'],
    [{ ...source, recordId: '1', scene: 'FLOW_TODO' }, '普通业务打印只支持列表或详情场景'],
    [{ ...source, recordId: '1', params: { nested: {} } }, '只能是字符串、数字或布尔值'],
    [{ ...source, recordId: '1', params: { tenantId: 1 } }, '打印参数名无效'],
  ])('fails closed for invalid client input', (options, message) => {
    expect(() => createBusinessPrintRecord(options)).toThrow(message)
  })
})
