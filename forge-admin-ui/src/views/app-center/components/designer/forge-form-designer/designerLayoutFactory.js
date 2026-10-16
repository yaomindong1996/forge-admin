import { createPageWidgetComponent, isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import {
  camelToSnake,
  generateFieldCode,
  isGenericDesignerComponentId,
} from '../form-first/formDesignerSchema'

const MAX_FORM_GRID_COLUMNS = 24
const DEFAULT_ROW_CELL_COUNT = 4

export function createForgeLayoutComponent(componentKey = 'title', schema = {}) {
  const gridColumns = clampGridColumns(schema?.layout?.gridColumns, 2)
  const id = `cmp_${componentKey || 'layout'}_${Date.now()}`
  if (isPageWidgetComponentKey(componentKey)) {
    return createPageWidgetComponent(componentKey, {
      id,
      gridColumns,
    })
  }
  if (componentKey === 'card') {
    return {
      id,
      componentKey: 'card',
      label: '新分组',
      props: {
        header: '新分组',
        size: 'small',
        bordered: true,
        embedded: false,
        segmented: false,
        hoverable: false,
      },
      layout: { span: gridColumns, align: 'left' },
      children: [],
    }
  }
  if (componentKey === 'subTable') {
    // 关联子表容器：承载子表分区语义（relationKey + 展示方式），不承载字段。
    return {
      id,
      componentKey: 'subTable',
      label: '关联子表',
      props: {
        header: '关联子表',
        relationKey: '',
        displayMode: 'inline_grid',
      },
      layout: { span: gridColumns, align: 'left' },
    }
  }
  if (componentKey === 'tabs') {
    return {
      id,
      componentKey: 'tabs',
      label: '标签页',
      props: {
        type: 'line',
        size: 'medium',
        placement: 'top',
        trigger: 'click',
        animated: true,
        closable: false,
        addable: false,
        justifyContent: 'start',
        tabsPadding: 0,
      },
      layout: { span: gridColumns, align: 'left' },
      children: [
        {
          id: `${id}_pane`,
          componentKey: 'tabPane',
          label: '标签一',
          props: { label: '标签一', name: `${id}_pane` },
          layout: { span: gridColumns, align: 'left' },
          children: [],
        },
      ],
    }
  }
  if (componentKey === 'collapse') {
    return {
      id,
      componentKey: 'collapse',
      label: '折叠面板',
      props: {
        accordion: false,
        arrowPlacement: 'left',
        displayDirective: 'if',
        triggerAreas: ['main', 'arrow'],
      },
      layout: { span: gridColumns, align: 'left' },
      children: [
        {
          id: `${id}_item`,
          componentKey: 'collapseItem',
          label: '分组一',
          props: { title: '分组一', name: `${id}_item` },
          layout: { span: gridColumns, align: 'left' },
          children: [],
        },
      ],
    }
  }
  if (componentKey === 'button') {
    return {
      id,
      componentKey: 'button',
      label: '按钮',
      props: {
        text: '按钮',
        type: 'primary',
        size: 'medium',
        secondary: false,
        dashed: false,
        round: false,
        block: false,
        disabled: false,
      },
      layout: { span: 1, align: 'left' },
      children: [],
    }
  }
  if (componentKey === 'row') {
    const columnCount = DEFAULT_ROW_CELL_COUNT
    const columnSpan = Math.max(1, Math.floor(MAX_FORM_GRID_COLUMNS / columnCount))
    return {
      id,
      componentKey: 'row',
      label: `${columnCount} 列栅格`,
      props: { gutter: 16, columns: MAX_FORM_GRID_COLUMNS },
      layout: { span: gridColumns, align: 'left' },
      children: Array.from({ length: columnCount }).map((_, index) => ({
        id: `${id}_col_${index + 1}`,
        componentKey: 'col',
        label: `第 ${index + 1} 列`,
        props: { span: columnSpan },
        layout: { span: columnSpan, align: 'left' },
        children: [],
      })),
    }
  }
  if (componentKey === 'table') {
    const columnCount = Math.max(2, Math.min(MAX_FORM_GRID_COLUMNS, gridColumns))
    return {
      id,
      componentKey: 'table',
      label: '表格布局',
      props: { columns: columnCount },
      layout: { span: gridColumns, align: 'left' },
      children: Array.from({ length: columnCount }).map((_, index) => ({
        id: `${id}_cell_${index + 1}`,
        componentKey: 'tableGrid',
        label: `单元格 ${index + 1}`,
        props: { span: 1 },
        layout: { span: 1, align: 'left' },
        children: [],
      })),
    }
  }
  if (['AiCrudPage', 'crudBlock'].includes(componentKey)) {
    const apiBase = buildDefaultApiBase(schema)
    return {
      id,
      componentKey: 'AiCrudPage',
      label: 'CRUD 区块',
      props: {
        title: 'CRUD 区块',
        description: '查询、表格、操作按钮的业务区块',
        apiBase,
        rowKey: 'id',
        apiConfig: buildDefaultCrudApiConfig(apiBase),
        crudOptions: {
          searchGridCols: Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, gridColumns)),
          searchLabelWidth: 'auto',
          searchEnableCollapse: true,
          searchMaxVisibleFields: 3,
          searchYGap: 16,
          editGridCols: Math.max(1, Math.min(3, gridColumns)),
          editLabelWidth: 'auto',
          editLabelPlacement: 'left',
          editLabelAlign: 'right',
          editSize: 'medium',
          editShowFeedback: true,
          editXGap: 16,
          editYGap: 8,
          modalType: 'modal',
          formOpenMode: 'modal',
          tabWorkspace: {
            maxTabs: 8,
            reuseRecordTab: true,
            closeAfterSave: false,
            showDirtyMark: true,
          },
          modalWidth: '900px',
          detailModalWidth: 'min(1080px, 92vw)',
          loadDetailOnEdit: true,
          showSearch: true,
          showPagination: true,
          pageSize: 10,
          tableSize: 'medium',
          renderMode: 'table',
          showRenderModeSwitch: true,
          hideToolbar: false,
          hideAdd: false,
          hideBatchDelete: false,
          hideSelection: false,
          striped: false,
          bordered: false,
          showImport: false,
          showExport: false,
          showExportTasks: true,
        },
      },
      layout: { span: gridColumns, align: 'left' },
      children: [
        createForgeFieldTemplateComponent({ componentKey: 'input', label: '查询条件' }, schema),
        createForgeLayoutComponent('table', schema),
      ],
    }
  }
  if (componentKey === 'AiFormSectionTitle') {
    return {
      id,
      componentKey: 'AiFormSectionTitle',
      label: '表单分隔线',
      props: {
        title: '表单分隔线',
      },
      layout: { span: gridColumns, align: 'left' },
      children: [],
    }
  }
  return {
    id,
    componentKey: 'title',
    label: '分组标题',
    props: { description: '' },
    layout: { span: gridColumns, align: 'left' },
    children: [],
  }
}

export function createForgeFieldTemplateComponent(template = {}, schema = {}) {
  const gridColumns = clampGridColumns(schema?.layout?.gridColumns, 2)
  const componentKey = template.componentKey || 'input'
  const label = template.label || '字段'
  const fieldCode = reserveTemplateFieldCode(componentKey, label, schema)
  const componentId = reserveTemplateComponentId(`cmp_${fieldCode}`, schema)
  return {
    id: componentId,
    componentKey,
    label,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName: camelToSnake(fieldCode),
      createIfMissing: true,
      source: 'designer',
      locked: false,
    },
    props: {
      placeholder: buildTemplatePlaceholder(componentKey, label),
      ...buildTemplateDefaultProps(componentKey),
      ...(template.props || {}),
    },
    layout: {
      span: ['textarea', 'fileUpload', 'imageUpload', 'barcodeScanner', 'daterange', 'datetimerange', 'timerange'].includes(componentKey) ? Math.min(2, gridColumns) : 1,
      align: 'left',
    },
    validation: {
      required: false,
      requiredMessage: '',
    },
    visibility: {
      hidden: false,
      readonly: false,
    },
    children: [],
  }
}

function buildTemplateDefaultProps(componentKey = '') {
  const map = {
    barcodeScanner: {
      allowManualInput: true,
      timeoutMs: 30000,
      formats: ['CODE128', 'CODE39', 'EAN13', 'EAN8', 'UPC', 'ITF14'],
    },
    radioButton: {
      options: buildDefaultOptions(),
    },
    select: {
      options: buildDefaultOptions(),
    },
    radio: {
      options: buildDefaultOptions(),
    },
    checkbox: {
      options: buildDefaultOptions(),
    },
    transfer: {
      sourceTitle: '可选项',
      targetTitle: '已选项',
      filterable: true,
      dataSourceType: 'static',
      options: [
        { label: '选项一', value: 'option1' },
        { label: '选项二', value: 'option2' },
        { label: '选项三', value: 'option3' },
      ],
      optionSource: {
        api: '',
        method: 'get',
        paramsText: '{}',
        recordsField: 'records',
        labelField: 'label',
        valueField: 'value',
        disabledField: 'disabled',
      },
    },
    cascader: {
      options: [
        {
          label: '一级选项',
          value: 'parent',
          children: buildDefaultOptions(),
        },
      ],
    },
    treeSelect: {
      options: [
        {
          label: '节点一',
          key: 'node_1',
          value: 'node_1',
          children: [{ label: '子节点', key: 'node_1_1', value: 'node_1_1' }],
        },
      ],
    },
    slider: {
      min: 0,
      max: 100,
      step: 1,
    },
    rate: {
      count: 5,
      allowHalf: true,
    },
    color: {
      showAlpha: true,
      modes: ['hex'],
    },
    text: {
      defaultValue: '展示文本',
    },
  }
  return map[componentKey] || {}
}

function buildDefaultOptions() {
  return [
    { label: '选项一', value: '1' },
    { label: '选项二', value: '2' },
  ]
}

function reserveTemplateFieldCode(componentKey = '', label = '', schema = {}) {
  const usedCodes = collectSchemaFieldCodes(schema)
  const base = buildTemplateFieldCodeBase(componentKey, label)
  if (!usedCodes.has(base))
    return base
  for (let index = 2; index < 1000; index += 1) {
    const candidate = `${base}${index}`
    if (!usedCodes.has(candidate))
      return candidate
  }
  return `${base}${Date.now().toString(36)}`
}

function reserveTemplateComponentId(baseId = '', schema = {}) {
  const usedIds = collectSchemaComponentIds(schema)
  const base = String(baseId || 'cmp_field').trim() || 'cmp_field'
  if (!usedIds.has(base))
    return base
  for (let index = 2; index < 1000; index += 1) {
    const candidate = `${base}_${index}`
    if (!usedIds.has(candidate))
      return candidate
  }
  return `${base}_${Date.now().toString(36)}`
}

function buildTemplateFieldCodeBase(componentKey = '', label = '') {
  const generated = generateFieldCode(label)
  if (generated && !isGenericDesignerFieldCode(generated))
    return generated
  const key = String(componentKey || 'input')
    .replace(/[^a-z0-9]/gi, '')
    .replace(/^\d+/, '')
  const suffix = key ? `${key[0].toUpperCase()}${key.slice(1)}` : 'Input'
  return `field${suffix}`
}

function collectSchemaFieldCodes(schema = {}) {
  const codes = new Set()
  const walk = (components = []) => {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      const fieldCode = component?.fieldBinding?.fieldCode || component?.field || component?.name
      if (fieldCode)
        codes.add(fieldCode)
      if (Array.isArray(component?.children))
        walk(component.children)
    })
  }
  walk(schema?.components || [])
  ;(Array.isArray(schema?.settings?.formAssets) ? schema.settings.formAssets : []).forEach((asset) => {
    walk(asset?.schema?.components || asset?.components || [])
  })
  ;(Array.isArray(schema?.forms) ? schema.forms : []).forEach((form) => {
    walk(form?.schema?.components || form?.components || [])
  })
  return codes
}

function collectSchemaComponentIds(schema = {}) {
  const ids = new Set()
  const walk = (components = []) => {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (component?.id)
        ids.add(String(component.id))
      if (Array.isArray(component?.children))
        walk(component.children)
    })
  }
  walk(schema?.components || [])
  ;(Array.isArray(schema?.settings?.formAssets) ? schema.settings.formAssets : []).forEach((asset) => {
    walk(asset?.schema?.components || asset?.components || [])
  })
  ;(Array.isArray(schema?.forms) ? schema.forms : []).forEach((form) => {
    walk(form?.schema?.components || form?.components || [])
  })
  return ids
}

function isGenericDesignerFieldCode(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return true
  if (/^field[0-9a-z]{4,}$/i.test(text))
    return true
  return isGenericDesignerComponentId(`cmp_${text}`, text)
}

function buildTemplatePlaceholder(componentKey, label) {
  if (['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'transfer', 'date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'time', 'timerange', 'userSelect', 'orgTreeSelect', 'regionTreeSelect', 'treeSelect', 'customSelect', 'color'].includes(componentKey))
    return `请选择${label}`
  return `请填写${label}`
}

function clampGridColumns(value, fallback = 2) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return fallback
  return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, number))
}

/**
 * 动态 CRUD 接口前缀必须是 `/ai/crud/{configKey}`。
 * 优先使用 schema.configKey（常含领域前缀，如 hr_business_object_xxx）；
 * 历史实现曾把 objectCode/formKey 收成 kebab `/business-object-xxx-default-form`，
 * 运行时会 404；也不要剥掉可用的完整 configKey 再丢领域前缀。
 */
function buildDefaultApiBase(schema = {}) {
  const preferred = String(schema?.configKey || '').trim()
  const rawKey = String(
    preferred
    || schema?.objectCode
    || schema?.formKey
    || 'business_object',
  ).trim()
  const configKey = rawKey
    .replace(/^form_/i, '')
    .replace(/-/g, '_')
    .replace(/[^a-z0-9_]/gi, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    // formKey（*_default_form）不是动态 CRUD configKey
    .replace(/_default_form$/i, '')
  return `/ai/crud/${configKey || 'business_object'}`
}

function buildDefaultCrudApiConfig(apiBase = '/ai/crud/business_object') {
  const prefix = String(apiBase || '').trim() || '/ai/crud/business_object'
  return {
    list: `get@${prefix}/page`,
    detail: `get@${prefix}/:id`,
    create: `post@${prefix}`,
    update: `put@${prefix}`,
    delete: `delete@${prefix}/:id`,
  }
}
