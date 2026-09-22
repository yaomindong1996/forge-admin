<script setup>
import { NAlert, NButton, NCard, NDropdown, NEmpty, NModal, NPagination, NSelect, NSpace, NSpin, NTable } from 'naive-ui'
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
  applicationCode: { type: String, default: '' },
  source: { type: Object, default: null },
  sources: { type: Array, default: () => [] },
  lockSource: Boolean,
})
const router = useRouter()
const route = useRoute()
const store = usePrintTemplateStore()
const user = useUserStore()
const { dict } = useDict('sys_print_scene')
const source = computed(() => props.source)
const appId = computed(() => props.applicationId)
const canManage = computed(() => hasPrintPermission(user, 'print:template:manage'))
const creating = ref(false)
const busy = ref(false)
const bindings = ref([])
const sceneOptions = computed(() => dict.value.sys_print_scene?.length ? dict.value.sys_print_scene : FALLBACK_PRINT_SCENE_OPTIONS)
watch(appId, (value) => {
  store.listGeneration++
  store.items = []
  store.total = 0
  creating.value = false
  bindings.value = []
  if (value)
    store.list(value, 1, source.value?.pageId)
  loadBindings()
}, { immediate: true })
onBeforeUnmount(() => {
  store.listGeneration++
  store.items = []
  bindings.value = []
})
watch(source, () => {
  creating.value = false
  if (appId.value)
    store.list(appId.value, 1, source.value?.pageId)
  loadBindings()
})
async function loadBindings() {
  const payload = printSourcePayload(source.value)
  if (!payload) {
    bindings.value = []
    return
  }
  try {
    const { data } = await api.printBindings(payload)
    bindings.value = data || []
  }
  catch (error) {
    bindings.value = []
    store.error = error.message || '无法读取打印场景'
  }
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
async function act(action) {
  if (busy.value)
    return
  const applicationId = appId.value
  busy.value = true
  store.error = ''
  try {
    await action()
    if (appId.value === applicationId)
      await store.list(applicationId, store.pageNum, source.value?.pageId)
  }
  catch (error) {
    if (appId.value === applicationId)
      store.error = error.message || '操作失败'
  }
  finally {
    busy.value = false
  }
}
async function copy(row) {
  await act(async () => {
    const copiedScenes = rowScenes(row)
    const { data } = await api.copyPrintTemplate(row.id, { expectedRevision: row.draftRevision, templateCode: newPrintTemplateCode(), templateName: `${row.templateName.slice(0, 95)} 副本` })
    await syncPrintTemplateScenes({
      source: printSourcePayload(data.source) || printSourcePayload(source.value),
      templateId: data.id,
      scenes: copiedScenes,
      bindings: [],
      save: api.savePrintBinding,
      remove: api.deletePrintBinding,
    })
    design(data)
  })
}
async function changeScenes(row, scenes) {
  await act(async () => {
    await syncPrintTemplateScenes({
      source: printSourcePayload(row.source) || printSourcePayload(source.value),
      templateId: row.id,
      scenes,
      bindings: bindings.value,
      save: api.savePrintBinding,
      remove: api.deletePrintBinding,
    })
  })
  await loadBindings()
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
    return act(() => api.changePrintTemplateStatus(row.id, { expectedRevision: row.draftRevision, status: Number(row.status) === 1 ? 0 : 1 }))
}
function sourceLabel(row) {
  return props.sources.find(item => item.source.pageId === row.source.pageId && item.source.objectCode === row.source.objectCode)?.label || '所属表单'
}
async function refresh() {
  if (appId.value)
    await store.list(appId.value, store.pageNum, source.value?.pageId)
  await loadBindings()
}
</script>

<template>
  <NCard :title="lockSource ? undefined : '打印模板'" size="small" :bordered="false" class="print-template-list">
    <template v-if="!lockSource" #header-extra>
      <NSpace>
        <NButton :disabled="!appId || busy" @click="refresh">
          刷新
        </NButton>
        <NButton v-if="canManage" type="primary" :disabled="!source || busy" @click="creating = true">
          新建模板
        </NButton>
      </NSpace>
    </template>
    <NAlert v-if="store.error" type="error">
      {{ store.error }}
    </NAlert>
    <NEmpty v-if="!appId" description="请从当前页面的打印设置进入" />
    <template v-else>
      <div v-if="lockSource" class="print-list-actions">
        <NButton :disabled="!appId || busy" @click="refresh">
          刷新
        </NButton>
        <NButton v-if="canManage" type="primary" :disabled="!source || busy" @click="creating = true">
          新建模板
        </NButton>
      </div>
      <div v-if="!lockSource" class="print-list-context">
        <slot name="source" />
        <p>勾选场景后重新发布应用，即可供业务用户使用。</p>
      </div>
      <NSpin :show="store.listing">
        <NEmpty v-if="!store.items.length && !store.listing" description="暂无打印模板" />
        <div v-else class="print-table-scroll">
          <NTable size="small">
            <thead><tr><th>模板名称</th><th>来源</th><th>使用场景</th><th>设计状态</th><th>启用状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="row in store.items" :key="row.id">
                <td>{{ row.templateName }}</td><td><DictTag dict-type="sys_print_source_type" :value="row.source.sourceType" /> · {{ sourceLabel(row) }}</td>
                <td>
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
                    placeholder="未绑定"
                    :aria-label="`${row.templateName}的使用场景`"
                    @update:value="scenes => changeScenes(row, scenes)"
                  />
                  <NSpace v-else>
                    <DictTag v-for="scene in rowScenes(row)" :key="scene" dict-type="sys_print_scene" :value="scene" />
                    <span v-if="!rowScenes(row).length">未绑定</span>
                  </NSpace>
                </td>
                <td><DictTag dict-type="sys_print_design_status" :value="row.designStatus" /></td>
                <td><DictTag dict-type="sys_normal_disable" :value="row.status" force-tag :type="Number(row.status) === 1 ? 'success' : 'default'" /></td>
                <td>
                  <NSpace>
                    <NButton text type="primary" @click="design(row)">
                      {{ canManage ? '设计' : '查看' }}
                    </NButton>
                    <NDropdown v-if="canManage" :options="moreOptions(row)" @select="key => more(key, row)">
                      <NButton text :disabled="busy" :aria-label="`${row.templateName}的更多操作`">
                        更多
                      </NButton>
                    </NDropdown>
                  </NSpace>
                </td>
              </tr>
            </tbody>
          </NTable>
        </div>
      </NSpin>
      <NPagination v-if="store.total > 20" :page="store.pageNum" :page-size="20" :item-count="store.total" :disabled="store.listing" style="margin-top: 16px" @update:page="page => store.list(appId, page, source?.pageId)" />
    </template>
    <NModal :show="!!deleting" preset="dialog" title="删除打印模板" positive-text="删除" negative-text="取消" :loading="busy" @negative-click="deleting = null" @close="deleting = null" @positive-click="async () => { const row = deleting; deleting = null; await act(() => api.deletePrintTemplate(row.id, row.draftRevision)) }">
      存在绑定或发布引用时无法删除。
    </NModal>
    <PrintTemplateCreate v-model:show="creating" :source="source" @created="design" />
  </NCard>
</template>

<style scoped>
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
  color: var(--text-tertiary);
  font-size: 12px;
}
.print-table-scroll {
  overflow-x: auto;
}
.print-table-scroll table {
  min-width: 860px;
}
.print-scene-select {
  min-width: 168px;
  max-width: 240px;
}
</style>
