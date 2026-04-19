<template>
  <n-alert
    v-if="demoStore.isDemo"
    type="warning"
    closable
    class="demo-banner"
    @close="handleClose"
  >
    <template #header>
      <div class="flex items-center gap-4">
        <n-icon size="18">
          <Warning />
        </n-icon>
        <span class="font-medium">演示环境</span>
      </div>
    </template>
    {{ demoStore.demoMessage }}
  </n-alert>
</template>

<script setup>
import { Warning } from '@vicons/ionicons5'
import { useDemoStore } from '@/stores/demo'

const demoStore = useDemoStore()

function handleClose() {
  localStorage.setItem('demo-banner-closed', 'true')
}

onMounted(() => {
  demoStore.loadDemoStatus()
})
</script>

<style scoped>
.demo-banner {
  flex-shrink: 0;
  text-align: center;
  background: #fff7e6;
  border-bottom: 1px solid #ffd591;
  border-radius: 0;
}

.demo-banner :deep(.n-alert-body) {
  padding: 4px 16px;
}

.demo-banner :deep(.n-alert-header) {
  padding: 4px 16px;
}
</style>
