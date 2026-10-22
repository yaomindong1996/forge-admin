import { getCurrentInstance, inject, isRef, provide, unref } from 'vue'

/** Symbol + helpers: child panels share parent ListPageGridDesigner setupState without props drilling. */
export const LIST_PAGE_DESIGNER_KEY = Symbol('listPageDesigner')
/** 页面资源表单下拉选项（与顶部「页面资源」同源）；单独 provide，避免 Proxy 解包失败导致空下拉 */
export const PAGE_FORM_ASSET_OPTIONS_KEY = Symbol('pageFormAssetOptions')

/**
 * Forward setupState + declared props. Include set/accessor descriptors so panel
 * v-models can write through to parent refs/computeds.
 */
function createSetupStateProxy(instance) {
  const resolve = (prop) => {
    const state = instance.setupState || {}
    if (Object.prototype.hasOwnProperty.call(state, prop))
      return { found: true, value: state[prop], source: 'setupState' }
    const props = instance.props || {}
    if (Object.prototype.hasOwnProperty.call(props, prop))
      return { found: true, value: props[prop], source: 'props' }
    return { found: false, value: undefined, source: null }
  }

  const assign = (prop, value) => {
    const hit = resolve(prop)
    if (!hit.found)
      return false
    if (hit.source === 'props')
      return false
    const current = hit.value
    if (isRef(current)) {
      current.value = value
      return true
    }
    instance.setupState[prop] = value
    return true
  }

  return new Proxy({}, {
    get(_target, prop) {
      if (prop === '__v_isRef' || prop === '__v_isReadonly' || prop === 'constructor')
        return undefined
      // 子面板模板直接绑 options/数组；这里解包 Ref，避免 n-select 收到 ComputedRef 表现为「没有选项」
      return unref(resolve(prop).value)
    },
    set(_target, prop, value) {
      return assign(prop, value)
    },
    has(_target, prop) {
      return resolve(prop).found
    },
    ownKeys() {
      const keys = new Set([
        ...Reflect.ownKeys(instance.setupState || {}),
        ...Reflect.ownKeys(instance.props || {}),
      ])
      return [...keys]
    },
    getOwnPropertyDescriptor(_target, prop) {
      const hit = resolve(prop)
      if (!hit.found)
        return undefined
      return {
        configurable: true,
        enumerable: true,
        get: () => unref(resolve(prop).value),
        set: value => assign(prop, value),
      }
    },
  })
}

export function provideListPageDesignerApi() {
  const instance = getCurrentInstance()
  if (!instance)
    throw new Error('provideListPageDesignerApi must run inside setup()')
  const api = createSetupStateProxy(instance)
  provide(LIST_PAGE_DESIGNER_KEY, api)
  return api
}

export function useListPageDesignerApi() {
  const api = inject(LIST_PAGE_DESIGNER_KEY, null)
  if (!api)
    throw new Error('useListPageDesignerApi() requires ListPageGridDesigner parent')
  // 必须原样返回 Proxy，禁止 `return { ...api, extra }`：展开会把 ref/computed
  // 解成一次性快照，属性/样式/交互 Tab 的 v-show 会失效。
  return api
}
