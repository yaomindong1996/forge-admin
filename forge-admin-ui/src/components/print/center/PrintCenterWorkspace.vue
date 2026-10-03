<script setup>
import {
  NAlert,
  NButton,
  NModal,
  NSpin,
  NTag,
} from 'naive-ui'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import IllustratedEmpty from '@/components/common/IllustratedEmpty.vue'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import { hasPrintPermission } from '@/components/print/management/printPermissions'
import PrintTemplateList from '@/components/print/management/PrintTemplateList.vue'
import { useUserStore } from '@/store'
import { usePrintCenterStore } from '@/stores/print/printCenterStore'
import PrintSourceForm from './PrintSourceForm.vue'
import PrintSourceList from './PrintSourceList.vue'

const route = useRoute()
const router = useRouter()
const store = usePrintCenterStore()
const user = useUserStore()
const editing = ref(false)
const creating = ref(false)
const deleting = ref(false)
const usageExpanded = ref(false)
const source = computed(() => store.selected)
const sourceIdentity = computed(() => store.sourceIdentity)
const typeLabel = computed(() => source.value?.sourceType === 'DATASET' ? '数据集' : '代码业务')
const statusLabel = computed(() => Number(source.value?.status) === 1 ? '已启用' : '已停用')
const canManageSource = computed(() => hasPrintPermission(user, 'print:source:manage'))
const usageSnippet = computed(() => {
  if (!source.value)
    return ''
  const sourceType = source.value.sourceType === 'DATASET' ? 'DATASET' : 'SERVICE'
  const sourceCode = source.value.sourceCode || ''
  const objectCode = source.value.objectCode || sourceCode
  return `<BusinessPrintButton
  source-code="${sourceCode}"
  source-type="${sourceType}"
  object-code="${objectCode}"
  :record-id="row.id"
  scene="DETAIL"
/>`
})

async function copyUsageSnippet() {
  const text = usageSnippet.value
  if (!text)
    return
  try {
    await navigator.clipboard.writeText(text)
    window.$message?.success?.('已复制接入代码')
  }
  catch {
    window.$message?.error?.('复制失败，请手动选择代码')
  }
}

function toggleUsageExpanded() {
  usageExpanded.value = !usageExpanded.value
}

onMounted(() => store.load(route.query.businessSourceId))
onBeforeUnmount(() => store.clear())

watch(() => store.selectedId, (id) => {
  if (String(route.query.businessSourceId || '') === String(id || ''))
    return
  router.replace({
    query: { ...route.query, businessSourceId: id || undefined },
  })
})

watch(() => route.query.businessSourceId, (id) => {
  if (id && String(id) !== String(store.selectedId || ''))
    store.select(id)
})

async function removeSource() {
  try {
    await store.remove()
    deleting.value = false
  }
  catch (error) {
    store.error = error.message || '删除失败'
  }
  return false
}

async function toggleSource() {
  try {
    await store.toggle()
  }
  catch (error) {
    store.error = error.message || '更新状态失败'
  }
}
</script>

<template>
  <div class="print-center-page">
    <!-- 左：可打印业务；右：来源操作 + 模板（含场景挂载） -->
    <MasterDetailWorkspace :aside-width="250">
      <template #aside>
        <PrintSourceList :can-manage="canManageSource" @create="creating = true" />
      </template>

      <div class="print-center-main">
        <NAlert v-if="store.error" type="error" closable @close="store.error = ''">
          {{ store.error }}
        </NAlert>
        <NSpin :show="store.detailLoading" class="print-center-main__spin">
          <template v-if="source">
            <!-- 当前业务摘要与来源操作 -->
            <header class="print-center-head">
              <div class="print-center-head__identity">
                <span class="print-center-head__icon" aria-hidden="true">
                  <i :class="source.sourceType === 'DATASET' ? 'i-lucide:database' : 'i-lucide:braces'" />
                </span>
                <div>
                  <div class="print-center-head__title">
                    <strong>{{ source.sourceName }}</strong>
                    <NTag size="small" :type="Number(source.status) === 1 ? 'success' : 'default'">
                      {{ statusLabel }}
                    </NTag>
                  </div>
                  <span>{{ source.sourceCode }} · {{ typeLabel }}</span>
                </div>
              </div>
              <div v-if="canManageSource" class="print-center-head__actions">
                <NButton size="small" @click="editing = true">
                  <template #icon>
                    <i class="i-lucide:settings-2" />
                  </template>
                  业务设置
                </NButton>
                <NButton size="small" :loading="store.saving" @click="toggleSource">
                  <template #icon>
                    <i :class="Number(source.status) === 1 ? 'i-lucide:pause-circle' : 'i-lucide:play-circle'" />
                  </template>
                  {{ Number(source.status) === 1 ? '停用' : '启用' }}
                </NButton>
                <NButton size="small" quaternary type="error" @click="deleting = true">
                  <template #icon>
                    <i class="i-lucide:trash-2" />
                  </template>
                  删除
                </NButton>
              </div>
            </header>

            <p class="print-center-guide">
              在本页完成：新建/设计模板 → 发布模板 → 再点启用。业务打印固定详情场景，按钮传 scene="DETAIL"。
            </p>

            <!-- 业务页怎么传参：默认收起，需要时再展开复制 -->
            <div class="print-center-usage">
              <button
                type="button"
                class="print-center-usage__toggle"
                :aria-expanded="usageExpanded"
                @click="toggleUsageExpanded"
              >
                <span class="print-center-usage__toggle-label">
                  业务页接入参数
                  <span class="print-center-usage__toggle-hint">
                    sourceType / scene / sourceCode
                  </span>
                </span>
                <i
                  class="print-center-usage__chevron"
                  :class="usageExpanded ? 'i-lucide:chevron-up' : 'i-lucide:chevron-down'"
                />
              </button>
              <div v-show="usageExpanded" class="print-center-usage__body">
                <div class="print-center-usage__head">
                  <span>
                    <code>sourceType</code> 看左侧业务类型（数据集=DATASET，代码业务=SERVICE）；
                    <code>scene</code> 业务打印固定传 <code>DETAIL</code>；
                    <code>sourceCode</code> 是调用编码。
                  </span>
                  <NButton size="tiny" secondary @click.stop="copyUsageSnippet">
                    <template #icon>
                      <i class="i-lucide:copy" />
                    </template>
                    复制代码
                  </NButton>
                </div>
                <pre class="print-center-usage__code">{{ usageSnippet }}</pre>
              </div>
            </div>

            <!-- 模板卡片内已含「挂载位置」场景绑定 -->
            <div class="print-center-pane">
              <PrintTemplateList
                :key="String(source.id)"
                :business-source-id="String(source.id)"
                :source="sourceIdentity"
                :sources="store.sources"
                :show-bindings="false"
                allow-create
                lock-source
              />
            </div>
          </template>
          <IllustratedEmpty v-else description="左侧选择一个业务，或点「新增」登记可打印业务" />
        </NSpin>
      </div>
    </MasterDetailWorkspace>

    <PrintSourceForm v-model:show="creating" />
    <PrintSourceForm v-model:show="editing" :source="source" />
    <NModal
      v-model:show="deleting"
      preset="dialog"
      title="删除可打印业务"
      positive-text="删除"
      negative-text="取消"
      :loading="store.saving"
      @positive-click="removeSource"
    >
      仅未关联模板时可以删除。已在用的建议先停用。
    </NModal>
  </div>
</template>

<style scoped>
.print-center-page {
  height: 100%;
  min-height: 0;
}

.print-center-main,
.print-center-main__spin {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.print-center-main__spin :deep(.n-spin-content) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.print-center-main > .n-alert {
  margin: 10px 12px 0;
}

.print-center-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px 8px;
}

.print-center-head__identity,
.print-center-head__title,
.print-center-head__actions {
  display: flex;
  align-items: center;
}

.print-center-head__identity {
  min-width: 0;
  gap: 10px;
}

.print-center-head__identity > div {
  min-width: 0;
}

.print-center-head__icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 3px;
  color: var(--primary-color, #1677ff);
  background: var(--gray-100, #f6f8fb);
  font-size: 16px;
}

.print-center-head__title {
  gap: 8px;
}

.print-center-head__title strong {
  overflow: hidden;
  color: var(--text-primary, #1d2129);
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.print-center-head__identity > div > span {
  display: block;
  margin-top: 2px;
  color: var(--text-tertiary, #86909c);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
}

.print-center-head__actions {
  flex: 0 0 auto;
  gap: 6px;
}

.print-center-guide {
  margin: 0 16px 8px;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 18px;
}

.print-center-usage {
  margin: 0 16px 8px;
  padding: 0;
  border: 1px solid var(--border-light, #e5e6eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  overflow: hidden;
}

.print-center-usage__toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  margin: 0;
  padding: 8px 12px;
  border: 0;
  background: transparent;
  color: var(--text-primary, #1d2129);
  cursor: pointer;
  text-align: left;
}

.print-center-usage__toggle:hover {
  background: rgba(0, 0, 0, 0.02);
}

.print-center-usage__toggle-label {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.print-center-usage__toggle-hint {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  font-weight: 400;
}

.print-center-usage__chevron {
  flex-shrink: 0;
  color: var(--text-tertiary, #86909c);
  font-size: 14px;
}

.print-center-usage__body {
  padding: 0 12px 10px;
}

.print-center-usage__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.print-center-usage__head span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 18px;
}

.print-center-usage__head code {
  padding: 0 3px;
  border-radius: 3px;
  background: var(--gray-100, #f2f3f5);
}

.print-center-usage__code {
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  border-radius: 4px;
  background: var(--gray-100, #f6f8fb);
  color: var(--text-primary, #1d2129);
  font-size: 12px;
  line-height: 18px;
  white-space: pre;
}

.print-center-pane {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 16px 16px;
  scrollbar-gutter: stable;
}

@media (max-width: 720px) {
  .print-center-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .print-center-head__actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
