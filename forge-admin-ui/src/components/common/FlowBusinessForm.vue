<template>
  <div class="flow-business-form">
    <!-- 加载中 -->
    <div v-if="loading" class="form-loading">
      <n-spin size="medium" />
      <span style="margin-left: 10px; color: #888">加载业务表单中...</span>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="loadError" class="form-error">
      <n-result status="warning" title="表单加载失败" :description="loadError">
        <template #footer>
          <n-button @click="loadFormComponent">
            重试
          </n-button>
        </template>
      </n-result>
    </div>

    <!-- 动态渲染业务表单组件 -->
    <component
      :is="formComponent"
      v-else-if="formComponent"
      :task-id="taskId"
      :business-key="businessKey"
      :process-instance-id="processInstanceId"
      :task-def-key="taskDefKey"
      :process-def-key="processDefKey"
      :variables="variables"
      :read-only="readOnly"
      @submit="handleFormSubmit"
      @cancel="$emit('cancel')"
    />

    <!-- 无表单时降级展示 -->
    <n-empty
      v-else
      description="该节点无自定义业务表单"
      size="medium"
      style="padding: 40px 0"
    />
  </div>
</template>

<script setup>
import { ref, shallowRef, watch } from 'vue'

/**
 * 流程业务表单路由组件（全自动模式）
 *
 * 设计思路：
 * - 利用 Vite import.meta.glob 自动扫描 @/views 下所有 .vue 文件
 * - formUrl 即为组件相对路径（去掉 /src/views 前缀和 .vue 后缀）
 *   例如：节点配置 formUrl = "/leave/LeaveApproveForm"
 *        对应组件 src/views/leave/LeaveApproveForm.vue
 * - 新增业务表单无需任何配置，只需将组件放到 @/views 下对应目录即可
 * - 支持大小写不敏感匹配和去查询参数匹配
 */

const props = defineProps({
  /** 任务 ID */
  taskId: { type: String, default: null },
  /** 业务 Key */
  businessKey: { type: String, default: null },
  /** 流程实例 ID */
  processInstanceId: { type: String, default: null },
  /** 任务节点 Key */
  taskDefKey: { type: String, default: null },
  /** 流程定义 Key */
  processDefKey: { type: String, default: null },
  /** 表单 URL（节点配置的 formUrl 字段，如 /leave/LeaveApproveForm） */
  formUrl: { type: String, default: null },
  /** 流程变量 */
  variables: { type: Object, default: () => ({}) },
  /** 是否只读（已办/发起人查看） */
  readOnly: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'cancel'])

const loading = ref(false)
const loadError = ref(null)
const formComponent = shallowRef(null)

// ============================================================
// 利用 Vite import.meta.glob 自动扫描 @/views 下所有 .vue 文件
// key 格式：/src/views/leave/LeaveApproveForm.vue
// ============================================================
const ALL_VIEW_MODULES = import.meta.glob('@/views/**/*.vue')

/**
 * 将 formUrl 解析为 glob key
 * formUrl 示例：
 *   /leave/LeaveApproveForm
 *   /leave/LeaveApproveForm?readOnly=true  （带查询参数，自动剥离）
 */
function resolveLoader(formUrl) {
  if (!formUrl)
    return null

  // 去掉查询参数
  const cleanUrl = formUrl.split('?')[0]

  // 拼接为 glob key：/src/views + cleanUrl + .vue
  const exactKey = `/src/views${cleanUrl}.vue`
  if (ALL_VIEW_MODULES[exactKey]) {
    return ALL_VIEW_MODULES[exactKey]
  }

  // 大小写不敏感兜底：遍历所有 key 进行小写比较
  const lowerKey = exactKey.toLowerCase()
  const fallbackEntry = Object.entries(ALL_VIEW_MODULES).find(
    ([k]) => k.toLowerCase() === lowerKey,
  )
  if (fallbackEntry) {
    return fallbackEntry[1]
  }

  return null
}

/**
 * 根据 formUrl 自动查找并加载对应业务组件
 */
async function loadFormComponent() {
  if (!props.formUrl) {
    formComponent.value = null
    return
  }

  const loader = resolveLoader(props.formUrl)

  if (!loader) {
    console.warn(`[FlowBusinessForm] 未找到对应组件：${props.formUrl}，请确认 formUrl 与组件路径一致`)
    formComponent.value = null
    loadError.value = null
    return
  }

  loading.value = true
  loadError.value = null
  formComponent.value = null

  try {
    const mod = await loader()
    formComponent.value = mod.default || mod
  }
  catch (e) {
    console.error('[FlowBusinessForm] 加载业务表单失败:', e)
    loadError.value = `无法加载表单组件：${props.formUrl}`
  }
  finally {
    loading.value = false
  }
}

function handleFormSubmit(data) {
  emit('submit', data)
}

// 监听 formUrl 变化重新加载
watch(() => props.formUrl, loadFormComponent, { immediate: true })
</script>

<style scoped>
.flow-business-form {
  min-height: 120px;
}
.form-loading {
  display: flex;
  align-items: center;
  padding: 40px 0;
  justify-content: center;
}
.form-error {
  padding: 20px 0;
}
</style>
