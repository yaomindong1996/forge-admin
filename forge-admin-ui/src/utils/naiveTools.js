import * as NaiveUI from 'naive-ui'
import { useAppStore } from '@/store'
import { isNullOrUndef } from '@/utils'
import { loadingService } from '@/directives/modules/loading'
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
      setTimeout(() => {
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

// 设置全屏 Loading
export function setupLoading() {
  let loadingInstance = null

  return {
    // 打开遮罩层
    show(options) {
      const config = typeof options === 'string'
          ? { text: options }
          : options || {}

      loadingInstance = loadingService.show({
        text: config.text || '加载中...',
        background: config.background || '0, 0, 0, 0.7',
        color: config.color,
        fontSize: config.fontSize,
      })

      return loadingInstance
    },

    // 关闭遮罩层
    close() {
      loadingService.close()
      loadingInstance = null
    },

    // 兼容 ElementUI 的 loading 方法名
    service(options) {
      return this.show(options)
    },
  }
}
