<template>
  <div class="excel-config-status">
    <NSwitch
      :value="enabled"
      :loading="saving"
      :disabled="saving || confirming"
      :aria-disabled="saving || confirming"
      :aria-label="`${planName}的启用状态`"
      size="small"
      @update:value="confirmChange"
    />
    <span class="excel-config-status__label">{{ statusLabel }}</span>
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
const emit = defineEmits(['refresh'])
const currentStatus = ref(Number(props.row.status))
const saving = ref(false)
const confirming = ref(false)
const enabled = computed(() => currentStatus.value === 1)
const planName = computed(() => props.row.exportName || props.row.configKey || '此方案')
const statusLabel = computed(() => {
  const label = props.options.find(option => Number(option.value) === currentStatus.value)?.label
  if (label) {
    return label.startsWith('已') ? label : `已${label}`
  }
  return enabled.value ? '已启用' : '已停用'
})

watch(() => props.row.status, value => currentStatus.value = Number(value))

function confirmChange(nextEnabled) {
  if (saving.value || confirming.value || nextEnabled === enabled.value) {
    return
  }
  const action = nextEnabled ? '启用' : '停用'
  confirming.value = true
  const release = () => {
    confirming.value = false
  }
  window.$dialog.warning({
    title: `${action}“${planName.value}”？`,
    content: nextEnabled
      ? '启用后，相关业务页面可以立即使用此方案下载文件或获取导入模板。'
      : '停用后，相关业务页面将暂时无法使用此方案，已保存的设置不会丢失。',
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
        const response = await request.put('/system/excel/export-config/status', null, {
          params: {
            id: props.row.id,
            status: nextEnabled ? 1 : 0,
          },
          needTip: false,
        })
        if (response?.code !== 200) {
          throw new Error(response?.respMsg || response?.message || `${action}失败`)
        }
        currentStatus.value = nextEnabled ? 1 : 0
        window.$message.success(`方案已${action}`)
      }
      catch (error) {
        window.$message.error(error?.message || `${action}失败，请重试`)
      }
      finally {
        saving.value = false
        confirming.value = false
        emit('refresh')
      }
    },
  })
}
</script>

<style scoped>
.excel-config-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.excel-config-status__label {
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
