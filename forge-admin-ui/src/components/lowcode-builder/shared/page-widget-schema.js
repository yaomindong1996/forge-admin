/**
 * @fileoverview 页面挂件 Schema — 运行时函数与桥接数据
 * @description pageWidgetCatalog 和 pageWidgetComponentKeys 由 designer-core 桥接生成，
 *   其余运行时函数（createPageWidgetDefaultProps、createWidgetDataBinding 等）保留本地。
 */
import { toPageWidgetCatalog, toPageWidgetComponentKeys } from '@/components/lowcode-builder/designer-core'

export const pageWidgetComponentKeys = toPageWidgetComponentKeys()

export function isPageWidgetComponentKey(componentKey = '') {
  return pageWidgetComponentKeys.includes(componentKey)
}

export const pageWidgetCatalog = toPageWidgetCatalog()

export function resolvePageWidgetMeta(componentKey = '') {
  return pageWidgetCatalog.find(item => item.componentKey === componentKey || item.blockType === componentKey) || null
}

export function createPageWidgetDefaultProps(componentKey = '') {
  if (componentKey === 'rich-text') {
    return {
      title: '富文本标题',
      content: '<h3>富文本内容</h3><p>支持加粗、斜体、标题、列表、引用、链接和源码编辑。</p>',
      placeholder: '请输入富文本内容',
      editorMode: 'visual',
      toolbarMode: 'default',
      editorModeName: 'default',
      excludeToolbarKeys: [],
      menuConfigText: '{}',
      readonly: false,
      minHeight: 180,
      backgroundColor: 'transparent',
      bordered: false,
      dataBinding: createWidgetDataBinding('content', { contentField: 'content', titleField: 'title' }),
    }
  }
  if (componentKey === 'transfer') {
    return {
      title: '穿梭框',
      sourceTitle: '可选项',
      targetTitle: '已选项',
      filterable: true,
      virtualScroll: false,
      disabled: false,
      dataSourceType: 'static',
      options: [
        { label: '选项一', value: 'option1' },
        { label: '选项二', value: 'option2' },
        { label: '选项三', value: 'option3' },
      ],
      value: ['option1'],
      optionSource: {
        api: '',
        method: 'get',
        paramsText: '{}',
        recordsField: 'records',
        labelField: 'label',
        valueField: 'value',
        disabledField: 'disabled',
      },
    }
  }
  if (componentKey === 'watermark') {
    return {
      content: '内部资料',
      previewText: '水印覆盖区域',
      cross: false,
      debug: false,
      fontSize: 14,
      fontFamily: '',
      fontStyle: 'normal',
      fontVariant: '',
      fontWeight: 400,
      fontColor: 'rgba(128, 128, 128, .3)',
      fullscreen: false,
      globalRotate: 0,
      lineHeight: 14,
      height: 32,
      image: '',
      imageHeight: undefined,
      imageOpacity: 1,
      imageWidth: undefined,
      rotate: 0,
      selectable: true,
      textAlign: 'left',
      width: 32,
      xGap: 0,
      xOffset: 0,
      yGap: 0,
      yOffset: 0,
      zIndex: 10,
      dataBinding: createWidgetDataBinding('content', { contentField: 'content' }),
    }
  }
  if (componentKey === 'vue-component') {
    return {
      componentName: 'CustomBusinessWidget',
      title: 'Vue 组件',
      description: '可维护 Vue SFC 风格代码。默认安全预览仅渲染 template，不执行 script。',
      templateCode: '<section class="custom-widget"><h3>{{ title }}</h3><p>{{ description }}</p></section>',
      scriptCode: 'export default {\n  props: {\n    title: String,\n    description: String\n  }\n}',
      styleCode: '.custom-widget {\n  padding: 16px;\n  border-radius: 12px;\n  background: #eff6ff;\n  color: #1e3a8a;\n}',
      propsJson: '{\n  "title": "业务组件",\n  "description": "这里展示组件 props 驱动的内容"\n}',
      previewMode: 'safe-template',
      safeMode: true,
    }
  }
  if (componentKey === 'html-tag') {
    return {
      tagName: 'section',
      textContent: 'HTML 标签内容',
      htmlContent: '<strong>HTML 内容</strong><p>支持安全标签和属性。</p>',
      renderMode: 'html',
      attributesText: '{\n  "class": "html-panel",\n  "aria-label": "说明区块"\n}',
      semanticRole: 'region',
      allowHtml: true,
      sanitize: true,
      dataBinding: createWidgetDataBinding('content', { contentField: 'htmlContent', titleField: 'textContent' }),
    }
  }
  if (componentKey === 'markdown') {
    return {
      title: 'Markdown 文档',
      content: '## Markdown 标题\n\n- 支持列表\n- 支持 **加粗** 和 *斜体*\n\n> 这是一段引用\n\n```js\nconst hello = "Forge"\n```',
      showTitle: true,
      previewMode: 'split',
      height: 320,
      breaks: true,
      sanitize: true,
      codeTheme: 'light',
      disabledMenus: [],
      dataBinding: createWidgetDataBinding('content', { contentField: 'content', titleField: 'title' }),
    }
  }
  if (componentKey === 'barcode') {
    return {
      title: '条形码',
      value: 'FORGE-2026-0001',
      format: 'CODE128',
      showText: true,
      barWidth: 2,
      barHeight: 72,
      fontSize: 14,
      margin: 8,
      lineColor: '#0f172a',
      background: 'transparent',
      dataBinding: createWidgetDataBinding('value', { valueField: 'value', titleField: 'title' }),
    }
  }
  if (componentKey === 'qrcode') {
    return {
      title: '二维码',
      value: 'https://forge.local',
      size: 132,
      margin: 0,
      foreground: '#0f172a',
      background: 'transparent',
      cornerColor: '#0f172a',
      errorCorrectionLevel: 'Q',
      dotsType: 'square',
      cornersSquareType: 'square',
      cornersDotType: 'square',
      showText: true,
      dataBinding: createWidgetDataBinding('value', { valueField: 'value', titleField: 'title' }),
    }
  }
  if (componentKey === 'calendar') {
    return {
      title: '日历',
      value: Date.now(),
      size: 'medium',
      showTitle: true,
      dataBinding: createWidgetDataBinding('value', { valueField: 'date' }),
    }
  }
  if (componentKey === 'code') {
    return {
      title: '代码',
      code: 'const message = "Forge Admin"\nconsole.log(message)',
      language: 'javascript',
      showLineNumbers: true,
      wordWrap: true,
      trim: true,
      dataBinding: createWidgetDataBinding('content', { contentField: 'code' }),
    }
  }
  if (componentKey === 'countdown') {
    return {
      title: '倒计时',
      duration: 3600000,
      active: true,
      precision: 0,
      separator: ':',
      dataBinding: createWidgetDataBinding('value', { valueField: 'duration' }),
    }
  }
  if (componentKey === 'descriptions') {
    return {
      title: '描述',
      column: 2,
      bordered: false,
      labelPlacement: 'left',
      size: 'small',
      itemsText: '[\n  { "label": "业务对象", "value": "订单管理" },\n  { "label": "状态", "value": "启用" },\n  { "label": "负责人", "value": "管理员" },\n  { "label": "更新时间", "value": "2026-06-25" }\n]',
      dataBinding: createWidgetDataBinding('items', { labelField: 'label', valueField: 'value' }),
    }
  }
  if (componentKey === 'announcement') {
    return {
      title: '公示标题',
      content: '这里展示公告、公示、提示或业务说明内容。',
      type: 'info',
      bordered: false,
      showIcon: true,
      closable: false,
      dataBinding: createWidgetDataBinding('content', { contentField: 'content', titleField: 'title' }),
    }
  }
  if (componentKey === 'list') {
    return {
      title: '列表',
      bordered: false,
      hoverable: true,
      size: 'small',
      itemsText: '[\n  { "title": "审批待办", "description": "等待业务负责人处理", "meta": "刚刚" },\n  { "title": "数据同步", "description": "订单数据已完成同步", "meta": "10 分钟前" },\n  { "title": "系统通知", "description": "低代码页面配置已更新", "meta": "今天" }\n]',
      dataBinding: createWidgetDataBinding('items', { titleField: 'title', descriptionField: 'description', metaField: 'meta' }),
    }
  }
  if (componentKey === 'log') {
    return {
      title: '日志',
      log: '[10:00:01] 页面配置加载完成\n[10:00:03] 查询接口 GET /page\n[10:00:05] 渲染完成',
      rows: 6,
      fontSize: 12,
      trim: true,
      language: 'log',
      dataBinding: createWidgetDataBinding('content', { contentField: 'log' }),
    }
  }
  if (componentKey === 'number-animation') {
    return {
      title: '数值动画',
      from: 0,
      to: 12836,
      precision: 0,
      duration: 1200,
      prefix: '',
      suffix: ' 条',
      color: '#2563eb',
      dataBinding: createWidgetDataBinding('value', { valueField: 'value' }),
    }
  }
  if (componentKey === 'breadcrumb') {
    return {
      itemsText: '[\n  { "label": "首页" },\n  { "label": "应用中心" },\n  { "label": "订单管理" }\n]',
      dataBinding: createWidgetDataBinding('items', { labelField: 'label' }),
    }
  }
  if (componentKey === 'menu') {
    return {
      mode: 'vertical',
      value: 'overview',
      collapsed: false,
      optionsText: '[\n  { "label": "概览", "key": "overview" },\n  { "label": "订单列表", "key": "orders" },\n  { "label": "数据统计", "key": "stats" }\n]',
      dataBinding: createWidgetDataBinding('items', { labelField: 'label', valueField: 'key', childrenField: 'children' }),
    }
  }
  if (componentKey === 'pagination') {
    return {
      page: 1,
      pageSize: 10,
      itemCount: 128,
      showSizePicker: true,
      simple: false,
      dataBinding: createWidgetDataBinding('total', { totalField: 'total' }),
    }
  }
  if (componentKey === 'split') {
    return {
      direction: 'horizontal',
      defaultSize: 0.38,
      min: 0.2,
      max: 0.8,
      pane1Title: '左侧面板',
      pane1Content: '这里放置筛选、导航或说明内容。',
      pane2Title: '右侧面板',
      pane2Content: '这里放置列表、详情或主要业务内容。',
    }
  }
  if (componentKey === 'audio-player') {
    return {
      title: '音频播放器',
      src: '',
      controls: true,
      autoplay: false,
      loop: false,
      muted: false,
      width: '100%',
      dataBinding: createWidgetDataBinding('value', { valueField: 'src', titleField: 'title' }),
    }
  }
  if (componentKey === 'video-player') {
    return {
      title: '视频播放器',
      src: '',
      controls: true,
      autoplay: false,
      loop: false,
      muted: false,
      poster: '',
      width: '100%',
      height: 'auto',
      dataBinding: createWidgetDataBinding('value', { valueField: 'src', titleField: 'title' }),
    }
  }
  if (componentKey === 'avatar') {
    return {
      title: '头像',
      src: '',
      text: '',
      size: 48,
      round: true,
      backgroundColor: '#ccc',
      dataBinding: createWidgetDataBinding('value', { valueField: 'src' }),
    }
  }
  if (componentKey === 'iframe') {
    return {
      title: '内嵌框架',
      src: '',
      width: '100%',
      height: '300px',
      border: 'none',
      sandbox: '',
      dataBinding: createWidgetDataBinding('value', { valueField: 'src', titleField: 'title' }),
    }
  }
  return {}
}

export function createWidgetDataBinding(target = 'items', overrides = {}) {
  return {
    enabled: false,
    target,
    sourceType: 'static',
    contextPath: '',
    api: '',
    method: 'get',
    paramsText: '{}',
    dataPath: 'data',
    labelField: 'label',
    valueField: 'value',
    titleField: 'title',
    descriptionField: 'description',
    metaField: 'meta',
    keyField: 'key',
    childrenField: 'children',
    contentField: 'content',
    totalField: 'total',
    ...overrides,
  }
}

export function createPageWidgetComponent(componentKey = '', options = {}) {
  const meta = resolvePageWidgetMeta(componentKey) || {}
  const id = options.id || `cmp_${componentKey || 'widget'}_${Date.now()}`
  const gridColumns = Math.max(1, Math.min(24, Number(options.gridColumns || 2)))
  return {
    id,
    componentKey,
    label: options.label || meta.label || meta.title || componentKey,
    props: {
      ...createPageWidgetDefaultProps(componentKey),
      ...(options.props || {}),
    },
    fieldBinding: {
      mode: 'virtual',
    },
    layout: {
      span: Math.min(gridColumns, Math.max(1, Number(options.span || gridColumns))),
      align: 'left',
    },
    visibility: {
      hidden: false,
      readonly: false,
    },
    children: [],
  }
}

export function safeJsonParseObject(value = '', fallback = {}) {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value || '{}') : value
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : fallback
  }
  catch {
    return fallback
  }
}

export function safeHtml(value = '') {
  return String(value || '')
    .replace(/<script\b[^>]*>/gi, '&lt;script&gt;')
    .replace(/<\/script>/gi, '&lt;/script&gt;')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

// 模板占位符统一支持三种写法：{{ 字段 }}、${字段}、$form.字段（与 AiFormItem 的动态参数语法保持一致）
const TEMPLATE_REF_PATTERN = /\{\{\s*([\w.$-]+)\s*\}\}|\$\{\s*([\w.$-]+)\s*\}|\$form\.([\w.$-]+)/g

function readTemplatePathValue(data = {}, path = '') {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], data)
}

export function interpolateTemplate(template = '', data = {}) {
  return String(template || '').replace(TEMPLATE_REF_PATTERN, (matched, mustacheKey, dollarKey, formKey) => {
    const key = mustacheKey || dollarKey || formKey
    if (!key)
      return matched
    const value = readTemplatePathValue(data, key)
    return value === undefined || value === null ? '' : String(value)
  })
}

// 提取模板里引用的字段路径集合，用于“引用字段的值变化时才重新请求”的精准联动判断
export function extractTemplateRefs(...texts) {
  const refs = new Set()
  texts.forEach((text) => {
    const source = String(text || '')
    for (const matched of source.matchAll(TEMPLATE_REF_PATTERN)) {
      const key = matched[1] || matched[2] || matched[3]
      if (key)
        refs.add(key)
    }
  })
  return Array.from(refs)
}

// 把模板引用的字段当前值拼接成签名：任一被引用字段的值变化，签名才会变化
export function buildTemplateRefSignature(data = {}, ...texts) {
  const refs = extractTemplateRefs(...texts)
  if (!refs.length)
    return ''
  return refs.map((path) => {
    const value = readTemplatePathValue(data, path)
    if (value === undefined || value === null)
      return `${path}=`
    if (typeof value === 'object')
      return `${path}=${JSON.stringify(value)}`
    return `${path}=${String(value)}`
  }).join('&')
}
