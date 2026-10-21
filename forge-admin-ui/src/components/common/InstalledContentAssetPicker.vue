<script setup>
import { NButton, NEmpty, NInput, NModal, NPagination, NSpin } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { contentAssetPage, removeContentAsset } from '@/api/contentAsset'
import ContentPackageImportDialog from '@/components/common/ContentPackageImportDialog.vue'
import {
  CONTENT_ASSET_KIND_LABEL,
  installContentPackageFromFile,
  loadInstalledContentPackage,
} from '@/utils/contentInstalledAsset'
import { officialMarketUrl } from '@/utils/officialMarket'

const props = defineProps({
  show: { type: Boolean, default: false },
  kind: { type: String, required: true },
  /** 选用按钮文案 */
  selectLabel: { type: String, default: '选用' },
  title: { type: String, default: '' },
})
const emit = defineEmits(['update:show', 'select', 'installed'])

const loading = ref(false)
const removingId = ref(null)
const selectingId = ref(null)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)
const items = ref([])
const installOpen = ref(false)
const installing = ref(false)

const kindLabel = computed(() => CONTENT_ASSET_KIND_LABEL[props.kind] || '模板')
const modalTitle = computed(() => props.title || `我的${kindLabel.value}`)
const marketHref = computed(() => officialMarketUrl(props.kind))
const kindIcon = computed(() => ({
  print: 'i-lucide:file-text',
  form: 'i-lucide:panel-top',
  'print-component': 'i-lucide:puzzle',
}[props.kind] || 'i-lucide:package'))
const kindTone = computed(() => ({
  print: { ink: '#1d4ed8', wash: '#dbeafe', border: '#bfdbfe' },
  form: { ink: '#0f766e', wash: '#ccfbf1', border: '#99f6e4' },
  'print-component': { ink: '#7c3aed', wash: '#ede9fe', border: '#ddd6fe' },
}[props.kind] || { ink: '#2563eb', wash: '#eff6ff', border: '#dbeafe' }))
const kindHint = computed(() => ({
  print: '整页单据版式，选用后会替换当前设计稿。',
  form: '表单结构包，可应用到当前表单设计。',
  'print-component': '审批章、签章等画布片段，选用后插入当前页。',
}[props.kind] || '从市场安装一次后可反复选用。'))

watch(() => props.show, async (visible) => {
  if (!visible)
    return
  keyword.value = ''
  pageNum.value = 1
  await refresh()
})

async function refresh() {
  loading.value = true
  try {
    const { data } = await contentAssetPage({
      kind: props.kind,
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize,
    })
    items.value = data?.records || data?.list || []
    total.value = Number(data?.total || 0)
  }
  catch (error) {
    items.value = []
    total.value = 0
    window.$message?.error(error.message || '加载我的模板失败')
  }
  finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  refresh()
}

async function onInstall({ file }) {
  installing.value = true
  try {
    const result = await installContentPackageFromFile({ file, kind: props.kind })
    window.$message?.success(`已安装到我的${kindLabel.value}`)
    installOpen.value = false
    emit('installed', result)
    await refresh()
  }
  catch (error) {
    window.$message?.error(error.message || '安装失败')
  }
  finally {
    installing.value = false
  }
}

async function onSelect(row) {
  if (!row?.id || selectingId.value)
    return
  selectingId.value = row.id
  try {
    const result = await loadInstalledContentPackage(row.id)
    emit('select', result)
    emit('update:show', false)
  }
  catch (error) {
    window.$message?.error(error.message || '读取模板失败')
  }
  finally {
    selectingId.value = null
  }
}

async function onRemove(row) {
  if (!row?.id || removingId.value)
    return
  removingId.value = row.id
  try {
    await removeContentAsset(row.id)
    window.$message?.success('已从我的模板中移除')
    if (items.value.length <= 1 && pageNum.value > 1)
      pageNum.value -= 1
    await refresh()
  }
  catch (error) {
    window.$message?.error(error.message || '移除失败')
  }
  finally {
    removingId.value = null
  }
}

function formatTime(value) {
  if (!value)
    return ''
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <NModal
    :show="show"
    preset="card"
    :title="modalTitle"
    :style="{ width: 'min(920px, 94vw)' }"
    :mask-closable="!installing && !selectingId"
    :closable="!installing && !selectingId"
    @update:show="emit('update:show', $event)"
  >
    <div class="asset-picker">
      <!-- 工具栏 -->
      <header class="asset-picker__toolbar">
        <NInput
          v-model:value="keyword"
          clearable
          size="small"
          placeholder="搜索名称或编码"
          class="asset-picker__search"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix>
            <i class="i-lucide:search" aria-hidden="true" />
          </template>
        </NInput>
        <a
          v-if="marketHref"
          class="asset-picker__market"
          :href="marketHref"
          target="_blank"
          rel="noopener noreferrer"
        >
          去市场安装
          <i class="i-lucide:external-link" aria-hidden="true" />
        </a>
        <NButton size="small" type="primary" @click="installOpen = true">
          <template #icon>
            <i class="i-lucide:download" />
          </template>
          安装本地包
        </NButton>
      </header>

      <p class="asset-picker__hint">
        <span class="asset-picker__kind-chip" :style="{ color: kindTone.ink, background: kindTone.wash }">
          <i :class="kindIcon" aria-hidden="true" />
          {{ kindLabel }}
        </span>
        {{ kindHint }}
      </p>

      <NSpin :show="loading">
        <NEmpty
          v-if="!loading && !items.length"
          :description="keyword ? '没有匹配的模板' : `还没有已安装的${kindLabel}`"
          class="asset-picker__empty"
        >
          <template #extra>
            <NButton size="small" type="primary" @click="installOpen = true">
              安装第一个模板包
            </NButton>
          </template>
        </NEmpty>

        <div v-else class="asset-picker__grid" role="list" :aria-label="modalTitle">
          <article
            v-for="row in items"
            :key="row.id"
            class="market-item"
            :style="{
              '--market-ink': kindTone.ink,
              '--market-wash': kindTone.wash,
              '--market-border': kindTone.border,
            }"
            role="listitem"
          >
            <div class="market-item__header">
              <div class="market-item__identity">
                <span class="market-item__icon" aria-hidden="true">
                  <i :class="kindIcon" />
                </span>
                <div class="market-item__title">
                  <strong :title="row.assetName">{{ row.assetName }}</strong>
                  <code :title="row.assetKey">{{ row.assetKey }}</code>
                </div>
              </div>
              <span class="market-item__badge">{{ kindLabel }}</span>
            </div>
            <div class="market-item__body">
              <div class="market-item__tags">
                <span v-if="row.assetVersion">v{{ row.assetVersion }}</span>
                <span v-if="row.sourceFileName" :title="row.sourceFileName">{{ row.sourceFileName }}</span>
                <span v-else>已安装</span>
              </div>
            </div>
            <div class="market-item__footer">
              <div class="market-item__meta">
                <span>
                  <i class="i-lucide:calendar" aria-hidden="true" />
                  {{ formatTime(row.updateTime || row.createTime) || '—' }}
                </span>
              </div>
              <div class="market-item__actions">
                <button
                  type="button"
                  class="market-item__action"
                  :disabled="removingId === row.id"
                  @click="onRemove(row)"
                >
                  移除
                </button>
                <span class="market-item__sep" aria-hidden="true" />
                <button
                  type="button"
                  class="market-item__action is-primary"
                  :disabled="selectingId === row.id"
                  @click="onSelect(row)"
                >
                  {{ selectingId === row.id ? '加载中…' : selectLabel }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </NSpin>

      <footer v-if="total > pageSize" class="asset-picker__pager">
        <NPagination
          v-model:page="pageNum"
          :page-size="pageSize"
          :item-count="total"
          size="small"
          @update:page="refresh"
        />
      </footer>
    </div>

    <ContentPackageImportDialog
      v-model:show="installOpen"
      :kind="kind"
      :busy="installing"
      @confirm="onInstall"
    />
  </NModal>
</template>

<style scoped>
.asset-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 320px;
}

.asset-picker__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.asset-picker__search {
  flex: 1 1 220px;
  min-width: 180px;
}

.asset-picker__market {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  color: var(--primary-color, #2563eb);
  font-size: 12px;
  text-decoration: none;
}

.asset-picker__market:hover {
  color: var(--primary-color-hover, #1d4ed8);
}

.asset-picker__hint {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.asset-picker__kind-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 650;
}

.asset-picker__empty {
  padding: 36px 12px;
}

.asset-picker__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr));
  gap: 14px;
  min-height: 180px;
}

.market-item {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--market-border, #e5e7eb);
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 4px 14px rgb(15 23 42 / 4%);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.market-item:hover {
  box-shadow: 0 12px 28px rgb(15 23 42 / 10%);
  transform: translateY(-2px);
}

.market-item__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 16px 10px;
}

.market-item__identity {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.market-item__icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 4px;
  background: linear-gradient(145deg, color-mix(in srgb, var(--market-wash) 40%, #fff), var(--market-wash));
  color: var(--market-ink);
  font-size: 18px;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--market-ink) 12%, transparent);
}

.market-item__title {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.market-item__title strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.market-item__title code {
  overflow: hidden;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.market-item__badge {
  flex: none;
  height: 20px;
  padding: 0 7px;
  border-radius: 4px;
  background: var(--market-wash);
  color: var(--market-ink);
  font-size: 11px;
  font-weight: 600;
  line-height: 20px;
}

.market-item__body {
  display: grid;
  gap: 8px;
  padding: 0 16px 12px;
}

.market-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.market-item__tags span {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  height: 20px;
  padding: 0 7px;
  overflow: hidden;
  border-radius: 4px;
  background: color-mix(in srgb, var(--market-wash) 70%, #fff);
  color: var(--market-ink);
  font-size: 11px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.market-item__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: nowrap;
  margin-top: auto;
  min-height: 36px;
  padding: 8px 14px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.market-item__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
}

.market-item__meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.market-item__actions {
  display: flex;
  align-items: center;
  flex: none;
  white-space: nowrap;
}

.market-item__action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.market-item__action:hover:not(:disabled) {
  color: #1d4ed8;
}

.market-item__action.is-primary {
  font-weight: 650;
}

.market-item__action:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.market-item__sep {
  width: 1px;
  height: 12px;
  margin: 0 6px;
  background: #e2e8f0;
}

.asset-picker__pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}
</style>
