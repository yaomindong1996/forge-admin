<template>
  <div class="rule-status">
    <NSwitch
      :value="enabled"
      :loading="saving"
      :disabled="saving || confirming"
      :aria-disabled="saving || confirming"
      :aria-label="`${pageName}的数据权限规则`"
      size="small"
      @update:value="confirmChange"
    />
    <span class="rule-status__label">{{ statusLabel }}</span>
  </div>
</template>

<script setup>
import { NSwitch } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { request } from '@/utils/request'

const props = defineProps({
  row: { type: Object, required: true },
  options: { type: Array, default: () => [] },
})
const emit = defineEmits(['refresh', 'updated'])
const currentStatus = ref(Number(props.row.enabled))
const saving = ref(false)
const confirming = ref(false)
const enabled = computed(() => currentStatus.value === 1)
const pageName = computed(() => props.row.resourceName || props.row.resourceCode || '此页面')
const statusLabel = computed(() => {
  const label = props.options.find(option => Number(option.value) === currentStatus.value)?.label
  return label ? (label.startsWith('已') ? label : `已${label}`) : '加载中'
})

watch(() => props.row.enabled, value => currentStatus.value = Number(value))

function confirmChange(nextEnabled) {
  if (saving.value || confirming.value || nextEnabled === enabled.value) {
    return
  }
  const action = nextEnabled ? '启用' : '禁用'
  const expectedEnabled = currentStatus.value
  confirming.value = true
  const release = () => {
    confirming.value = false
  }
  window.$dialog.warning({
    title: `${action}「${pageName.value}」的数据权限规则？`,
    content: nextEnabled
      ? '启用后，将按角色授权的数据范围过滤相关查询。已打开的业务页面需要重新查询。'
      : '禁用后，此规则将停止限制数据可见范围，相关人员可能看到更多数据。配置会保留，可随时重新启用。',
    positiveText: `确认${action}`,
    negativeText: '取消',
    maskClosable: false,
    onNegativeClick: release,
    onClose: release,
    onAfterLeave: release,
    onPositiveClick: async () => {
      if (saving.value) {
        return false
      }
      saving.value = true
      try {
        const response = await request.post('/system/dataScopeConfig/status', {
          id: props.row.id,
          enabled: nextEnabled ? 1 : 0,
          expectedEnabled,
        }, { needTip: false })
        if (response?.code !== 200) {
          throw new Error(response?.message || `${action}失败，请重试`)
        }
        currentStatus.value = nextEnabled ? 1 : 0
        emit('updated')
        window.$message.success(`规则已${action}，请在相关页面重新查询`)
      }
      catch (error) {
        window.$message.error(error?.message || `${action}失败，请重试`)
      }
      finally {
        saving.value = false
        confirming.value = false
        // 请求异常也可能已提交，重新查询服务端状态，不能盲目声称回滚。
        emit('refresh')
      }
    },
  })
}
</script>

<style scoped>
.rule-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.rule-status__label {
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
