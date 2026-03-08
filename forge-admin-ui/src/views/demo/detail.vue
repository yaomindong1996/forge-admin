<template>
  <CommonPage back show-header title="详情页面">
    <n-card title="页面内容">
      <p>这是详情页面的内容</p>
    </n-card>
  </CommonPage>
</template>

<script setup>
defineOptions({
  name: 'DetailPage',
  title: '详情页面'
})

import { CommonPage } from "@/components/index.js";
</script>