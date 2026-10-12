<template>
  <div class="process-message-config">
    <NFormItem label="消息模板" label-placement="top">
      <NSelect :value="config.messageTemplateCode || null" :options="options" filterable placeholder="选择已启用消息模板" @update:value="value => update({ messageTemplateCode: value })" />
    </NFormItem>
    <NCheckbox :checked="config.useApplicationCollaboration === true" @update:checked="value => update({ useApplicationCollaboration: value })">
      通过本应用企业协同通道发送
    </NCheckbox>
    <p>接收人为流程发起人。未勾选时发送站内信；勾选后须先在应用设置 → 集成与开放中绑定可用消息连接。</p>
    <p>仅消息节点执行时发送，绑定本身不触发通知。企业账号未绑定或连接不可用时不会回退其他通道。</p>
  </div>
</template>

<script setup>
import { NCheckbox, NFormItem, NSelect } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({ config: { type: Object, required: true }, templates: { type: Array, default: () => [] } })
const emit = defineEmits(['update'])
const options = computed(() => props.templates.map(item => ({ value: item.templateCode || item.value || item.code, label: item.templateName || item.label || item.name || item.templateCode })))
function update(patch) {
  emit('update', { ...props.config, ...patch })
}
</script>

<style scoped>
p {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.65;
}
</style>
