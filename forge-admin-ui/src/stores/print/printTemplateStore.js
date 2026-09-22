import { defineStore } from 'pinia'
import * as api from '@/api/print'
import { newPrintTemplateCode } from '@/components/print/id'
import { printSourcePayload } from '@/components/print/management/printRouteContext'
import { DEFAULT_PRINT_SCENES, syncPrintTemplateScenes } from '@/components/print/management/printSceneBinding'
import { formatPrintApiError } from '@/components/print/protocol/formatPrintError'
import { createPrintDocument } from '@/components/print/protocol/types'
import { assertPrintDocument } from '@/components/print/protocol/validate'

export const usePrintTemplateStore = defineStore('printTemplates', {
  state: () => ({
    row: null,
    document: null,
    catalog: [],
    name: '',
    savedName: '',
    loading: false,
    saving: false,
    error: '',
    notice: '',
    generation: 0,
    listGeneration: 0,
    items: [],
    total: 0,
    pageNum: 1,
    listing: false,
    versions: [],
    bindings: [],
    scene: 'DETAIL',
    panel: null,
  }),
  getters: { nameDirty: state => state.name !== state.savedName },
  actions: {
    clear() {
      this.generation++
      this.row = null
      this.document = null
      this.catalog = []
      this.versions = []
      this.bindings = []
      this.name = ''
      this.savedName = ''
      this.error = ''
      this.notice = ''
      this.loading = false
      this.saving = false
      this.panel = null
    },
    async list(applicationId, pageNum = 1, pageId) {
      const generation = ++this.listGeneration
      this.listing = true
      this.error = ''
      try {
        const query = { applicationId, pageNum, pageSize: 20 }
        if (pageId)
          query.pageId = pageId
        const { data } = await api.printTemplates(query)
        if (generation !== this.listGeneration)
          return
        this.items = data.records
        this.total = data.total
        this.pageNum = pageNum
      }
      catch (error) {
        if (generation === this.listGeneration) {
          this.items = []
          this.error = error.message || '无法读取打印模板'
        }
      }
      finally {
        if (generation === this.listGeneration)
          this.listing = false
      }
    },
    async open(id) {
      this.clear()
      const generation = this.generation
      this.loading = true
      try {
        const { data } = await api.printTemplate(id)
        if (generation !== this.generation)
          return
        const { data: catalog } = await api.printCatalog({ source: data.source })
        if (generation !== this.generation)
          return
        const document = JSON.parse(data.schemaJson)
        assertPrintDocument(document)
        this.row = data
        this.document = document
        this.catalog = catalog.fields
        this.name = data.templateName
        this.savedName = data.templateName
      }
      catch (error) {
        if (generation === this.generation)
          this.error = error.message || '无法载入模板'
      }
      finally {
        if (generation === this.generation)
          this.loading = false
      }
    },
    async create(source, name, scenes = DEFAULT_PRINT_SCENES, schema = createPrintDocument()) {
      const payload = printSourcePayload(source)
      if (!payload)
        throw new Error('打印来源无效')
      const { data } = await api.createPrintTemplate({ ...payload, templateName: name, templateCode: newPrintTemplateCode(), schemaJson: JSON.stringify(schema) })
      try {
        await syncPrintTemplateScenes({
          source: printSourcePayload(data.source) || payload,
          templateId: data.id,
          scenes,
          bindings: [],
          save: api.savePrintBinding,
          remove: api.deletePrintBinding,
        })
      }
      catch (error) {
        if (window.$message)
          window.$message.warning(error.message || '模板已创建，但场景绑定失败')
      }
      return data
    },
    async save(document) {
      if (!this.row || this.saving)
        throw new Error('模板未就绪或正在保存')
      const generation = this.generation
      const row = this.row
      const name = this.name
      this.saving = true
      this.error = ''
      this.notice = ''
      try {
        const { data } = await api.updatePrintTemplate(row.id, { expectedRevision: row.draftRevision, templateName: name, schemaJson: JSON.stringify(document) })
        if (generation !== this.generation)
          return
        this.row = data
        this.savedName = name
        // 只确认发出时的名称；画布 document 保持原引用，避免覆盖请求期间的编辑。
        window.$message?.success?.('草稿已保存')
      }
      catch (error) {
        if (generation === this.generation)
          this.error = formatPrintApiError(error, '保存失败')
        throw error
      }
      finally {
        if (generation === this.generation)
          this.saving = false
      }
    },
    async publish() {
      if (!this.row || this.saving)
        return false
      const generation = this.generation
      const row = this.row
      this.saving = true
      this.error = ''
      try {
        const { data } = await api.publishPrintTemplate(row.id, { expectedRevision: row.draftRevision })
        if (generation !== this.generation)
          return false
        this.row = data.template
        window.$message?.success?.(`已发布模板版本 ${data.version.versionNo}`)
        return true
      }
      catch (error) {
        if (generation === this.generation)
          this.error = error.message || '发布失败'
        return false
      }
      finally {
        if (generation === this.generation)
          this.saving = false
      }
    },
    async loadVersions() {
      const generation = this.generation
      const { data } = await api.printVersions(this.row.id)
      if (generation === this.generation)
        this.versions = data
    },
    async versionDocument(versionId) {
      const generation = this.generation
      const { data } = await api.printVersion(this.row.id, versionId)
      if (generation !== this.generation)
        return null
      const document = JSON.parse(data.schemaJson)
      assertPrintDocument(document)
      return document
    },
    async loadBindings() {
      const generation = this.generation
      this.bindings = []
      const payload = printSourcePayload(this.row?.source)
      if (!payload)
        return
      const { data } = await api.printBindings(payload)
      if (generation === this.generation)
        this.bindings = data
    },
    async bind(isDefault, status = 1) {
      const row = this.row
      const generation = this.generation
      const payload = printSourcePayload(row.source)
      if (!payload)
        throw new Error('打印来源无效')
      const binding = this.bindings.find(item => String(item.templateId) === String(row.id) && item.scene === this.scene)
      await api.savePrintBinding({ source: payload, templateId: row.id, scene: this.scene, id: binding?.id, expectedRevision: binding?.bindingRevision, isDefault, status, sortOrder: binding?.sortOrder ?? 0 })
      if (generation === this.generation)
        await this.loadBindings()
    },
    async unbind(binding) {
      const generation = this.generation
      await api.deletePrintBinding(binding.id, binding.bindingRevision)
      if (generation === this.generation)
        await this.loadBindings()
    },
  },
})
