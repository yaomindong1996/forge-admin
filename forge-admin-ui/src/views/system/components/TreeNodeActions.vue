<template>
  <div class="tree-node-actions" @mouseenter="showActions = true" @mouseleave="showActions = false">
    <div class="node-content">
      <i v-if="node.icon" :class="node.icon" class="node-icon" />
      <span class="node-title">{{ node.label || node.title || node.name }}</span>
    </div>

    <div v-if="showActions" class="action-buttons">
      <!-- 新增子项按钮 -->
      <n-tooltip v-if="canAddChild">
        <template #trigger>
          <n-button
            size="tiny"
            circle
            quaternary
            type="primary"
            @click.stop="handleAction('add')"
          >
            <template #icon>
              <n-icon><AddCircleOutline /></n-icon>
            </template>
          </n-button>
        </template>
        {{ addChildText }}
      </n-tooltip>

      <!-- 编辑按钮 -->
      <!--      <n-tooltip v-if="canEdit"> -->
      <!--        <template #trigger> -->
      <!--          <n-button -->
      <!--            size="tiny" -->
      <!--            circle -->
      <!--            quaternary -->
      <!--            @click.stop="handleAction('edit')" -->
      <!--          > -->
      <!--            <template #icon> -->
      <!--              <n-icon><CreateOutline /></n-icon> -->
      <!--            </template> -->
      <!--          </n-button> -->
      <!--        </template> -->
      <!--        编辑{{ nodeTypeText }} -->
      <!--      </n-tooltip> -->

      <!-- 删除按钮 -->
      <n-tooltip v-if="canDelete">
        <template #trigger>
          <n-button
            size="tiny"
            circle
            quaternary
            type="error"
            @click.stop="handleAction('delete')"
          >
            <template #icon>
              <n-icon><TrashOutline /></n-icon>
            </template>
          </n-button>
        </template>
        删除{{ nodeTypeText }}
      </n-tooltip>

      <!-- 刷新按钮 -->
      <n-tooltip>
        <template #trigger>
          <n-button
            size="tiny"
            circle
            quaternary
            @click.stop="handleAction('refresh')"
          >
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
          </n-button>
        </template>
        刷新
      </n-tooltip>
    </div>
  </div>
</template>

<script setup>
import { AddCircleOutline, RefreshOutline, TrashOutline } from '@vicons/ionicons5'
import { computed, ref } from 'vue'

const props = defineProps({
  node: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['action'])

const showActions = ref(false)

// 节点类型文本
const nodeTypeText = computed(() => {
  switch (props.node.type) {
    case 'root':
    case 'sys':
      return '子系统'
    case 'subapp':
      return '子系统'
    case 'module':
      return '模块'
    default:
      return '节点'
  }
})

// 新增子项的文本
const addChildText = computed(() => {
  switch (props.node.type) {
    case 'root':
    case 'sys':
      return '新增子系统'
    case 'subapp':
      return '新增模块'
    case 'module':
      return '新增菜单'
    default:
      return '新增'
  }
})

// 是否可以新增子项
const canAddChild = computed(() => {
  // root 和 sys 可以新增子系统
  // subapp 可以新增模块
  // module 可以新增菜单
  return ['root', 'sys', 'subapp', 'module'].includes(props.node.type)
})

// 是否可以编辑（根节点不能编辑）
const canEdit = computed(() => {
  return props.node.type !== 'root' && props.node.type !== 'sys'
})

// 是否可以删除（根节点不能删除）
const canDelete = computed(() => {
  return props.node.type !== 'root' && props.node.type !== 'sys'
})

// 处理操作
function handleAction(action) {
  emit('action', {
    action,
    node: props.node,
  })
}
</script>

<style scoped>
.tree-node-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 2px 0;
}

.node-content {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.node-icon {
  margin-right: 6px;
  font-size: 16px;
}

.node-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}
</style>
