<template>
  <div class="leave-approve-form">
    <!-- 申请详情（只读展示） -->
    <n-card class="detail-card" size="small">
      <template #header>
        <div class="card-header">
          <i class="i-material-symbols:description text-primary" />
          请假申请详情
        </div>
      </template>

      <n-descriptions :column="2" label-placement="left" size="small" bordered>
        <n-descriptions-item label="申请人">
          <div class="user-row">
            <n-avatar :size="20" round style="background: #18a058; font-size: 11px">
              {{ (leaveInfo.applyUserName || '?')[0] }}
            </n-avatar>
            <span style="margin-left: 6px">{{ leaveInfo.applyUserName || '-' }}</span>
          </div>
        </n-descriptions-item>
        <n-descriptions-item label="请假类型">
          <n-tag :type="getLeaveTypeTag(leaveInfo.leaveType)" size="small">
            {{ getLeaveTypeText(leaveInfo.leaveType) }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="开始时间">
          {{ formatTime(leaveInfo.startTime) }}
        </n-descriptions-item>
        <n-descriptions-item label="结束时间">
          {{ formatTime(leaveInfo.endTime) }}
        </n-descriptions-item>
        <n-descriptions-item label="请假天数">
          <n-tag type="warning" size="small">
            {{ leaveInfo.duration ?? '-' }} 天
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="申请时间">
          {{ formatTime(leaveInfo.applyTime) }}
        </n-descriptions-item>
        <n-descriptions-item label="请假原因" :span="2">
          <div class="reason-text">
            {{ leaveInfo.reason || '-' }}
          </div>
        </n-descriptions-item>
      </n-descriptions>

      <!-- 附件 -->
      <div v-if="attachments.length > 0" class="attachments">
        <div class="attach-title">
          申请附件
        </div>
        <div class="attach-list">
          <n-tag
            v-for="(att, i) in attachments"
            :key="i"
            size="small"
            style="cursor:pointer"
            @click="previewAttachment(att)"
          >
            <template #icon>
              <i class="i-material-symbols:attach-file" />
            </template>
            {{ att.name }}
          </n-tag>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="detailLoading" class="loading-row">
        <n-spin size="small" />
        <span style="margin-left: 8px; color: #888">加载申请详情...</span>
      </div>
    </n-card>

    <!-- 审批操作区（只读模式下隐藏） -->
    <n-card v-if="!readOnly" class="approve-card" size="small">
      <template #header>
        <div class="card-header">
          <i class="i-material-symbols:rate-review text-primary" />
          审批操作
        </div>
      </template>

      <n-form :model="approveData" label-placement="top" size="medium">
        <n-form-item label="审批意见" required>
          <n-input
            v-model:value="approveData.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见（必填）"
            :maxlength="500"
            show-count
          />
        </n-form-item>

        <!-- 可选：审批附件 -->
        <n-form-item label="审批附件（可选）">
          <n-upload
            :file-list="approveFileList"
            :max="3"
            list-type="text"
            @update:file-list="approveFileList = $event"
          >
            <n-button size="small">
              <template #icon>
                <i class="i-material-symbols:upload-file" />
              </template>
              上传附件
            </n-button>
          </n-upload>
        </n-form-item>
      </n-form>

      <div class="action-buttons">
        <n-popconfirm
          positive-text="确认通过"
          negative-text="取消"
          @positive-click="handleApprove"
        >
          <template #trigger>
            <n-button type="primary" :loading="submitting" size="large">
              <template #icon>
                <i class="i-material-symbols:check-circle" />
              </template>
              同意
            </n-button>
          </template>
          确认同意该请假申请？
        </n-popconfirm>

        <n-popconfirm
          positive-text="确认驳回"
          negative-text="取消"
          @positive-click="handleReject"
        >
          <template #trigger>
            <n-button type="error" :loading="submitting" size="large">
              <template #icon>
                <i class="i-material-symbols:cancel" />
              </template>
              驳回
            </n-button>
          </template>
          确认驳回该请假申请？
        </n-popconfirm>
      </div>
    </n-card>

    <!-- 只读模式下展示审批结论 -->
    <n-card v-else-if="approveResult" class="result-card" size="small">
      <template #header>
        <div class="card-header">
          <i class="i-material-symbols:fact-check text-primary" />
          审批结论
        </div>
      </template>
      <n-alert :type="approveResult === 'approve' ? 'success' : 'error'" :title="approveResult === 'approve' ? '审批通过' : '已驳回'">
        {{ approveComment || '无审批意见' }}
      </n-alert>
    </n-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import leaveApi from '@/api/leave'

const props = defineProps({
  /** 任务 ID */
  taskId: { type: String, default: null },
  /** 业务 Key（请假记录 ID） */
  businessKey: { type: String, default: null },
  /** 流程实例 ID */
  processInstanceId: { type: String, default: null },
  /** 任务节点 Key */
  taskDefKey: { type: String, default: null },
  /** 流程定义 Key */
  processDefKey: { type: String, default: null },
  /** 流程变量（包含申请人填写的表单数据） */
  variables: { type: Object, default: () => ({}) },
  /** 是否只读 */
  readOnly: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'cancel'])

const detailLoading = ref(false)
const submitting = ref(false)
const approveFileList = ref([])

// 请假详情（优先从 variables 取，不足时通过 businessKey 加载）
const leaveInfo = reactive({
  applyUserName: '',
  leaveType: '',
  startTime: null,
  endTime: null,
  duration: null,
  reason: '',
  attachments: '',
  applyTime: null,
})

// 只读模式下的审批结论
const approveResult = computed(() => props.variables?.approveResult)
const approveComment = computed(() => props.variables?.approveComment)

// 附件解析
const attachments = computed(() => {
  if (!leaveInfo.attachments)
    return []
  try {
    const arr = JSON.parse(leaveInfo.attachments)
    return Array.isArray(arr) ? arr : []
  }
  catch {
    return []
  }
})

// 审批表单
const approveData = reactive({ comment: '' })

// 加载申请详情
async function loadDetail() {
  // 先尝试从 variables 中获取
  if (props.variables) {
    const v = props.variables
    leaveInfo.applyUserName = v.startUserName || v.applyUserName || ''
    leaveInfo.leaveType = v.leaveType || ''
    leaveInfo.startTime = v.startTime || null
    leaveInfo.endTime = v.endTime || null
    leaveInfo.duration = v.duration || null
    leaveInfo.reason = v.reason || ''
    leaveInfo.attachments = v.attachments || ''
    leaveInfo.applyTime = v.applyTime || null
  }

  // 若 variables 中业务数据不全，通过 businessKey 补充加载
  if (props.businessKey && !leaveInfo.reason) {
    detailLoading.value = true
    try {
      const res = await leaveApi.getDetail(props.businessKey)
      if (res.code === 200 && res.data) {
        const d = res.data
        leaveInfo.applyUserName = d.applyUserName || leaveInfo.applyUserName
        leaveInfo.leaveType = d.leaveType || leaveInfo.leaveType
        leaveInfo.startTime = d.startTime || leaveInfo.startTime
        leaveInfo.endTime = d.endTime || leaveInfo.endTime
        leaveInfo.duration = d.duration ?? leaveInfo.duration
        leaveInfo.reason = d.reason || leaveInfo.reason
        leaveInfo.attachments = d.attachments || leaveInfo.attachments
        leaveInfo.applyTime = d.applyTime || leaveInfo.applyTime
      }
    }
    catch (e) {
      console.error('[LeaveApproveForm] 加载请假详情失败:', e)
    }
    finally {
      detailLoading.value = false
    }
  }
}

// 同意
function handleApprove() {
  if (!approveData.comment?.trim()) {
    window.$message?.warning('请输入审批意见')
    return
  }
  emit('submit', {
    action: 'approve',
    comment: approveData.comment,
    variables: {
      approveResult: 'approve',
      approveComment: approveData.comment,
    },
  })
}

// 驳回
function handleReject() {
  if (!approveData.comment?.trim()) {
    window.$message?.warning('请输入驳回原因')
    return
  }
  emit('submit', {
    action: 'reject',
    comment: approveData.comment,
    variables: {
      approveResult: 'reject',
      approveComment: approveData.comment,
    },
  })
}

// 预览附件
function previewAttachment(att) {
  if (att.url)
    window.open(att.url, '_blank')
}

// 工具函数
function formatTime(val) {
  if (!val)
    return '-'
  const d = new Date(val)
  return isNaN(d.getTime()) ? String(val) : d.toLocaleString('zh-CN', { hour12: false })
}

const leaveTypeMap = {
  annual: { text: '年假', type: 'success' },
  sick: { text: '病假', type: 'warning' },
  personal: { text: '事假', type: 'default' },
  marriage: { text: '婚假', type: 'info' },
  maternity: { text: '产假', type: 'error' },
}
function getLeaveTypeText(t) { return leaveTypeMap[t]?.text || t || '-' }
function getLeaveTypeTag(t) { return leaveTypeMap[t]?.type || 'default' }

onMounted(() => loadDetail())
</script>

<style scoped>
.leave-approve-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}
.text-primary {
  color: #2080f0;
}
.user-row {
  display: flex;
  align-items: center;
}
.reason-text {
  white-space: pre-wrap;
  word-break: break-word;
  color: #555;
}
.attachments {
  margin-top: 12px;
}
.attach-title {
  font-size: 13px;
  color: #888;
  margin-bottom: 6px;
}
.attach-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.loading-row {
  display: flex;
  align-items: center;
  padding: 12px 0;
}
.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}
.detail-card,
.approve-card,
.result-card {
  border-radius: 8px;
}
</style>
