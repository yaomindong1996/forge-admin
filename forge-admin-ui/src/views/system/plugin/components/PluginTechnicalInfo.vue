<template>
  <NCollapse class="plugin-technical">
    <NCollapseItem title="技术信息" name="technical">
      <NDescriptions :column="1" label-placement="left" size="small" bordered>
        <NDescriptionsItem label="插件标识">
          {{ plugin.id }}
        </NDescriptionsItem>
        <NDescriptionsItem label="当前版本">
          {{ plugin.version || '未提供' }}
        </NDescriptionsItem>
        <NDescriptionsItem label="发行版">
          <DictTag :value="plugin.edition" :options="dict.sys_plugin_edition" />
        </NDescriptionsItem>
        <NDescriptionsItem label="加载状态">
          <DictTag :value="plugin.loadState" :options="dict.sys_plugin_load_state" />
        </NDescriptionsItem>
        <NDescriptionsItem label="核心兼容范围">
          {{ plugin.requiresCore || '随核心发行' }}
        </NDescriptionsItem>
        <NDescriptionsItem label="后端模块">
          {{ plugin.serverModule || '未声明' }}
        </NDescriptionsItem>
      </NDescriptions>
      <p>以下为当前服务的功能开关结果，不等同于许可证验证或业务健康检查。</p>
      <div v-for="feature in plugin.features || []" :key="feature.code" class="plugin-technical__feature">
        <code>{{ feature.code }}</code>
        <DictTag :value="String(feature.enabled)" :options="dict.sys_plugin_feature_state" />
      </div>
    </NCollapseItem>
  </NCollapse>
</template>

<script setup>
import { NCollapse, NCollapseItem, NDescriptions, NDescriptionsItem } from 'naive-ui'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineProps({ plugin: { type: Object, required: true } })
const { dict } = useDict('sys_plugin_edition', 'sys_plugin_load_state', 'sys_plugin_feature_state')
</script>

<style scoped>
.plugin-technical {
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}
.plugin-technical p {
  font-size: 12px;
  color: var(--text-tertiary);
}
.plugin-technical__feature {
  display: flex;
  gap: 8px;
  justify-content: space-between;
  padding: 8px 0;
}
.plugin-technical code,
.plugin-technical :deep(.n-descriptions-table-content) {
  overflow-wrap: anywhere;
}
</style>
