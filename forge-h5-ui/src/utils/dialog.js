import { runWithFeedbackHost } from '@/components/feedback/feedback-host'

const DEFAULT_OPTIONS = {
  title: '提示',
  description: '',
  confirmText: '确定',
  cancelText: '取消',
  buttonText: '知道了',
  isDestructive: false,
}

function normalizeOptions(options = {}) {
  return {
    ...DEFAULT_OPTIONS,
    ...options,
    description: options.description ?? options.content ?? DEFAULT_OPTIONS.description,
  }
}

function wotButtonProps(isDestructive) {
  return isDestructive
    ? { type: 'error', plain: false, round: false }
    : { type: 'primary', plain: false, round: false }
}

export async function showConfirmDialog(options = {}) {
  const config = normalizeOptions(options)
  try {
    const result = await runWithFeedbackHost(feedback => feedback.message.confirm({
      title: config.title,
      msg: config.description,
      confirmButtonText: config.confirmText,
      cancelButtonText: config.cancelText,
      closeOnClickModal: false,
      zIndex: 12000,
      confirmButtonProps: wotButtonProps(config.isDestructive),
      cancelButtonProps: { type: 'info', plain: true, round: false },
    }))
    return result?.action === 'confirm'
  }
  catch {
    return false
  }
}

export async function showAlertDialog(options = {}) {
  const config = normalizeOptions(options)
  try {
    await runWithFeedbackHost(feedback => feedback.message.alert({
      title: config.title,
      msg: config.description,
      confirmButtonText: config.buttonText,
      closeOnClickModal: false,
      zIndex: 12000,
      confirmButtonProps: wotButtonProps(false),
    }))
    return true
  }
  catch {
    return false
  }
}

export async function showPromptDialog(options = {}) {
  const config = normalizeOptions(options)
  try {
    const result = await runWithFeedbackHost(feedback => feedback.message.prompt({
      title: config.title,
      msg: config.description,
      inputValue: config.value || '',
      inputPlaceholder: config.placeholder || '',
      confirmButtonText: config.confirmText,
      cancelButtonText: config.cancelText,
      closeOnClickModal: false,
      zIndex: 12000,
      confirmButtonProps: wotButtonProps(false),
      cancelButtonProps: { type: 'info', plain: true, round: false },
    }))
    return result?.action === 'confirm' ? String(result.value ?? '') : null
  }
  catch {
    return null
  }
}

export function showActionSheetDialog(options = {}) {
  return runWithFeedbackHost(feedback => feedback.actionSheet(options))
}
