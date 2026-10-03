<script setup>
import { useRouter } from 'vue-router'
import HomeActionIcon from './HomeActionIcon.vue'
import HomeSection from './HomeSection.vue'

const router = useRouter()
const steps = [
  { title: '创建应用', desc: '定义业务应用入口', path: '/app-center', icon: 'create' },
  { title: '设计对象', desc: '配置表单、列表与字段', path: '/ai/lowcode-apps', icon: 'design' },
  { title: '编排流程', desc: '组织审批与状态流转', path: '/flow/model', icon: 'workflow' },
  { title: '发布授权', desc: '配置菜单与访问权限', path: '/system/menu', icon: 'authorization' },
]
</script>

<template>
  <!-- 有真实目标路由的四步业务导航 -->
  <HomeSection
    title="业务搭建路径"
    description="从应用创建到发布，按业务步骤进入工作区"
    artwork="application"
    class="home-build-path"
  >
    <template #action>
      <button type="button" @click="router.push('/app-center')">
        应用中心 <HomeActionIcon name="arrow" :size="14" />
      </button>
    </template>
    <nav class="build-step-list" aria-label="业务搭建路径">
      <button
        v-for="(step, index) in steps" :key="step.path" type="button" class="build-step"
        @click="router.push(step.path)"
      >
        <span class="build-step-icon"><HomeActionIcon :name="step.icon" :size="24" /></span>
        <span class="build-step-copy">
          <strong>
            <span class="build-step-number">{{ String(index + 1).padStart(2, '0') }}</span>{{ step.title }}
          </strong>
          <span>{{ step.desc }}</span>
        </span>
        <HomeActionIcon name="arrow" :size="14" class="build-step-arrow" />
      </button>
    </nav>
  </HomeSection>
</template>

<style scoped>
.home-build-path {
  margin-bottom: 12px;
}
.build-step-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 12px 14px;
}
.build-step {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 14px;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 66px;
  padding: 10px 12px;
  border: 1px solid var(--home-border, #e5e7eb);
  border-radius: var(--home-radius-sm, 6px);
  background: var(--home-panel, #fff);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background 160ms ease,
    border-color 160ms ease;
}
.build-step:hover {
  background: var(--home-brand-soft, #f2f6ff);
  border-color: var(--home-brand-border, #bfd0ff);
}
.build-step:focus-visible {
  outline: 2px solid var(--home-brand, #0e42d2);
  outline-offset: 2px;
}
.build-step-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--home-brand, #0e42d2);
}
.build-step-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}
.build-step-copy strong {
  font-size: 13px;
  font-weight: 500;
  line-height: 18px;
}
.build-step-number {
  margin-right: 7px;
  color: var(--home-muted, #86909c);
  font-size: 11px;
  font-weight: 400;
}
.build-step-copy > span {
  color: var(--home-muted, #86909c);
  font-size: 11px;
  line-height: 16px;
}
.build-step-copy strong,
.build-step-copy > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.build-step-arrow {
  color: var(--home-muted, #86909c);
}
@media (max-width: 1000px) {
  .build-step-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 480px) {
  .build-step-list {
    gap: 8px;
    padding: 10px;
  }
  .build-step {
    grid-template-columns: 24px minmax(0, 1fr);
    gap: 7px;
    padding: 10px 8px;
  }
  .build-step-arrow {
    display: none;
  }
  .build-step-number {
    margin-right: 4px;
  }
}
</style>
