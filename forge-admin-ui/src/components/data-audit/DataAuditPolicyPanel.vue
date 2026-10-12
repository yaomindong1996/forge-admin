<template>
  <section class="data-audit-policy-panel">
    <header v-if="!hideTitle" class="policy-title">
      <div>
        <h4>数据变更审计</h4>
        <p>记录业务数据每次保存后的实际变化，便于追溯谁在何时改了哪些内容。</p>
      </div>
    </header>

    <n-spin :show="loading">
      <div class="policy-status" :class="draft.enabled ? 'is-enabled' : 'is-disabled'">
        <span class="status-mark" aria-hidden="true">
          <i :class="draft.enabled ? 'i-lucide:shield-check' : 'i-lucide:shield-off'" />
        </span>
        <div class="status-copy">
          <div class="status-line">
            <strong>{{ draft.enabled ? '正在记录数据变化' : '当前未记录新的变化' }}</strong>
            <n-tag :type="draft.enabled ? 'success' : 'default'" size="small" :bordered="false">
              {{ draft.enabled ? '已开启' : '已停用' }}
            </n-tag>
          </div>
          <p>{{ statusDescription }}</p>
          <span v-if="policy.enabledAt">首次开启于 {{ policy.enabledAt }}</span>
        </div>
      </div>

      <div class="policy-settings">
        <div class="setting-row">
          <div class="setting-copy">
            <strong>记录数据变化</strong>
            <p>开启后，每次新增、修改和删除都会保存操作者、时间及字段前后值。</p>
          </div>
          <n-switch v-model:value="draft.enabled" :disabled="!canConfig" aria-label="记录数据变化" />
        </div>

        <div class="setting-row">
          <div class="setting-copy">
            <strong>要求填写变更原因</strong>
            <p>开启后，用户修改或删除数据时必须先说明原因；关闭后原因可不填。</p>
          </div>
          <n-switch v-model:value="draft.reasonRequired" :disabled="!canConfig" aria-label="要求填写变更原因" />
        </div>

        <div class="setting-row">
          <div class="setting-copy">
            <strong>在业务详情显示变更记录</strong>
            <p>用户可在记录详情中查看已授权的历史。关闭只隐藏入口，不会停止采集或删除历史。</p>
          </div>
          <n-switch v-model:value="draft.showInDetail" :disabled="!canConfig" aria-label="在业务详情显示变更记录" />
        </div>
      </div>

      <n-alert v-if="draft.enabled && !coveragePassed" type="error" :bordered="false" class="coverage-alert">
        当前对象还有 {{ blockedItems.length }} 项检查未通过，处理后才能开启数据变更审计。
      </n-alert>

      <n-collapse v-if="policy.coverageItems?.length" class="coverage-collapse">
        <n-collapse-item name="coverage">
          <template #header>
            <span class="coverage-title">启用检查</span>
            <n-tag :type="coveragePassed ? 'success' : 'error'" size="small" :bordered="false">
              {{ coveragePassed ? '全部通过' : `${blockedItems.length} 项阻断` }}
            </n-tag>
          </template>
          <ul class="coverage-list">
            <li v-for="item in policy.coverageItems" :key="item.code">
              <i :class="item.passed ? 'i-lucide:check' : 'i-lucide:x'" />
              <span><strong>{{ item.label }}</strong>{{ item.passed ? '' : `：${item.message}` }}</span>
            </li>
          </ul>
        </n-collapse-item>
      </n-collapse>

      <footer v-if="canConfig" class="policy-actions">
        <span>{{ dirty ? '设置尚未保存' : lastSavedText }}</span>
        <div>
          <n-button size="small" :disabled="!dirty || saving" @click="resetDraft">
            撤销修改
          </n-button>
          <n-button
            size="small"
            type="primary"
            :loading="saving"
            :disabled="!dirty || (draft.enabled && !coveragePassed)"
            @click="savePolicy"
          >
            保存设置
          </n-button>
        </div>
      </footer>
    </n-spin>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { dataAuditPolicy, updateDataAuditPolicy } from '@/api/data-audit'
import { useUserStore } from '@/store'

const props = defineProps({
  objectId: { type: [String, Number], default: '' },
  hideTitle: { type: Boolean, default: false },
})

const emit = defineEmits(['updated'])
const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const policy = ref({})
const draft = reactive({ enabled: false, reasonRequired: true, showInDetail: true })

const canConfig = computed(() => {
  if (userStore.isAdmin)
    return true
  const grants = userStore.permissions || []
  return grants.includes('ai:dataAudit:config') || grants.includes('*:*:*')
})
const coveragePassed = computed(() => (policy.value.coverageItems || []).every(item => item.passed))
const blockedItems = computed(() => (policy.value.coverageItems || []).filter(item => !item.passed))
const dirty = computed(() => draft.enabled !== (policy.value.enabled === true)
  || draft.reasonRequired !== (policy.value.reasonRequired !== false)
  || draft.showInDetail !== (policy.value.showInDetail !== false))
const statusDescription = computed(() => {
  if (draft.enabled)
    return '保存后开始记录后续变化。开启前发生的修改不会自动补录。'
  if (policy.value.enabledAt)
    return '保存后停止记录新的变化，停用前的历史仍可查询；重新开启后将继续追加。'
  return '开启并保存后才会产生变更记录，之前的数据变化不会自动补录。'
})
const lastSavedText = computed(() => policy.value.updatedAt ? `最近保存 ${policy.value.updatedAt}` : '设置已保存')

function syncDraft() {
  draft.enabled = policy.value.enabled === true
  draft.reasonRequired = policy.value.reasonRequired !== false
  draft.showInDetail = policy.value.showInDetail !== false
}

async function load() {
  if (!props.objectId)
    return
  loading.value = true
  try {
    const res = await dataAuditPolicy(props.objectId)
    if (res.code === 200) {
      policy.value = res.data || {}
      syncDraft()
    }
  }
  catch (error) {
    window.$message?.error?.(error?.message || '加载数据变更审计设置失败')
  }
  finally {
    loading.value = false
  }
}

function resetDraft() {
  syncDraft()
}

function confirmStopCollecting() {
  if (!window.$dialog?.warning)
    return Promise.resolve(true)
  return new Promise((resolve) => {
    window.$dialog.warning({
      title: '停用数据变更审计？',
      content: '停用后不再记录新的数据变化。已有历史会继续保留并可查询，重新开启前的变化不会补录。',
      positiveText: '确认停用',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
}

async function savePolicy() {
  if (policy.value.enabled === true && !draft.enabled && !(await confirmStopCollecting())) {
    draft.enabled = true
    return
  }
  saving.value = true
  try {
    const res = await updateDataAuditPolicy(props.objectId, {
      enabled: draft.enabled,
      reasonRequired: draft.reasonRequired,
      showInDetail: draft.showInDetail,
    })
    if (res.code === 200) {
      policy.value = res.data || {}
      syncDraft()
      emit('updated', policy.value)
      window.$message?.success?.('数据变更审计设置已保存')
    }
  }
  catch (error) {
    window.$message?.error?.(error?.message || '保存数据变更审计设置失败')
  }
  finally {
    saving.value = false
  }
}

watch(() => props.objectId, load)
onMounted(load)
</script>

<style scoped>
.data-audit-policy-panel {
  display: grid;
  gap: 12px;
  color: var(--text-primary, #111827);
}

.data-audit-policy-panel :deep(.n-spin-content) {
  display: grid;
  gap: 12px;
}

.policy-title h4,
.policy-title p,
.policy-status p,
.setting-copy p {
  margin: 0;
}

.policy-title h4 {
  font-size: 15px;
}

.policy-title p,
.policy-status p,
.policy-status span,
.setting-copy p,
.policy-actions > span {
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

.policy-title p {
  margin-top: 4px;
}

.policy-status {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary);
}

.status-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 4px;
  background: var(--bg-primary, #fff);
  color: var(--text-tertiary, #64748b);
  font-size: 18px;
}

.policy-status.is-enabled .status-mark {
  color: var(--primary-color, #2563eb);
  border-color: color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent);
}

.status-copy {
  min-width: 0;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 3px;
}

.policy-settings {
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 70px;
  padding: 12px 14px;
}

.setting-row + .setting-row {
  border-top: 1px solid var(--border-light, #e5e7eb);
}

.setting-copy {
  min-width: 0;
}

.setting-copy strong {
  display: block;
  margin-bottom: 3px;
  font-size: 13px;
  font-weight: 600;
}

.coverage-alert {
  margin: 0;
}

.coverage-title {
  margin-right: 8px;
  font-size: 13px;
  font-weight: 600;
}

.coverage-list {
  display: grid;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.coverage-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
}

.coverage-list li i {
  margin-top: 2px;
  color: var(--text-tertiary, #64748b);
}

.policy-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 2px;
}

.policy-actions > div {
  display: flex;
  gap: 8px;
}

@media (max-width: 720px) {
  .setting-row,
  .policy-actions {
    align-items: flex-start;
  }

  .policy-actions {
    flex-direction: column;
  }
}
</style>
