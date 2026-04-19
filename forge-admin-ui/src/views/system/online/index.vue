<template>
  <div class="online-user-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/auth/online/page',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="tokenValue"
      :hide-add="true"
      :hide-selection="true"
      :hide-batch-delete="true"
    />

    <!-- 详情弹窗 -->
    <n-modal
      v-model:show="detailVisible"
      title="在线用户详情"
      preset="card"
      style="width: 800px"
    >
      <div v-if="currentUser" class="user-detail">
        <div class="detail-section">
          <h4 class="section-title">
            用户信息
          </h4>
          <div class="detail-row">
            <span class="label">用户名：</span>
            <span class="value">{{ currentUser.username || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">真实姓名：</span>
            <span class="value">{{ currentUser.realName || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">用户ID：</span>
            <span class="value">{{ currentUser.userId || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">所属部门：</span>
            <span class="value">{{ currentUser.deptName || '-' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">
            登录信息
          </h4>
          <div class="detail-row">
            <span class="label">登录IP：</span>
            <span class="value">{{ currentUser.ipAddress || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">登录地点：</span>
            <span class="value">{{ currentUser.loginLocation || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">浏览器：</span>
            <span class="value">{{ currentUser.browser || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">操作系统：</span>
            <span class="value">{{ currentUser.os || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">登录时间：</span>
            <span class="value">{{ currentUser.loginTime || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">最后活动时间：</span>
            <span class="value">{{ currentUser.lastActivityTime || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">过期时间：</span>
            <span class="value">{{ currentUser.expireTime || '-' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">
            会话信息
          </h4>
          <div class="detail-row">
            <span class="label">Token值：</span>
            <span class="value code">{{ currentUser.tokenValue || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">状态：</span>
            <NTag :type="currentUser.status === 1 ? 'success' : 'default'" size="small">
              {{ currentUser.status === 1 ? '在线' : '离线' }}
            </NTag>
          </div>
          <div class="detail-row">
            <span class="label">封禁状态：</span>
            <NTag :type="currentUser.banned ? 'error' : 'success'" size="small">
              {{ currentUser.banned ? '已封禁' : '正常' }}
            </NTag>
          </div>
        </div>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="detailVisible = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemOnlineUser' })

const crudRef = ref(null)
const detailVisible = ref(false)
const currentUser = ref(null)

// 搜索配置
const searchSchema = [
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    props: {
      placeholder: '支持用户名 / 真实姓名模糊查询',
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'username',
    label: '用户名',
    width: 120,
  },
  {
    prop: 'realName',
    label: '真实姓名',
    width: 120,
  },
  {
    prop: 'deptName',
    label: '部门',
    width: 160,
    ellipsis: true,
  },
  {
    prop: 'ipAddress',
    label: '登录IP',
    width: 130,
  },
  {
    prop: 'loginLocation',
    label: '登录地点',
    width: 150,
    ellipsis: true,
  },
  {
    prop: 'browser',
    label: '浏览器',
    width: 120,
    ellipsis: true,
  },
  {
    prop: 'os',
    label: '操作系统',
    width: 120,
    ellipsis: true,
  },
  {
    prop: 'loginTime',
    label: '登录时间',
    width: 160,
  },
  {
    prop: 'lastActivityTime',
    label: '最后活动时间',
    width: 160,
  },
  {
    prop: 'expireTime',
    label: '过期时间',
    width: 160,
  },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    render(row) {
      const type = row.status === 1 ? 'success' : 'default'
      const text = row.status === 1 ? '在线' : '离线'
      return h(NTag, { type, size: 'small' }, { default: () => text })
    },
  },
  {
    prop: 'banned',
    label: '封禁状态',
    width: 100,
    render(row) {
      const banned = row.banned
      const type = banned ? 'error' : 'success'
      const text = banned ? '已封禁' : '正常'
      return h(NTag, { type, size: 'small' }, { default: () => text })
    },
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '详情', key: 'detail', onClick: handleViewDetail },
      { label: '强制下线', key: 'kickout', type: 'error', onClick: handleKickout },
      { label: '封禁(1小时)', key: 'ban', type: 'warning', onClick: handleBan, visible: row => !row.banned },
      { label: '解封', key: 'unban', onClick: handleUnban, visible: row => !!row.banned },
    ],
  },
])

// 查看详情
function handleViewDetail(row) {
  currentUser.value = row
  detailVisible.value = true
}

function handleKickout(row) {
  window.$dialog.warning({
    title: '确认操作',
    content: `确定要强制下线用户 "${row.username}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        const res = await request.post('/auth/online/kickout', null, {
          params: { tokenValue: row.tokenValue },
        })
        console.log('强制下线结果:', res)
        if (res.code === 200) {
          window.$message?.success('已强制下线')
          crudRef.value?.refresh()
        }
        else {
          window.$message?.error(res.message || '操作失败')
        }
      }
      catch (error) {
        console.error('强制下线失败:', error)
        window.$message?.error('操作失败')
      }
    },
  })
}

function handleBan(row) {
  window.$dialog.warning({
    title: '确认封禁',
    content: `确定要封禁用户 "${row.username}" 1小时并强制下线所有会话吗？`,
    positiveText: '确定',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        const res = await request.post('/auth/online/ban', null, {
          params: {
            userId: row.userId,
            banSeconds: 3600,
            reason: '后台封禁',
          },
        })
        if (res.code === 200) {
          window.$message?.success('封禁成功')
          crudRef.value?.refresh()
        }
        else {
          window.$message?.error(res.message || '操作失败')
        }
      }
      catch (error) {
        console.error('封禁失败:', error)
        window.$message?.error('操作失败')
      }
    },
  })
}

function handleUnban(row) {
  window.$dialog.warning({
    title: '确认解封',
    content: `确定要解封用户 "${row.username}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        const res = await request.post('/auth/online/unban', null, {
          params: { userId: row.userId },
        })
        if (res.code === 200) {
          window.$message?.success('解封成功')
          crudRef.value?.refresh()
        }
        else {
          window.$message?.error(res.message || '操作失败')
        }
      }
      catch (error) {
        console.error('解封失败:', error)
        window.$message?.error('操作失败')
      }
    },
  })
}
</script>

<style scoped lang="scss">
.online-user-page {
  height: 100%;
}

.user-detail {
  .detail-section {
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #333;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid #f0f0f0;
    }

    .detail-row {
      display: flex;
      align-items: flex-start;
      padding: 8px 0;
      font-size: 13px;

      .label {
        width: 120px;
        color: #666;
        flex-shrink: 0;
      }

      .value {
        flex: 1;
        color: #333;
        word-break: break-all;

        &.code {
          font-family: 'Courier New', monospace;
          font-size: 12px;
          background: #f5f5f5;
          padding: 4px 8px;
          border-radius: 4px;
        }
      }
    }
  }
}
</style>
