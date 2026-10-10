<template>
  <section class="plugin-detail" aria-label="插件详情" :aria-busy="store.detailLoading">
    <!-- 详情加载不复用上一插件资料 -->
    <div v-if="store.detailLoading" class="plugin-detail__feedback">
      <NSkeleton width="96px" height="96px" />
      <NSkeleton text :repeat="5" />
    </div>
    <div v-else-if="store.detailError" class="plugin-detail__feedback">
      <NAlert type="error" title="插件详情加载失败">
        {{ store.detailError }}
        <NButton text type="primary" @click="store.open(store.selectedId, store.detailTab)">
          重新加载
        </NButton>
      </NAlert>
    </div>
    <template v-else-if="store.detail">
      <!-- 当前插件与统一操作 -->
      <header class="plugin-detail__header">
        <PluginIllustration :plugin="store.detail" :size="48" />
        <div class="plugin-detail__identity">
          <div class="plugin-detail__title">
            <h2>{{ store.detail.name || store.detail.id }}</h2>
            <DictTag :options="dict.sys_plugin_origin" :value="store.detail.origin" type="default" :bordered="false" />
          </div>
          <p>{{ presentation.summary }}</p>
          <span class="plugin-detail__version">{{ store.detail.version ? `v${store.detail.version}` : '版本未提供' }}</span>
        </div>
        <NButton v-if="pluginGuideUrl" tag="a" :href="pluginGuideUrl" target="_blank" rel="noopener noreferrer">
          <template #icon>
            <i class="i-lucide:book-open" />
          </template>
          使用文档
        </NButton>
      </header>
      <NTabs v-model:value="store.detailTab" class="plugin-detail__tabs" type="line">
        <NTab name="introduction">
          功能介绍
        </NTab>
        <NTab name="installation">
          安装说明
        </NTab>
        <NTab name="license">
          授权状态
        </NTab>
      </NTabs>
      <!-- 详情正文独立滚动，底部内容不被导航或分页遮挡 -->
      <div
        :key="store.selectedId + store.detailTab" class="plugin-detail__body"
        tabindex="0" aria-label="插件详情正文"
      >
        <section v-if="store.detailTab === 'introduction'" class="plugin-introduction">
          <h3>功能概览</h3>
          <p>{{ presentation.introduction }}</p>
          <div v-if="presentation.highlights.length" class="plugin-introduction__features">
            <div v-for="item in presentation.highlights" :key="item">
              <i class="i-lucide:check-circle-2" aria-hidden="true" />
              <span>{{ item }}</span>
            </div>
          </div>
          <div class="plugin-introduction__guide">
            <i class="i-lucide:book-open" aria-hidden="true" />
            <div>
              <h4>从了解能力，到开始使用</h4>
              <p>安装说明帮助你核对交付方式与使用前准备，授权状态用于查看当前实例的许可证结果。</p>
              <NButton text type="primary" @click="store.detailTab = 'installation'">
                查看安装说明 →
              </NButton>
            </div>
          </div>
          <PluginTechnicalInfo :plugin="store.detail" />
        </section>
        <PluginInstallationGuide v-else-if="store.detailTab === 'installation'" :plugin="store.detail" />
        <PluginLicenseSummary v-else :plugin="store.detail" />
      </div>
    </template>
  </section>
</template>

<script setup>
import { NAlert, NButton, NSkeleton, NTab, NTabs } from 'naive-ui'
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { usePluginCenterStore } from '@/stores/plugin/centerStore'
import { pluginGuideUrl } from '../pluginLinks'
import { pluginPresentation } from '../pluginPresentation'
import PluginIllustration from './PluginIllustration.vue'
import PluginInstallationGuide from './PluginInstallationGuide.vue'
import PluginLicenseSummary from './PluginLicenseSummary.vue'
import PluginTechnicalInfo from './PluginTechnicalInfo.vue'

const store = usePluginCenterStore()
const { dict } = useDict('sys_plugin_origin')
const presentation = computed(() => pluginPresentation(store.detail))
</script>

<style scoped>
.plugin-detail {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
}
.plugin-detail__feedback {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.plugin-detail__header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  flex-wrap: wrap;
}
.plugin-detail__identity {
  flex: 1;
  min-width: 160px;
  overflow-wrap: anywhere;
}
.plugin-detail__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.plugin-detail h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 500;
}
.plugin-detail p {
  margin: 8px 0;
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.8;
}
.plugin-detail__version {
  font-size: 12px;
  color: var(--text-tertiary);
}
.plugin-detail__tabs {
  flex: none;
  padding: 0 20px;
}
.plugin-detail__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable;
  padding: 24px 20px;
}
.plugin-introduction {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.plugin-introduction h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}
.plugin-introduction > p {
  margin: 0;
}
.plugin-introduction__features {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.plugin-introduction__features > div {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px 0;
  font-size: 13px;
}
.plugin-introduction__features i {
  color: var(--primary-color);
  flex: none;
}
.plugin-introduction__guide {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 4px;
}
.plugin-introduction__guide > i {
  font-size: 24px;
  color: var(--primary-color);
  flex: none;
}
.plugin-introduction__guide h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
}
@media (max-width: 640px) {
  .plugin-detail__header {
    padding: 12px;
    gap: 8px;
  }
  .plugin-detail__tabs {
    padding: 0 12px;
  }
  .plugin-detail__body {
    padding: 16px 12px;
  }
  .plugin-introduction__features {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
