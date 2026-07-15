<template>
  <n-drawer :show="show" :width="720" placement="right" @update:show="emit('update:show', $event)">
    <n-drawer-content :title="versionTitle" closable>
      <n-spin :show="loading">
        <div v-if="detail" class="version-detail">
          <div class="version-meta">
            <div><span>版本状态</span><DictTag dict-type="ai_business_application_publish_status" :value="detail.publishStatus" /></div>
            <div><span>发布时间</span><strong>{{ detail.publishedTime || '-' }}</strong></div>
            <div><span>发布人</span><strong>{{ detail.publishedBy || '-' }}</strong></div>
            <div><span>快照摘要</span><code>{{ detail.snapshotHash || '-' }}</code></div>
          </div>

          <n-alert
            v-if="detail.publishStatus === 'ROLLBACK'"
            type="warning"
            :bordered="false"
            title="回滚发布版本"
          >
            该版本从 v{{ detail.sourceVersionNo }} 恢复生成；数据库结构和业务数据未执行反向回滚。
          </n-alert>

          <section class="asset-section">
            <header><h3>业务对象</h3><span>{{ objects.length }} 项</span></header>
            <div class="asset-table">
              <div v-for="item in objects" :key="item.objectId" class="asset-row object-row">
                <strong>{{ item.objectName || item.objectCode }}</strong>
                <code>{{ item.objectCode }}</code>
                <DictTag dict-type="ai_business_application_object_role" :value="item.objectRole" :bordered="false" />
                <span>{{ item.tableName || '未绑定物理表' }}</span>
              </div>
              <n-empty v-if="!objects.length" size="small" description="该版本没有业务对象快照" />
            </div>
          </section>

          <section class="asset-section">
            <header><h3>页面入口</h3><span>{{ entries.length }} 项</span></header>
            <div class="asset-table">
              <div v-for="item in entries" :key="item.id" class="asset-row entry-row">
                <strong :title="entryDisplayName(item)">{{ entryDisplayName(item) }}</strong>
                <span>{{ item.objectName || '应用级入口' }}</span>
                <DictTag dict-type="ai_business_app_entry_mode" :value="item.entryMode" :bordered="false" />
                <DictTag dict-type="sys_enable_disable" :value="item.status" :bordered="false" />
              </div>
              <n-empty v-if="!entries.length" size="small" description="该版本没有页面入口快照" />
            </div>
          </section>

          <section class="asset-section">
            <header><h3>业务扩展</h3><span>{{ extensions.length }} 项</span></header>
            <div class="asset-table">
              <div v-for="item in extensions" :key="item.id" class="asset-row extension-row">
                <strong>{{ item.extensionName || item.extensionCode }}</strong>
                <code>{{ item.extensionCode }}</code>
                <DictTag dict-type="ai_business_extension_type" :value="item.extensionType" :bordered="false" />
                <span>v{{ item.enabledVersion || item.releaseVersion || '-' }}</span>
              </div>
              <n-empty v-if="!extensions.length" size="small" description="该版本没有扩展快照" />
            </div>
          </section>
        </div>
      </n-spin>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { businessApplicationVersionDetail } from '@/api/business-application'

const props = defineProps({
  show: Boolean,
  applicationId: {
    type: [String, Number],
    default: null,
  },
  versionNo: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['update:show'])
const loading = ref(false)
const detail = ref(null)

const versionTitle = computed(() => props.versionNo ? `应用版本 v${props.versionNo}` : '应用版本详情')
const objects = computed(() => detail.value?.snapshot?.objects || [])
const entries = computed(() => detail.value?.snapshot?.entries || [])
const extensions = computed(() => detail.value?.snapshot?.extensions || [])

watch(() => [props.show, props.applicationId, props.versionNo], loadDetail, { immediate: true })

async function loadDetail() {
  if (!props.show || !props.applicationId || !props.versionNo)
    return
  loading.value = true
  try {
    const response = await businessApplicationVersionDetail(props.applicationId, props.versionNo)
    detail.value = response.data || null
  }
  finally {
    loading.value = false
  }
}

function entryDisplayName(item = {}) {
  const appName = String(item.appName || '').trim()
  const appCode = String(item.appCode || '').trim()
  const technicalName = !appName || appName === appCode || /^[A-Z][A-Z0-9_]*$/.test(appName)
  if (!technicalName)
    return appName
  if (item.objectName)
    return `${item.objectName}入口`
  return '业务访问入口'
}
</script>

<style scoped>
.version-detail,
.asset-section {
  display: grid;
  gap: 14px;
}

.version-detail {
  gap: 20px;
}

.version-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.version-meta > div {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  align-items: center;
  min-height: 42px;
  padding: 7px 10px;
  border-right: 1px solid var(--border-light, #e5e6eb);
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.version-meta > div:nth-child(2n) {
  border-right: 0;
}

.version-meta > div:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.version-meta span,
.asset-section header span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.version-meta strong,
.version-meta code {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-section header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.asset-section h3 {
  margin: 0;
  font-size: 14px;
}

.asset-table {
  overflow: hidden;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.asset-row {
  display: grid;
  gap: 10px;
  align-items: center;
  min-height: 42px;
  padding: 7px 10px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.asset-row:last-child {
  border-bottom: 0;
}

.object-row,
.entry-row,
.extension-row {
  grid-template-columns: minmax(130px, 1.2fr) minmax(110px, 1fr) 100px minmax(120px, 1fr);
}

.asset-row strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-row code,
.asset-row span {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-table :deep(.n-empty) {
  padding: 24px;
}

@media (max-width: 680px) {
  .version-meta {
    grid-template-columns: 1fr;
  }

  .version-meta > div {
    border-right: 0;
    border-bottom: 1px solid var(--border-light, #e5e6eb) !important;
  }

  .version-meta > div:last-child {
    border-bottom: 0 !important;
  }
}
</style>
