<script setup>
/**
 * BranchNode — 网关节点（condition / parallel / inclusive 共享样式）
 *
 * 网关是分支锚点，不是审批办理节点。画布布局仍给它保留标准节点尺寸用于连线，
 * 视觉上只在中心渲染轻量锚点，避免被误认为普通任务卡。
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  outgoingCount: { type: Number, default: 0 },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
})

const emit = defineEmits(['click', 'delete', 'contextMenu'])

const META = {
  condition: { icon: 'i-lucide:git-branch', color: 'warning', label: '条件分支' },
  parallel: { icon: 'i-lucide:split', color: 'success', label: '并行分支' },
  inclusive: { icon: 'i-lucide:git-merge', color: 'success', label: '包容分支' },
}

const meta = computed(() => META[props.node?.nodeType] || META.condition)

const subtitle = computed(() => {
  const t = props.node?.nodeType
  const n = props.outgoingCount || 0
  if (t === 'condition')
    return n > 0 ? `${n} 条条件分支` : '点击配置分支条件'
  if (t === 'parallel')
    return n > 0 ? `${n} 条并行分支（同时执行）` : '点击配置并行分支'
  if (t === 'inclusive')
    return n > 0 ? `${n} 条包容分支` : '点击配置包容分支'
  return ''
})

const canDelete = computed(() => !props.readonly)

function handleClick() {
  emit('click', props.node)
}

function handleDelete(event) {
  event.stopPropagation()
  if (canDelete.value)
    emit('delete', props.node)
}

function handleContextMenu(event) {
  event.preventDefault()
  emit('contextMenu', { event, node: props.node })
}
</script>

<template>
  <div
    class="flow-node-card branch-node-shell"
    :class="[
      selected ? 'is-selected' : '',
      readonly ? 'is-readonly' : '',
      `is-${meta.color}`,
    ]"
    :data-node-id="node?.id"
    :data-node-type="node?.nodeType"
    :title="`${node?.name || meta.label}：${subtitle}`"
    @click="handleClick"
    @contextmenu="handleContextMenu"
  >
    <div class="branch-node-diamond">
      <span class="branch-node-icon">
        <i :class="meta.icon" class="text-base" />
      </span>
      <span class="branch-node-assistive">
        {{ node?.name || meta.label }} {{ meta.label }} {{ subtitle }}
      </span>
      <button
        v-if="canDelete"
        class="branch-node-delete"
        aria-label="删除网关"
        @click.stop="handleDelete"
      >
        <i class="i-lucide:x text-xs" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.branch-node-shell {
  position: relative;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  box-shadow: none;
  cursor: pointer;
}

.branch-node-diamond {
  position: relative;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #f59e0b;
  border-radius: 7px;
  background: #fff7ed;
  box-shadow:
    0 0 0 1px rgba(245, 158, 11, 0.14),
    0 2px 6px rgba(15, 23, 42, 0.08);
  transform: rotate(45deg);
  transition:
    box-shadow 160ms ease,
    transform 160ms ease;
}

.branch-node-shell.is-info .branch-node-diamond {
  border-color: #4f46e5;
  background: #eef2ff;
  box-shadow:
    0 0 0 1px rgba(79, 70, 229, 0.14),
    0 2px 6px rgba(15, 23, 42, 0.08);
}

.branch-node-shell.is-success .branch-node-diamond {
  border-color: #10b981;
  background: #ecfdf5;
  box-shadow:
    0 0 0 1px rgba(16, 185, 129, 0.14),
    0 2px 6px rgba(15, 23, 42, 0.08);
}

.branch-node-shell:not(.is-readonly):hover .branch-node-diamond {
  box-shadow:
    0 0 0 3px rgba(245, 158, 11, 0.12),
    0 4px 10px rgba(15, 23, 42, 0.12);
  transform: rotate(45deg) scale(1.04);
}

.branch-node-shell.is-selected .branch-node-diamond {
  box-shadow:
    0 0 0 3px rgba(37, 99, 235, 0.18),
    0 4px 10px rgba(15, 23, 42, 0.12);
}

.branch-node-icon {
  display: inline-flex;
  color: #d97706;
  line-height: 1;
  transform: rotate(-45deg);
}

.branch-node-shell.is-info .branch-node-icon {
  color: #4f46e5;
}

.branch-node-shell.is-success .branch-node-icon {
  color: #059669;
}

.branch-node-delete {
  position: absolute;
  right: -13px;
  top: -13px;
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-tertiary);
  opacity: 0;
  transform: rotate(-45deg);
  transition:
    background 160ms ease,
    color 160ms ease;
}

.branch-node-shell:hover .branch-node-delete,
.branch-node-shell.is-selected .branch-node-delete {
  opacity: 1;
}

.branch-node-delete:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.branch-node-shell.is-readonly {
  cursor: default;
}

.branch-node-shell.is-readonly .branch-node-delete {
  display: none;
}

.branch-node-assistive {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

@media (prefers-reduced-motion: reduce) {
  .branch-node-diamond,
  .branch-node-delete {
    transition: none;
  }

  .branch-node-shell:not(.is-readonly):hover .branch-node-diamond {
    transform: rotate(45deg);
  }
}
</style>
