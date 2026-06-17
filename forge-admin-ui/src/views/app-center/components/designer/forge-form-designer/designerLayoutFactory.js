const MAX_FORM_GRID_COLUMNS = 6

export function createForgeLayoutComponent(componentKey = 'title', schema = {}) {
  const gridColumns = clampGridColumns(schema?.layout?.gridColumns, 2)
  const id = `cmp_${componentKey || 'layout'}_${Date.now()}`
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
    const columnCount = Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, gridColumns))
    return {
      id,
      componentKey: 'row',
      label: `${columnCount} 列栅格`,
      props: { gutter: 16, columns: columnCount },
      layout: { span: gridColumns, align: 'left' },
      children: Array.from({ length: columnCount }).map((_, index) => ({
        id: `${id}_col_${index + 1}`,
        componentKey: 'col',
        label: `第 ${index + 1} 列`,
        props: { span: 1 },
        layout: { span: 1, align: 'left' },
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
        apiConfig: {
          list: `get@${apiBase}/page`,
          detail: `post@${apiBase}/getById`,
          add: `post@${apiBase}/add`,
          update: `post@${apiBase}/edit`,
          delete: `post@${apiBase}/remove/:id`,
        },
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
          modalWidth: '900px',
          detailModalWidth: 'min(1080px, 92vw)',
          loadDetailOnEdit: true,
          showSearch: true,
          showPagination: true,
          pageSize: 10,
          tableSize: 'small',
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
  const fieldCode = `field_${Date.now().toString(36)}`
  return {
    id: `cmp_${fieldCode}`,
    componentKey,
    label,
    fieldBinding: {
      mode: 'field',
      fieldCode,
      columnName: fieldCode,
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
      span: ['textarea', 'fileUpload', 'imageUpload', 'daterange', 'datetimerange', 'timerange'].includes(componentKey) ? Math.min(2, gridColumns) : 1,
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

function buildTemplatePlaceholder(componentKey, label) {
  if (['select', 'dictSelect', 'radio', 'radioButton', 'checkbox', 'date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'time', 'timerange', 'userSelect', 'orgTreeSelect', 'regionTreeSelect', 'treeSelect', 'customSelect', 'color'].includes(componentKey))
    return `请选择${label}`
  return `请输入${label}`
}

function clampGridColumns(value, fallback = 2) {
  const number = Number(value)
  if (!Number.isFinite(number))
    return fallback
  return Math.max(1, Math.min(MAX_FORM_GRID_COLUMNS, number))
}

function buildDefaultApiBase(schema = {}) {
  const rawKey = String(schema?.objectCode || schema?.formKey || 'business/object').trim()
  const normalized = rawKey
    .replace(/^form_/, '')
    .replace(/_/g, '-')
    .replace(/[^a-z0-9/-]/gi, '')
    .replace(/\/+/g, '/')
  return `/${normalized || 'business/object'}`
}
