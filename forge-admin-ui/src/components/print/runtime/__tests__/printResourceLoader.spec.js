import { describe, expect, it, vi } from 'vitest'
import { preparePrintResources } from '../../engine/resources'
import { PrintError } from '../../protocol/types'
import { loadPrintResources } from '../printResourceLoader'

vi.mock('../../engine/resources', () => ({ preparePrintResources: vi.fn() }))

describe('runtime print resource loader', () => {
  it('passes the cancellation signal, catalog and authenticated resolver to the engine', async () => {
    const controller = new AbortController()
    const resolveFile = vi.fn()
    const prepared = { images: new Map(), dispose: vi.fn() }
    preparePrintResources.mockResolvedValue(prepared)
    await expect(loadPrintResources({}, {}, { signal: controller.signal, catalog: [{ path: 'flow.history.signature', type: 'IMAGE' }], resolveFile })).resolves.toBe(prepared)
    expect(preparePrintResources).toHaveBeenCalledWith({}, {}, expect.objectContaining({ signal: controller.signal, resolveFile }))
  })

  it.each([
    ['INVALID_RESOURCE', 'RESOURCE_FAILED'],
    ['RESOURCE_RESOLVER_REQUIRED', 'RESOURCE_FAILED'],
    ['RESOURCE_NOT_READY', 'RESOURCE_FAILED'],
    ['RESOURCE_CANCELLED', 'PRINT_CANCELLED'],
  ])('maps %s to the bounded runtime audit code %s', async (input, output) => {
    preparePrintResources.mockRejectedValue(new PrintError(input, 'private details', 'flow.history[0].signature'))
    await expect(loadPrintResources({}, {})).rejects.toMatchObject({ code: output, path: 'flow.history[0].signature' })
  })

  it('preserves timeout failures so the user can distinguish a retryable wait', async () => {
    preparePrintResources.mockRejectedValue(new PrintError('RESOURCE_TIMEOUT', '打印资源准备超时'))
    await expect(loadPrintResources({}, {})).rejects.toMatchObject({ code: 'RESOURCE_TIMEOUT' })
  })
})
