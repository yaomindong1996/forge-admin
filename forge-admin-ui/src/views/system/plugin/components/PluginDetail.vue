<template>
  <NDrawer :show="show" width="min(560px, 100vw)" :auto-focus="false" @update:show="emit('close')">
    <NDrawerContent title="插件详情" closable>
      <!-- 加载与失败反馈 -->
      <NSkeleton v-if="loading" text :repeat="8" />
      <NAlert v-else-if="error" type="error" title="详情加载失败">
        <p>{{ error }}</p>
        <NButton @click="emit('retry')">
          重试
        </NButton>
      </NAlert>
      <!-- 运行时声明 -->
      <div v-else-if="plugin" class="plugin-detail">
        <NDescriptions label-placement="left" :column="1" bordered size="small">
          <NDescriptionsItem label="插件名称">
            {{ plugin.name }}
          </NDescriptionsItem>
          <NDescriptionsItem label="稳定标识">
            {{ plugin.id }}
          </NDescriptionsItem>
          <NDescriptionsItem label="插件版本">
            {{ plugin.version }}
          </NDescriptionsItem>
          <NDescriptionsItem label="来源">
            <DictTag :options="dict.sys_plugin_origin" :value="plugin.origin" />
          </NDescriptionsItem>
          <NDescriptionsItem label="发行版">
            <DictTag :options="dict.sys_plugin_edition" :value="plugin.edition" />
          </NDescriptionsItem>
          <NDescriptionsItem label="加载状态">
            <DictTag :options="dict.sys_plugin_load_state" :value="plugin.loadState" />
          </NDescriptionsItem>
          <NDescriptionsItem label="核心兼容范围">
            {{ plugin.requiresCore || '随核心发行' }}
          </NDescriptionsItem>
          <NDescriptionsItem label="后端模块">
            {{ plugin.serverModule || '未声明' }}
          </NDescriptionsItem>
          <NDescriptionsItem label="前端声明">
            {{ plugin.hasUi ? '包含前端声明，构建状态待核验' : '未单独声明' }}
          </NDescriptionsItem>
        </NDescriptions>

        <section class="plugin-detail__features">
          <h3>声明功能与当前使用权</h3>
          <NEmpty v-if="!plugin.features.length" description="未声明独立功能编码" size="small" />
          <div v-for="feature in plugin.features" :key="feature.code" class="plugin-detail__feature">
            <code>{{ feature.code }}</code>
            <DictTag :options="dict.sys_plugin_feature_state" :value="String(feature.enabled)" />
          </div>
        </section>
        <NAlert type="info" :bordered="false" :show-icon="false">
          加载状态仅表示当前服务包含该模块，不代表健康检查通过。
          功能使用权与用户菜单权限分别校验；本页面不展示商业许可证或维保到期状态。
        </NAlert>
      </div>
    </NDrawerContent>
  </NDrawer>
</template>

<script setup>
import { NAlert, NButton, NDescriptions, NDescriptionsItem, NDrawer, NDrawerContent, NEmpty, NSkeleton } from 'naive-ui'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineProps({
  show: Boolean,
  loading: Boolean,
  plugin: { type: Object, default: null },
  error: { type: String, default: '' },
})
const emit = defineEmits(['close', 'retry'])
const { dict } = useDict('sys_plugin_origin', 'sys_plugin_edition', 'sys_plugin_load_state', 'sys_plugin_feature_state')
</script>

<style scoped>
.plugin-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.plugin-detail :deep(.n-descriptions-table-content) {
  overflow-wrap: anywhere;
}
.plugin-detail__features h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
}
.plugin-detail__feature {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}
.plugin-detail__feature code {
  overflow-wrap: anywhere;
}
</style>
