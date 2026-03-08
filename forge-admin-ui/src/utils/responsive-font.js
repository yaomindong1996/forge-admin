/**
 * 响应式字体大小工具
 * 通过CSS变量实现字体大小的自动缩放
 */
import {ref} from "vue";

const screenWidth = ref(window.innerWidth)
const fontScale = ref(1)

export const calculateFontScale = () => {
  const width = window.innerWidth
  screenWidth.value = width

  // 根据屏幕宽度计算字体缩放比例
  if (width <= 768) {
    fontScale.value = 0.8  // 小屏幕缩小到80%
  } else if (width <= 1024) {
    fontScale.value = 0.85  // 中等屏幕缩小到90%
  } else if (width <= 1440) {
    fontScale.value = 0.9    // 大屏幕正常大小
  } else {
    fontScale.value = 1  // 超大屏幕放大到110%
  }

  // 设置CSS变量
  document.documentElement.style.setProperty('--font-scale', fontScale.value)

  // 返回计算结果，供调用者使用
  return fontScale.value
}

/**
 * 初始化响应式字体
 * @param {Function} callback - 可选的回调函数，用于在字体缩放变化时执行自定义逻辑
 */
export function initResponsiveFont(callback) {
  // 初始设置
  const scale = calculateFontScale()
  if (typeof callback === 'function') {
    callback(scale)
  }

  // 监听窗口大小变化
  let resizeTimer
  window.addEventListener('resize', () => {
    // 防抖处理
    clearTimeout(resizeTimer)
    resizeTimer = setTimeout(() => {
      const scale = calculateFontScale()
      if (typeof callback === 'function') {
        callback(scale)
      }
    }, 100)
  })
}
