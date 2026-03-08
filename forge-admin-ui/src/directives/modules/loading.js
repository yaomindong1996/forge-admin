import { createApp } from 'vue'
import Loading from '../../components/Loading.vue'

// 存储全局loading实例
let loadingInstance = null
let loadingApp = null

// 全局loading控制方法
export const loadingService = {
  // 显示全局loading
  show(options = {}) {
    // 如果已有实例，先关闭
    if (loadingInstance) {
      this.close()
    }
    
    // 创建新的loading实例
    const container = document.createElement('div')
    document.body.appendChild(container)
    
    loadingApp = createApp(Loading, {
      fullscreen: true,
      text: options.text || '加载中...',
      background: options.background || '0, 0, 0, 0.7',
      color: options.color || '#ffffff',
      fontSize: options.fontSize || 14
    })
    
    loadingInstance = loadingApp.mount(container)
    
    // 防止页面滚动
    document.body.style.overflow = 'hidden'
    
    return loadingInstance
  },
  
  // 关闭全局loading
  close() {
    if (loadingInstance && loadingApp) {
      loadingApp.unmount()
      if (loadingInstance.$el && loadingInstance.$el.parentNode) {
        loadingInstance.$el.parentNode.removeChild(loadingInstance.$el)
      }
      loadingInstance = null
      loadingApp = null
      document.body.style.overflow = ''
    }
  }
}

// 指令形式的loading
const loadingDirective = {
  mounted(el, binding) {
    // 确保元素具有相对定位，以便loading遮罩正确定位
    const computedStyle = window.getComputedStyle(el)
    if (computedStyle.position === 'static') {
      el.style.position = 'relative'
    }
    
    // 创建loading实例
    const mask = document.createElement('div')
    mask.style.position = 'absolute'
    mask.style.top = '0'
    mask.style.left = '0'
    mask.style.width = '100%'
    mask.style.height = '100%'
    mask.style.zIndex = '999'
    mask.style.display = 'none'
    
    // 创建loading组件
    const loadingContainer = document.createElement('div')
    mask.appendChild(loadingContainer)
    el.appendChild(mask)
    
    // 保存引用
    el._loadingMask = mask
    el._loadingContainer = loadingContainer
    
    // 创建Vue实例
    if (binding.value) {
      // 直接调用更新逻辑而不是不存在的update方法
      updateLoading(el, binding)
    }
  },
  
  updated(el, binding) {
    if (binding.value !== binding.oldValue) {
      updateLoading(el, binding)
    }
  },
  
  unmounted(el) {
    // 清理资源
    if (el._loadingApp) {
      el._loadingApp.unmount()
    }
    if (el._loadingMask && el._loadingMask.parentNode) {
      el._loadingMask.parentNode.removeChild(el._loadingMask)
    }
    el._loadingMask = null
    el._loadingContainer = null
    el._loadingApp = null
    el._loadingInstance = null
  }
}

// 提取更新逻辑到独立函数
function updateLoading(el, binding) {
  if (binding.value) {
    // 显示loading
    el._loadingMask.style.display = 'block'
    
    // 创建loading组件实例
    if (!el._loadingApp) {
      const options = binding.value || {}
      el._loadingApp = createApp(Loading, {
        fullscreen: false,
        text: options.text || '加载中...',
        background: options.background || '255, 255, 255, 0.9',
        color: options.color || '#333333',
        fontSize: options.fontSize || 14
      })
      
      el._loadingInstance = el._loadingApp.mount(el._loadingContainer)
    }
  } else {
    // 隐藏loading
    if (el._loadingMask) {
      el._loadingMask.style.display = 'none'
    }
  }
}

export default loadingDirective