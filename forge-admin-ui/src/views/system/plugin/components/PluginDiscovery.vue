<template>
  <section class="plugin-discovery">
    <div class="plugin-discovery__hero">
      <div class="plugin-discovery__copy">
        <div class="plugin-discovery__title">
          <span class="plugin-discovery__mark" aria-hidden="true">
            <i class="i-lucide:compass" />
          </span>
          <h2>发现适合业务的插件</h2>
        </div>
        <p>
          在插件市场查看功能介绍、版本和交付说明，获取源码后在开发工程中完成集成。
        </p>
        <div class="plugin-discovery__actions">
          <NButton
            v-if="pluginMarketUrl"
            tag="a"
            type="primary"
            :href="pluginMarketUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            <template #icon>
              <i class="i-lucide:rocket" />
            </template>
            打开插件市场
          </NButton>
          <NAlert v-else type="info" :bordered="false">
            插件市场地址配置无效，请由管理员检查 VITE_PLUGIN_MARKET_URL 后重新构建前端。
          </NAlert>
        </div>
        <p class="plugin-discovery__note">
          本页「我的插件」只展示当前服务实际加载的模块。
        </p>
      </div>
      <div class="plugin-discovery__art" aria-hidden="true">
        <img :src="pluginModules" width="180" height="120" alt="">
        <span class="discovery-orb" />
      </div>
    </div>
  </section>
</template>

<script setup>
import { NAlert, NButton } from 'naive-ui'
import pluginModules from '@/assets/illustrations/plugins/plugin-modules.png'
import { pluginMarketUrl } from '../pluginLinks'
</script>

<style scoped>
.plugin-discovery {
  width: 100%;
  padding: 4px 0 24px;
}

.plugin-discovery__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(160px, 0.75fr);
  gap: 18px;
  width: 100%;
  padding: 22px 24px;
  overflow: hidden;
  border: 1px dashed #dbe5f5;
  border-radius: 8px;
  background:
    radial-gradient(circle at 90% 20%, rgb(147 197 253 / 30%), transparent 40%),
    linear-gradient(135deg, #ffffff 0%, #f5f9ff 100%);
}

.plugin-discovery__title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.plugin-discovery__mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(145deg, #67e8f9, #0ea5e9);
  color: #fff;
  font-size: 18px;
  box-shadow: 0 8px 16px rgb(14 165 233 / 28%);
}

.plugin-discovery h2 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.plugin-discovery__copy p {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.plugin-discovery__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.plugin-discovery__note {
  margin: 0 !important;
  color: #94a3b8 !important;
  font-size: 12px !important;
}

.plugin-discovery__art {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 140px;
}

.plugin-discovery__art img {
  position: relative;
  z-index: 1;
  object-fit: contain;
  opacity: 0.9;
}

.discovery-orb {
  position: absolute;
  inset: auto 10% 8% auto;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, rgb(125 211 252 / 50%), transparent 70%);
}

@media (max-width: 720px) {
  .plugin-discovery__hero {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .plugin-discovery__art {
    display: none;
  }
}
</style>
