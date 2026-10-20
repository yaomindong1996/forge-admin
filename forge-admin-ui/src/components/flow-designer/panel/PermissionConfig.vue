<script setup>
/**
 * PermissionConfig — 8 个操作权限布尔开关
 *
 * 字段名对齐 user-task-parser DEFAULT_PERMISSIONS。
 * 指定节点驳回由流程级 allowMultiReturn 控制，不再在节点上展示 allowReturn。
 */
import { computed } from 'vue'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const FIELDS = [
  { key: 'allowApprove', label: '允许通过', desc: '审批人可点击“通过”' },
  { key: 'allowReject', label: '允许驳回', desc: '按流程级驳回策略处理：可能结束流程、退回发起人修改，或走手工设计路径' },
  { key: 'allowRejectToStart', label: '退回发起人修改', desc: '始终保留原流程实例，进入发起人修改节点，修改后沿原流程重提' },
  { key: 'allowDelegate', label: '允许委派', desc: '审批人可委托他人代审' },
  { key: 'allowTerminate', label: '允许终止', desc: '可直接终止流程' },
  { key: 'requireSignature', label: '强制签名', desc: '审批时必须电子签名' },
  { key: 'requireComment', label: '审批意见', desc: '同意或驳回流程时必须填写审批意见' },
]

function bool(key, fallback = false) {
  return computed({
    get: () => Boolean(props.config?.[key] ?? fallback),
    set: v => emit('update:config', { [key]: !!v }),
  })
}

// 创建一组 v-model 引用
const fields = FIELDS.map(({ key, label, desc, fallback }) => ({
  key,
  label,
  desc,
  model: bool(key, fallback),
}))
</script>

<template>
  <div class="permission-config">
    <div
      v-for="f in fields"
      :key="f.key"
      class="permission-row"
    >
      <div class="permission-copy">
        <div class="permission-title">
          {{ f.label }}
        </div>
        <div class="permission-description">
          {{ f.desc }}
        </div>
      </div>
      <n-switch v-model:value="f.model.value" :aria-label="f.label" :disabled="readonly" />
    </div>
  </div>
</template>

<style scoped>
.permission-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
}
.permission-row + .permission-row {
  border-top: 1px solid var(--border-light);
}
.permission-copy {
  min-width: 0;
  flex: 1;
}
.permission-title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
}
.permission-description {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
.permission-row :deep(.n-switch) {
  flex: none;
  margin-top: 2px;
}
</style>
