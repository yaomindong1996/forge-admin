<template>
  <div class="app-settings-panel">
    <n-spin :show="loading">
      <div v-if="settingsLoaded" class="settings-panel-layout">
        <aside class="settings-panel-nav">
          <button
            v-for="item in sections"
            :key="item.key"
            type="button"
            :class="{ active: activeSection === item.key }"
            @click="selectSection(item.key)"
          >
            <n-icon><component :is="item.icon" /></n-icon>
            <span>{{ item.label }}</span>
          </button>
        </aside>
        <main class="settings-panel-content">
          <AppSettingsBasic v-if="activeSection === 'basic'" v-model="settingsModel" />
          <AppSettingsAccess v-else-if="activeSection === 'access'" ref="accessRef" v-model="settingsModel" :application="application" />
          <AppSettingsNavigation v-else-if="activeSection === 'navigation'" v-model="settingsModel" :pages="applicationPages" />
          <AppSettingsPermission
            v-else-if="activeSection === 'permission'"
            v-model="settingsModel"
            :application="application"
          />
          <AppSettingsGlobalization v-else-if="activeSection === 'globalization'" v-model="settingsModel" />
          <AppSettingsAdvanced v-else v-model="settingsModel" />
          <div class="settings-panel-actions">
            <n-button type="primary" :loading="saving" @click="saveSettings">
              保存设置
            </n-button>
          </div>
        </main>
      </div>
      <n-result v-else-if="!loading" status="error" title="加载失败" :description="loadError">
        <template #footer>
          <n-button @click="loadSettings">
            重新加载
          </n-button>
        </template>
      </n-result>
    </n-spin>
  </div>
</template>

<script setup>
import {
  ColorPaletteOutline,
  EarthOutline,
  LinkOutline,
  LockClosedOutline,
  MenuOutline,
  OptionsOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  businessApplicationDetailByCode,
  checkBusinessApplicationSlugAvailable,
  saveBusinessApplicationPortalConfig,
  updateBusinessApplication,
} from '@/api/business-application'
import { resolveApplicationSettingsSection } from './application-print-entry'
import { normalizePortalConfig, parseJsonObject } from './portal/portal-config'
import AppSettingsAccess from './settings/AppSettingsAccess.vue'
import AppSettingsAdvanced from './settings/AppSettingsAdvanced.vue'
import AppSettingsBasic from './settings/AppSettingsBasic.vue'
import AppSettingsGlobalization from './settings/AppSettingsGlobalization.vue'
import AppSettingsNavigation from './settings/AppSettingsNavigation.vue'
import AppSettingsPermission from './settings/AppSettingsPermission.vue'

const props = defineProps({
  application: { type: Object, default: null },
})

const emit = defineEmits(['saved'])

const message = useMessage()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const activeSection = ref(resolveApplicationSettingsSection(route.query.settingsSection))
const settingsModel = ref({})
const applicationOptions = ref({})
const accessRef = ref(null)

const sections = [
  { key: 'basic', label: '基础属性', icon: ColorPaletteOutline },
  { key: 'access', label: '访问地址', icon: LinkOutline },
  { key: 'navigation', label: '导航设置', icon: MenuOutline },
  { key: 'permission', label: '应用权限', icon: LockClosedOutline },
  { key: 'globalization', label: '全球化', icon: EarthOutline },
  { key: 'advanced', label: '高级设置', icon: OptionsOutline },
]

const settingsLoaded = computed(() => !!props.application && !!settingsModel.value.applicationName)

const applicationPages = computed(() => applicationOptions.value?.inAppBuilder?.nodes || [])

function selectSection(section) {
  const next = resolveApplicationSettingsSection(section)
  activeSection.value = next
  const settingsSection = next === 'basic' ? undefined : next
  if (route.query.settingsSection === settingsSection)
    return
  router.replace({
    query: {
      ...route.query,
      settingsSection,
    },
  })
}

async function loadSettings() {
  const code = props.application?.applicationCode
  if (!code)
    return
  loading.value = true
  loadError.value = ''
  try {
    const response = await businessApplicationDetailByCode(code)
    const app = response.data || null
    if (!app)
      throw new Error('应用不存在')
    applicationOptions.value = parseJsonObject(app.options)
    const portal = normalizePortalConfig(app.portalConfig)
    const pageOrder = applicationPages.value
      .filter(node => node.type === 'page')
      .sort((left, right) => Number(left.sort || 0) - Number(right.sort || 0))
      .map(node => String(node.id))
    settingsModel.value = {
      ...portal,
      id: app.id,
      applicationName: app.applicationName || '',
      applicationCode: app.applicationCode || '',
      portalSlug: app.portalSlug || app.applicationCode || '',
      icon: app.icon || '',
      description: app.description || '',
      status: app.status === 0 ? 0 : 1,
      navigation: { ...portal.navigation, pageOrder: portal.navigation.pageOrder?.length ? portal.navigation.pageOrder : pageOrder },
    }
  }
  catch (error) {
    loadError.value = error?.message || '暂时无法读取应用设置。'
  }
  finally {
    loading.value = false
  }
}

async function saveSettings() {
  if (!props.application || saving.value)
    return
  if (!String(settingsModel.value.applicationName || '').trim()) {
    message.error('请输入应用名称')
    activeSection.value = 'basic'
    return
  }
  saving.value = true
  try {
    let slugAvailable = true
    if (accessRef.value)
      slugAvailable = await accessRef.value.checkSlug()
    else
      slugAvailable = (await checkBusinessApplicationSlugAvailable(settingsModel.value.portalSlug, props.application.id)).data === true
    if (!slugAvailable) {
      activeSection.value = 'access'
      message.error('请先修正门户访问地址')
      return
    }
    await updateBusinessApplication({
      id: props.application.id,
      applicationCode: props.application.applicationCode,
      applicationName: String(settingsModel.value.applicationName).trim(),
      suiteCode: props.application.suiteCode,
      icon: settingsModel.value.icon || null,
      description: settingsModel.value.description || null,
      status: settingsModel.value.status,
      options: JSON.stringify(applicationOptions.value),
    })
    await saveBusinessApplicationPortalConfig(props.application.id, {
      portalSlug: settingsModel.value.portalSlug,
      portalConfig: settingsModel.value,
    })
    message.success('应用设置已保存')
    emit('saved')
    await loadSettings()
  }
  catch (error) {
    message.error(error?.message || '保存应用设置失败')
  }
  finally {
    saving.value = false
  }
}

watch(() => props.application?.applicationCode, (code) => {
  if (code)
    loadSettings()
}, { immediate: true })
watch(() => route.query.settingsSection, (section) => {
  activeSection.value = resolveApplicationSettingsSection(section)
})
</script>

<style scoped>
.app-settings-panel {
  width: 100%;
  min-height: 100%;
  padding: 0;
}

.app-settings-panel :deep(.n-spin-container),
.app-settings-panel :deep(.n-spin-content) {
  min-height: 360px;
}

.settings-panel-layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 20px;
  width: 100%;
}

.settings-panel-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-self: start;
  padding: 8px;
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  background: #fff;
}

.settings-panel-nav button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
  text-align: left;
}

.settings-panel-nav button:hover {
  background: #f2f3f5;
  color: #1f2329;
}

.settings-panel-nav button.active {
  background: #f2f3f5;
  color: #1f2329;
  font-weight: 600;
}

.settings-panel-content {
  min-width: 0;
}

.settings-panel-content :deep(.settings-section-card) {
  padding: 24px;
  border: 1px solid #e5e6eb;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 3px rgb(31 35 41 / 6%);
}

.settings-panel-content :deep(.settings-section-card > header) {
  margin-bottom: 22px;
}

.settings-panel-content :deep(.settings-section-card > header h2) {
  margin: 0;
  font-size: 18px;
  font-weight: 650;
}

.settings-panel-content :deep(.settings-section-card > header p) {
  margin: 6px 0 0;
  color: #86909c;
  font-size: 13px;
}

.settings-panel-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e5e6eb;
}
</style>
