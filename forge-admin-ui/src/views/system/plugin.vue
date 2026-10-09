<template>
  <NTabs type="line" animated default-value="runtime">
    <NTabPane name="runtime" tab="当前后端" display-directive="if">
      <div class="plugin-center">
        <!-- 页面标题与刷新 -->
        <header class="plugin-center__header">
          <div>
            <h2>插件中心</h2>
            <p>当前后端服务 · 核心 {{ metadata.coreVersion || '—' }}</p>
          </div>
          <NButton v-if="canList" :loading="loading" @click="load">
            <template #icon>
              <i class="i-lucide:refresh-cw" />
            </template>
            刷新清单
          </NButton>
        </header>

        <!-- 筛选区 -->
        <form class="plugin-center__filters" @submit.prevent="search">
          <NInput v-model:value="query.keyword" clearable :maxlength="100" placeholder="插件名称或标识" />
          <NSelect v-model:value="query.origin" clearable :options="origins" placeholder="全部来源" />
          <NButton type="primary" attr-type="submit" :loading="loading">
            查询
          </NButton>
          <NButton @click="reset">
            重置
          </NButton>
        </form>

        <!-- 列表与错误反馈 -->
        <NAlert v-if="error" type="error" title="清单加载失败">
          {{ error }} <NButton text type="primary" @click="load">
            重试
          </NButton>
        </NAlert>
        <NDataTable
          :columns="columns" :data="records" :loading="loading" :row-key="row => row.id"
          :scroll-x="760" :bordered="false" size="medium"
        >
          <template #empty>
            <NEmpty description="当前服务没有符合条件的插件" />
          </template>
        </NDataTable>
        <footer class="plugin-center__footer">
          <span>共 {{ total }} 个</span>
          <NPagination
            :page="query.pageNum" :page-size="query.pageSize" :item-count="total"
            :page-sizes="[15, 30, 50]" show-size-picker @update:page="changePage" @update:page-size="changeSize"
          />
        </footer>
        <p class="plugin-center__note">
          这里只显示当前后端实例的构建声明，不包含尚未部署的源码和纯前端插件。
          安装、升级与卸载需重新构建部署；独立流程服务请在对应实例核验。
        </p>

        <!-- 单层详情面板 -->
        <PluginDetail
          :show="detailVisible" :loading="detailLoading" :plugin="detail" :error="detailError"
          @close="closeDetail" @retry="openDetail()"
        />
      </div>
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
    <NTabPane v-if="hasPermission('system:plugin:delivery:list')" name="delivery" tab="发布与部署" display-directive="if">
      <PluginDeliveryWorkbench />
    </NTabPane>
  </NTabs>
</template>

<script setup>
import { NAlert, NButton, NDataTable, NEmpty, NInput, NPagination, NSelect, NTabPane, NTabs } from 'naive-ui'
import { computed, h, onMounted } from 'vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import PluginBuildComparison from './plugin/components/PluginBuildComparison.vue'
import PluginDeliveryWorkbench from './plugin/components/PluginDeliveryWorkbench.vue'
import PluginDetail from './plugin/components/PluginDetail.vue'
import PluginWorkbench from './plugin/components/PluginWorkbench.vue'
import RuntimeLicenseStatus from './plugin/components/RuntimeLicenseStatus.vue'
import { usePluginCenter } from './plugin/usePluginCenter'
import { usePluginPermission } from './plugin/usePluginPermission'

defineOptions({ name: 'SystemPluginCenter' })
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

const columns = computed(() => [
  {
    title: '插件',
    key: 'name',
    minWidth: 220,
    render: row => h(SystemTableCell, {
      title: row.name,
      subtitle: row.id,
      interactive: canDetail.value,
      onActivate: () => openDetail(row.id),
    }),
  },
  { title: '版本', key: 'version', width: 100 },
  dictColumn('来源', 'origin', 'sys_plugin_origin', 110),
  dictColumn('发行版', 'edition', 'sys_plugin_edition', 100),
  dictColumn('当前实例', 'loadState', 'sys_plugin_load_state', 120),
  { title: '声明功能', key: 'features', width: 110, render: row => row.features.length },
])

function dictColumn(title, key, type, width) {
  return { title, key, width, render: row => h(DictTag, { options: dict.value[type], value: row[key] }) }
}

onMounted(load)
</script>

<style scoped>
.plugin-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.plugin-center__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.plugin-center h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}
.plugin-center__header p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
}
.plugin-center__filters {
  display: grid;
  grid-template-columns: minmax(160px, 320px) 160px auto auto;
  justify-content: start;
  gap: 8px;
}
.plugin-center__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.plugin-center__footer > span {
  font-size: 12px;
  color: var(--text-tertiary);
}
.plugin-center__note {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-tertiary);
}
@media (max-width: 640px) {
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
  .plugin-center__footer :deep(.n-pagination) {
    flex-wrap: wrap;
  }
}
</style>
