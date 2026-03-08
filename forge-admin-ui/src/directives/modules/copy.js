import { copy } from '@/utils/clipboard'

/**
 * v-copy 指令
 * 用法：
 * 1. 基础用法：<button v-copy="'复制的文本'">复制</button>
 * 2. 配置用法：<button v-copy="{ text: '文本', success: '已复制', error: '复制失败' }">复制</button>
 */
const copyDirective = {
  mounted(el, binding) {
    // 添加点击事件
    el._copyHandler = async () => {
      const value = binding.value
      
      if (!value) {
        console.warn('v-copy: 没有提供要复制的内容')
        return
      }
      
      // 支持字符串或对象配置
      if (typeof value === 'string') {
        await copy(value)
      } else if (typeof value === 'object') {
        const { text, success, error } = value
        await copy(text, success, error)
      }
    }
    
    // 添加样式，表示可点击
    el.style.cursor = 'pointer'
    
    // 绑定点击事件
    el.addEventListener('click', el._copyHandler)
  },
  
  updated(el, binding) {
    // 当绑定值更新时，更新处理函数
    if (el._copyHandler) {
      el.removeEventListener('click', el._copyHandler)
      
      el._copyHandler = async () => {
        const value = binding.value
        
        if (!value) {
          console.warn('v-copy: 没有提供要复制的内容')
          return
        }
        
        if (typeof value === 'string') {
          await copy(value)
        } else if (typeof value === 'object') {
          const { text, success, error } = value
          await copy(text, success, error)
        }
      }
      
      el.addEventListener('click', el._copyHandler)
    }
  },
  
  unmounted(el) {
    // 清理事件监听
    if (el._copyHandler) {
      el.removeEventListener('click', el._copyHandler)
      delete el._copyHandler
    }
  }
}

export default copyDirective
