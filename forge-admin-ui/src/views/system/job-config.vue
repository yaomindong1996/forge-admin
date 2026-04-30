<template>
  <div class="job-config-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon job-icon">
            <i class="i-material-symbols:schedule-rounded" />
          </div>
          <h2 class="page-title">
            定时任务管理
          </h2>
        </div>
        <div class="header-desc">
          定时任务调度配置，支持Cron表达式与任务执行控制
        </div>
      </div>
      <div class="header-right">
        <NButton type="warning" size="small" @click="handleCleanLogs(7)">
          <template #icon>
            <i class="i-material-symbols:delete-sweep-outline" />
          </template>
          清理7天前日志
        </NButton>
        <NButton type="error" size="small" @click="handleCleanLogs(0)">
          <template #icon>
            <i class="i-material-symbols:delete-forever-outline" />
          </template>
          清空所有日志
        </NButton>
      </div>
    </div>

    <!-- 任务列表 -->
    <div class="job-content">
      <AiCrudPage
        ref="crudRef"
        :api-config="{
          list: 'get@/job/config/page',
          detail: 'get@/job/config/{id}',
          add: 'post@/job/config',
          update: 'put@/job/config',
          delete: 'delete@/job/config/{id}',
        }"
        :search-schema="searchSchema"
        :columns="tableColumns"
        :edit-schema="editSchema"
        row-key="id"
        :edit-grid-cols="1"
        modal-width="900px"
        add-button-text="新增定时任务"
        :before-submit="beforeSubmit"
        modal-type="modal"
      >
        <!-- Cron表达式自定义插槽 -->
        <template #form-cronExpression="{ value, updateValue }">
          <div class="flex items-center gap-8" style="width: 100%">
            <NInput
              :value="value"
              placeholder="请输入Cron表达式，如：0 0 12 * * ?"
              style="flex: 1"
              @update:value="updateValue"
            />
            <NPopover
              trigger="click"
              placement="bottom-start"
              scrollable
              :style="{ maxHeight: '500px' }"
            >
              <template #trigger>
                <NButton text type="primary" size="small">
                  选择常用
                </NButton>
              </template>
              <div class="cron-selector-list">
                <div
                  v-for="item in commonCronList"
                  :key="item.expression"
                  class="cron-selector-item"
                  @click="updateValue(item.expression)"
                >
                  <div class="cron-desc">
                    {{ item.description }}
                  </div>
                  <code class="cron-expr">{{ item.expression }}</code>
                </div>
              </div>
            </NPopover>
          </div>
        </template>
      </AiCrudPage>
    </div>

    <!-- 运行日志弹窗 -->
    <n-modal
      v-model:show="logModalVisible"
      :title="`运行日志 - ${currentJob.jobName || ''}`"
      preset="card"
      style="width: 90%; max-width: 1400px"
      :mask-closable="false"
    >
      <JobLogList ref="logListRef" :job-name="currentJob.jobName" />
      <template #footer>
        <n-space justify="end">
          <NButton @click="logModalVisible = false">
            关闭
          </NButton>
          <NButton type="primary" @click="handleRefreshLog">
            <template #icon>
              <i class="i-material-symbols:refresh" />
            </template>
            刷新
          </NButton>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NInput, NPopover, NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'
import JobLogList from './job-log-list.vue'

defineOptions({ name: 'JobConfig' })

const crudRef = ref(null)
const logModalVisible = ref(false)
const logListRef = ref(null)
const currentJob = ref({})

// 常用的Cron表达式
const commonCronList = [
  { description: '每分钟执行一次', expression: '0 * * * * ?' },
  { description: '每5分钟执行一次', expression: '0 0/5 * * * ?' },
  { description: '每10分钟执行一次', expression: '0 0/10 * * * ?' },
  { description: '每15分钟执行一次', expression: '0 0/15 * * * ?' },
  { description: '每30分钟执行一次', expression: '0 0/30 * * * ?' },
  { description: '每小时执行一次', expression: '0 0 * * * ?' },
  { description: '每天凌晨1点执行', expression: '0 0 1 * * ?' },
  { description: '每天凌晨2点执行', expression: '0 0 2 * * ?' },
  { description: '每天上午9点执行', expression: '0 0 9 * * ?' },
  { description: '每天中午12点执行', expression: '0 0 12 * * ?' },
  { description: '每天下午6点执行', expression: '0 0 18 * * ?' },
  { description: '每天晚上11点执行', expression: '0 0 23 * * ?' },
  { description: '每周一上午9点执行', expression: '0 0 9 ? * MON' },
  { description: '每月1号凌晨1点执行', expression: '0 0 1 1 * ?' },
  { description: '每天上午9点到下午6点，每小时执行一次', expression: '0 0 9-18 * * ?' },
  { description: '工作日上午9点到下午6点，每小时执行一次', expression: '0 0 9-18 ? * MON-FRI' },
]

// 搜索表单
const searchSchema = [
  {
    field: 'jobName',
    label: '任务名称',
    type: 'input',
    props: {
      placeholder: '请输入任务名称',
    },
  },
  {
    field: 'jobGroup',
    label: '任务分组',
    type: 'input',
    props: {
      placeholder: '请输入任务分组',
    },
  },
  {
    field: 'status',
    label: '任务状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: [
        { label: '全部', value: null },
        { label: '停止', value: 0 },
        { label: '运行中', value: 1 },
      ],
      clearable: true,
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'jobName',
    label: '任务名称',
    width: 150,
    ellipsis: { tooltip: true },
  },
  {
    prop: 'jobGroup',
    label: '任务分组',
    width: 120,
  },
  {
    prop: 'executeMode',
    label: '执行模式',
    width: 100,
    render: (row) => {
      const modeMap = {
        BEAN: { text: 'Bean', type: 'info' },
        HANDLER: { text: 'Handler', type: 'success' },
      }
      const config = modeMap[row.executeMode] || { text: row.executeMode || '-', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'executorBean',
    label: 'Bean名称',
    width: 150,
    ellipsis: { tooltip: true },
    render: row => row.executorBean || '-',
  },
  {
    prop: 'executorMethod',
    label: '方法名',
    width: 120,
    render: row => row.executorMethod || '-',
  },
  {
    prop: 'executorHandler',
    label: 'Handler',
    width: 120,
    ellipsis: { tooltip: true },
    render: row => row.executorHandler || '-',
  },
  {
    prop: 'cronExpression',
    label: 'Cron表达式',
    width: 140,
    render: row => h('code', { class: 'text-primary text-12' }, row.cronExpression),
  },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    render: (row) => {
      const statusMap = {
        0: { text: '停止', type: 'warning' },
        1: { text: '运行中', type: 'success' },
      }
      const config = statusMap[row.status] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'description',
    label: '任务描述',
    minWidth: 150,
    ellipsis: { tooltip: true },
    render: row => row.description || '-',
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '启动', key: 'start', type: 'primary', onClick: handleStart, visible: row => row.status === 0 },
      { label: '停止', key: 'stop', type: 'primary', onClick: handleStop, visible: row => row.status !== 0 },
      { label: '运行一次', key: 'trigger', type: 'primary', onClick: handleTrigger },
      { label: '运行日志', key: 'log', type: 'primary', onClick: handleViewLog },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = [
  {
    type: 'divider',
    label: '基本信息',
    props: { titlePlacement: 'left' },
  },
  {
    field: 'jobName',
    label: '任务名称',
    type: 'input',
    rules: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
    props: { placeholder: '请输入任务名称（唯一标识）' },
  },
  {
    field: 'jobGroup',
    label: '任务分组',
    type: 'input',
    defaultValue: 'DEFAULT',
    rules: [{ required: true, message: '请输入任务分组', trigger: 'blur' }],
    props: { placeholder: '请输入任务分组，如：DEFAULT' },
  },
  {
    field: 'description',
    label: '任务描述',
    type: 'textarea',
    props: { placeholder: '请输入任务描述', rows: 2 },
  },
  {
    type: 'divider',
    label: '执行配置',
    props: { titlePlacement: 'left' },
  },
  {
    field: 'executeMode',
    label: '执行模式',
    type: 'radio',
    defaultValue: 'BEAN',
    rules: [{ required: true, message: '请选择执行模式', trigger: 'change' }],
    props: {
      options: [
        { label: 'Bean模式（本地调用）', value: 'BEAN' },
        { label: 'Handler模式（扩展调用）', value: 'HANDLER' },
      ],
    },
  },
  {
    field: 'executorBean',
    label: 'Bean名称',
    type: 'input',
    vIf: formData => formData.executeMode === 'BEAN',
    rules: [{ required: true, message: '请输入Bean名称', trigger: 'blur' }],
    props: { placeholder: '请输入Spring Bean名称，如：demoJob' },
  },
  {
    field: 'executorMethod',
    label: '方法名',
    type: 'input',
    vIf: formData => formData.executeMode === 'BEAN',
    rules: [{ required: true, message: '请输入方法名', trigger: 'blur' }],
    props: { placeholder: '请输入要执行的方法名，如：execute' },
  },
  {
    field: 'executorHandler',
    label: 'Handler名称',
    type: 'input',
    vIf: formData => formData.executeMode === 'HANDLER',
    rules: [{ required: true, message: '请输入Handler名称', trigger: 'blur' }],
    props: { placeholder: '请输入Handler名称' },
  },
  {
    field: 'jobParam',
    label: '任务参数',
    type: 'textarea',
    props: { placeholder: '请输入任务参数（JSON格式，可选）', rows: 2 },
  },
  {
    type: 'divider',
    label: '调度配置',
    props: { titlePlacement: 'left' },
  },
  {
    field: 'cronExpression',
    label: 'Cron表达式',
    type: 'slot',
    slotName: 'cronExpression',
    rules: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }],
  },
  {
    field: 'status',
    label: '任务状态',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: [
        { label: '停止', value: 0 },
        { label: '运行', value: 1 },
      ],
    },
  },
  {
    type: 'divider',
    label: '高级配置',
    props: { titlePlacement: 'left' },
  },
  {
    field: 'retryCount',
    label: '失败重试次数',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '失败后重试次数',
      min: 0,
      max: 5,
    },
  },
  {
    field: 'alarmEmail',
    label: '告警邮箱',
    type: 'input',
    props: { placeholder: '失败时发送告警的邮箱地址（可选）' },
  },
  {
    field: 'webhookUrl',
    label: 'WebHook地址',
    type: 'input',
    props: { placeholder: '失败时回调的WebHook地址（可选）' },
  },
]

// 表单提交前处理
function beforeSubmit(formData) {
  // 清理不需要的字段
  const data = { ...formData }

  // 根据执行模式清理多余字段
  if (data.executeMode === 'BEAN') {
    delete data.executorHandler
  }
  else if (data.executeMode === 'HANDLER') {
    delete data.executorBean
    delete data.executorMethod
  }

  return data
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除任务"${row.jobName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/job/config/${row.id}`)
        window.$message.success('删除成功')
        crudRef.value?.refresh()
      }
      catch (error) {
        console.error('删除失败:', error)
      }
    },
  })
}

// 启动任务
async function handleStart(row) {
  try {
    await request.post(`/job/config/${row.id}/start`)
    window.$message.success('启动成功')
    crudRef.value?.refresh()
  }
  catch (error) {
    console.error('启动失败:', error)
  }
}

// 停止任务
async function handleStop(row) {
  window.$dialog.warning({
    title: '确认停止',
    content: `确定要停止任务"${row.jobName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.post(`/job/config/${row.id}/stop`)
        window.$message.success('停止成功')
        crudRef.value?.refresh()
      }
      catch (error) {
        console.error('停止失败:', error)
      }
    },
  })
}

// 立即执行一次
async function handleTrigger(row) {
  try {
    window.$message.loading('正在执行...', { key: 'trigger', duration: 0 })
    await request.post(`/job/config/${row.id}/trigger`)
    window.$message.success('任务已提交执行', { key: 'trigger' })
  }
  catch (error) {
    window.$message.error('执行失败', { key: 'trigger' })
    console.error('执行失败:', error)
  }
}

// 查看运行日志
function handleViewLog(row) {
  currentJob.value = row
  logModalVisible.value = true
}

// 刷新日志
function handleRefreshLog() {
  logListRef.value?.refresh()
}

// 清理日志
function handleCleanLogs(days) {
  const title = days === 0 ? '清空所有日志' : `清理${days}天前日志`
  const content = days === 0
    ? '确定要清空所有任务日志吗？此操作不可恢复！'
    : `确定要清理${days}天前的任务日志吗？`

  window.$dialog.warning({
    title,
    content,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.delete('/job/log/clean', { params: { days } })
        const count = res.data || 0
        window.$message.success(`清理成功，共清理 ${count} 条日志`)
      }
      catch (error) {
        window.$message.error('清理失败')
        console.error('清理失败:', error)
      }
    },
  })
}
</script>

<style scoped>
.job-config-page {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 页面头部 */
.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.title-icon.job-icon {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-desc {
  font-size: 12px;
  color: #64748b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 任务列表内容 */
.job-content {
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.job-content :deep(.ai-crud-page) {
  height: 100%;
}

/* Cron选择器样式 */
.job-content :deep(.cron-selector-list) {
  max-height: 450px;
  overflow-y: auto;
  padding: 8px;
  width: 500px;
}

.job-content :deep(.cron-selector-item) {
  padding: 12px 16px;
  margin-bottom: 8px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}

.job-content :deep(.cron-selector-item:hover) {
  border-color: #f59e0b;
  background: #fffbeb;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.1);
}

.job-content :deep(.cron-desc) {
  font-size: 14px;
  color: #262626;
  margin-bottom: 6px;
  font-weight: 500;
}

.job-content :deep(.cron-expr) {
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 12px;
  color: #f59e0b;
  background: #fffbeb;
  padding: 4px 10px;
  border-radius: 4px;
  display: inline-block;
}

/* 深色模式 */
.dark .page-header {
  background: #0f172a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark .page-title {
  color: #f1f5f9;
}

.dark .header-desc {
  color: #94a3b8;
}

.dark .job-content {
  background: #0f172a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.dark .job-content :deep(.cron-selector-item) {
  background: #1e293b;
  border-color: #334155;
}

.dark .job-content :deep(.cron-selector-item:hover) {
  border-color: #f59e0b;
  background: #292524;
}

.dark .job-content :deep(.cron-desc) {
  color: #f1f5f9;
}

.dark .job-content :deep(.cron-expr) {
  color: #fbbf24;
  background: #422006;
}
</style>
