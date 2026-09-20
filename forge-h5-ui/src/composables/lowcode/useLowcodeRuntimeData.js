import { normalizeDictOptions } from '../../utils/lowcode-runtime.js'

export function useLowcodeRuntimeData({
  runtimeStore,
  api,
  getMainFields,
  getChildren,
  onFormLoad,
  onDetailLoaded,
  onFormReset,
  handleError,
}) {
  let loadTimer

  async function loadRuntime() {
    if (!runtimeStore.configKey) {
      runtimeStore.errorMessage = '缺少低代码配置标识'
      runtimeStore.loading = false
      return
    }
    runtimeStore.loading = true
    runtimeStore.errorMessage = ''
    try {
      const response = await api.getLowcodeRenderConfig(runtimeStore.configKey, {
        appId: runtimeStore.routeQuery.appId || undefined,
        applicationId: runtimeStore.routeQuery.applicationId || undefined,
      })
      runtimeStore.applyConfig(response?.data || {})
      await loadDictionaries()
      if (runtimeStore.mode === 'list') await loadList()
      else if (runtimeStore.currentId) await loadDetail(runtimeStore.currentId)
      else await initializeForm()
    }
    catch (error) {
      runtimeStore.errorMessage = error?.message || '低代码运行配置加载失败'
    }
    finally {
      runtimeStore.loading = false
    }
  }

  async function loadDictionaries() {
    const types = new Set()
    const collect = fields => fields.forEach((field) => {
      if ((field.type === 'dictSelect' || field.type === 'pillSelect') && (field.dictType || field.props?.dictType))
        types.add(field.dictType || field.props.dictType)
    })
    collect(getMainFields())
    getChildren().forEach(child => collect(child.fields))
    await Promise.all([...types].map(async (type) => {
      try { runtimeStore.setDictOptions(type, normalizeDictOptions((await api.getDictOptions(type))?.data)) }
      catch { runtimeStore.setDictOptions(type, []) }
    }))
  }

  async function loadList() {
    const response = await api.getLowcodePage(runtimeStore.configKey, {
      pageNum: runtimeStore.page,
      pageSize: runtimeStore.pageSize,
      ...runtimeStore.searchData,
    })
    runtimeStore.applyList(response?.data || {})
  }

  function loadListDebounced() {
    clearTimeout(loadTimer)
    loadTimer = setTimeout(() => {
      runtimeStore.page = 1
      loadList().catch(handleError)
    }, 350)
  }

  function resetSearch() {
    runtimeStore.resetSearch()
    loadList().catch(handleError)
  }

  async function loadDetail(id) {
    const response = await api.getLowcodeDetail(runtimeStore.configKey, id)
    runtimeStore.applyDetail(response?.data || {})
    await onFormLoad(runtimeStore.mainData, getMainFields())
    await onDetailLoaded()
  }

  async function initializeForm() {
    onFormReset()
    runtimeStore.initializeFormData()
    try { await onFormLoad(runtimeStore.mainData, getMainFields()) }
    catch (error) { handleError(error) }
  }

  function dispose() { clearTimeout(loadTimer) }

  return { loadRuntime, loadList, loadListDebounced, resetSearch, loadDetail, initializeForm, dispose }
}
