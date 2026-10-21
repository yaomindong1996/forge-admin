<script setup>
import { computed, ref, useSlots } from 'vue'
import { officialMarketOpenLabel, officialMarketTitle, officialMarketUrl } from '@/utils/officialMarket'
import ContentPackageImportDialog from './ContentPackageImportDialog.vue'

const props = defineProps({
  kind: { type: String, required: true },
  variant: { type: String, default: 'hub' },
  q: { type: String, default: '' },
  showImport: { type: Boolean, default: true },
  /** file：弹窗选文件；action：只通知父级打开专用导入（如应用包） */
  importMode: { type: String, default: 'file' },
})
const emit = defineEmits(['import'])
const slots = useSlots()

const importOpen = ref(false)
const importing = ref(false)
const href = computed(() => officialMarketUrl(props.kind, { q: props.q }))
const openLabel = computed(() => officialMarketOpenLabel(props.kind))
const title = computed(() => officialMarketTitle(props.kind))
const importLabel = computed(() => ({
  app: '导入应用',
  print: '安装打印模板',
  form: '安装表单模板',
  'print-component': '安装打印组件',
  plugin: '导入',
}[props.kind] || '安装模板'))
/** 工具栏用短文案，避免三颗描边按钮挤成一团 */
const toolbarOpenLabel = computed(() => ({
  app: '应用市场',
  print: '模板市场',
  form: '表单市场',
  'print-component': '组件市场',
  plugin: '插件市场',
}[props.kind] || '打开市场'))
const toolbarImportLabel = computed(() => ({
  app: '导入',
  print: '安装',
  form: '安装',
  'print-component': '安装',
  plugin: '导入',
}[props.kind] || '安装'))
const hint = computed(() => ({
  app: '内置模板可直接启用。更多方案到应用市场下载后，再点「导入应用」。',
  print: '空白可直接新建。市场版式先「安装到我的模板」，再在列表「从我的模板选用」。',
  form: '空白可直接新建。市场表单先安装到我的模板，其它页面可反复选用。',
  'print-component': '审批章、签章等可从组件市场下载后安装到我的模板。',
  plugin: '在插件市场查看能力说明，下载源码后在工程中集成。',
}[props.kind] || '到对应市场下载后，再回到这里安装到我的模板。'))
const iconClass = computed(() => ({
  app: 'i-lucide:layout-grid',
  print: 'i-lucide:printer',
  form: 'i-lucide:panel-top',
  'print-component': 'i-lucide:puzzle',
  plugin: 'i-lucide:boxes',
}[props.kind] || 'i-lucide:store'))
const hasExtra = computed(() => Boolean(slots.extra))

function pickImport() {
  if (props.importMode === 'action') {
    emit('import', { kind: props.kind })
    return
  }
  importOpen.value = true
}

async function onDialogConfirm({ file, kind }) {
  importing.value = true
  try {
    emit('import', { file, kind })
    importOpen.value = false
  }
  finally {
    importing.value = false
  }
}
</script>

<template>
  <section v-if="href && variant === 'hub'" class="official-market-entry is-hub">
    <span class="official-market-entry__icon" aria-hidden="true">
      <i :class="iconClass" />
    </span>
    <div class="official-market-entry__copy">
      <strong>{{ title }}</strong>
      <p>{{ hint }}</p>
    </div>
    <div class="official-market-entry__actions">
      <n-button type="primary" tag="a" :href="href" target="_blank" rel="noopener noreferrer">
        <template #icon>
          <i class="i-lucide:external-link" />
        </template>
        {{ openLabel }}
      </n-button>
      <n-button v-if="showImport" @click="pickImport">
        <template #icon>
          <i class="i-lucide:upload" />
        </template>
        {{ importLabel }}
      </n-button>
    </div>
  </section>

  <!-- 工具栏：文字链 + 竖线，避免三颗同权描边按钮 -->
  <div v-else-if="href && variant === 'toolbar'" class="official-market-entry is-toolbar" role="group" :aria-label="title">
    <a class="market-link" :href="href" target="_blank" rel="noopener noreferrer" :title="openLabel">
      <i class="i-lucide:store" aria-hidden="true" />
      <span>{{ toolbarOpenLabel }}</span>
    </a>
    <template v-if="showImport">
      <span class="market-sep" aria-hidden="true" />
      <button type="button" class="market-link" :title="importLabel" @click="pickImport">
        <i class="i-lucide:download" aria-hidden="true" />
        <span>{{ toolbarImportLabel }}</span>
      </button>
    </template>
    <template v-if="hasExtra">
      <span class="market-sep" aria-hidden="true" />
      <span class="market-extra">
        <slot name="extra" />
      </span>
    </template>
  </div>

  <span v-else-if="href" class="official-market-entry is-link">
    <a :href="href" target="_blank" rel="noopener noreferrer">
      <i class="i-lucide:external-link" aria-hidden="true" />
      {{ openLabel }}
    </a>
    <button v-if="showImport" type="button" @click="pickImport">
      <i class="i-lucide:upload" aria-hidden="true" />
      {{ importLabel }}
    </button>
  </span>

  <ContentPackageImportDialog
    v-if="showImport && importMode === 'file'"
    v-model:show="importOpen"
    :kind="kind"
    :busy="importing"
    @confirm="onDialogConfirm"
  />
</template>

<style scoped>
.official-market-entry.is-hub {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin: 0 0 12px;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
}
.official-market-entry__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid #dbeafe;
  border-radius: 2px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 18px;
}
.official-market-entry__copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.official-market-entry__copy strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}
.official-market-entry__copy p {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}
.official-market-entry__actions,
.official-market-entry.is-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.official-market-entry.is-toolbar {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 32px;
  padding: 0;
  border: 0;
  background: transparent;
}

.market-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  padding: 0 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary, #4b5563);
  font: inherit;
  font-size: 12px;
  line-height: 28px;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
  transition: color 140ms ease, background 140ms ease;
}

.market-link i {
  font-size: 14px;
  opacity: 0.9;
}

.market-link:hover {
  color: var(--primary-color, #2563eb);
  background: color-mix(in srgb, var(--primary-color, #2563eb) 8%, transparent);
}

.market-sep {
  width: 1px;
  height: 12px;
  margin: 0 2px;
  flex: none;
  background: var(--border-light, #e5e7eb);
}

.market-extra {
  display: inline-flex;
  align-items: center;
}

.market-extra :deep(.market-link),
.market-extra :deep(button.market-link),
.market-extra :deep(a.market-link) {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  padding: 0 8px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary, #4b5563);
  font: inherit;
  font-size: 12px;
  line-height: 28px;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
}

.market-extra :deep(.market-link:hover),
.market-extra :deep(button.market-link:hover),
.market-extra :deep(a.market-link:hover) {
  color: var(--primary-color, #2563eb);
  background: color-mix(in srgb, var(--primary-color, #2563eb) 7%, transparent);
}

.market-extra :deep(.market-link:disabled),
.market-extra :deep(button.market-link:disabled) {
  opacity: 0.45;
  cursor: not-allowed;
}

.official-market-entry.is-link a,
.official-market-entry.is-link button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--primary-color);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
  text-decoration: none;
}
.official-market-entry.is-link button {
  color: var(--text-secondary);
}
.official-market-entry.is-link a:hover,
.official-market-entry.is-link button:hover {
  color: var(--primary-color);
}
@media (max-width: 720px) {
  .official-market-entry.is-hub {
    grid-template-columns: 36px minmax(0, 1fr);
  }
  .official-market-entry__actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
