import { request } from '@/utils'

export function getRetrievalDefaults() {
  return request.get('/ai/resources/retrieval/defaults')
}

export function saveRetrievalDefaults(data) {
  return request.post('/ai/resources/retrieval/defaults/save', data)
}
