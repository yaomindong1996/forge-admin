<template>
  <div class="publish-panel">
    <div class="publish-card">
      <div class="card-title">
        发布配置
      </div>
      <n-form label-placement="left" label-width="92" size="small">
        <n-form-item label="业务领域">
          <n-input :value="props.draft.domainName || props.draft.domainCode || '-'" disabled />
        </n-form-item>
        <n-form-item label="主数据模型">
          <n-input :value="props.draft.objectName || props.draft.objectCode || '-'" disabled />
        </n-form-item>
        <n-form-item label="引用模型">
          <n-input :value="modelSummary" disabled />
        </n-form-item>
        <n-form-item label="菜单名称">
          <n-input v-model:value="form.menuName" placeholder="发布后的菜单名称" />
        </n-form-item>
        <n-form-item label="菜单父级">
          <MenuParentSelect v-model:value="form.menuParentId" />
        </n-form-item>
        <n-form-item label="菜单排序">
          <n-input-number v-model:value="form.menuSort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-alert type="info" :bordered="false">
          表结构创建和字段追加已移动到数据模型设计页。发布仅生成运行配置、菜单和版本快照。
        </n-alert>
        <n-space>
          <n-button type="primary" :loading="publishing" :disabled="!appId" @click="publish">
            一键发布
          </n-button>
        </n-space>
      </n-form>
    </div>

    <VersionTimeline
      :versions="versions"
      :loading="versionLoading"
      @rollback="rollback"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  lowcodePublish,
  lowcodeRollback,
  lowcodeVersions,
} from '@/api/lowcode-crud'
import MenuParentSelect from '../shared/MenuParentSelect.vue'
import VersionTimeline from './VersionTimeline.vue'

const props = defineProps({
  appId: {
    type: [String, Number],
    default: null,
  },
  draft: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['published', 'rolledBack'])

const form = reactive({
  menuName: '',
  menuParentId: null,
  menuSort: 0,
})
const publishing = ref(false)
const versions = ref([])
const versionLoading = ref(false)
const modelSummary = computed(() => {
  const refs = props.draft.pageSchema?.modelRefs || []
  if (!refs.length)
    return props.draft.objectName || props.draft.objectCode || '-'
  return refs.map(item => item.modelName || item.modelCode).filter(Boolean).join('、')
})

watch(
  () => props.draft,
  (value) => {
    form.menuName = value.menuName || value.appName || value.modelSchema?.businessName || ''
    form.menuParentId = value.menuParentId || null
    form.menuSort = value.menuSort || 0
  },
  { immediate: true, deep: true },
)

onMounted(() => {
  loadVersions()
})

async function publish() {
  if (!props.appId) {
    window.$message?.warning('请先保存草稿')
    return
  }
  publishing.value = true
  try {
    await lowcodePublish(props.appId, {
      deployMode: 'SKIP_DDL',
      confirmOnlineDdl: false,
      domainId: props.draft.domainId,
      domainCode: props.draft.domainCode,
      domainName: props.draft.domainName,
      objectCode: props.draft.objectCode,
      objectName: props.draft.objectName,
      businessSuiteCode: props.draft.businessSuiteCode || props.draft.domainCode,
      businessObjectCode: props.draft.businessObjectCode || props.draft.objectCode,
      businessObjectName: props.draft.businessObjectName || props.draft.objectName,
      menuName: form.menuName,
      menuParentId: form.menuParentId,
      menuSort: form.menuSort,
      modelSchema: props.draft.modelSchema,
      pageSchema: props.draft.pageSchema,
    })
    window.$message?.success('发布成功')
    await loadVersions()
    emit('published')
  }
  catch (e) {
    window.$message?.error(e?.message || '发布失败')
  }
  finally {
    publishing.value = false
  }
}

async function loadVersions() {
  if (!props.appId)
    return
  versionLoading.value = true
  try {
    const res = await lowcodeVersions(props.appId)
    versions.value = res.data || []
  }
  finally {
    versionLoading.value = false
  }
}

async function rollback(versionId) {
  if (!props.appId)
    return
  await lowcodeRollback(props.appId, versionId)
  window.$message?.success('回滚成功')
  await loadVersions()
  emit('rolledBack')
}
</script>

<style scoped>
.publish-panel {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) 340px;
  gap: 16px;
}

.publish-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

@media (max-width: 1280px) {
  .publish-panel {
    grid-template-columns: 1fr;
  }
}
</style>
