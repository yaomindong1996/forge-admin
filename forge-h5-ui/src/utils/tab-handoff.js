// uni.switchTab 不能携带查询参数，页签页之间用一次性存储交接参数，目标页在 onShow 中读取并清除。
const PREFIX = 'forge_h5_tab_handoff:'

export function setTabHandoff(tab, payload) {
  try { uni.setStorageSync(`${PREFIX}${tab}`, payload) }
  catch (error) { console.warn('写入页签交接参数失败:', error) }
}

export function takeTabHandoff(tab) {
  try {
    const payload = uni.getStorageSync(`${PREFIX}${tab}`)
    if (payload) uni.removeStorageSync(`${PREFIX}${tab}`)
    return payload || null
  }
  catch {
    return null
  }
}

export function openTab(tab, url, payload) {
  if (payload) setTabHandoff(tab, payload)
  uni.switchTab({ url, fail: () => uni.reLaunch({ url }) })
}
