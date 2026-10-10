<template>
  <section class="plugin-center-page">
    <!-- 页面操作只面向当前客户，平台管理工作台不进入客户端依赖树 -->
    <header class="plugin-center__header">
      <div class="plugin-center__heading">
        <img
          v-if="!store.detailVisible" :src="pluginModules" class="plugin-center__artwork"
          width="120" height="80" alt="" aria-hidden="true"
        >
        <NButton v-if="store.detailVisible" quaternary aria-label="返回插件列表" @click="store.back">
          <template #icon>
            <i class="i-lucide:arrow-left" />
          </template>
          返回
        </NButton>
        <div>
          <h1>插件中心</h1>
          <p v-if="!store.detailVisible">
            查看插件能力，了解安装与授权
          </p>
          <p v-if="!store.detailVisible" class="plugin-center__caption">
            当前服务实际加载的模块
          </p>
        </div>
      </div>
      <div class="plugin-center__actions">
        <NButton
          v-if="pluginMarketUrl" tag="a" :href="pluginMarketUrl" target="_blank" rel="noopener noreferrer"
        >
          <template #icon>
            <i class="i-lucide:external-link" />
          </template>
          前往插件市场
        </NButton>
        <NButton
          v-if="store.canList" quaternary :loading="store.loading || store.detailLoading"
          aria-label="刷新插件信息" title="刷新插件信息" @click="refresh"
        >
          <template #icon>
            <i class="i-lucide:refresh-cw" />
          </template>
        </NButton>
      </div>
    </header>
    <NAlert v-if="!store.canList" type="info" :bordered="false">
      当前账号没有插件中心查询权限，请联系系统管理员。
    </NAlert>
    <template v-else>
      <!-- A：首页保留 DOM 和滚动位置，详情返回无需重新查询 -->
      <div v-show="!store.detailVisible" class="plugin-center__home">
        <NTabs v-model:value="store.homeTab" type="line" class="plugin-center__tabs">
          <NTab name="installed">
            我的插件
          </NTab>
          <NTab name="discover">
            发现插件
          </NTab>
        </NTabs>
        <template v-if="store.homeTab === 'installed'">
          <div class="plugin-center__toolbar">
            <PluginCenterToolbar />
          </div>
          <div class="plugin-center__scroll" tabindex="0" aria-label="插件列表滚动区">
            <NAlert v-if="store.error" type="error" title="插件清单加载失败" :bordered="false">
              {{ store.error }}
              <NButton text type="primary" @click="store.load">
                重新加载
              </NButton>
            </NAlert>
            <PluginRuntimeGallery
              v-else :records="store.records" :loading="store.loading" :can-detail="store.canDetail"
              :filtered="Boolean(store.query.keyword || store.query.origin)" @open="store.open"
            />
          </div>
          <footer class="plugin-center__footer">
            <span>{{ store.error ? '清单加载失败' : `当前服务共 ${store.total} 个插件` }}</span>
            <NPagination
              :page="store.query.pageNum" :page-size="store.query.pageSize" :item-count="store.total"
              :page-slot="5" show-size-picker :page-sizes="[15, 30, 48]"
              @update:page="store.changePage" @update:page-size="store.changeSize"
            />
          </footer>
        </template>
        <div v-else class="plugin-center__scroll">
          <PluginDiscovery />
        </div>
      </div>
      <!-- B：连体列表＋详情，复用系统公共滚动与响应式布局 -->
      <MasterDetailWorkspace
        v-if="store.detailVisible && store.canDetail" :aside-width="280" class="plugin-center__workspace"
      >
        <template #aside>
          <PluginListNavigation />
        </template>
        <PluginDetail />
      </MasterDetailWorkspace>
    </template>
  </section>
</template>

<script setup>
import { NAlert, NButton, NPagination, NTab, NTabs } from 'naive-ui'
import { onActivated, onBeforeUnmount, onDeactivated, onMounted } from 'vue'
import pluginModules from '@/assets/illustrations/plugins/plugin-modules.png'
import MasterDetailWorkspace from '@/components/common/MasterDetailWorkspace.vue'
import { usePluginCenterStore } from '@/stores/plugin/centerStore'
import PluginCenterToolbar from './plugin/components/PluginCenterToolbar.vue'
import PluginDetail from './plugin/components/PluginDetail.vue'
import PluginDiscovery from './plugin/components/PluginDiscovery.vue'
import PluginListNavigation from './plugin/components/PluginListNavigation.vue'
import PluginRuntimeGallery from './plugin/components/PluginRuntimeGallery.vue'
import { pluginMarketUrl } from './plugin/pluginLinks'

defineOptions({ name: 'SystemPluginCenter' })
const store = usePluginCenterStore()
function refresh() {
  store.load()
  if (store.detailVisible)
    store.open(store.selectedId, store.detailTab)
}
onMounted(store.activate)
onActivated(store.activate)
onDeactivated(store.deactivate)
onBeforeUnmount(store.deactivate)
</script>

<style scoped>
.plugin-center-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary);
}
.plugin-center__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: var(--bg-primary);
}
.plugin-center__heading,
.plugin-center__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.plugin-center__header h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 500;
}
.plugin-center__header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
}
.plugin-center__home {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.plugin-center__artwork {
  flex: none;
  object-fit: contain;
}
.plugin-center__header .plugin-center__caption {
  margin-top: 5px;
  font-size: 11px;
}
.plugin-center__tabs {
  padding: 0 16px;
  background: var(--bg-primary);
  flex: none;
}
.plugin-center__toolbar {
  padding: 12px 16px;
  background: var(--bg-primary);
}
.plugin-center__scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable;
  padding: 12px 0;
}
.plugin-center__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--border-light);
  background: var(--bg-primary);
}
.plugin-center__footer > span {
  color: var(--text-tertiary);
  font-size: 12px;
}
.plugin-center__workspace {
  flex: 1;
  min-height: 0;
}
/* 窄屏上下堆叠时保留可用详情高度，由公共工作台统一承接外层滚动。 */
@media (max-width: 960px) {
  .plugin-center__workspace :deep(.master-detail-workspace__main) {
    min-height: 540px;
  }
}
@media (max-width: 540px) {
  .plugin-center__artwork {
    width: 72px;
    height: 56px;
  }
  .plugin-center__caption {
    display: none;
  }
  .plugin-center__header {
    padding: 12px;
  }
  .plugin-center__toolbar {
    padding: 12px 0;
  }
  .plugin-center__footer {
    padding: 8px 0;
  }
  .plugin-center__footer :deep(.n-pagination) {
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
