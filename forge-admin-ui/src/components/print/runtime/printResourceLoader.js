import { preparePrintResources } from '../engine/resources'
import { PrintError } from '../protocol/types'

const RESOURCE_FAILURE_CODES = new Set([
  'INVALID_RESOURCE',
  'RESOURCE_RESOLVER_REQUIRED',
  'RESOURCE_NOT_READY',
])

/**
 * Runtime boundary for authenticated images and signatures. It keeps engine
 * details out of audit events and never turns a failed resource into a blank
 * printable document.
 */
export async function loadPrintResources(document, context, options = {}) {
  try {
    return await preparePrintResources(document, context, options)
  }
  catch (error) {
    if (error?.code === 'RESOURCE_CANCELLED') {
      throw new PrintError('PRINT_CANCELLED', '打印准备已取消', error.path)
    }
    if (RESOURCE_FAILURE_CODES.has(error?.code)) {
      throw new PrintError('RESOURCE_FAILED', '打印图片或签名加载失败', error.path)
    }
    throw error
  }
}
