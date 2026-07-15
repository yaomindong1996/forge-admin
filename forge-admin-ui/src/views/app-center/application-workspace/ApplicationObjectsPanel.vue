<template>
  <div class="objects-panel">
    <header class="panel-heading">
      <div>
        <h2>数据对象</h2>
        <p>应用以业务对象组织数据库表、业务字段和页面设计；对象仍可被多个应用复用。</p>
      </div>
      <n-space>
        <n-button secondary @click="openBinder">
          关联已有对象
        </n-button>
        <n-button secondary @click="openCreate('DB_IMPORT')">
          从数据库表导入
        </n-button>
        <n-button type="primary" @click="openCreate('BLANK')">
          新建对象
        </n-button>
      </n-space>
    </header>

    <n-spin :show="loading">
      <n-empty
        v-if="!loading && !objects.length"
        class="objects-empty"
        description="当前应用还没有数据对象"
      >
        <template #extra>
          <n-space>
            <n-button secondary @click="openBinder">
              关联已有对象
            </n-button>
            <n-button type="primary" @click="openCreate('DB_IMPORT')">
              从数据库表开始
            </n-button>
          </n-space>
        </template>
      </n-empty>

      <div v-else class="object-table">
        <div class="object-row object-row-head">
          <span>业务对象</span>
          <span>应用内角色</span>
          <span>数据源 / 物理表</span>
          <span>结构状态</span>
          <span>共享影响</span>
          <span>操作</span>
        </div>
        <div v-for="item in objects" :key="item.objectId" class="object-row">
          <div class="object-identity">
            <strong>{{ item.objectName || item.objectCode }}</strong>
            <span>
              <code>{{ item.objectCode }}</code>
              <DictTag dict-type="ai_business_object_type" :value="item.objectType" :bordered="false" />
            </span>
          </div>
          <DictSelect
            class="role-select"
            :value="item.objectRole"
            dict-type="ai_business_application_object_role"
            :clearable="false"
            :disabled="saving"
            @update:value="value => changeRole(item, value)"
          />
          <div class="database-anchor">
            <span>{{ item.datasourceName || item.datasourceCode || '平台主库' }}</span>
            <code>{{ item.tableName || '尚未确定物理表' }}</code>
          </div>
          <div class="sync-state" :class="syncTone(item.syncStatus)">
            <i />
            <span>{{ syncLabel(item.syncStatus) }}</span>
          </div>
          <span class="shared-impact">
            {{ Number(item.sharedApplicationCount || 0) > 1 ? `${item.sharedApplicationCount} 个应用共用` : '仅当前应用' }}
          </span>
          <div class="object-actions">
            <a class="cursor-pointer text-primary" @click="openDesigner(item)">数据结构</a>
            <a class="cursor-pointer text-info" @click="openForm(item)">设计表单</a>
            <a class="cursor-pointer text-info" @click="openDesignerPanel(item, 'actions')">业务动作</a>
            <a class="cursor-pointer text-error" @click="removeObject(item)">移除</a>
          </div>
        </div>
      </div>
    </n-spin>

    <ApplicationObjectBinder
      v-model:show="binderVisible"
      :objects="availableObjects"
      :loading="availableLoading"
      :default-role="hasPrimary ? 'SHARED' : 'PRIMARY'"
      @confirm="bindObject"
    />

    <BusinessObjectWizardDrawer
      v-model:show="wizardVisible"
      :suites="workspaceSuites"
      :default-suite-code="application?.suiteCode"
      :default-create-mode="createMode"
      lock-suite
      @saved="handleObjectCreated"
    />
  </div>
</template>

<script setup>
import { useDialog, useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import { businessObjectList } from '@/api/business-app'
import {
  businessApplicationObjects,
  saveBusinessApplicationObjects,
} from '@/api/business-application'
import DictSelect from '@/components/DictSelect.vue'
import DictTag from '@/components/DictTag.vue'
import BusinessObjectWizardDrawer from '../components/BusinessObjectWizardDrawer.vue'
import ApplicationObjectBinder from './ApplicationObjectBinder.vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  initialCreateMode: {
    type: String,
    default: '',
  },
  initialObjects: {
    type: Array,
    default: null,
  },
})

const emit = defineEmits(['changed', 'initialCreateOpened', 'openDesigner'])
const dialog = useDialog()
const message = useMessage()
const objects = ref([])
const loading = ref(false)
const saving = ref(false)
const binderVisible = ref(false)
const availableLoading = ref(false)
const availableObjects = ref([])
const wizardVisible = ref(false)
const createMode = ref('BLANK')

const hasPrimary = computed(() => objects.value.some(item => item.objectRole === 'PRIMARY'))
const workspaceSuites = computed(() => props.application
  ? [{
      suiteCode: props.application.suiteCode,
      suiteName: props.application.suiteName || props.application.suiteCode,
    }]
  : [])

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

onMounted(() => {
  if (props.initialCreateMode) {
    openCreate(props.initialCreateMode)
    emit('initialCreateOpened')
  }
})

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

async function openBinder() {
  if (!props.application?.suiteCode)
    return
  binderVisible.value = true
  availableLoading.value = true
  try {
    const response = await businessObjectList({ suiteCode: props.application.suiteCode })
    const currentIds = new Set(objects.value.map(item => String(item.objectId)))
    availableObjects.value = (response.data || []).filter(item => !currentIds.has(String(item.id)))
  }
  finally {
    availableLoading.value = false
  }
}

function openCreate(mode) {
  createMode.value = mode
  wizardVisible.value = true
}

async function bindObject(binding) {
  const next = [...objects.value, {
    objectId: binding.objectId,
    objectRole: binding.objectRole,
    sortOrder: objects.value.length,
  }]
  await persistObjects(next)
  binderVisible.value = false
  message.success('业务对象已加入应用')
}

async function handleObjectCreated(result) {
  const role = hasPrimary.value ? 'SHARED' : 'PRIMARY'
  try {
    await persistObjects([...objects.value, {
      objectId: result.id,
      objectRole: role,
      sortOrder: objects.value.length,
    }])
    openDesigner({ objectCode: result.objectCode })
  }
  catch {
    message.error('对象已创建，但加入应用失败；可通过“关联已有对象”重试')
  }
}

async function changeRole(item, role) {
  if (!role || role === item.objectRole)
    return
  const next = objects.value.map((current) => {
    if (current.objectId === item.objectId)
      return { ...current, objectRole: role }
    if (role === 'PRIMARY' && current.objectRole === 'PRIMARY')
      return { ...current, objectRole: 'SHARED' }
    return current
  })
  await persistObjects(next)
  message.success('对象角色已更新')
}

function removeObject(item) {
  dialog.warning({
    title: '移除应用对象',
    content: `仅移除“${item.objectName || item.objectCode}”与当前应用的编排关系，不会删除业务对象或数据库表。`,
    positiveText: '确认移除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await persistObjects(objects.value.filter(current => current.objectId !== item.objectId))
      message.success('对象关联已移除')
    },
  })
}

async function persistObjects(next) {
  if (!props.application?.id || saving.value)
    return
  saving.value = true
  try {
    await saveBusinessApplicationObjects(props.application.id, next.map((item, index) => ({
      objectId: item.objectId,
      objectRole: item.objectRole,
      sortOrder: Number(item.sortOrder ?? index),
      options: item.options || null,
    })))
    await loadObjects()
    emit('changed')
  }
  finally {
    saving.value = false
  }
}

function openDesigner(item) {
  openDesignerPanel(item, 'fields')
}

function openForm(item) {
  openDesignerPanel(item, 'form')
}

function openDesignerPanel(item, panel) {
  emit('openDesigner', {
    objectId: item.objectId,
    objectCode: item.objectCode,
    panel,
  })
}

function syncLabel(status) {
  const labels = {
    IN_SYNC: '结构已同步',
    FAILED: '上次同步失败',
    OUT_OF_SYNC: '存在未同步变更',
    TABLE_MISSING: '物理表未创建',
    CHECK_FAILED: '结构检查失败',
    UNKNOWN: '待检查',
  }
  return labels[status] || '待检查'
}

function syncTone(status) {
  if (status === 'IN_SYNC')
    return 'is-ready'
  if (status === 'FAILED' || status === 'CHECK_FAILED')
    return 'is-error'
  return 'is-warning'
}
</script>

<style scoped>
.objects-panel {
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
  color: var(--text-primary, #1d2129);
  font-size: 18px;
}

.panel-heading p {
  margin: 5px 0 0;
  color: var(--text-tertiary, #86909c);
}

.objects-empty {
  min-height: 260px;
  padding-top: 64px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.object-table {
  overflow-x: auto;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.object-row {
  display: grid;
  grid-template-columns: minmax(190px, 1.2fr) 132px minmax(190px, 1.15fr) 130px 120px 190px;
  gap: 14px;
  align-items: center;
  min-width: 1020px;
  min-height: 64px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.object-row:last-child {
  border-bottom: 0;
}

.object-row-head {
  min-height: 38px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 12px;
  font-weight: 600;
}

.object-identity,
.database-anchor {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.object-identity strong {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.object-identity > span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.object-identity code,
.database-anchor code {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.database-anchor > span {
  overflow: hidden;
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-select {
  width: 124px;
}

.sync-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.sync-state i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #bf8700;
}

.sync-state.is-ready i {
  background: #2da44e;
}

.sync-state.is-error i {
  background: #cf222e;
}

.shared-impact {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.object-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
}
</style>
