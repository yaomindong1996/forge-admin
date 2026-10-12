<template>
  <div class="legacy-config-page">
    <n-result
      status="info"
      title="配置入口已迁移"
      description="业务页面配置、功能代码预览和下载已统一收敛到应用管理。"
    >
      <template #footer>
        <n-space justify="center">
          <n-button type="primary" @click="router.push('/app-center')">
            进入应用管理
          </n-button>
        </n-space>
      </template>
    </n-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.legacy-config-page {
  display: grid;
  min-height: 100%;
  place-items: center;
  background: var(--bg-secondary);
  padding: 24px;
}
</style>
