export function normalizeManagedCachePolicy(policy = {}) {
  return {
    applicationCode: policy.applicationCode || '',
    cacheName: policy.cacheName || '',
    enabled: policy.enabled ?? true,
    cacheMode: policy.cacheMode || 'REDIS',
    localTtlSeconds: Number(policy.localTtlSeconds || 0),
    redisTtlSeconds: Number(policy.redisTtlSeconds || 0),
    localMaxSize: Number(policy.localMaxSize || 0),
    cacheNull: policy.cacheNull ?? false,
    nullTtlSeconds: Number(policy.nullTtlSeconds || 0),
    policyVersion: Number(policy.policyVersion || 0),
  }
}

export function validateManagedCachePolicy(policy, allowedModes = []) {
  if (!allowedModes.includes(policy.cacheMode)) {
    return `当前缓存不允许使用 ${policy.cacheMode} 模式`
  }

  const positiveValues = [
    policy.localTtlSeconds,
    policy.redisTtlSeconds,
    policy.localMaxSize,
    policy.nullTtlSeconds,
  ]
  if (positiveValues.some(value => !Number.isFinite(Number(value)) || Number(value) <= 0)) {
    return 'TTL 和本地容量必须大于 0'
  }

  if (policy.cacheMode === 'MULTI'
    && Number(policy.localTtlSeconds) > Number(policy.redisTtlSeconds)) {
    return '多级缓存的本地 TTL 不能大于 Redis TTL'
  }
  return null
}
