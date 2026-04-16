<template>
  <div class="go-aside-footer">
    <n-divider class="go-mt-0"></n-divider>
  </div>
</template>
<script setup lang="ts">
const props = defineProps({
  collapsed: Boolean
})
</script>

<style lang="scss" scoped>
@include go('aside-footer') {
  padding-bottom: 20px;
}

@include dark() {
  @include go('aside-footer') {
  }
}
</style>