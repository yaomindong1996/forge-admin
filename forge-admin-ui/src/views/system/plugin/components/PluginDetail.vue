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
      <!-- 当前插件由导航或页标题标识，详情从页签开始，不重复堆叠介绍头。 -->
      <NTabs v-model:value="store.detailTab" class="plugin-detail__tabs" type="line">
        <NTab name="introduction">
          功能介绍
        </NTab>
        <NTab v-if="canExamples" name="examples">
          使用示例
        </NTab>
        <NTab name="installation">
          安装说明
        </NTab>
        <NTab name="license">
          授权状态
        </NTab>
        <NTab v-if="canSettings" name="settings">
          插件设置
        </NTab>
      </NTabs>
      <!-- 详情正文独立滚动，底部内容不被导航或分页遮挡 -->
      <div
        :key="store.selectedId + store.detailTab" class="plugin-detail__body"
        :class="{
          'plugin-detail__body--settings': settingsActive,
          'plugin-detail__body--examples': examplesActive,
        }"
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
              <NButton
                v-if="canExamples" text type="primary" class="plugin-introduction__example-link"
                @click="store.detailTab = 'examples'"
              >
                查看使用示例 →
              </NButton>
            </div>
          </div>
          <PluginTechnicalInfo :plugin="store.detail" />
        </section>
        <PluginInstallationGuide v-else-if="store.detailTab === 'installation'" :plugin="store.detail" />
        <PluginExamplesOutlet
          v-else-if="examplesActive" :plugin-id="store.detail.id"
        />
        <PluginSettingsOutlet
          v-else-if="store.detailTab === 'settings' && canSettings" :plugin-id="store.detail.id"
        />
        <PluginLicenseSummary v-else :plugin="store.detail" />
      </div>
    </template>
  </section>
</template>

<script setup>
import { NAlert, NButton, NSkeleton, NTab, NTabs } from 'naive-ui'
import { computed, watch } from 'vue'
import { usePluginCenterStore } from '@/stores/plugin/centerStore'
import { getPluginExamplesExtension } from '@/utils/plugin-examples'
import { getPluginSettingsExtension } from '@/utils/plugin-settings'
import { pluginPresentation } from '../pluginPresentation'
import { usePluginPermission } from '../usePluginPermission'
import PluginExamplesOutlet from './PluginExamplesOutlet.vue'
import PluginInstallationGuide from './PluginInstallationGuide.vue'
import PluginLicenseSummary from './PluginLicenseSummary.vue'
import PluginSettingsOutlet from './PluginSettingsOutlet.vue'
import PluginTechnicalInfo from './PluginTechnicalInfo.vue'

const store = usePluginCenterStore()
const presentation = computed(() => pluginPresentation(store.detail))
const hasPermission = usePluginPermission()
const canSettings = computed(() => {
  const extension = getPluginSettingsExtension(store.detail?.id)
  return !!extension && hasPermission(extension.permission)
})
const settingsActive = computed(() => store.detailTab === 'settings' && canSettings.value)
const canExamples = computed(() => {
  const extension = getPluginExamplesExtension(store.detail?.id)
  return !!extension && hasPermission(extension.permission)
})
const examplesActive = computed(() => store.detailTab === 'examples' && canExamples.value)
watch([canSettings, () => store.detailTab], ([allowed]) => {
  if (!allowed && store.detailTab === 'settings')
    store.detailTab = 'introduction'
}, { flush: 'sync' })
watch([canExamples, () => store.detailTab], ([allowed]) => {
  if (!allowed && store.detailTab === 'examples')
    store.detailTab = 'introduction'
}, { flush: 'sync' })
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
.plugin-detail p {
  margin: 8px 0;
  font-size: 13px;
  color: var(--text-tertiary);
  line-height: 1.8;
}
.plugin-detail__tabs {
  flex: none;
  padding: 0 20px;
}
/* 新增可选设置页签后，窄屏仍可直接看到选中项，不把入口挤出容器。 */
@media (max-width: 600px) {
  .plugin-detail__tabs :deep(.n-tabs-tab) {
    font-size: 12px;
  }
  .plugin-detail__tabs :deep(.n-tabs-tab-pad) {
    width: 16px;
  }
}
.plugin-detail__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable;
  padding: 24px 20px;
}
.plugin-detail__body--settings,
.plugin-detail__body--examples {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 20px;
}
.plugin-detail__body--examples {
  padding: 8px 12px;
  scrollbar-gutter: auto;
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
.plugin-introduction__example-link {
  margin-left: 16px;
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
  .plugin-detail__body--examples {
    padding: 8px;
  }
  .plugin-introduction__features {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
