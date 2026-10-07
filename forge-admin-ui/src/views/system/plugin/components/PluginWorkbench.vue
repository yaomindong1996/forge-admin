<template>
  <section class="plugin-workbench">
    <!-- 工具栏及上传 -->
    <header>
      <div>上传预检 → 人工确认 → 待构建；独立执行器尚未接入。</div>
      <NSpace>
        <NButton :loading="list.loading.value" @click="list.load">
          刷新任务
        </NButton>
        <NUpload
          v-if="canUpload" accept=".zip" :show-file-list="false" :disabled="actions.busy.value"
          :custom-request="handleUpload"
        >
          <NButton type="primary" :loading="actions.busy.value">
            上传 ZIP 预检
          </NButton>
        </NUpload>
      </NSpace>
    </header>
    <p class="plugin-workbench__note">
      ZIP ≤8 MiB；拒绝本地配置和构建产物。包须来自可信交付方，不会在当前服务执行。
    </p>
    <!-- 查询区 -->
    <form @submit.prevent="list.search">
      <NInput v-model:value="list.query.keyword" placeholder="插件名称或标识" clearable :maxlength="100" />
      <NSelect
        v-model:value="list.query.status" :options="dict.sys_plugin_task_status || []"
        clearable placeholder="全部任务状态"
      />
      <NButton attr-type="submit" :loading="list.loading.value">
        查询
      </NButton>
    </form>
    <NAlert v-if="actions.actionError.value" type="error">
      {{ actions.actionError.value }}
      <NButton v-if="actions.pendingUpload.value" text @click="actions.retryUpload">
        重试同一上传
      </NButton>
    </NAlert>
    <NAlert v-if="list.error.value" type="error" title="任务加载失败">
      {{ list.error.value }}
    </NAlert>
    <!-- 审计任务列表 -->
    <NDataTable
      :columns="columns" :data="list.records.value" :loading="list.loading.value" :scroll-x="780" :bordered="false"
    >
      <template #empty>
        <NEmpty description="暂无符合条件的插件任务" />
      </template>
    </NDataTable>
    <footer>
      <span>共 {{ list.total.value }} 个任务</span>
      <NPagination
        :page="list.query.pageNum" :page-size="list.query.pageSize" :item-count="list.total.value"
        @update:page="list.changePage"
      />
    </footer>
    <!-- 当前任务预览 -->
    <PluginTaskPreview
      :show="actions.visible.value" :task="actions.task.value" :busy="actions.busy.value"
      :loading="actions.detailLoading.value" :error="actions.detailError.value"
      :action-error="actions.actionError.value"
      @close="actions.close" @retry="actions.open()" @confirm="actions.confirm" @cancel="actions.cancel"
    />
  </section>
</template>

<script setup>
import { NAlert, NButton, NDataTable, NEmpty, NInput, NPagination, NSelect, NSpace, NUpload } from 'naive-ui'
import { computed, h, onMounted } from 'vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { pluginTime } from '../pluginTaskUtils'
import { usePluginPermission } from '../usePluginPermission'
import { usePluginTaskActions } from '../usePluginTaskActions'
import { usePluginTaskList } from '../usePluginTaskList'
import PluginTaskPreview from './PluginTaskPreview.vue'

const list = usePluginTaskList()
const actions = usePluginTaskActions(list.load)
const permission = usePluginPermission()
const canUpload = computed(() => permission('system:plugin:upload'))
const { dict } = useDict('sys_plugin_task_status', 'sys_plugin_task_operation')
const columns = computed(() => [
  { title: '插件任务', key: 'name', minWidth: 210, render: row => h(SystemTableCell, {
    title: row.name,
    subtitle: row.pluginId,
    interactive: permission('system:plugin:task:detail'),
    onActivate: () => actions.open(row.id),
  }) },
  { title: '包版本', key: 'version', width: 100 },
  { title: '操作', key: 'operation', minWidth: 180, render: row =>
    h(DictTag, { value: row.operation, options: dict.value.sys_plugin_task_operation }) },
  { title: '任务状态', key: 'status', width: 120, render: row =>
    h(DictTag, { value: row.status, options: dict.value.sys_plugin_task_status }) },
  { title: '提交时间', key: 'createdTime', width: 170, render: row => pluginTime(row.createdTime) },
])
async function handleUpload({ file, onFinish, onError }) {
  await actions.upload(file.file)
  if (actions.actionError.value)
    onError()
  else
    onFinish()
}
onMounted(list.load)
</script>

<style scoped>
.plugin-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.plugin-workbench header,
.plugin-workbench footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.plugin-workbench__note {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 0;
}
.plugin-workbench form {
  display: grid;
  grid-template-columns: minmax(160px, 320px) 180px auto;
  gap: 8px;
}
.plugin-workbench footer {
  font-size: 12px;
}
@media (max-width: 640px) {
  .plugin-workbench form {
    grid-template-columns: 1fr 1fr;
  }
  .plugin-workbench form :deep(.n-input) {
    grid-column: 1 / -1;
  }
  .plugin-workbench footer :deep(.n-pagination) {
    flex-wrap: wrap;
  }
}
</style>
