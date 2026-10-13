/** StructuredListPageDesigner.vue setup part 1. */
import { SearchOutline } from '@vicons/ionicons5'
import { NButton, NColorPicker, NIcon, NInput, NInputNumber, NSelect, NSwitch } from 'naive-ui'
import { computed, defineComponent, h, nextTick, ref } from 'vue'
import draggable from 'vuedraggable'
import CrudDefaultParamsEditor from '../CrudDefaultParamsEditor.vue'
import CrudHookRulesEditor from '../CrudHookRulesEditor.vue'
import GridBlockRenderer from '../GridBlockRenderer.vue'
import {
  patchCrudGridLayoutItems,
  resolveCrudPreviewFieldBindings,
} from '../list-page-field-bindings'
import { isPageFieldVisible } from '../page-schema'

export function applyStructuredListPageDesignerPart1(props, emit) {
  const __impl = {}
  const mut = {}

  const queryTypeOptions = [
    { label: '等于', value: 'eq' },
    { label: '包含', value: 'like' },
    { label: '大于等于', value: 'ge' },
    { label: '小于等于', value: 'le' },
    { label: '区间', value: 'between' },
    { label: '多值', value: 'in' },
  ]
  const searchComponentOptions = [
    { label: '自动', value: '' },
    { label: '输入框', value: 'input' },
    { label: '数字输入', value: 'number' },
    { label: '下拉选择', value: 'select' },
    { label: '字典选择', value: 'dictSelect' },
    { label: '组织树', value: 'orgTreeSelect' },
    { label: '用户选择', value: 'userSelect' },
    { label: '区划树', value: 'regionTreeSelect' },
    { label: '树形选择', value: 'treeSelect' },
    { label: '日期', value: 'date' },
    { label: '日期时间', value: 'datetime' },
    { label: '时间', value: 'time' },
  ]
  const tableRenderOptions = [
    { label: '自动（字典字段默认标签）', value: '' },
    { label: '字典标签', value: 'dictTag' },
    { label: '组织名称', value: 'orgName' },
    { label: '用户名称', value: 'userName' },
    { label: '区划名称', value: 'regionName' },
    { label: '文件名称', value: 'fileUpload' },
    { label: '图片预览', value: 'imageUpload' },
  ]
  const treeLoadModeOptions = [
    { label: '全量加载', value: 'full' },
    { label: '懒加载', value: 'lazy' },
  ]
  const sortOrderOptions = [
    { label: '降序', value: 'desc' },
    { label: '升序', value: 'asc' },
  ]
  const alignOptions = [
    { label: '左对齐', value: 'left' },
    { label: '居中', value: 'center' },
    { label: '右对齐', value: 'right' },
  ]
  const sizeOptions = [
    { label: '紧凑', value: 'small' },
    { label: '默认', value: 'medium' },
    { label: '宽松', value: 'large' },
  ]
  const renderModeOptions = [
    { label: '表格', value: 'table' },
    { label: '卡片', value: 'card' },
  ]
  const formOpenModeOptions = [
    { label: '弹窗', value: 'modal' },
    { label: '抽屉', value: 'drawer' },
    { label: '平铺', value: 'flat' },
    { label: '多页签', value: 'tabWorkspace' },
  ]
  const drawerPlacementOptions = [
    { label: '右侧', value: 'right' },
    { label: '左侧', value: 'left' },
    { label: '顶部', value: 'top' },
    { label: '底部', value: 'bottom' },
  ]
  const labelPlacementOptions = [
    { label: '左侧', value: 'left' },
    { label: '顶部', value: 'top' },
  ]
  const fixedOptions = [
    { label: '不固定', value: '' },
    { label: '固定左侧', value: 'left' },
    { label: '固定右侧', value: 'right' },
  ]
  const columnClickActionOptions = [
    { label: '无动作', value: 'none' },
    { label: '跳转页面', value: 'navigate' },
  ]
  const eventTriggerOptions = [
    { label: '行点击', value: 'rowClick' },
    { label: '加载完成', value: 'load' },
    { label: '新增成功', value: 'createSuccess' },
    { label: '编辑成功', value: 'updateSuccess' },
    { label: '删除成功', value: 'deleteSuccess' },
    { label: '导出完成', value: 'exportSuccess' },
  ]
  const eventActionOptions = [
    { label: '无动作', value: 'none' },
    { label: '跳转页面', value: 'navigate' },
    { label: '刷新当前组件', value: 'refreshBlock' },
    { label: '打开弹窗', value: 'openModal' },
    { label: '打开抽屉', value: 'openDrawer' },
    { label: '接口请求', value: 'request' },
    { label: '自定义脚本', value: 'customScript' },
  ]
  const customActionPositionOptions = [
    { label: '行操作列', value: 'row' },
    { label: '工具栏', value: 'toolbar' },
  ]
  const customActionTypeOptions = [
    { label: '打开路由', value: 'route' },
    { label: '跳转页面', value: 'page' },
    { label: '打开弹窗', value: 'modal' },
    { label: '接口请求', value: 'request' },
    { label: '自定义脚本', value: 'script' },
  ]
  const buttonTypeOptions = [
    { label: '主要', value: 'primary' },
    { label: '默认', value: 'default' },
    { label: '成功', value: 'success' },
    { label: '警告', value: 'warning' },
    { label: '危险', value: 'error' },
  ]

  const FieldOrderEditor = defineComponent({
    name: 'FieldOrderEditor',
    props: {
      title: {
        type: String,
        required: true,
      },
      emptyText: {
        type: String,
        required: true,
      },
      fields: {
        type: Array,
        default: () => [],
      },
      selectedRefs: {
        type: Array,
        default: () => [],
      },
      filter: {
        type: Function,
        required: true,
      },
      mode: {
        type: String,
        default: '',
      },
      settings: {
        type: Object,
        default: () => ({}),
      },
      pageOptions: {
        type: Array,
        default: () => [],
      },
    },
    emits: ['update', 'updateSettings'],
    setup(editorProps, { emit }) {
      const fieldMap = computed(() => new Map(editorProps.fields.map(field => [field.field, field])))
      const selectedRows = computed(() => editorProps.selectedRefs.map(ref => fieldMap.value.get(ref)).filter(Boolean))
      const availableRows = computed(() => editorProps.fields.filter((field) => {
        return editorProps.filter(field) && !editorProps.selectedRefs.includes(field.field)
      }))
      const queryFieldOptions = computed(() => {
        const options = editorProps.fields
          .filter(field => !field.systemField || field.field === 'id')
          .map(field => ({
            label: field.label ? `${field.label}（${field.sourceField || field.field}）` : (field.sourceField || field.field),
            value: field.field,
          }))
        if (!options.some(item => item.value === 'id'))
          options.unshift({ label: 'ID（id）', value: 'id' })
        return options
      })
      const renderTargetFieldOptions = (field = {}) => {
        const options = queryFieldOptions.value.map(item => ({ ...item }))
        const defaultTarget = `${field.field}Name`
        if (!options.some(item => item.value === defaultTarget)) {
          options.unshift({
            label: `${defaultTarget}（默认翻译字段）`,
            value: defaultTarget,
          })
        }
        return options
      }
      const updateRows = rows => emit('update', rows.map(row => row.field))
      const remove = field => emit('update', editorProps.selectedRefs.filter(ref => ref !== field))
      const add = field => emit('update', [...editorProps.selectedRefs, field])
      const updateSetting = (field, patch) => {
        emit('updateSettings', {
          field,
          settings: {
            ...(editorProps.settings?.[field] || {}),
            ...patch,
          },
        })
      }
      const fieldControl = (label, control, desc = '') => h('label', { class: 'field-setting-control' }, [
        h('span', { class: 'field-setting-label' }, label),
        control,
        desc ? h('small', null, desc) : null,
      ])
      return () => h('div', { class: 'field-editor' }, [
        h('div', { class: 'field-editor-title' }, editorProps.title),
        selectedRows.value.length
          ? h(draggable, {
              'modelValue': selectedRows.value,
              'itemKey': 'field',
              'handle': '.field-handle',
              'animation': 160,
              'class': 'selected-field-list',
              'onUpdate:modelValue': updateRows,
            }, {
              item: ({ element }) => {
                const setting = editorProps.settings?.[element.field] || {}
                const tableRenderType = setting.renderType || resolveDefaultTableRenderType(element)
                return h('div', { class: ['selected-field-row', editorProps.mode ? `mode-${editorProps.mode}` : ''] }, [
                  h('span', { class: 'field-handle' }, '☰'),
                  h('span', { class: 'field-name' }, [
                    h('span', null, element.label || element.field),
                    element.sourceLabel || element.modelName
                      ? h('small', null, element.sourceLabel || element.modelName)
                      : null,
                  ]),
                  h('span', { class: 'field-code' }, element.field),
                  h('button', { type: 'button', onClick: () => remove(element.field) }, '移除'),
                  editorProps.mode === 'search'
                    ? h('div', { class: 'field-setting-row search-setting-row' }, [
                        fieldControl('查询方式', h(NSelect, {
                          value: setting.queryType || element.queryType || 'like',
                          options: queryTypeOptions,
                          size: 'tiny',
                          placeholder: '查询方式',
                          onUpdateValue: value => updateSetting(element.field, { queryType: value }),
                        })),
                        fieldControl('查询组件', h(NSelect, {
                          value: setting.componentType || resolveDefaultSearchComponentType(element),
                          options: searchComponentOptions,
                          size: 'tiny',
                          placeholder: '查询组件',
                          onUpdateValue: value => updateSetting(element.field, { componentType: value }),
                        })),
                        fieldControl('映射字段', h(NSelect, {
                          value: setting.queryField || element.field,
                          options: queryFieldOptions.value,
                          size: 'tiny',
                          filterable: true,
                          placeholder: '映射字段',
                          onUpdateValue: value => updateSetting(element.field, { queryField: value }),
                        }), '接口查询参数使用哪个字段'),
                        fieldControl('对齐', h(NSelect, {
                          value: setting.align || 'left',
                          options: alignOptions,
                          size: 'tiny',
                          placeholder: '对齐',
                          onUpdateValue: value => updateSetting(element.field, { align: value || 'left' }),
                        })),
                      ])
                    : null,
                  editorProps.mode === 'table'
                    ? h('div', { class: 'field-setting-row table-setting-row' }, [
                        h('span', { class: 'field-inline-switch' }, [
                          h('span', null, '排序'),
                          h(NSwitch, {
                            value: Boolean(setting.sortable ?? element.sortable),
                            size: 'small',
                            onUpdateValue: value => updateSetting(element.field, { sortable: value }),
                          }),
                        ]),
                        fieldControl('渲染方式', h(NSelect, {
                          value: tableRenderType,
                          options: tableRenderOptions,
                          size: 'tiny',
                          placeholder: '渲染方式',
                          onUpdateValue: value => updateSetting(element.field, { renderType: value }),
                        })),
                        fieldControl('对齐', h(NSelect, {
                          value: setting.align || 'left',
                          options: alignOptions,
                          size: 'tiny',
                          placeholder: '对齐',
                          onUpdateValue: value => updateSetting(element.field, { align: value || 'left' }),
                        })),
                        fieldControl('列宽', h(NInputNumber, {
                          value: setting.width || element.width || 160,
                          min: 80,
                          max: 800,
                          size: 'tiny',
                          showButton: false,
                          placeholder: '列宽',
                          onUpdateValue: value => updateSetting(element.field, { width: value || null }),
                        }), '单位 px'),
                        fieldControl('固定列', h(NSelect, {
                          value: setting.fixed || '',
                          options: fixedOptions,
                          size: 'tiny',
                          placeholder: '固定列',
                          onUpdateValue: value => updateSetting(element.field, { fixed: value || null }),
                        })),
                        fieldControl('文字颜色', h(NColorPicker, {
                          value: setting.textColor || '',
                          size: 'small',
                          showAlpha: true,
                          placeholder: '文字颜色',
                          onUpdateValue: value => updateSetting(element.field, { textColor: value || '' }),
                        })),
                        fieldControl('点击动作', h(NSelect, {
                          value: setting.clickAction || 'none',
                          options: columnClickActionOptions,
                          size: 'tiny',
                          placeholder: '点击动作',
                          onUpdateValue: value => updateSetting(element.field, { clickAction: value || 'none' }),
                        })),
                        setting.clickAction === 'navigate'
                          ? fieldControl('目标页面', h(NSelect, {
                              value: setting.targetPageKey || '',
                              options: editorProps.pageOptions,
                              size: 'tiny',
                              filterable: true,
                              placeholder: '选择详情页或自定义页',
                              onUpdateValue: value => updateSetting(element.field, { targetPageKey: value || '' }),
                            }))
                          : null,
                        setting.clickAction === 'navigate'
                          ? fieldControl('参数名', h(NInput, {
                              value: setting.targetParamName || 'id',
                              size: 'tiny',
                              placeholder: '目标页面参数名',
                              onUpdateValue: value => updateSetting(element.field, { targetParamName: normalizeParamName(value) || 'id' }),
                            }), '目标页面接收的参数名，默认 id')
                          : null,
                        setting.clickAction === 'navigate'
                          ? fieldControl('取值字段', h(NSelect, {
                              value: setting.targetParamField || 'id',
                              options: queryFieldOptions.value,
                              size: 'tiny',
                              filterable: true,
                              placeholder: '从当前行取哪个字段',
                              onUpdateValue: value => updateSetting(element.field, { targetParamField: value || 'id' }),
                            }), '默认从当前行 id 取值')
                          : null,
                        isNameRenderType(tableRenderType)
                          ? fieldControl('名称字段', h(NSelect, {
                              value: setting.targetField || `${element.field}Name`,
                              options: renderTargetFieldOptions(element),
                              size: 'tiny',
                              filterable: true,
                              tag: true,
                              placeholder: '名称字段',
                              onUpdateValue: value => updateSetting(element.field, { targetField: value }),
                            }), '用哪个字段显示名称')
                          : null,
                      ])
                    : null,
                ])
              },
            })
          : h('div', { class: 'field-empty' }, editorProps.emptyText),
        availableRows.value.length
          ? h('div', { class: 'available-field-list' }, availableRows.value.map(field => h('button', {
              key: field.field,
              type: 'button',
              onClick: () => add(field.field),
            }, [
              h('span', null, field.label || field.field),
              field.sourceLabel || field.modelName ? h('small', null, field.sourceLabel || field.modelName) : null,
            ])))
          : null,
      ])
    },
  })

  const fieldMap = computed(() => new Map(props.fields.map(field => [field.field, field])))
  const searchZone = computed(() => findZone('search'))
  const tableZone = computed(() => findZone('table'))
  const activeFieldZone = ref('search')
  const configExpanded = ref(false)
  const configCollapsed = ref(false)
  const fieldEditorAnchor = ref(null)
  const fieldOptions = computed(() => props.fields.map(field => ({
    label: field.label ? `${field.label}（${field.field}）` : field.field,
    value: field.field,
  })))
  const pageTargetOptions = computed(() => props.pages.map(page => ({
    label: `${page.pageName || page.pageKey}（${page.pageType || 'custom'}）`,
    value: page.pageKey,
  })))
  const sortFieldOptions = computed(() => {
    const options = fieldOptions.value.map(item => ({ ...item }))
    if (!options.some(item => item.value === 'id')) {
      options.unshift({ label: 'ID（id）', value: 'id' })
    }
    return options
  })
  const searchFields = computed(() => resolveFields(searchZone.value, field => field.searchable))
  const tableFields = computed(() => resolveFields(tableZone.value, field => field.listVisible !== false))
  const treeConfig = computed(() => tableZone.value?.props?.treeConfig || {})
  const tableEvents = computed(() => tableZone.value?.props?.events || [])
  const tableCustomActions = computed(() => tableZone.value?.props?.customActions || [])
  const configKeyword = ref('')
  const defaultSortText = computed(() => {
    const field = sortFieldOptions.value.find(item => item.value === (tableZone.value?.props?.defaultSortField || 'id'))
    const order = sortOrderOptions.find(item => item.value === (tableZone.value?.props?.defaultSortOrder || 'desc'))
    return `${field?.label || 'ID（id）'} ${order?.label || '降序'}`
  })
  const defaultApiValues = computed(() => {
    const key = props.modelValue.configKey
      || props.modelSchema?.configKey
      || props.modelSchema?.object?.configKey
      || props.modelSchema?.object?.code
      || props.modelSchema?.objectCode
      || props.modelSchema?.modelCode
      || ''
    const prefix = key ? `/ai/crud/${key}` : '/ai/crud/当前配置'
    return {
      api: prefix,
      listApi: `get@${prefix}/page`,
      detailApi: `get@${prefix}/:id`,
      createApi: `post@${prefix}`,
      updateApi: `put@${prefix}`,
      deleteApi: `delete@${prefix}/:id`,
    }
  })
  const crudPreviewBlock = computed(() => {
    const tableProps = tableZone.value?.props || {}
    const searchProps = searchZone.value?.props || {}
    const formOpenMode = resolveTableFormOpenMode(tableProps)
    // 查询区与表格区字段必须分离，禁止再把 search fieldRefs 合并进列表 fieldRefs
    const { fieldRefs, searchFieldRefs } = resolveCrudPreviewFieldBindings({
      tableFieldRefs: tableZone.value?.fieldRefs || [],
      searchFieldRefs: searchZone.value?.fieldRefs || [],
      fallbackTableFields: tableFields.value,
    })
    return {
      id: 'structured_crud_preview',
      blockType: 'AiCrudPage',
      label: tableProps.title || '业务列表',
      fieldRefs,
      props: {
        title: tableProps.title || '业务列表',
        rowKey: tableProps.rowKey || 'id',
        api: tableProps.api || defaultApiValues.value.api,
        listApi: tableProps.listApi || defaultApiValues.value.listApi,
        detailApi: tableProps.detailApi || defaultApiValues.value.detailApi,
        createApi: tableProps.createApi || defaultApiValues.value.createApi,
        updateApi: tableProps.updateApi || defaultApiValues.value.updateApi,
        deleteApi: tableProps.deleteApi || defaultApiValues.value.deleteApi,
        importApi: tableProps.importApi || '',
        exportApi: tableProps.exportApi || '',
        listDataField: tableProps.listDataField || 'records',
        listTotalField: tableProps.listTotalField || 'total',
        showSearch: searchZone.value?.enabled !== false,
        showPagination: tableProps.showPagination !== false,
        showImport: tableProps.showImport !== false,
        showExport: tableProps.showExport !== false,
        enableCustomQuery: tableProps.enableCustomQuery !== false,
        hideBatchDelete: tableProps.hideBatchDelete === true,
        hideSelection: tableProps.hideSelection === true,
        showRenderModeSwitch: tableProps.showRenderModeSwitch !== false,
        tableSize: tableProps.tableSize || 'medium',
        renderMode: tableProps.renderMode || 'table',
        bordered: tableProps.bordered === true,
        striped: tableProps.striped === true,
        maxHeight: tableProps.maxHeight,
        scrollX: tableProps.scrollX,
        searchGridCols: tableProps.searchGridCols || 4,
        searchLabelWidth: tableProps.searchLabelWidth || 'auto',
        searchMaxVisibleFields: tableProps.searchMaxVisibleFields || 3,
        searchEnableCollapse: tableProps.searchEnableCollapse !== false,
        searchFieldRefs,
        searchFieldSettings: searchProps.fieldSettings || {},
        formOpenMode,
        tabWorkspace: tableProps.tabWorkspace || {},
        modalType: resolveTableModalType(formOpenMode, tableProps),
        drawerPlacement: tableProps.drawerPlacement || 'right',
        modalWidth: tableProps.modalWidth || '800px',
        detailModalWidth: tableProps.detailModalWidth || 'min(1080px, 92vw)',
        editGridCols: tableProps.editGridCols || 1,
        editLabelPlacement: tableProps.editLabelPlacement || 'left',
        editLabelWidth: tableProps.editLabelWidth || 'auto',
        editSize: tableProps.editSize || 'medium',
        addButtonText: tableProps.addButtonText || '新增',
        exportButtonText: tableProps.exportButtonText || '导出',
        exportFileName: tableProps.exportFileName || '',
        events: tableProps.events || [],
        customActions: tableProps.customActions || [],
        crudHookRules: tableProps.crudHookRules || {},
        beforeSubmitRules: tableProps.beforeSubmitRules || [],
        publicParams: tableProps.publicParams || {},
        publicQuery: tableProps.publicQuery || {},
        formDefaultValues: tableProps.formDefaultValues || {},
        submitDefaultParams: tableProps.submitDefaultParams || {},
        previewLiveData: tableProps.previewLiveData === true,
        defaultSortField: tableProps.defaultSortField || 'id',
        defaultSortOrder: tableProps.defaultSortOrder || 'desc',
        fieldSettings: { ...(tableProps.fieldSettings || {}) },
        style: {
          widthMode: 'full',
          width: '100%',
          height: '100%',
        },
      },
    }
  })
  const treePreviewBlock = computed(() => ({
    id: 'structured_tree_preview',
    blockType: 'tree-panel',
    label: treeConfig.value.treeTitle || '筛选树',
    fieldRefs: [],
    props: {
      ...treeConfig.value,
      enabled: treeConfig.value.enabled !== false,
      treeTitle: treeConfig.value.treeTitle || '筛选树',
      sourceModelName: treeConfig.value.sourceModelName || '树形筛选',
      keyField: treeConfig.value.keyField || 'id',
      parentField: treeConfig.value.parentField || 'parentId',
      labelField: treeConfig.value.labelField || firstUsableField(),
      filterField: treeConfig.value.filterField || treeConfig.value.parentField || 'parentId',
      targetField: treeConfig.value.targetField || 'id',
      childrenField: treeConfig.value.childrenField || 'children',
      loadMode: treeConfig.value.loadMode || 'full',
      style: {
        widthMode: 'full',
        width: '100%',
        height: '100%',
      },
    },
  }))
  const activeFieldEditor = computed(() => {
    if (activeFieldZone.value === 'table') {
      return {
        zoneKey: 'table',
        title: '列表字段',
        emptyText: '当前没有列表字段',
        mode: 'table',
        selectedRefs: tableZone.value?.fieldRefs || [],
        settings: tableZone.value?.props?.fieldSettings || {},
        filter: field => isPageFieldVisible(field, 'table'),
      }
    }
    return {
      zoneKey: 'search',
      title: '查询字段',
      emptyText: '当前没有查询字段',
      mode: 'search',
      selectedRefs: searchZone.value?.fieldRefs || [],
      settings: searchZone.value?.props?.fieldSettings || {},
      filter: field => isPageFieldVisible(field, 'search'),
    }
  })
  function findZone(zoneKey) {
    return props.modelValue.zones?.find(zone => zone.zoneKey === zoneKey) || null
  }

  function resolveFields(zone, fallback) {
    if (!zone || zone.enabled === false)
      return []
    const refs = zone.fieldRefs?.length
      ? zone.fieldRefs
      : props.fields.filter(fallback).map(field => field.field)
    return refs.map(ref => fieldMap.value.get(ref)).filter(Boolean)
  }

  function firstUsableField() {
    return props.fields.find(field => !field.systemField)?.field || props.fields[0]?.field || 'name'
  }

  function uniqueRefs(refs = []) {
    return Array.from(new Set(refs.filter(Boolean)))
  }

  function resolveDefaultSearchComponentType(field = {}) {
    const componentType = field.componentType || field.dataType || 'input'
    if (field.dictType)
      return 'dictSelect'
    if (['int', 'bigint', 'decimal', 'double', 'float'].includes(field.dataType))
      return 'number'
    return componentType === 'inputNumber' ? 'number' : componentType
  }

  function resolveDefaultTableRenderType(field = {}) {
    const componentType = field.componentType || ''
    if (field.dictType)
      return 'dictTag'
    if (componentType === 'orgTreeSelect')
      return 'orgName'
    if (componentType === 'userSelect')
      return 'userName'
    if (componentType === 'regionTreeSelect')
      return 'regionName'
    if (componentType === 'fileUpload' || componentType === 'imageUpload')
      return componentType
    return ''
  }

  function isNameRenderType(renderType) {
    return ['orgName', 'userName', 'regionName', 'fileUpload', 'imageUpload'].includes(renderType)
  }

  function updateZoneRefs(zoneKey, refs) {
    patchZone(zoneKey, { fieldRefs: refs }, createGridPatchForZone(zoneKey, { fieldRefs: refs }))
  }

  function updateTableProp(key, value) {
    patchZone('table', {
      props: {
        ...(tableZone.value?.props || {}),
        [key]: value,
      },
    }, createGridPatchForZone('table', {
      props: {
        [key]: value,
      },
    }))
  }

  function normalizeFormOpenModePatch(value) {
    const formOpenMode = value === 'tabWorkspace' ? 'tabWorkspace' : (['modal', 'drawer', 'flat'].includes(value) ? value : 'modal')
    return {
      formOpenMode,
      modalType: ['modal', 'drawer'].includes(formOpenMode) ? formOpenMode : 'modal',
    }
  }

  function resolveTableFormOpenMode(tableProps = {}) {
    const value = tableProps.formOpenMode || tableProps.modalType || 'modal'
    const mode = String(value || '').trim()
    if (mode === 'tabWorkspace' || mode.toLowerCase() === 'tabworkspace')
      return 'tabWorkspace'
    const normalized = mode.toLowerCase()
    return ['modal', 'drawer', 'flat'].includes(normalized) ? normalized : 'modal'
  }

  function resolveTableModalType(formOpenMode, tableProps = {}) {
    if (['modal', 'drawer'].includes(formOpenMode))
      return formOpenMode
    const normalized = String(tableProps.modalType || '').trim().toLowerCase()
    return ['modal', 'drawer'].includes(normalized) ? normalized : 'modal'
  }

  function updateTableFormOpenMode(value) {
    const patch = normalizeFormOpenModePatch(value)
    patchZone('table', {
      props: {
        ...(tableZone.value?.props || {}),
        ...patch,
      },
    }, createGridPatchForZone('table', {
      props: patch,
    }))
  }

  function applyTableGlobalAlign(value) {
    const align = ['left', 'center', 'right'].includes(value) ? value : 'left'
    const zone = tableZone.value
    if (!zone)
      return
    const nextSettings = { ...(zone.props?.fieldSettings || {}) }
    ;(zone.fieldRefs || []).forEach((fieldName) => {
      nextSettings[fieldName] = {
        ...(nextSettings[fieldName] || {}),
        align,
      }
    })
    patchZone('table', {
      props: {
        ...(zone.props || {}),
        globalAlign: align,
        fieldSettings: nextSettings,
      },
    }, createGridPatchForZone('table', {
      props: {
        globalAlign: align,
        fieldSettings: nextSettings,
      },
    }))
  }

  function updateTableRowGap(value) {
    updateTableProp('rowGap', Math.max(0, Math.min(32, Number(value ?? 8))))
  }

  function updateTreeConfig(key, value) {
    patchZone('table', {
      props: {
        ...(tableZone.value?.props || {}),
        treeConfig: {
          ...(treeConfig.value || {}),
          enabled: true,
          [key]: value,
        },
      },
    })
  }

  function addTableEvent() {
    updateTableProp('events', [
      ...tableEvents.value,
      {
        id: `evt_${Date.now()}`,
        trigger: 'rowClick',
        action: 'none',
        targetPageKey: '',
        targetBlockId: '',
        description: '',
        params: [],
      },
    ])
  }

  function updateTableEvent(eventIdx, patch) {
    const list = [...tableEvents.value]
    list[eventIdx] = { ...(list[eventIdx] || {}), ...patch }
    updateTableProp('events', list)
  }

  function removeTableEvent(eventIdx) {
    const list = [...tableEvents.value]
    list.splice(eventIdx, 1)
    updateTableProp('events', list)
  }

  function addTableCustomAction() {
    updateTableProp('customActions', [
      ...tableCustomActions.value,
      {
        key: `custom_${Date.now()}`,
        label: '操作按钮',
        position: 'row',
        type: 'primary',
        actionType: 'route',
        routePath: '',
        targetPageKey: '',
        params: [],
      },
    ])
  }

  function updateTableCustomAction(actionIdx, patch) {
    const list = [...tableCustomActions.value]
    list[actionIdx] = { ...(list[actionIdx] || {}), ...patch }
    updateTableProp('customActions', list)
  }

  function removeTableCustomAction(actionIdx) {
    const list = [...tableCustomActions.value]
    list.splice(actionIdx, 1)
    updateTableProp('customActions', list)
  }

  function configSectionVisible(keywords = []) {
    const keyword = String(configKeyword.value || '').trim().toLowerCase()
    if (!keyword)
      return true
    return keywords.some(item => String(item || '').toLowerCase().includes(keyword))
  }

  function updateTableHookRules(rules) {
    patchZone('table', {
      props: {
        ...(tableZone.value?.props || {}),
        crudHookRules: rules || {},
        beforeSubmitRules: [],
      },
    }, createGridPatchForZone('table', {
      props: {
        crudHookRules: rules || {},
        beforeSubmitRules: [],
      },
    }))
  }

  function resolveTableDefaultParams() {
    const props = tableZone.value?.props || {}
    return {
      publicParams: props.publicParams || {},
      publicQuery: props.publicQuery || {},
      formDefaultValues: props.formDefaultValues || {},
      submitDefaultParams: props.submitDefaultParams || {},
    }
  }

  function updateTableDefaultParams(params = {}) {
    if (isSameDefaultParams(resolveTableDefaultParams(), params))
      return
    patchZone('table', {
      props: {
        ...(tableZone.value?.props || {}),
        publicParams: params.publicParams || {},
        publicQuery: params.publicQuery || {},
        formDefaultValues: params.formDefaultValues || {},
        submitDefaultParams: params.submitDefaultParams || {},
      },
    }, createGridPatchForZone('table', {
      props: {
        publicParams: params.publicParams || {},
        publicQuery: params.publicQuery || {},
        formDefaultValues: params.formDefaultValues || {},
        submitDefaultParams: params.submitDefaultParams || {},
      },
    }))
  }

  function isSameDefaultParams(left = {}, right = {}) {
    return JSON.stringify(normalizeDefaultParams(left)) === JSON.stringify(normalizeDefaultParams(right))
  }

  function normalizeDefaultParams(source = {}) {
    return ['publicParams', 'publicQuery', 'formDefaultValues', 'submitDefaultParams'].reduce((result, key) => {
      const params = source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])
        ? source[key]
        : {}
      result[key] = Object.keys(params).sort().reduce((next, paramKey) => {
        next[paramKey] = params[paramKey]
        return next
      }, {})
      return result
    }, {})
  }

  function eventTriggerText(trigger) {
    return eventTriggerOptions.find(item => item.value === trigger)?.label || trigger || '触发'
  }

  function eventActionText(action) {
    return eventActionOptions.find(item => item.value === action)?.label || action || '动作'
  }

  function updateZoneFieldSetting(zoneKey, payload) {
    const zone = findZone(zoneKey)
    if (!zone || !payload?.field)
      return
    patchZone(zoneKey, {
      props: {
        ...(zone.props || {}),
        fieldSettings: {
          ...(zone.props?.fieldSettings || {}),
          [payload.field]: payload.settings,
        },
      },
    }, createGridPatchForZone(zoneKey, {
      props: {
        fieldSettings: {
          ...(zone.props?.fieldSettings || {}),
          [payload.field]: payload.settings,
        },
      },
    }))
  }

  function openFieldPanel(zoneKey) {
    activeFieldZone.value = zoneKey
    configCollapsed.value = false
    nextTick(() => {
      fieldEditorAnchor.value?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
    })
  }

  function collapseConfigPanel() {
    configExpanded.value = false
    configCollapsed.value = true
  }

  function normalizeParamName(value) {
    return String(value || '').trim().replace(/[^\w.$-]/g, '')
  }

  function patchZone(zoneKey, patch, gridPatch = null) {
    const zones = (props.modelValue.zones || []).map((zone) => {
      if (zone.zoneKey !== zoneKey)
        return zone
      return {
        ...zone,
        ...patch,
        props: patch.props ? { ...(zone.props || {}), ...patch.props } : zone.props || {},
      }
    })
    const nextValue = { ...props.modelValue, zones }
    if (gridPatch) {
      nextValue.listGridLayout = patchCrudGridLayout(props.modelValue.listGridLayout, gridPatch)
      nextValue.pages = patchPagesGridLayout(props.modelValue.pages || [], 'list', nextValue.listGridLayout)
    }
    emit('update:modelValue', nextValue)
  }

  function patchPagesGridLayout(pages = [], pageKey = 'list', gridLayout) {
    if (!Array.isArray(pages) || !pages.length)
      return pages
    return pages.map(page => page.pageKey === pageKey
      ? { ...page, gridLayout }
      : page)
  }

  function createGridPatchForZone(zoneKey, patch) {
    if (zoneKey === 'search') {
      return {
        zoneKey: 'search',
        blockTypes: ['AiCrudPage', 'search-form'],
        fieldRefs: patch.fieldRefs,
        props: patch.props,
      }
    }
    if (zoneKey === 'table') {
      return {
        zoneKey: 'table',
        blockTypes: ['AiCrudPage', 'data-table', 'AiTable'],
        fieldRefs: patch.fieldRefs,
        props: patch.props,
      }
    }
    return null
  }

  function patchCrudGridLayout(layout = {}, gridPatch = {}) {
    if (!layout?.items?.length)
      return layout
    return {
      ...layout,
      items: patchCrudGridLayoutItems(layout.items || [], gridPatch),
    }
  }

  function resetSearchFields() {
    updateZoneRefs('search', props.fields.filter(field => isPageFieldVisible(field, 'search')).map(field => field.field))
  }

  function resetTableFields() {
    updateZoneRefs('table', props.fields.filter(field => isPageFieldVisible(field, 'table')).map(field => field.field))
  }

  __impl.findZone = findZone
  __impl.resolveFields = resolveFields
  __impl.firstUsableField = firstUsableField
  __impl.uniqueRefs = uniqueRefs
  __impl.resolveDefaultSearchComponentType = resolveDefaultSearchComponentType
  __impl.resolveDefaultTableRenderType = resolveDefaultTableRenderType
  __impl.isNameRenderType = isNameRenderType
  __impl.updateZoneRefs = updateZoneRefs
  __impl.updateTableProp = updateTableProp
  __impl.normalizeFormOpenModePatch = normalizeFormOpenModePatch
  __impl.resolveTableFormOpenMode = resolveTableFormOpenMode
  __impl.resolveTableModalType = resolveTableModalType
  __impl.updateTableFormOpenMode = updateTableFormOpenMode
  __impl.applyTableGlobalAlign = applyTableGlobalAlign
  __impl.updateTableRowGap = updateTableRowGap
  __impl.updateTreeConfig = updateTreeConfig
  __impl.addTableEvent = addTableEvent
  __impl.updateTableEvent = updateTableEvent
  __impl.removeTableEvent = removeTableEvent
  __impl.addTableCustomAction = addTableCustomAction
  __impl.updateTableCustomAction = updateTableCustomAction
  __impl.removeTableCustomAction = removeTableCustomAction
  __impl.configSectionVisible = configSectionVisible
  __impl.updateTableHookRules = updateTableHookRules
  __impl.resolveTableDefaultParams = resolveTableDefaultParams
  __impl.updateTableDefaultParams = updateTableDefaultParams
  __impl.isSameDefaultParams = isSameDefaultParams
  __impl.normalizeDefaultParams = normalizeDefaultParams
  __impl.eventTriggerText = eventTriggerText
  __impl.eventActionText = eventActionText
  __impl.updateZoneFieldSetting = updateZoneFieldSetting
  __impl.openFieldPanel = openFieldPanel
  __impl.collapseConfigPanel = collapseConfigPanel
  __impl.normalizeParamName = normalizeParamName
  __impl.patchZone = patchZone
  __impl.patchPagesGridLayout = patchPagesGridLayout
  __impl.createGridPatchForZone = createGridPatchForZone
  __impl.patchCrudGridLayout = patchCrudGridLayout
  __impl.resetSearchFields = resetSearchFields
  __impl.resetTableFields = resetTableFields

  return {
    props, emit, __impl, mut, addTableCustomAction, addTableEvent, applyTableGlobalAlign, collapseConfigPanel,
    configSectionVisible, createGridPatchForZone, eventActionText, eventTriggerText, findZone, firstUsableField,
    isNameRenderType, isSameDefaultParams, normalizeDefaultParams, normalizeFormOpenModePatch, normalizeParamName,
    openFieldPanel, patchCrudGridLayout, patchPagesGridLayout, patchZone, removeTableCustomAction, removeTableEvent,
    resetSearchFields, resetTableFields, resolveDefaultSearchComponentType, resolveDefaultTableRenderType,
    resolveFields, resolveTableDefaultParams, resolveTableFormOpenMode, resolveTableModalType, uniqueRefs,
    updateTableCustomAction, updateTableDefaultParams, updateTableEvent, updateTableFormOpenMode, updateTableHookRules,
    updateTableProp, updateTableRowGap, updateTreeConfig, updateZoneFieldSetting, updateZoneRefs, queryTypeOptions,
    searchComponentOptions, tableRenderOptions, treeLoadModeOptions, sortOrderOptions, alignOptions, sizeOptions,
    renderModeOptions, formOpenModeOptions, drawerPlacementOptions, labelPlacementOptions, fixedOptions,
    columnClickActionOptions, eventTriggerOptions, eventActionOptions, customActionPositionOptions,
    customActionTypeOptions, buttonTypeOptions, FieldOrderEditor, fieldMap, searchZone, tableZone, activeFieldZone,
    configExpanded, configCollapsed, fieldEditorAnchor, fieldOptions, pageTargetOptions, sortFieldOptions,
    searchFields, tableFields, treeConfig, tableEvents, tableCustomActions, configKeyword, defaultSortText,
    defaultApiValues, crudPreviewBlock, treePreviewBlock, activeFieldEditor,
  }
}
