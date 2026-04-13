<template>
  <div>
    <n-modal
      class="go-view-pro-ai"
      title="GoViewPro AI"
      v-model:show="showModal"
      preset="card"
      size="small"
      transform-origin="center"
      style="width: 700px"
    >
      <n-divider style="margin: 4px 0 10px" />
      <n-flex>
        <n-text>点击前往查看👉：</n-text>
        <n-a href="https://ai.goviewlink.com/saas/" target="_blank">https://ai.goviewlink.com/saas/</n-a>
      </n-flex>
      <n-a href="https://ai.goviewlink.com/saas/" target="_blank"> </n-a>
      <n-image class="go-view-pro-img" width="668" :src="aiImage" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import aiImage from '@/assets/images/tips/go-view-pro-ai.png'

const showModal = ref(true)
</script>

<style scoped lang="scss">
.go-view-pro-ai {
  .go-view-pro-img {
    display: block;
    width: 100%;
    border-radius: 8px;
    margin-top: 10px;
  }
}
</style>
