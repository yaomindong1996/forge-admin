<template>
  <div class="resource-empty" role="status">
    <div class="resource-empty__drawing" aria-hidden="true">
      <i :class="icon" /><span /><span />
    </div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <div class="resource-empty__actions">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  icon: { type: String, default: 'i-lucide:layers' },
})
</script>
