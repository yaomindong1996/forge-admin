<template>
  <section class="template-create">
    <div class="template-create__toolbar">
      <n-input v-model:value="keyword" clearable placeholder="搜索模板名称或场景">
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
      <n-radio-group v-model:value="deliveryMode" size="small">
        <n-radio-button value="ONLINE">
          在线使用
        </n-radio-button>
        <n-radio-button value="SOURCE">
          生成源码
        </n-radio-button>
      </n-radio-group>
    </div>

    <div class="template-grid">
      <button
        v-for="template in templates"
        :key="template.key"
        type="button"
        class="template-card"
        :class="{ active: selectedKey === template.key }"
        :style="templateToneStyle(template)"
        @click="selectedKey = template.key"
      >
        <span class="template-card__icon" aria-hidden="true">
          <IconRenderer :icon="template.icon" :size="22" />
        </span>
        <span class="template-card__content">
          <span class="template-card__title">
            <strong>{{ template.name }}</strong>
            <span class="template-card__tag">{{ template.category }}</span>
          </span>
          <span class="template-card__description">{{ template.description }}</span>
          <small>{{ template.useCount }} 次启用 · {{ layoutLabel(template.templateCode) }}</small>
        </span>
        <n-icon v-if="selectedKey === template.key" class="template-card__check">
          <CheckmarkCircle />
        </n-icon>
      </button>
    </div>

    <n-empty v-if="!templates.length" description="没有匹配的应用模板" />
    <n-alert v-if="deliveryMode === 'SOURCE'" type="info" :bordered="false">
      系统会先生成可运行的在线应用，再打开统一代码预览与下载面板，确保源码与低代码协议一致。
    </n-alert>
  </section>
</template>

<script setup>
import { CheckmarkCircle, SearchOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import IconRenderer from '@/components/IconRenderer.vue'
import {
  APPLICATION_TEMPLATE_CATALOG,
  buildTemplateInitializePayload,
  filterApplicationTemplates,
  findApplicationTemplate,
  templateToneStyle,
} from './app-template-catalog'

const props = defineProps({
  templateKey: { type: String, default: '' },
  initialDeliveryMode: { type: String, default: 'ONLINE' },
})

const keyword = ref('')
const selectedKey = ref(props.templateKey || APPLICATION_TEMPLATE_CATALOG[0]?.key || '')
const deliveryMode = ref(props.initialDeliveryMode === 'SOURCE' ? 'SOURCE' : 'ONLINE')
const templates = computed(() => filterApplicationTemplates(keyword.value, 'official'))

watch(() => props.templateKey, (value) => {
  if (value)
    selectedKey.value = value
})

function layoutLabel(templateCode) {
  if (templateCode === 'TREE_TABLE')
    return '左树右表'
  if (templateCode === 'MASTER_DETAIL')
    return '主子表'
  return '单表 CRUD'
}

function validate() {
  if (!findApplicationTemplate(selectedKey.value))
    throw new Error('请选择应用模板')
  return true
}

function getPayload() {
  const template = findApplicationTemplate(selectedKey.value)
  return {
    template,
    initialization: buildTemplateInitializePayload(template),
    deliveryMode: deliveryMode.value,
  }
}

defineExpose({ getPayload, validate })
</script>

<style scoped>
.template-create {
  display: grid;
  gap: 14px;
}

.template-create__toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto;
  gap: 12px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.template-card {
  --template-ink: #356cde;
  --template-wash: #e0e7ff;
  position: relative;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--template-ink) 16%, var(--n-border-color));
  border-radius: 8px;
  background:
    linear-gradient(
      145deg,
      color-mix(in srgb, var(--template-wash) 55%, #fff) 0%,
      var(--n-color) 48%
    );
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background 160ms ease;
}

.template-card:hover {
  border-color: color-mix(in srgb, var(--template-ink) 42%, var(--n-border-color));
  box-shadow: 0 8px 18px color-mix(in srgb, var(--template-ink) 12%, transparent);
}

.template-card.active {
  border-color: var(--template-ink);
  background:
    linear-gradient(
      145deg,
      var(--template-wash) 0%,
      color-mix(in srgb, var(--template-wash) 35%, #fff) 100%
    );
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--template-ink) 28%, transparent);
}

.template-card__icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 10px;
  color: var(--template-ink);
  background: var(--template-wash);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--template-ink) 12%, transparent);
}

.template-card__content {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.template-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.template-card__title strong {
  color: #0f172a;
}

.template-card__tag {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  padding: 1px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--template-ink) 12%, #fff);
  color: var(--template-ink);
  font-size: 11px;
  font-weight: 650;
  line-height: 18px;
}

.template-card__description {
  color: var(--n-text-color-2);
  font-size: 13px;
  line-height: 1.5;
}

.template-card small {
  color: color-mix(in srgb, var(--template-ink) 38%, var(--n-text-color-3));
}

.template-card__check {
  position: absolute;
  top: 9px;
  right: 9px;
  color: var(--template-ink);
  font-size: 18px;
}

@media (max-width: 760px) {
  .template-create__toolbar,
  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>
