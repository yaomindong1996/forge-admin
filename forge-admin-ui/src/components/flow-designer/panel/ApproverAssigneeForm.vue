<script setup>
/**
 * ApproverAssigneeForm — 审批人选择表单（assignee Tab）
 *
 * 字段：
 *   - taskType: assignee / candidateUsers / candidateGroups
 *   - assignee（taskType=assignee 下）：4 种静态变量 / custom / spel / 自由表达式
 *   - candidateUsers / candidateGroups
 *   - 表单引用（formKey / formJson / formUrl，简化为 formKey 文本输入）
 *
 * 与 NodePropertiesPanel.vue 字段一致。candidate 选择 UI 在 forge-admin-ui 已有 UserSelectModal
 * 等组件，本面板直接用文本输入 + 后续 Task 27 接入选人弹窗。
 */
import { computed } from 'vue'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const TASK_TYPE_OPTIONS = [
  { label: '指定审批人', value: 'assignee' },
  { label: '候选用户', value: 'candidateUsers' },
  { label: '候选角色', value: 'candidateGroups' },
]

const ASSIGNEE_OPTIONS = [
  { label: '发起人', value: '${initiator}' },
  { label: '上级领导', value: '${initiatorLeader}' },
  { label: '部门主管', value: '${deptManager}' },
  { label: 'HR', value: '${hr}' },
  { label: '指定人员（${user_xxx}）', value: 'custom' },
  { label: 'SPEL 表达式', value: 'spel' },
]

const SPEL_TEMPLATES = [
  { label: '部门主管（DEPT_LEADER）', value: 'DEPT_LEADER' },
  { label: '直属领导（DIRECT_LEADER）', value: 'DIRECT_LEADER' },
  { label: '自定义', value: '' },
]

const taskType = useField('taskType', 'assignee')
const assignee = useField('assignee', '')
const assigneeExpr = useField('assigneeExpr', '')
const assigneeUserName = useField('assigneeUserName', '')
const spelTemplate = useField('spelTemplate', '')
const candidateUsersText = computed({
  get: () => (props.config.candidateUsers || []).join(','),
  set: v => emit('update:config', {
    candidateUsers: String(v || '').split(/[,，\s]+/).filter(Boolean),
  }),
})
const candidateGroupsText = computed({
  get: () => (props.config.candidateGroups || []).join(','),
  set: v => emit('update:config', {
    candidateGroups: String(v || '').split(/[,，\s]+/).filter(Boolean),
  }),
})
const formKey = useField('formKey', '')

function useField(name, fallback = '') {
  return computed({
    get: () => props.config?.[name] ?? fallback,
    set: v => emit('update:config', { [name]: v }),
  })
}

const isCustomAssignee = computed(() => assignee.value === 'custom')
const isSpelAssignee = computed(() => assignee.value === 'spel')
</script>

<template>
  <div class="space-y-3">
    <n-form-item label="任务类型" label-placement="left">
      <n-select
        v-model:value="taskType"
        :options="TASK_TYPE_OPTIONS"
        :disabled="readonly"
      />
    </n-form-item>

    <template v-if="taskType === 'assignee'">
      <n-form-item label="审批人" label-placement="left">
        <n-select
          v-model:value="assignee"
          :options="ASSIGNEE_OPTIONS"
          :disabled="readonly"
          placeholder="请选择审批人"
        />
      </n-form-item>

      <n-form-item v-if="isCustomAssignee" label="人员表达式" label-placement="left">
        <n-input
          v-model:value="assigneeExpr"
          placeholder="${user_1001}"
          :disabled="readonly"
        />
      </n-form-item>
      <n-form-item v-if="isCustomAssignee" label="审批人姓名" label-placement="left">
        <n-input
          v-model:value="assigneeUserName"
          placeholder="可选：用于卡片显示"
          :disabled="readonly"
        />
      </n-form-item>

      <n-form-item v-if="isSpelAssignee" label="SPEL 模板" label-placement="left">
        <n-select
          v-model:value="spelTemplate"
          :options="SPEL_TEMPLATES"
          :disabled="readonly"
          placeholder="可选模板"
        />
      </n-form-item>
      <n-form-item v-if="isSpelAssignee" label="SPEL 表达式" label-placement="left">
        <n-input
          v-model:value="assigneeExpr"
          placeholder="${spelService.getApprover(...)}"
          :disabled="readonly"
        />
      </n-form-item>
    </template>

    <template v-else-if="taskType === 'candidateUsers'">
      <n-form-item label="候选用户 ID" label-placement="left">
        <n-input
          v-model:value="candidateUsersText"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 4 }"
          placeholder="多个用户 ID 用英文逗号分隔，如 1001,1002"
          :disabled="readonly"
        />
      </n-form-item>
    </template>

    <template v-else-if="taskType === 'candidateGroups'">
      <n-form-item label="候选角色编码" label-placement="left">
        <n-input
          v-model:value="candidateGroupsText"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 4 }"
          placeholder="多个角色编码用英文逗号分隔，如 role_admin,role_audit"
          :disabled="readonly"
        />
      </n-form-item>
    </template>

    <n-divider />

    <n-form-item label="表单 Key" label-placement="left">
      <n-input v-model:value="formKey" placeholder="可选" :disabled="readonly" />
    </n-form-item>
  </div>
</template>
