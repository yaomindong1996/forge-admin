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
        <n-form-item label="挂载位置">
          <n-radio-group v-model:value="form.mountTarget" size="small">
            <n-radio-button value="ADMIN">
              管理端
            </n-radio-button>
            <n-radio-button value="MOBILE">
              移动端
            </n-radio-button>
            <n-radio-button value="BOTH">
              两端同时
            </n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="菜单名称">
          <n-input v-model:value="form.menuName" placeholder="发布后的菜单名称" />
        </n-form-item>
        <n-form-item v-if="showAdminMenuConfig" label="菜单父级">
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

    <div v-if="publishedLinks" class="publish-links-card">
      <div class="card-title">
        发布成功 — 访问链接
      </div>
      <div class="link-group">
        <div v-for="link in publishedLinks" :key="link.label" class="link-row">
          <span class="link-label">{{ link.label }}</span>
          <n-input-group>
            <n-input :value="link.url" readonly size="small" />
            <n-button size="small" @click="copyLink(link.url)">
              复制
            </n-button>
          </n-input-group>
        </div>
      </div>
    </div>

    <VersionTimeline
      :versions="versions"
      :loading="versionLoading"
      @rollback="rollback"
    />
  </div>
</template>

<script setup>
import { useClipboard } from '@vueuse/core'
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
  mountTarget: 'ADMIN',
})
const publishing = ref(false)
const versions = ref([])
const versionLoading = ref(false)
const publishedLinks = ref(null)

const showAdminMenuConfig = computed(() => {
  return form.mountTarget === 'ADMIN' || form.mountTarget === 'BOTH'
})

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
    form.mountTarget = value.mountTarget || 'ADMIN'
  },
  { immediate: true, deep: true },
)

onMounted(() => {
  loadVersions()
})

function buildPublishedLinks() {
  const configKey = props.draft.configKey
  const appId = props.appId
  if (!configKey) {
    return null
  }

  const origin = window.location.origin
  const links = []
  const mt = form.mountTarget

  if (mt === 'ADMIN' || mt === 'BOTH') {
    links.push({
      label: '管理端 · 列表页',
      url: `${origin}/ai/crud-page/${configKey}?appId=${appId}`,
    })
    links.push({
      label: '管理端 · 表单填报',
      url: `${origin}/ai/crud-page/${configKey}?appId=${appId}&runtimeOpenMode=CREATE_FORM&mode=create`,
    })
  }
  if (mt === 'MOBILE' || mt === 'BOTH') {
    links.push({
      label: '移动端 · 列表页',
      url: `/pages/lowcode-runtime?configKey=${configKey}&appId=${appId}`,
    })
    links.push({
      label: '移动端 · 表单填报',
      url: `/pages/lowcode-runtime?configKey=${configKey}&appId=${appId}&mode=create`,
    })
  }
  return links
}

function copyLink(url) {
  const { copy } = useClipboard()
  copy(url).then(() => {
    window.$message?.success('链接已复制到剪贴板')
  }).catch(() => {
    window.$message?.error('复制失败，请手动复制')
  })
}

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
      menuParentId: showAdminMenuConfig.value ? form.menuParentId : null,
      menuSort: form.menuSort,
      mountTarget: form.mountTarget,
      modelSchema: props.draft.modelSchema,
      pageSchema: props.draft.pageSchema,
    })
    publishedLinks.value = buildPublishedLinks()
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
  publishedLinks.value = null
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

.publish-card,
.publish-links-card {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 14px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.link-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.link-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.link-label {
  flex-shrink: 0;
  min-width: 110px;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .publish-panel {
    grid-template-columns: 1fr;
  }
}
</style>
