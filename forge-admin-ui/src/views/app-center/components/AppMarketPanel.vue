<template>
  <section class="app-market-panel">
    <div class="market-toolbar">
      <n-tabs v-model:value="activeGroup" type="segment" size="small">
        <n-tab name="official">
          官方模板
        </n-tab>
        <n-tab name="private">
          组织私有模板
        </n-tab>
        <n-tab name="recommended">
          推荐应用
        </n-tab>
      </n-tabs>
      <n-input v-model:value="keyword" clearable placeholder="搜索模板名称、分类或场景">
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
    </div>

    <div v-if="visibleTemplates.length" class="market-grid" role="list">
      <article
        v-for="(template, index) in visibleTemplates"
        :key="template.key"
        class="market-card"
        :style="{ '--market-card-order': index }"
        role="listitem"
      >
        <div class="market-card__visual" :style="templateAccent(template)">
          <span class="market-card__index">0{{ index + 1 }}</span>
          <span class="market-card__icon">
            <IconRenderer :icon="template.icon" :size="28" />
          </span>
          <span class="market-card__layout">{{ layoutLabel(template.templateCode) }}</span>
        </div>
        <div class="market-card__body">
          <header>
            <div>
              <strong>{{ template.name }}</strong>
              <span>{{ template.category }}</span>
            </div>
            <n-tag size="small" :bordered="false" type="success">
              官方
            </n-tag>
          </header>
          <p>{{ template.description }}</p>
          <div class="market-card__meta">
            <span><n-icon><FlashOutline /></n-icon>{{ template.useCount }} 次启用</span>
            <span>支持在线应用与源码交付</span>
          </div>
          <footer>
            <n-button secondary type="primary" @click="startTemplate(template, 'SOURCE')">
              生成源码
            </n-button>
            <n-button type="primary" @click="startTemplate(template, 'ONLINE')">
              立即启用
            </n-button>
          </footer>
        </div>
      </article>
    </div>

    <n-empty
      v-else
      class="market-empty"
      :description="emptyDescription"
    >
      <template v-if="activeGroup === 'private'" #extra>
        <span class="market-empty__hint">组织模板尚未接入持久化协议，本次不会展示虚构数据。</span>
      </template>
    </n-empty>
  </section>
</template>

<script setup>
import { FlashOutline, SearchOutline } from '@vicons/ionicons5'
import { computed, ref } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import {
  filterApplicationTemplates,
} from './create/app-template-catalog'

const emit = defineEmits(['createTemplate'])
const activeGroup = ref('official')
const keyword = ref('')

const visibleTemplates = computed(() => {
  if (activeGroup.value === 'private')
    return []
  const templates = filterApplicationTemplates(keyword.value, 'official')
  return activeGroup.value === 'recommended'
    ? [...templates].sort((left, right) => Number(right.useCount || 0) - Number(left.useCount || 0)).slice(0, 3)
    : templates
})
const emptyDescription = computed(() => {
  if (activeGroup.value === 'private')
    return '当前组织还没有发布私有应用模板'
  return '没有匹配的应用模板'
})

function startTemplate(template, deliveryMode) {
  emit('createTemplate', { deliveryMode, template })
}

function layoutLabel(templateCode) {
  if (templateCode === 'TREE_TABLE')
    return '左树右表'
  if (templateCode === 'MASTER_DETAIL')
    return '主从协作'
  return '单表应用'
}

function templateAccent(template) {
  const accents = {
    'customer-management': ['#0f766e', '#ccfbf1'],
    'inventory-ledger': ['#b45309', '#fef3c7'],
    'order-management': ['#1d4ed8', '#dbeafe'],
    'project-task': ['#be123c', '#ffe4e6'],
  }
  const [ink, wash] = accents[template.key] || ['#4338ca', '#e0e7ff']
  return { '--market-ink': ink, '--market-wash': wash }
}
</script>

<style scoped>
.app-market-panel {
  --market-border: var(--border-light);
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding: 22px clamp(18px, 3vw, 42px) 48px;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--bg-secondary) 54%, transparent) 1px, transparent 1px),
    var(--bg-primary);
  background-size: 32px 32px;
}

.market-toolbar {
  display: flex;
  flex-wrap: wrap;
  max-width: 1180px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin: 0 auto 16px;
}

.market-toolbar :deep(.n-tabs) {
  min-width: 0;
  flex: 1 1 auto;
  overflow-x: auto;
}

.market-toolbar :deep(.n-tabs-nav) {
  min-width: max-content;
}

.market-toolbar :deep(.n-input) {
  width: min(340px, 36vw);
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  max-width: 1180px;
  margin: 0 auto;
}

.market-card {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--market-border);
  border-radius: 9px;
  background: var(--bg-primary);
  box-shadow: 0 5px 18px rgb(15 23 42 / 5%);
  animation: market-card-enter 0.34s both;
  animation-delay: calc(var(--market-card-order) * 45ms);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.market-card:hover {
  box-shadow: 0 14px 30px rgb(15 23 42 / 11%);
  transform: translateY(-2px);
}

.market-card__visual {
  position: relative;
  display: grid;
  align-content: space-between;
  min-height: 230px;
  padding: 18px;
  overflow: hidden;
  color: var(--market-ink);
  background:
    linear-gradient(145deg, transparent 45%, rgb(255 255 255 / 72%) 45.5%, transparent 46%), var(--market-wash);
}

.market-card__visual::after {
  position: absolute;
  right: -38px;
  bottom: -42px;
  width: 120px;
  height: 120px;
  border: 18px solid currentcolor;
  border-radius: 50%;
  content: '';
  opacity: 0.09;
}

.market-card__index {
  font-family: Georgia, serif;
  font-size: 12px;
  font-style: italic;
}

.market-card__icon {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--market-ink) 20%, transparent);
  border-radius: 50%;
  background: color-mix(in srgb, var(--bg-primary) 64%, transparent);
}

.market-card__layout {
  width: max-content;
  padding-top: 8px;
  border-top: 2px solid currentcolor;
  font-size: 11px;
  font-weight: 700;
}

.market-card__body {
  display: grid;
  grid-template-rows: auto minmax(44px, 1fr) auto auto;
  gap: 12px;
  min-width: 0;
  padding: 18px;
}

.market-card__body header,
.market-card__body footer,
.market-card__meta,
.market-card__meta span {
  display: flex;
  align-items: center;
}

.market-card__body header {
  justify-content: space-between;
  gap: 12px;
}

.market-card__body header > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.market-card__body strong {
  color: var(--text-primary);
  font-size: 15px;
}

.market-card__body header span,
.market-card__meta {
  color: var(--text-tertiary);
  font-size: 10px;
}

.market-card__body p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.65;
}

.market-card__meta {
  flex-wrap: wrap;
  gap: 10px;
}

.market-card__meta span {
  gap: 4px;
}

.market-card__body footer {
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--market-border);
}

.market-empty {
  max-width: 1180px;
  min-height: 320px;
  justify-content: center;
  margin: 0 auto;
  border: 1px dashed var(--market-border);
  border-radius: 9px;
  background: var(--bg-primary);
}

.market-empty__hint {
  color: var(--text-tertiary);
  font-size: 11px;
}

@keyframes market-card-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
}

@media (max-width: 900px) {
  .market-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .app-market-panel {
    padding: 12px 10px 30px;
  }

  .market-toolbar {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .market-toolbar :deep(.n-tabs) {
    width: 100%;
  }

  .market-toolbar :deep(.n-input) {
    width: 100%;
  }

  .market-card {
    grid-template-columns: 106px minmax(0, 1fr);
  }

  .market-card__visual {
    min-height: 250px;
    padding: 14px;
  }

  .market-card__body {
    padding: 14px;
  }

  .market-card__body footer {
    align-items: stretch;
    flex-direction: column-reverse;
  }
}
</style>
