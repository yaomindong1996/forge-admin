<template>
  <section class="app-market-panel">
    <!-- 头图入口 -->
    <header class="market-hero">
      <div class="market-hero__copy">
        <div class="market-hero__title">
          <span class="market-hero__mark" aria-hidden="true">
            <i class="i-lucide:layout-grid" />
          </span>
          <h2>应用市场</h2>
        </div>
        <p>
          丰富的应用模板开箱即用，一键安装快速启动您的业务系统。
          也支持导入应用，扩展更多可能性。
        </p>
        <div class="market-hero__actions">
          <n-button
            v-if="officialHref"
            type="primary"
            tag="a"
            :href="officialHref"
            target="_blank"
            rel="noopener noreferrer"
          >
            <template #icon>
              <i class="i-lucide:rocket" />
            </template>
            打开应用市场
          </n-button>
          <n-button secondary type="primary" @click="emit('importPackage')">
            <template #icon>
              <i class="i-lucide:upload" />
            </template>
            导入应用
          </n-button>
        </div>
      </div>
      <div class="market-hero__art" aria-hidden="true">
        <span class="hero-block hero-block--lg">
          <i class="i-lucide:star" />
        </span>
        <span class="hero-block hero-block--md">
          <i class="i-lucide:download" />
        </span>
        <span class="hero-block hero-block--sm">
          <i class="i-lucide:code-2" />
        </span>
        <span class="hero-orb" />
      </div>
    </header>

    <!-- 分组 Tab -->
    <n-tabs v-model:value="activeGroup" type="line" size="medium" class="market-tabs">
      <n-tab name="official">
        <span class="market-tab-label"><i class="i-lucide:shield-check" />官方模板</span>
      </n-tab>
      <n-tab name="private">
        <span class="market-tab-label"><i class="i-lucide:lock" />组织私有模板</span>
      </n-tab>
      <n-tab name="recommended">
        <span class="market-tab-label"><i class="i-lucide:thumbs-up" />推荐应用</span>
      </n-tab>
    </n-tabs>

    <!-- 搜索 / 分类 -->
    <div class="market-toolbar">
      <n-input v-model:value="keyword" clearable placeholder="搜索模板名称、分类或场景">
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
      <n-select
        v-model:value="category"
        :options="categoryOptions"
        clearable
        placeholder="全部分类"
      >
        <template #arrow>
          <i class="i-lucide:sliders-horizontal" />
        </template>
      </n-select>
    </div>

    <!-- 模板卡片 -->
    <div v-if="visibleTemplates.length" class="market-list" role="list">
      <article
        v-for="template in visibleTemplates"
        :key="template.key"
        class="market-item"
        :style="templateTone(template)"
        role="listitem"
      >
        <div class="market-item__header">
          <div class="market-item__identity">
            <span class="market-item__icon" aria-hidden="true">
              <IconRenderer :icon="template.icon" :size="20" />
            </span>
            <div class="market-item__title">
              <strong>{{ template.name }}</strong>
              <code>{{ template.key }}</code>
            </div>
          </div>
          <span class="market-item__badge">官方</span>
        </div>
        <div class="market-item__body">
          <div class="market-item__tags">
            <span>{{ template.category }}</span>
            <span>{{ layoutLabel(template.templateCode) }}</span>
          </div>
          <p>{{ template.description || '暂无描述' }}</p>
        </div>
        <div class="market-item__footer">
          <!-- 左侧只留启用次数；「在线/源码」与右侧两个操作重复，去掉避免窄卡裁切 -->
          <div class="market-item__meta">
            <span><i class="i-lucide:activity" />{{ template.useCount }} 次启用</span>
          </div>
          <div class="market-item__actions">
            <button type="button" class="market-item__action" @click="startTemplate(template, 'SOURCE')">
              <i class="i-lucide:code-xml" />生成源码
            </button>
            <span class="market-item__sep" />
            <button type="button" class="market-item__action is-primary" @click="startTemplate(template, 'ONLINE')">
              <i class="i-lucide:play" />立即启用
            </button>
          </div>
        </div>
      </article>
    </div>

    <n-empty
      v-else
      class="market-empty"
      :description="emptyDescription"
    >
      <template v-if="officialSearchHref" #extra>
        <n-button type="primary" tag="a" :href="officialSearchHref" target="_blank" rel="noopener noreferrer">
          <template #icon>
            <i class="i-lucide:external-link" />
          </template>
          到官网应用市场搜索「{{ keyword }}」
        </n-button>
      </template>
      <template v-else-if="activeGroup === 'private'" #extra>
        <span class="market-empty__hint">组织模板尚未接入持久化协议，本次不会展示虚构数据。</span>
      </template>
    </n-empty>
  </section>
</template>

<script setup>
import { SearchOutline } from '@vicons/ionicons5'
import { computed, ref } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { officialMarketUrl } from '@/utils/officialMarket'
import {
  APPLICATION_TEMPLATE_CATALOG,
  filterApplicationTemplates,
  resolveApplicationTemplateTone,
} from './create/app-template-catalog'

const emit = defineEmits(['createTemplate', 'importPackage'])
const activeGroup = ref('official')
const keyword = ref('')
const category = ref(null)

const officialHref = computed(() => officialMarketUrl('app'))
const categoryOptions = computed(() => {
  const labels = [...new Set(APPLICATION_TEMPLATE_CATALOG.map(item => item.category).filter(Boolean))]
  return [{ label: '全部分类', value: null }, ...labels.map(label => ({ label, value: label }))]
})

const visibleTemplates = computed(() => {
  if (activeGroup.value === 'private')
    return []
  let templates = filterApplicationTemplates(keyword.value, 'official')
  if (category.value)
    templates = templates.filter(item => item.category === category.value)
  return activeGroup.value === 'recommended'
    ? [...templates].sort((left, right) => Number(right.useCount || 0) - Number(left.useCount || 0)).slice(0, 3)
    : templates
})
const emptyDescription = computed(() => {
  if (activeGroup.value === 'private')
    return '当前组织还没有发布私有应用模板'
  if (keyword.value.trim() || category.value)
    return '本地没有匹配的应用模板'
  return '没有匹配的应用模板'
})
const officialSearchHref = computed(() => {
  const query = keyword.value.trim()
  if (!query || activeGroup.value === 'private')
    return ''
  return officialMarketUrl('app', { q: query })
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

/** 按模板稳定映射色相，与创建向导模板卡共用 catalog.tone。 */
function templateTone(template) {
  const tone = resolveApplicationTemplateTone(template)
  return { '--market-ink': tone.ink, '--market-wash': tone.wash }
}
</script>

<style scoped>
.app-market-panel {
  --market-border: #e5e7eb;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding: 18px clamp(14px, 2.4vw, 32px) 36px;
  background: linear-gradient(180deg, #eef4ff 0%, #f6f8fb 160px, #f6f8fb 100%);
}

.market-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(180px, 0.8fr);
  gap: 18px;
  width: 100%;
  margin: 0 0 8px;
  padding: 22px 24px;
  overflow: hidden;
  border: 1px solid #dbe5f5;
  border-radius: 8px;
  background:
    radial-gradient(circle at 88% 18%, rgb(96 165 250 / 28%), transparent 36%),
    linear-gradient(135deg, #ffffff 0%, #f0f6ff 55%, #e8f0ff 100%);
  box-shadow: 0 10px 28px rgb(37 99 235 / 8%);
}

.market-hero__title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.market-hero__mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  background: linear-gradient(145deg, #60a5fa, #2563eb);
  color: #fff;
  font-size: 18px;
  box-shadow: 0 8px 16px rgb(37 99 235 / 28%);
}

.market-hero__title h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.market-hero__copy p {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.market-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.market-hero__art {
  position: relative;
  min-height: 140px;
}

.hero-block {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #fff;
  box-shadow: 0 14px 28px rgb(37 99 235 / 22%);
}

.hero-block--lg {
  right: 42px;
  bottom: 28px;
  width: 86px;
  height: 86px;
  font-size: 30px;
  background: linear-gradient(145deg, #93c5fd, #2563eb);
  transform: rotate(-8deg);
}

.hero-block--md {
  right: 118px;
  top: 18px;
  width: 58px;
  height: 58px;
  font-size: 22px;
  background: linear-gradient(145deg, #67e8f9, #0ea5e9);
  transform: rotate(12deg);
}

.hero-block--sm {
  right: 18px;
  top: 34px;
  width: 46px;
  height: 46px;
  font-size: 18px;
  background: linear-gradient(145deg, #c4b5fd, #7c3aed);
  transform: rotate(18deg);
}

.hero-orb {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgb(147 197 253 / 45%), transparent 70%);
}

.market-tabs {
  width: 100%;
  margin: 0 0 12px;
}

.market-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.market-toolbar {
  display: flex;
  flex-wrap: wrap;
  width: 100%;
  align-items: center;
  gap: 10px;
  margin: 0 0 14px;
}

.market-toolbar :deep(.n-input) {
  flex: 1 1 240px;
  min-width: 0;
}

.market-toolbar :deep(.n-select) {
  flex: 0 1 180px;
  min-width: 140px;
}

.market-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 280px), 1fr));
  gap: 14px;
  width: 100%;
}

.market-item {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--market-border);
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 4px 14px rgb(15 23 42 / 4%);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease;
}

.market-item:hover {
  box-shadow: 0 12px 28px rgb(15 23 42 / 10%);
  transform: translateY(-2px);
}

.market-item__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 16px 10px;
}

.market-item__identity {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.market-item__icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 44px;
  height: 44px;
  border-radius: 4px;
  background: linear-gradient(145deg, color-mix(in srgb, var(--market-wash) 40%, #fff), var(--market-wash));
  color: var(--market-ink);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--market-ink) 12%, transparent);
}

.market-item__title {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.market-item__title strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.market-item__title code {
  overflow: hidden;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.market-item__badge {
  flex: none;
  height: 20px;
  padding: 0 7px;
  border-radius: 4px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 600;
  line-height: 20px;
}

.market-item__body {
  display: grid;
  gap: 8px;
  padding: 0 16px 12px;
}

.market-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.market-item__tags span {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--market-wash) 70%, #fff);
  color: var(--market-ink);
  font-size: 11px;
  font-weight: 500;
}

.market-item__body p {
  margin: 0;
  display: -webkit-box;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.market-item__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: nowrap;
  margin-top: auto;
  min-height: 36px;
  padding: 8px 14px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

.market-item__meta {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px;
  min-width: 0;
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
}

.market-item__meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: none;
  white-space: nowrap;
}

.market-item__actions {
  display: flex;
  align-items: center;
  flex: none;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.market-item__action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.market-item__action:hover {
  color: #1d4ed8;
}

.market-item__action.is-primary {
  font-weight: 650;
}

.market-item__sep {
  width: 1px;
  height: 12px;
  margin: 0 6px;
  background: #e2e8f0;
}

.market-empty {
  width: 100%;
  min-height: 280px;
  justify-content: center;
  border: 1px dashed #dbe5f5;
  border-radius: 4px;
  background: #fff;
}

.market-empty__hint {
  color: #94a3b8;
  font-size: 11px;
}

@media (max-width: 860px) {
  .market-hero {
    grid-template-columns: 1fr;
  }

  .market-hero__art {
    display: none;
  }
}

@media (max-width: 680px) {
  .app-market-panel {
    padding: 12px 10px 24px;
  }

  .market-hero {
    padding: 16px;
    border-radius: 4px;
  }

  .market-toolbar :deep(.n-select) {
    flex: 1 1 100%;
  }

  .market-item__footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
