import { PrintError } from '../protocol/types'
import { mmToPx } from '../protocol/units'

export async function exportPrintPdf(result, options = {}) {
  const { toPng } = await import('html-to-image')
  const { jsPDF } = await import('jspdf')
  const session = options.sources?.length ? null : await createFallbackSession(result, options)
  try {
    const pages = options.sources?.length
      ? [...options.sources]
      : [...session.document.querySelectorAll('[data-print-page]')]
    if (!pages.length)
      throw new PrintError('PDF_UNAVAILABLE', '没有可导出的页面')
    const widthMm = result.geometry.widthMm
    const heightMm = result.geometry.heightMm
    const widthPx = Math.max(1, Math.round(mmToPx(widthMm)))
    const heightPx = Math.max(1, Math.round(mmToPx(heightMm)))
    const orientation = widthMm > heightMm ? 'landscape' : 'portrait'
    const PdfDocument = jsPDF
    const pdf = new PdfDocument({
      unit: 'mm',
      format: [widthMm, heightMm],
      orientation,
      compress: true,
    })
    for (let index = 0; index < pages.length; index++) {
      const page = pages[index]
      await waitForLayout(page, options.signal)
      const image = await toPng(page, {
        pixelRatio: 2,
        backgroundColor: '#ffffff',
        cacheBust: false,
        width: widthPx,
        height: heightPx,
        style: {
          transform: 'none',
          left: '0',
          top: '0',
          margin: '0',
          width: `${widthPx}px`,
          height: `${heightPx}px`,
        },
        filter: node => node === page || !isZeroBox(node),
      })
      if (index > 0)
        pdf.addPage([widthMm, heightMm], orientation)
      pdf.addImage(image, 'PNG', 0, 0, widthMm, heightMm)
    }
    const filename = options.filename || `打印${result.templateVersion ? `-v${result.templateVersion}` : ''}.pdf`
    pdf.save(filename)
    options.onEvent?.({ result: 'PDF_DOWNLOADED', pageCount: pages.length, filename })
    return { pageCount: pages.length, filename }
  }
  catch (error) {
    throw wrapPdfError(error)
  }
  finally {
    session?.dispose()
  }
}

function isZeroBox(el) {
  return !!el && el.nodeType === 1 && (el.offsetWidth === 0 || el.offsetHeight === 0)
}

function wrapPdfError(error) {
  if (error instanceof PrintError)
    return error
  const detail = String(error?.message || '').trim()
  return new PrintError('PDF_UNAVAILABLE', detail ? `导出 PDF 失败：${detail}` : '导出 PDF 失败，请稍后重试')
}

async function waitForLayout(el, signal) {
  if (typeof document !== 'undefined' && document.fonts?.ready)
    await document.fonts.ready.catch(() => undefined)
  for (let i = 0; i < 8; i++) {
    if (signal?.aborted)
      throw signal.reason
    if (el.offsetWidth > 0 && el.offsetHeight > 0)
      return
    await new Promise((resolve) => {
      if (typeof requestAnimationFrame === 'function')
        requestAnimationFrame(() => resolve())
      else
        resolve()
    })
  }
}

async function createFallbackSession(result, options) {
  const { createBrowserPrintSession } = await import('./browserPrint')
  return createBrowserPrintSession(result, { ...options, mode: 'pdf' })
}
