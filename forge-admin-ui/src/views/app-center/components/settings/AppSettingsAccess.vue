<template>
  <section class="settings-section-card">
    <header>
      <h2>访问地址</h2>
      <p>门户地址在当前租户内唯一，修改后需重新发布才会影响正式门户。</p>
    </header>

    <n-form class="access-form" label-placement="top">
      <div class="access-block">
        <n-form-item label="门户 slug" :validation-status="slugStatus" :feedback="slugFeedback">
          <n-input-group class="portal-slug-field">
            <n-input-group-label>/app/</n-input-group-label>
            <n-input
              :value="modelValue.portalSlug"
              maxlength="50"
              placeholder="例如 crm-portal"
              @update:value="handleSlugInput"
              @blur="checkSlug"
            />
          </n-input-group>
        </n-form-item>
      </div>

      <div class="access-link-grid">
        <article class="access-link-card">
          <div class="access-link-card__qr" aria-hidden="true">
            <n-qr-code :value="pcUrl" :size="96" :color="modelValue.themeColor || '#3370ff'" />
          </div>
          <div class="access-link-card__body">
            <span class="access-link-card__label">电脑端</span>
            <code class="access-link-card__url" :title="pcUrl">{{ pcUrl }}</code>
            <p class="access-link-card__desc">在电脑浏览器打开完整应用门户。</p>
            <div class="access-link-card__actions">
              <n-button secondary size="small" @click="copyLink(pcUrl, '电脑端')">
                复制链接
              </n-button>
              <n-button type="primary" size="small" :disabled="!modelValue.portalSlug" @click="openLink(pcUrl)">
                打开电脑端
              </n-button>
            </div>
          </div>
        </article>

        <article class="access-link-card">
          <div class="access-link-card__qr" aria-hidden="true">
            <n-qr-code :value="h5Url" :size="96" :color="modelValue.themeColor || '#3370ff'" />
          </div>
          <div class="access-link-card__body">
            <span class="access-link-card__label">移动端</span>
            <code class="access-link-card__url" :title="h5Url">{{ h5Url }}</code>
            <p class="access-link-card__desc">扫码打开独立 H5 运行页，和旧访问入口同一套地址。</p>
            <div class="access-link-card__actions">
              <n-button secondary size="small" :disabled="!h5Url" @click="copyLink(h5Url, '移动端')">
                复制移动端链接
              </n-button>
              <n-button type="primary" size="small" :disabled="!h5Url" @click="openLink(h5Url)">
                打开移动端
              </n-button>
            </div>
          </div>
        </article>
      </div>
    </n-form>
  </section>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref } from 'vue'
import { checkBusinessApplicationSlugAvailable } from '@/api/business-application'
import { buildPortalAccessUrls, RESERVED_PORTAL_SLUGS } from '../portal/portal-config'

const props = defineProps({
  modelValue: { type: Object, required: true },
  application: { type: Object, default: null },
  configKey: { type: String, default: '' },
  objects: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue'])
const message = useMessage()
const slugStatus = ref(undefined)
const slugFeedback = ref('只允许 2-50 位字母、数字、中划线或下划线。')
let validationSequence = 0

const accessUrls = computed(() => buildPortalAccessUrls({
  origin: window.location.origin,
  basePath: import.meta.env.BASE_URL,
  slug: props.modelValue.portalSlug || props.modelValue.applicationCode,
  configKey: props.configKey,
  h5BaseUrl: props.modelValue.distribution?.h5BaseUrl,
  appId: props.modelValue.id,
  application: props.application || props.modelValue,
  objects: props.objects,
}))
const pcUrl = computed(() => accessUrls.value.pcUrl)
const h5Url = computed(() => accessUrls.value.h5Url)

function handleSlugInput(value) {
  slugStatus.value = undefined
  slugFeedback.value = '只允许 2-50 位字母、数字、中划线或下划线。'
  emit('update:modelValue', { ...props.modelValue, portalSlug: String(value || '').trim() })
}

async function checkSlug() {
  const slug = String(props.modelValue.portalSlug || '').trim()
  if (!/^[\w-]{2,50}$/.test(slug)) {
    slugStatus.value = 'error'
    slugFeedback.value = '门户地址格式不正确。'
    return false
  }
  if (RESERVED_PORTAL_SLUGS.includes(slug.toLowerCase())) {
    slugStatus.value = 'error'
    slugFeedback.value = '该地址是系统保留路径，请更换其它值。'
    return false
  }
  const sequence = ++validationSequence
  try {
    const response = await checkBusinessApplicationSlugAvailable(slug, props.modelValue.id)
    if (sequence !== validationSequence)
      return false
    const available = response.data === true
    slugStatus.value = available ? 'success' : 'error'
    slugFeedback.value = available ? '该访问地址可用。' : '该访问地址已被占用。'
    return available
  }
  catch (error) {
    if (sequence !== validationSequence)
      return false
    slugStatus.value = 'error'
    slugFeedback.value = error?.message || '地址校验失败。'
    return false
  }
}

async function copyLink(value, label) {
  await navigator.clipboard.writeText(value)
  message.success(`${label}访问链接已复制`)
}

function openLink(value) {
  window.open(value, '_blank', 'noopener,noreferrer')
}

defineExpose({ checkSlug })
</script>

<style scoped>
.access-form {
  width: 100%;
}

.access-block {
  margin-bottom: 20px;
  padding: 16px 18px 4px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-secondary, #f8fafc) 70%, #fff);
}

.portal-slug-field {
  width: min(100%, 480px);
}

.access-link-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.access-link-card {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  background: #fff;
}

.access-link-card__qr {
  display: grid;
  place-items: center;
  width: 96px;
  height: 96px;
  overflow: hidden;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #fff;
}

.access-link-card__qr :deep(.n-qr-code) {
  display: block;
  width: 96px !important;
  height: 96px !important;
}

.access-link-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.access-link-card__label {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
}

.access-link-card__url {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-primary, #1f2329);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.45;
  word-break: break-all;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.access-link-card__desc {
  margin: 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.5;
}

.access-link-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

@media (max-width: 1080px) {
  .access-link-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .access-link-card {
    grid-template-columns: 1fr;
    justify-items: start;
  }
}
</style>
