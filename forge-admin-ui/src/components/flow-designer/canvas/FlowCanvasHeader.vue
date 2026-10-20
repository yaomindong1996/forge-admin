<script setup>
import { NButton, NPopover } from 'naive-ui'
import { useFlowOverlayTarget } from '../composables/useFlowOverlayTarget.js'
import { getNodePresentation } from '../constants/node-presentation.js'

defineProps({ processName: { type: String, default: '' }, canUndo: Boolean, canRedo: Boolean, readonly: Boolean })
defineEmits(['undo', 'redo'])
const legend = ['start', 'approver', 'condition', 'carbonCopy'].map(getNodePresentation)
const overlayTarget = useFlowOverlayTarget()
</script>

<template>
  <div class="flow-canvas-header">
    <div class="flow-canvas-context">
      <div class="flow-canvas-breadcrumb" :title="processName">
        <span>流程中心</span><span>/</span><span>{{ processName || '流程设计' }}</span>
      </div>
      <div class="flow-canvas-legend" aria-label="节点类型图例">
        <span v-for="item in legend" :key="item.label">
          <i :style="{ background: item.color }" />{{ item.label }}
        </span>
      </div>
    </div>
    <!-- 撤销/重做与现有历史栈连接，帮助只解释真实已支持的操作 -->
    <div class="flow-canvas-actions">
      <NButton
        quaternary size="small" aria-label="撤销" title="撤销" :disabled="readonly || !canUndo"
        @click="$emit('undo')"
      >
        <template #icon>
          <i class="i-lucide:undo-2" />
        </template>
      </NButton>
      <NButton
        quaternary size="small" aria-label="重做" title="重做" :disabled="readonly || !canRedo"
        @click="$emit('redo')"
      >
        <template #icon>
          <i class="i-lucide:redo-2" />
        </template>
      </NButton>
      <span class="flow-canvas-actions__divider" />
      <NPopover trigger="click" placement="bottom-end" :show-arrow="false" :to="overlayTarget || undefined">
        <template #trigger>
          <NButton quaternary size="small">
            <template #icon>
              <i class="i-lucide:circle-help" />
            </template>使用说明
          </NButton>
        </template>
        <div class="flow-canvas-help">
          <strong>设计流程</strong>
          <p>点击连线上的「＋」添加节点；点击节点配置办理人、表单和操作权限。</p>
          <p>点击条件卡配置对应分支，点击网关可查看全部分支。</p>
          <p>按住空格拖拽，或直接拖动画布空白处移动；Ctrl / ⌘ + 滚轮缩放。</p>
          <p>节点确认后，需点击顶部「保存草稿」保存整个流程。</p>
        </div>
      </NPopover>
    </div>
  </div>
</template>

<style scoped>
.flow-canvas-header {
  position: absolute;
  top: 20px;
  right: 24px;
  left: 24px;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  pointer-events: none;
}
.flow-canvas-context {
  min-width: 0;
}
.flow-canvas-breadcrumb {
  display: flex;
  gap: 8px;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
}
.flow-canvas-breadcrumb span:last-child {
  overflow: hidden;
  max-width: 220px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-canvas-legend {
  display: flex;
  gap: 18px;
  margin-top: 14px;
  color: var(--text-secondary, #4e5969);
  font-size: 11px;
}
.flow-canvas-legend span {
  display: flex;
  align-items: center;
  gap: 6px;
}
.flow-canvas-legend i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
.flow-canvas-actions {
  display: flex;
  height: 30px;
  align-items: center;
  gap: 4px;
  pointer-events: auto;
}
.flow-canvas-actions__divider {
  height: 16px;
  margin: 0 6px;
  border-left: 1px solid var(--border-light, #e5e7eb);
}
.flow-canvas-help {
  max-width: min(280px, calc(100vw - 50px));
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
  line-height: 1.7;
}
.flow-canvas-help strong {
  color: var(--text-primary, #1d2129);
  font-weight: 500;
}
.flow-canvas-help p {
  margin: 8px 0 0;
}
@media (max-width: 680px) {
  .flow-canvas-header {
    top: 12px;
    right: 12px;
    left: 12px;
  }
  .flow-canvas-context {
    display: none;
  }
  .flow-canvas-actions {
    margin-left: auto;
  }
}
</style>
