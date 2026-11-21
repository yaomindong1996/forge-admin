import { request } from '@/utils'

export function hubRoute(data) {
  return request.post('/ai/hub/route', data)
}

export function hubAgents() {
  return request.get('/ai/hub/agents')
}

export function hubTodos(params) {
  return request.get('/ai/hub/todos', { params })
}
