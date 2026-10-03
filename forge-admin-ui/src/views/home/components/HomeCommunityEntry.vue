<template>
  <section class="home-community-entry">
    <!-- 首页保留明确入口，二维码按需展开，不占用业务区高度 -->
    <div><i class="i-material-symbols:help-outline-rounded" /><strong>社区与支持</strong><span>使用交流与技术帮助</span></div>
    <NButton text type="primary" @click="supportVisible = true">
      扫码联系 / 查看二维码
    </NButton>
    <CommunitySupportDialog v-model:show="supportVisible" />
  </section>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { ref } from 'vue'
import CommunitySupportDialog from '@/components/common/CommunitySupportDialog.vue'

const supportVisible = ref(false)
</script>

<style scoped>
.home-community-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 16px;
  padding: 12px 16px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}
.home-community-entry div {
  display: flex;
  align-items: center;
  gap: 8px;
}
.home-community-entry i {
  color: var(--primary-color, #2563eb);
  font-size: 18px;
}
.home-community-entry strong {
  font-size: 13px;
  font-weight: 500;
}
.home-community-entry span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}
@media (max-width: 480px) {
  .home-community-entry span {
    display: none;
  }
}
</style>
