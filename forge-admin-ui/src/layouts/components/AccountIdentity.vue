<template>
  <!-- 身份展示与动作分离，面板里不再套第二层个人菜单 -->
  <span class="account-identity">
    <n-avatar v-if="avatarSrc" round :size="28" :src="avatarSrc" @error="avatarSrc = ''" />
    <n-avatar v-else round :size="28" class="identity-fallback">{{ avatarText }}</n-avatar>
    <span v-if="name" class="account-name">{{ name }}</span>
  </span>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useUserStore } from '@/store'
import { resolveRenderableFileUrl } from '@/utils/file'

const userStore = useUserStore()
const name = computed(() => userStore.realName || userStore.staffInfo?.staffName || userStore.username)
const avatarText = computed(() => name.value?.charAt(0) || 'U')
const avatarSrc = ref('')
let generation = 0

watch(() => userStore.avatar, async (avatar) => {
  const current = ++generation
  avatarSrc.value = ''
  if (!avatar) {
    return
  }
  try {
    const url = await resolveRenderableFileUrl(avatar, undefined, true)
    // 换身份或卸载后的旧响应不能覆盖当前头像。
    if (current === generation) {
      avatarSrc.value = url
    }
  }
  catch {
    console.warn('账户头像加载失败，使用姓名标识')
  }
}, { immediate: true })
onBeforeUnmount(() => {
  generation++
})
</script>

<style scoped>
.account-identity {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.identity-fallback {
  background: var(--primary-500);
  font-size: 12px;
}
.account-identity :deep(.n-avatar) {
  flex-shrink: 0;
}
.account-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
</style>
