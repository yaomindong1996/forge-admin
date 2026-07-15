<template>
  <header class="workspace-header">
    <div class="workspace-identity">
      <n-button quaternary circle class="back-button" aria-label="返回应用总览" @click="emit('back')">
        <template #icon>
          <n-icon><ArrowBackOutline /></n-icon>
        </template>
      </n-button>
      <span class="workspace-icon" aria-hidden="true">
        <IconRenderer v-if="application?.icon" :icon="application.icon" :size="22" />
        <n-icon v-else><AppsOutline /></n-icon>
      </span>
      <div class="workspace-title">
        <div class="workspace-title-line">
          <h1>{{ application?.applicationName || '应用工作台' }}</h1>
          <DictTag
            dict-type="ai_business_application_design_status"
            :value="application?.designStatus"
            :bordered="false"
          />
          <DictTag dict-type="sys_enable_disable" :value="application?.status" :bordered="false" />
        </div>
        <p>
          <span>{{ application?.suiteName || application?.suiteCode || '未关联业务域' }}</span>
          <code>{{ application?.applicationCode }}</code>
          <span v-if="workspace?.latestChangeTime">最近更新 {{ workspace.latestChangeTime }}</span>
        </p>
      </div>
    </div>

    <div class="workspace-actions">
      <span class="readiness-state" :class="readinessClass">
        <i />
        {{ readinessLabel }}
      </span>
      <n-button secondary :loading="refreshing" @click="emit('refresh')">
        刷新
      </n-button>
      <n-button secondary @click="emit('preview')">
        <template #icon>
          <n-icon><EyeOutline /></n-icon>
        </template>
        预览应用
      </n-button>
      <n-button secondary @click="emit('code')">
        <template #icon>
          <n-icon><CodeSlashOutline /></n-icon>
        </template>
        代码
      </n-button>
      <n-button secondary @click="emit('primaryAction')">
        {{ primaryActionLabel }}
      </n-button>
      <n-button type="primary" @click="emit('publish')">
        <template #icon>
          <n-icon><RocketOutline /></n-icon>
        </template>
        发布应用
      </n-button>
    </div>
  </header>
</template>

<script setup>
import { AppsOutline, ArrowBackOutline, CodeSlashOutline, EyeOutline, RocketOutline } from '@vicons/ionicons5'
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'

const props = defineProps({
  application: {
    type: Object,
    default: null,
  },
  workspace: {
    type: Object,
    default: null,
  },
  refreshing: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['back', 'refresh', 'preview', 'code', 'primaryAction', 'publish'])

const readinessLabel = computed(() => {
  if (Number(props.workspace?.blockingCount || 0) > 0)
    return `${props.workspace.blockingCount} 项阻断`
  if (Number(props.workspace?.warningCount || 0) > 0)
    return `${props.workspace.warningCount} 项提醒`
  return '配置就绪'
})

const readinessClass = computed(() => {
  if (Number(props.workspace?.blockingCount || 0) > 0)
    return 'is-blocked'
  if (Number(props.workspace?.warningCount || 0) > 0)
    return 'is-warning'
  return 'is-ready'
})

const primaryActionLabel = computed(() => Number(props.application?.entryCount || 0) > 0 ? '查看页面入口' : '配置数据对象')
</script>

<style scoped>
.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 60px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  background: var(--bg-primary, #fff);
}

.workspace-identity,
.workspace-title-line,
.workspace-actions {
  display: flex;
  align-items: center;
}

.workspace-identity {
  min-width: 0;
  gap: 9px;
}

.workspace-icon {
  display: inline-flex;
  flex: 0 0 34px;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
  color: var(--primary-color, #165dff);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 18px;
}

.back-button {
  flex: 0 0 auto;
}

.workspace-title {
  min-width: 0;
}

.workspace-title-line {
  flex-wrap: wrap;
  gap: 7px;
}

.workspace-title h1 {
  overflow: hidden;
  margin: 0;
  color: var(--text-primary, #1d2129);
  font-size: 16px;
  font-weight: 650;
  line-height: 26px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-title p {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 2px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.workspace-title code {
  color: var(--text-secondary, #4e5969);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.workspace-actions {
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.readiness-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 4px;
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}

.readiness-state i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2da44e;
}

.readiness-state.is-warning i {
  background: #bf8700;
}

.readiness-state.is-blocked i {
  background: #cf222e;
}

@media (max-width: 860px) {
  .workspace-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .workspace-actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
