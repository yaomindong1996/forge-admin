import { request } from '@/utils'

// 工作台聚合在 forge-flow；走 /api/flow/* 才能命中生产 Nginx 已有的 flow 反代
export function getWorkspaceSummary() {
  return request.get('/api/flow/workspace/summary')
}

export function getWorkspaceTodoCount() {
  return request.get('/api/flow/workspace/todo-count')
}
