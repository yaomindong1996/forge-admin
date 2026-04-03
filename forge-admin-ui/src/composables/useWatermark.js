import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { getWatermarkConfig } from '@/api/config'
import { WHITE_LIST } from '@/config/whitelist.config.js'
import { useAuthStore, useUserStore } from '@/store'

/**
 * 水印配置
 */
const defaultConfig = {
  enable: false,
  content: '系统水印',
  contentType: 'text', // text: 固定文本, dict: 字典配置
  opacity: 0.1,
  fontSize: 16,
  fontColor: '#cccccc',
  rotate: -20,
  gapX: 200,
  gapY: 200,
  offsetX: 0,
  offsetY: 0,
  zIndex: 1000,
  showTimestamp: false,
  timestampFormat: 'yyyy-MM-dd HH:mm:ss',
}

/**
 * 水印内容解析器映射
 * key: 字典code, value: 解析函数，返回显示文本
 */
const contentParsers = {
  // 姓名+手机号
  name_phone: (userStore) => {
    const name = userStore.realName || userStore.username || ''
    const phone = userStore.phone || ''
    return phone ? `${name} ${phone}` : name
  },
  // 姓名
  name: (userStore) => {
    return userStore.realName || userStore.username || ''
  },
  // 用户名
  username: (userStore) => {
    return userStore.username || ''
  },
  // 手机号
  phone: (userStore) => {
    return userStore.phone || ''
  },
  // 邮箱
  email: (userStore) => {
    return userStore.email || ''
  },
  // 用户ID
  user_id: (userStore) => {
    return String(userStore.userId || '')
  },
}

/**
 * 格式化时间戳
 */
function formatDate(date, format) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return format
    .replace('yyyy', year)
    .replace('MM', month)
    .replace('dd', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 创建水印
 */
function createWatermarkCanvas(config) {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')

  // 计算文本尺寸
  const font = `${config.fontSize}px Arial`
  ctx.font = font
  const textMetrics = ctx.measureText(config.content)
  const textWidth = textMetrics.width
  const textHeight = config.fontSize

  // 计算旋转后的画布尺寸
  const angle = (config.rotate * Math.PI) / 180
  const sin = Math.abs(Math.sin(angle))
  const cos = Math.abs(Math.cos(angle))

  // 画布尺寸 = 文本尺寸 + 间距
  const canvasWidth = textWidth * cos + textHeight * sin + config.gapX
  const canvasHeight = textWidth * sin + textHeight * cos + config.gapY

  canvas.width = canvasWidth
  canvas.height = canvasHeight

  // 绘制水印文本
  ctx.font = font
  ctx.fillStyle = config.fontColor
  ctx.globalAlpha = config.opacity
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  // 保存上下文状态
  ctx.save()

  // 移动到画布中心并旋转
  const centerX = (textWidth * cos + textHeight * sin) / 2 + config.offsetX
  const centerY = (textWidth * sin + textHeight * cos) / 2 + config.offsetY
  ctx.translate(centerX, centerY)
  ctx.rotate(angle)

  // 绘制文本
  let displayContent = config.content
  if (config.showTimestamp) {
    const timestamp = formatDate(new Date(), config.timestampFormat)
    displayContent += `\n${timestamp}`
  }

  const lines = displayContent.split('\n')
  lines.forEach((line, index) => {
    const yOffset = (index - (lines.length - 1) / 2) * config.fontSize * 1.2
    ctx.fillText(line, 0, yOffset)
  })

  ctx.restore()

  return canvas.toDataURL('image/png')
}

/**
 * 获取水印显示内容
 */
function getWatermarkDisplayContent(config, userStore) {
  // 如果内容类型是字典配置
  if (config.contentType === 'dict' || contentParsers[config.content]) {
    const parser = contentParsers[config.content]
    if (parser && userStore.userInfo) {
      return parser(userStore)
    }
    // 如果没有对应的解析器或用户未登录，返回空字符串
    return ''
  }
  // 固定文本
  return config.content || ''
}

/**
 * 水印 composable
 */
export function useWatermark() {
  const userStore = useUserStore()
  const authStore = useAuthStore()
  const watermarkConfig = ref({ ...defaultConfig })
  const watermarkUrl = ref('')
  let refreshTimer = null

  // 计算实际显示的水印内容
  const displayContent = computed(() => {
    return getWatermarkDisplayContent(watermarkConfig.value, userStore)
  })

  /**
   * 加载水印配置
   */
  const loadWatermarkConfig = async () => {
    // 检查是否需要加载水印配置
    // 1. 必须有 token
    const hasToken = authStore.accessToken
    if (!hasToken) {
      return
    }

    // 2. 当前路由不在白名单中
    const currentPath = window.location.pathname
    if (WHITE_LIST.some(path => currentPath.startsWith(path))) {
      return
    }

    try {
      const res = await getWatermarkConfig()
      if (res.code === 200 && res.data) {
        watermarkConfig.value = { ...defaultConfig, ...res.data }
        updateWatermark()
      }
    }
    catch (error) {
      console.error('加载水印配置失败:', error)
    }
  }

  /**
   * 更新水印
   */
  const updateWatermark = () => {
    if (!watermarkConfig.value.enable) {
      watermarkUrl.value = ''
      return
    }

    // 如果没有显示内容，不显示水印
    const content = displayContent.value
    if (!content) {
      watermarkUrl.value = ''
      return
    }

    // 创建带实际显示内容的水印配置
    const configWithContent = {
      ...watermarkConfig.value,
      content,
    }

    const dataUrl = createWatermarkCanvas(configWithContent)
    watermarkUrl.value = dataUrl
  }

  /**
   * 启动定时刷新（用于更新时间戳）
   */
  const startRefreshTimer = () => {
    stopRefreshTimer()
    if (watermarkConfig.value.enable && watermarkConfig.value.showTimestamp) {
      refreshTimer = setInterval(() => {
        updateWatermark()
      }, 1000) // 每秒刷新一次
    }
  }

  /**
   * 停止定时刷新
   */
  const stopRefreshTimer = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }

  /**
   * 获取水印样式
   */
  const getWatermarkStyle = () => {
    if (!watermarkConfig.value.enable || !watermarkUrl.value) {
      return {}
    }

    return {
      position: 'fixed',
      top: 0,
      left: 0,
      width: '100%',
      height: '100%',
      pointerEvents: 'none',
      zIndex: watermarkConfig.value.zIndex,
      backgroundImage: `url(${watermarkUrl.value})`,
      backgroundRepeat: 'repeat',
    }
  }

  // 监听配置变化
  watch(
    () => watermarkConfig.value,
    () => {
      updateWatermark()
      startRefreshTimer()
    },
    { deep: true },
  )

  // 监听用户登录状态变化，更新水印内容
  watch(
    () => userStore.userInfo,
    () => {
      if (watermarkConfig.value.enable) {
        updateWatermark()
      }
    },
    { deep: true },
  )

  // 组件挂载时加载配置
  onMounted(() => {
    loadWatermarkConfig()
  })

  // 组件卸载时清理定时器
  onUnmounted(() => {
    stopRefreshTimer()
  })

  return {
    watermarkConfig,
    watermarkUrl,
    displayContent,
    loadWatermarkConfig,
    updateWatermark,
    getWatermarkStyle,
    // 注册自定义内容解析器
    registerContentParser: (code, parser) => {
      contentParsers[code] = parser
    },
  }
}

export default useWatermark
