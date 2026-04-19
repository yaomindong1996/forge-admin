import { NWatermark } from 'naive-ui'
/**
 * v-watermark 指令
 * 为元素添加水印效果
 *
 * 使用方式：
 * 1. 字符串形式：v-watermark="'水印文字'"
 * 2. 对象形式：v-watermark="{ content: '水印文字', fontSize: 16, rotate: -22 }"
 *
 * 示例：
 * <div v-watermark="'机密文件'">内容区域</div>
 * <div v-watermark="{ content: '内部资料', fontSize: 20, fontColor: 'rgba(255, 0, 0, .1)' }">内容区域</div>
 */
import { createApp, h } from 'vue'

// 存储每个元素的水印实例
const watermarkMap = new WeakMap()

/**
 * 创建水印实例
 * @param {HTMLElement} el - 目标元素
 * @param {string | object} binding - 指令绑定值
 */
function createWatermarkInstance(el, binding) {
  // 销毁旧实例
  destroyWatermarkInstance(el)

  const value = binding.value
  if (!value)
    return

  // 解析配置
  const config = typeof value === 'string' ? { content: value } : value

  const {
    content = '',
    image = '',
    fontSize = 16,
    fontColor = 'rgba(128, 128, 128, .15)',
    width = 120,
    height = 64,
    xGap = 12,
    yGap = 60,
    rotate = -22,
    zIndex = 1,
    selectable = false,
    ...restProps
  } = config

  // 确保元素有定位上下文
  const position = window.getComputedStyle(el).position
  if (position === 'static') {
    el.style.position = 'relative'
  }

  // 创建水印容器
  const container = document.createElement('div')
  container.style.cssText = `
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: ${zIndex};
    overflow: hidden;
  `
  el.appendChild(container)

  // 创建 Vue 应用实例
  const app = createApp({
    render() {
      return h(NWatermark, {
        content,
        image,
        fontSize,
        fontColor,
        width,
        height,
        xGap,
        yGap,
        rotate,
        selectable,
        fullscreen: false, // 指令模式下不使用全屏
        ...restProps,
      })
    },
  })

  app.mount(container)

  // 保存实例信息
  watermarkMap.set(el, {
    app,
    container,
  })
}

/**
 * 销毁水印实例
 * @param {HTMLElement} el - 目标元素
 */
function destroyWatermarkInstance(el) {
  const instance = watermarkMap.get(el)
  if (instance) {
    instance.app.unmount()
    if (instance.container.parentNode) {
      instance.container.parentNode.removeChild(instance.container)
    }
    watermarkMap.delete(el)
  }
}

export default {
  mounted(el, binding) {
    createWatermarkInstance(el, binding)
  },

  updated(el, binding) {
    // 只有当值发生变化时才重新创建
    if (binding.value !== binding.oldValue) {
      createWatermarkInstance(el, binding)
    }
  },

  unmounted(el) {
    destroyWatermarkInstance(el)
  },
}
