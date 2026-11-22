<script setup>
import { NButton, useMessage } from 'naive-ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { locateAssistantGuide } from '@/composables/ai/assistant-guide-location'
import { usePermissionStore, useUserStore } from '@/store'
import './guide-highlight.css'

defineProps({ guide: { type: Object, required: true } })
const emit = defineEmits(['locate'])
const user = useUserStore()
const permission = usePermissionStore()
const router = useRouter()
const message = useMessage()
const locating = ref(false)

async function locate(option, anchor) {
  if (locating.value)
    return
  locating.value = true
  try {
    const result = await locateAssistantGuide(router, permission, user, option, anchor, () => emit('locate'))
    if (result === 'marked')
      message.success('已在页面标记对应位置')
    else if (result === 'opened')
      message.info('已到目标页面。当前没有可标记的操作控件。')
    else
      message.warning('页面已切换或入口不可访问，请按当前页面重新提问。')
  }
  catch {
    message.error('打开目标页面失败，请稍后再试。')
  }
  finally {
    locating.value = false
  }
}
</script>

<template>
  <section class="page-guide" aria-label="页面操作指引">
    <p v-if="guide.note" class="guide-note">
      {{ guide.note }}
    </p>
    <div v-for="(option, index) in guide.options" :key="option.id ?? index" class="guide-option">
      <div class="guide-head">
        <i class="i-lucide:map-pin" aria-hidden="true" />
        <div>
          <strong>{{ option.title }}</strong>
          <small v-if="option.trail.length > 1">{{ option.trail.join(' › ') }}</small>
        </div>
      </div>
      <ul v-if="option.steps.length" class="guide-steps">
        <li v-for="(step, stepIndex) in option.steps" :key="step.anchor">
          <span>{{ step.text }}</span>
          <NButton
            v-if="option.steps.length > 1" text type="primary" size="small"
            :aria-label="`定位第 ${stepIndex + 1} 个操作`" :disabled="locating" @click="locate(option, step.anchor)"
          >
            定位
          </NButton>
        </li>
      </ul>
      <NButton
        text type="primary" size="small" :loading="locating"
        @click="locate(option, option.steps[0]?.anchor || 'page')"
      >
        {{ option.currentScope ? '标记位置' : '打开并标记' }}
        <template #icon>
          <i class="i-lucide:locate-fixed" aria-hidden="true" />
        </template>
      </NButton>
    </div>
  </section>
</template>

<style scoped>
.page-guide {
  display: grid;
  gap: 8px;
  margin: 8px 0 0;
}
.guide-option {
  display: grid;
  gap: 7px;
  justify-items: start;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}
.guide-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.guide-head i {
  flex: none;
  margin-top: 3px;
  color: var(--primary-color);
}
.guide-head strong,
.guide-head small {
  display: block;
}
.guide-head small {
  color: var(--text-secondary);
}
.guide-steps {
  display: grid;
  gap: 5px;
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
}
.guide-steps button {
  margin-left: 6px;
}
.guide-note {
  margin: 0;
  color: var(--text-secondary);
}
</style>
