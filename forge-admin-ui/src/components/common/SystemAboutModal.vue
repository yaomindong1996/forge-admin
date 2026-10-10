<template>
  <!-- 所有布局共用一个入口弹窗；后端不可用时仍可查看真实前端构建信息。 -->
  <NModal
    :show="store.visible" preset="card" title="关于系统" class="system-about-modal"
    style="width: min(560px, calc(100vw - 32px))" :segmented="{ content: true, footer: true }"
    @update:show="!$event && store.close()"
  >
    <div class="system-about-content">
      <!-- 主版本与可读状态 -->
      <div class="version-heading">
        <span class="version-label">当前系统</span>
        <strong>{{ store.backend?.version ? `v${store.backend.version}` : '版本待确认' }}</strong>
        <DictTag v-if="store.backend?.edition" dict-type="sys_plugin_edition" :value="store.backend.edition" />
      </div>
      <NAlert v-if="store.error" type="warning" :show-icon="true">
        {{ store.error }}
      </NAlert>
      <NAlert v-else-if="store.warning" type="warning" :show-icon="true">
        {{ store.warning }}
      </NAlert>
      <NAlert v-else-if="store.backend && !store.backend.version" type="info">
        后端尚未生成构建信息，管理员可重新构建后查看。
        框架核心版本不等同于系统发行版本。
      </NAlert>

      <!-- 构建信息：日期为当前设备时区，不将构建时间描述为部署时间。 -->
      <NSpin :show="store.loading" description="读取版本信息">
        <section v-for="section in sections" :key="section.title" class="version-section">
          <h3>{{ section.title }}</h3>
          <dl>
            <div v-for="row in section.rows" :key="row.label" class="version-row">
              <dt>{{ row.label }}</dt><dd>{{ row.value }}</dd>
            </div>
          </dl>
        </section>
      </NSpin>
      <p class="version-hint">
        时间按当前设备时区显示；此页面不执行在线升级。
      </p>

      <!-- 随包说明只作纯文本显示，不能执行日志中的 HTML。 -->
      <NCollapse>
        <NCollapseItem title="本版本说明（前端随附）" name="notes">
          <pre class="version-notes">{{ store.local.notes || '此构建未附带当前版本说明。' }}</pre>
        </NCollapseItem>
      </NCollapse>
    </div>
    <template #footer>
      <div class="version-actions">
        <NButton size="small" :loading="store.loading" @click="store.refresh()">
          刷新信息
        </NButton>
        <NButton size="small" type="primary" :disabled="store.loading" @click="copyVersion">
          复制版本信息
        </NButton>
      </div>
    </template>
  </NModal>
</template>

<script setup>
import { NAlert, NButton, NCollapse, NCollapseItem, NModal, NSpin } from 'naive-ui'
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useSystemVersionStore } from '@/stores/system/versionStore'
import { copyText } from '@/utils/tab-interactions'
import { versionDiagnostics, versionSections } from '../../../../scripts/forge-shared/version-view.mjs'

const store = useSystemVersionStore()
const sections = computed(() => versionSections(store.local, store.backend))
function copyVersion() {
  return copyText(versionDiagnostics(store.local, store.backend), '版本信息已复制')
}
</script>

<style scoped>
.system-about-content {
  display: grid;
  gap: 16px;
  max-height: 65vh;
  overflow-y: auto;
}
.version-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.version-heading strong {
  font-size: 20px;
  font-weight: 500;
  color: var(--text-primary);
}
.version-label,
dt,
.version-hint {
  color: var(--text-secondary);
  font-size: 13px;
}
.version-section + .version-section {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
h3 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 500;
}
dl,
dd,
.version-hint {
  margin: 0;
}
.version-row {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 12px;
  padding: 4px 0;
}
dd {
  font-size: 13px;
  overflow-wrap: anywhere;
}
.version-notes {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font: inherit;
  font-size: 13px;
}
.version-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
