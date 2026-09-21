<script setup>
import { NAlert, NButton, NCard, NDropdown, NEmpty, NModal, NPagination, NSpace, NSpin, NTable } from 'naive-ui'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as api from '@/api/print'
import DictTag from '@/components/DictTag.vue'
import { hasPrintPermission } from '@/components/print/management/printPermissions'
import PrintTemplateCreate from '@/components/print/management/PrintTemplateCreate.vue'
import { useUserStore } from '@/store'
import { usePrintTemplateStore } from '@/stores/print/printTemplateStore'

const props = defineProps({ applicationId: { type: String, default: null }, source: { type: Object, default: null }, sources: { type: Array, default: () => [] } })
const router = useRouter()
const store = usePrintTemplateStore()
const user = useUserStore()
const source = computed(() => props.source)
const appId = computed(() => props.applicationId)
const canManage = computed(() => hasPrintPermission(user, 'print:template:manage'))
const creating = ref(false)
const busy = ref(false)
watch(appId, (value) => {
  store.listGeneration++
  store.items = []
  store.total = 0
  creating.value = false
  if (value)
    store.list(value)
}, { immediate: true })
onBeforeUnmount(() => {
  store.listGeneration++
  store.items = []
})
watch(source, () => {
  creating.value = false
})
function design(row) {
  router.push({ path: '/print/designer', query: { templateId: String(row.id) } })
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
      await store.list(applicationId, store.pageNum)
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
    const { data } = await api.copyPrintTemplate(row.id, { expectedRevision: row.draftRevision, templateCode: `print_${crypto.randomUUID().replaceAll('-', '')}`, templateName: `${row.templateName.slice(0, 95)} 副本` })
    design(data)
  })
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
</script>

<template>
  <NCard title="打印模板" size="small" :bordered="false" class="print-template-list">
    <template #header-extra>
      <NSpace>
        <NButton :disabled="!appId || busy" @click="store.list(appId, store.pageNum)">
          刷新
        </NButton><NButton v-if="canManage" type="primary" :disabled="!source || busy" @click="creating = true">
          新建模板
        </NButton>
      </NSpace>
    </template>
    <NAlert v-if="store.error" type="error">
      {{ store.error }}
    </NAlert>
    <NEmpty v-if="!appId" description="请从所属应用的表单进入打印模板管理" />
    <template v-else>
      <div class="print-list-context">
        <slot name="source" /><p>模板发布并绑定列表或详情后，重新发布应用即可供业务用户使用。</p>
      </div>
      <NSpin :show="store.listing">
        <NEmpty v-if="!store.items.length && !store.listing" description="暂无打印模板" />
        <div v-else class="print-table-scroll">
          <NTable size="small">
            <thead><tr><th>模板名称</th><th>来源</th><th>设计状态</th><th>启用状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="row in store.items" :key="row.id">
                <td>{{ row.templateName }}</td><td><DictTag dict-type="sys_print_source_type" :value="row.source.sourceType" /> · {{ sourceLabel(row) }}</td>
                <td><DictTag dict-type="sys_print_design_status" :value="row.designStatus" /></td><td><DictTag dict-type="sys_normal_disable" :value="row.status" /></td>
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
      <NPagination v-if="store.total > 20" :page="store.pageNum" :page-size="20" :item-count="store.total" :disabled="store.listing" style="margin-top: 16px" @update:page="page => store.list(appId, page)" />
    </template>
    <NModal :show="!!deleting" preset="dialog" title="删除打印模板" positive-text="删除" negative-text="取消" :loading="busy" @negative-click="deleting = null" @close="deleting = null" @positive-click="async () => { const row = deleting; deleting = null; await act(() => api.deletePrintTemplate(row.id, row.draftRevision)) }">
      存在绑定或发布引用时无法删除。
    </NModal>
    <PrintTemplateCreate v-model:show="creating" :source="source" @created="design" />
  </NCard>
</template>

<style scoped>
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
  min-width: 620px;
}
</style>
