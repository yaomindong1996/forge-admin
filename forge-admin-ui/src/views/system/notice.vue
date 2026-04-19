<template>
  <div class="system-notice-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/notice"
      :api-config="{
        list: 'get@/system/notice/page',
        detail: 'post@/system/notice/getById',
        add: 'post@/system/notice/add',
        update: 'post@/system/notice/edit',
        delete: 'post@/system/notice/remove',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="noticeId"
      add-button-text="新增公告"
      :load-detail-on-edit="true"
      :edit-grid-cols="1"
      modal-width="1000px"
    >
      <!-- 富文本编辑器插槽 -->
      <template #form-noticeContent="{ value, updateValue }">
        <n-input
          :value="value"
          type="textarea"
          placeholder="请输入公告内容"
          :rows="10"
          @update:value="updateValue"
        />
      </template>

      <!-- 发布范围插槽 -->
      <template #form-publishScope="{ value, updateValue }">
        <n-radio-group :value="value" @update:value="updateValue">
          <n-radio :value="0">
            全部组织
          </n-radio>
          <n-radio :value="1">
            指定组织
          </n-radio>
        </n-radio-group>
      </template>

      <!-- 组织选择器插槽 -->
      <template #form-orgIds="{ value, updateValue }">
        <n-tree-select
          :value="value"
          :options="orgTreeOptions"
          multiple
          cascade
          checkable
          placeholder="请选择组织"
          clearable
          check-strategy="child"
          @update:value="updateValue"
        />
      </template>
    </AiCrudPage>

    <!-- 已读/未读统计弹窗 -->
    <n-modal
      v-model:show="showStatisticsModal"
      title="已读/未读统计"
      preset="card"
      style="width: 900px"
      :segmented="{ content: 'soft' }"
    >
      <div v-if="currentStatistics" class="statistics-content">
        <n-space vertical size="large">
          <!-- 统计数据 -->
          <n-card title="阅读情况" size="small">
            <n-space>
              <NStatistic label="应读人数" :value="currentStatistics.totalUserCount" />
              <NStatistic label="已读人数" :value="currentStatistics.readCount">
                <template #suffix>
                  <NTag type="success" size="small">
                    {{ currentStatistics.readRate }}%
                  </NTag>
                </template>
              </NStatistic>
              <NStatistic label="未读人数" :value="currentStatistics.unreadCount">
                <template #suffix>
                  <NTag type="warning" size="small">
                    {{ (100 - currentStatistics.readRate).toFixed(2) }}%
                  </NTag>
                </template>
              </NStatistic>
            </n-space>
          </n-card>

          <!-- 已读用户列表 -->
          <n-card title="已读用户列表" size="small">
            <n-data-table
              :columns="readUserColumns"
              :data="readUserList"
              :pagination="{ pageSize: 5 }"
              :max-height="300"
              size="small"
            />
          </n-card>

          <!-- 未读用户列表 -->
          <n-card title="未读用户列表" size="small">
            <n-data-table
              :columns="unreadUserColumns"
              :data="unreadUserList"
              :pagination="{ pageSize: 5 }"
              :max-height="300"
              size="small"
            />
          </n-card>
        </n-space>
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { NStatistic, NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemNotice' })

const crudRef = ref(null)
const showStatisticsModal = ref(false)
const currentStatistics = ref(null)
const readUserList = ref([])
const unreadUserList = ref([])
const orgTreeOptions = ref([])

// 公告类型选项
const noticeTypeOptions = [
  { label: '通知公告', value: 'NOTICE' },
  { label: '系统公告', value: 'ANNOUNCEMENT' },
  { label: '新闻动态', value: 'NEWS' },
]

// 发布状态选项
const publishStatusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已撤回', value: 2 },
]

// 是否置顶选项
const isTopOptions = [
  { label: '否', value: 0 },
  { label: '是', value: 1 },
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'noticeTitle',
    label: '公告标题',
    type: 'input',
    props: {
      placeholder: '请输入公告标题',
    },
  },
  {
    field: 'noticeType',
    label: '公告类型',
    type: 'select',
    props: {
      placeholder: '请选择公告类型',
      options: noticeTypeOptions,
    },
  },
  {
    field: 'publishStatus',
    label: '发布状态',
    type: 'select',
    props: {
      placeholder: '请选择发布状态',
      options: publishStatusOptions,
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'noticeId',
    label: '公告ID',
    width: 100,
  },
  {
    prop: 'noticeTitle',
    label: '公告标题',
    width: 250,
    render: (row) => {
      return h('div', { class: 'flex items-center' }, [
        row.isTop === 1 ? h(NTag, { type: 'error', size: 'small', style: { marginRight: '8px' } }, { default: () => '置顶' }) : null,
        h('span', row.noticeTitle),
      ])
    },
  },
  {
    prop: 'noticeType',
    label: '公告类型',
    width: 120,
    render: (row) => {
      const typeMap = {
        NOTICE: { text: '通知公告', type: 'info' },
        ANNOUNCEMENT: { text: '系统公告', type: 'warning' },
        NEWS: { text: '新闻动态', type: 'success' },
      }
      const config = typeMap[row.noticeType] || { text: row.noticeType, type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'publishStatus',
    label: '发布状态',
    width: 100,
    render: (row) => {
      const statusMap = {
        0: { text: '草稿', type: 'default' },
        1: { text: '已发布', type: 'success' },
        2: { text: '已撤回', type: 'warning' },
      }
      const config = statusMap[row.publishStatus] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'publisherName',
    label: '发布人',
    width: 120,
  },
  {
    prop: 'publishTime',
    label: '发布时间',
    width: 180,
  },
  {
    prop: 'readCount',
    label: '阅读次数',
    width: 100,
  },
  {
    prop: 'effectiveTime',
    label: '生效时间',
    width: 180,
  },
  {
    prop: 'expirationTime',
    label: '失效时间',
    width: 180,
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '统计', key: 'statistics', type: 'info', onClick: handleViewStatistics, visible: row => row.publishStatus === 1 },
      { label: '发布', key: 'publish', type: 'success', onClick: handlePublish, visible: row => row.publishStatus === 0 },
      { label: '撤回', key: 'revoke', type: 'warning', onClick: handleRevoke, visible: row => row.publishStatus === 1 },
      { label: '置顶', key: 'top', onClick: handleTop, visible: row => row.isTop !== 1 },
      { label: '取消置顶', key: 'untop', type: 'warning', onClick: handleTop, visible: row => row.isTop === 1 },
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
  },
  {
    field: 'noticeTitle',
    label: '公告标题',
    type: 'input',
    rules: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
    props: {
      placeholder: '请输入公告标题',
    },
  },
  {
    field: 'noticeType',
    label: '公告类型',
    type: 'select',
    defaultValue: 'NOTICE',
    rules: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
    props: {
      placeholder: '请选择公告类型',
      options: noticeTypeOptions,
    },
  },
  {
    field: 'noticeContent',
    label: '公告内容',
    type: 'slot',
    slotName: 'noticeContent',
    rules: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  },
  {
    type: 'divider',
    label: '发布设置',
    props: {
      titlePlacement: 'left',
    },
  },
  {
    field: 'publishScope',
    label: '发布范围',
    type: 'slot',
    slotName: 'publishScope',
    defaultValue: 0,
    props: {
      placeholder: '请选择发布范围',
    },
  },
  {
    field: 'orgIds',
    label: '指定组织',
    type: 'slot',
    slotName: 'orgIds',
    vIf: formData => formData.publishScope === 1,
    rules: [
      {
        type: 'array',
        required: true,
        message: '请选择组织',
        trigger: 'change',
      },
    ],
  },
  {
    field: 'attachmentIds',
    label: '附件',
    type: 'fileUpload',
    props: {
      multiple: true,
      limit: 10,
      fileSize: 50,
      accept: '*',
      businessType: 'notice',
      valueType: 'string',
    },
  },
  {
    field: 'effectiveTime',
    label: '生效时间',
    type: 'datetime',
    props: {
      placeholder: '请选择生效时间',
      type: 'datetime',
      clearable: true,
    },
  },
  {
    field: 'expirationTime',
    label: '失效时间',
    type: 'datetime',
    props: {
      placeholder: '请选择失效时间',
      type: 'datetime',
      clearable: true,
    },
  },
  {
    field: 'isTop',
    label: '是否置顶',
    type: 'radio',
    defaultValue: 0,
    props: {
      options: isTopOptions,
    },
  },
  {
    field: 'topSort',
    label: '置顶排序',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '数字越大越靠前',
      min: 0,
    },
    vIf: formData => formData.isTop === 1,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    props: {
      placeholder: '请输入备注',
      rows: 3,
    },
  },
]

// 已读用户列表表格列
const readUserColumns = [
  { title: '用户姓名', key: 'userName', width: 120 },
  { title: '组织名称', key: 'orgName', width: 150 },
  { title: '手机号', key: 'phone', width: 120 },
  { title: '阅读时间', key: 'readTime', width: 180 },
]

// 未读用户列表表格列
const unreadUserColumns = [
  { title: '用户姓名', key: 'userName', width: 120 },
  { title: '组织名称', key: 'orgName', width: 150 },
  { title: '手机号', key: 'phone', width: 120 },
  { title: '邮箱', key: 'email', ellipsis: { tooltip: true } },
]

// 加载组织树数据
async function loadOrgTree() {
  try {
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      orgTreeOptions.value = transformOrgTree(res.data)
    }
  }
  catch (error) {
    console.error('加载组织树失败:', error)
  }
}

// 转换组织树数据格式
function transformOrgTree(data) {
  if (!data || data.length === 0)
    return []
  return data.map(item => ({
    key: item.id,
    label: item.orgName,
    children: item.children ? transformOrgTree(item.children) : undefined,
  }))
}

// 查看统计
async function handleViewStatistics(row) {
  try {
    // 加载统计数据
    const statsRes = await request.get(`/system/notice/statistics/${row.noticeId}`)
    if (statsRes.code === 200) {
      currentStatistics.value = statsRes.data
    }

    // 加载已读用户列表
    const readRes = await request.get(`/system/notice/read-users/${row.noticeId}`)
    if (readRes.code === 200) {
      readUserList.value = readRes.data || []
    }

    // 加载未读用户列表
    const unreadRes = await request.get(`/system/notice/unread-users/${row.noticeId}`)
    if (unreadRes.code === 200) {
      unreadUserList.value = unreadRes.data || []
    }

    showStatisticsModal.value = true
  }
  catch (error) {
    window.$message.error('加载统计数据失败')
  }
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该公告吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/remove', null, {
          params: { noticeId: row.noticeId },
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('删除失败')
      }
    },
  })
}

// 发布公告
function handlePublish(row) {
  window.$dialog.info({
    title: '确认发布',
    content: '确定要发布该公告吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/publish', null, {
          params: { noticeId: row.noticeId },
        })
        if (res.code === 200) {
          window.$message.success('发布成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('发布失败')
      }
    },
  })
}

// 撤回公告
function handleRevoke(row) {
  window.$dialog.warning({
    title: '确认撤回',
    content: '确定要撤回该公告吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/revoke', null, {
          params: { noticeId: row.noticeId },
        })
        if (res.code === 200) {
          window.$message.success('撤回成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('撤回失败')
      }
    },
  })
}

// 置顶/取消置顶
function handleTop(row) {
  const isTop = row.isTop === 1 ? 0 : 1
  const title = isTop === 1 ? '置顶公告' : '取消置顶'

  window.$dialog.info({
    title: `确认${title}`,
    content: `确定要${title}该公告吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/notice/top', null, {
          params: {
            noticeId: row.noticeId,
            isTop,
            topSort: isTop === 1 ? (row.topSort || 0) : 0,
          },
        })
        if (res.code === 200) {
          window.$message.success(`${title}成功`)
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error(`${title}失败`)
      }
    },
  })
}

onMounted(() => {
  loadOrgTree()
})
</script>

<style scoped>
.system-notice-page {
  height: 100%;
}
</style>
