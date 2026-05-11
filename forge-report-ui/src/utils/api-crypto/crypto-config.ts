export const cryptoConfig = {
  enabled: true,
  algorithm: 'SM4',
  secretKey: '',
  enableDynamicKey: true,
  enableReplay: true,
  replayExcludePaths: ['/auth/captcha', '/auth/captcha/**', '/auth/loginConfig', '/crypto/public-key'],
  includePaths: [] as string[],
  excludePaths: ['/auth/captcha', '/auth/captcha/**', '/auth/login', '/auth/loginConfig', '/crypto/public-key', '/crypto/exchange']
}

export function matchPath(path: string, pattern: string): boolean {
  if (!pattern) return false
  const normalizedPath = path.replace(/^\/forge-report-api/, '')
  const regexPattern = pattern.replace(/\*\*/g, '.*').replace(/\*/g, '[^/]*')
  const regex = new RegExp(`^${regexPattern}$`)
  return regex.test(path) || regex.test(normalizedPath)
}

export function shouldEncrypt(url = ''): boolean {
  if (!cryptoConfig.enabled) return false
  const path = url.split('?')[0]

  for (const pattern of cryptoConfig.excludePaths) {
    if (matchPath(path, pattern)) return false
  }

  if (!cryptoConfig.includePaths.length) return true

  return cryptoConfig.includePaths.some(pattern => matchPath(path, pattern))
}

export function updateCryptoConfig(config: Partial<typeof cryptoConfig>) {
  Object.assign(cryptoConfig, config)
}
