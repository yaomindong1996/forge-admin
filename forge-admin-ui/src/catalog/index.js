/**
 * 页面模板 Catalog（前端注册表）
 *
 * 只负责"如何渲染"——将 templateKey 映射到对应的 Vue 渲染组件。
 * 模板的 prompt 约束、默认配置等元数据存储在后端 ai_page_template 表中。
 *
 * 新增模板步骤：
 *  1. 在 src/components/page-templates/ 下新建 XxxTemplate.vue
 *  2. 在此处注册：catalog['your-key'] = { component: () => import('...') }
 *  3. 在 ai_page_template 表中插入对应元数据记录（templateKey 必须一致）
 */

const catalog = {
  /** 标准 CRUD：平坦型数据，抽屉/弹窗表单 */
  'simple-crud': {
    label: '标准 CRUD',
    icon: 'mdi:table',
    component: () => import('@/components/page-templates/SimpleCrudTemplate.vue'),
  },

  /** 树形 CRUD：左树右表，父子层级 */
  'tree-crud': {
    label: '树形 CRUD',
    icon: 'mdi:file-tree',
    component: () => import('@/components/page-templates/TreeCrudTemplate.vue'),
  },
}

/**
 * 根据 templateKey 获取渲染组件（异步）
 * @param {string} templateKey
 * @returns {Promise<Component> | null}
 */
export function getTemplateComponent(templateKey) {
  const entry = catalog[templateKey]
  if (!entry) {
    console.warn(`[catalog] 未找到模板: ${templateKey}，降级使用 simple-crud`)
    return catalog['simple-crud']?.component?.() || null
  }
  return entry.component()
}

/**
 * 获取所有已注册的模板列表（供前端展示，在后端数据加载前的 fallback）
 */
export function getCatalogList() {
  return Object.entries(catalog).map(([key, value]) => ({
    templateKey: key,
    templateName: value.label,
    icon: value.icon,
  }))
}

export default catalog
