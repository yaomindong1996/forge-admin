import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  applyRuntimeCryptoConfig,
  cryptoConfig,
  loadRuntimeCryptoConfig,
  normalizeRuntimeCryptoConfig,
  shouldEncrypt,
  updateCryptoConfig,
} from '../crypto-config'

describe('crypto runtime config', () => {
  beforeEach(() => {
    updateCryptoConfig({
      enabled: true,
      enableApiCrypto: true,
      enableFieldCrypto: true,
      algorithm: 'SM4',
      enableDynamicKey: true,
      enableReplay: false,
      secretKey: '',
      includePaths: [],
    })
  })

  it('disables browser API crypto when the global switch is off', () => {
    const normalized = applyRuntimeCryptoConfig({
      enabled: false,
      enableApiCrypto: true,
      enableDynamicKey: true,
      excludePaths: [],
    })

    expect(normalized.enabled).toBe(false)
    expect(shouldEncrypt('/system/user/page')).toBe(false)
    expect(normalized.excludePaths).toContain('/api/config/manage/crypto')
    expect(normalized.excludePaths).toContain('/crypto/config')
  })

  it('treats the API switch as an independent effective off switch', () => {
    applyRuntimeCryptoConfig({ enabled: true, enableApiCrypto: false })

    expect(cryptoConfig.enabled).toBe(true)
    expect(shouldEncrypt('/system/user/page')).toBe(false)
  })

  it('keeps a safe enabled default and drops all key material', () => {
    const normalized = normalizeRuntimeCryptoConfig({
      secretKey: 'must-not-reach-browser-state',
      rsaPrivateKey: 'private-key',
    })

    expect(normalized.enabled).toBe(true)
    expect(normalized.enableApiCrypto).toBe(true)
    expect(normalized).not.toHaveProperty('secretKey')
    expect(normalized).not.toHaveProperty('rsaPrivateKey')
  })

  it('does not downgrade the default when runtime config loading fails', async () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const result = await loadRuntimeCryptoConfig(vi.fn().mockRejectedValue(new Error('offline')))

    expect(result).toBeNull()
    expect(cryptoConfig.enabled).toBe(true)
    warn.mockRestore()
  })
})
