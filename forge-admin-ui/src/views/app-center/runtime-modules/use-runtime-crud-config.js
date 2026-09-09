import { reactive, ref } from 'vue'
import { crudConfigRender } from '@/api/ai'
import { businessObjectDesigner, businessObjectRuntimeInfo } from '@/api/business-app'
import { buildRuntimeCrudProps } from '@/components/lowcode-builder/shared/runtime-crud-props'
import { normalizeObjectDesignerFieldCatalog } from '../in-app-builder/page-form-object-promotion'

/**
 * 页面区块运行时 CRUD 配置的加载与缓存。
 *
 * 优先使用工作区对象快照自带的 configKey 请求草稿预览配置，
 * 失败时回退已发布配置；设计态可进一步降级读取对象设计器字段目录
 * 维持静态预览，避免整个应用页被单对象故障中断。
 *
 * @param {object} options
 * @param {object} options.application 当前应用 ref（取 id）
 * @param {object} options.workspaceEntries 工作区入口 ref（解析运行入口 id）
 * @param {object} options.canLoadDesignerSchema 是否允许走对象设计器降级（编辑/草稿态）
 */
export function useRuntimeCrudConfig({ application, workspaceEntries, canLoadDesignerSchema }) {
  const runtimeCrudPropsByObjectId = ref({})
  const runtimeCrudConfigRequests = new Map()
  const runtimeCrudLoadingObjectIds = reactive(new Set())
  const runtimeCrudUnavailableObjectIds = reactive(new Set())

  function resolveRuntimeEntryId(configKey) {
    const normalized = String(configKey || '').trim()
    if (!normalized)
      return null
    const entry = workspaceEntries.value.find(item => String(item?.configKey || '').trim() === normalized)
    return entry?.id ?? null
  }

  function fetchRuntimeCrudConfig(configKey) {
    if (!runtimeCrudConfigRequests.has(configKey)) {
      const request = (async () => {
        const options = {
          needTip: false,
          appId: resolveRuntimeEntryId(configKey),
          applicationId: application.value?.id,
        }
        try {
          const config = (await crudConfigRender(configKey, true, options)).data
          return { config, designPreview: true }
        }
        catch {
          // 草稿预览拿不到时退回已发布配置
          const config = (await crudConfigRender(configKey, false, options)).data
          return { config, designPreview: false }
        }
      })()
      request.catch(() => runtimeCrudConfigRequests.delete(configKey))
      runtimeCrudConfigRequests.set(configKey, request)
    }
    return runtimeCrudConfigRequests.get(configKey)
  }

  async function loadRuntimeCrudProps(objectRef, cacheKey) {
    try {
      let runtimeLoadError = null
      try {
        // 工作台对象快照本身已经带 configKey，优先使用它，和旧列表设计器的
        // 运行入口一致，也避免运行用户额外依赖“查看业务对象”权限。
        let runtimeInfo = {}
        let configKey = String(objectRef.configKey || '').trim()
        if (!configKey) {
          runtimeInfo = (await businessObjectRuntimeInfo(objectRef.objectId ?? objectRef.id)).data || {}
          configKey = String(runtimeInfo.configKey || '').trim()
        }
        if (!configKey)
          throw new Error('该业务对象还没有可用的列表运行配置')
        const { config, designPreview } = await fetchRuntimeCrudConfig(configKey)
        if (!config || typeof config !== 'object')
          throw new Error('业务对象运行配置为空')
        runtimeCrudPropsByObjectId.value = {
          ...runtimeCrudPropsByObjectId.value,
          [cacheKey]: {
            ...buildRuntimeCrudProps(config, { designPreview }),
            title: config.title || runtimeInfo.objectName || objectRef.objectName || '',
          },
        }
        return
      }
      catch (error) {
        runtimeLoadError = error
      }

      if (canLoadDesignerSchema.value) {
        try {
          const designer = (await businessObjectDesigner(objectRef.objectId ?? objectRef.id)).data || {}
          const fieldCatalog = normalizeObjectDesignerFieldCatalog(designer.modelSchema?.fields || designer.fields || [])
          if (fieldCatalog.length) {
            runtimeCrudPropsByObjectId.value = {
              ...runtimeCrudPropsByObjectId.value,
              [cacheKey]: {
                fieldCatalog,
                title: designer.objectName || objectRef.objectName || '',
                designPreview: true,
                draftOnly: true,
              },
            }
            return
          }
        }
        catch (error) {
          runtimeLoadError = error
        }
      }

      runtimeCrudUnavailableObjectIds.add(cacheKey)
      // 对象还在设计且没有字段时维持静态预览，不中断整个应用页。
      console.warn('[application-runtime] 加载业务对象字段目录失败', runtimeLoadError?.message || runtimeLoadError)
    }
    finally {
      runtimeCrudLoadingObjectIds.delete(cacheKey)
    }
  }

  /** load 周期切换时清空全部缓存，避免上一个应用的配置泄入下一个应用。 */
  function resetRuntimeCrudConfig() {
    runtimeCrudPropsByObjectId.value = {}
    runtimeCrudConfigRequests.clear()
    runtimeCrudLoadingObjectIds.clear()
    runtimeCrudUnavailableObjectIds.clear()
  }

  return {
    runtimeCrudPropsByObjectId,
    runtimeCrudLoadingObjectIds,
    runtimeCrudUnavailableObjectIds,
    fetchRuntimeCrudConfig,
    loadRuntimeCrudProps,
    resetRuntimeCrudConfig,
  }
}
