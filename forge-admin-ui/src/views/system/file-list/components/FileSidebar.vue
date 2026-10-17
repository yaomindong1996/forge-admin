<template>
  <nav class="file-sidebar" aria-label="文件分类">
    <h2 class="sidebar-heading">
      文件管理
    </h2>
    <div class="sidebar-scroll">
      <button
        v-for="item in categories" :key="item.id" type="button" class="category"
        :class="{ active: store.selectedGroup === item.id }"
        :aria-current="store.selectedGroup === item.id ? 'page' : undefined"
        @click="store.selectGroup(item.id)"
      >
        <i :class="item.icon" aria-hidden="true" />
        <span class="category-name">{{ item.label }}</span>
        <span class="category-count">{{ item.count ?? '—' }}</span>
      </button>
      <div class="group-heading">
        <span>自定义分组</span>
        <NButton size="tiny" quaternary title="管理分组" aria-label="管理分组" @click="store.groupModalVisible = true">
          <template #icon>
            <i class="i-lucide:settings-2" />
          </template>
        </NButton>
      </div>
      <button
        v-for="group in store.groups" :key="group.id" type="button" class="category"
        :class="{ active: store.selectedGroup === group.id }"
        :aria-current="store.selectedGroup === group.id ? 'page' : undefined"
        :title="group.groupName" @click="store.selectGroup(group.id)"
      >
        <i class="i-lucide:folder" aria-hidden="true" />
        <span class="category-name">{{ group.groupName }}</span>
        <span class="category-count">{{ group.fileCount || 0 }}</span>
      </button>
      <div v-if="!store.groups.length" class="no-groups">
        <span>用分组整理文件</span>
        <NButton text type="primary" size="small" @click="store.groupModalVisible = true">
          新建分组
        </NButton>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { computed } from 'vue'
import { useFileListStore } from '@/stores/system/fileListStore'

const store = useFileListStore()
const categories = computed(() => [
  { id: 'all', label: '全部文件', icon: 'i-lucide:files', count: store.statistics.total },
  { id: 'images', label: '图片', icon: 'i-lucide:image', count: store.statistics.imageCount },
  { id: 'documents', label: '文档', icon: 'i-lucide:file-text', count: store.statistics.documentCount },
])
</script>

<style scoped>
.file-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.sidebar-heading {
  margin: 0;
  padding: 22px 20px 18px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.sidebar-scroll {
  flex: 1;
  min-height: 0;
  padding: 0 12px 16px;
  overflow: auto;
  scrollbar-gutter: stable;
}
.category {
  display: flex;
  text-decoration: none;
  align-items: center;
  gap: 10px;
  width: 100%;
  margin: 2px 0;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  text-align: left;
  font-size: 13px;
  cursor: pointer;
  transition:
    background 150ms,
    color 150ms,
    border-color 150ms;
}
.category:hover {
  background: var(--bg-secondary);
}
.category.active {
  background: color-mix(in srgb, var(--primary-color) 7%, transparent);
  border-color: color-mix(in srgb, var(--primary-color) 12%, transparent);
  color: var(--file-link-color, var(--primary-color));
}
.category:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 1px;
}
.category i {
  flex-shrink: 0;
  font-size: 17px;
}
.category-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.category-count {
  flex-shrink: 0;
  color: var(--text-tertiary);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}
.category.active .category-count {
  color: inherit;
}
.group-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 22px;
  padding: 0 10px 8px;
  color: var(--text-tertiary);
  font-size: 12px;
}
.no-groups {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  color: var(--text-tertiary);
  font-size: 12px;
}
@media (max-width: 960px) {
  .sidebar-heading {
    padding: 12px 20px 8px;
  }
  .group-heading {
    margin-top: 12px;
  }
}
</style>
