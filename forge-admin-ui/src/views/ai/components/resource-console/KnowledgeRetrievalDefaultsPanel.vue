<script setup>
import { NAlert, NButton, NForm, NFormItem, NInputNumber, NSelect, NSpin } from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getRetrievalDefaults,
  modelPage,
  saveRetrievalDefaults,
  storeInstancePage,
} from '@/api/ai'

const loading = ref(false)
const saving = ref(false)
const configured = ref(false)
const formRef = ref(null)
const embeddingModels = ref([])
const rerankModels = ref([])
const vectorStores = ref([])

const form = reactive({
  embeddingModelId: null,
  rerankModelId: null,
  vectorStoreInstanceId: null,
  dimension: 1536,
  chunkSize: 512,
  expectedRevision: null,
})

const rules = {
  embeddingModelId: { required: true, type: 'any', message: '请选择默认向量模型', trigger: 'change' },
  vectorStoreInstanceId: { required: true, type: 'any', message: '请选择默认向量存储实例', trigger: 'change' },
  dimension: { required: true, type: 'number', message: '请输入向量维度', trigger: 'blur' },
  chunkSize: { required: true, type: 'number', message: '请输入默认分段大小', trigger: 'blur' },
}

const embeddingOptions = computed(() =>
  embeddingModels.value.map(item => ({
    label: `${item.modelName || item.modelId}（${item.modelId}）`,
    value: String(item.id),
  })),
)

const rerankOptions = computed(() =>
  rerankModels.value.map(item => ({
    label: `${item.modelName || item.modelId}（${item.modelId}）`,
    value: String(item.id),
  })),
)

const vectorStoreOptions = computed(() =>
  vectorStores.value.map(item => ({
    label: `${item.instanceName}（${item.storeType}）`,
    value: String(item.id),
  })),
)

async function loadData() {
  loading.value = true
  try {
    const [defaultsRes, embedRes, rerankRes, storeRes] = await Promise.all([
      getRetrievalDefaults(),
      modelPage({ pageNum: 1, pageSize: 200, modelType: 'embedding' }),
      modelPage({ pageNum: 1, pageSize: 200, modelType: 'rerank' }),
      storeInstancePage({ pageNum: 1, pageSize: 200, category: 'vector' }),
    ])
    embeddingModels.value = embedRes.data?.records || []
    rerankModels.value = rerankRes.data?.records || []
    vectorStores.value = storeRes.data?.records || []

    const data = defaultsRes.data
    if (defaultsRes.code === 200 && data?.configured) {
      configured.value = true
      form.embeddingModelId = data.embeddingModelId ? String(data.embeddingModelId) : null
      form.rerankModelId = data.rerankModelId ? String(data.rerankModelId) : null
      form.vectorStoreInstanceId = data.vectorStoreInstanceId ? String(data.vectorStoreInstanceId) : null
      form.dimension = Number(data.dimension || 1536)
      form.chunkSize = Number(data.chunkSize || 512)
      form.expectedRevision = data.revision ?? null
    }
    else {
      configured.value = false
      form.expectedRevision = null
    }
  }
  catch (e) {
    window.$message.error(e.message || '加载检索默认值失败')
  }
  finally {
    loading.value = false
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }
  saving.value = true
  try {
    const res = await saveRetrievalDefaults({
      embeddingModelId: form.embeddingModelId,
      rerankModelId: form.rerankModelId || null,
      vectorStoreInstanceId: form.vectorStoreInstanceId,
      dimension: form.dimension,
      chunkSize: form.chunkSize,
      expectedRevision: form.expectedRevision,
    })
    if (res.code === 200) {
      window.$message.success('默认检索配置已保存')
      await loadData()
    }
    else {
      window.$message.error(res.msg || '保存失败')
    }
  }
  catch (e) {
    window.$message.error(e.message || '保存失败')
  }
  finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<template>
  <section class="retrieval-panel" aria-label="知识检索默认值">
    <div class="retrieval-panel__head">
      <div>
        <h2>知识检索默认值</h2>
        <p>新建知识库时自动带入的向量化模型、向量库与分段参数，降低重复配置成本。</p>
      </div>
      <div class="retrieval-panel__actions">
        <NButton secondary :loading="loading" @click="loadData">
          刷新
        </NButton>
        <NButton type="primary" :loading="saving" @click="handleSave">
          保存默认配置
        </NButton>
      </div>
    </div>

    <NAlert v-if="!loading && !configured" type="info" :bordered="false">
      当前租户尚未设置知识检索默认值，请先在「供应商与模型」添加向量模型、在「向量存储」配置向量库实例后再保存。
    </NAlert>

    <NSpin :show="loading">
      <div class="retrieval-panel__card">
        <NForm ref="formRef" :model="form" :rules="rules" label-placement="top">
          <div class="retrieval-grid">
            <NFormItem label="默认向量模型（Embedding）" path="embeddingModelId">
              <NSelect
                v-model:value="form.embeddingModelId"
                :options="embeddingOptions"
                filterable
                clearable
                placeholder="选择已启用的向量化模型"
              />
            </NFormItem>
            <NFormItem label="默认重排模型（Rerank，可选）" path="rerankModelId">
              <NSelect
                v-model:value="form.rerankModelId"
                :options="rerankOptions"
                filterable
                clearable
                placeholder="不选则使用基础相似度排序"
              />
            </NFormItem>
            <NFormItem label="默认向量存储实例" path="vectorStoreInstanceId">
              <NSelect
                v-model:value="form.vectorStoreInstanceId"
                :options="vectorStoreOptions"
                filterable
                clearable
                placeholder="选择向量存储实例（Milvus / PgVector / ES）"
              />
            </NFormItem>
            <NFormItem label="向量维度（Dimension）" path="dimension">
              <NInputNumber v-model:value="form.dimension" :min="64" :max="8192" :step="64" style="width: 100%" />
            </NFormItem>
            <NFormItem label="默认分段大小（Chunk Size）" path="chunkSize">
              <NInputNumber v-model:value="form.chunkSize" :min="128" :max="4096" :step="64" style="width: 100%" />
            </NFormItem>
          </div>
        </NForm>
      </div>
    </NSpin>
  </section>
</template>

<style scoped>
.retrieval-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px;
}

.retrieval-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.retrieval-panel__head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.retrieval-panel__head p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-tertiary, #6b7280);
}

.retrieval-panel__actions {
  display: flex;
  gap: 8px;
}

.retrieval-panel__card {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  padding: 16px;
}

.retrieval-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px 16px;
}
</style>
