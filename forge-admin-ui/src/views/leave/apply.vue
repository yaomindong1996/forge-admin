<template>
  <div class="leave-apply p-4">
    <n-card title="请假申请">
      <n-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-placement="left"
        label-width="100"
        require-mark-placement="right-hanging"
      >
        <!-- 请假类型 -->
        <n-form-item label="请假类型" path="leaveType">
          <n-select
            v-model:value="formData.leaveType"
            :options="leaveTypeOptions"
            placeholder="请选择请假类型"
          />
        </n-form-item>

        <!-- 时间选择 -->
        <n-form-item label="开始时间" path="startTime">
          <n-date-picker
            v-model:value="formData.startTime"
            type="datetime"
            placeholder="请选择开始时间"
            style="width: 100%"
          />
        </n-form-item>

        <n-form-item label="结束时间" path="endTime">
          <n-date-picker
            v-model:value="formData.endTime"
            type="datetime"
            placeholder="请选择结束时间"
            style="width: 100%"
            @update:value="calcDuration"
          />
        </n-form-item>

        <!-- 请假天数 -->
        <n-form-item label="请假天数" path="duration">
          <n-input-number
            v-model:value="formData.duration"
            :precision="1"
            :min="0.5"
            :step="0.5"
            placeholder="请假天数"
            style="width: 100%"
          >
            <template #suffix>
              天
            </template>
          </n-input-number>
        </n-form-item>

        <!-- 请假原因 -->
        <n-form-item label="请假原因" path="reason">
          <n-input
            v-model:value="formData.reason"
            type="textarea"
            placeholder="请输入请假原因"
            :rows="4"
            maxlength="500"
            show-count
          />
        </n-form-item>

        <!-- 附件上传 -->
        <n-form-item label="附件">
          <n-upload
            :file-list="fileList"
            :max="5"
            list-type="text"
            @update:file-list="handleFileChange"
          >
            <n-button>
              <template #icon>
                <n-icon><i-mdi-upload /></n-icon>
              </template>
              上传附件
            </n-button>
          </n-upload>
        </n-form-item>
      </n-form>

      <n-space justify="end" class="mt-4">
        <n-button :loading="draftLoading" @click="handleSaveDraft">
          保存草稿
        </n-button>
        <n-button type="primary" :loading="submitLoading" @click="handleSubmit">
          提交申请
        </n-button>
      </n-space>
    </n-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import leaveApi from '@/api/leave'
import { useUserStore } from '@/store'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const submitLoading = ref(false)
const draftLoading = ref(false)
const fileList = ref([])

// 表单数据
const formData = reactive({
  leaveType: null,
  startTime: null,
  endTime: null,
  duration: null,
  reason: '',
  attachments: '',
})

// 请假类型选项
const leaveTypeOptions = [
  { label: '年假', value: 'annual' },
  { label: '病假', value: 'sick' },
  { label: '事假', value: 'personal' },
  { label: '婚假', value: 'marriage' },
  { label: '产假', value: 'maternity' },
]

// 表单验证规则
const rules = {
  leaveType: {
    required: true,
    message: '请选择请假类型',
    trigger: ['blur', 'change'],
  },
  startTime: {
    required: true,
    type: 'number',
    message: '请选择开始时间',
    trigger: ['blur', 'change'],
  },
  endTime: {
    required: true,
    type: 'number',
    message: '请选择结束时间',
    trigger: ['blur', 'change'],
  },
  duration: {
    required: true,
    type: 'number',
    message: '请输入请假天数',
    trigger: ['blur', 'change'],
  },
  reason: {
    required: true,
    message: '请输入请假原因',
    trigger: ['blur', 'input'],
  },
}

// 计算请假天数
function calcDuration() {
  if (formData.startTime && formData.endTime) {
    const start = new Date(formData.startTime)
    const end = new Date(formData.endTime)
    const diff = end - start
    if (diff > 0) {
      // 按工作日计算（简化处理，实际应排除周末和节假日）
      const days = diff / (1000 * 60 * 60 * 24)
      formData.duration = Math.round(days * 10) / 10
    }
    else {
      formData.duration = 0
      window.$message?.warning('结束时间必须大于开始时间')
    }
  }
}

// 处理文件变化
function handleFileChange(files) {
  fileList.value = files
  // 将文件列表转为JSON字符串
  formData.attachments = JSON.stringify(files.map(f => ({
    name: f.name,
    url: f.url || '',
  })))
}

// 提交申请
async function handleSubmit() {
  try {
    await formRef.value?.validate()

    submitLoading.value = true

    // 构建提交数据
    const data = {
      ...formData,
      startTime: formData.startTime ? new Date(formData.startTime).toISOString() : null,
      endTime: formData.endTime ? new Date(formData.endTime).toISOString() : null,
      applyUserId: userStore.userId,
      applyUserName: userStore.username,
    }

    const res = await leaveApi.submit(data)

    window.$message?.success('提交成功')
    router.push('/leave/list')
  }
  catch (error) {
    console.error('提交失败:', error)
    if (error?.response?.data?.message) {
      window.$message?.error(error.response.data.message)
    }
  }
  finally {
    submitLoading.value = false
  }
}

// 保存草稿
async function handleSaveDraft() {
  try {
    draftLoading.value = true

    const data = {
      ...formData,
      startTime: formData.startTime ? new Date(formData.startTime).toISOString() : null,
      endTime: formData.endTime ? new Date(formData.endTime).toISOString() : null,
      applyUserId: userStore.userId,
      applyUserName: userStore.username,
    }

    await leaveApi.saveDraft(data)

    window.$message?.success('草稿保存成功')
    router.push('/leave/list')
  }
  catch (error) {
    console.error('保存草稿失败:', error)
    if (error?.response?.data?.message) {
      window.$message?.error(error.response.data.message)
    }
  }
  finally {
    draftLoading.value = false
  }
}
</script>

<style scoped>
.leave-apply {
  max-width: 800px;
  margin: 0 auto;
}
</style>
