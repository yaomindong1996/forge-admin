import { useAuthStore } from '@/store/modules/auth'
import { request } from '@/utils'
import { generateUUID } from '@/utils/common'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''

export function lowcodeAppPage(params) {
  return request.get('/ai/lowcode/app/page', { params })
}

export function lowcodeDomainPage(params) {
  return request.get('/ai/lowcode/domain/page', { params })
}

export function lowcodeDomainTree(params) {
  return request.get('/ai/lowcode/domain/tree', { params })
}

export function lowcodeDomainDetail(id) {
  return request.get(`/ai/lowcode/domain/${id}`)
}

export function lowcodeCreateDomain(data) {
  return request.post('/ai/lowcode/domain', data)
}

export function lowcodeUpdateDomain(data) {
  return request.put('/ai/lowcode/domain', data)
}

export function lowcodeUpdateDomainStatus(id, status) {
  return request.put(`/ai/lowcode/domain/${id}/status`, { status })
}

export function lowcodeDomainWorkspace(id) {
  return request.get(`/ai/lowcode/domain/${id}/workspace`)
}

export function lowcodeDomainDefaults(id) {
  return request.get(`/ai/lowcode/domain/${id}/defaults`)
}

export function lowcodeDeleteDomain(id) {
  return request.delete(`/ai/lowcode/domain/${id}`)
}

export function lowcodeAppDetail(id) {
  return request.get(`/ai/lowcode/app/${id}`)
}

export function lowcodeDeleteApp(id) {
  return request.delete(`/ai/lowcode/app/${id}`)
}

export function lowcodeSaveDraft(data) {
  return request.post('/ai/lowcode/app/draft', data)
}

export function lowcodeMoveDomain(id, data) {
  return request.put(`/ai/lowcode/app/${id}/move-domain`, data)
}

export function lowcodePreview(id, data) {
  return request.post(`/ai/lowcode/app/${id}/preview`, data)
}

export function lowcodePublish(id, data) {
  return request.post(`/ai/lowcode/app/${id}/publish`, data)
}

export function lowcodeVersions(id) {
  return request.get(`/ai/lowcode/app/${id}/versions`)
}

export function lowcodeRollback(id, versionId) {
  return request.post(`/ai/lowcode/app/${id}/rollback/${versionId}`)
}

export function lowcodeAiGenerateApp(data) {
  return request.post('/ai/lowcode/app/ai/generate', data)
}

export function lowcodeAiStreamGenerateApp(data, onEvent, onComplete, onError) {
  const controller = new AbortController()
  const authStore = useAuthStore()

  fetch(`${BASE_URL}/ai/lowcode/app/ai/stream-generate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
      'X-Timestamp': Date.now().toString(),
      'X-Nonce': generateUUID(),
    },
    body: JSON.stringify(data),
    signal: controller.signal,
  })
    .then((response) => {
      if (!response.ok)
        throw new Error(response.statusText || 'AI 生成请求失败')
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function read() {
        reader.read().then(({ done, value }) => {
          if (done)
            return
          buffer += decoder.decode(value, { stream: true })
          const events = buffer.split('\n\n')
          buffer = events.pop() || ''

          for (const eventStr of events) {
            if (!eventStr.trim())
              continue
            const lines = eventStr.split('\n')
            let eventType = 'message'
            let eventData = ''
            for (const line of lines) {
              if (line.startsWith('event:'))
                eventType = line.substring(6).trim()
              else if (line.startsWith('data:'))
                eventData += line.substring(5).trim()
            }
            if (!eventData)
              continue
            try {
              const parsed = JSON.parse(eventData)
              if (eventType === 'complete')
                onComplete?.(parsed)
              else if (eventType === 'error')
                onError?.(parsed.message || 'AI 生成失败')
              else
                onEvent?.({ event: eventType, data: parsed })
            }
            catch (error) {
              console.warn('[lowcodeAiStreamGenerateApp] parse event failed:', error)
            }
          }
          read()
        }).catch((error) => {
          if (error.name !== 'AbortError')
            onError?.(error.message || 'AI 生成失败')
        })
      }
      read()
    })
    .catch((error) => {
      if (error.name !== 'AbortError')
        onError?.(error.message || 'AI 生成失败')
    })

  return controller
}

export function lowcodeAppCodePreview(id, params) {
  return request.get(`/ai/lowcode/app/${id}/code/preview`, { params })
}

export function lowcodeAppCodeOptions(id) {
  return request.get(`/ai/lowcode/app/${id}/code/options`)
}

export function lowcodeSaveAppCodeOptions(id, data) {
  return request.put(`/ai/lowcode/app/${id}/code/options`, data)
}

export async function lowcodeDownloadAppCode(id, params = {}) {
  const authStore = useAuthStore()
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '')
      search.append(key, value)
  })
  const query = search.toString()
  const resp = await fetch(`${BASE_URL}/ai/lowcode/app/${id}/code/download${query ? `?${query}` : ''}`, {
    method: 'GET',
    headers: {
      'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
      'X-Timestamp': Date.now().toString(),
      'X-Nonce': generateUUID(),
    },
  })
  if (!resp.ok)
    throw new Error(await resp.text() || resp.statusText)
  return resp.blob()
}

export function lowcodeValidateModel(data) {
  return request.post('/ai/lowcode/model/validate', data)
}

export function lowcodeModelPage(params) {
  return request.get('/ai/lowcode/model/page', { params })
}

export function lowcodeModelList(params) {
  return request.get('/ai/lowcode/model/list', { params })
}

export function lowcodeModelDetail(id) {
  return request.get(`/ai/lowcode/model/${id}`)
}

export function lowcodeCreateModel(data) {
  return request.post('/ai/lowcode/model', data)
}

export function lowcodeUpdateModel(data) {
  return request.put('/ai/lowcode/model', data)
}

export function lowcodeUpdateModelStatus(id, status) {
  return request.put(`/ai/lowcode/model/${id}/status`, { status })
}

export function lowcodeDeleteModel(id) {
  return request.delete(`/ai/lowcode/model/${id}`)
}

export function lowcodeDdlPreview(data) {
  return request.post('/ai/lowcode/model/ddl/preview', { modelSchema: data })
}

export function lowcodePreviewDbTableModel(data) {
  return request.post('/ai/lowcode/model/preview-db-table', data)
}

export function lowcodeImportDbTableModel(data) {
  return request.post('/ai/lowcode/model/import-db-table', data)
}

export function genDatasourceEnabled() {
  return request.get('/generator/datasource/enabled')
}

export function genDatasourceTables(datasourceId) {
  return request.get(`/generator/datasource/${datasourceId}/tables`)
}
