<template>
  <section class="plugin-gallery" :aria-busy="loading" aria-label="我的插件">
    <!-- 首次加载与真实卡片保持相同的信息分区，刷新保留现有清单。 -->
    <div v-if="loading && !records.length" class="plugin-gallery__grid">
      <div v-for="index in 6" :key="index" class="plugin-card--skeleton">
        <div class="plugin-card__body">
          <div class="plugin-card__content">
            <NSkeleton width="36px" height="36px" />
            <NSkeleton text width="55%" />
          </div>
          <NSkeleton text :repeat="2" />
        </div>
        <div class="plugin-card__footer">
          <NSkeleton text width="75%" />
        </div>
      </div>
    </div>
    <IllustratedEmpty
      v-else-if="!records.length" artwork="application"
      :description="filtered ? '没有找到匹配的插件，试试其他关键词' : '当前服务暂无已加载插件'"
    />
    <div v-else class="plugin-gallery__grid">
      <PluginCard
        v-for="row in records" :key="row.id" :plugin="row" :origins="dict.sys_plugin_origin"
        :can-detail="canDetail" @open="(id, tab) => emit('open', id, tab)"
      />
    </div>
  </section>
</template>

<script setup>
import { NSkeleton } from 'naive-ui'
import IllustratedEmpty from '@/components/common/IllustratedEmpty.vue'
import { useDict } from '@/composables/useDict'
import PluginCard from './PluginCard.vue'

defineProps({
  records: { type: Array, default: () => [] },
  loading: Boolean,
  canDetail: Boolean,
  filtered: Boolean,
})
const emit = defineEmits(['open'])
const { dict } = useDict('sys_plugin_origin')
</script>

<style scoped>
.plugin-gallery__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
  gap: 12px;
}
.plugin-card--skeleton {
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
}
.plugin-card__body {
  padding: 16px;
}
.plugin-card__content {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.plugin-card__footer {
  padding: 10px 16px;
  border-top: 1px solid var(--border-light);
}
</style>
