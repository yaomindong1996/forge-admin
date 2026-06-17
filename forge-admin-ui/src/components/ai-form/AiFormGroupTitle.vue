<template>
  <div
    class="ai-form-group-title"
    :style="style"
    :class="className"
  >
    <span class="ai-form-group-title__text">{{ label || title || '分组标题' }}</span>
  </div>
</template>

<script setup>
defineProps({
  label: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '',
  },
  style: {
    type: [String, Object, Array],
    default: undefined,
  },
  className: {
    type: [String, Object, Array],
    default: undefined,
  },
})
</script>

<style scoped lang="scss">
.ai-form-group-title {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 0 0 0 10px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  position: relative;
  &:before{
    position: absolute;
    top: 50%;
    left: 0;
    transform: translateY(-50%);
    content: "";
    width: 3px;
    height: 18px;
    background: #2563eb;
    border-radius: 8px;
  }
}

.ai-form-group-title__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
