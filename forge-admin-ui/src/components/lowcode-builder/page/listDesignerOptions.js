/** List page grid designer — static option catalogs (keep pure; no Vue imports). */

export const canvasWidthOptions = [
  { label: '390 移动', value: 390 },
  { label: '768 窄屏', value: 768 },
  { label: '960 抽屉', value: 960 },
  { label: '1200', value: 1200 },
  { label: '1366 默认', value: 1366 },
  { label: '1440', value: 1440 },
  { label: '1600', value: 1600 },
  { label: '1920', value: 1920 },
]
export const canvasZoomOptions = [
  { label: '50%', value: 0.5 },
  { label: '67%', value: 0.67 },
  { label: '75%', value: 0.75 },
  { label: '90%', value: 0.9 },
  { label: '100%', value: 1 },
  { label: '125%', value: 1.25 },
]
export const canvasPreviewModeOptions = [
  { label: '桌面', value: 'desktop' },
  { label: '窄屏', value: 'narrow' },
  { label: '弹窗', value: 'modal' },
  { label: '抽屉', value: 'drawer' },
  { label: '移动', value: 'mobile' },
]
export const crudPreviewModeOptions = [
  { label: '模拟数据', value: 'mock' },
  { label: '真实列表', value: 'realList' },
  { label: '新增表单', value: 'create' },
  { label: '编辑表单', value: 'edit' },
  { label: '详情状态', value: 'detail' },
]
export const actionPositionOptions = [
  { label: '工具栏', value: 'toolbar' },
  { label: '行操作列', value: 'row' },
  { label: '详情页', value: 'detail' },
]
export const actionTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
]
export const actionBehaviorOptions = [
  { label: '应用内页面', value: 'page' },
  { label: '站内跳转', value: 'route' },
  { label: '外部链接', value: 'external' },
  { label: '调用 API', value: 'CALL_API' },
  { label: '发起主流程', value: 'START_FLOW' },
  { label: '执行触发器', value: 'TRIGGER' },
  { label: '刷新列表', value: 'refresh' },
]
export const actionOpenTargetOptions = [
  { label: '当前页', value: '_self' },
  { label: '新窗口', value: '_blank' },
]
export const successBehaviorOptions = [
  { label: '无', value: 'none' },
  { label: '刷新列表', value: 'refreshList' },
  { label: '返回上一页', value: 'goBack' },
]
export const apiMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'POST 加密', value: 'POST_ENCRYPT' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' },
]
export const apiParamTargetOptions = [
  { label: 'Path', value: 'path' },
  { label: 'Query', value: 'query' },
  { label: 'Body', value: 'body' },
  { label: 'Header', value: 'header' },
]
export const paramSourceOptions = [
  { label: '固定值', value: 'static' },
  { label: '当前行字段', value: 'rowField' },
  { label: '路由参数', value: 'routeQuery' },
  { label: '系统变量', value: 'system' },
]
export const routeParamOptions = [
  { label: '记录 ID（id）', value: 'id' },
  { label: '记录 ID（recordId）', value: 'recordId' },
  { label: '页面 Key（pageKey）', value: 'pageKey' },
  { label: '页面 ID（pageId）', value: 'pageId' },
  { label: '表单 Key（formKey）', value: 'formKey' },
]
export const systemVariableOptions = [
  { label: '当前时间戳', value: 'now' },
  { label: '当前日期', value: 'today' },
  { label: '当前用户 ID', value: 'userId' },
  { label: '当前租户 ID', value: 'tenantId' },
  { label: '勾选行 ID', value: 'selectedIds' },
]
export const labelPlacementOptions = [
  { label: '左侧', value: 'left' },
  { label: '顶部', value: 'top' },
]
export const labelAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '右对齐', value: 'right' },
]
export const gridVerticalAlignOptions = [
  { label: '垂直填满', value: 'stretch' },
  { label: '靠上', value: 'start' },
  { label: '垂直居中', value: 'center' },
  { label: '靠下', value: 'end' },
]
export const directionOptions = [
  { label: '横向', value: 'row' },
  { label: '纵向', value: 'vertical' },
  { label: '纵向 CSS', value: 'column' },
]
export const splitDirectionOptions = [
  { label: '横向', value: 'horizontal' },
  { label: '纵向', value: 'vertical' },
]
export const menuModeOptions = [
  { label: '纵向', value: 'vertical' },
  { label: '横向', value: 'horizontal' },
]
export const alertTypeOptions = [
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
]
export const richEditorModeOptions = [
  { label: '可视编辑', value: 'visual' },
  { label: '源码模式', value: 'source' },
]
export const wangEditorModeOptions = [
  { label: '默认', value: 'default' },
  { label: '简洁', value: 'simple' },
]
export const watermarkFontStyleOptions = [
  { label: 'normal', value: 'normal' },
  { label: 'italic', value: 'italic' },
  { label: 'oblique 12deg', value: 'oblique 12deg' },
]
export const watermarkTextAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
export const barcodeFormatOptions = [
  'CODE128',
  'CODE39',
  'EAN13',
  'EAN8',
  'UPC',
  'ITF14',
  'MSI',
  'pharmacode',
  'codabar',
].map(value => ({ label: value, value }))
export const qrcodeErrorCorrectionOptions = [
  { label: 'L', value: 'L' },
  { label: 'M', value: 'M' },
  { label: 'Q', value: 'Q' },
  { label: 'H', value: 'H' },
]
export const qrcodeDotsTypeOptions = [
  'square',
  'dots',
  'rounded',
  'classy',
  'classy-rounded',
  'extra-rounded',
].map(value => ({ label: value, value }))
export const qrcodeCornerTypeOptions = [
  'square',
  'dot',
  'extra-rounded',
].map(value => ({ label: value, value }))
export const transferDataSourceOptions = [
  { label: '静态选项', value: 'static' },
  { label: '远程接口', value: 'remote' },
]
export const widgetDataSourceOptions = [
  { label: '静态配置', value: 'static' },
  { label: '当前详情/表单数据', value: 'context' },
  { label: '远程接口', value: 'remote' },
]
export const detailInfoDataSourceOptions = [
  { label: '当前详情数据', value: 'current' },
  { label: '当前详情字段路径', value: 'context' },
  { label: '远程详情接口', value: 'remote' },
]
export const dataBindablePageWidgetKeys = [
  'rich-text',
  'watermark',
  'vue-component',
  'html-tag',
  'markdown',
  'barcode',
  'qrcode',
  'calendar',
  'code',
  'countdown',
  'descriptions',
  'announcement',
  'list',
  'log',
  'number-animation',
  'breadcrumb',
  'menu',
  'pagination',
  'split',
]
export const localDataBindableBlockTypes = [
  'stats-strip',
  'quick-entry',
  'bar-chart',
  'line-chart',
  'area-chart',
  'pie-chart',
  'detail-info',
  'workflow-task-list',
  'notice-board',
  'welcome-banner',
  'info-panel',
  'custom-html',
  'tag-list',
  'steps',
  'timeline',
  'empty-state',
  'text-title',
  'paragraph',
  'statistic',
  'link',
  'text-tip',
  'audio-player',
  'video-player',
  'avatar',
]
export const markdownPreviewModeOptions = [
  { label: '源码 + 预览', value: 'split' },
  { label: '仅源码', value: 'source' },
  { label: '仅预览', value: 'preview' },
]
export const htmlTagOptions = ['div', 'section', 'article', 'aside', 'header', 'footer', 'main', 'span', 'p', 'strong', 'em', 'small', 'label'].map(value => ({ label: value, value }))
export const htmlRenderModeOptions = [
  { label: 'HTML 安全渲染', value: 'html' },
  { label: '纯文本', value: 'text' },
]
export const vuePreviewModeOptions = [
  { label: '安全模板预览', value: 'safe-template' },
  { label: 'Props 模板预览', value: 'live' },
  { label: '代码视图', value: 'code' },
]
export const justifyContentOptions = [
  { label: '靠左', value: 'flex-start' },
  { label: '居中', value: 'center' },
  { label: '靠右', value: 'flex-end' },
  { label: '两端', value: 'space-between' },
  { label: '环绕', value: 'space-around' },
]
export const simpleConfigBlockTypes = [
  'rich-text',
  'signature-pad',
  'transfer',
  'step-form',
  'vue-component',
  'html-tag',
  'text-title',
  'paragraph',
  'statistic',
  'link',
  'text-tip',
  'watermark',
  'audio-player',
  'video-player',
  'avatar',
  'barcode',
  'iframe',
  'qrcode',
  'markdown',
  'calendar',
  'code',
  'countdown',
  'announcement',
  'list',
  'log',
  'number-animation',
  'breadcrumb',
  'menu',
  'pagination',
  'split',
  'box-layout',
  'space',
  'descriptions',
]
export const componentSizeOptions = [
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]
export const tableDensityOptions = [
  { label: '紧凑', value: 'small' },
  { label: '默认', value: 'medium' },
  { label: '宽松', value: 'large' },
]
export const renderModeOptions = [
  { label: '表格', value: 'table' },
  { label: '卡片', value: 'card' },
]
export const expandTriggerOptions = [
  { label: '图标', value: 'icon' },
  { label: '整行', value: 'row' },
  { label: '图标和整行', value: 'both' },
]
export const expandLayoutModeOptions = [
  { label: '单面板', value: 'single' },
  { label: 'Tabs', value: 'tabs' },
  { label: '上下堆叠', value: 'stack' },
]
export const expandPanelTypeOptions = [
  { label: '描述信息', value: 'descriptions' },
  { label: '子表表格', value: 'table' },
  { label: '只读表单', value: 'form' },
  { label: '数量余额', value: 'quantity-balance' },
  { label: '数量流水', value: 'quantity-ledger' },
  { label: '数量锁定', value: 'quantity-lock' },
  { label: '多面板 Tabs', value: 'tabs' },
  { label: '自定义插槽', value: 'custom' },
]
export const expandDataSourceTypeOptions = [
  { label: '当前行数据', value: 'row' },
  { label: '接口加载', value: 'api' },
  { label: '数量台账', value: 'quantity' },
  { label: '静态数据', value: 'static' },
]
export const requestMethodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
]
export const tagTypeOptions = [
  { label: '默认', value: 'default' },
  { label: '主要', value: 'primary' },
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
]
export const treeLoadModeOptions = [
  { label: '全量加载', value: 'full' },
  { label: '懒加载', value: 'lazy' },
]
export const sortOrderOptions = [
  { label: '降序', value: 'desc' },
  { label: '升序', value: 'asc' },
]
export const shadowOptions = [
  { label: '无阴影', value: 'none' },
  { label: '轻微', value: '0 2px 8px rgba(15, 23, 42, 0.08)' },
  { label: '中等', value: '0 10px 24px rgba(15, 23, 42, 0.12)' },
  { label: '明显', value: '0 18px 42px rgba(15, 23, 42, 0.16)' },
]
export const eventTriggerOptions = [
  { label: '点击组件时', value: 'click' },
  { label: '区块加载完成后', value: 'load' },
  { label: '字段值变化时', value: 'change' },
  { label: '选择树节点时', value: 'nodeSelect' },
  { label: '点击表格行时', value: 'rowClick' },
  { label: '表单提交成功后', value: 'submitSuccess' },
]
export const blockEventActionOptions = [
  { label: '不执行动作', value: 'none' },
  { label: '返回上一页', value: 'back' },
  { label: '跳转到目标页面', value: 'navigate' },
  { label: '刷新目标区块', value: 'refreshBlock' },
  { label: '过滤目标区块', value: 'filterBlock' },
  { label: '打开弹窗表单', value: 'openModal' },
  { label: '打开抽屉表单', value: 'openDrawer' },
  { label: '请求后端接口', value: 'request' },
  { label: '提示消息', value: 'message' },
  { label: '执行自定义脚本', value: 'customScript' },
]
export const backButtonActionOptions = [
  { label: '浏览器返回', value: 'back' },
  { label: '跳转页面', value: 'navigate' },
]
export const resizeAnchors = ['top-left', 'top', 'top-right', 'right', 'bottom-right', 'bottom', 'bottom-left', 'left']
export const CANVAS_AUTO_SCROLL_EDGE = 56
export const CANVAS_AUTO_SCROLL_MAX_STEP = 20
