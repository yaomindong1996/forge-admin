import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getDesignerComponent,
  insertDesignerComponent,
  normalizeFormDesignerSchema,
  removeDesignerComponent,
  updateDesignerComponent,
  updateDesignerLayout,
} from '@/views/app-center/components/designer/form-first/formDesignerSchema'

/**
 * 表单设计器共享状态（Pinia 化改造，AGENTS.md 5.14）
 *
 * 设计原则：
 * - 画布 schema / 选中组件 / 字段 / 关系等跨面板状态统一收敛于此，
 *   属性面板子组件直接读写 store，不再通过 props + emit 层层透传。
 * - 存量父组件（ForgeFormDesigner / BusinessFormDesigner / application-runtime）
 *   通过「入口组件桥接」兼容：props → syncFromProps 进 store，
 *   store.schema 变化 → 入口组件 emit('update:schema') 对外广播。
 */
export const useFormDesignerStore = defineStore('formDesigner', () => {
  // ---------- state ----------
  /** 画布 schema（唯一事实源；子面板禁止直接改内部字段，必须走 action 生成新引用） */
  const schema = ref(normalizeFormDesignerSchema({}))
  /** 当前选中组件 ID（空串 = 未选中，展示表单级属性） */
  const selectedId = ref('')
  /** 业务对象字段列表 */
  const fields = ref([])
  /** 业务对象关联关系列表（可选；主子表面板不再强制依赖） */
  const relations = ref([])
  /** 业务对象编码 */
  const objectCode = ref('')
  /** 表单级属性面板当前 tab（basic / events / style） */
  const formPropertyTab = ref('basic')

  // ---------- getters ----------
  const selectedComponent = computed(() => getDesignerComponent(schema.value, selectedId.value))

  const formAssets = computed(() => {
    return Array.isArray(schema.value.settings?.formAssets) ? schema.value.settings.formAssets : []
  })

  const defaultFormKey = computed(() => {
    return schema.value.defaultFormKey || schema.value.settings?.defaultFormKey || schema.value.formKey
  })

  const formGovernanceSettings = computed(() => schema.value.settings?.governance || {})

  /** 画布上全部 subTable 容器（含嵌套在布局内的），供主子表配置面板统一管理 */
  const subTableComponents = computed(() => {
    const components = []
    const walk = (list) => {
      ;(Array.isArray(list) ? list : []).forEach((component) => {
        if (!component || typeof component !== 'object')
          return
        if (component.componentKey === 'subTable')
          components.push(component)
        if (Array.isArray(component.children))
          walk(component.children)
      })
    }
    walk(schema.value.components)
    return components
  })

  // ---------- actions ----------
  /** 入口组件桥接：把 props 同步进 store（同引用赋值不会触发额外响应） */
  function syncFromProps(payload = {}) {
    if (payload.schema !== undefined && payload.schema !== null && payload.schema !== schema.value)
      schema.value = payload.schema
    if (payload.selectedId !== undefined && payload.selectedId !== selectedId.value)
      selectedId.value = payload.selectedId || ''
    if (payload.fields !== undefined && payload.fields !== fields.value)
      fields.value = payload.fields || []
    if (payload.relations !== undefined && payload.relations !== relations.value)
      relations.value = payload.relations || []
    if (payload.objectCode !== undefined && payload.objectCode !== objectCode.value)
      objectCode.value = payload.objectCode || ''
  }

  /** 应用一份新 schema（子面板统一入口） */
  function applySchema(next) {
    schema.value = next
  }

  /** 选中组件（空串表示回到表单级属性） */
  function selectComponent(componentId = '') {
    selectedId.value = componentId || ''
  }

  /** 更新表单级 layout 配置（表单项配置 / 校验反馈 / 操作按钮等共用） */
  function updateLayout(patch = {}) {
    applySchema(updateDesignerLayout(schema.value, patch))
  }

  /** 更新表单 settings 配置（governance / formAssets / offlineDraft 等） */
  function updateSettings(settingsPatch = {}) {
    applySchema({
      ...schema.value,
      settings: {
        ...(schema.value.settings || {}),
        ...settingsPatch,
      },
    })
  }

  /** 更新 governance 子配置（权限 / 事件 / 字段规则 / 断网草稿共用） */
  function updateGovernance(patch = {}) {
    updateSettings({
      governance: {
        ...formGovernanceSettings.value,
        ...patch,
      },
    })
  }

  /** 更新指定组件（patch 语义同 updateDesignerComponent） */
  function updateComponent(componentId = '', patch = {}) {
    if (!componentId)
      return
    applySchema(updateDesignerComponent(schema.value, componentId, patch))
  }

  /** 插入组件（target: { parentId, index }） */
  function insertComponent(target = {}, component = {}) {
    applySchema(insertDesignerComponent(schema.value, target, component))
  }

  /** 删除组件 */
  function removeComponent(componentId = '') {
    if (!componentId)
      return
    applySchema(removeDesignerComponent(schema.value, componentId))
    if (selectedId.value === componentId)
      selectedId.value = ''
  }

  /** 切换表单级属性面板 tab */
  function setFormPropertyTab(tab = 'basic') {
    formPropertyTab.value = tab
  }

  return {
    schema,
    selectedId,
    fields,
    relations,
    objectCode,
    formPropertyTab,
    selectedComponent,
    formAssets,
    defaultFormKey,
    formGovernanceSettings,
    subTableComponents,
    syncFromProps,
    applySchema,
    selectComponent,
    updateLayout,
    updateSettings,
    updateGovernance,
    updateComponent,
    insertComponent,
    removeComponent,
    setFormPropertyTab,
  }
})
