<template>
  <div class="plugin-center-page">
    <NTabs v-model:value="activeTab" type="line" animated>
      <NTabPane name="runtime" tab="已加载插件" display-directive="if">
        <section class="plugin-center">
          <header class="plugin-center__hero">
            <div class="plugin-center__hero-copy">
              <span class="plugin-center__kicker">本机运行清单</span>
              <h2>插件中心</h2>
              <p>
                查看当前 Admin 后端实例已装配的插件声明。这里是运行态清单，不是官网源码市场，也不负责远程部署。
              </p>
              <div class="plugin-center__chips">
                <NTag size="small" :bordered="false" type="info">
                  核心 {{ metadata.coreVersion || '—' }}
                </NTag>
                <NTag v-if="metadata.edition" size="small" :bordered="false">
                  {{ metadata.edition }}
                </NTag>
                <NTag size="small" :bordered="false">
                  共 {{ total }} 个
                </NTag>
              </div>
            </div>
            <NButton v-if="canList" secondary :loading="loading" @click="load">
              <template #icon>
                <i class="i-lucide:refresh-cw" />
              </template>
              刷新清单
            </NButton>
          </header>

          <form class="plugin-center__filters" @submit.prevent="search">
            <NInput
              v-model:value="query.keyword"
              clearable
              :maxlength="100"
              placeholder="搜索插件名称或标识"
            >
              <template #prefix>
                <i class="i-lucide:search" />
              </template>
            </NInput>
            <NSelect
              v-model:value="query.origin"
              clearable
              :options="origins"
              placeholder="全部来源"
            />
            <NButton type="primary" attr-type="submit" :loading="loading">
              查询
            </NButton>
            <NButton @click="reset">
              重置
            </NButton>
          </form>

          <NAlert v-if="error" type="error" title="清单加载失败" :bordered="false">
            {{ error }}
            <NButton text type="primary" @click="load">
              重试
            </NButton>
          </NAlert>

          <PluginRuntimeGallery
            :records="records"
            :loading="loading"
            :can-detail="canDetail"
            @open="openDetail"
          />

          <footer class="plugin-center__footer">
            <span>仅展示当前后端 classpath 中的构建声明</span>
            <NPagination
              :page="query.pageNum"
              :page-size="query.pageSize"
              :item-count="total"
              :page-sizes="[12, 15, 30, 48]"
              show-size-picker
              @update:page="changePage"
              @update:page-size="changeSize"
            />
          </footer>

          <NAlert type="info" :bordered="false" class="plugin-center__note">
            加载成功只代表模块声明与类路径装配，不代表健康检查通过。安装升级请走「安装工作台」；
            远程制品发布属于运维交付能力，与本页浏览无关。
          </NAlert>

          <PluginDetail
            :show="detailVisible"
            :loading="detailLoading"
            :plugin="detail"
            :error="detailError"
            @close="closeDetail"
            @retry="openDetail()"
          />
        </section>
      </NTabPane>

      <NTabPane name="ui" tab="构建对比" display-directive="if">
        <PluginBuildComparison />
      </NTabPane>

      <NTabPane name="tasks" tab="安装工作台" display-directive="if">
        <PluginWorkbench />
      </NTabPane>

      <NTabPane name="license" tab="运行时授权" display-directive="if">
        <RuntimeLicenseStatus />
      </NTabPane>

      <NTabPane
        v-if="hasPermission('system:plugin:delivery:list')"
        name="delivery"
        tab="运维交付"
        display-directive="if"
      >
        <section class="plugin-delivery-tab">
          <NAlert type="warning" :bordered="false" title="这不是客户端插件浏览功能">
            「运维交付」面向平台运维：把已审核制品发布到目标环境并做部署核验。
            普通查看本机已加载插件，请留在「已加载插件」页签。官网源码上架请去网站管理端的插件发布。
          </NAlert>
          <PluginDeliveryWorkbench />
        </section>
      </NTabPane>
    </NTabs>
  </div>
</template>

<script setup>
import {
  NAlert,
  NButton,
  NInput,
  NPagination,
  NSelect,
  NTabPane,
  NTabs,
  NTag,
} from 'naive-ui'
import { computed, onMounted, ref } from 'vue'
import { useDict } from '@/composables/useDict'
import PluginBuildComparison from './plugin/components/PluginBuildComparison.vue'
import PluginDeliveryWorkbench from './plugin/components/PluginDeliveryWorkbench.vue'
import PluginDetail from './plugin/components/PluginDetail.vue'
import PluginRuntimeGallery from './plugin/components/PluginRuntimeGallery.vue'
import PluginWorkbench from './plugin/components/PluginWorkbench.vue'
import RuntimeLicenseStatus from './plugin/components/RuntimeLicenseStatus.vue'
import { usePluginCenter } from './plugin/usePluginCenter'
import { usePluginPermission } from './plugin/usePluginPermission'

defineOptions({ name: 'SystemPluginCenter' })

const activeTab = ref('runtime')
const { dict } = useDict('sys_plugin_origin', 'sys_plugin_edition', 'sys_plugin_load_state')
const origins = computed(() => dict.value.sys_plugin_origin || [])
const hasPermission = usePluginPermission()
const canList = computed(() => hasPermission('system:plugin:list'))
const canDetail = computed(() => hasPermission('system:plugin:detail'))
const {
  query,
  records,
  total,
  loading,
  error,
  metadata,
  detail,
  detailVisible,
  detailLoading,
  detailError,
  load,
  search,
  reset,
  changePage,
  changeSize,
  openDetail,
  closeDetail,
} = usePluginCenter()

onMounted(load)
</script>

<style scoped>
.plugin-center-page {
  min-width: 0;
}
.plugin-center {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  padding: 4px 2px 12px;
}
.plugin-center__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  background:
    radial-gradient(circle at 12% 18%, color-mix(in srgb, var(--primary-color, #0e42d2) 10%, #fff), transparent 42%),
    linear-gradient(180deg, #fff, #f8fafc);
}
.plugin-center__kicker {
  color: var(--primary-color, #0e42d2);
  font-size: 12px;
  font-weight: 600;
}
.plugin-center__hero h2 {
  margin: 6px 0 8px;
  font-size: 22px;
  font-weight: 650;
  line-height: 1.25;
}
.plugin-center__hero p {
  margin: 0;
  max-width: 720px;
  color: var(--text-secondary, #4e5969);
  font-size: 13px;
  line-height: 1.6;
}
.plugin-center__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.plugin-center__filters {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 180px auto auto;
  gap: 8px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  background: var(--card-color, #fff);
}
.plugin-center__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.plugin-center__footer > span,
.plugin-center__note {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.6;
}
.plugin-center__note {
  margin: 0;
}
.plugin-delivery-tab {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 4px;
}
@media (max-width: 860px) {
  .plugin-center__hero {
    flex-direction: column;
  }
  .plugin-center__filters {
    grid-template-columns: 1fr 1fr;
  }
  .plugin-center__filters :deep(.n-input) {
    grid-column: 1 / -1;
  }
  .plugin-center__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
