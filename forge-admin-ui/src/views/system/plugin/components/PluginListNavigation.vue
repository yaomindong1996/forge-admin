<template>
  <section class="plugin-navigation" aria-label="切换插件">
    <!-- 固定筛选区 -->
    <PluginCenterToolbar compact />
    <!-- 独立滚动列表，错误不冒充空清单 -->
    <div class="plugin-navigation__list" :aria-busy="store.loading">
      <NAlert v-if="store.error" type="error" title="插件列表加载失败">
        {{ store.error }}
        <NButton text type="primary" @click="store.load">
          重试
        </NButton>
      </NAlert>
      <NSkeleton v-else-if="store.loading && !store.records.length" text :repeat="6" />
      <NEmpty v-else-if="!store.records.length" size="small" description="没有匹配的插件" />
      <template v-else>
        <button
          v-for="plugin in store.records" :key="plugin.id" type="button"
          class="plugin-navigation__item" :class="{ 'is-selected': store.selectedId === plugin.id }"
          :aria-current="store.selectedId === plugin.id ? 'true' : undefined"
          @click="store.open(plugin.id)"
        >
          <PluginIllustration :plugin="plugin" :size="46" />
          <span>
            <strong>{{ plugin.name || plugin.id }}</strong>
            <small>{{ pluginPresentation(plugin).summary }}</small>
          </span>
        </button>
      </template>
    </div>
    <footer v-if="store.total > store.query.pageSize" class="plugin-navigation__pagination">
      <NPagination
        simple :page="store.query.pageNum" :page-size="store.query.pageSize"
        :item-count="store.total" @update:page="store.changePage"
      />
    </footer>
  </section>
</template>

<script setup>
import { NAlert, NButton, NEmpty, NPagination, NSkeleton } from 'naive-ui'
import { usePluginCenterStore } from '@/stores/plugin/centerStore'
import { pluginPresentation } from '../pluginPresentation'
import PluginCenterToolbar from './PluginCenterToolbar.vue'
import PluginIllustration from './PluginIllustration.vue'

const store = usePluginCenterStore()
</script>

<style scoped>
.plugin-navigation {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.plugin-navigation__list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable;
  padding: 0 8px 8px;
}
.plugin-navigation__item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transition:
    color 150ms,
    background 150ms,
    border-color 150ms;
}
.plugin-navigation__item:hover {
  background: var(--bg-secondary);
}
.plugin-navigation__item.is-selected {
  border-color: color-mix(in srgb, var(--primary-color) 22%, transparent);
  background: color-mix(in srgb, var(--primary-color) 8%, var(--bg-primary));
  color: var(--primary-color);
}
.plugin-navigation__item:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: -2px;
}
.plugin-navigation__item > span {
  min-width: 0;
}
.plugin-navigation__item strong {
  display: block;
  font-size: 13px;
  font-weight: 500;
}
.plugin-navigation__item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 11px;
}
.plugin-navigation__pagination {
  padding: 8px 12px;
  border-top: 1px solid var(--border-light);
}
</style>
