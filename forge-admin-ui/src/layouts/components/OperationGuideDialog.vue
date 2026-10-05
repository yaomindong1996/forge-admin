<template>
  <!-- 全局独立弹层，不依赖当前布局、隐藏按钮或工具浮层的生命周期 -->
  <n-modal v-model:show="appStore.guideOpen" preset="card" title="操作指引" class="operation-guide-dialog">
    <nav class="guide-steps" aria-label="指引步骤">
      <button
        v-for="(item, index) in steps" :key="item.key" type="button"
        :aria-current="index === current ? 'step' : undefined" @click="current = index"
      >
        <i :class="item.icon" /><span>{{ item.title }}</span>
      </button>
    </nav>
    <section class="guide-content" aria-live="polite">
      <span class="guide-counter">{{ current + 1 }} / {{ steps.length }}</span>
      <h3>{{ step.title }}</h3>
      <p>{{ step.content }}</p>
      <n-button v-if="step.key === 'appearance'" secondary type="primary" @click="openAppearance">
        打开布局与外观
      </n-button>
    </section>
    <template #footer>
      <div class="guide-footer">
        <n-button quaternary @click="appStore.guideOpen = false">
          关闭指引
        </n-button>
        <div class="guide-navigation">
          <n-button :disabled="current === 0" @click="current--">
            上一步
          </n-button>
          <n-button v-if="current < steps.length - 1" type="primary" @click="current++">
            下一步
          </n-button>
          <n-button v-else type="primary" @click="appStore.guideOpen = false">
            完成
          </n-button>
        </div>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store'
import { createOperationGuide } from './operation-guide'

const appStore = useAppStore()
const route = useRoute()
const current = ref(0)
const steps = computed(() => createOperationGuide(appStore.layout))
const step = computed(() => steps.value[current.value])
watch(() => appStore.guideOpen, (open) => {
  if (open) {
    current.value = 0
  }
}, { flush: 'sync' })
watch([() => route.fullPath, () => appStore.layout], () => {
  appStore.guideOpen = false
})
function openAppearance() {
  appStore.guideOpen = false
  appStore.appearanceOpen = true
}
</script>

<style>
.operation-guide-dialog {
  width: min(560px, calc(100vw - 32px));
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
}
</style>

<style scoped>
.guide-steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}
.guide-steps button {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 6px 10px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  color: var(--text-secondary);
  background: var(--bg-primary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
.guide-steps button:is(:hover, [aria-current='step']) {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: color-mix(in srgb, var(--primary-color) 6%, var(--bg-primary));
}
.guide-steps button:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}
.guide-content {
  padding: 20px 0;
  min-height: 152px;
  color: var(--text-primary);
}
.guide-counter {
  color: var(--text-tertiary);
  font-size: 12px;
}
.guide-content h3 {
  margin: 8px 0;
  font-size: 16px;
  font-weight: 500;
}
.guide-content p {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
}
.guide-footer,
.guide-navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
@media (max-width: 400px) {
  .guide-steps {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
