import { ref, shallowRef, toValue } from 'vue'
import { resolvePrintSource } from '@/api/print'
import { printSourcePayload } from '@/components/print/management/printRouteContext'

const BUSINESS_SCENES = new Set(['LIST', 'DETAIL'])
const PARAMETER_KEY = /^[a-z]\w{0,79}$/i
const RESERVED_PARAMETERS = new Set([
  'tenantId',
  'userId',
  'actor',
  'applicationId',
  'applicationVersionId',
  'businessSourceId',
  'sourceCode',
  'sourceRevision',
  'templateId',
  'templateVersionId',
  'processRunId',
])

function readOptions(options) {
  const resolved = toValue(options) || {}
  return Object.fromEntries(
    Object.entries(resolved).map(([key, value]) => [key, toValue(value)]),
  )
}

function validateParams(params) {
  if (params == null)
    return {}
  if (Object.getPrototypeOf(params) !== Object.prototype)
    throw new Error('打印参数必须是普通对象')
  const entries = Object.entries(params)
  if (entries.length > 50)
    throw new Error('打印参数不能超过 50 个')
  for (const [key, value] of entries) {
    if (!PARAMETER_KEY.test(key) || RESERVED_PARAMETERS.has(key))
      throw new Error(`打印参数名无效：${key}`)
    if (value != null && !['string', 'number', 'boolean'].includes(typeof value))
      throw new Error(`打印参数 ${key} 只能是字符串、数字或布尔值`)
    if (typeof value === 'number' && !Number.isFinite(value))
      throw new Error(`打印参数 ${key} 不是有效数字`)
  }
  return Object.fromEntries(entries)
}

/**
 * 创建普通业务打印请求。这里只携带记录身份和受控参数，不接受页面正文或模板版本。
 */
export function createBusinessPrintRecord(options = {}) {
  const recordId = String(options.recordId ?? '').trim()
  const scene = String(options.scene || 'DETAIL')
  const source = printSourcePayload({
    businessSourceId: options.businessSourceId,
    sourceCode: options.sourceCode,
    sourceType: options.sourceType || 'SERVICE',
    objectCode: options.objectCode,
  })
  if (!source?.businessSourceId)
    throw new Error('打印业务来源无效')
  if (!recordId || recordId.length > 128)
    throw new Error('打印记录标识无效')
  if (!BUSINESS_SCENES.has(scene))
    throw new Error('普通业务打印只支持列表或详情场景')
  return {
    source,
    recordId,
    scene,
    params: validateParams(options.params),
  }
}

/**
 * 允许普通业务只配置稳定 sourceCode；内部 ID、来源类型和对象编码由服务端按租户解析。
 */
export async function resolveBusinessPrintRecord(options = {}, resolver = resolvePrintSource) {
  if (options.businessSourceId && options.sourceType && options.objectCode)
    return createBusinessPrintRecord(options)
  const sourceCode = String(options.sourceCode || '').trim()
  const { data } = await resolver(sourceCode)
  if (!data || data.sourceCode !== sourceCode)
    throw new Error('打印业务来源不可用')
  return createBusinessPrintRecord({
    ...options,
    businessSourceId: data.id,
    sourceCode: data.sourceCode,
    sourceType: data.sourceType,
    objectCode: data.objectCode,
  })
}

/**
 * 业务页面统一打印入口。可在初始化时提供静态/响应式配置，也可在 open 时覆盖记录参数。
 */
export function useBusinessPrint(options = {}) {
  const visible = ref(false)
  const record = shallowRef(null)
  const error = ref('')
  const loading = ref(false)
  let generation = 0

  async function open(overrides = {}) {
    const current = ++generation
    error.value = ''
    loading.value = true
    try {
      const next = await resolveBusinessPrintRecord({
        ...readOptions(options),
        ...readOptions(overrides),
      })
      if (current !== generation)
        return null
      record.value = next
      visible.value = true
      return record.value
    }
    catch (reason) {
      if (current !== generation)
        return null
      record.value = null
      visible.value = false
      error.value = reason.message || '打印参数无效'
      return null
    }
    finally {
      if (current === generation)
        loading.value = false
    }
  }

  function close() {
    generation++
    visible.value = false
    record.value = null
    error.value = ''
    loading.value = false
  }

  return { visible, record, error, loading, open, close }
}
