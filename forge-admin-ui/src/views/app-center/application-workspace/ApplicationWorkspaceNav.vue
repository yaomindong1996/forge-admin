<template>
  <nav class="workspace-nav" aria-label="应用工作台分区">
    <button
      v-for="item in sections"
      :key="item.sectionKey"
      type="button"
      class="nav-item"
      :class="{ active: item.sectionKey === activeSection }"
      @click="emit('select', item.sectionKey)"
    >
      <n-icon><component :is="iconMap[item.sectionKey] || GridOutline" /></n-icon>
      <span class="nav-copy">
        <strong>{{ sectionLabel[item.sectionKey] || item.sectionName }}</strong>
        <small>{{ sectionDescription[item.sectionKey] }}</small>
      </span>
      <span
        v-if="Number(item.problemCount || 0) > 0"
        class="nav-problem"
        :title="`${item.problemCount} 项待处理问题`"
        :aria-label="`${item.problemCount} 项待处理问题`"
      >
        !
      </span>
      <span v-else-if="Number(item.assetCount || 0) > 0" class="nav-count">
        {{ item.assetCount }}
      </span>
    </button>
  </nav>
</template>

<script setup>
import {
  CheckmarkDoneOutline,
  CodeSlashOutline,
  DocumentsOutline,
  GitNetworkOutline,
  GridOutline,
  KeyOutline,
  LayersOutline,
} from '@vicons/ionicons5'

defineProps({
  sections: {
    type: Array,
    default: () => [],
  },
  activeSection: {
    type: String,
    default: 'overview',
  },
})

const emit = defineEmits(['select'])

const iconMap = {
  overview: GridOutline,
  objects: LayersOutline,
  entries: DocumentsOutline,
  automation: GitNetworkOutline,
  enhancements: CodeSlashOutline,
  permissions: KeyOutline,
  releases: CheckmarkDoneOutline,
}

const sectionDescription = {
  overview: '状态与待办',
  objects: '字段、表单、列表和规则',
  entries: '菜单、访问方式与权限',
  automation: '流程与触发器',
  enhancements: '动作与扩展',
  permissions: '访问与数据权限',
  releases: '检查、版本、历史',
}

const sectionLabel = {
  objects: '业务对象',
  entries: '页面入口',
}
</script>

<style scoped>
.workspace-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 8px;
}

.nav-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  gap: 9px;
  align-items: center;
  width: 100%;
  min-height: 52px;
  padding: 7px 9px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  color: var(--text-secondary, #4e5969);
  background: transparent;
  text-align: left;
}

.nav-item:hover {
  border-color: var(--border-default, #c9cdd4);
  background: var(--bg-hover, #f2f3f5);
}

.nav-item.active {
  border-color: color-mix(in srgb, var(--primary-color, #165dff) 28%, var(--border-default, #c9cdd4));
  color: var(--primary-color, #165dff);
  background: color-mix(in srgb, var(--primary-color, #165dff) 7%, var(--bg-primary, #fff));
}

.nav-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 1px;
}

.nav-copy strong {
  color: inherit;
  font-size: 13px;
  font-weight: 600;
}

.nav-copy small {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-count,
.nav-problem {
  min-width: 20px;
  padding: 1px 5px;
  border-radius: 10px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-tertiary, #f2f3f5);
  font-size: 11px;
  text-align: center;
}

.nav-problem {
  color: #9a6700;
  background: #fff8c5;
}
</style>
