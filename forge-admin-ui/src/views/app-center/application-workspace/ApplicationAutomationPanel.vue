<template>
  <div class="automation-panel">
    <header class="panel-heading">
      <div>
        <h2>流程自动化</h2>
        <p>从应用对象进入现有 Flowable 流程、触发器、消息和任务配置。</p>
      </div>
      <span>{{ application?.flowCount || 0 }} 项应用级流程绑定</span>
    </header>

    <n-spin :show="loading">
      <n-empty v-if="!loading && !objects.length" description="请先在数据对象分区加入业务对象" />
      <div v-else class="automation-list">
        <div v-for="item in objects" :key="item.objectId" class="automation-row">
          <div>
            <strong>{{ item.objectName || item.objectCode }}</strong>
            <code>{{ item.objectCode }}</code>
          </div>
          <DictTag dict-type="ai_business_application_object_role" :value="item.objectRole" :bordered="false" />
          <div class="automation-actions">
            <n-button size="small" secondary @click="openPanel(item, 'flow-app')">
              业务流程
            </n-button>
            <n-button size="small" secondary @click="openPanel(item, 'triggers')">
              自动化触发器
            </n-button>
            <n-button size="small" secondary @click="openPanel(item, 'actions')">
              业务动作
            </n-button>
          </div>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { businessApplicationObjects } from '@/api/business-application'
import DictTag from '@/components/DictTag.vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  initialObjects: {
    type: Array,
    default: null,
  },
})

const emit = defineEmits(['openDesigner'])
const loading = ref(false)
const objects = ref([])

watch([
  () => props.application?.id,
  () => props.initialObjects,
], ([applicationId, initialObjects]) => {
  if (!applicationId)
    return
  if (Array.isArray(initialObjects)) {
    objects.value = [...initialObjects]
    return
  }
  loadObjects()
}, { immediate: true })

async function loadObjects() {
  if (!props.application?.id)
    return
  loading.value = true
  try {
    const response = await businessApplicationObjects(props.application.id)
    objects.value = response.data || []
  }
  finally {
    loading.value = false
  }
}

function openPanel(item, panel) {
  emit('openDesigner', {
    objectId: item.objectId,
    objectCode: item.objectCode,
    panel,
  })
}
</script>

<style scoped>
.automation-panel {
  display: grid;
  gap: 16px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.panel-heading h2 {
  margin: 0;
  font-size: 18px;
}

.panel-heading p {
  margin: 5px 0 0;
  color: var(--text-tertiary, #86909c);
}

.panel-heading > span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.automation-list {
  overflow: hidden;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.automation-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 100px auto;
  gap: 16px;
  align-items: center;
  min-height: 62px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.automation-row:last-child {
  border-bottom: 0;
}

.automation-row > div:first-child {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.automation-row strong {
  font-size: 13px;
}

.automation-row code,
.automation-row > span {
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}

.automation-actions {
  display: flex;
  gap: 8px;
}
</style>
