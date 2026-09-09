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
      :key="formComponentKey"
      :task-id="taskId"
      :business-key="businessKey"
      :process-instance-id="processInstanceId"
      :task-def-key="taskDefKey"
      :process-def-key="processDefKey"
      :variables="variables"
      :approval-policy="approvalPolicy"
      :initial-task-context="initialTaskContext"
      :read-only="readOnly"
      :submitting="submitting"
      :submitting-action="submittingAction"
      @submit="handleFormSubmit"
      @cancel="$emit('cancel')"
    >
      <template #actions>
        <slot name="actions" />
      </template>
    </component>

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
import { markRaw, onBeforeUnmount, ref, shallowRef, watch } from 'vue'

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
  /** 审批动作权限和办理要求 */
  approvalPolicy: { type: Object, default: () => ({}) },
  /** 父级已加载的业务表单上下文，避免业务组件重复请求 */
  initialTaskContext: { type: Object, default: null },
  /** 是否只读（已办/发起人查看） */
  readOnly: { type: Boolean, default: false },
  /** 父级审批提交中 */
  submitting: { type: Boolean, default: false },
  /** 父级正在提交的审批动作 */
  submittingAction: { type: String, default: '' },
})

const emit = defineEmits(['submit', 'cancel'])

const loading = ref(false)
const loadError = ref(null)
const formComponent = shallowRef(null)
const formComponentKey = ref(null)
let loadSequence = 0
let disposed = false

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

  // 去掉查询参数和前后空格（防止BPMN XML中带的前导空格）
  const cleanUrl = formUrl.split('?')[0].trim()

  // 构造目标路径
  const targetPath = `${cleanUrl}.vue`
  const expectedKey = `/src/views${targetPath}`

  // 直接遍历所有模块，按路径匹配
  for (const [key, loader] of Object.entries(ALL_VIEW_MODULES)) {
    if (key === expectedKey || key.includes(targetPath) || key.endsWith(targetPath)) {
      return loader
    }
  }

  // 大小写不敏感兜底
  const lowerTarget = targetPath.toLowerCase()
  for (const [key, loader] of Object.entries(ALL_VIEW_MODULES)) {
    if (key.toLowerCase().includes(lowerTarget) || key.toLowerCase().endsWith(lowerTarget)) {
      return loader
    }
  }

  return null
}

/**
 * 根据 formUrl 自动查找并加载对应业务组件
 */
async function loadFormComponent() {
  const sequence = ++loadSequence

  if (!props.formUrl) {
    loading.value = false
    loadError.value = null
    formComponent.value = null
    formComponentKey.value = null
    return
  }

  const loader = resolveLoader(props.formUrl)

  if (!loader) {
    console.warn(`[FlowBusinessForm] 未找到对应组件：${props.formUrl}，请确认 formUrl 与组件路径一致`)
    loading.value = false
    formComponent.value = null
    formComponentKey.value = null
    loadError.value = null
    return
  }

  loading.value = true
  loadError.value = null
  formComponent.value = null
  formComponentKey.value = null

  try {
    const mod = await loader()
    if (disposed || sequence !== loadSequence) {
      return
    }

    const component = mod?.default || mod
    if (!isRenderableComponent(component)) {
      throw new TypeError('业务表单模块没有导出有效的 Vue 组件')
    }

    formComponent.value = markRaw(component)
    formComponentKey.value = `${normalizeFormUrl(props.formUrl)}:${sequence}`
  }
  catch (e) {
    if (disposed || sequence !== loadSequence) {
      return
    }

    console.error('[FlowBusinessForm] 加载业务表单失败:', e)
    formComponent.value = null
    formComponentKey.value = null
    loadError.value = `无法加载表单组件：${props.formUrl}`
  }
  finally {
    if (!disposed && sequence === loadSequence) {
      loading.value = false
    }
  }
}

function normalizeFormUrl(formUrl) {
  return String(formUrl || '').split('?')[0].trim().toLowerCase()
}

function isRenderableComponent(component) {
  if (typeof component === 'function') {
    return true
  }

  if (!component || typeof component !== 'object') {
    return false
  }

  return ['render', 'setup', 'template', '__name', '__file']
    .some(key => Object.prototype.hasOwnProperty.call(component, key))
}

function handleFormSubmit(data) {
  emit('submit', data)
}

// 监听 formUrl 变化重新加载
watch(() => props.formUrl, loadFormComponent, { immediate: true })

onBeforeUnmount(() => {
  disposed = true
  loadSequence += 1
})
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
