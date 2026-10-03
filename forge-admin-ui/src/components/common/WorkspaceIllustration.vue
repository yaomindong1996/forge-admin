<script setup>
import { computed } from 'vue'
import applicationArtwork from '@/assets/images/workbench/application-assets.webp'
import workflowArtwork from '@/assets/images/workbench/workflow-model.webp'
import welcomeArtwork from '@/assets/images/workbench/workspace-welcome.webp'

const props = defineProps({
  artwork: {
    type: String,
    default: 'application',
    validator: value => ['welcome', 'application', 'workflow'].includes(value),
  },
  size: { type: String, default: 'empty', validator: value => ['hero', 'card', 'empty', 'small'].includes(value) },
  eager: Boolean,
})

const artworks = { welcome: welcomeArtwork, application: applicationArtwork, workflow: workflowArtwork }
const source = computed(() => artworks[props.artwork] || applicationArtwork)
</script>

<template>
  <!-- 插画不承载业务信息，避免屏幕阅读器重复读出卡片名称。 -->
  <img
    :src="source"
    class="workspace-illustration"
    :class="`workspace-illustration--${size}`"
    alt=""
    aria-hidden="true"
    :loading="eager ? 'eager' : 'lazy'"
    decoding="async"
    draggable="false"
  >
</template>

<style scoped>
.workspace-illustration {
  display: block;
  flex-shrink: 0;
  max-width: 100%;
  object-fit: contain;
  pointer-events: none;
  user-select: none;
}

.workspace-illustration--hero {
  width: 200px;
  height: 138px;
}

.workspace-illustration--card {
  width: 58px;
  height: 58px;
}

.workspace-illustration--empty {
  width: 112px;
  height: 100px;
}

.workspace-illustration--small {
  width: 80px;
  height: 72px;
}

html.dark .workspace-illustration {
  opacity: 0.85;
}
</style>
