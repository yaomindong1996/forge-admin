<template>
  <AppPage>
    <iframe :src="frameSrc" frameborder="0" class="wh-full" />
  </AppPage>
</template>

<script setup>
import { computed } from 'vue'

const route = useRoute()

const frameSrc = computed(() => route.meta.originPath || route.query.page || route.query.url || '')
</script>
