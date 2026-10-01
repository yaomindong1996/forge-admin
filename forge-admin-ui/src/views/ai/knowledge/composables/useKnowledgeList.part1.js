/** list.vue setup part 1. */
import { NButton, NPopconfirm, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  knowledgeDocumentPage as fetchDocPage,
  knowledgePage as fetchKbPage,
  modelPage as fetchModelPage,
  storeInstancePage as fetchStorePage,
  knowledgeCreate,
  knowledgeDelete,
  knowledgeDocumentDelete,
  knowledgeDocumentChunks,
  knowledgeDocumentContent,
  knowledgeDocumentProgressSSE,
  knowledgeDocumentReprocess,
  knowledgeDocumentUpload,
  knowledgeUpdate,
  ragSearchDebug,
} from '@/api/ai'
import DictTag from '@/components/DictTag.vue'
import { useAuthStore } from '@/store'
import { useDict } from '@/composables/useDict'
export function applyKnowledgeListPart1() {
  const __impl = {}
  const mut = {}

  const authStore = useAuthStore()
  const { dict } = useDict('ai_status', 'ai_knowledge_process_status', 'ai_store_instance_category', 'ai_vector_store_type')

  const statusOptions = computed(() => dict.value.ai_status || [])
  const processStatusOptions = computed(() => dict.value.ai_knowledge_process_status || [])
  const pageSizes = [10, 20, 50]
  const modalCardStyle = { maxWidth: '860px', width: 'calc(100vw - 32px)' }

  const kbDrawerWidth = computed(() => {
    if (typeof window === 'undefined')
      return 760
    return Math.min(760, Math.max(400, window.innerWidth - 24))
  })

  const chunkStrategyOptions = [
    { label: '长度分块', value: 'length' },
    { label: '分隔符分块', value: 'delimiter' },
    { label: '正则分块', value: 'regex' },
    { label: '智能分块', value: 'smart' },
    { label: '问答分块', value: 'qa' },
  ]
  const chunkStrategyIcons = {
    length: '⚡',
    delimiter: '✂',
    regex: '.*',
    smart: '🧠',
    qa: '❓',
  }
  const dedupStrategyOptions = [
    { label: '不去重', value: 'none' },
    { label: '按名称去重', value: 'name' },
    { label: '按内容去重', value: 'content' },
    { label: '名称或内容', value: 'name_or_content' },
  ]

  // 切片配置（按策略展开）
  const chunkMaxTokens = ref(600)
  const chunkOverlap = ref(null)
  const chunkDelimiters = ref('')
  const chunkRegex = ref('')

  const uploadPrefix = import.meta.env.VITE_REQUEST_PREFIX || '/dev-api'
  const uploadHeaders = computed(() => {
    const token = authStore.accessToken
    return { Authorization: token ? `Bearer ${token}` : '' }
  })

  const search = reactive({ name: '' })
  const kbList = ref([])
  const loading = ref(false)
  const selectedKb = ref(null)
  const pagination = reactive({ pageNum: 1, pageSize: 10, itemCount: 0 })

  const docSearch = reactive({ processStatus: null })
  const docList = ref([])
  const docLoading = ref(false)
  const docPagination = reactive({ pageNum: 1, pageSize: 10, itemCount: 0 })
  const docProcessing = reactive({})

  const kbFormRef = ref(null)
  const kbModal = reactive({ show: false, isEdit: false, saving: false, form: createKbForm() })
  const searchModal = reactive({
    show: false,
    query: '',
    topK: 5,
    threshold: 0.5,
    loading: false,
    results: [],
    meta: null,
    searchType: '',
    rerankEnable: false,
    lostInMiddle: false,
    fusionStrategy: 'rrf',
    queryComplete: false,
    nearbyCount: 0,
    filterExpr: '',
    showAdvanced: false,
  })

  // 存储实例 / 模型下拉数据
  const storeInstanceOptions = ref([])
  const embeddingModelOptions = ref([])
  const rerankModelOptions = ref([])

  // 检索模式选项（检索调试弹窗分段按钮；空串=管线默认融合）
  const searchModeOptions = [
    { label: '默认融合', value: '' },
    { label: '纯向量', value: 'vector' },
    { label: 'BM25', value: 'bm25' },
    { label: '混合', value: 'hybrid' },
  ]
  // 融合策略选项（检索调试弹窗高级设置；对齐后端 fusionStrategy=rrf/weighted_sum）
  const fusionStrategyOptions = [
    { label: 'RRF 融合（默认）', value: 'rrf' },
    { label: '加权融合', value: 'weighted_sum' },
  ]
  // 融合方式选项（知识库检索配置）
  const rerankTypeOptions = [
    { label: 'RRF 融合（默认）', value: 'rrf' },
    { label: '加权融合（向量 + BM25）', value: 'weighted' },
  ]

  // 检索配置（存 search_config_json；可空项不写入，交给后端默认）
  function defaultSearchCfg() {
    return {
      rerankEnable: false,
      lostInMiddle: false,
      nearbyCount: 0,
      topK: null,
      threshold: null,
      rerankType: null,
      vectorWeight: null,
      bm25Weight: null,
      rrfK: null,
    }
  }

  const searchCfg = reactive(defaultSearchCfg())

  function resetSearchCfg() {
    Object.assign(searchCfg, defaultSearchCfg())
  }

  function loadSearchCfg(json) {
    if (!json)
      return
    let cfg
    try {
      cfg = JSON.parse(json)
    }
    catch { return }
    if (!cfg || typeof cfg !== 'object')
      return
    searchCfg.rerankEnable = cfg.rerank_enable != null ? cfg.rerank_enable : false
    searchCfg.lostInMiddle = cfg.lost_in_middle != null ? cfg.lost_in_middle : false
    searchCfg.nearbyCount = cfg.nearby_count != null ? cfg.nearby_count : 0
    searchCfg.topK = cfg.topK != null ? cfg.topK : null
    searchCfg.threshold = cfg.threshold != null ? cfg.threshold : null
    searchCfg.rerankType = cfg.rerank_type || null
    searchCfg.vectorWeight = cfg.vector_weight != null ? cfg.vector_weight : null
    searchCfg.bm25Weight = cfg.bm25_weight != null ? cfg.bm25_weight : null
    searchCfg.rrfK = cfg.rrf_k != null ? cfg.rrf_k : null
  }

  function createKbForm() {
    return {
      knowledgeName: '',
      description: '',
      icon: '',
      vectorStoreInstanceId: null,
      embeddingModelId: null,
      rerankModelId: null,
      dimensionOfVectorModel: null,
      chunkStrategy: 'length',
      chunkConfigJson: '',
      searchConfigJson: '',
      dedupStrategy: 'none',
      dedupAction: 'reject',
      uploadConfirm: '0',
      status: '0',
    }
  }

  const kbRules = {
    knowledgeName: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
    vectorStoreInstanceId: [{ required: true, message: '请选择向量存储实例', trigger: 'change' }],
  }

  async function loadKbs() {
    loading.value = true
    try {
      const res = await fetchKbPage({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        ...(search.name ? { knowledgeName: search.name } : {}),
      })
      if (res.code === 200 && res.data) {
        kbList.value = res.data.records || []
        pagination.itemCount = Number(res.data.total || 0)
        if (selectedKb.value) {
          const current = kbList.value.find(k => k.id === selectedKb.value.id)
          if (current)
            selectedKb.value = current
        }
      }
    }
    catch {}
    finally {
      loading.value = false
    }
  }

  async function loadStoreInstances() {
    try {
      const res = await fetchStorePage({ pageNum: 1, pageSize: 100 })
      if (res.code === 200 && res.data)
        storeInstanceOptions.value = (res.data.records || []).map(s => ({ label: s.instanceName, value: s.id }))
    }
    catch {}
  }

  async function loadModelOptions() {
    try {
      const [embRes, rerankRes] = await Promise.all([
        fetchModelPage({ pageNum: 1, pageSize: 100, modelType: 'embedding' }),
        fetchModelPage({ pageNum: 1, pageSize: 100, modelType: 'rerank' }),
      ])
      if (embRes.code === 200 && embRes.data)
        embeddingModelOptions.value = (embRes.data.records || []).map(m => ({ label: m.modelName || m.modelId, value: m.id }))
      if (rerankRes.code === 200 && rerankRes.data)
        rerankModelOptions.value = (rerankRes.data.records || []).map(m => ({ label: m.modelName || m.modelId, value: m.id }))
    }
    catch {}
  }

  function handleSearch() {
    pagination.pageNum = 1
    loadKbs()
  }

  function handleReset() {
    search.name = ''
    pagination.pageNum = 1
    loadKbs()
  }

  function handlePageChange(page) {
    pagination.pageNum = page
    loadKbs()
  }

  function handlePageSizeChange(pageSize) {
    pagination.pageSize = pageSize
    pagination.pageNum = 1
    loadKbs()
  }

  function handleSelect(kb) {
    if (selectedKb.value?.id === kb.id)
      return
    selectedKb.value = kb
    docPagination.pageNum = 1
    loadDocs()
  }

  function handleAdd() {
    kbModal.isEdit = false
    kbModal.form = createKbForm()
    resetSearchCfg()
    kbModal.show = true
  }

  async function handleEdit(kb) {
    kbModal.isEdit = true
    kbModal.form = { ...createKbForm(), ...kb }
    resetSearchCfg()
    loadSearchCfg(kb.searchConfigJson)
    kbModal.show = true
  }

  async function handleSave() {
    try {
      await kbFormRef.value?.validate()
    }
    catch { return }
    kbModal.saving = true
    try {
      const payload = { ...kbModal.form }
      // 按切片策略拼 chunkConfigJson
      const strategy = payload.chunkStrategy
      if (strategy === 'length') {
        payload.chunkConfigJson = JSON.stringify({
          max_tokens: chunkMaxTokens.value,
          overlap: chunkOverlap.value ?? 16,
        })
      }
      else if (strategy === 'delimiter') {
        payload.chunkConfigJson = JSON.stringify({ delimiters: chunkDelimiters.value })
      }
      else if (strategy === 'regex') {
        payload.chunkConfigJson = JSON.stringify({ regex: chunkRegex.value })
      }
      // 拼检索配置 searchConfigJson（布尔/数量总是写入以支持关闭；可空项不写入，交给后端默认）
      const cfg = {}
      if (searchCfg.rerankEnable != null)
        cfg.rerank_enable = searchCfg.rerankEnable
      if (searchCfg.lostInMiddle != null)
        cfg.lost_in_middle = searchCfg.lostInMiddle
      if (searchCfg.nearbyCount != null)
        cfg.nearby_count = searchCfg.nearbyCount
      if (searchCfg.topK != null)
        cfg.topK = searchCfg.topK
      if (searchCfg.threshold != null)
        cfg.threshold = searchCfg.threshold
      if (searchCfg.rerankType)
        cfg.rerank_type = searchCfg.rerankType
      if (searchCfg.vectorWeight != null)
        cfg.vector_weight = searchCfg.vectorWeight
      if (searchCfg.bm25Weight != null)
        cfg.bm25_weight = searchCfg.bm25Weight
      if (searchCfg.rrfK != null)
        cfg.rrf_k = searchCfg.rrfK
      payload.searchConfigJson = Object.keys(cfg).length ? JSON.stringify(cfg) : ''
      const res = kbModal.isEdit ? await knowledgeUpdate(payload) : await knowledgeCreate(payload)
      if (res.code === 200) {
        window.$message.success(kbModal.isEdit ? '更新成功' : '新增成功')
        kbModal.show = false
        await loadKbs()
      }
      else {
        window.$message.error(res.msg || '操作失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '操作失败')
    }
    finally {
      kbModal.saving = false
    }
  }

  async function handleDelete(id) {
    try {
      const res = await knowledgeDelete(id)
      if (res.code === 200) {
        window.$message.success('删除成功')
        if (selectedKb.value?.id === id)
          selectedKb.value = null
        await loadKbs()
      }
      else {
        window.$message.error(res.msg || '删除失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '删除失败')
    }
  }

  // ===== 文档管理 =====

  async function loadDocs() {
    if (!selectedKb.value) {
      docList.value = []
      return
    }
    docLoading.value = true
    try {
      const res = await fetchDocPage({
        pageNum: docPagination.pageNum,
        pageSize: docPagination.pageSize,
        knowledgeId: selectedKb.value.id,
        ...(docSearch.processStatus ? { processStatus: docSearch.processStatus } : {}),
      })
      if (res.code === 200 && res.data) {
        docList.value = res.data.records || []
        docPagination.itemCount = Number(res.data.total || 0)
      }
    }
    catch {}
    finally {
      docLoading.value = false
    }
  }

  function handleDocSearch() {
    docPagination.pageNum = 1
    loadDocs()
  }

  function handleDocPageChange(page) {
    docPagination.pageNum = page
    loadDocs()
  }

  function handleDocPageSizeChange(pageSize) {
    docPagination.pageSize = pageSize
    docPagination.pageNum = 1
    loadDocs()
  }

  function handleUploadFinish({ event }) {
    try {
      const res = JSON.parse(event.target.response)
      if (res.code === 200 && res.data) {
        const fileId = res.data.fileId || res.data.id
        submitDocumentUpload(fileId, res.data)
      }
      else {
        window.$message.error(res.msg || '上传失败')
      }
    }
    catch {
      window.$message.error('上传失败')
    }
    return false
  }

  function handleUploadError() {
    window.$message.error('文件上传失败')
  }

  async function submitDocumentUpload(fileId, fileData) {
    try {
      const res = await knowledgeDocumentUpload({
        knowledgeId: selectedKb.value.id,
        fileId,
        docName: fileData.originalName || fileData.fileName || `文档${Date.now()}`,
        sourceType: 'upload',
        confirm: true,
      })
      if (res.code === 200) {
        window.$message.success('文档上传成功，开始处理')
        await loadDocs()
      }
      else {
        window.$message.error(res.msg || '文档上传失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '文档上传失败')
    }
  }

  function subscribeDocProgress(doc) {
    if (docProcessing[doc.id])
      return
    docProcessing[doc.id] = true
    knowledgeDocumentProgressSSE(
      doc.id,
      (event) => {
        if (event && (event.percent === 100 || event.status === 'success' || event.status === 'failed')) {
          docProcessing[doc.id] = false
          loadDocs()
        }
      },
      () => { docProcessing[doc.id] = false },
      () => { docProcessing[doc.id] = false },
    )
  }

  async function handleDeleteDoc(doc) {
    try {
      const res = await knowledgeDocumentDelete(doc.id)
      if (res.code === 200) {
        window.$message.success('删除成功')
        await loadDocs()
      }
      else {
        window.$message.error(res.msg || '删除失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '删除失败')
    }
  }

  async function reprocessDoc(doc) {
    try {
      const res = await knowledgeDocumentReprocess(doc.id)
      if (res.code === 200) {
        window.$message.success('已重新处理')
        subscribeDocProgress(doc)
        await loadDocs()
      }
      else {
        window.$message.error(res.msg || '重试失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '重试失败')
    }
  }

  // ===== 文档查看（原文 / 分块） =====

  const docViewModal = reactive({
    show: false,
    docName: '',
    tab: 'content',
    content: null,
    chunks: [],
    loading: false,
  })

  // 文档统计（原文字符/段落、分块词元合计）
  const docCharCount = computed(() => (docViewModal.content || '').length)
  const docParagraphCount = computed(() => {
    const s = docViewModal.content || ''
    return s ? s.split(/\r?\n/).filter((l) => l.trim()).length : 0
  })
  const chunkTotalTokens = computed(() =>
    docViewModal.chunks.reduce((sum, c) => sum + (c.tokenCount ?? 0), 0),
  )

  async function openDocView(doc) {
    docViewModal.show = true
    docViewModal.docName = doc.docName
    docViewModal.tab = 'content'
    docViewModal.content = null
    docViewModal.chunks = []
    docViewModal.loading = true
    try {
      const [contentRes, chunkRes] = await Promise.all([
        knowledgeDocumentContent(doc.id),
        knowledgeDocumentChunks(doc.id),
      ])
      docViewModal.content = contentRes.code === 200 ? contentRes.data : null
      docViewModal.chunks = chunkRes.code === 200 && Array.isArray(chunkRes.data) ? chunkRes.data : []
      if (contentRes.code !== 200) window.$message.error(contentRes.msg || '原文加载失败')
      if (chunkRes.code !== 200) window.$message.error(chunkRes.msg || '分块加载失败')
    }
    catch (e) {
      window.$message.error(e.message || '文档加载失败')
    }
    finally {
      docViewModal.loading = false
    }
  }

  // ===== 检索调试 =====

  function openSearchDebug() {
    // 打开时按知识库检索配置初始化（rerank/nearby 跟随配置，可临时覆盖做对比）
    let cfg = null
    try {
      cfg = selectedKb.value?.searchConfigJson ? JSON.parse(selectedKb.value.searchConfigJson) : null
    }
    catch {}
    searchModal.rerankEnable = cfg?.rerank_enable != null ? cfg.rerank_enable : false
    searchModal.lostInMiddle = cfg?.lost_in_middle != null ? cfg.lost_in_middle : false
    searchModal.nearbyCount = cfg?.nearby_count != null ? cfg.nearby_count : 0
    searchModal.topK = cfg?.topK || 5
    searchModal.threshold = cfg?.threshold != null ? cfg.threshold : 0.5
    searchModal.searchType = ''
    searchModal.fusionStrategy = 'rrf'
    searchModal.queryComplete = false
    searchModal.filterExpr = ''
    searchModal.results = []
    searchModal.meta = null
    searchModal.showAdvanced = false
    searchModal.show = true
  }

  async function handleSearchDebug() {
    if (!searchModal.query.trim()) {
      window.$message.warning('请输入检索问题')
      return
    }
    searchModal.loading = true
    searchModal.results = []
    searchModal.meta = null
    try {
      const params = {
        knowledgeId: selectedKb.value.id,
        query: searchModal.query,
        topK: searchModal.topK || 5,
        threshold: searchModal.threshold,
        rerankEnable: searchModal.rerankEnable,
        lostInMiddle: searchModal.lostInMiddle,
        fusionStrategy: searchModal.fusionStrategy,
        queryComplete: searchModal.queryComplete,
        nearbyCount: searchModal.nearbyCount > 0 ? searchModal.nearbyCount : undefined,
        filterExpr: searchModal.filterExpr?.trim() || undefined,
      }
      if (searchModal.searchType)
        params.searchType = searchModal.searchType
      // 走 RAG 管线调试端点：返回结果 + 元信息（实际检索类型/各路命中数/耗时/补全query）
      const res = await ragSearchDebug(params)
      if (res.code === 200 && res.data) {
        searchModal.results = res.data.list || []
        searchModal.meta = res.data.meta || null
      }
      else {
        window.$message.error(res.msg || '检索失败')
      }
    }
    catch (e) {
      window.$message.error(e.message || '检索失败')
    }
    finally {
      searchModal.loading = false
    }
  }

  // ===== 检索调试辅助 =====

  const searchModeLabel = computed(() => {
    const t = searchModal.meta?.searchType
    if (!t)
      return '默认融合'
    if (t === 'vector')
      return '纯向量'
    if (t === 'bm25')
      return 'BM25'
    if (t === 'hybrid')
      return '混合'
    return t
  })

  function scorePercent(score) {
    const v = Number(score) || 0
    return Math.max(0, Math.min(100, v * 100))
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
  }

  function escapeRegExp(s) {
    return String(s).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  }

  function highlightText(text, query) {
    if (!text)
      return ''
    let html = escapeHtml(text)
    const terms = (query || '').trim().split(/\s+/).filter(Boolean)
    for (const t of terms) {
      const escaped = escapeHtml(t)
      if (!escaped)
        continue
      html = html.replace(new RegExp(escapeRegExp(escaped), 'gi'), (m) => `<mark class="fm-hl">${m}</mark>`)
    }
    return html
  }

  function copyText(text) {
    if (!text)
      return
    navigator.clipboard?.writeText(text)
      .then(() => window.$message.success('已复制'))
      .catch(() => window.$message.error('复制失败'))
  }

  function openDocFromResult(r) {
    openDocView({ id: r.documentId, docName: r.docName || `文档 #${r.documentId}` })
  }

  function resetSearchDebug() {
    searchModal.query = ''
    searchModal.results = []
    searchModal.meta = null
    searchModal.searchType = ''
    searchModal.showAdvanced = false
    searchModal.threshold = 0.5
    searchModal.topK = 5
    searchModal.rerankEnable = false
    searchModal.lostInMiddle = false
    searchModal.fusionStrategy = 'rrf'
    searchModal.queryComplete = false
    searchModal.nearbyCount = 0
    searchModal.filterExpr = ''
  }

  // ===== 表格列 =====

  const docColumns = [
    { title: '文档名称', key: 'docName', width: 240, ellipsis: { tooltip: true } },
    {
      title: '类型',
      key: 'docType',
      width: 90,
      render(row) { return h('span', {}, row.docType || '—') },
    },
    {
      title: '处理状态',
      key: 'processStatus',
      width: 110,
      render(row) {
        const opts = processStatusOptions.value
        return h(DictTag, { dictType: 'ai_knowledge_process_status', value: row.processStatus, size: 'small' })
      },
    },
    { title: '分块数', key: 'chunkCount', width: 80, align: 'right', render(row) { return row.chunkCount ?? '-' } },
    {
      title: '处理时间',
      key: 'updateTime',
      width: 150,
      render(row) { return row.updateTime ? String(row.updateTime).replace('T', ' ').slice(0, 16) : '—' },
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      fixed: 'right',
      render(row) {
        const actions = []
        actions.push(h(NButton, { text: true, size: 'small', onClick: () => openDocView(row) }, { default: () => '查看' }))
        if (row.processStatus === 'pending' || row.processStatus === 'processing') {
          actions.push(h(NButton, { text: true, size: 'small', class: 'text-warning', loading: !!docProcessing[row.id], onClick: () => subscribeDocProgress(row) }, { default: () => docProcessing[row.id] ? '处理中' : '刷新进度' }))
        }
        if (row.processStatus === 'failed') {
          actions.push(h(NButton, { text: true, size: 'small', class: 'text-warning', onClick: () => reprocessDoc(row) }, { default: () => '重试' }))
        }
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleDeleteDoc(row) }, {
          trigger: () => h(NButton, { text: true, size: 'small', class: 'text-error' }, { default: () => '删除' }),
          default: () => '确定删除该文档吗？',
        }))
        return h('div', { class: 'table-actions' }, actions)
      },
    },
  ]

  onMounted(() => {
    loadKbs()
    loadStoreInstances()
    loadModelOptions()
  })
  __impl.defaultSearchCfg = defaultSearchCfg
  __impl.resetSearchCfg = resetSearchCfg
  __impl.loadSearchCfg = loadSearchCfg
  __impl.createKbForm = createKbForm
  __impl.loadKbs = loadKbs
  __impl.loadStoreInstances = loadStoreInstances
  __impl.loadModelOptions = loadModelOptions
  __impl.handleSearch = handleSearch
  __impl.handleReset = handleReset
  __impl.handlePageChange = handlePageChange
  __impl.handlePageSizeChange = handlePageSizeChange
  __impl.handleSelect = handleSelect
  __impl.handleAdd = handleAdd
  __impl.handleEdit = handleEdit
  __impl.handleSave = handleSave
  __impl.handleDelete = handleDelete
  __impl.loadDocs = loadDocs
  __impl.handleDocSearch = handleDocSearch
  __impl.handleDocPageChange = handleDocPageChange
  __impl.handleDocPageSizeChange = handleDocPageSizeChange
  __impl.handleUploadFinish = handleUploadFinish
  __impl.handleUploadError = handleUploadError
  __impl.submitDocumentUpload = submitDocumentUpload
  __impl.subscribeDocProgress = subscribeDocProgress
  __impl.handleDeleteDoc = handleDeleteDoc
  __impl.reprocessDoc = reprocessDoc
  __impl.openDocView = openDocView
  __impl.openSearchDebug = openSearchDebug
  __impl.handleSearchDebug = handleSearchDebug
  __impl.scorePercent = scorePercent
  __impl.escapeHtml = escapeHtml
  __impl.escapeRegExp = escapeRegExp
  __impl.highlightText = highlightText
  __impl.copyText = copyText
  __impl.openDocFromResult = openDocFromResult
  __impl.resetSearchDebug = resetSearchDebug

  return {
    __impl, mut, copyText, createKbForm, defaultSearchCfg, escapeHtml, escapeRegExp, handleAdd,
    handleDelete, handleDeleteDoc, handleDocPageChange, handleDocPageSizeChange, handleDocSearch, handleEdit, handlePageChange, handlePageSizeChange,
    handleReset, handleSave, handleSearch, handleSearchDebug, handleSelect, handleUploadError, handleUploadFinish, highlightText,
    loadDocs, loadKbs, loadModelOptions, loadSearchCfg, loadStoreInstances, openDocFromResult, openDocView, openSearchDebug,
    reprocessDoc, resetSearchCfg, resetSearchDebug, scorePercent, submitDocumentUpload, subscribeDocProgress, authStore, statusOptions,
    processStatusOptions, pageSizes, modalCardStyle, kbDrawerWidth, chunkStrategyOptions, chunkStrategyIcons, dedupStrategyOptions, chunkMaxTokens,
    chunkOverlap, chunkDelimiters, chunkRegex, uploadPrefix, uploadHeaders, search, kbList, loading,
    selectedKb, pagination, docSearch, docList, docLoading, docPagination, docProcessing, kbFormRef,
    kbModal, searchModal, storeInstanceOptions, embeddingModelOptions, rerankModelOptions, searchModeOptions, fusionStrategyOptions, rerankTypeOptions,
    searchCfg, kbRules, docViewModal, docCharCount, docParagraphCount, chunkTotalTokens, searchModeLabel, docColumns,
  }
}
