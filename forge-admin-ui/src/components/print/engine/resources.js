import { readOwnPath, resolveBinding, resolveCollection } from '../protocol/binding'
import { formatValue } from '../protocol/formatters'
import { PRINT_LIMITS, PrintError } from '../protocol/types'
import { isSafeImageReference } from '../protocol/validate'
import { encodePrintCode } from '../renderers/codes'
import { hasGenericFontFallback, requireLocalFont } from './fonts'

const PRINT_IMAGE_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp'])
const MAX_IMAGE_BYTES = 10 * 1024 * 1024

function normalizeImageReference(value) {
  if (value == null)
    return ''
  if (typeof value === 'number' && Number.isFinite(value))
    return String(Math.trunc(value))
  if (typeof value !== 'string')
    return ''
  const text = value.trim()
  if (!text || text === 'null' || text === 'undefined')
    return ''
  return text
}

function sniffImageType(bytes) {
  if (bytes.length >= 8 && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E && bytes[3] === 0x47)
    return 'image/png'
  if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF)
    return 'image/jpeg'
  if (bytes.length >= 12 && bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46
    && bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50) {
    return 'image/webp'
  }
  return ''
}

async function readBlobBytes(blob, limit = 16) {
  const slice = typeof blob.slice === 'function' ? blob.slice(0, limit) : blob
  if (typeof slice.arrayBuffer === 'function')
    return new Uint8Array(await slice.arrayBuffer())
  return await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(new Uint8Array(reader.result || []))
    reader.onerror = () => reject(reader.error || new Error('read blob'))
    reader.readAsArrayBuffer(slice)
  })
}

async function asPrintImageBlob(blob) {
  if (!(blob instanceof Blob) || blob.size > MAX_IMAGE_BYTES)
    return null
  const declared = String(blob.type || '').split(';')[0].trim().toLowerCase()
  if (PRINT_IMAGE_TYPES.has(declared))
    return blob
  try {
    const type = sniffImageType(await readBlobBytes(blob))
    return type ? new Blob([blob], { type }) : null
  }
  catch {
    return null
  }
}

export function abortable(promise, signal) {
  return new Promise((resolve, reject) => {
    const cancel = () => reject(signal.reason || new PrintError('RESOURCE_CANCELLED', '打印准备已取消'))
    if (signal.aborted) {
      cancel()
      return
    }
    signal.addEventListener('abort', cancel, { once: true })
    Promise.resolve(promise).then(resolve, reject).finally(() => signal.removeEventListener('abort', cancel))
  })
}

async function decodeImage(src, signal) {
  const image = new Image()
  image.src = src
  try {
    await abortable(image.decode(), signal)
    if (!image.naturalWidth || !image.naturalHeight) {
      throw new Error('empty image')
    }
  }
  finally {
    image.removeAttribute('src')
  }
}

export function tableImageResourceKey(sectionId, rowIndex, columnId) {
  return `table:${sectionId}:${rowIndex}:${columnId}`
}

export async function preparePrintResources(document, context, options = {}) {
  const controller = new AbortController()
  const { signal } = controller
  const cancel = () => controller.abort(new PrintError('RESOURCE_CANCELLED', '打印准备已取消'))
  options.signal?.addEventListener('abort', cancel, { once: true })
  if (options.signal?.aborted) {
    cancel()
  }
  const timer = setTimeout(() => controller.abort(new PrintError('RESOURCE_TIMEOUT', '打印资源准备超时')), options.timeoutMs ?? PRINT_LIMITS.resourceTimeoutMs)
  const images = new Map()
  const files = new Map()
  const urls = new Set()
  const revoke = options.revokeObjectURL || (url => URL.revokeObjectURL(url))
  const dispose = () => {
    urls.forEach(url => revoke(url))
    urls.clear()
    images.clear()
  }
  let location = 'fonts'
  try {
    const fonts = options.fonts === undefined ? globalThis.document?.fonts : options.fonts
    if (fonts) {
      await abortable(fonts.ready, signal)
      const faces = []
      JSON.stringify(document, (key, value) => {
        if (key === 'style' && value) {
          const family = value.fontFamily || 'Arial, sans-serif'
          faces.push({
            family,
            css: `${value.fontStyle || 'normal'} ${value.fontWeight || 400} ${value.fontSizePt || 10}pt ${family}`,
          })
        }
        return value
      })
      faces.push({ family: 'Arial, sans-serif', css: 'normal 400 10pt Arial, sans-serif' })
      const seenFamily = new Set()
      const seenCss = new Set()
      for (const face of faces) {
        if (seenFamily.has(face.family))
          continue
        seenFamily.add(face.family)
        await abortable((options.requireLocalFont || requireLocalFont)(face.family), signal)
      }
      for (const face of faces) {
        if (seenCss.has(face.css))
          continue
        seenCss.add(face.css)
        await abortable(fonts.load(face.css, '打印中文Aa012345'), signal)
        if (!fonts.check(face.css, '打印中文Aa012345') && !hasGenericFontFallback(face.family))
          throw new PrintError('FONT_UNAVAILABLE', '打印字体未就绪', 'fonts')
      }
    }
    const aliases = new Map(document.resources.map(resource => [resource.id, resource.fileId]))
    const loadImage = async (key, rawReference, path) => {
      const reference = normalizeImageReference(rawReference)
      if (!reference)
        return
      location = path
      if (!isSafeImageReference(reference)) {
        throw new PrintError('INVALID_RESOURCE', '图片引用无效', location)
      }
      const fileId = aliases.get(reference) || reference
      let src = files.get(fileId)
      if (!src) {
        if (reference.startsWith('data:')) {
          src = reference
        }
        else {
          if (!options.resolveFile) {
            throw new PrintError('RESOURCE_RESOLVER_REQUIRED', '缺少鉴权文件读取能力', location)
          }
          const blob = await asPrintImageBlob(await abortable(options.resolveFile(fileId, { signal }), signal))
          if (!blob) {
            throw new PrintError('INVALID_RESOURCE', '文件不是受支持的图片或体积超过限制', location)
          }
          src = (options.createObjectURL || (value => URL.createObjectURL(value)))(blob)
          urls.add(src)
        }
        await abortable((options.decodeImage || decodeImage)(src, signal), signal)
        files.set(fileId, src)
      }
      images.set(key, src)
    }
    const elements = [...document.header.elements, ...document.body.flatMap(section => section.elements || []), ...document.footer.elements]
    for (const element of elements) {
      location = element.id
      let src
      if (element.type === 'IMAGE') {
        await loadImage(element.id, resolveBinding(element.binding, context), element.id)
      }
      else if (element.type === 'STATIC_TABLE') {
        for (const cell of element.table?.cells || []) {
          if (cell.contentType !== 'IMAGE')
            continue
          await loadImage(`static-cell:${element.id}:${cell.id}`, resolveBinding(cell.binding, context), cell.id)
        }
      }
      else if (['BARCODE', 'QRCODE'].includes(element.type)) {
        const text = formatValue(resolveBinding(element.binding, context), element.format)
        src = await abortable((options.encodeCode || encodePrintCode)({ ...element, text }, signal), signal)
        await abortable((options.decodeImage || decodeImage)(src, signal), signal)
      }
      if (src) {
        images.set(element.id, src)
      }
    }
    const fieldTypes = new Map((options.catalog || []).map(field => [field.path, field.type]))
    const tableLike = [
      ...document.body.filter(item => item.kind === 'TABLE'),
      ...elements.filter(item => item.type === 'DATA_TABLE'),
    ]
    for (const section of tableLike) {
      const rows = resolveCollection(section.collectionPath, context)
      for (const column of section.columns || []) {
        if (fieldTypes.get(`${section.collectionPath}.${column.field}`) !== 'IMAGE') {
          continue
        }
        for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
          const path = `${section.collectionPath}[${rowIndex}].${column.field}`
          await loadImage(tableImageResourceKey(section.id, rowIndex, column.id), readOwnPath(rows[rowIndex], column.field), path)
        }
      }
      const bands = [
        ...(section.headerRows || []).map((row, index) => ({ key: `header-${index}`, row })),
        ...(section.subtotal ? [{ key: 'subtotal', row: section.subtotal }] : []),
        ...(section.footer ? [{ key: 'footer', row: section.footer }] : []),
      ]
      for (const band of bands) {
        for (let cellIndex = 0; cellIndex < (band.row.cells || []).length; cellIndex++) {
          const cell = band.row.cells[cellIndex]
          if (cell.contentType !== 'IMAGE')
            continue
          await loadImage(`band:${section.id}:${band.key}:${cellIndex}`, resolveBinding(cell.binding, context), `${section.id}:${band.key}:${cellIndex}`)
        }
      }
    }
    if (document.paper?.designBackground?.fileId) {
      await loadImage('design-background', document.paper.designBackground.fileId, 'paper.designBackground')
    }
    if (signal.aborted) {
      throw signal.reason
    }
    return { images, dispose }
  }
  catch (error) {
    dispose()
    throw error instanceof PrintError ? error : new PrintError('RESOURCE_FAILED', '打印资源加载失败', location)
  }
  finally {
    clearTimeout(timer)
    options.signal?.removeEventListener('abort', cancel)
  }
}
