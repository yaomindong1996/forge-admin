import { NWatermark } from 'naive-ui'
/**
 * 水印工具
 * 基于 Naive UI 的 NWatermark 组件实现
 */
import { createApp, h } from 'vue'

/**
 * 创建全屏水印
 * @param {object} options - 水印配置选项
 * @param {string} options.content - 水印文本内容
 * @param {string} options.image - 水印图片地址
 * @param {number} options.fontSize - 字体大小，默认 14
 * @param {string} options.fontColor - 字体颜色，默认 'rgba(128, 128, 128, .3)'
 * @param {number} options.width - 水印宽度，默认 32
 * @param {number} options.height - 水印高度，默认 32
 * @param {number} options.xGap - 水印水平间距，默认 0
 * @param {number} options.yGap - 水印垂直间距，默认 0
 * @param {number} options.rotate - 水印旋转角度，默认 0
 * @param {number} options.zIndex - 水印层级，默认 10
 * @param {boolean} options.fullscreen - 是否全屏，默认 true
 * @param {boolean} options.selectable - 文本是否可选，默认 true
 * @returns {object} 返回包含 destroy 方法的对象，用于销毁水印
 */
export function createWatermark(options = {}) {
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
    zIndex = 1000,
    fullscreen = true,
    selectable = false,
    ...restProps
  } = options

  // 创建挂载容器
  const container = document.createElement('div')
  container.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: ${zIndex};
  `
  document.body.appendChild(container)

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
        zIndex,
        fullscreen,
        selectable,
        ...restProps,
      })
    },
  })

  // 挂载应用
  app.mount(container)

  // 返回销毁方法
  return {
    destroy() {
      app.unmount()
      document.body.removeChild(container)
    },
  }
}

/**
 * 全局水印方法设置
 * @returns {object} 返回全局水印方法
 */
export function setupWatermark() {
  let watermarkInstance = null

  return {
    /**
     * 显示全屏水印
     * @param {string | object} options - 水印内容或配置对象
     */
    show(options) {
      // 如果已存在水印，先销毁
      if (watermarkInstance) {
        watermarkInstance.destroy()
      }

      // 支持字符串快捷方式
      const config = typeof options === 'string' ? { content: options } : options

      watermarkInstance = createWatermark(config)
      return watermarkInstance
    },

    /**
     * 隐藏全屏水印
     */
    hide() {
      if (watermarkInstance) {
        watermarkInstance.destroy()
        watermarkInstance = null
      }
    },

    /**
     * 更新水印（先销毁再创建）
     * @param {string | object} options - 新的水印配置
     */
    update(options) {
      return this.show(options)
    },
  }
}
