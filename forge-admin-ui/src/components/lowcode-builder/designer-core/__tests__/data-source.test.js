/**
 * @file 数据源 spec 测试 — 验证 7 类数据源定义完整性
 */

import { describe, it, expect } from 'vitest'
import {
  DATA_SOURCE_KINDS,
  resolveDataSourceKinds,
  isDataSourceKind,
} from '../index'

describe('DataSource Spec', () => {
  it('should define 7 data source kinds', () => {
    const kinds = Object.keys(DATA_SOURCE_KINDS)
    expect(kinds.length).toBe(7)
    expect(kinds).toContain('static')
    expect(kinds).toContain('dict')
    expect(kinds).toContain('managed')
    expect(kinds).toContain('remote')
    expect(kinds).toContain('context')
    expect(kinds).toContain('relation')
    expect(kinds).toContain('builtin')
  })

  it('each kind should have label and kind fields', () => {
    Object.values(DATA_SOURCE_KINDS).forEach(def => {
      expect(def.kind).toBeDefined()
      expect(def.label).toBeDefined()
      expect(typeof def.label).toBe('string')
    })
  })

  it('dict kind should require dictType', () => {
    expect(DATA_SOURCE_KINDS.dict.needDictType).toBe(true)
  })

  it('remote kind should require api', () => {
    expect(DATA_SOURCE_KINDS.remote.needApi).toBe(true)
  })

  it('isDataSourceKind should validate correctly', () => {
    expect(isDataSourceKind('static')).toBe(true)
    expect(isDataSourceKind('dict')).toBe(true)
    expect(isDataSourceKind('invalid')).toBe(false)
    expect(isDataSourceKind('')).toBe(false)
  })

  it('resolveDataSourceKinds should filter valid kinds', () => {
    const valid = resolveDataSourceKinds(['static', 'dict', 'invalid'])
    expect(valid.length).toBe(2)
    expect(valid.map(k => k.kind)).toContain('static')
    expect(valid.map(k => k.kind)).toContain('dict')
  })
})
