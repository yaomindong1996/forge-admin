import { request } from '@/utils'
import { generateUUID } from '@/utils/common'
import { useAuthStore } from '@/store/modules/auth'

const BASE_URL = import.meta.env.VITE_REQUEST_PREFIX || ''

export function streamGenerate(data, onChunk, onComplete, onError) {
  const controller = new AbortController()
  const authStore = useAuthStore()

  fetch(BASE_URL + '/ai/crud-generator/stream-generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': authStore.accessToken ? `Bearer ${authStore.accessToken}` : '',
      'X-Timestamp': Date.now().toString(),
      'X-Nonce': generateUUID(),
    },
    body: JSON.stringify(data),
    signal: controller.signal,
  })
    .then(response => {
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            return
          }
          buffer += decoder.decode(value, { stream: true })
          
          const events = buffer.split('\n\n')
          buffer = events.pop() || ''

          for (const eventStr of events) {
            if (!eventStr.trim()) continue
            
            const lines = eventStr.split('\n')
            let eventType = 'message'
            let eventData = ''
            
            for (const line of lines) {
              if (line.startsWith('event:')) {
                eventType = line.substring(6).trim()
              } else if (line.startsWith('data:')) {
                eventData = line.substring(5).trim()
              }
            }
            
            if (eventData) {
              try {
                const parsed = JSON.parse(eventData)
                if (eventType === 'progress' || eventType === 'chunk' || eventType === 'meta') {
                  onChunk({ event: eventType, data: parsed })
                } else if (eventType === 'complete') {
                  onComplete(parsed)
                } else if (eventType === 'error') {
                  onError(parsed.message)
                }
              } catch (e) {
                console.warn('JSON parse error:', e)
              }
            }
          }
          read()
        }).catch(err => {
          if (err.name !== 'AbortError') {
            onError(err.message)
          }
        })
      }
      read()
    })
    .catch(err => {
      if (err.name !== 'AbortError') {
        onError(err.message)
      }
    })

  return controller
}

export function crudGeneratorSessionList(params) {
  return request.get('/ai/admin/session/page', { params })
}

export function crudGeneratorSessionMessages(sessionId) {
  return request.get(`/ai/admin/session/${sessionId}/messages`)
}

export function crudGeneratorSessionDelete(sessionId) {
  return request.delete(`/ai/admin/session/${sessionId}`)
}