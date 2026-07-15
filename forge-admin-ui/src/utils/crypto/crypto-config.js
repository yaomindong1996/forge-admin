const REQUIRED_API_EXCLUDE_PATHS = [
  '/auth/captcha',
  '/auth/captcha/**',
  '/auth/login',
  '/auth/loginConfig',
  '/auth/tenant/options',
  '/auth/tenant/assets/**',
  '/social/callback',
  '/crypto/config',
  '/crypto/public-key',
  '/crypto/exchange',
  '/api/config/manage/crypto',
]

const REQUIRED_REPLAY_EXCLUDE_PATHS = [
  '/auth/captcha',
  '/auth/captcha/**',
  '/auth/loginConfig',
  '/auth/tenant/options',
  '/auth/tenant/assets/**',
  '/social/callback',
  '/crypto/config',
  '/crypto/public-key',
  '/crypto/exchange',
]

/**
 * 加密配置。运行时会由后端安全裁剪配置覆盖；加载失败时保持安全默认开启。
 */
export const cryptoConfig = {
  // 是否启用加密
  enabled: true,
  // 是否启用 API 级加解密
  enableApiCrypto: true,
  // 是否启用字段级加解密（仅用于展示运行状态）
  enableFieldCrypto: true,
  // 默认加密算法 SM4/AES
  algorithm: 'SM4',
  // 密钥（Base64编码，16字节）- 动态密钥模式下由密钥交换服务设置
  secretKey: '',
  // 是否启用动态密钥
  enableDynamicKey: true,
  // 是否启用防重放
  enableReplay: true,
  // 防重放包含路径
  replayIncludePaths: [],
  // 防重放排除路径（支持通配符 ** 匹配多级路径）
  replayExcludePaths: [...REQUIRED_REPLAY_EXCLUDE_PATHS],
  // 需要加密的路径（支持通配符）
  includePaths: [],
  // 排除加密的路径
  excludePaths: [...REQUIRED_API_EXCLUDE_PATHS],
}

function normalizeBoolean(value, fallback) {
  if (typeof value === 'boolean')
    return value
  if (value === 'true' || value === 1 || value === '1')
    return true
  if (value === 'false' || value === 0 || value === '0')
    return false
  return fallback
}

function normalizePaths(value, fallback, required = []) {
  const source = Array.isArray(value) ? value : fallback
  return [...new Set([
    ...source.map(item => String(item || '').trim()).filter(Boolean),
    ...required,
  ])]
}

/**
 * 只接收浏览器实际需要的安全字段，主动丢弃 secretKey、RSA 密钥等敏感配置。
 */
export function normalizeRuntimeCryptoConfig(config = {}) {
  const source = config && typeof config === 'object' ? config : {}
  const algorithm = String(source.algorithm || 'SM4').toUpperCase()
  return {
    enabled: normalizeBoolean(source.enabled, true),
    enableApiCrypto: normalizeBoolean(source.enableApiCrypto, true),
    enableFieldCrypto: normalizeBoolean(source.enableFieldCrypto, true),
    algorithm: ['SM4', 'AES'].includes(algorithm) ? algorithm : 'SM4',
    enableDynamicKey: normalizeBoolean(source.enableDynamicKey, true),
    enableReplay: normalizeBoolean(
      source.enableReplay ?? source.enableReplayProtection,
      false,
    ),
    replayIncludePaths: normalizePaths(source.replayIncludePaths, []),
    replayExcludePaths: normalizePaths(
      source.replayExcludePaths,
      REQUIRED_REPLAY_EXCLUDE_PATHS,
      REQUIRED_REPLAY_EXCLUDE_PATHS,
    ),
    includePaths: normalizePaths(source.includePaths, []),
    excludePaths: normalizePaths(
      source.excludePaths,
      REQUIRED_API_EXCLUDE_PATHS,
      REQUIRED_API_EXCLUDE_PATHS,
    ),
  }
}

export function applyRuntimeCryptoConfig(config) {
  const normalized = normalizeRuntimeCryptoConfig(config)
  Object.assign(cryptoConfig, normalized)
  return normalized
}

export async function loadRuntimeCryptoConfig(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== 'function')
    return null

  const prefix = String(import.meta.env.VITE_REQUEST_PREFIX || '').replace(/\/+$/, '')
  try {
    const response = await fetchImpl(`${prefix}/crypto/config`, {
      cache: 'no-store',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
    })
    if (!response?.ok)
      throw new Error(`HTTP ${response?.status || 'unknown'}`)
    const payload = await response.json()
    if (payload?.code !== 200 || !payload?.data)
      throw new Error(payload?.message || payload?.msg || '运行配置响应无效')
    return applyRuntimeCryptoConfig(payload.data)
  }
  catch (error) {
    console.warn('[Crypto] 加解密运行配置加载失败，保持安全默认开启:', error)
    return null
  }
}

/**
 * 判断路径是否匹配模式
 * @param {string} path 路径
 * @param {string} pattern 模式（支持 ** 通配符）
 */
export function matchPath(path, pattern) {
  if (!pattern)
    return false
  // 将 ** 转换为正则表达式
  const regexPattern = pattern
    .replace(/\*\*/g, '.*')
    .replace(/\*/g, '[^/]*')
  const regex = new RegExp(`^${regexPattern}$`)
  return regex.test(path)
}

/**
 * 判断是否需要加密
 * @param {string} url 请求URL
 */
export function shouldEncrypt(url) {
  if (!cryptoConfig.enabled || !cryptoConfig.enableApiCrypto)
    return false

  // 提取路径部分
  const path = url.split('?')[0]

  // 检查排除路径
  for (const pattern of cryptoConfig.excludePaths) {
    if (matchPath(path, pattern)) {
      return false
    }
  }

  // 如果没有配置包含路径，则默认加密
  if (cryptoConfig.includePaths.length === 0) {
    return true
  }

  // 检查包含路径
  for (const pattern of cryptoConfig.includePaths) {
    if (matchPath(path, pattern)) {
      return true
    }
  }

  return false
}

/**
 * 更新加密配置
 * @param {object} config 配置对象
 */
export function updateCryptoConfig(config) {
  Object.assign(cryptoConfig, config)
}
