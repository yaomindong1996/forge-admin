/**
 * 加密配置
 */
export const cryptoConfig = {
  // 是否启用加密
  enabled: true,
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
  // 防重放排除路径
  replayExcludePaths: ['/auth/captcha', '/crypto/public-key'],
  // 需要加密的路径（支持通配符）
  includePaths: [],
  // 排除加密的路径
  excludePaths: ['/auth/captcha', '/auth/login', '/crypto/public-key', '/crypto/exchange']
}

/**
 * 判断路径是否匹配模式
 * @param {string} path 路径
 * @param {string} pattern 模式（支持 ** 通配符）
 */
export function matchPath(path, pattern) {
  if (!pattern) return false
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
  if (!cryptoConfig.enabled) return false
  
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
