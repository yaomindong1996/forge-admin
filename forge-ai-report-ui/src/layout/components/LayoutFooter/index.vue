<template>
  <div class="go-footer">
    <slot>
    </slot>
  </div>
</template>

<script setup lang="ts">
</script>

<style lang="scss" scoped>
@include go(footer) {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  padding: 0 40px;
  height: $--footer-height;
}
</style>