<template>
  <div class="page-design-publish">
    <section class="page-design-publish-card">
      <header>
        <div>
          <h2>页面访问</h2>
          <p>保存后可预览草稿；电脑端和移动端正式地址要等应用发布后才会按当前页面生效。</p>
        </div>
        <n-space>
          <n-button secondary :loading="saving" :disabled="!dirty" @click="emit('save')">
            <template #icon>
              <n-icon><SaveOutline /></n-icon>
            </template>
            保存当前页面
          </n-button>
          <n-button secondary :disabled="!pageId" @click="emit('preview')">
            <template #icon>
              <n-icon><PlayOutline /></n-icon>
            </template>
            预览页面
          </n-button>
        </n-space>
      </header>
      <dl class="page-design-publish-meta">
        <div>
          <dt>当前页面</dt>
          <dd>{{ pageTitle || '未命名页面' }}</dd>
        </div>
        <div>
          <dt>页面编码</dt>
          <dd>{{ pageId || '-' }}</dd>
        </div>
        <div>
          <dt>草稿状态</dt>
          <dd>{{ dirty ? '有未保存修改' : '已保存到草稿' }}</dd>
        </div>
        <div>
          <dt>正式发布</dt>
          <dd>{{ published ? `已发布 v${application.lastPublishVersion}` : '尚未发布，正式地址暂不可用' }}</dd>
        </div>
      </dl>
    </section>

    <!-- 系统菜单挂载：可选。与「应用权限 → 页面入口」无关；门户内能否看到页面只看角色授权。 -->
    <section class="page-design-publish-card">
      <header>
        <div>
          <h2>系统菜单挂载（可选）</h2>
          <p>
            只影响是否出现在<strong>系统左侧菜单</strong>。应用门户内的页面入口由「应用权限」授权，发布后即可勾选，不必挂载系统菜单。
          </p>
        </div>
        <n-switch
          :value="mountDraft.systemMenuVisible === true"
          @update:value="onMountEnabledChange"
        />
      </header>
      <div v-if="mountDraft.systemMenuVisible" class="mount-config">
        <n-tag size="small" :type="mountReady ? 'success' : 'warning'" :bordered="false" class="mount-status-tag">
          {{ mountReady
            ? '已配置父级，发布后出现在系统菜单'
            : '未选父级：不会进系统菜单，但应用门户入口仍可按角色授权' }}
        </n-tag>
        <PageSystemMenuMountForm
          :model-value="mountDraft"
          :hint="''"
          @update:model-value="onMountDraftChange"
        />
      </div>
      <p v-else class="mount-off-hint">
        未挂载系统菜单。普通用户进应用门户看页面：发布应用 → 应用权限勾选「页面入口」→ 用户重新登录即可。
      </p>
    </section>

    <AppPublishAccess :application="application" :page-id="pageId" :config-key="configKey" :objects="objects" />

    <!-- 表单填报直链 -->
    <section v-if="formFillLinks.length" class="page-design-publish-card">
      <header>
        <div>
          <h2>表单填报直链</h2>
          <p>可直接分享此链接，打开后进入表单填报模式。</p>
        </div>
      </header>
      <div class="form-fill-links">
        <div v-for="link in formFillLinks" :key="link.label" class="form-fill-link-row">
          <span class="link-label">{{ link.label }}</span>
          <n-input-group>
            <n-input :value="link.url" readonly size="small" />
            <n-button size="small" secondary @click="copyLink(link.url, link.label)">
              复制
            </n-button>
          </n-input-group>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { PlayOutline, SaveOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed } from 'vue'
import AppPublishAccess from '../publish/AppPublishAccess.vue'
import PageSystemMenuMountForm from './PageSystemMenuMountForm.vue'
import {
  createPageMountDraft,
  showAdminMenuMountConfig,
  showMobileMenuMountConfig,
} from './page-system-menu-mount'

const props = defineProps({
  application: { type: Object, required: true },
  node: { type: Object, default: null },
  pageId: { type: String, default: '' },
  pageTitle: { type: String, default: '' },
  dirty: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  configKey: { type: String, default: '' },
  objects: { type: Array, default: () => [] },
})

const emit = defineEmits(['save', 'preview', 'update'])
const message = useMessage()

const published = computed(() => Boolean(props.application?.lastPublishVersion) && Number(props.application?.status) === 1)

const mountDraft = computed(() => createPageMountDraft(props.node))

const currentMountTarget = computed(() => mountDraft.value.mountTarget)

const mountReady = computed(() => {
  const draft = mountDraft.value
  if (!draft.systemMenuVisible)
    return false
  const target = String(draft.mountTarget || 'ADMIN').toUpperCase()
  if (showAdminMenuMountConfig(target) && !draft.menuParentId)
    return false
  if (showMobileMenuMountConfig(target) && !draft.mobileMenuParentId)
    return false
  return true
})

const formFillLinks = computed(() => {
  const origin = window.location.origin
  const basePath = String(import.meta.env.BASE_URL || '').replace(/\/$/, '')
  const slug = props.application?.portalSlug || props.application?.applicationCode
  const appId = props.application?.id
  const mt = currentMountTarget.value
  const links = []

  if (mt === 'ADMIN' || mt === 'BOTH') {
    if (props.pageId && slug) {
      links.push({
        label: '管理端 · 表单填报',
        url: `${origin}${basePath}/app/${encodeURIComponent(slug)}?pageId=${encodeURIComponent(props.pageId)}&runtimeOpenMode=CREATE_FORM&mode=create`,
      })
    }
    if (props.configKey) {
      links.push({
        label: '低代码 · 表单填报',
        url: `${origin}${basePath}/ai/crud-page/${props.configKey}?appId=${appId}&runtimeOpenMode=CREATE_FORM&mode=create`,
      })
    }
  }

  if ((mt === 'MOBILE' || mt === 'BOTH') && props.configKey) {
    const h5Base = props.application?.portalConfig?.distribution?.h5BaseUrl || 'http://localhost:3009'
    links.push({
      label: '移动端 · 表单填报',
      url: `${h5Base.replace(/\/$/, '')}/#/pages/lowcode-runtime?configKey=${props.configKey}&appId=${appId}&mode=create`,
    })
  }

  return links
})

function onMountEnabledChange(enabled) {
  emit('update', {
    systemMenuVisible: enabled === true,
    ...(enabled ? {} : { menuParentId: null, mobileMenuParentId: null }),
  })
}

function onMountDraftChange(next = {}) {
  const previous = mountDraft.value
  const patch = {}
  for (const key of ['mountTarget', 'menuName', 'menuParentId', 'mobileMenuParentId', 'menuSort', 'systemMenuVisible']) {
    if (next[key] !== previous[key])
      patch[key] = next[key]
  }
  if (!Object.keys(patch).length)
    return
  // 仅在开关已打开时保留挂载；改挂载位置不再偷偷打开开关
  if (previous.systemMenuVisible !== true && patch.systemMenuVisible !== true)
    patch.systemMenuVisible = false
  else if (previous.systemMenuVisible === true || patch.systemMenuVisible === true)
    patch.systemMenuVisible = true
  emit('update', patch)
}

async function copyLink(url, label) {
  try {
    await navigator.clipboard.writeText(url)
    message.success(`${label}链接已复制`)
  }
  catch {
    message.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.page-design-publish {
  display: grid;
  gap: 16px;
  width: min(1120px, 100%);
  margin: 0 auto;
}

.page-design-publish-card {
  padding: 24px;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  background: var(--bg-primary);
  box-shadow: 0 1px 3px rgb(31 35 41 / 6%);
}

.page-design-publish-card header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-design-publish-card h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 650;
}

.page-design-publish-card p {
  margin: 6px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.page-design-publish-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 0;
}

.page-design-publish-meta div {
  display: grid;
  gap: 6px;
}

.page-design-publish-meta dt {
  color: var(--text-tertiary);
  font-size: 12px;
}

.page-design-publish-meta dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 14px;
  word-break: break-all;
}

.mount-config {
  display: grid;
  gap: 12px;
}

.mount-status-tag {
  width: fit-content;
}

.mount-off-hint {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 1.6;
}

.form-fill-links {
  display: grid;
  gap: 10px;
}

.form-fill-link-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.link-label {
  color: var(--text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .page-design-publish-card header,
  .page-design-publish-meta {
    grid-template-columns: 1fr;
  }

  .page-design-publish-card header {
    display: grid;
  }

  .form-fill-link-row {
    grid-template-columns: 1fr;
  }
}
</style>
