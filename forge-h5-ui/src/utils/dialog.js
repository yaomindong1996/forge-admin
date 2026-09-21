import { resolveStaticUrl } from '@/utils/assets'

const DEFAULT_OPTIONS = {
  title: '提示',
  description: '',
  icon: 'info',
  confirmText: '确定',
  cancelText: '取消',
  buttonText: '知道了',
  isDestructive: false,
}

const ICON_MAP = {
  success: '/static/icons/ai-icon/check-circle.svg',
  warning: '/static/icons/ai-icon/alert-triangle.svg',
  info: '/static/icons/ai-icon/info.svg',
}

const TONE_CLASS = {
  success: 'forge-dialog--success',
  warning: 'forge-dialog--warning',
  info: 'forge-dialog--info',
}

function ensureH5Document() {
  return typeof document !== 'undefined' && document.body
}

function normalizeOptions(options = {}) {
  return {
    ...DEFAULT_OPTIONS,
    ...options,
    icon: options.icon || DEFAULT_OPTIONS.icon,
    description: options.description ?? options.content ?? DEFAULT_OPTIONS.description,
  }
}

function createIcon(icon) {
  const iconUrl = resolveStaticUrl(ICON_MAP[icon] || ICON_MAP.info)
  const iconNode = document.createElement('div')
  iconNode.className = 'forge-dialog__icon'

  const mask = document.createElement('span')
  mask.className = 'forge-dialog__icon-mask'
  mask.style.webkitMask = `url(${iconUrl}) center / contain no-repeat`
  mask.style.mask = `url(${iconUrl}) center / contain no-repeat`
  iconNode.appendChild(mask)

  return iconNode
}

function createCloseButton(onClose) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = 'forge-dialog__close'
  button.setAttribute('aria-label', '关闭')
  button.addEventListener('click', onClose)
  return button
}

function createButton(text, className, onClick) {
  const button = document.createElement('button')
  button.type = 'button'
  button.className = className
  button.textContent = text
  button.addEventListener('click', onClick)
  return button
}

function showDomDialog(options, mode) {
  const config = normalizeOptions(options)

  return new Promise((resolve) => {
    const root = document.createElement('div')
    root.className = `forge-dialog ${TONE_CLASS[config.icon] || TONE_CLASS.info}`

    const backdrop = document.createElement('div')
    backdrop.className = 'forge-dialog__backdrop'

    const panel = document.createElement('div')
    panel.className = 'forge-dialog__panel'

    const glow = document.createElement('div')
    glow.className = 'forge-dialog__glow'

    const close = createCloseButton(() => closeDialog(false))
    const body = document.createElement('div')
    body.className = 'forge-dialog__body'

    const title = document.createElement('div')
    title.className = 'forge-dialog__title'
    title.textContent = config.title

    const desc = document.createElement('div')
    desc.className = 'forge-dialog__desc'
    desc.textContent = config.description

    body.appendChild(createIcon(config.icon))
    body.appendChild(title)
    body.appendChild(desc)

    const actions = document.createElement('div')
    actions.className = mode === 'confirm' ? 'forge-dialog__actions' : 'forge-dialog__actions forge-dialog__actions--single'

    if (mode === 'confirm') {
      actions.appendChild(createButton(config.cancelText, 'forge-dialog__button forge-dialog__button--cancel', () => closeDialog(false)))
      actions.appendChild(createButton(
        config.confirmText,
        `forge-dialog__button forge-dialog__button--confirm${config.isDestructive ? ' forge-dialog__button--danger' : ''}`,
        () => closeDialog(true),
      ))
    }
    else {
      actions.appendChild(createButton(config.buttonText, 'forge-dialog__button forge-dialog__button--dark', () => closeDialog(true)))
    }

    panel.appendChild(glow)
    panel.appendChild(close)
    panel.appendChild(body)
    panel.appendChild(actions)
    root.appendChild(backdrop)
    root.appendChild(panel)
    document.body.appendChild(root)

    requestAnimationFrame(() => root.classList.add('is-open'))
    backdrop.addEventListener('click', () => closeDialog(false))

    function closeDialog(confirmed) {
      root.classList.remove('is-open')
      root.classList.add('is-leaving')
      window.setTimeout(() => {
        root.remove()
        resolve(confirmed)
      }, 180)
    }
  })
}

function showNativeConfirm(options = {}) {
  const config = normalizeOptions(options)
  return new Promise((resolve) => {
    uni.showModal({
      title: config.title,
      content: config.description,
      confirmText: config.confirmText,
      cancelText: config.cancelText,
      confirmColor: config.isDestructive ? '#ef4444' : '#1f5fbf',
      success: res => resolve(!!res.confirm),
      fail: () => resolve(false),
    })
  })
}

function showNativeAlert(options = {}) {
  const config = normalizeOptions(options)
  return new Promise((resolve) => {
    uni.showModal({
      title: config.title,
      content: config.description,
      showCancel: false,
      confirmText: config.buttonText,
      confirmColor: '#0f172a',
      success: () => resolve(true),
      fail: () => resolve(false),
    })
  })
}

export function showConfirmDialog(options = {}) {
  if (!ensureH5Document()) {
    return showNativeConfirm(options)
  }
  return showDomDialog(options, 'confirm')
}

export function showAlertDialog(options = {}) {
  if (!ensureH5Document()) {
    return showNativeAlert(options)
  }
  return showDomDialog(options, 'alert')
}
