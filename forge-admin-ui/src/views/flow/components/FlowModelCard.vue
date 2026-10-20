<script setup>
import { NDropdown } from 'naive-ui'
import { computed } from 'vue'
import WorkspaceIllustration from '@/components/common/WorkspaceIllustration.vue'
import DictTag from '@/components/DictTag.vue'
import { getModelSecondaryAction, publishedVersionLabel } from '../utils/modelCatalog'

// 标签、分类和权限继续由页面的真实字典与流程状态提供。
const props = defineProps({
  item: { type: Object, required: true },
  statusLabel: { type: String, default: '' },
  statusClass: { type: String, default: '' },
  designerLabel: { type: String, default: '' },
  designerClass: { type: String, default: '' },
  categoryLabel: { type: String, default: '' },
  bindingLabel: { type: String, default: '' },
  updateLabel: { type: String, default: '' },
  actionOptions: { type: Array, default: () => [] },
  publicationOptions: { type: Array, default: () => [] },
  sortMode: Boolean,
  deployDisabled: Boolean,
  busy: Boolean,
})
const emit = defineEmits(['design', 'deploy', 'instances', 'action', 'dragStart', 'drop'])
const secondaryAction = computed(() => getModelSecondaryAction(props.item))
const hasPublicationLabel = computed(() => props.publicationOptions.some(option => option.value === props.item.publicationState))
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
            <div class="card-title" :title="item.modelName">
              {{ item.modelName }}
            </div>
            <div v-if="sortMode" class="card-sort-hint">
              拖动调整顺序
            </div>
            <div class="card-key" :title="item.modelKey">
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
      <div class="card-binding" :class="{ empty: !item.businessBindings?.length, failed: item.bindingLoadError }" :title="bindingLabel">
        <i class="i-lucide:link-2" />
        <span>{{ bindingLabel }}</span>
      </div>
      <div v-if="item.description" class="card-desc" :title="item.description">
        {{ item.description }}
      </div>
      <!-- 后端快照摘要与字典；未知状态不拿模型初始版本号来兜底。 -->
      <div class="card-publication" title="根据已保存的流程图和表单与发布快照比较，包含布局或格式变更；其他未版本化设置不参与判断">
        <span>{{ publishedVersionLabel(item) }}</span>
        <DictTag
          v-if="hasPublicationLabel" :options="publicationOptions" :value="item.publicationState"
          size="small" :bordered="false"
        />
        <span v-else class="card-publication-unavailable">发布信息暂不可用</span>
      </div>
    </div>
    <div class="card-footer">
      <div class="card-metadata">
        <div class="meta-item">
          <i class="i-lucide:calendar" />
          {{ updateLabel || '未更新' }}
        </div>
      </div>
      <div class="card-actions">
        <button
          type="button"
          class="card-action-link"
          :disabled="sortMode || busy"
          @click.stop="emit('design', item)"
        >
          设计流程
        </button>
        <template v-if="secondaryAction">
          <span class="card-action-separator" />
          <button
            type="button"
            class="card-action-link"
            :disabled="sortMode || busy || (secondaryAction.key === 'deploy' && deployDisabled)"
            @click.stop="emit(secondaryAction.key, item)"
          >
            {{ secondaryAction.label }}
          </button>
        </template>
        <span class="card-action-separator" />
        <NDropdown
          trigger="click"
          :options="actionOptions"
          :disabled="busy || sortMode"
          @select="key => emit('action', key, item)"
        >
          <button type="button" class="card-more-action" :disabled="busy || sortMode" aria-label="更多操作" @click.stop>
            <i class="i-lucide:more-horizontal" />
          </button>
        </NDropdown>
      </div>
    </div>
  </div>
</template>

<style scoped src="./flowModelCard.css"></style>
