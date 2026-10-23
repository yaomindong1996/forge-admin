/** 模板卡片色相：与应用市场卡片共用，避免创建向导与市场观感不一致。 */
const DEFAULT_TEMPLATE_TONE = { ink: '#356cde', wash: '#e0e7ff' }

export const APPLICATION_TEMPLATE_CATALOG = [
  {
    key: 'customer-management',
    name: '客户档案管理',
    category: '客户经营',
    source: 'official',
    icon: 'ionicons5:PeopleOutline',
    tone: { ink: '#2563eb', wash: '#dbeafe' },
    description: '客户基础资料、联系方式和状态维护，适合作为 CRM 起点。',
    useCount: 128,
    templateCode: 'SINGLE_CRUD',
    initialization: {
      primaryObjectName: '客户档案',
      primaryObjectCode: 'customer',
    },
  },
  {
    key: 'inventory-ledger',
    name: '商品库存台账',
    category: '进销存',
    source: 'official',
    icon: 'ionicons5:CubeOutline',
    tone: { ink: '#7c3aed', wash: '#ede9fe' },
    description: '按商品分类管理库存台账，自动生成左树右表页面。',
    useCount: 96,
    templateCode: 'TREE_TABLE',
    initialization: {
      primaryObjectName: '商品库存',
      primaryObjectCode: 'inventory_item',
      treeObjectName: '商品分类',
      treeObjectCode: 'inventory_category',
      treeKeyField: 'id',
      treeLabelField: 'categoryName',
      treeParentField: 'parentCategory',
      primaryTreeField: 'categoryId',
    },
  },
  {
    key: 'order-management',
    name: '订单与明细',
    category: '进销存',
    source: 'official',
    icon: 'ionicons5:ReceiptOutline',
    tone: { ink: '#059669', wash: '#d1fae5' },
    description: '主订单与多行明细的标准结构，适合采购、销售和出入库单。',
    useCount: 83,
    templateCode: 'MASTER_DETAIL',
    initialization: {
      primaryObjectName: '业务订单',
      primaryObjectCode: 'business_order',
      primaryKeyField: 'id',
      details: [
        {
          objectName: '订单明细',
          objectCode: 'business_order_detail',
          foreignKeyField: 'orderId',
          relationName: '订单明细',
        },
      ],
    },
  },
  {
    key: 'project-task',
    name: '项目任务清单',
    category: '项目管理',
    source: 'official',
    icon: 'ionicons5:CheckboxOutline',
    tone: { ink: '#ea580c', wash: '#ffedd5' },
    description: '用于任务登记、负责人协作和进度跟踪的轻量应用。',
    useCount: 61,
    templateCode: 'SINGLE_CRUD',
    initialization: {
      primaryObjectName: '项目任务',
      primaryObjectCode: 'project_task',
    },
  },
]

export function resolveApplicationTemplateTone(template) {
  const tone = template?.tone
  if (tone?.ink && tone?.wash)
    return tone
  return DEFAULT_TEMPLATE_TONE
}

export function filterApplicationTemplates(keyword, source = null) {
  const normalizedKeyword = String(keyword || '').trim().toLowerCase()
  return APPLICATION_TEMPLATE_CATALOG.filter((template) => {
    if (source && template.source !== source)
      return false
    if (!normalizedKeyword)
      return true
    return [template.name, template.category, template.description]
      .some(value => String(value || '').toLowerCase().includes(normalizedKeyword))
  })
}

export function findApplicationTemplate(templateKey) {
  return APPLICATION_TEMPLATE_CATALOG.find(template => template.key === templateKey) || null
}

export function templateToneStyle(template) {
  const tone = resolveApplicationTemplateTone(template)
  return {
    '--template-ink': tone.ink,
    '--template-wash': tone.wash,
  }
}

export function buildTemplateInitializePayload(template) {
  if (!template)
    return null
  return {
    templateCode: template.templateCode,
    ...(template.initialization || {}),
  }
}
