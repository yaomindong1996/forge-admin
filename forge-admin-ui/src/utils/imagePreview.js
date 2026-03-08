import { createApp, h, ref, onMounted, nextTick } from 'vue'
import { NImageGroup, NImage } from 'naive-ui'

/**
 * 图片预览工具函数
 * 基于 Naive UI 的图片预览功能
 */

/**
 * 预览单张图片
 * @param {string} src - 图片地址
 */
export function previewImage(src) {
  if (!src) {
    console.warn('previewImage: 图片地址不能为空')
    return
  }

  previewImages([src])
}

/**
 * 预览多张图片
 * @param {Array<string>} images - 图片地址数组
 * @param {number} initialIndex - 初始显示的图片索引，默认为 0
 */
export function previewImages(images, initialIndex = 0) {
  if (!images || !Array.isArray(images) || images.length === 0) {
    console.warn('previewImages: 图片数组不能为空')
    return
  }

  // 创建一个临时容器
  const container = document.createElement('div')
  container.style.cssText = 'position: fixed; top: 0; left: 0; pointer-events: none; z-index: -1;'
  document.body.appendChild(container)

  // 创建 Vue 应用实例
  const app = createApp({
    setup() {
      const imageRefs = ref([])

      onMounted(() => {
        // 使用 nextTick 确保 DOM 已渲染
        nextTick(() => {
          // 触发指定索引的图片点击以打开预览
          if (imageRefs.value[initialIndex]) {
            const imgElement = imageRefs.value[initialIndex].$el || imageRefs.value[initialIndex]
            if (imgElement && imgElement.querySelector) {
              const img = imgElement.querySelector('img')
              if (img) {
                img.click()
              }
            }
          }
        })
      })

      return () => h(
          NImageGroup,
          {},
          {
            default: () => images.map((src, index) =>
                h(NImage, {
                  ref: el => {
                    if (el) imageRefs.value[index] = el
                  },
                  src,
                  style: { width: '1px', height: '1px', opacity: 0 }
                })
            )
          }
      )
    }
  })

  // 挂载应用
  app.mount(container)

  // 清理函数
  const cleanup = () => {
    setTimeout(() => {
      app.unmount()
      if (container.parentNode) {
        document.body.removeChild(container)
      }
    }, 500)
  }

  // 监听关闭事件（通过 MutationObserver 检测预览关闭）
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.removedNodes.length > 0) {
        mutation.removedNodes.forEach((node) => {
          if (node.classList && node.classList.contains('n-image-preview-container')) {
            cleanup()
            observer.disconnect()
          }
        })
      }
    })
  })

  observer.observe(document.body, { childList: true })

  return app
}

/**
 * 全局图片预览方法
 * 使用方式：
 * window.$imagePreview('图片.jpg')
 * window.$imagePreview(['图片1.jpg', '图片2.jpg'], 1)
 */
export function setupImagePreview() {
  return {
    // 预览单张图片
    show(src) {
      previewImage(src)
    },

    // 预览多张图片
    showList(images, initialIndex = 0) {
      previewImages(images, initialIndex)
    }
  }
}
