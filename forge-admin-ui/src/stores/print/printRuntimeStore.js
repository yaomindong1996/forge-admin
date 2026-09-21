import { defineStore } from 'pinia'
import * as api from '@/api/print'
import { assertPrintDocument } from '@/components/print/protocol/validate'

const errors = new Set(['PRINT_CANCELLED', 'RESOURCE_FAILED', 'RESOURCE_TIMEOUT', 'FIELD_NOT_ALLOWED', 'INVALID_TEMPLATE', 'FONT_UNAVAILABLE', 'LIMIT_EXCEEDED', 'ELEMENT_TOO_TALL', 'PRINT_UNAVAILABLE', 'PDF_UNAVAILABLE'])
export const usePrintRuntimeStore = defineStore('printRuntime', {
  state: () => ({ record: null, options: [], selectedId: null, prepared: null, loading: false, error: '', generation: 0, eventPending: false, eventSent: false, eventError: '', pendingEvent: null }),
  actions: {
    close() {
      this.generation++
      this.record = null
      this.options = []
      this.selectedId = null
      this.prepared = null
      this.loading = false
      this.error = ''
      this.eventPending = false
      this.eventSent = false
      this.eventError = ''
      this.pendingEvent = null
    },
    async open(record) {
      this.close()
      this.record = JSON.parse(JSON.stringify(record))
      const generation = this.generation
      this.loading = true
      try {
        const { data } = await api.availablePrintTemplates(this.record)
        if (generation !== this.generation)
          return
        this.options = data
        const preferred = data.find(item => item.isDefault) || (data.length === 1 ? data[0] : null)
        if (preferred)
          await this.select(preferred.id)
      }
      catch (error) {
        if (generation === this.generation)
          this.error = error.message || '无法读取可用打印模板'
      }
      finally {
        if (generation === this.generation)
          this.loading = false
      }
    },
    async select(templateId) {
      if (!this.record)
        return
      const generation = ++this.generation
      this.selectedId = templateId
      this.prepared = null
      this.loading = true
      this.error = ''
      this.eventPending = false
      this.eventSent = false
      this.pendingEvent = null
      this.eventError = ''
      try {
        const { data } = await api.preparePrint(this.record, templateId)
        if (generation !== this.generation)
          return
        const template = JSON.parse(data.schemaJson)
        assertPrintDocument(template)
        this.prepared = { ...data, template, context: { ...data.context, system: { generatedAt: data.generatedAt } } }
      }
      catch (error) {
        if (generation === this.generation)
          this.error = error.message || '无法准备打印'
      }
      finally {
        if (generation === this.generation)
          this.loading = false
      }
    },
    async event(event) {
      if (!this.prepared || this.eventPending || this.eventSent)
        return
      const generation = this.generation
      const executionId = this.prepared.executionId
      this.eventPending = true
      this.eventError = ''
      this.pendingEvent = event
      try {
        await api.recordPrintEvent(executionId, event)
        if (generation === this.generation) {
          this.eventSent = true
          this.pendingEvent = null
        }
      }
      catch {
        if (generation === this.generation)
          this.eventError = '打印会话事件未记录，可重试；这不代表实际出纸结果。'
      }
      finally {
        if (generation === this.generation)
          this.eventPending = false
      }
    },
    failed(error) {
      return this.event({ result: 'FAILED', errorCode: errors.has(error?.code) ? error.code : 'PRINT_FAILED' })
    },
  },
})
