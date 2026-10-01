import { defineStore } from 'pinia'
import * as api from '@/api/print'

export const usePrintCenterStore = defineStore('printCenter', {
  state: () => ({
    sources: [],
    total: 0,
    selectedId: null,
    selected: null,
    loading: false,
    detailLoading: false,
    saving: false,
    error: '',
    keyword: '',
    type: null,
    generation: 0,
  }),
  getters: {
    sourceIdentity: state => state.selected
      ? {
          businessSourceId: String(state.selected.id),
          sourceCode: state.selected.sourceCode,
          sourceType: state.selected.sourceType,
          objectCode: state.selected.objectCode,
        }
      : null,
  },
  actions: {
    async load(preferredId) {
      const generation = ++this.generation
      this.loading = true
      this.error = ''
      try {
        const { data } = await api.printSources({
          sourceName: this.keyword || undefined,
          sourceType: this.type || undefined,
          pageNum: 1,
          pageSize: 100,
        })
        if (generation !== this.generation)
          return
        this.sources = data?.records || []
        this.total = Number(data?.total) || this.sources.length
        const target = preferredId || this.selectedId
        const next = this.sources.find(item => String(item.id) === String(target))
          || this.sources[0]
          || null
        if (preferredId && !this.sources.some(item => String(item.id) === String(preferredId))) {
          await this.select(target)
          if (this.selected) {
            this.sources = [this.selected, ...this.sources]
            return
          }
        }
        await this.select(next?.id)
      }
      catch (error) {
        if (generation === this.generation) {
          this.sources = []
          this.total = 0
          this.selected = null
          this.selectedId = null
          this.error = error.message || '无法读取打印业务来源'
        }
      }
      finally {
        if (generation === this.generation)
          this.loading = false
      }
    },
    async select(id) {
      if (!id) {
        this.selectedId = null
        this.selected = null
        return
      }
      const generation = this.generation
      this.selectedId = String(id)
      this.detailLoading = true
      this.error = ''
      try {
        const { data } = await api.printSourceDetail(id)
        if (generation === this.generation && String(this.selectedId) === String(id))
          this.selected = data
      }
      catch (error) {
        if (generation === this.generation && String(this.selectedId) === String(id)) {
          this.selected = null
          this.error = error.message || '无法读取打印来源详情'
        }
      }
      finally {
        if (generation === this.generation)
          this.detailLoading = false
      }
    },
    async save(form) {
      this.saving = true
      this.error = ''
      try {
        const current = form.id
        const payload = current
          ? {
              expectedRevision: form.sourceRevision,
              sourceName: form.sourceName,
              providerCode: form.providerCode || null,
              datasetId: form.datasetId || null,
              objectCode: form.objectCode,
              parameterSchemaJson: form.parameterSchemaJson || null,
              mappingJson: form.mappingJson || null,
            }
          : {
              sourceCode: form.sourceCode,
              sourceName: form.sourceName,
              sourceType: form.sourceType,
              providerCode: form.providerCode || null,
              datasetId: form.datasetId || null,
              objectCode: form.objectCode,
              parameterSchemaJson: form.parameterSchemaJson || null,
              mappingJson: form.mappingJson || null,
            }
        const { data } = current
          ? await api.updatePrintSource(current, payload)
          : await api.createPrintSource(payload)
        await this.load(data.id)
        return data
      }
      finally {
        this.saving = false
      }
    },
    async toggle() {
      if (!this.selected)
        return
      this.saving = true
      try {
        const { data } = await api.changePrintSourceStatus(this.selected.id, {
          expectedRevision: this.selected.sourceRevision,
          status: Number(this.selected.status) === 1 ? 0 : 1,
        })
        await this.load(data.id)
      }
      finally {
        this.saving = false
      }
    },
    async remove() {
      if (!this.selected)
        return
      this.saving = true
      try {
        await api.deletePrintSource(this.selected.id, this.selected.sourceRevision)
        this.selectedId = null
        await this.load()
      }
      finally {
        this.saving = false
      }
    },
    clear() {
      const generation = this.generation + 1
      this.$reset()
      this.generation = generation
    },
  },
})
