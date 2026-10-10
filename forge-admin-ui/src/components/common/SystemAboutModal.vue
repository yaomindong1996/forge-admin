<template>
  <!-- 所有布局共用一个入口弹窗；后端不可用时仍可查看真实前端构建信息。 -->
  <NModal
    :show="store.visible" preset="card" title="关于系统" class="system-about-modal"
    style="width: min(660px, calc(100vw - 32px))" :segmented="{ content: true, footer: true }"
    @update:show="!$event && store.close()"
  >
    <div class="system-about-content">
      <!-- 主版本与可读状态 -->
      <header class="version-heading">
        <div class="version-emblem" aria-hidden="true">
          <ForgeSymbol name="layers" :size="32" />
        </div>
        <div class="version-heading__content">
          <span class="version-label">当前系统</span>
          <div class="version-heading__value">
            <strong>{{ store.backend?.version ? `v${store.backend.version}` : '版本待确认' }}</strong>
            <DictTag v-if="store.backend?.edition" dict-type="sys_plugin_edition" :value="store.backend.edition" />
          </div>
          <p>系统版本与构建档案</p>
        </div>
        <ForgeSymbol class="version-heading__motif" name="module" :size="60" />
      </header>
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
        <div class="version-sections">
          <section v-for="(section, index) in sections" :key="section.title" class="version-section">
            <h3><ForgeSymbol :name="index === 0 ? 'platform' : 'monitor'" :size="17" />{{ section.title }}</h3>
            <dl>
              <div v-for="row in section.rows" :key="row.label" class="version-row">
                <dt>{{ row.label }}</dt><dd>{{ row.value }}</dd>
              </div>
            </dl>
          </section>
        </div>
      </NSpin>
      <p class="version-hint">
        <i class="i-lucide:info" aria-hidden="true" />
        时间按当前设备时区显示；此页面不执行在线升级。
      </p>

      <!-- 随包说明只作纯文本显示，不能执行日志中的 HTML。 -->
      <NCollapse class="version-release">
        <NCollapseItem title="本版本说明（前端随附）" name="notes">
          <pre class="version-notes">{{ store.local.notes || '此构建未附带当前版本说明。' }}</pre>
        </NCollapseItem>
      </NCollapse>
    </div>
    <template #footer>
      <div class="version-actions">
        <NButton size="small" :loading="store.loading" @click="store.refresh()">
          <template #icon>
            <i class="i-lucide:refresh-cw" />
          </template>
          刷新信息
        </NButton>
        <NButton size="small" type="primary" :disabled="store.loading" @click="copyVersion">
          <template #icon>
            <i class="i-lucide:copy" />
          </template>
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
import ForgeSymbol from './ForgeSymbol.vue'

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
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 18%, var(--border-light));
  border-radius: 6px;
  background: color-mix(in srgb, var(--primary-color) 4%, var(--bg-primary));
}
.version-emblem {
  display: grid;
  place-items: center;
  width: 52px;
  height: 56px;
  color: var(--primary-color);
}
.version-heading__content {
  flex: 1;
  min-width: 0;
}
.version-heading__value {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 3px 0;
}
.version-heading__content p {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
}
.version-heading__motif {
  color: var(--primary-color);
  opacity: 0.15;
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
.version-sections {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.version-section {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
}
h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 500;
}
h3 .forge-symbol {
  color: var(--primary-color);
}
dl,
dd,
.version-hint {
  margin: 0;
}
.version-row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 8px;
  padding: 6px 0;
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
.version-hint {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
}
.version-hint i {
  flex: none;
}
.version-release {
  padding: 12px;
  border-radius: 4px;
  background: var(--bg-secondary);
}
@media (max-width: 600px) {
  .version-sections {
    grid-template-columns: minmax(0, 1fr);
  }
  .version-heading {
    padding: 12px;
  }
  .version-heading__motif {
    display: none;
  }
}
</style>
