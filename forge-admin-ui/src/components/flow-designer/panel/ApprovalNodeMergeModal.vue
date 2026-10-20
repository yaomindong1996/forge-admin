<script setup>
import { NAlert, NButton, NEmpty, NModal, NRadio, NRadioGroup } from 'naive-ui'
import { computed, watch } from 'vue'
import { useApprovalNodeMergeStore } from '@/stores/designer/approvalNodeMergeStore.js'
import { useFlowOverlayTarget } from '../composables/useFlowOverlayTarget.js'
import { getApprovalMergeCandidates } from '../utils/approval-node-merge.js'

const props = defineProps({ ownerId: { type: String, required: true }, flow: { type: Object, required: true } })
const emit = defineEmits(['confirm'])
const state = useApprovalNodeMergeStore()
const overlayTarget = useFlowOverlayTarget()
const visible = computed(() => state.ownerId === props.ownerId)
const source = computed(() => props.flow.nodes.find(node => node.id === state.sourceId))
const candidates = computed(() => {
  if (!visible.value)
    return []
  const outgoing = props.flow.edges.filter(edge => edge.source === state.sourceId)
  const nextId = outgoing.length === 1 ? outgoing[0].target : null
  // 不把流程里所有无关审批塞进选择器；相同后继中的不兼容节点仍展示具体原因。
  return getApprovalMergeCandidates(props.flow, state.sourceId).filter(item => !nextId
    || props.flow.edges.some(edge => edge.source === item.node.id && edge.target === nextId))
})
const selected = computed(() => candidates.value.find(item => item.node.id === state.targetId))
const nextNode = computed(() => props.flow.nodes.find(node => node.id === selected.value?.nextId))
const canConfirm = computed(() => selected.value?.eligible && !state.blockedReason)

watch(candidates, (items) => {
  if (!visible.value)
    return
  if (!items.some(item => item.node.id === state.targetId && item.eligible))
    state.targetId = items.find(item => item.eligible)?.node.id || null
}, { immediate: true })

function previousNames(nodeId) {
  return props.flow.edges.filter(edge => edge.target === nodeId)
    .map(edge => props.flow.nodes.find(node => node.id === edge.source)?.name || '未命名节点')
    .join('、')
}
function close() {
  state.close(props.ownerId)
}
</script>

<template>
  <!-- 只修改当前草稿；选择来源/目标后仍须人工确认，不自动发布。 -->
  <NModal
    v-if="visible"
    :show="visible" preset="card" title="合并审批节点" :to="overlayTarget || undefined"
    :mask-closable="false" :style="{ width: 'min(600px, calc(100vw - 32px))', maxHeight: 'calc(100dvh - 32px)' }"
    :content-style="{ overflowY: 'auto', minHeight: 0 }"
    class="approval-node-merge-modal" @update:show="!$event && close()"
  >
    <p class="merge-intro">
      将“{{ source?.name || '当前节点' }}”合并到下方节点，两条支路共用保留的审批节点。
    </p>
    <NAlert v-if="state.blockedReason" type="warning" :show-icon="false">
      {{ state.blockedReason }}
    </NAlert>
    <!-- 候选来源于当前真实流程图，不能仅凭审批人摘要判断可合并。 -->
    <NRadioGroup v-else v-model:value="state.targetId" :name="`merge-${ownerId}`" class="merge-candidates">
      <NRadio
        v-for="item in candidates" :key="item.node.id" :value="item.node.id" :disabled="!item.eligible"
        class="merge-candidate" :class="{ 'is-selected': state.targetId === item.node.id }"
      >
        <span class="merge-candidate-title">{{ item.node.name || '未命名审批' }}</span>
        <span class="merge-candidate-meta">前置节点：{{ previousNames(item.node.id) }}</span>
        <span class="merge-candidate-reason">{{ item.eligible ? '审批配置一致，可合并' : item.reason }}</span>
        <span v-if="item.differenceKeys?.length" class="merge-candidate-meta">差异字段：{{ item.differenceKeys.join('、') }}</span>
      </NRadio>
      <NEmpty v-if="!candidates.length" size="small" description="没有后续节点相同的其他审批节点" />
    </NRadioGroup>
    <NAlert v-if="canConfirm" type="info" :show-icon="false" class="merge-summary">
      保留“{{ selected.node.name }}”（{{ selected.node.id }}）及全部审批配置，移除“{{ source?.name }}”（{{ source?.id }}）；
      原分支条件、默认路径保持不变，共同后续为“{{ nextNode?.name || '未命名节点' }}”。
    </NAlert>
    <p class="merge-note">
      仅修改当前流程草稿，可撤销。保存并发布新版本后供新流程使用，已有运行实例不会随之合并。
      若外部业务按节点 ID 绑定规则或回调，发布前请同步核对。
    </p>
    <NAlert v-if="state.error" type="error" :show-icon="false">
      {{ state.error }}
    </NAlert>
    <template #footer>
      <div class="merge-footer">
        <NButton @click="close">
          取消
        </NButton>
        <NButton type="primary" :disabled="!canConfirm" @click="emit('confirm')">
          确认合并
        </NButton>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
.approval-node-merge-modal {
  display: flex;
  flex-direction: column;
}
.merge-intro {
  margin: 0 0 14px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
}
.merge-candidates {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.merge-candidate.n-radio {
  display: flex;
  align-items: flex-start;
  box-sizing: border-box;
  width: 100%;
  margin: 0;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  padding: 12px;
  white-space: normal;
}
.merge-candidate.is-selected {
  border-color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 5%, var(--bg-primary));
}
.merge-candidate :deep(.n-radio__label) {
  min-width: 0;
  flex: 1;
}
.merge-candidate-title {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
}
.merge-candidate-meta,
.merge-candidate-reason {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.merge-summary {
  margin-top: 14px;
}
.merge-note {
  margin: 14px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}
.merge-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
