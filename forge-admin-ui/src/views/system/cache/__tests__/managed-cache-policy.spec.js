import { describe, expect, it } from 'vitest'
import { normalizeManagedCachePolicy, validateManagedCachePolicy } from '../managed-cache-policy'

describe('managed cache policy', () => {
  it('normalizes an effective policy without changing code-owned fields', () => {
    const normalized = normalizeManagedCachePolicy({
      applicationCode: 'forge-admin',
      cacheName: 'system:dict-data',
      allowedModes: ['LOCAL', 'MULTI'],
      enabled: false,
      cacheMode: 'LOCAL',
      localTtlSeconds: 60,
      redisTtlSeconds: 1800,
      localMaxSize: 2000,
      cacheNull: false,
      nullTtlSeconds: 30,
      policyVersion: 4,
    })

    expect(normalized).toEqual({
      applicationCode: 'forge-admin',
      cacheName: 'system:dict-data',
      enabled: false,
      cacheMode: 'LOCAL',
      localTtlSeconds: 60,
      redisTtlSeconds: 1800,
      localMaxSize: 2000,
      cacheNull: false,
      nullTtlSeconds: 30,
      policyVersion: 4,
    })
    expect(normalized).not.toHaveProperty('allowedModes')
  })

  it('rejects modes outside the code-owned allow list', () => {
    expect(validateManagedCachePolicy(
      validPolicy({ cacheMode: 'REDIS' }),
      ['LOCAL', 'MULTI'],
    )).toBe('当前缓存不允许使用 REDIS 模式')
  })

  it('requires positive ttl and local capacity values', () => {
    expect(validateManagedCachePolicy(
      validPolicy({ redisTtlSeconds: 0 }),
      ['LOCAL', 'REDIS', 'MULTI'],
    )).toBe('TTL 和本地容量必须大于 0')
  })

  it('requires multi-level local ttl not to exceed redis ttl', () => {
    expect(validateManagedCachePolicy(
      validPolicy({ cacheMode: 'MULTI', localTtlSeconds: 1801 }),
      ['LOCAL', 'REDIS', 'MULTI'],
    )).toBe('多级缓存的本地 TTL 不能大于 Redis TTL')
  })

  it('accepts a valid policy', () => {
    expect(validateManagedCachePolicy(
      validPolicy(),
      ['LOCAL', 'REDIS', 'MULTI'],
    )).toBeNull()
  })
})

function validPolicy(overrides = {}) {
  return {
    applicationCode: 'forge-admin',
    cacheName: 'system:dict-data',
    enabled: true,
    cacheMode: 'MULTI',
    localTtlSeconds: 60,
    redisTtlSeconds: 1800,
    localMaxSize: 2000,
    cacheNull: false,
    nullTtlSeconds: 30,
    policyVersion: 0,
    ...overrides,
  }
}
