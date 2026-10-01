<script setup>
import { NAlert, NButton, NCard, NDropdown, NEmpty, NModal, NPagination, NSelect, NSpace, NSpin, NTag } from 'naive-ui'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as api from '@/api/print'
import DictTag from '@/components/DictTag.vue'
import { newPrintTemplateCode } from '@/components/print/id'
import { hasPrintPermission } from '@/components/print/management/printPermissions'
import { printSourcePayload } from '@/components/print/management/printRouteContext'
import { FALLBACK_PRINT_SCENE_OPTIONS, scenesOfTemplate, syncPrintTemplateScenes } from '@/components/print/management/printSceneBinding'
import PrintTemplateCreate from '@/components/print/management/PrintTemplateCreate.vue'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

const props = defineProps({
  applicationId: { type: String, default: null },
  businessSourceId: { type: String, default: null },
  applicationCode: { type: String, default: '' },
  source: { type: Object, default: null },
  sources: { type: Array, default: () => [] },
  lockSource: Boolean,
  showBindings: { type: Boolean, default: true },
  allowCreate: { type: Boolean, default: true },
})
const router = useRouter()
const route = useRoute()
const store = usePrintTemplateStore()
const user = useUserStore()
const { dict } = useDict('sys_print_scene')
const source = computed(() => props.source)
const appId = computed(() => props.applicationId)
const standaloneId = computed(() => props.businessSourceId || source.value?.businessSourceId || null)
const scopeKey = computed(() => standaloneId.value
  ? `source:${standaloneId.value}`
  : `app:${appId.value || ''}:${source.value?.pageId || source.value?.formKey || ''}`)
const hasContext = computed(() => Boolean(standaloneId.value || appId.value))
const listSource = computed(() => standaloneId.value ? source.value : appId.value)
const canManage = computed(() => hasPrintPermission(user, 'print:template:manage'))
const creating = ref(false)
const busy = ref(false)
const busyRowId = ref(null)
const bindings = ref([])
const bindingGeneration = ref(0)
const sceneOptions = computed(() => dict.value.sys_print_scene?.length ? dict.value.sys_print_scene : FALLBACK_PRINT_SCENE_OPTIONS)
watch(scopeKey, async () => {
  store.listGeneration++
  store.items = []
  store.total = 0
  creating.value = false
  bindings.value = []
  if (hasContext.value)
    await store.list(listSource.value, 1, source.value?.pageId)
  // 业务打印虽隐藏挂载 UI，仍需加载并补齐 DETAIL 绑定
  if (props.showBindings || standaloneId.value)
    await loadBindings()
  if (standaloneId.value)
    await ensureStandaloneDetailBindings()
}, { immediate: true })
onBeforeUnmount(() => {
  store.listGeneration++
  bindingGeneration.value++
  store.items = []
  bindings.value = []
})
async function loadBindings() {
  const generation = ++bindingGeneration.value
  const payload = printSourcePayload(source.value)
  if (!payload) {
    bindings.value = []
    return
  }
  try {
    const { data } = await api.printBindings(payload)
    if (generation === bindingGeneration.value)
      bindings.value = data || []
  }
  catch (error) {
    if (generation === bindingGeneration.value) {
      bindings.value = []
      store.error = error.message || '无法读取打印场景'
    }
  }
}
/** 业务打印固定详情场景：已发布却未绑 DETAIL 的模板自动补上。 */
async function ensureStandaloneDetailBindings() {
  if (!standaloneId.value || !canManage.value)
    return
  const payload = printSourcePayload(source.value)
  if (!payload)
    return
  const activeScope = scopeKey.value
  let next = bindings.value
  let changed = false
  for (const row of store.items) {
    if (!row?.publishedVersionId)
      continue
    if (scenesOfTemplate(next, row.id).includes('DETAIL'))
      continue
    try {
      next = await syncPrintTemplateScenes({
        source: payload,
        templateId: row.id,
        templateVersionId: row.publishedVersionId,
        scenes: ['DETAIL'],
        bindings: next,
        save: api.savePrintBinding,
        remove: api.deletePrintBinding,
      })
      changed = true
    }
    catch (error) {
      if (scopeKey.value === activeScope)
        store.error = error.message || '自动绑定详情场景失败'
      return
    }
  }
  if (changed && scopeKey.value === activeScope)
    bindings.value = next
}
function rowScenes(row) {
  return scenesOfTemplate(bindings.value, row.id)
}
function design(row) {
  router.push({
    path: '/print/designer',
    query: {
      templateId: String(row.id),
      applicationCode: props.applicationCode || undefined,
      pageId: row.source?.pageId || source.value?.pageId || undefined,
      from: route.fullPath,
    },
  })
}
function patchTemplateRow(rowId, patch) {
  const idx = store.items.findIndex(item => String(item.id) === String(rowId))
  if (idx < 0)
    return
  store.items[idx] = { ...store.items[idx], ...patch }
}
async function act(action, { refreshList = true } = {}) {
  if (busy.value)
    return
  const activeScope = scopeKey.value
  busy.value = true
  store.error = ''
  try {
    await action()
    if (refreshList && scopeKey.value === activeScope)
      await store.list(listSource.value, store.pageNum, source.value?.pageId)
  }
  catch (error) {
    if (scopeKey.value === activeScope)
      store.error = error.message || '操作失败'
  }
  finally {
    busy.value = false
    busyRowId.value = null
  }
}
async function copy(row) {
  await act(async () => {
    const copiedScenes = rowScenes(row)
    const { data } = await api.copyPrintTemplate(row.id, { expectedRevision: row.draftRevision, templateCode: newPrintTemplateCode(), templateName: `${row.templateName.slice(0, 95)} 副本` })
    if (!data.source?.businessSourceId) {
      await syncPrintTemplateScenes({
        source: printSourcePayload(data.source) || printSourcePayload(source.value),
        templateId: data.id,
        scenes: copiedScenes,
        bindings: [],
        save: api.savePrintBinding,
        remove: api.deletePrintBinding,
      })
    }
    design(data)
  })
}
async function changeScenes(row, scenes) {
  if (busy.value)
    return
  const activeScope = scopeKey.value
  if (standaloneId.value && !row.publishedVersionId) {
    store.error = '请先发布模板，再绑定到业务场景'
    return
  }
  const previous = bindings.value
  busy.value = true
  busyRowId.value = row.id
  store.error = ''
  try {
    const next = await syncPrintTemplateScenes({
      source: printSourcePayload(row.source) || printSourcePayload(source.value),
      templateId: row.id,
      templateVersionId: standaloneId.value ? row.publishedVersionId : undefined,
      scenes,
      bindings: previous,
      save: api.savePrintBinding,
      remove: api.deletePrintBinding,
    })
    if (scopeKey.value === activeScope)
      bindings.value = next
  }
  catch (error) {
    if (scopeKey.value === activeScope) {
      bindings.value = previous
      store.error = error.message || '更新挂载位置失败'
    }
  }
  finally {
    busy.value = false
    busyRowId.value = null
  }
}
async function toggleStatus(row) {
  if (busy.value)
    return
  const activeScope = scopeKey.value
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  busy.value = true
  busyRowId.value = row.id
  store.error = ''
  try {
    const { data } = await api.changePrintTemplateStatus(row.id, {
      expectedRevision: row.draftRevision,
      status: nextStatus,
    })
    if (scopeKey.value === activeScope && data) {
      patchTemplateRow(row.id, {
        status: data.status ?? nextStatus,
        draftRevision: data.draftRevision ?? row.draftRevision,
        designStatus: data.designStatus ?? row.designStatus,
      })
    }
  }
  catch (error) {
    if (scopeKey.value === activeScope)
      store.error = error.message || '更新状态失败'
  }
  finally {
    busy.value = false
    busyRowId.value = null
  }
}
function moreOptions(row) {
  return [
    { label: '复制', key: 'copy' },
    { label: Number(row.status) === 1 ? '停用' : '启用', key: 'status' },
    { label: '删除', key: 'delete' },
  ]
}
const deleting = ref(null)
function more(key, row) {
  if (key === 'copy')
    return copy(row)
  if (key === 'delete') {
    deleting.value = row
    return
  }
  if (key === 'status')
    return toggleStatus(row)
}
function sourceLabel(row) {
  if (row.source?.businessSourceId)
    return props.sources.find(item => String(item.id) === String(row.source.businessSourceId))?.sourceName || '业务来源'
  return props.sources.find(item => item.source?.pageId === row.source?.pageId
    && item.source?.objectCode === row.source?.objectCode)?.label || '所属表单'
}
function sceneLabel(value) {
  return sceneOptions.value.find(item => String(item.value) === String(value))?.label || value
}
function isEnabled(row) {
  return Number(row.status) === 1
}
async function refresh() {
  if (hasContext.value)
    await store.list(listSource.value, store.pageNum, source.value?.pageId)
  if (props.showBindings || standaloneId.value)
    await loadBindings()
  if (standaloneId.value)
    await ensureStandaloneDetailBindings()
}
</script>

<template>
  <NCard :title="lockSource ? undefined : '打印模板'" size="small" :bordered="false" class="print-template-list">
    <template v-if="!lockSource" #header-extra>
      <NSpace>
        <NButton :disabled="!hasContext || busy" @click="refresh">
          <template #icon>
            <i class="i-lucide:refresh-cw" />
          </template>
          刷新
        </NButton>
        <NButton v-if="canManage && allowCreate" type="primary" :disabled="!source || busy" @click="creating = true">
          <template #icon>
            <i class="i-lucide:file-plus" />
          </template>
          新建模板
        </NButton>
      </NSpace>
    </template>
    <NAlert v-if="store.error" type="error" class="print-list-alert">
      {{ store.error }}
    </NAlert>
    <NEmpty v-if="!hasContext" description="请选择业务来源" />
    <template v-else>
      <div v-if="lockSource" class="print-list-actions">
        <NButton :disabled="!hasContext || busy" @click="refresh">
          <template #icon>
            <i class="i-lucide:refresh-cw" />
          </template>
          刷新
        </NButton>
        <NButton v-if="canManage && allowCreate" type="primary" :disabled="!source || busy" @click="creating = true">
          <template #icon>
            <i class="i-lucide:file-plus" />
          </template>
          新建模板
        </NButton>
      </div>
      <div v-if="!lockSource" class="print-list-context">
        <slot name="source" />
        <p>勾选场景后重新发布应用，即可供业务用户使用。</p>
      </div>
      <NSpin :show="store.listing">
        <NEmpty v-if="!store.items.length && !store.listing" description="暂无打印模板" />
        <div v-else class="print-card-grid" role="list" aria-label="打印模板">
          <article
            v-for="row in store.items"
            :key="row.id"
            class="print-card"
            :class="{ disabled: !isEnabled(row), busy: busyRowId === row.id }"
            role="listitem"
          >
            <button type="button" class="print-card__preview" :aria-label="`设计 ${row.templateName}`" @click="design(row)">
              <span class="print-card__paper" aria-hidden="true">
                <span class="print-card__paper-line short" />
                <span class="print-card__paper-line" />
                <span class="print-card__paper-line" />
                <span class="print-card__paper-line mid" />
                <span class="print-card__paper-block" />
                <span class="print-card__paper-line" />
                <span class="print-card__paper-line mid" />
              </span>
              <i class="print-card__preview-icon i-lucide:printer" />
            </button>
            <div class="print-card__header">
              <div class="print-card__title-block">
                <div class="print-card__icon" aria-hidden="true">
                  <i class="i-lucide:file-text" />
                </div>
                <div class="print-card__title-main">
                  <strong class="print-card__name" :title="row.templateName">{{ row.templateName }}</strong>
                  <span class="print-card__code" :title="row.templateCode">{{ row.templateCode || sourceLabel(row) }}</span>
                </div>
              </div>
              <DictTag
                dict-type="sys_normal_disable"
                :value="row.status"
                force-tag
                :type="isEnabled(row) ? 'success' : 'default'"
              />
            </div>
            <div class="print-card__body">
              <div class="print-card__tags">
                <DictTag dict-type="sys_print_design_status" :value="row.designStatus" />
                <DictTag dict-type="sys_print_source_type" :value="row.source?.sourceType" />
              </div>
              <div v-if="showBindings" class="print-card__scenes">
                <span class="print-card__scenes-label">
                  <i class="i-lucide:map-pin" />
                  挂载位置
                </span>
                <NSelect
                  v-if="canManage"
                  class="print-scene-select"
                  size="small"
                  multiple
                  :value="rowScenes(row)"
                  :options="sceneOptions"
                  :max-tag-count="2"
                  :consistent-menu-width="false"
                  :disabled="busy || !source"
                  :loading="busyRowId === row.id"
                  placeholder="未绑定"
                  :aria-label="`${row.templateName}的挂载位置`"
                  @update:value="scenes => changeScenes(row, scenes)"
                />
                <div v-else class="print-card__scene-tags">
                  <NTag v-for="scene in rowScenes(row)" :key="scene" size="small" :bordered="false">
                    {{ sceneLabel(scene) }}
                  </NTag>
                  <span v-if="!rowScenes(row).length" class="print-card__scene-empty">未绑定</span>
                </div>
              </div>
            </div>
            <div class="print-card__footer">
              <div class="print-card__meta">
                <span><i class="i-lucide:link-2" />{{ sourceLabel(row) }}</span>
              </div>
              <div class="print-card__actions">
                <button type="button" class="print-card__action" @click="design(row)">
                  <i :class="canManage ? 'i-lucide:pen-line' : 'i-lucide:eye'" />
                  {{ canManage ? '设计' : '查看' }}
                </button>
                <template v-if="canManage">
                  <span class="print-card__sep" />
                  <NDropdown :options="moreOptions(row)" :disabled="busy" @select="key => more(key, row)">
                    <button type="button" class="print-card__more" :disabled="busy" :aria-label="`${row.templateName}的更多操作`">
                      <i class="i-lucide:more-horizontal" />
                    </button>
                  </NDropdown>
                </template>
              </div>
            </div>
          </article>
        </div>
      </NSpin>
      <NPagination v-if="store.total > 20" :page="store.pageNum" :page-size="20" :item-count="store.total" :disabled="store.listing" style="margin-top: 16px" @update:page="page => store.list(listSource, page, source?.pageId)" />
    </template>
    <NModal :show="!!deleting" preset="dialog" title="删除打印模板" positive-text="删除" negative-text="取消" :loading="busy" @negative-click="deleting = null" @close="deleting = null" @positive-click="async () => { const row = deleting; deleting = null; await act(() => api.deletePrintTemplate(row.id, row.draftRevision)) }">
      存在绑定或发布引用时无法删除。
    </NModal>
    <PrintTemplateCreate v-model:show="creating" :source="source" @created="design" />
  </NCard>
</template>

<style scoped>
.print-list-alert {
  margin-bottom: 12px;
}
.print-list-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
.print-list-context {
  display: grid;
  gap: 10px;
  margin-bottom: 12px;
}
.print-list-context p {
  margin: 0;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
}
.print-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr));
  gap: 12px;
  align-content: start;
  min-width: 0;
}
.print-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 2px;
  transition:
    box-shadow 160ms ease,
    border-color 160ms ease,
    opacity 160ms ease;
}
.print-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}
.print-card.disabled {
  opacity: 0.78;
}
.print-card.busy {
  pointer-events: none;
}
.print-card__preview {
  position: relative;
  display: grid;
  place-items: center;
  height: 132px;
  margin: 0;
  padding: 0;
  border: 0;
  border-bottom: 1px solid #eef2f7;
  background: #f8fafc;
  cursor: pointer;
}
.print-card__paper {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 78px;
  height: 104px;
  padding: 12px 10px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.08);
}
.print-card__paper-line,
.print-card__paper-block {
  display: block;
  height: 4px;
  border-radius: 1px;
  background: #e2e8f0;
}
.print-card__paper-line.short {
  width: 42%;
}
.print-card__paper-line.mid {
  width: 68%;
}
.print-card__paper-block {
  height: 22px;
  margin: 2px 0;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}
.print-card__preview-icon {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 14px;
  color: #64748b;
}
.print-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 12px 8px;
  min-width: 0;
}
.print-card__title-block {
  display: flex;
  gap: 8px;
  min-width: 0;
}
.print-card__icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 28px;
  height: 28px;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 2px;
  font-size: 14px;
}
.print-card__title-main {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.print-card__name {
  overflow: hidden;
  color: var(--text-primary, #0f172a);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.print-card__code {
  overflow: hidden;
  color: var(--text-tertiary, #94a3b8);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.print-card__body {
  display: grid;
  gap: 10px;
  flex: 1;
  min-width: 0;
  padding: 0 12px 12px;
}
.print-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.print-card__scenes {
  display: grid;
  gap: 6px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 2px;
}
.print-card__scenes-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 11px;
}
.print-card__scene-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.print-card__scene-empty {
  color: #94a3b8;
  font-size: 12px;
}
.print-scene-select {
  width: 100%;
}
.print-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  padding: 8px 12px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
}
.print-card__meta {
  min-width: 0;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.print-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.print-card__actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex: none;
}
.print-card__action,
.print-card__more {
  margin: 0;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: var(--primary-color, #2563eb);
  font-size: 12px;
  line-height: 20px;
  cursor: pointer;
}

.print-card__action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.print-card__action:hover,
.print-card__more:hover {
  color: var(--primary-color-hover, #1d4ed8);
}
.print-card__more {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  color: #64748b;
  font-size: 14px;
}
.print-card__more:disabled,
.print-card__action:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.print-card__sep {
  width: 1px;
  height: 12px;
  margin: 0 2px;
  background: #e2e8f0;
}
</style>
