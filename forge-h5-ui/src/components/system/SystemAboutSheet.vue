<template>
  <!-- 复用 Wot 弹层，长提交号和发行说明可换行、滚动，不挤压底部操作。 -->
  <AiPopupSheet
    :model-value="store.visible" title="关于系统" description="当前系统与前端构建信息"
    max-height="88vh" body-max-height="calc(88vh - 180px - env(safe-area-inset-bottom))"
    @update:model-value="!$event && store.close()"
  >
    <view class="version-sheet">
      <!-- 版本摘要 -->
      <view class="version-summary">
        <text class="version-number">
          {{ store.backend?.version ? `v${store.backend.version}` : '版本待确认' }}
        </text>
        <text v-if="editionLabel" class="version-edition">{{ editionLabel }}</text>
      </view>
      <text v-if="store.loading" class="version-note">正在读取后端版本…</text>
      <text v-if="store.error || store.warning" class="version-warning">{{ store.error || store.warning }}</text>
      <text v-else-if="store.backend && !store.backend.version" class="version-note">
        后端未生成构建信息，请管理员重新构建服务。核心版本不等同于系统发行版本。
      </text>
      <!-- 后端与前端分别展示，不以本地版本兜底接口失败。 -->
      <view v-for="section in sections" :key="section.title" class="version-section">
        <text class="version-section-title">{{ section.title }}</text>
        <view v-for="row in section.rows" :key="row.label" class="version-row">
          <text class="version-label">{{ row.label }}</text>
          <text class="version-value" selectable>{{ row.value }}</text>
        </view>
      </view>
      <text class="version-note">时间按当前设备时区显示；此页面不执行在线升级。</text>
      <wd-collapse v-model="expanded">
        <wd-collapse-item title="本版本说明（前端随附）" name="notes">
          <text class="version-notes" selectable>
            {{ store.local.notes || '此构建未附带当前版本说明。' }}
          </text>
        </wd-collapse-item>
      </wd-collapse>
    </view>
    <template #footer>
      <view class="version-actions">
        <AiButton size="sm" variant="secondary" :loading="store.loading" @click="store.refresh()">
          刷新信息
        </AiButton>
        <AiButton size="sm" :disabled="store.loading" @click="copyVersion">复制版本信息</AiButton>
      </view>
    </template>
  </AiPopupSheet>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { versionDiagnostics, versionSections } from '../../../../scripts/forge-shared/version-view.mjs'
import api from '@/api'
import AiButton from '@/components/AiButton.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import { useSystemVersionStore } from '@/store/modules/version'
import { toast } from '@/utils/notify'

const store = useSystemVersionStore()
const expanded = ref([])
const editions = ref([])
const sections = computed(() => versionSections(store.local, store.backend))
const editionLabel = computed(() => {
  const code = store.backend?.edition
  return editions.value.find(item => item.dictValue === code)?.dictLabel || code || ''
})
onMounted(async () => {
  try {
    const response = await api.getDictOptions('sys_plugin_edition')
    if (response.code === 200 && Array.isArray(response.data)) editions.value = response.data
  }
  catch { /* 字典不可用时保留服务端原始编码，不阻断版本查询。 */ }
})
function copyVersion() {
  uni.setClipboardData({
    data: versionDiagnostics(store.local, store.backend),
    success: () => toast('版本信息已复制', { type: 'success' }),
    fail: () => toast('复制失败，请长按版本信息复制', { type: 'error' }),
  })
}
</script>

<style scoped>
.version-sheet { display: flex; flex-direction: column; gap: 14px; }
.version-summary { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.version-number { color: var(--forge-text-primary); font-size: 20px; font-weight: 500; }
.version-edition, .version-note { color: var(--forge-text-tertiary); font-size: 12px; line-height: 1.6; }
.version-warning { padding: 10px; border-radius: 6px; background: var(--forge-surface-muted); }
.version-warning { color: var(--forge-text-secondary); font-size: 13px; line-height: 1.6; }
.version-section { padding-top: 12px; border-top: 1px solid var(--forge-border, #e5e6eb); }
.version-section-title { display: block; margin-bottom: 8px; color: var(--forge-text-primary); font-size: 14px; }
.version-row { display: flex; gap: 12px; padding: 5px 0; font-size: 13px; line-height: 1.5; }
.version-label { flex: 0 0 68px; color: var(--forge-text-tertiary); }
.version-value { flex: 1; min-width: 0; overflow-wrap: anywhere; color: var(--forge-text-primary); }
.version-notes { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 13px; line-height: 1.6; }
.version-actions { display: flex; gap: 12px; }
.version-actions > * { flex: 1; min-width: 0; }
</style>
