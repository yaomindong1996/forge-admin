<script setup>
import { NDropdown } from 'naive-ui'
import WorkspaceIllustration from '@/components/common/WorkspaceIllustration.vue'

// 标签、分类和权限继续由页面的真实字典与流程状态提供。
defineProps({
  item: { type: Object, required: true },
  statusLabel: { type: String, default: '' },
  statusClass: { type: String, default: '' },
  designerLabel: { type: String, default: '' },
  designerClass: { type: String, default: '' },
  categoryLabel: { type: String, default: '' },
  bindingLabel: { type: String, default: '' },
  updateLabel: { type: String, default: '' },
  actionOptions: { type: Array, default: () => [] },
  sortMode: Boolean,
  deployDisabled: Boolean,
  busy: Boolean,
})
const emit = defineEmits(['design', 'deploy', 'instances', 'action', 'dragStart', 'drop'])
</script>

<template>
  <!-- 模型身份、业务关系与操作 -->
  <div
    class="model-card"
    :class="{ 'model-card-sortable': sortMode }"
    :draggable="sortMode"
    @dragstart="emit('dragStart', item)"
    @dragover.prevent
    @drop="emit('drop', item)"
  >
    <div class="card-header">
      <div class="card-title-block">
        <div class="card-title-row">
          <div class="card-title-icon-box">
            <WorkspaceIllustration artwork="workflow" size="card" />
          </div>
          <div class="card-title-main">
            <div class="card-title">
              {{ item.modelName }}
            </div>
            <div v-if="sortMode" class="card-sort-hint">
              拖动调整顺序
            </div>
            <div class="card-key">
              {{ item.modelKey }}
            </div>
          </div>
        </div>
      </div>
      <span class="status-tag" :class="statusClass">
        {{ statusLabel }}
      </span>
    </div>
    <div class="card-body">
      <div class="card-tags">
        <span class="designer-type-badge" :class="designerClass">
          {{ designerLabel }}
        </span>
        <span v-if="categoryLabel" class="category-badge">
          {{ categoryLabel }}
        </span>
      </div>
      <div class="card-binding" :class="{ empty: !item.businessBindings?.length }">
        <i class="i-lucide:link-2" />
        <span>{{ bindingLabel }}</span>
      </div>
      <div class="card-desc">
        {{ item.description || '暂无描述' }}
      </div>
    </div>
    <div class="card-footer">
      <div class="card-metadata">
        <div class="meta-item">
          <i class="i-lucide:calendar" />
          {{ updateLabel || '未更新' }}
        </div>
        <div class="meta-item">
          <i class="i-lucide:git-commit" />
          v{{ item.version || 1 }}
        </div>
      </div>
      <div class="card-actions">
        <button
          type="button"
          class="card-action-link"
          @click.stop="emit('design', item)"
        >
          编辑
        </button>
        <template v-if="item.status === 0 || item.status === 1">
          <span class="card-action-separator" />
          <button
            v-if="item.status === 0"
            type="button"
            class="card-action-link"
            :disabled="deployDisabled"
            @click.stop="emit('deploy', item)"
          >
            发布
          </button>
          <button
            v-else
            type="button"
            class="card-action-link"
            @click.stop="emit('instances', item)"
          >
            实例
          </button>
        </template>
        <span class="card-action-separator" />
        <NDropdown
          trigger="click"
          :options="actionOptions"
          :disabled="busy"
          @select="key => emit('action', key, item)"
        >
          <button type="button" class="card-more-action" aria-label="更多操作" @click.stop>
            <i class="i-lucide:more-horizontal" />
          </button>
        </NDropdown>
      </div>
    </div>
  </div>
</template>

<style scoped src="./flowModelCard.css"></style>
