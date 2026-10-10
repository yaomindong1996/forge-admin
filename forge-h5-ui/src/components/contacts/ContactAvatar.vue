<template>
  <view class="contact-avatar" :class="`contact-avatar--${size}`">
    <AiAuthImage v-if="src && !failed" class="contact-avatar__image" :src="src" mode="aspectFill" @error="failed = true" />
    <text v-else class="contact-avatar__text">{{ initials }}</text>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import AiAuthImage from '@/components/AiAuthImage.vue'
import { contactInitials } from '@/utils/contacts'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: String, default: 'md', validator: value => ['md', 'lg'].includes(value) },
})

const failed = ref(false)
const initials = computed(() => contactInitials(props.name))

watch(() => props.src, () => { failed.value = false })
</script>

<style lang="scss" scoped>
.contact-avatar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: var(--forge-color-primary);
}

.contact-avatar--md {
  width: 40px;
  height: 40px;
}

.contact-avatar--lg {
  width: 64px;
  height: 64px;
}

.contact-avatar__image {
  width: 100%;
  height: 100%;
}

.contact-avatar__text {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}

.contact-avatar--lg .contact-avatar__text {
  font-size: 20px;
}
</style>
