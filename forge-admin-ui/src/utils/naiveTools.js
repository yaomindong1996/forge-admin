import * as NaiveUI from 'naive-ui'
import { loadingService } from '@/directives/modules/loading'
import { useAppStore } from '@/store'
import { isNullOrUndef } from '@/utils'
import { copy } from './clipboard'
import { setupImagePreview } from './imagePreview'
import { setupWatermark } from './watermark'

export function setupMessage(NMessage) {
  class Message {
    static instance
    constructor() {
      // 单例模式
      if (Message.instance)
        return Message.instance
      Message.instance = this
      this.message = {}
      this.removeTimer = {}
    }

    removeMessage(key, duration = 5000) {
      this.removeTimer[key] && clearTimeout(this.removeTimer[key])
      this.removeTimer[key] = setTimeout(() => {
        this.message[key]?.destroy()
      }, duration)
    }

    destroy(key, duration = 200) {
      // 先取消同 key 已有的自动销毁计时，并跟踪本次销毁计时，
      // 避免紧随其后的同 key 消息（如 destroy loading 后立即 error）被连带销毁
      this.removeTimer[key] && clearTimeout(this.removeTimer[key])
      this.removeTimer[key] = setTimeout(() => {
        this.message[key]?.destroy()
      }, duration)
    }

    showMessage(type, content, option = {}) {
      if (Array.isArray(content)) {
        return content.forEach(msg => NMessage[type](msg, option))
      }

      if (!option.key) {
        return NMessage[type](content, option)
      }

      const currentMessage = this.message[option.key]
      if (currentMessage) {
        currentMessage.type = type
        currentMessage.content = content
      }
      else {
        this.message[option.key] = NMessage[type](content, {
          ...option,
          duration: 0,
          onAfterLeave: () => {
            delete this.message[option.key]
          },
        })
      }
      this.removeMessage(option.key, option.duration)
    }

    loading(content, option) {
      this.showMessage('loading', content, option)
    }

    success(content, option) {
      this.showMessage('success', content, option)
    }

    error(content, option) {
      this.showMessage('error', content, option)
    }

    info(content, option) {
      this.showMessage('info', content, option)
    }

    warning(content, option) {
      this.showMessage('warning', content, option)
    }
  }

  return new Message()
}

export function setupDialog(NDialog) {
  NDialog.confirm = function (option = {}) {
    const showIcon = !isNullOrUndef(option.title)
    return NDialog[option.type || 'warning']({
      showIcon,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: option.confirm,
      onNegativeClick: option.cancel,
      onMaskClick: option.cancel,
      ...option,
    })
  }

  return NDialog
}

export function setupNaiveDiscreteApi() {
  const appStore = useAppStore()
  const configProviderProps = computed(() => ({
    theme: appStore.isDark ? NaiveUI.darkTheme : undefined,
    themeOverrides: useAppStore().naiveThemeOverrides,
  }))
  const { message, dialog, notification, loadingBar } = NaiveUI.createDiscreteApi(
    ['message', 'dialog', 'notification', 'loadingBar'],
    { configProviderProps },
  )

  window.$loadingBar = loadingBar
  window.$notification = notification
  window.$message = setupMessage(message)
  window.$dialog = setupDialog(dialog)
  window.$loading = setupLoading()
  window.$copy = copy
  window.$imagePreview = setupImagePreview()
  window.$watermark = setupWatermark()
  window.$homePath = import.meta.env.VITE_HOME_PATH
}

// 设置全屏 Loading（短请求默认延迟展示，避免“闪一下”）
export function setupLoading() {
  let loadingInstance = null
  let showTimer = null

  function clearShowTimer() {
    if (showTimer) {
      clearTimeout(showTimer)
      showTimer = null
    }
  }

  function mountLoading(config) {
    loadingInstance = loadingService.show({
      text: config.text || '加载中...',
      background: config.background || '255, 255, 255, 0.55',
      color: config.color || '#333333',
      fontSize: config.fontSize,
    })
    return loadingInstance
  }

  return {
    // 打开遮罩层；delay 默认 280ms，接口更快时不展示，避免闪烁
    show(options) {
      const config = typeof options === 'string'
        ? { text: options }
        : options || {}
      const delay = Number.isFinite(config.delay) ? config.delay : 280

      clearShowTimer()
      if (delay <= 0) {
        return mountLoading(config)
      }

      showTimer = setTimeout(() => {
        showTimer = null
        mountLoading(config)
      }, delay)
      return null
    },

    // 关闭遮罩层
    close() {
      clearShowTimer()
      loadingService.close()
      loadingInstance = null
    },

    // 兼容 ElementUI 的 loading 方法名
    service(options) {
      return this.show(options)
    },
  }
}
