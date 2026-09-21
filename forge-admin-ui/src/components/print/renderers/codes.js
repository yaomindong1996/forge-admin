import JsBarcode from 'jsbarcode'
import QRCodeVue3 from 'qrcode-vue3'
import { createApp, h } from 'vue'
import { PrintError } from '../protocol/types'
import { mmToPx } from '../protocol/units'

/** Use the public QR component and materialize its image before measuring or printing. */
export async function encodePrintCode(node, signal, document = globalThis.document) {
  if (!node.text || node.text.length > 2000) {
    throw new PrintError('INVALID_CODE', '条码或二维码内容为空或过长', node.id)
  }
  if (node.type === 'BARCODE') {
    try {
      const canvas = document.createElement('canvas')
      const showText = node.showCodeText !== false
      const barHeight = Math.max(20, Math.ceil(mmToPx(node.heightMm) * 3 * (showText ? 0.72 : 1)))
      JsBarcode(canvas, node.text, {
        format: node.barcodeFormat || 'CODE128',
        displayValue: showText,
        fontSize: Math.max(18, Math.round(mmToPx(2.2) * 3)),
        textMargin: 4,
        width: 3,
        height: barHeight,
        margin: 8,
      })
      return canvas.toDataURL('image/png')
    }
    catch {
      throw new PrintError('INVALID_CODE', '条码内容不符合编码规则', node.id)
    }
  }
  const host = document.createElement('div')
  host.style.cssText = 'position:fixed;left:-100000px;top:0;visibility:hidden;'
  document.body.append(host)
  let app
  let observer
  let cancel
  try {
    return await new Promise((resolve, reject) => {
      cancel = () => reject(new PrintError('RESOURCE_CANCELLED', '编码生成已取消', node.id))
      signal?.addEventListener('abort', cancel, { once: true })
      if (signal?.aborted) {
        cancel()
        return
      }
      observer = new MutationObserver(() => {
        const image = host.querySelector('img')
        if (image?.src?.startsWith('data:image/png;')) {
          resolve(image.src)
        }
      })
      observer.observe(host, { subtree: true, childList: true, attributes: true })
      app = createApp({ render: () => h(QRCodeVue3, {
        value: node.text,
        width: Math.ceil(mmToPx(Math.min(node.widthMm, node.heightMm)) * 3),
        height: Math.ceil(mmToPx(Math.min(node.widthMm, node.heightMm)) * 3),
        margin: 16,
        dotsOptions: { type: 'square', color: '#000000' },
        cornersSquareOptions: { type: 'square', color: '#000000' },
        cornersDotOptions: { type: 'square', color: '#000000' },
        backgroundOptions: { color: '#ffffff' },
        fileExt: 'png',
      }) })
      app.config.errorHandler = () => reject(new PrintError('INVALID_CODE', '二维码生成失败', node.id))
      app.mount(host)
    })
  }
  finally {
    signal?.removeEventListener('abort', cancel)
    observer?.disconnect()
    app?.unmount()
    host.remove()
  }
}
