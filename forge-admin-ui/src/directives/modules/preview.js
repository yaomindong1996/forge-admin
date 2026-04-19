import { previewImage } from '@/utils/imagePreview'

/**
 * v-preview 指令
 * 用于图片点击预览
 *
 * 用法：
 * 1. 基础用法：<img v-preview src="图片.jpg" />
 * 2. 指定预览图：<img v-preview="'大图.jpg'" src="缩略图.jpg" />
 */
const previewDirective = {
  mounted(el, binding) {
    // 确保元素是图片
    if (el.tagName !== 'IMG') {
      console.warn('v-preview 指令只能用于 <img> 元素')
      return
    }

    // 添加点击事件
    el._previewHandler = () => {
      // 优先使用指令绑定的值，否则使用图片的 src
      const previewSrc = binding.value || el.src

      if (!previewSrc) {
        console.warn('v-preview: 没有找到可预览的图片地址')
        return
      }

      previewImage(previewSrc)
    }

    // 添加样式，表示可点击
    el.style.cursor = 'pointer'

    // 绑定点击事件
    el.addEventListener('click', el._previewHandler)
  },

  updated(el, binding) {
    // 更新处理函数
    if (el._previewHandler) {
      el.removeEventListener('click', el._previewHandler)

      el._previewHandler = () => {
        const previewSrc = binding.value || el.src

        if (!previewSrc) {
          console.warn('v-preview: 没有找到可预览的图片地址')
          return
        }

        previewImage(previewSrc)
      }

      el.addEventListener('click', el._previewHandler)
    }
  },

  unmounted(el) {
    // 清理事件监听
    if (el._previewHandler) {
      el.removeEventListener('click', el._previewHandler)
      delete el._previewHandler
    }
  },
}

export default previewDirective
