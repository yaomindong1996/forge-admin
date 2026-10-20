<script setup>
/**
 * StartConfig — 开始节点配置
 *
 * 发起人变量由后端在流程启动时固定注入为 initiator，表单配置走流程模型全局配置。
 */
defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})
defineEmits(['update:config'])
</script>

<template>
  <div class="space-y-3">
    <div class="config-hint">
      <div class="start-config-row">
        <span>发起人</span>
        <n-tag size="small" type="success" :bordered="false">
          当前登录用户
        </n-tag>
      </div>
      <div class="start-config-row">
        <span>变量名</span>
        <code>initiator</code>
      </div>
    </div>
    <div class="config-hint">
      发起人由系统自动记录，流程表单请在顶部“更多设置 / 表单配置”中维护。
    </div>
  </div>
</template>

<style scoped>
.start-config-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.start-config-row + .start-config-row {
  margin-top: 8px;
}
.start-config-row code {
  color: var(--text-primary);
}
</style>
