import { reactive } from 'vue'

// 只供本地可视化诊断；不调用网络或模拟真实审批成功。
export const fixture = reactive({ scenario: 'normal', revision: 0, requests: [], notice: '' })
export const useAuthStore = () => ({ accessToken: '' })
export const useUserStore = () => ({ userId: 'fixture', tenantId: 'fixture' })
export const isNullOrUndef = value => value === null || value === undefined
export const router = { push: route => fixture.notice = `跳转入口：${JSON.stringify(route)}` }
const states = [['尚未发布', 'UNPUBLISHED', 'default'], ['与发布版本一致', 'SYNCED', 'default'],
  ['有未发布修改', 'CHANGED', 'warning'], ['发布信息待核对', 'UNKNOWN', 'warning']]
const dictionary = {
  flow_model_publication_state: states,
  flow_model_status: [['设计中', '0', 'warning'], ['已发布', '1', 'success'], ['已挂起', '2', 'default']],
  flow_designer_type: [['审批流程', 'approval', 'default'], ['业务流程', 'business', 'default']],
}
const models = [
  { id: 'fixture-contract', modelKey: 'contract_review', modelName: '合同用印审批', status: 1,
    version: 3, publishedVersion: 3, publicationState: 'CHANGED', category: 'office', categoryName: '行政办公',
    description: '核对合同信息及用印权限，办理完成后通知申请人。' },
  { id: 'fixture-expense', modelKey: 'expense_review', modelName: '费用报销', status: 0,
    version: 1, publicationState: 'UNPUBLISHED', category: 'finance', categoryName: '财务管理' },
  { id: 'fixture-purchase', modelKey: 'purchase_review', modelName: '采购申请审批', status: 1,
    version: 2, publishedVersion: 2, publicationState: 'SYNCED', category: 'office', categoryName: '行政办公' },
  { id: 'fixture-access', modelKey: 'access_review', modelName: '访客预约', status: 2,
    version: 2, publishedVersion: 2, publicationState: 'SYNCED', category: 'office', categoryName: '行政办公' },
  { id: 'fixture-legacy', modelKey: 'legacy_review', modelName: '历史归档流程', status: 1,
    version: 7, publicationState: 'UNKNOWN', category: 'office', categoryName: '行政办公' },
  { id: 'fixture-business', modelKey: 'document_archive', modelName: '业务资料归档', status: 0,
    version: 1, publicationState: 'UNPUBLISHED', designerType: 'business', category: 'office', categoryName: '行政办公' },
].map(row => ({ designerType: 'approval', updateTime: '2026-10-20T09:30:00', ...row }))

export const request = Object.assign(async () => { throw new Error('隔离验收禁止真实业务请求') }, {
  async get(url, config = {}) {
    fixture.requests.push(url)
    if (url.includes('/system/dict/data/type/')) {
      const key = url.split('/').at(-1)
      return { code: 200, data: (dictionary[key] || []).map(([dictLabel, dictValue, listClass], index) =>
        ({ dictLabel, dictValue, listClass, dictSort: index, dictStatus: 1 })) }
    }
    if (url.includes('/category/')) return { code: 200, data: [
      { id: 'office', categoryName: '行政办公', categoryCode: 'office' },
      { id: 'finance', categoryName: '财务管理', categoryCode: 'finance' },
    ] }
    if (url === '/api/flow/model/page') {
      if (fixture.scenario === 'error') throw new Error('合成的列表加载失败')
      const query = config.params || {}
      const records = fixture.scenario === 'empty' ? [] : models.filter(row =>
        (!query.modelName || `${row.modelName} ${row.modelKey}`.includes(query.modelName))
        && (!query.category || row.category === query.category)
        && (query.status === null || query.status === undefined || row.status === query.status))
      return { code: 200, data: { records: structuredClone(records), total: records.length } }
    }
    throw new Error(`本地夹具未配置：${url}`)
  },
  async post(url) {
    fixture.notice = `本地验收不提交业务写操作：${url}`
    throw new Error('隔离验收禁止提交业务写操作')
  },
})
export async function businessFlowModelBindingsBatch(keys) {
  fixture.requests.push('/ai/business/flow/model/business-bindings')
  if (fixture.scenario === 'bindings-error') throw new Error('合成的绑定失败')
  return { code: 200, data: keys.filter(key => key !== 'expense_review').map(key =>
    ({ flowModelKey: key, applicationName: '验收业务应用', objectName: '示例对象', objectCode: key })) }
}
export const businessFlowFormAssets = async () => ({ code: 200, data: [] })
export const businessFlowModelBindings = async key => businessFlowModelBindingsBatch([key])
export const postEncrypt = request.post
export const getEncrypt = request.get
export default request
