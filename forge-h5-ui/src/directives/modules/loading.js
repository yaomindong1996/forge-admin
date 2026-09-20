import { createApp, ref } from 'vue'
import Loading from '../../components/Loading.vue'

// H5环境下使用DOM方式显示loading
function showH5Loading(options = {}) {
  if (typeof document === 'undefined') return null
  
  const existing = document.querySelector('.global-loading-wrapper')
  if (existing) existing.remove()
  
  const container = document.createElement('div')
  container.className = 'global-loading-wrapper'
  document.body.appendChild(container)

  const loadingApp = createApp(Loading, {
    visible: true,
    fullScreen: true,
    text: options.text || '加载中...',
    type: options.type || 'brand',
    theme: options.theme || 'brand',
    size: options.size || 'md',
    blur: options.blur !== false,
    zIndex: options.zIndex || 200,
  })

  const instance = loadingApp.mount(container)
  document.body.style.overflow = 'hidden'

  return {
    close: () => {
      loadingApp.unmount()
      if (container.parentNode) {
        container.parentNode.removeChild(container)
      }
      document.body.style.overflow = ''
    }
  }
}

let currentLoading = null

// loading服务
export const loadingService = {
  show(options = {}) {
    if (typeof uni !== 'undefined' && typeof document === 'undefined') {
      // App/小程序环境使用uni.showLoading
      uni.showLoading({
        title: options.text || '加载中...',
        mask: true
      })
      return {
        close: () => uni.hideLoading()
      }
    }
    // H5环境
    return showH5Loading(options)
  },
  close() {
    if (typeof uni !== 'undefined' && typeof document === 'undefined') {
      uni.hideLoading()
    } else {
      const existing = document.querySelector('.global-loading-wrapper')
      if (existing) {
        existing.remove()
        document.body.style.overflow = ''
      }
    }
  }
}

// v-loading指令
const loadingDirective = {
  mounted(el, binding) {
    if (typeof window === 'undefined' || typeof document === 'undefined') return

    const computedStyle = window.getComputedStyle?.(el)
    if (!computedStyle || computedStyle.position === 'static') {
      el.style.position = 'relative'
    }

    const mask = document.createElement('div')
    mask.className = 'v-loading-mask'
    mask.style.cssText = 'position:absolute;top:0;left:0;width:100%;height:100%;z-index:999;display:none;background:rgba(255,255,255,0.58);backdrop-filter:blur(8px);align-items:center;justify-content:center;border-radius:inherit;'
    mask.innerHTML = '<div class="v-loading-card"><div class="v-loading-dots"><i class="v-loading-dot v-loading-dot--1"></i><i class="v-loading-dot v-loading-dot--2"></i><i class="v-loading-dot v-loading-dot--3"></i></div><div class="v-loading-text">加载中...</div></div>'
    el.appendChild(mask)
    el._loadingMask = mask
  },
  updated(el, binding) {
    if (typeof document === 'undefined') return

    if (binding.value && el._loadingMask) {
      el._loadingMask.style.display = 'flex'
    } else if (el._loadingMask) {
      el._loadingMask.style.display = 'none'
    }
  },
  unmounted(el) {
    if (typeof document === 'undefined') return

    if (el._loadingMask && el._loadingMask.parentNode) {
      el._loadingMask.parentNode.removeChild(el._loadingMask)
    }
    el._loadingMask = null
  }
}

export default loadingDirective
