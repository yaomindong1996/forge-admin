<!--
  AI模态框组件

  功能特性：
  - 支持自定义宽度、标题、样式
  - 支持拖拽移动（可配置）
  - 支持自定义头部、内容、底部插槽
  - 支持确认/取消按钮的显示控制和样式配置
  - 支持异步操作（确认按钮loading状态）
  - 提供完整的生命周期钩子（onOk、onCancel、onClose）

  使用示例：
  <ai-modal ref="modalRef">
    <div>模态框内容</div>
  </ai-modal>

  // 打开模态框
  modalRef.value.open({
    title: '标题',
    width: '600px',
    onOk: async () => {
      // 处理确认逻辑
      await someAsyncOperation()
      return true // 返回false阻止关闭
    }
  })
-->
<template>
  <n-modal
    v-model:show="show"
    class="modal-box"
    :style="{ width: modalOptions.width, ...modalOptions.modalStyle }"
    :preset="undefined"
    size="huge"
    :bordered="false"
    :mask-closable="modalOptions.maskClosable"
    @after-leave="onAfterLeave"
    @update:show="handleUpdateShow"
  >
    <n-card
      :style="modalOptions.contentStyle"
      :closable="modalOptions.closable"
      :segmented="modalOptions.segmented"
      @close="close()"
    >
      <!-- 头部标题区域 -->
      <template #header>
        <header
          v-if="modalOptions.title || modalOptions.showHeader"
          class="modal-header"
          :class="{ 'cursor-move': modalOptions.draggable }"
        >
          {{ modalOptions.title }}
        </header>
      </template>

      <!-- 头部额外内容插槽 -->
      <template v-if="modalOptions.showHeaderExtra" #header-extra>
        <slot name="header-extra" />
      </template>

      <!-- 主体内容区域 -->
      <div class="modal-content">
        <slot />
      </div>

      <!-- 底部按钮区域 -->
      <template #footer>
        <slot name="footer">
          <footer v-if="modalOptions.showFooter" class="flex justify-end">
            <!-- 底部左侧插槽（用于放置额外的左侧按钮或内容） -->
            <slot name="footer-left">
              <div />
            </slot>

            <!-- 底部右侧按钮组 -->
            <div class="footer-buttons">
              <!-- 取消按钮 -->
              <n-button
                v-if="modalOptions.showCancel"
                :type="modalOptions.cancelType"
                :ghost="modalOptions.cancelGhost"
                @click="handleCancel()"
              >
                {{ modalOptions.cancelText }}
              </n-button>
              <!-- 确定按钮 -->
              <n-button
                v-if="modalOptions.showOk"
                :type="modalOptions.okType"
                :ghost="modalOptions.okGhost"
                :loading="modalOptions.okLoading"
                class="ml-20"
                @click="handleOk()"
              >
                {{ modalOptions.okText }}
              </n-button>
            </div>
          </footer>
        </slot>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { initDrag } from './utils.js'

/**
 * 组件Props定义
 *
 * 所有props都可以在调用open()方法时通过options参数动态覆盖
 */
const props = defineProps({
  /** 模态框宽度 */
  width: {
    type: String,
    default: '800px',
  },
  /** 模态框标题 */
  title: {
    type: String,
    default: '',
  },
  /** 是否显示关闭按钮 */
  closable: {
    type: Boolean,
    default: true,
  },
  /** 点击遮罩层是否可关闭 */
  maskClosable: {
    type: Boolean,
    default: false,
  },
  /** 取消按钮文本 */
  cancelText: {
    type: String,
    default: '取消',
  },
  /** 确定按钮文本 */
  okText: {
    type: String,
    default: '确定',
  },
  /** 是否显示底部区域 */
  showFooter: {
    type: Boolean,
    default: true,
  },
  /** 是否显示取消按钮 */
  showCancel: {
    type: Boolean,
    default: true,
  },
  /** 是否显示确定按钮 */
  showOk: {
    type: Boolean,
    default: true,
  },
  /** 取消按钮类型（default/primary/info/success/warning/error） */
  cancelType: {
    type: String,
    default: 'default',
  },
  /** 确定按钮类型（default/primary/info/success/warning/error） */
  okType: {
    type: String,
    default: 'primary',
  },
  /** 取消按钮是否为幽灵按钮 */
  cancelGhost: {
    type: Boolean,
    default: false,
  },
  /** 确定按钮是否为幽灵按钮 */
  okGhost: {
    type: Boolean,
    default: false,
  },
  /** 模态框自定义样式 */
  modalStyle: {
    type: Object,
    default: () => {},
  },
  /** 内容区域自定义样式 */
  contentStyle: {
    type: Object,
    default: () => {},
  },
  /** 卡片内容是否有分段（控制header/content/footer的分隔线） */
  segmented: {
    type: [Boolean, Object],
    default: false,
  },
  /** 是否启用拖拽功能 */
  draggable: {
    type: Boolean,
    default: true,
  },
  /** 是否显示头部 */
  showHeader: {
    type: Boolean,
    default: true,
  },
  /** 是否显示头部额外内容插槽 */
  showHeaderExtra: {
    type: Boolean,
    default: false,
  },
  /** 点击确定按钮的回调函数，返回false可阻止模态框关闭 */
  onOk: {
    type: Function,
    default: () => {},
  },
  /** 点击取消按钮的回调函数，返回false可阻止模态框关闭 */
  onCancel: {
    type: Function,
    default: () => {},
  },
  /** 模态框关闭时的回调函数 */
  onClose: {
    type: Function,
    default: () => {},
  },
})

/**
 * 响应式状态
 */

// 控制模态框的显示与隐藏
const show = ref(false)

// 存储模态框的配置信息（合并props和动态传入的options）
const modalOptions = ref({})

/**
 * 确定按钮loading状态的计算属性
 *
 * 支持外部通过暴露的okLoading来控制按钮的加载状态
 * 使用示例：modalRef.value.okLoading = true
 */
const okLoading = computed({
  get() {
    return !!modalOptions.value?.okLoading
  },
  set(v) {
    if (modalOptions.value) {
      modalOptions.value.okLoading = v
    }
  },
})

/**
 * 打开模态框
 *
 * @param {object} options - 模态框配置选项，会与props合并
 * @param {string} options.title - 标题
 * @param {string} options.width - 宽度
 * @param {Function} options.onOk - 确定回调
 * @param {Function} options.onCancel - 取消回调
 * @param {Function} options.onClose - 关闭回调
 * @param {boolean} options.draggable - 是否可拖拽
 * @param {object} options.modalStyle - 模态框样式
 * @param {object} options.contentStyle - 内容样式
 * ... 其他所有props都可以通过options传入
 *
 * @example
 * modalRef.value.open({
 *   title: '编辑用户',
 *   width: '600px',
 *   onOk: async () => {
 *     await saveUser()
 *     return true
 *   }
 * })
 */
async function open(options = {}) {
  // 将props和options合并赋值给modalOptions
  modalOptions.value = { ...props, ...options }

  // 将show的值设置为true，显示模态框
  show.value = true
  await nextTick()

  // 如果启用拖拽功能，则初始化拖拽
  // 使用Array.prototype.at获取最后一个元素，确保在多个模态框情况下操作正确的实例
  if (modalOptions.value.draggable) {
    initDrag(
      Array.prototype.at.call(document.querySelectorAll('.modal-header'), -1),
      Array.prototype.at.call(document.querySelectorAll('.modal-box'), -1),
    )
  }
}

/**
 * 关闭模态框
 *
 * 隐藏模态框并触发onClose回调
 */
function close() {
  show.value = false
  if (typeof modalOptions.value.onClose === 'function') {
    modalOptions.value.onClose()
  }
}

/**
 * 处理模态框确定操作
 *
 * @param {*} data - 传递给onOk回调的数据
 *
 * 流程：
 * 1. 如果没有配置onOk回调，直接关闭模态框
 * 2. 调用onOk回调，等待异步操作完成
 * 3. 如果回调返回值不为false，关闭模态框
 * 4. 如果发生错误，捕获错误并重置loading状态
 */
async function handleOk(data) {
  // 如果modalOptions中没有onOk函数，则直接关闭模态框
  if (typeof modalOptions.value.onOk !== 'function') {
    return close()
  }
  try {
    // 调用onOk函数，传入data参数
    const res = await modalOptions.value.onOk(data)
    // 如果onOk函数的返回值不为false，则关闭模态框
    if (res !== false)
      close()
  }
  catch (error) {
    console.error(error)
    // 发生错误时，重置loading状态，防止按钮一直处于加载中
    okLoading.value = false
  }
}

/**
 * 处理模态框取消操作
 *
 * @param {*} data - 传递给onCancel回调的数据
 *
 * 流程：
 * 1. 如果没有配置onCancel回调，直接关闭模态框
 * 2. 调用onCancel回调，等待异步操作完成
 * 3. 如果回调返回值不为false，关闭模态框
 * 4. 如果发生错误，捕获错误并重置loading状态
 */
async function handleCancel(data) {
  // 如果modalOptions中没有onCancel函数，则直接关闭模态框
  if (typeof modalOptions.value.onCancel !== 'function') {
    return close()
  }
  try {
    // 调用onCancel函数，传入data参数
    const res = await modalOptions.value.onCancel(data)

    // 如果onCancel函数的返回值不为false，则关闭模态框
    if (res !== false)
      close()
  }
  catch (error) {
    console.error(error)
    // 发生错误时，重置loading状态
    okLoading.value = false
  }
}

/**
 * 模态框离开后的回调
 *
 * 在模态框完全消失后触发
 * 如果启用拖拽功能，重新初始化拖拽（处理多个模态框的情况）
 */
async function onAfterLeave() {
  await nextTick()
  // 如果启用拖拽功能，则重新初始化拖拽（处理多个模态框的情况）
  if (modalOptions.value.draggable) {
    initDrag(
      Array.prototype.at.call(document.querySelectorAll('.modal-header'), -1),
      Array.prototype.at.call(document.querySelectorAll('.modal-box'), -1),
    )
  }
}

/**
 * 处理模态框显示状态更新
 *
 * @param {boolean} newShow - 新的显示状态
 *
 * 当模态框被外部因素关闭时（如点击遮罩层、ESC键等），触发onClose回调
 */
function handleUpdateShow(newShow) {
  if (!newShow) {
    if (typeof modalOptions.value.onClose === 'function') {
      modalOptions.value.onClose()
    }
  }
}

/**
 * 暴露给父组件的方法和属性
 *
 * - open: 打开模态框
 * - close: 关闭模态框
 * - handleOk: 手动触发确定操作
 * - handleCancel: 手动触发取消操作
 * - okLoading: 确定按钮loading状态（可读写）
 * - options: 当前模态框配置
 * - show: 模态框显示状态
 */
defineExpose({
  open,
  close,
  handleOk,
  handleCancel,
  okLoading,
  options: modalOptions,
  show,
})
</script>

<style scoped>
/* 模态框内容区域样式 */
.modal-content {
  padding: 20px 0;
}

/* 拖拽时的鼠标样式 */
.cursor-move {
  cursor: move;
}

/* 底部按钮组样式 */
.footer-buttons {
  display: flex;
  gap: 20px;
}
</style>
