<template>
  <section class="plugin-discovery">
    <PluginIllustration :size="96" />
    <h2>发现适合业务的插件</h2>
    <p>在插件市场查看功能介绍、版本和交付说明，获取源码后在开发工程中完成集成。</p>
    <NButton
      v-if="pluginMarketUrl" tag="a" type="primary" :href="pluginMarketUrl"
      target="_blank" rel="noopener noreferrer"
    >
      <template #icon>
        <i class="i-lucide:external-link" />
      </template>
      打开插件市场
    </NButton>
    <NAlert v-else type="info" :bordered="false">
      插件市场地址配置无效，请由管理员检查 VITE_PLUGIN_MARKET_URL 后重新构建前端。
    </NAlert>
    <p class="plugin-discovery__note">
      本页“我的插件”只展示当前服务实际加载的模块。
    </p>
  </section>
</template>

<script setup>
import { NAlert, NButton } from 'naive-ui'
import { pluginMarketUrl } from '../pluginLinks'
import PluginIllustration from './PluginIllustration.vue'
</script>

<style scoped>
.plugin-discovery {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 16px;
  gap: 16px;
}
.plugin-discovery h2 {
  font-size: 16px;
  font-weight: 500;
  margin: 0;
}
.plugin-discovery p {
  max-width: 520px;
  margin: 0;
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.8;
}
.plugin-discovery .plugin-discovery__note {
  font-size: 12px;
}
</style>
