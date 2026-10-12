<template>
  <section class="publish-section-card">
    <header>
      <div>
        <h2>{{ pageId ? '页面地址' : '组织内访问' }}</h2>
        <p>{{ pageId ? '电脑端打开当前页面的独立运行地址；移动端打开独立 H5 运行页。' : '电脑端打开应用门户，移动端打开独立 H5 运行页。' }}</p>
      </div>
      <n-tag :type="portalAvailable ? 'success' : 'warning'" :bordered="false">
        {{ portalAvailable ? '可访问' : '暂不可用' }}
      </n-tag>
    </header>
    <div class="publish-access-grid">
      <div class="publish-access-body">
        <n-qr-code :value="pcUrl" :size="120" :color="themeColor" />
        <div class="publish-access-copy">
          <span>电脑端</span>
          <strong>{{ pcUrl }}</strong>
          <p>{{ pageId ? '在电脑浏览器直接打开当前页面，不经过应用门户。' : '在电脑浏览器打开应用门户。' }}</p>
          <n-space>
            <n-button secondary @click="copyLink(pcUrl, '电脑端')">
              复制链接
            </n-button>
            <n-button type="primary" :disabled="!portalAvailable" @click="openLink(pcUrl)">
              打开电脑端
            </n-button>
          </n-space>
        </div>
      </div>
      <div class="publish-access-body is-h5">
        <n-qr-code :value="h5Url" :size="120" :color="themeColor" />
        <div class="publish-access-copy">
          <span>移动端</span>
          <strong>{{ h5Url }}</strong>
          <p>扫码在手机打开 H5 运行页，和旧访问入口同一套地址。</p>
          <n-space>
            <n-button secondary :disabled="!h5Url" @click="copyLink(h5Url, '移动端')">
              复制移动端链接
            </n-button>
            <n-button type="primary" :disabled="!portalAvailable || !h5Url" @click="openLink(h5Url)">
              打开移动端
            </n-button>
          </n-space>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed } from 'vue'
import { buildPortalAccessUrls, normalizePortalConfig } from '../portal/portal-config'

const props = defineProps({
  application: { type: Object, required: true },
  pageId: { type: String, default: '' },
  configKey: { type: String, default: '' },
  objects: { type: Array, default: () => [] },
})
const message = useMessage()
const portalConfig = computed(() => normalizePortalConfig(props.application.portalConfig))
const themeColor = computed(() => portalConfig.value.themeColor || '#3370ff')
const accessUrls = computed(() => buildPortalAccessUrls({
  origin: window.location.origin,
  basePath: import.meta.env.BASE_URL,
  slug: props.application.portalSlug || props.application.applicationCode,
  pageId: props.pageId,
  configKey: props.configKey,
  h5BaseUrl: portalConfig.value.distribution?.h5BaseUrl,
  appId: props.application.id,
  application: props.application,
  objects: props.objects,
}))
const pcUrl = computed(() => accessUrls.value.pcUrl)
const h5Url = computed(() => accessUrls.value.h5Url)
const portalAvailable = computed(() => Boolean(props.application.lastPublishVersion) && Number(props.application.status) === 1)

async function copyLink(value, label) {
  await navigator.clipboard.writeText(value)
  message.success(`${label}访问链接已复制`)
}

function openLink(value) {
  window.open(value, '_blank', 'noopener,noreferrer')
}
</script>

<style scoped>
.publish-section-card {
  min-width: 0;
  padding: 20px 24px;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  background: var(--bg-primary);
  box-shadow: 0 1px 3px rgb(31 35 41 / 6%);
}

.publish-section-card > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.publish-section-card h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
}

.publish-section-card header p {
  margin: 5px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
}

.publish-access-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.publish-access-body {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.publish-access-body.is-h5 {
  background: #f2f6ff;
}

.publish-access-copy {
  display: grid;
  min-width: 0;
  gap: 7px;
}

.publish-access-copy span,
.publish-access-copy p {
  margin: 0;
  color: var(--text-color-3, #86909c);
  font-size: 12px;
}

.publish-access-copy strong {
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1080px) {
  .publish-access-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .publish-access-body {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
