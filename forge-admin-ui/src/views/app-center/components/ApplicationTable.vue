<template>
  <div class="application-card-grid" role="list" aria-label="业务应用列表">
    <article
      v-for="application in applications"
      :key="application.id"
      class="application-card"
      :class="{ 'has-problem': Number(application.problemCount || 0) > 0 }"
      role="listitem"
      tabindex="0"
      @click="emit('enter', application)"
      @keydown.enter.self.prevent="emit('enter', application)"
    >
      <header class="application-card-head">
        <span class="application-icon" :style="applicationIconStyle(application)">
          <IconRenderer v-if="application.icon" :icon="application.icon" :size="18" />
          <n-icon v-else><AppsOutline /></n-icon>
        </span>
        <span class="application-copy">
          <strong>{{ application.applicationName || application.applicationCode }}</strong>
          <code>{{ application.applicationCode }}</code>
        </span>
        <span
          class="application-design-status"
          :class="{ 'is-draft': isDraftApplication(application) }"
        >
          <DictTag
            dict-type="ai_business_application_design_status"
            :value="application.designStatus"
            :bordered="false"
          />
        </span>
      </header>

      <p class="application-description">
        {{ application.description || '尚未补充应用说明' }}
      </p>

      <div class="application-meta">
        <span :title="application.suiteCode">
          {{ application.suiteName || application.suiteCode || '未关联业务域' }}
        </span>
        <span>更新 {{ formatDate(application.updateTime) }}</span>
      </div>

      <div class="application-assets" aria-label="应用资产统计">
        <span><strong>{{ application.objectCount || 0 }}</strong><small>对象</small></span>
        <span><strong>{{ application.entryCount || 0 }}</strong><small>入口</small></span>
        <span><strong>{{ application.flowCount || 0 }}</strong><small>流程</small></span>
        <span><strong>{{ application.extensionCount || 0 }}</strong><small>扩展</small></span>
      </div>

      <footer class="application-card-foot">
        <div class="application-runtime-state">
          <DictTag dict-type="sys_enable_disable" :value="application.status" :bordered="false" />
          <span v-if="Number(application.problemCount || 0) > 0" class="problem-text">
            {{ application.problemCount }} 项待处理
          </span>
          <span v-else-if="application.lastPublishVersion">
            v{{ application.lastPublishVersion }}
          </span>
          <span v-else>尚未发布</span>
        </div>

        <div class="application-actions" @click.stop>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button
                quaternary
                circle
                size="small"
                class="action-icon-button enter-application-button"
                aria-label="打开应用工作台"
                @click="emit('enter', application)"
              >
                <template #icon>
                  <n-icon><OpenOutline /></n-icon>
                </template>
              </n-button>
            </template>
            打开应用工作台
          </n-tooltip>
          <n-tooltip v-if="isDraftApplication(application)" trigger="hover">
            <template #trigger>
              <n-button
                quaternary
                circle
                type="primary"
                size="small"
                class="action-icon-button"
                aria-label="发布应用"
                @click="emit('publish', application)"
              >
                <template #icon>
                  <n-icon><RocketOutline /></n-icon>
                </template>
              </n-button>
            </template>
            发布应用
          </n-tooltip>
          <n-dropdown
            trigger="click"
            :options="actionOptions(application)"
            @select="key => handleAction(key, application)"
          >
            <n-button quaternary circle size="small" aria-label="更多应用操作">
              <template #icon>
                <n-icon><EllipsisHorizontal /></n-icon>
              </template>
            </n-button>
          </n-dropdown>
        </div>
      </footer>
    </article>
  </div>
</template>

<script setup>
import { AppsOutline, EllipsisHorizontal, OpenOutline, RocketOutline } from '@vicons/ionicons5'
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'

defineProps({
  applications: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['enter', 'edit', 'code', 'publish', 'toggle', 'delete'])
const APPLICATION_ICON_HUES = [171, 28, 262, 340, 198, 86, 221, 12]

function applicationIconStyle(application) {
  const identity = application?.applicationCode || application?.applicationName || application?.id || 'application'
  let hash = 0
  const text = String(identity)
  for (let index = 0; index < text.length; index += 1)
    hash = (hash * 31 + text.charCodeAt(index)) % 2147483647
  return { '--application-icon-hue': String(APPLICATION_ICON_HUES[hash % APPLICATION_ICON_HUES.length]) }
}

function actionOptions(application) {
  return [
    { label: '预览与下载代码', key: 'code' },
    { label: '编辑应用', key: 'edit' },
    { label: Number(application.status) === 1 ? '停用应用' : '启用应用', key: 'toggle' },
    { type: 'divider', key: 'divider' },
    { label: '删除应用', key: 'delete' },
  ]
}

function isDraftApplication(application) {
  return String(application?.designStatus || '').toUpperCase() === 'DRAFT'
}

function handleAction(key, application) {
  if (key === 'code')
    emit('code', application)
  else if (key === 'edit')
    emit('edit', application)
  else if (key === 'toggle')
    emit('toggle', application)
  else if (key === 'delete')
    emit('delete', application)
}

function formatDate(value) {
  if (!value)
    return '-'
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime()))
    return String(value)
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}
</script>

<style scoped>
.application-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 268px), 1fr));
  align-content: start;
  gap: 10px;
  min-width: 0;
  min-height: 100%;
  padding: 12px;
}

.application-card {
  display: grid;
  grid-template-rows: auto auto auto auto auto;
  gap: 6px;
  min-width: 0;
  min-height: 166px;
  padding: 10px 11px 8px;
  border: 1px solid var(--n-border-color, var(--border-default, #c9cdd4));
  border-radius: 7px;
  outline: none;
  color: var(--n-text-color, var(--text-primary, #1d2129));
  background: var(--n-color, var(--bg-primary, #fff));
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.application-card:hover,
.application-card:focus-visible {
  border-color: color-mix(
    in srgb,
    var(--n-primary-color, var(--primary-color, #165dff)) 55%,
    var(--n-border-color, #c9cdd4)
  );
  box-shadow: 0 5px 14px rgb(29 33 41 / 8%);
  transform: translateY(-1px);
}

.application-card.has-problem {
  border-top-color: var(--warning-color, #ff7d00);
}

.application-card-head {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.application-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid hsl(var(--application-icon-hue, 171) 24% 85%);
  border-radius: 7px;
  color: hsl(var(--application-icon-hue, 171) 34% 39%);
  background: hsl(var(--application-icon-hue, 171) 28% 95%);
}

.application-copy {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.application-copy strong,
.application-copy code,
.application-description,
.application-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.application-copy strong {
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 13px;
  font-weight: 650;
}

.application-copy code {
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10px;
}

.application-card-head :deep(.n-tag) {
  max-width: 72px;
}

.application-design-status.is-draft :deep(.n-tag) {
  color: var(--warning-color, #d46b08);
  background-color: color-mix(in srgb, var(--warning-color, #ff7d00) 14%, transparent);
}

.application-description {
  margin: 0;
  color: var(--n-text-color-2, var(--text-secondary, #4e5969));
  font-size: 11px;
  line-height: 18px;
}

.application-meta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  font-size: 10px;
}

.application-assets {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--n-border-color, var(--border-light, #e5e6eb));
  border-radius: 6px;
  background: var(--n-color-embedded, var(--bg-secondary, #f7f8fa));
}

.application-assets span {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 3px;
  min-width: 0;
  padding: 5px 3px;
  border-right: 1px solid var(--n-border-color, var(--border-light, #e5e6eb));
}

.application-assets span:last-child {
  border-right: 0;
}

.application-assets strong {
  color: var(--n-text-color, var(--text-primary, #1d2129));
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.application-assets small {
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  font-size: 10px;
}

.application-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  padding-top: 1px;
}

.application-runtime-state,
.application-actions {
  display: flex;
  align-items: center;
  min-width: 0;
}

.application-runtime-state {
  gap: 6px;
  overflow: hidden;
  color: var(--n-text-color-3, var(--text-tertiary, #86909c));
  font-size: 10px;
  white-space: nowrap;
}

.problem-text {
  overflow: hidden;
  color: var(--warning-color, #d46b08);
  text-overflow: ellipsis;
}

.application-actions {
  flex: 0 0 auto;
  justify-content: flex-end;
  gap: 2px;
}

.enter-application-button {
  color: var(--n-text-color, var(--text-primary, #1d2129));
}

:global(.dark) .application-icon {
  border-color: hsl(var(--application-icon-hue, 171) 18% 34%);
  color: hsl(var(--application-icon-hue, 171) 35% 70%);
  background: hsl(var(--application-icon-hue, 171) 20% 21%);
}

@media (max-width: 620px) {
  .application-card-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 8px;
  }

  .application-card {
    min-height: 160px;
  }
}
</style>
