<template>
  <n-dropdown
    v-if="switchableTenantCount > 1"
    trigger="click"
    :options="dropdownOptions"
    :disabled="dropdownOptions.length === 0"
    :render-label="renderDropdownLabel"
    @select="handleSelect"
  >
    <button
      type="button" class="tenant-switcher" :disabled="switchingTenant"
      :title="`当前租户：${currentTenantName}`" :aria-label="`切换租户，当前为${currentTenantName}`"
    >
      <i class="tenant-icon i-material-symbols:domain-rounded" />
      <span class="tenant-name">{{ currentTenantName }}</span>
      <i v-if="switchableTenantCount > 1" class="tenant-arrow i-material-symbols:expand-more-rounded" />
    </button>
  </n-dropdown>
</template>

<script setup>
import api from '@/api'
import { useAuthStore, useUserStore } from '@/store'

const router = useRouter()
const userStore = useUserStore()
const authStore = useAuthStore()

const tenantOptions = ref([])
const switchingTenant = ref(false)

const currentTenantId = computed(() => userStore.userInfo?.tenantId)
const currentTenantName = computed(() => {
  const current = tenantOptions.value.find(item => item.tenantId === currentTenantId.value)
  return current?.tenantName || userStore.userInfo?.tenantName || '当前租户'
})
const displayTenantOptions = computed(() => {
  if (tenantOptions.value.length > 0) {
    return tenantOptions.value
  }
  if (!currentTenantId.value) {
    return []
  }
  return [{
    tenantId: currentTenantId.value,
    tenantName: userStore.userInfo?.tenantName || '当前租户',
  }]
})
const switchableTenantCount = computed(() => displayTenantOptions.value.length)
const dropdownOptions = computed(() => displayTenantOptions.value.map(item => ({
  label: item.tenantName || `租户 ${item.tenantId}`,
  key: `tenant:${item.tenantId}`,
  tenantId: item.tenantId,
  tenantName: item.tenantName || `租户 ${item.tenantId}`,
  current: item.tenantId === currentTenantId.value,
  disabled: switchingTenant.value || item.tenantId === currentTenantId.value,
  icon: () => h('i', {
    class: item.tenantId === currentTenantId.value
      ? 'i-material-symbols:check-circle-rounded text-14 text-success'
      : 'i-material-symbols:corporate-fare-rounded text-14',
  }),
})))

function renderDropdownLabel(option) {
  return h('div', {
    style: {
      minWidth: '168px',
      display: 'flex',
      flexDirection: 'column',
      gap: '2px',
      padding: '2px 0',
    },
  }, [
    h('span', {
      style: {
        color: option.current ? 'var(--primary-color)' : 'var(--text-primary)',
        fontSize: '13px',
        fontWeight: option.current ? 700 : 600,
        lineHeight: 1.25,
      },
    }, option.tenantName || option.label),
    h('span', {
      style: {
        color: 'var(--text-tertiary)',
        fontSize: '12px',
        lineHeight: 1.2,
      },
    }, option.current ? '当前租户' : '可切换租户'),
  ])
}

async function loadTenantOptions() {
  if (!userStore.userInfo)
    return
  try {
    const res = await api.getCurrentTenantOptions()
    if (res.code === 200) {
      tenantOptions.value = res.data || []
    }
  }
  catch (error) {
    console.error('load tenant options error', error)
  }
}

function handleSelect(key) {
  if (!key?.startsWith('tenant:'))
    return
  handleTenantSwitch(Number(key.replace('tenant:', '')))
}

async function handleTenantSwitch(tenantId) {
  if (!tenantId || tenantId === currentTenantId.value)
    return
  try {
    switchingTenant.value = true
    const res = await api.switchTenant(tenantId)
    if (res.code === 200) {
      authStore.resetLoginState({ resetAuth: false })
      $message.success('租户已切换')
      await router.replace({ path: '/', query: { tenantSwitch: Date.now() } })
    }
  }
  catch {
    $message.error('切换租户失败')
  }
  finally {
    switchingTenant.value = false
  }
}

watch(() => userStore.userInfo?.tenantId, () => {
  loadTenantOptions()
}, { immediate: true })
</script>

<style scoped>
.tenant-switcher {
  height: 32px;
  max-width: 220px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 9px;
  border: 1px solid var(--chrome-control-border, color-mix(in srgb, currentColor 22%, transparent));
  border-radius: 8px;
  background: var(--chrome-control-bg, color-mix(in srgb, currentColor 8%, transparent));
  color: var(--chrome-text, var(--text-secondary));
  cursor: pointer;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
  margin-right: 8px;
}

.tenant-switcher:hover {
  border-color: var(--chrome-control-border, color-mix(in srgb, currentColor 34%, transparent));
  background: var(--chrome-control-hover-bg, color-mix(in srgb, currentColor 12%, transparent));
  color: var(--chrome-text, var(--text-primary));
}

.tenant-switcher.is-single {
  cursor: default;
}

.tenant-icon,
.tenant-arrow {
  flex-shrink: 0;
  font-size: 16px;
  color: inherit;
}

.tenant-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
}
</style>
