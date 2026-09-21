<template>
  <section class="settings-section-card application-print-settings">
    <header>
      <h2>打印模板</h2>
      <p>为当前应用的业务表单创建、设计和发布打印模板。</p>
    </header>

    <n-spin :show="loading">
      <ApplicationPrintPanel
        v-if="workspace"
        :application="workspace.application"
        :application-objects="workspace.objects"
      />
      <n-result
        v-else-if="loadError"
        status="error"
        title="打印工作区加载失败"
        :description="loadError"
      >
        <template #footer>
          <n-button @click="loadWorkspace">
            重新加载
          </n-button>
        </template>
      </n-result>
      <n-empty v-else-if="!loading" description="当前应用没有可用的打印工作区" />
    </n-spin>
  </section>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { businessApplicationWorkspaceByCode } from '@/api/business-application'
import ApplicationPrintPanel from '../../application-workspace/ApplicationPrintPanel.vue'

const props = defineProps({
  application: { type: Object, default: null },
})

const loading = ref(false)
const loadError = ref('')
const workspace = ref(null)
let loadGeneration = 0

async function loadWorkspace() {
  const applicationCode = String(props.application?.applicationCode || '').trim()
  const generation = ++loadGeneration
  workspace.value = null
  loadError.value = ''
  if (!applicationCode) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const response = await businessApplicationWorkspaceByCode(applicationCode)
    if (generation !== loadGeneration)
      return
    const result = response.data || null
    if (!result?.application)
      throw new Error('应用打印工作区不存在')
    workspace.value = {
      application: result.application,
      objects: Array.isArray(result.objects) ? result.objects : [],
    }
  }
  catch (error) {
    if (generation === loadGeneration)
      loadError.value = error?.message || '暂时无法读取打印工作区。'
  }
  finally {
    if (generation === loadGeneration)
      loading.value = false
  }
}

watch(() => props.application?.applicationCode, loadWorkspace, { immediate: true })
onBeforeUnmount(() => {
  loadGeneration += 1
})
</script>

<style scoped>
.application-print-settings {
  min-height: 360px;
}

.application-print-settings :deep(.n-spin-container),
.application-print-settings :deep(.n-spin-content) {
  min-height: 260px;
}
</style>
