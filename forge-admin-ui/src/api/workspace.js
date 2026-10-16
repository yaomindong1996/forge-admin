import { request } from '@/utils'

// 工作台聚合在 forge-flow；走 /api/flow/* 才能命中生产 Nginx 已有的 flow 反代。
// needTip:false：失败由 WorkspaceSummaryMetrics 内联告警，避免弹全局「请求资源不存在」打断应用页。
export function getWorkspaceSummary() {
  return request.get('/api/flow/workspace/summary', { needTip: false })
}

export function getWorkspaceTodoCount() {
  return request.get('/api/flow/workspace/todo-count', { needTip: false })
}
