<template>
  <NDrawer :show="show" width="min(760px, 100vw)" :mask-closable="!busy" @update:show="emit('close')">
    <NDrawerContent title="插件包预检与任务" closable>
      <!-- 请求状态 -->
      <NSkeleton v-if="loading" text :repeat="10" />
      <NAlert v-else-if="error" type="error" title="任务加载失败">
        {{ error }} <NButton @click="emit('retry')">
          重试
        </NButton>
      </NAlert>
      <div v-else-if="task" class="plugin-preview">
        <!-- 操作错误集中在当前详情，避免背景列表重复提示 -->
        <NAlert v-if="actionError" type="error">
          {{ actionError }}
        </NAlert>
        <!-- 摘要与状态 -->
        <NDescriptions bordered :column="1" size="small" label-placement="left">
          <NDescriptionsItem label="插件">
            {{ task.name }} · {{ task.pluginId }}
          </NDescriptionsItem>
          <NDescriptionsItem label="状态">
            <DictTag :options="dict.sys_plugin_task_status" :value="task.status" />
          </NDescriptionsItem>
          <NDescriptionsItem label="操作">
            <DictTag :options="dict.sys_plugin_task_operation" :value="task.operation" />
          </NDescriptionsItem>
          <NDescriptionsItem label="版本变化">
            {{ task.preview.currentVersion || '当前实例未加载' }} → {{ task.version }}
          </NDescriptionsItem>
          <NDescriptionsItem label="任务 ID">
            {{ task.id }}
          </NDescriptionsItem>
          <NDescriptionsItem label="任务版本（revision）">
            {{ task.revision }}
          </NDescriptionsItem>
          <NDescriptionsItem label="包 SHA-256">
            <code>{{ task.sha256 }}</code>
          </NDescriptionsItem>
          <NDescriptionsItem label="提交时间">
            {{ pluginTime(task.createdTime) }}
          </NDescriptionsItem>
          <NDescriptionsItem v-if="task.confirmedTime" label="确认时间">
            {{ pluginTime(task.confirmedTime) }}
          </NDescriptionsItem>
          <NDescriptionsItem v-if="task.cancelledTime" label="取消时间">
            {{ pluginTime(task.cancelledTime) }}
          </NDescriptionsItem>
        </NDescriptions>
        <PluginBuildExecution :execution="task.execution" :busy="busy" @refresh="emit('retry')" />
        <PluginTaskReview :task="task" :busy="busy" @review="emit('review')" />
        <!-- 已知阻断与尚未核验范围 -->
        <NAlert v-for="blocker in task.preview.blockers" :key="blocker" type="error">
          {{ blocker }}
        </NAlert>
        <NAlert type="warning" title="执行边界" :bordered="false">
          <ul>
            <li v-for="warning in task.preview.warnings" :key="warning">
              {{ warning }}
            </li>
          </ul>
        </NAlert>
        <p>
          包内 {{ task.preview.source.fileCount }} 个文件 · 展开 {{ size(task.preview.source.expandedBytes) }}；
          以下仅展示前 {{ task.preview.source.files.length }} 项。完整包将在执行器重新校验。
        </p>
        <NDataTable
          :columns="fileColumns" :data="task.preview.source.files" :max-height="280" :scroll-x="600" size="small"
        />
        <!-- 明确确认只排队，不执行构建 -->
        <NCheckbox v-if="canConfirm && task.status === 'await_confirmation'" v-model:checked="acknowledged">
          已理解整包替换及数据库风险；确认仅进入待构建，不立即安装或部署。
        </NCheckbox>
      </div>
      <template #footer>
        <NSpace>
          <NButton :disabled="busy" @click="emit('close')">
            关闭
          </NButton>
          <NButton v-if="canCancel && cancellable" :loading="busy" @click="emit('cancel')">
            取消任务
          </NButton>
          <NButton
            v-if="canConfirm && task?.status === 'await_confirmation'" type="primary"
            :disabled="!acknowledged || busy" :loading="busy" @click="emit('confirm')"
          >
            确认待构建
          </NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<script setup>
import {
  NAlert,
  NButton,
  NCheckbox,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NDrawer,
  NDrawerContent,
  NSkeleton,
  NSpace,
} from 'naive-ui'
import { computed, ref, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { pluginTime } from '../pluginTaskUtils'
import { usePluginPermission } from '../usePluginPermission'
import PluginBuildExecution from './PluginBuildExecution.vue'
import PluginTaskReview from './PluginTaskReview.vue'

const props = defineProps({
  show: Boolean,
  loading: Boolean,
  busy: Boolean,
  task: { type: Object, default: null },
  error: { type: String, default: '' },
  actionError: { type: String, default: '' },
})
const emit = defineEmits(['close', 'retry', 'confirm', 'cancel', 'review'])
const { dict } = useDict('sys_plugin_task_status', 'sys_plugin_task_operation')
const permission = usePluginPermission()
const canConfirm = computed(() => permission('system:plugin:confirm'))
const canCancel = computed(() => permission('system:plugin:cancel'))
const acknowledged = ref(false)
watch(() => [props.task?.id, props.task?.revision, props.show], () => {
  acknowledged.value = false
})
const cancellable = computed(() => ['await_confirmation', 'queued'].includes(props.task?.status))
const size = bytes => `${(bytes / 1024).toFixed(1)} KiB`
const fileColumns = [
  { title: '包内相对路径', key: 'path', minWidth: 260, ellipsis: { tooltip: true } },
  { title: '大小', key: 'bytes', width: 100, render: row => size(row.bytes) },
  { title: 'SHA-256', key: 'sha256', width: 250, ellipsis: { tooltip: true } },
]
</script>

<style scoped>
.plugin-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.plugin-preview code,
.plugin-preview :deep(.n-descriptions-table-content) {
  overflow-wrap: anywhere;
}
.plugin-preview p,
.plugin-preview li {
  font-size: 12px;
  line-height: 1.6;
}
.plugin-preview ul {
  padding-left: 18px;
  margin: 0;
}
</style>
