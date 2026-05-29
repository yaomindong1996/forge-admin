<template>
  <div class="relation-panel">
    <NTabs type="line" animated>
      <NTabPane name="runtime" tab="关联入口">
        <div class="panel-intro">
          <div>
            <h3>关联数据入口</h3>
            <p>从当前对象进入下游业务列表，入口会先校验目标对象是否已发布运行配置。</p>
          </div>
        </div>

        <NSpin :show="loadingRuntime">
          <div v-if="runtimeRelations.length" class="relation-runtime-list">
            <div
              v-for="relation in runtimeRelations"
              :key="relation.relationId"
              class="relation-runtime-item"
              :class="{ 'item-openable': relation.canOpen, 'item-disabled': !relation.canOpen }"
              @click="openRelation(relation)"
            >
              <div class="runtime-icon">
                <NIcon :size="22">
                  <LinkOutline v-if="relation.canOpen" />
                  <AlertCircleOutline v-else />
                </NIcon>
              </div>
              <div class="runtime-content">
                <div class="runtime-header">
                  <div>
                    <span class="runtime-name">{{ relation.relationName }}</span>
                    <p>{{ relation.message || relation.targetObjectName || '关联对象' }}</p>
                  </div>
                  <NTag :type="relation.canOpen ? 'success' : 'warning'" size="small" round>
                    {{ relation.canOpen ? '可进入' : '待配置' }}
                  </NTag>
                </div>
                <div class="runtime-meta">
                  <span>{{ relation.sourceObjectName || relation.sourceObjectCode }}</span>
                  <span>→</span>
                  <span>{{ relation.targetObjectName || relation.targetObjectCode }}</span>
                </div>
              </div>
              <NButton
                v-if="relation.canOpen"
                secondary
                type="primary"
                size="small"
                class="runtime-action"
                @click.stop="openRelation(relation)"
              >
                {{ relation.nextActionLabel || `查看${relation.targetObjectName || '数据'}` }}
              </NButton>
              <NButton
                v-else-if="relation.nextAction"
                secondary
                type="warning"
                size="small"
                class="runtime-action"
                @click.stop="handleAction(relation)"
              >
                {{ relation.nextActionLabel || '去配置' }}
              </NButton>
            </div>
          </div>
          <NEmpty v-else-if="!loadingRuntime" description="暂无关联入口" />
        </NSpin>
      </NTabPane>

      <NTabPane name="config" tab="关系配置">
        <NSpin :show="loading">
          <div v-if="relations.length" class="relation-list">
            <div v-for="relation in relations" :key="relation.id" class="relation-row">
              <div class="relation-main">
                <DictTag dict-type="ai_business_relation_type" :value="relation.relationType" :bordered="false" />
                <strong>{{ relation.relationName }}</strong>
                <span>{{ relation.sourceObjectName || relation.sourceObjectCode }} → {{ relation.targetObjectName || relation.targetObjectCode }}</span>
              </div>
              <p>{{ relation.description || relation.relationConfig || '对象关系配置' }}</p>
            </div>
          </div>
          <NEmpty v-else-if="!loading" description="暂无对象关系配置" />
        </NSpin>
      </NTabPane>
    </NTabs>
  </div>
</template>

<script setup>
import {
  AlertCircleOutline,
  LinkOutline,
} from '@vicons/ionicons5'
import { NButton, NEmpty, NIcon, NSpin, NTabPane, NTabs, NTag } from 'naive-ui'
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { businessObjectRelationRuntime, businessObjectRelations } from '@/api/business-app'
import DictTag from '@/components/DictTag.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
})

const emit = defineEmits(['action'])

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadingRuntime = ref(false)
const relations = ref([])
const runtimeRelations = ref([])

watch(() => props.objectId, (newVal) => {
  if (newVal) {
    loadRelations()
    loadRuntimeRelations()
  }
  else {
    relations.value = []
    runtimeRelations.value = []
  }
}, { immediate: true })

async function loadRelations() {
  if (!props.objectId) {
    relations.value = []
    return
  }
  loading.value = true
  try {
    const res = await businessObjectRelations(props.objectId)
    relations.value = res.data || []
  }
  finally {
    loading.value = false
  }
}

async function loadRuntimeRelations() {
  if (!props.objectId) {
    runtimeRelations.value = []
    return
  }
  loadingRuntime.value = true
  try {
    const res = await businessObjectRelationRuntime(props.objectId)
    runtimeRelations.value = res.data || []
  }
  catch (error) {
    console.error('加载关系运行入口失败:', error)
    runtimeRelations.value = []
  }
  finally {
    loadingRuntime.value = false
  }
}

function openRelation(relation) {
  if (!relation.canOpen)
    return

  const targetPath = resolveRelationTargetPath(relation)
  if (!targetPath) {
    handleAction({ ...relation, nextAction: 'PUBLISH_APP' })
    return
  }

  if (/^https?:\/\//i.test(targetPath)) {
    window.open(targetPath, '_blank', 'noopener,noreferrer')
    return
  }

  router.push({
    path: targetPath,
    query: buildRelationQuery(relation),
  })
}

function resolveRelationTargetPath(relation) {
  if (relation.targetUrl)
    return relation.targetUrl
  if (relation.targetConfigKey)
    return `/ai/crud-page/${encodeURIComponent(relation.targetConfigKey)}`
  if (relation.targetObjectCode)
    return `/app-center/object/${relation.targetObjectCode}`
  return ''
}

function buildRelationQuery(relation) {
  const query = {}
  if (route.query.suiteCode)
    query.suiteCode = route.query.suiteCode
  if (relation.relationId)
    query.relationId = relation.relationId
  if (relation.defaultFilter)
    query.defaultFilter = relation.defaultFilter
  return query
}

function handleAction(relation) {
  if (relation.nextAction)
    emit('action', relation.nextAction, relation)
}
</script>

<style scoped>
.relation-panel {
  min-height: 220px;
}

.panel-intro {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px 14px;
}

.panel-intro h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  line-height: 1.4;
}

.panel-intro p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.relation-runtime-list {
  display: grid;
  gap: 10px;
}

.relation-runtime-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.relation-runtime-item.item-openable {
  cursor: pointer;
}

.relation-runtime-item.item-openable:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.relation-runtime-item.item-disabled {
  background: #fffdf4;
}

.runtime-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
}

.relation-runtime-item.item-disabled .runtime-icon {
  background: #fef3c7;
  color: #d97706;
}

.runtime-content {
  min-width: 0;
}

.runtime-header {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
  min-width: 0;
}

.runtime-name {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.runtime-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.runtime-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.runtime-action {
  min-width: 92px;
}

.relation-list {
  display: grid;
  gap: 10px;
}

.relation-row {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.relation-main {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.relation-main strong {
  color: #111827;
  font-size: 14px;
}

.relation-main span {
  color: #6b7280;
  font-size: 13px;
}

.relation-row p {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 680px) {
  .relation-runtime-item {
    grid-template-columns: 40px minmax(0, 1fr);
  }

  .runtime-action {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>
