const MAX_VALUE_LENGTH = 2048

export function scanBarcode(options = {}) {
  const platform = detectPlatform()
  const globals = typeof globalThis === 'undefined' ? {} : globalThis
  if (platform === 'WECHAT_ENTERPRISE' && typeof globals.wx?.scanQRCode === 'function')
    return scanWeCom(globals.wx, options.signal).then(result => normalizeResult(result, platform))

  if (typeof uni !== 'undefined' && typeof uni.scanCode === 'function') {
    return scanUni(options)
      .then(result => normalizeResult(result, platform))
      .catch(error => {
        if (error?.code === 'SCAN_CANCELLED' || typeof BarcodeDetector === 'undefined' || typeof document === 'undefined') throw error
        return scanBrowserCamera(options).then(result => normalizeResult(result, platform))
      })
  }

  if (typeof BarcodeDetector !== 'undefined' && typeof document !== 'undefined')
    return scanBrowserCamera(options).then(result => normalizeResult(result, platform))

  return Promise.reject(scanError('SCAN_UNSUPPORTED', '当前环境不支持摄像头扫码，请手工输入条码', platform))
}

function detectPlatform() {
  const ua = typeof navigator === 'undefined' ? '' : navigator.userAgent || ''
  if (/wxwork/i.test(ua)) return 'WECHAT_ENTERPRISE'
  if (/android|iphone|ipad|mobile/i.test(ua)) return 'H5'
  return 'BROWSER'
}

function scanUni(options) {
  return new Promise((resolve, reject) => {
    let finished = false
    const finish = (handler, value) => {
      if (finished) return
      finished = true
      options.signal?.removeEventListener?.('abort', abort)
      handler(value)
    }
    const abort = () => finish(reject, scanError('SCAN_CANCELLED', '已取消扫码'))
    if (options.signal?.aborted) return abort()
    options.signal?.addEventListener?.('abort', abort, { once: true })
    try {
      uni.scanCode({
        scanType: ['barCode', 'qrCode'],
        success: result => finish(resolve, result),
        fail: error => finish(reject, normalizeScanError(error)),
      })
    }
    catch (error) {
      finish(reject, normalizeScanError(error))
    }
  })
}

function scanWeCom(wx, signal) {
  return new Promise((resolve, reject) => {
    let finished = false
    const finish = (handler, value) => {
      if (finished) return
      finished = true
      signal?.removeEventListener?.('abort', abort)
      handler(value)
    }
    const abort = () => finish(reject, scanError('SCAN_CANCELLED', '已取消扫码'))
    if (signal?.aborted) return abort()
    signal?.addEventListener?.('abort', abort, { once: true })
    wx.scanQRCode({
      needResult: 1,
      scanType: ['barCode', 'qrCode'],
      success: result => finish(resolve, result),
      fail: error => finish(reject, normalizeScanError(error)),
    })
  })
}

function scanBrowserCamera({ timeoutMs = 30000, signal } = {}) {
  return new Promise((resolve, reject) => {
    const shell = document.createElement('div')
    Object.assign(shell.style, { position: 'fixed', inset: '0', zIndex: '2147483000', display: 'grid', placeItems: 'center', padding: '18px', background: 'rgba(15,23,42,.8)' })
    const panel = document.createElement('div')
    Object.assign(panel.style, { width: 'min(460px,100%)', overflow: 'hidden', borderRadius: '6px', background: '#1d2129', color: '#fff' })
    const video = document.createElement('video')
    Object.assign(video, { autoplay: true, muted: true, playsInline: true })
    Object.assign(video.style, { display: 'block', width: '100%', minHeight: '260px', background: '#020617', objectFit: 'cover' })
    const footer = document.createElement('div')
    Object.assign(footer.style, { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px', fontSize: '14px' })
    const hint = document.createElement('span'); hint.textContent = '请将商品条码放入取景框'
    const cancel = document.createElement('button'); cancel.type = 'button'; cancel.textContent = '取消'
    Object.assign(cancel.style, { minHeight: '44px', padding: '10px 16px', border: '1px solid #4e5969', borderRadius: '6px', color: '#fff', background: 'transparent' })
    footer.append(hint, cancel); panel.append(video, footer); shell.append(panel); document.body.append(shell)
    let settled = false; let stream; let timer; let raf; let detector
    const cleanup = () => { clearTimeout(timer); cancelAnimationFrame(raf); stream?.getTracks?.().forEach(track => track.stop()); video.srcObject = null; shell.remove(); signal?.removeEventListener?.('abort', abort) }
    const finish = (handler, value) => { if (settled) return; settled = true; cleanup(); handler(value) }
    const abort = () => finish(reject, scanError('SCAN_CANCELLED', '已取消扫码'))
    cancel.addEventListener('click', abort, { once: true })
    signal?.addEventListener?.('abort', abort, { once: true })
    if (signal?.aborted) return abort()
    timer = setTimeout(() => finish(reject, scanError('SCAN_TIMEOUT', '扫码超时，请重试')), Math.min(60000, Math.max(1000, Number(timeoutMs) || 30000)))
    Promise.resolve().then(async () => {
      try {
        detector = new BarcodeDetector({ formats: ['code_128', 'ean_13', 'ean_8', 'upc_a', 'qr_code'] })
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false })
        video.srcObject = stream
        await video.play()
        const detect = async () => {
          if (settled) return
          try {
            const results = await detector.detect(video)
            const result = results?.find(item => String(item.rawValue || '').trim())
            if (result) return finish(resolve, { value: result.rawValue, type: result.format || 'UNKNOWN' })
          }
          catch {}
          raf = requestAnimationFrame(detect)
        }
        raf = requestAnimationFrame(detect)
      }
      catch (error) {
        finish(reject, scanError(error?.name === 'NotAllowedError' ? 'SCAN_PERMISSION_DENIED' : 'SCAN_FAILED', '扫码失败，请重试', error))
      }
    })
  })
}

function normalizeResult(result, platform) {
  const value = String(result?.resultStr || result?.codeString || result?.value || result || '').trim()
  if (!value || value.length > MAX_VALUE_LENGTH)
    throw scanError('SCAN_INVALID_RESULT', '扫码结果无效', platform)
  return { value, type: String(result?.scanType || result?.type || 'UNKNOWN'), platform }
}

function normalizeScanError(error) {
  const code = String(error?.code || error?.errCode || '').toUpperCase()
  if (code.includes('CANCEL') || String(error?.errMsg || error?.message || '').includes('取消')) return scanError('SCAN_CANCELLED', '已取消扫码', error)
  return scanError('SCAN_FAILED', '扫码失败，请重试', error)
}

function scanError(code, message, cause) { const error = new Error(message || code); error.code = code; if (cause) error.cause = cause; return error }
