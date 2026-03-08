/**
 * 密钥交换服务
 * 负责与后端进行动态密钥协商
 */
import { rsaEncrypt } from './rsa'
import { cryptoConfig, updateCryptoConfig } from './crypto-config'

// 密钥存储键名
const STORAGE_KEYS = {
  SESSION_KEY: 'crypto_session_key',
  PUBLIC_KEY: 'crypto_public_key',
  EXCHANGED: 'crypto_exchanged'
}

// 密钥交换状态
const keyExchangeState = {
  publicKey: null,       // RSA 公钥
  sessionKey: null,      // 会话密钥
  exchanged: false,      // 是否已完成密钥交换
  exchanging: false,     // 是否正在进行密钥交换
}

/**
 * 从 localStorage 恢复密钥状态
 */
function restoreKeyState() {
  try {
    const sessionKey = localStorage.getItem(STORAGE_KEYS.SESSION_KEY)
    const publicKey = localStorage.getItem(STORAGE_KEYS.PUBLIC_KEY)
    const exchanged = localStorage.getItem(STORAGE_KEYS.EXCHANGED) === 'true'
    
    if (sessionKey) {
      keyExchangeState.sessionKey = sessionKey
      keyExchangeState.exchanged = exchanged
      // 恢复加密配置
      updateCryptoConfig({ secretKey: sessionKey })
      console.log('[Crypto] 密钥状态已从本地存储恢复')
    }
    
    if (publicKey) {
      keyExchangeState.publicKey = publicKey
    }
  } catch (error) {
    console.error('[Crypto] 恢复密钥状态失败:', error)
  }
}

/**
 * 保存密钥状态到 localStorage
 */
function saveKeyState() {
  try {
    if (keyExchangeState.sessionKey) {
      localStorage.setItem(STORAGE_KEYS.SESSION_KEY, keyExchangeState.sessionKey)
      localStorage.setItem(STORAGE_KEYS.EXCHANGED, String(keyExchangeState.exchanged))
    }
    if (keyExchangeState.publicKey) {
      localStorage.setItem(STORAGE_KEYS.PUBLIC_KEY, keyExchangeState.publicKey)
    }
  } catch (error) {
    console.error('[Crypto] 保存密钥状态失败:', error)
  }
}

/**
 * 清除保存的密钥状态
 */
function clearStoredKeyState() {
  try {
    localStorage.removeItem(STORAGE_KEYS.SESSION_KEY)
    localStorage.removeItem(STORAGE_KEYS.PUBLIC_KEY)
    localStorage.removeItem(STORAGE_KEYS.EXCHANGED)
  } catch (error) {
    console.error('[Crypto] 清除密钥状态失败:', error)
  }
}

// 页面加载时恢复密钥状态
restoreKeyState()

/**
 * 生成随机会话密钥
 * @param {number} length 密钥长度（字节），默认16字节
 * @returns {string} Base64编码的密钥
 */
function generateSessionKey(length = 16) {
  const array = new Uint8Array(length)
  crypto.getRandomValues(array)
  return btoa(String.fromCharCode.apply(null, array))
}

/**
 * 获取 RSA 公钥
 * @param {object} axios axios 实例
 * @param {boolean} forceRefresh 是否强制刷新
 * @returns {Promise<string>} RSA公钥
 */
export async function fetchPublicKey(axios, forceRefresh = false) {
  // if (keyExchangeState.publicKey && !forceRefresh) {
  //   return keyExchangeState.publicKey
  // }

  try {
    const res = await axios.get('/crypto/public-key')
    // 拦截器已处理，返回的是 data 对象，不是完整 response
    // res = { code: 200, data: { publicKey, algorithm }, msg: 'success' }
    if (res && res.code === 200 && res.data && res.data.publicKey) {
      keyExchangeState.publicKey = res.data.publicKey
      // 保存公钥到本地存储
      saveKeyState()
      return keyExchangeState.publicKey
    }
    throw new Error(res?.msg || '获取公钥失败')
  } catch (error) {
    console.error('获取公钥失败:', error)
    throw error
  }
}

/**
 * 执行密钥交换
 * @param {object} axios axios 实例
 * @param {string} sessionId 会话标识（可选，默认从 token 获取）
 * @returns {Promise<boolean>} 是否成功
 */
export async function exchangeKey(axios, sessionId) {
  // 如果已经交换过，直接返回
  if (keyExchangeState.exchanged) {
    return true
  }

  // 如果正在交换中，等待完成
  if (keyExchangeState.exchanging) {
    console.log('[Crypto] 密钥交换进行中，等待完成...')
    return new Promise((resolve) => {
      const maxWait = 50 // 最多等待5秒
      let count = 0
      const checkInterval = setInterval(() => {
        count++
        if (!keyExchangeState.exchanging || count > maxWait) {
          clearInterval(checkInterval)
          resolve(keyExchangeState.exchanged)
        }
      }, 100)
    })
  }

  console.log('[Crypto] 开始执行密钥交换...')
  keyExchangeState.exchanging = true

  try {
    // 1. 获取公钥
    const publicKey = await fetchPublicKey(axios)

    // 2. 生成会话密钥
    const sessionKey = generateSessionKey(16)

    // 3. 用 RSA 公钥加密会话密钥
    const encryptedKey = rsaEncrypt(sessionKey, publicKey)

    // 4. 发送到后端
    const headers = {}
    if (sessionId) {
      headers['X-Session-Id'] = sessionId
    }

    const res = await axios.post('/crypto/exchange', 
      { encryptedKey },
      { headers }
    )

    // 拦截器已处理，返回的是 data 对象
    if (res && res.code === 200) {
      // 5. 保存会话密钥并更新配置
      keyExchangeState.sessionKey = sessionKey
      keyExchangeState.exchanged = true
      
      // 更新加密配置使用新的会话密钥
      updateCryptoConfig({ secretKey: sessionKey })
      
      // 持久化密钥状态
      saveKeyState()
      
      console.log('[Crypto] 密钥交换成功')
      return true
    }

    throw new Error(res?.msg || '密钥交换失败')
  } catch (error) {
    console.error('密钥交换失败:', error)
    return false
  } finally {
    keyExchangeState.exchanging = false
  }
}

/**
 * 获取当前会话密钥
 * @returns {string|null} 会话密钥
 */
export function getSessionKey() {
  return keyExchangeState.sessionKey || cryptoConfig.secretKey
}

/**
 * 检查是否已完成密钥交换
 * @returns {boolean}
 */
export function isKeyExchanged() {
  return keyExchangeState.exchanged
}

/**
 * 重置密钥交换状态（用于登出时）
 */
export function resetKeyExchange() {
  keyExchangeState.publicKey = null
  keyExchangeState.sessionKey = null
  keyExchangeState.exchanged = false
  keyExchangeState.exchanging = false
  
  // 清除本地存储
  clearStoredKeyState()
  
  // 重置加密配置
  updateCryptoConfig({ secretKey: '' })
  
  console.log('[Crypto] 密钥交换状态已重置')
}

/**
 * 初始化密钥交换（在应用启动或登录后调用）
 * @param {object} axios axios 实例
 * @param {string} sessionId 会话标识
 * @returns {Promise<boolean>}
 */
export async function initKeyExchange(axios, sessionId) {
  if (!cryptoConfig.enableDynamicKey) {
    console.log('[Crypto] 动态密钥未启用，使用静态密钥')
    return true
  }

  return exchangeKey(axios, sessionId)
}

/**
 * 为登录请求加密密码
 * @param {string} password 明文密码
 * @param {object} axios axios 实例
 * @returns {Promise<string>} 加密后的密码
 */
export async function encryptPassword(password, axios) {
  try {
    // 获取 RSA 公钥
    const publicKey = await fetchPublicKey(axios)
    // 使用 RSA 公钥加密密码
    const encryptedPassword = rsaEncrypt(password, publicKey)
    return encryptedPassword
  } catch (error) {
    console.error('密码加密失败:', error)
    // 加密失败时返回原密码（降级方案）
    return password
  }
}
