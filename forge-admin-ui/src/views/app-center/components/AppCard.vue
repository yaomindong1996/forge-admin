<template>
  <article class="app-card">
    <div class="app-icon">
      <n-icon><AppsOutline /></n-icon>
    </div>
    <div class="app-main">
      <div class="app-title-line">
        <h3>{{ app.appName || app.appCode }}</h3>
        <DictTag dict-type="sys_enable_disable" :value="app.status" :bordered="false" />
      </div>
      <p>{{ app.description || '业务应用入口' }}</p>
      <div class="app-tags">
        <DictTag dict-type="ai_business_app_type" :value="app.appType" :bordered="false" />
        <DictTag dict-type="ai_business_app_entry_mode" :value="app.entryMode" :bordered="false" />
        <span v-if="app.objectName" class="object-chip">{{ app.objectName }}</span>
        <span class="binding-chip">{{ app.bindingCount || 0 }} 能力</span>
      </div>
    </div>
    <div class="app-actions">
      <n-space :size="6">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button quaternary circle size="small" :disabled="isOpenDisabled(app)" @click="emit('open', app)">
              <template #icon>
                <n-icon><OpenOutline /></n-icon>
              </template>
            </n-button>
          </template>
          {{ openTip(app) }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button quaternary circle size="small" @click="emit('config', app)">
              <template #icon>
                <n-icon><SettingsOutline /></n-icon>
              </template>
            </n-button>
          </template>
          配置
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button quaternary circle size="small" @click="emit('toggle', app)">
              <template #icon>
                <n-icon><PowerOutline /></n-icon>
              </template>
            </n-button>
          </template>
          {{ app.status === 1 ? '停用' : '启用' }}
        </n-tooltip>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button quaternary circle size="small" @click="emit('delete', app)">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
            </n-button>
          </template>
          删除
        </n-tooltip>
      </n-space>
    </div>
  </article>
</template>

<script setup>
import { AppsOutline, OpenOutline, PowerOutline, SettingsOutline, TrashOutline } from '@vicons/ionicons5'
import DictTag from '@/components/DictTag.vue'

defineProps({
  app: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['open', 'config', 'toggle', 'delete'])

function isOpenDisabled(app) {
  if (app.status !== 1)
    return true
  if (app.entryMode === 'RUNTIME')
    return !app.configKey && !app.entryUrl
  return !app.entryUrl
}

function openTip(app) {
  if (app.status !== 1)
    return '入口已停用'
  if (isOpenDisabled(app))
    return '未配置打开地址'
  return '打开'
}
</script>

<style scoped>
.app-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  min-height: 104px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease;
}

.app-card:hover {
  border-color: #16a34a;
  box-shadow: 0 10px 24px rgb(15 23 42 / 8%);
}

.app-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  background: #ecfdf5;
  color: #16a34a;
  font-size: 22px;
}

.app-main {
  min-width: 0;
}

.app-title-line {
  display: flex;
  min-width: 0;
  gap: 10px;
  align-items: center;
}

.app-title-line h3 {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: #111827;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-main p {
  display: -webkit-box;
  margin: 6px 0 0;
  overflow: hidden;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.app-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.object-chip,
.binding-chip {
  display: inline-flex;
  max-width: 160px;
  align-items: center;
  overflow: hidden;
  border-radius: 4px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
  line-height: 22px;
  padding: 0 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.binding-chip {
  background: #ecfdf5;
  color: #15803d;
}

.app-actions {
  justify-self: end;
}

@media (max-width: 520px) {
  .app-card {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .app-actions {
    grid-column: 2;
  }
}
</style>
