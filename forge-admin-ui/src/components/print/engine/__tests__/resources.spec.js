import { describe, expect, it, vi } from 'vitest'
import { createDesignerSampleContext, SAMPLE_IMAGE_DATA_URL } from '../../designer/designerSample'
import { createPrintDocument } from '../../protocol/types'
import { requireLocalFont } from '../fonts'
import { MeasurementCache } from '../measurementCache'
import { preparePrintResources } from '../resources'

const PNG_BYTES = Uint8Array.from(atob(SAMPLE_IMAGE_DATA_URL.slice(SAMPLE_IMAGE_DATA_URL.indexOf(',') + 1)), char => char.charCodeAt(0))

function imageDocument() {
  const doc = createPrintDocument()
  doc.header = { heightMm: 20, repeat: true, elements: [{ id: 'logo', type: 'IMAGE', xMm: 0, yMm: 0, widthMm: 20, heightMm: 20, binding: { source: 'FIELD', path: 'main.logo' } }] }
  return doc
}

describe('bounded print resource session', () => {
  it('rejects a requested local font when the browser cannot load it', async () => {
    class MissingFont {
      async load() {
        throw new Error('font missing')
      }
    }
    await expect(requireLocalFont('Unavailable Font', MissingFont)).rejects.toMatchObject({ code: 'FONT_UNAVAILABLE' })
    await expect(requireLocalFont('Unavailable Font, sans-serif', MissingFont)).resolves.toBeUndefined()
    await expect(requireLocalFont('sans-serif', MissingFont)).resolves.toBeUndefined()
  })

  it('accepts a font stack when a later named face or generic fallback is available', async () => {
    class PartialFont {
      constructor(_, source) {
        this.source = source
      }

      async load() {
        if (this.source.includes('STHeiti') || this.source.includes('Missing'))
          throw new Error('font missing')
      }
    }
    await expect(requireLocalFont('STHeiti, Heiti SC, sans-serif', PartialFont)).resolves.toBeUndefined()
    await expect(requireLocalFont('Missing, Arial, sans-serif', PartialFont)).resolves.toBeUndefined()
  })
  it('resolves authorized files once and revokes URLs exactly once', async () => {
    const revoke = vi.fn()
    const resolver = vi.fn(async () => new Blob(['image'], { type: 'image/png' }))
    const resources = await preparePrintResources(imageDocument(), { main: { logo: '123' } }, {
      resolveFile: resolver,
      decodeImage: vi.fn(async () => {}),
      fonts: null,
      createObjectURL: () => 'blob:local',
      revokeObjectURL: revoke,
    })
    expect(resources.images.get('logo')).toBe('blob:local')
    expect(resolver).toHaveBeenCalledWith('123', expect.objectContaining({ signal: expect.any(AbortSignal) }))
    resources.dispose()
    resources.dispose()
    expect(revoke).toHaveBeenCalledTimes(1)
  })
  it('rejects remote resources and cleans decoded failures', async () => {
    await expect(preparePrintResources(imageDocument(), { main: { logo: 'https://untrusted/image' } }, { fonts: null })).rejects.toMatchObject({ code: 'INVALID_RESOURCE' })
    const revoke = vi.fn()
    await expect(preparePrintResources(imageDocument(), { main: { logo: '123' } }, {
      fonts: null,
      resolveFile: async () => new Blob(['x'], { type: 'image/png' }),
      createObjectURL: () => 'blob:bad',
      revokeObjectURL: revoke,
      decodeImage: async () => { throw new Error('decode failed') },
    })).rejects.toMatchObject({ code: 'RESOURCE_FAILED', path: 'logo' })
    expect(revoke).toHaveBeenCalledWith('blob:bad')
  })
  it('bounds a hanging resolver and aborts the underlying request', async () => {
    let requestSignal
    const result = preparePrintResources(imageDocument(), { main: { logo: '123' } }, {
      fonts: null,
      timeoutMs: 10,
      resolveFile: (_, options) => {
        requestSignal = options.signal
        return new Promise(() => {})
      },
    })
    await expect(result).rejects.toMatchObject({ code: 'RESOURCE_TIMEOUT' })
    expect(requestSignal.aborted).toBe(true)
  })
  it('loads flow history signatures as authenticated table image resources', async () => {
    const doc = createPrintDocument()
    doc.body = [{
      id: 'history',
      kind: 'TABLE',
      collectionPath: 'flow.history',
      repeatHeader: true,
      columns: [
        { id: 'assignee', field: 'assigneeName', title: '办理人', widthMm: 80 },
        { id: 'signature', field: 'signature', title: '办理签名', widthMm: 80 },
      ],
    }]
    const resolver = vi.fn(async () => new Blob(['signature'], { type: 'image/png' }))
    const resources = await preparePrintResources(doc, { flow: { history: [{ assigneeName: '审核人', signature: 'signature_1' }] } }, {
      catalog: [
        { path: 'flow.history', type: 'COLLECTION' },
        { path: 'flow.history.assigneeName', type: 'TEXT' },
        { path: 'flow.history.signature', type: 'IMAGE' },
      ],
      resolveFile: resolver,
      decodeImage: vi.fn(async () => {}),
      fonts: null,
      createObjectURL: () => 'blob:signature',
      revokeObjectURL: vi.fn(),
    })
    expect(resolver).toHaveBeenCalledWith('signature_1', expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(resources.images.get('table:history:0:signature')).toBe('blob:signature')
    resources.dispose()
  })
  it('loads designer sample signatures without a file resolver', async () => {
    const doc = createPrintDocument()
    doc.body = [{
      id: 'history',
      kind: 'TABLE',
      collectionPath: 'flow.history',
      columns: [{ id: 'signature', field: 'signature', title: '办理签名', widthMm: 80 }],
    }]
    const catalog = [
      { path: 'flow.history', type: 'COLLECTION' },
      { path: 'flow.history.signature', type: 'IMAGE' },
    ]
    const resources = await preparePrintResources(doc, createDesignerSampleContext(catalog), {
      catalog,
      fonts: null,
      decodeImage: vi.fn(async () => {}),
    })
    expect(resources.images.get('table:history:0:signature')).toBe(SAMPLE_IMAGE_DATA_URL)
    resources.dispose()
  })
  it('accepts png signatures served as octet-stream and skips blank history rows', async () => {
    const doc = createPrintDocument()
    doc.body = [{
      id: 'history',
      kind: 'TABLE',
      collectionPath: 'flow.history',
      columns: [{ id: 'signature', field: 'signature', title: '办理签名', widthMm: 80 }],
    }]
    const resolver = vi.fn(async () => new Blob([PNG_BYTES], { type: 'application/octet-stream' }))
    const resources = await preparePrintResources(doc, { flow: { history: [{ signature: '' }, { signature: 12345 }] } }, {
      catalog: [
        { path: 'flow.history', type: 'COLLECTION' },
        { path: 'flow.history.signature', type: 'IMAGE' },
      ],
      resolveFile: resolver,
      decodeImage: vi.fn(async () => {}),
      fonts: null,
      createObjectURL: () => 'blob:signature',
      revokeObjectURL: vi.fn(),
    })
    expect(resolver).toHaveBeenCalledTimes(1)
    expect(resolver).toHaveBeenCalledWith('12345', expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(resources.images.has('table:history:0:signature')).toBe(false)
    expect(resources.images.get('table:history:1:signature')).toBe('blob:signature')
    resources.dispose()
  })
  it('does not block preview when a missing primary font still has a CSS fallback', async () => {
    const fonts = {
      ready: Promise.resolve(),
      load: vi.fn(async () => {}),
      check: vi.fn(() => false),
    }
    const doc = createPrintDocument()
    doc.header = {
      heightMm: 12,
      repeat: true,
      elements: [{
        id: 'title',
        type: 'TEXT',
        xMm: 0,
        yMm: 0,
        widthMm: 40,
        heightMm: 8,
        binding: { source: 'CONSTANT', value: '标题' },
        style: { fontFamily: 'STHeiti, sans-serif' },
      }],
    }
    const resources = await preparePrintResources(doc, {}, { fonts, requireLocalFont: async () => {} })
    expect(fonts.load).toHaveBeenCalled()
    resources.dispose()
  })
  it('still rejects a named font with no generic fallback when it cannot be checked', async () => {
    const fonts = {
      ready: Promise.resolve(),
      load: vi.fn(async () => {}),
      check: vi.fn(() => false),
    }
    const doc = createPrintDocument()
    doc.header = {
      heightMm: 12,
      repeat: true,
      elements: [{
        id: 'title',
        type: 'TEXT',
        xMm: 0,
        yMm: 0,
        widthMm: 40,
        heightMm: 8,
        binding: { source: 'CONSTANT', value: '标题' },
        style: { fontFamily: 'STHeiti' },
      }],
    }
    await expect(preparePrintResources(doc, {}, { fonts, requireLocalFont: async () => {} })).rejects.toMatchObject({ code: 'FONT_UNAVAILABLE' })
  })
  it('keys measurements by text, width and complete style and evicts old entries', () => {
    const cache = new MeasurementCache(2)
    cache.set(['A', 10, { fontSizePt: 10 }], 1)
    expect(cache.get(['A', 10, { fontSizePt: 12 }])).toBeUndefined()
    expect(cache.get(['A', 11, { fontSizePt: 10 }])).toBeUndefined()
    cache.set(['B'], 2)
    cache.set(['C'], 3)
    expect(cache.get(['A', 10, { fontSizePt: 10 }])).toBeUndefined()
  })
})
