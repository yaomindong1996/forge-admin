import { describe, expect, it } from 'vitest'
import { imageJobResult, imageRecordId, imageToolTask } from '../image-job-utils'

describe('图片任务 ID 与结果协议', () => {
  it('long 字符串保持精度，安全的小整数兼容', () => {
    expect(imageRecordId('9007199254740993')).toBe('9007199254740993')
    expect(imageRecordId('9223372036854775807')).toBe('9223372036854775807')
    expect(imageRecordId(31)).toBe('31')
  })

  it.each([9007199254740992, '9223372036854775808', '0', '-1', '01', '1/../../secret', 'https://invalid.test', null])(
    '拒绝无效或已丢精度 ID：%s',
    (id) => { expect(imageRecordId(id)).toBe('') },
  )

  it('成功只保留真实 fileId，不缓存 prompt 或返回地址', () => {
    expect(imageJobResult({ id: '31', status: 'success', resultFileId: '9007199254740993', prompt: '合成提示词', resultEndpoint: 'https://invalid.test' }, '31'))
      .toEqual({ id: '31', status: 'success', resultFileId: '9007199254740993', errorMsg: '' })
  })

  it.each([
    { id: '32', status: 'pending' },
    { id: '31', status: 'unknown' },
    { id: '31', status: 'success', resultFileId: null },
  ])('未知、错任务和缺文件响应拒绝：%j', (result) => {
    expect(() => imageJobResult(result, '31')).toThrow()
  })

  it('只识别已经成功执行的内置工具任务，不访问任意 endpoint', () => {
    const tool = { name: 'image_generate', status: 'done', result: JSON.stringify({
      recordId: '31',
      status: 'pending',
      resultEndpoint: '/ai/image-generate/31',
    }) }
    expect(imageToolTask(tool)).toEqual({ recordId: '31' })
    expect(imageToolTask({ ...tool, status: 'running' })).toBeNull()
    expect(imageToolTask({ ...tool, name: 'http_request' })).toBeNull()
    expect(imageToolTask({ ...tool, result: '{' })).toBeNull()
    expect(imageToolTask({ ...tool, result: { recordId: '31', status: 'pending', resultEndpoint: 'https://invalid.test/31' } })).toBeNull()
  })
})
