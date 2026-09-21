import { createApp, h, nextTick } from 'vue'
import { abortable } from '../engine/resources'
import { PRINT_LIMITS, PrintError } from '../protocol/types'
import { mmToPx } from '../protocol/units'
import PrintPage from './PrintPage.vue'

function frameStyle(result) {
  const widthPx = Math.max(320, Math.ceil(mmToPx(result.geometry.widthMm)) + 24)
  const heightPx = Math.max(320, Math.ceil(mmToPx(result.geometry.heightMm) * Math.max(result.pages.length, 1)) + 24)
  return `position:fixed;left:-100000px;top:0;width:${widthPx}px;height:${heightPx}px;border:0;`
}

/** A disposable document owns all print CSS; the application page is never restyled. */
export async function createBrowserPrintSession(result, options = {}) {
  const owner = options.document || document
  const pdf = options.mode === 'pdf'
  const frame = owner.createElement('iframe')
  frame.dataset.forgePrint = ''
  frame.title = pdf ? 'PDF 文档' : '打印文档'
  frame.style.cssText = frameStyle(result)
  const controller = new AbortController()
  const cancel = () => controller.abort(new PrintError('PRINT_CANCELLED', '打印会话已取消'))
  options.signal?.addEventListener('abort', cancel, { once: true })
  if (options.signal?.aborted) {
    cancel()
  }
  const timer = setTimeout(() => controller.abort(new PrintError('RESOURCE_TIMEOUT', '打印文档准备超时')), PRINT_LIMITS.resourceTimeoutMs)
  let app
  let disposed = false
  let printWindow
  const dispose = () => {
    if (disposed) {
      return
    }
    disposed = true
    clearTimeout(timer)
    options.signal?.removeEventListener('abort', cancel)
    controller.signal.removeEventListener('abort', dispose)
    printWindow?.removeEventListener('afterprint', dispose)
    app?.unmount()
    frame.remove()
  }
  try {
    if (controller.signal.aborted) {
      throw controller.signal.reason
    }
    owner.body.append(frame)
    const paperDocument = frame.contentDocument
    printWindow = frame.contentWindow
    if (!paperDocument || !printWindow) {
      throw new PrintError('PRINT_UNAVAILABLE', '浏览器无法创建打印文档')
    }
    // 不拷贝宿主页全局 CSS：Vue scoped 样式进不了 iframe，布局一律靠组件内联 style。
    const style = paperDocument.createElement('style')
    style.textContent = `@page { size: ${result.geometry.widthMm}mm ${result.geometry.heightMm}mm; margin: 0; }
html, body { margin: 0; padding: 0; background: white; color: #000; }
* { box-sizing: border-box; }
img { max-width: 100%; }
[data-print-page] { break-after: page; page-break-after: always; }
[data-print-page]:last-child { break-after: auto; page-break-after: auto; }`
    paperDocument.head.append(style)
    const host = paperDocument.createElement('div')
    paperDocument.body.append(host)
    app = createApp({ render: () => result.pages.map(page => h(PrintPage, {
      key: page.number,
      page,
      geometry: result.geometry,
      watermark: result.watermark,
      overlay: result.overlay?.print ? result.overlay : null,
    })) })
    app.mount(host)
    await nextTick()
    if (paperDocument.fonts) {
      await abortable(paperDocument.fonts.ready, controller.signal)
    }
    await abortable(Promise.all([...paperDocument.images].map(image => image.decode().catch(() => undefined))), controller.signal)
    clearTimeout(timer)
    controller.signal.addEventListener('abort', dispose, { once: true })
    if (!pdf)
      printWindow.addEventListener('afterprint', dispose, { once: true })
    return {
      dispose,
      document: paperDocument,
      print() {
        if (disposed) {
          throw new PrintError('PRINT_SESSION_CLOSED', '打印会话已关闭')
        }
        try {
          printWindow.focus()
          printWindow.print()
          options.onEvent?.({ result: 'DIALOG_OPENED', pageCount: result.pages.length })
        }
        catch {
          dispose()
          throw new PrintError('PRINT_UNAVAILABLE', '无法打开浏览器打印对话框')
        }
      },
    }
  }
  catch (error) {
    dispose()
    throw error
  }
}
