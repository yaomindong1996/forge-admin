/* eslint-disable no-template-curly-in-string -- 本文件专测 ${}/{{}}/$form 占位符语法，字符串中的 ${} 是测试数据而非模板字面量 */
import { describe, expect, it } from 'vitest'
import { buildTemplateRefSignature, extractTemplateRefs, interpolateTemplate } from '../page-widget-schema'

describe('interpolateTemplate 统一占位符语法', () => {
  const data = { deptId: 'D1', user: { name: 'Tom' }, id: 42 }

  it('支持 ${字段} 语法', () => {
    expect(interpolateTemplate('{"deptId": "${deptId}"}', data)).toBe('{"deptId": "D1"}')
  })

  it('支持 $form.字段 嵌套路径语法', () => {
    expect(interpolateTemplate('$form.user.name', data)).toBe('Tom')
  })

  it('保持 {{ 字段 }} 旧语法兼容', () => {
    expect(interpolateTemplate('{{ id }}', data)).toBe('42')
  })

  it('混合语法可同时使用', () => {
    expect(interpolateTemplate('a${deptId}b{{id}}c', data)).toBe('aD1b42c')
  })

  it('引用不存在的字段输出空串', () => {
    expect(interpolateTemplate('${nope}', data)).toBe('')
  })
})

describe('extractTemplateRefs / buildTemplateRefSignature', () => {
  it('提取三种语法的引用字段', () => {
    const refs = extractTemplateRefs('/api/x/${a}', '{"b": "{{ c }}", "d": "$form.e"}')
    expect(refs).toEqual(['a', 'c', 'e'])
  })

  it('签名只随被引用字段的值变化', () => {
    const template = '{"x": "${a}"}'
    const sig1 = buildTemplateRefSignature({ a: 1, b: 2 }, template)
    expect(buildTemplateRefSignature({ a: 2, b: 2 }, template)).not.toBe(sig1)
    expect(buildTemplateRefSignature({ a: 1, b: 9 }, template)).toBe(sig1)
  })

  it('无引用时签名为空串', () => {
    expect(buildTemplateRefSignature({ a: 1 }, '{"x": 1}')).toBe('')
  })
})
