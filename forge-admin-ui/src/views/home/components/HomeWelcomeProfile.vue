<script setup>
import WorkspaceIllustration from '@/components/common/WorkspaceIllustration.vue'

defineProps({ title: { type: String, required: true } })
</script>

<template>
  <!-- 首页问候与工作台主视觉 -->
  <div class="home-welcome-profile">
    <div class="home-welcome-copy">
      <span class="home-welcome-kicker">工作台</span>
      <h1>{{ title }}</h1>
      <p>从今日待办开始，让业务有序运转。</p>
      <span class="home-welcome-caption">审批协作 · 应用搭建 · 系统管理</span>
    </div>
    <WorkspaceIllustration artwork="welcome" size="hero" eager />
  </div>
</template>

<style scoped>
.home-welcome-profile {
  grid-area: profile;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  padding: 8px 12px 4px;
}

.home-welcome-copy {
  flex: 1;
  min-width: 0;
}

.home-welcome-profile :deep(.workspace-illustration) {
  width: clamp(110px, 36%, 200px);
}

.home-welcome-kicker {
  color: var(--home-brand, var(--primary-color, #1677ff));
  font-size: 12px;
  font-weight: 500;
}

h1 {
  margin: 8px 0 6px;
  color: var(--home-text, var(--text-primary, #1d2129));
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
}

p {
  margin: 0;
  color: var(--home-text-secondary, var(--text-secondary, #4e5969));
  font-size: 13px;
  line-height: 1.6;
}

.home-welcome-caption {
  display: block;
  margin-top: 12px;
  color: var(--home-muted, var(--text-tertiary, #86909c));
  font-size: 11px;
}

@media (max-width: 600px) {
  .home-welcome-profile {
    gap: 8px;
    padding: 4px;
  }

  .home-welcome-copy {
    min-width: 0;
  }

  h1 {
    font-size: 17px;
  }

  .home-welcome-profile :deep(.workspace-illustration) {
    width: 104px;
    height: 88px;
  }

  .home-welcome-caption {
    display: none;
  }
}
</style>
