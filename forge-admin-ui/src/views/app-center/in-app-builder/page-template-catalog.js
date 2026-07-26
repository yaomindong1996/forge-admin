export const inAppPageTemplateCatalog = [
  {
    key: 'blank',
    label: '空白页面',
    description: '从一个组件开始搭建',
    pageTypes: ['content', 'home', 'intro', 'entry'],
    pageType: 'content',
    blockTypes: [],
  },
  {
    key: 'intro',
    label: '介绍页',
    description: '适合说明业务目标和入口',
    pageTypes: ['intro', 'content', 'home'],
    pageType: 'intro',
    blockTypes: ['page-title', 'info-panel'],
  },
  {
    key: 'overview',
    label: '业务概览',
    description: '标题、指标和提示信息',
    pageTypes: ['home', 'content', 'intro'],
    pageType: 'content',
    blockTypes: ['page-title', 'stats-strip', 'info-panel'],
  },
  {
    key: 'data',
    label: '数据工作台',
    description: '适合后续添加数据列表',
    pageTypes: ['content'],
    pageType: 'content',
    blockTypes: ['page-title', 'AiCrudPage'],
  },
  {
    key: 'crud',
    label: '数据列表（CRUD）',
    description: '筛选、列表和新增编辑，复用业务对象的真实接口与字段配置',
    pageTypes: ['object'],
    pageType: 'object',
    requiresObject: true,
    objectPageMode: 'crud',
    blockTypes: ['AiCrudPage'],
  },
  {
    key: 'tree-table',
    label: '左树右表',
    description: '左侧树筛选，右侧展示业务列表；字段和接口仍来自绑定对象',
    pageTypes: ['object'],
    pageType: 'object',
    requiresObject: true,
    objectPageMode: 'tree-table',
    blockTypes: ['tree-panel', 'AiCrudPage'],
  },
  {
    key: 'master-detail',
    label: '主子表',
    description: '用于主记录与明细录入，后续在业务对象中配置关联字段',
    pageTypes: ['object'],
    pageType: 'object',
    requiresObject: true,
    objectPageMode: 'master-detail',
    blockTypes: ['AiCrudPage', 'sub-table-tabs'],
  },
]

export function resolveInAppPageTemplate(key) {
  return inAppPageTemplateCatalog.find(item => item.key === key) || inAppPageTemplateCatalog[0]
}
