import { runWithFeedbackHost } from '@/components/feedback/feedback-host'

const DEFAULT_TOAST = {
  type: 'info',
  duration: 2200,
}

const DEFAULT_NOTIFY = {
  title: '',
  description: '',
  type: 'info',
  duration: 3600,
}

const TOAST_METHOD = {
  success: 'success',
  warning: 'warning',
  error: 'error',
  info: 'info',
  loading: 'loading',
}

const NOTIFY_TYPE = {
  success: 'success',
  warning: 'warning',
  error: 'danger',
  danger: 'danger',
  info: 'primary',
  primary: 'primary',
}

export function toast(message, options = {}) {
  const config = { ...DEFAULT_TOAST, ...options }
  const method = TOAST_METHOD[config.type] || 'info'
  runWithFeedbackHost((feedback) => {
    feedback.toast[method]({
      msg: String(message || ''),
      duration: config.duration,
      direction: 'horizontal',
      position: config.position || 'middle-top',
      zIndex: 12000,
      cover: config.cover === true,
    })
  })
}

export function notify(options = {}) {
  const config = { ...DEFAULT_NOTIFY, ...options }
  const message = [config.title, config.description].filter(Boolean).join('：')
  runWithFeedbackHost((feedback) => {
    feedback.notify.showNotify({
      message,
      type: NOTIFY_TYPE[config.type] || 'primary',
      duration: config.duration,
      zIndex: 12000,
      rootPortal: true,
    })
  })
}

export function showToastMessage(message, options = {}) {
  toast(message, options)
}

export function showNotifyMessage(options = {}) {
  notify(options)
}
