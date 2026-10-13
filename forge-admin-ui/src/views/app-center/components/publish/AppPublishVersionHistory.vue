<template>
  <section class="publish-history-host">
    <ApplicationPublishPanel
      ref="panelRef"
      :application="application"
      @changed="emit('changed')"
      @navigate="emit('navigate', $event)"
    />
  </section>
</template>

<script setup>
import { ref } from 'vue'
import ApplicationPublishPanel from '../../application-workspace/ApplicationPublishPanel.vue'

defineProps({ application: { type: Object, required: true } })
const emit = defineEmits(['changed', 'navigate'])
const panelRef = ref(null)

function preparePublish() {
  panelRef.value?.preparePublish?.()
}

defineExpose({ preparePublish })
</script>

<style scoped>
.publish-history-host {
  border: 1px solid var(--border-color, #e5e6eb);
  border-radius: 10px;
  background: var(--card-color, #fff);
  padding: 20px;
}
</style>
