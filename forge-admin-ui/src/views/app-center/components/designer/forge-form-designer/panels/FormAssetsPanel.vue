<template>
  <!-- 多表单：切换 / 编辑当前 / 其他表单 / 底部模板市场 -->
  <section class="panel-item form-asset-panel">
    <header class="form-asset-toolbar">
      <p class="form-asset-hint">
        同一业务对象可有多套表单（如新增、编辑、弹窗）
      </p>
      <div class="form-asset-toolbar__actions">
        <button type="button" class="form-asset-icon-button" title="复制当前表单" @click="duplicateCurrentFormAsset">
          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M9 3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V4h-9a1 1 0 0 1-1-1Z" fill="currentColor" />
            <path d="M5 6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H5Zm0 2h10v12H5V8Z" fill="currentColor" />
          </svg>
        </button>
        <button type="button" class="form-asset-icon-button" title="新建空白表单" @click="createBlankFormAsset">
          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M11 4a1 1 0 1 1 2 0v7h7a1 1 0 1 1 0 2h-7v7a1 1 0 1 1-2 0v-7H4a1 1 0 1 1 0-2h7V4Z" fill="currentColor" />
          </svg>
        </button>
      </div>
    </header>

    <div class="form-asset-tabs">
      <button type="button" class="form-asset-tab active" @click="showFormSettings">
        <span>{{ schema.formName || '主表单' }}</span>
        <strong>当前</strong>
      </button>
      <button
        v-for="asset in formAssets"
        :key="asset.formKey"
        type="button"
        class="form-asset-tab"
        @click="switchFormAsset(asset.formKey)"
      >
        <span>{{ asset.formName || '未命名表单' }}</span>
      </button>
    </div>

    <div class="form-asset-edit-grid">
      <n-form-item label="表单名称">
        <n-input
          :value="schema.formName || ''"
          placeholder="请输入表单名称"
          @update:value="updateCurrentFormMeta({ formName: $event || '业务表单' })"
        />
      </n-form-item>
      <n-form-item label="表单编码">
        <n-input
          :value="schema.formKey || ''"
          placeholder="form_key"
          @update:value="updateCurrentFormMeta({ formKey: $event || schema.formKey })"
        />
      </n-form-item>
      <n-form-item label="表单用途">
        <n-select
          :value="schema.usage || ['create', 'edit']"
          :options="formUsageOptions"
          multiple
          clearable
          placeholder="选择用途"
          @update:value="updateCurrentFormMeta({ usage: $event })"
        />
      </n-form-item>
      <n-form-item label="默认表单">
        <span v-if="defaultFormKey === schema.formKey" class="form-default-badge">
          默认表单
        </span>
        <n-button
          v-else
          size="small"
          secondary
          @click="setDefaultFormKey(schema.formKey)"
        >
          设为默认
        </n-button>
      </n-form-item>
    </div>

    <div v-if="formAssets.length" class="form-asset-list">
      <div class="form-asset-list-title">
        其他表单
      </div>
      <div v-for="asset in formAssets" :key="asset.formKey" class="form-asset-card">
        <div class="form-asset-main">
          <n-input
            :value="asset.formName"
            size="small"
            placeholder="表单名称"
            @update:value="updateFormAssetMeta(asset.formKey, { formName: $event || '未命名表单' })"
          />
          <n-select
            :value="asset.usage || ['create', 'edit']"
            :options="formUsageOptions"
            multiple
            clearable
            size="small"
            placeholder="用途"
            @update:value="updateFormAssetMeta(asset.formKey, { usage: $event })"
          />
        </div>
        <div class="form-asset-card__ops">
          <n-button
            size="tiny"
            quaternary
            :type="defaultFormKey === asset.formKey ? 'primary' : 'default'"
            @click="setDefaultFormKey(asset.formKey)"
          >
            {{ defaultFormKey === asset.formKey ? '默认' : '设默认' }}
          </n-button>
          <n-button size="tiny" quaternary type="error" @click="removeFormAsset(asset.formKey)">
            删除
          </n-button>
        </div>
      </div>
    </div>

    <aside v-if="formMarketUrl" class="form-market-promo">
      <img class="form-market-promo__art" :src="marketStarUrl" alt="" draggable="false">
      <div class="form-market-promo__copy">
        <strong>更多表单模板</strong>
        <em>持续更新中</em>
      </div>
      <a
        class="form-market-btn form-market-btn--primary"
        :href="formMarketUrl"
        target="_blank"
        rel="noopener noreferrer"
      >
        探索表单市场 →
      </a>
      <button type="button" class="form-market-btn form-market-btn--ghost" @click="libraryOpen = true">
        从我的模板选用
      </button>
      <button type="button" class="form-market-btn form-market-btn--ghost" @click="importOpen = true">
        安装表单模板
      </button>
    </aside>

    <ContentPackageImportDialog
      v-model:show="importOpen"
      kind="form"
      :busy="importing"
      @confirm="importFormPackage"
    />
    <InstalledContentAssetPicker
      v-model:show="libraryOpen"
      kind="form"
      title="我的表单模板"
      select-label="应用到当前表单"
      @select="applyLibraryForm"
    />
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import ContentPackageImportDialog from '@/components/common/ContentPackageImportDialog.vue'
import InstalledContentAssetPicker from '@/components/common/InstalledContentAssetPicker.vue'
import marketStarUrl from '@/assets/print/component-market-star.png'
import { useFormDesignerStore } from '@/store'
import { officialMarketUrl } from '@/utils/officialMarket'
import { installContentPackageFromFile } from '@/utils/contentInstalledAsset'
import { cloneValue, normalizeFormDesignerSchema } from '../../form-first/formDesignerSchema'

/**
 * 多表单管理面板（从 ForgePropertyPanel 拆出，Pinia 化改造，AGENTS.md 5.14）
 *
 * 一个业务对象可维护多个表单资产（主表单 + 复制弹窗/空白副本），
 * 支持切换当前编辑表单、编辑元信息、设默认、复制、删除。
 */
const designerStore = useFormDesignerStore()
const importOpen = ref(false)
const libraryOpen = ref(false)
const importing = ref(false)
const formMarketUrl = computed(() => officialMarketUrl('form'))

function applyFormPack(pack, successMessage) {
  designerStore.applySchema(normalizeFormDesignerSchema(pack.payload))
  window.$message?.success(successMessage)
}

async function importFormPackage({ file }) {
  importing.value = true
  try {
    const { pack } = await installContentPackageFromFile({ file, kind: 'form' })
    applyFormPack(pack, '已安装到我的模板，并应用到当前表单')
    importOpen.value = false
  }
  catch (error) {
    window.$message?.error(error.message || '安装表单模板失败')
  }
  finally {
    importing.value = false
  }
}

function applyLibraryForm({ pack }) {
  try {
    applyFormPack(pack, `已选用「${pack.name || '表单模板'}」`)
  }
  catch (error) {
    window.$message?.error(error.message || '选用表单模板失败')
  }
}

const schema = computed(() => designerStore.schema)
const formAssets = computed(() => designerStore.formAssets)
const defaultFormKey = computed(() => designerStore.defaultFormKey)

const formUsageOptions = [
  { label: '新增', value: 'create' },
  { label: '编辑', value: 'edit' },
  { label: '详情', value: 'detail' },
  { label: '填报', value: 'submit' },
  { label: '审批', value: 'approve' },
  { label: '移动端', value: 'mobile' },
]

function resolveFormUsage(value) {
  const usage = Array.isArray(value)
    ? value.map(item => String(item || '').trim()).filter(Boolean)
    : []
  return usage.length ? Array.from(new Set(usage)) : ['create', 'edit']
}

/** 回到当前（主）表单设置 */
function showFormSettings() {
  designerStore.selectComponent('')
  designerStore.setFormPropertyTab('basic')
}

function updateCurrentFormMeta(patch = {}) {
  const current = schema.value
  const nextFormKey = patch.formKey || current.formKey
  const nextDefaultFormKey = patch.formKey && defaultFormKey.value === current.formKey
    ? nextFormKey
    : defaultFormKey.value
  designerStore.applySchema({
    ...current,
    ...patch,
    usage: patch.usage ? resolveFormUsage(patch.usage) : current.usage,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(current.settings || {}),
      defaultFormKey: nextDefaultFormKey,
    },
  })
}

function setDefaultFormKey(formKey = '') {
  if (!formKey)
    return
  const current = schema.value
  designerStore.applySchema({
    ...current,
    defaultFormKey: formKey,
    settings: {
      ...(current.settings || {}),
      defaultFormKey: formKey,
    },
  })
}

function updateFormAssetMeta(formKey = '', patch = {}) {
  const current = schema.value
  const nextPatch = {
    ...patch,
    ...(Object.prototype.hasOwnProperty.call(patch, 'usage')
      ? { usage: resolveFormUsage(patch.usage) }
      : {}),
  }
  designerStore.applySchema({
    ...current,
    settings: {
      ...(current.settings || {}),
      formAssets: formAssets.value.map((asset) => {
        if (asset.formKey !== formKey)
          return asset
        return {
          ...asset,
          ...nextPatch,
          schema: asset.schema
            ? {
                ...asset.schema,
                ...nextPatch,
              }
            : asset.schema,
        }
      }),
    },
  })
}

function duplicateCurrentFormAsset() {
  const current = schema.value
  const nextAssetKey = `${current.formKey || 'form'}_dialog_${Date.now()}`
  const usage = resolveFormUsage(current.usage)
  const assetSchema = cloneValue(current)
  assetSchema.formKey = nextAssetKey
  assetSchema.formName = `${current.formName || '表单'}弹窗`
  assetSchema.usage = usage
  assetSchema.settings = {
    ...(assetSchema.settings || {}),
    formAssets: [],
  }
  designerStore.applySchema({
    ...current,
    settings: {
      ...(current.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          usage,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function createBlankFormAsset() {
  const current = schema.value
  const nextAssetIndex = formAssets.value.length + 2
  const nextAssetKey = `${current.formKey || 'form'}_form_${Date.now()}`
  const usage = ['create', 'edit']
  const assetSchema = {
    ...cloneValue(current),
    formKey: nextAssetKey,
    formName: `表单 ${nextAssetIndex}`,
    usage,
    components: [],
    settings: {
      ...(current.settings || {}),
      formAssets: [],
    },
  }
  designerStore.applySchema({
    ...current,
    settings: {
      ...(current.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          usage,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function switchFormAsset(formKey = '') {
  const current = schema.value
  const asset = formAssets.value.find(item => item.formKey === formKey)
  if (!asset?.schema)
    return
  const nextDefaultFormKey = defaultFormKey.value || current.formKey
  const currentUsage = resolveFormUsage(current.usage)
  const assetUsage = resolveFormUsage(asset.usage || asset.schema.usage)
  const currentAsset = {
    formKey: current.formKey,
    formName: current.formName,
    usage: currentUsage,
    schema: {
      ...cloneValue(current),
      usage: currentUsage,
      settings: {
        ...(current.settings || {}),
        formAssets: [],
      },
    },
  }
  const nextAssets = formAssets.value
    .filter(item => item.formKey !== formKey && item.formKey !== currentAsset.formKey)
    .concat(currentAsset)
  designerStore.selectComponent('')
  const nextSchema = normalizeFormDesignerSchema({
    ...cloneValue(asset.schema),
    usage: assetUsage,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(asset.schema.settings || {}),
      formAssets: nextAssets,
      defaultFormKey: nextDefaultFormKey,
    },
  })
  designerStore.applySchema({
    ...nextSchema,
    usage: assetUsage,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(nextSchema.settings || {}),
      defaultFormKey: nextDefaultFormKey,
    },
  })
}

function removeFormAsset(formKey = '') {
  const current = schema.value
  const nextDefaultFormKey = defaultFormKey.value === formKey ? current.formKey : defaultFormKey.value
  designerStore.applySchema({
    ...current,
    defaultFormKey: nextDefaultFormKey,
    settings: {
      ...(current.settings || {}),
      defaultFormKey: nextDefaultFormKey,
      formAssets: formAssets.value.filter(item => item.formKey !== formKey),
    },
  })
}
</script>

<style scoped>
.panel-item {
  display: flex;
  flex-direction: column;
}

.panel-item :deep(.n-form-item) {
  margin-bottom: 6px;
  border: 0;
  border-radius: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.panel-item :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.form-asset-panel {
  background: var(--bg-primary);
}

.form-asset-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.form-asset-hint {
  margin: 0;
  color: var(--text-tertiary, #8f959e);
  font-size: 11px;
  line-height: 16px;
}

.form-asset-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.form-asset-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 13px;
  padding: 0;
}

.form-asset-icon-button:hover {
  border-color: var(--border-light, #e8edf3);
  background: #f5f7fb;
  color: var(--primary-color, #2f6bff);
}

.form-asset-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.form-asset-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 168px;
  flex: 0 0 auto;
  height: 30px;
  cursor: pointer;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  padding: 0 9px;
}

.form-asset-tab:hover {
  border-color: #bfdbfe;
  background: var(--bg-secondary);
}

.form-asset-tab.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.form-asset-tab span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
}

.form-asset-tab strong {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 10px;
  line-height: 16px;
  padding: 0 5px;
}

.form-asset-edit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}

.form-asset-edit-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.form-default-badge {
  display: inline-flex;
  align-items: center;
  height: 26px;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  background: #f0fdf4;
  color: #16a34a;
  font-size: 11px;
  font-weight: 700;
  padding: 0 8px;
}

.form-asset-list {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}

.form-asset-list-title {
  color: var(--text-tertiary);
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.form-asset-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
  color: var(--text-primary);
  padding: 8px 10px;
}

.form-asset-main {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.form-asset-card__ops {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.form-market-promo {
  display: grid;
  justify-items: center;
  gap: 6px;
  margin: 4px -2px 0;
  padding: 10px 10px 12px;
  border: 0;
  border-top: 1px solid #e8edf3;
  border-radius: 0;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef3ff 100%);
  text-align: center;
}

.form-market-promo__art {
  display: block;
  width: 52px;
  height: 52px;
  object-fit: contain;
  pointer-events: none;
  user-select: none;
}

.form-market-promo__copy {
  display: grid;
  gap: 2px;
  justify-items: center;
}

.form-market-promo__copy strong {
  color: #3b6ef5;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.25;
}

.form-market-promo__copy em {
  color: #7b8db0;
  font-size: 10px;
  font-style: normal;
  line-height: 1.25;
}

.form-market-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 30px;
  padding: 0 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
}

.form-market-btn--primary {
  border: 1px solid transparent;
  background: var(--primary-color, #2f6bff);
  color: #fff;
  box-shadow: 0 1px 2px rgb(47 107 255 / 18%);
}

.form-market-btn--primary:hover {
  filter: brightness(1.04);
}

.form-market-btn--ghost {
  border: 1px solid #e8edf3;
  background: #fff;
  color: var(--text-secondary, #4e5969);
  font-size: 11px;
  font-weight: 500;
}

.form-market-btn--ghost:hover {
  border-color: color-mix(in srgb, var(--primary-color, #2f6bff) 35%, #e8edf3);
  color: var(--primary-color, #2f6bff);
}
</style>
