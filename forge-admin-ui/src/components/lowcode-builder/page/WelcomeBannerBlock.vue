<template>
  <div
    class="welcome-banner"
    :class="[`tone-${tone}`, { 'is-compact': compact, 'has-cta': showCta && ctaText }]"
    :style="bannerStyle"
  >
    <div class="wb-main">
      <div v-if="showAvatar" class="wb-avatar" aria-hidden="true">
        <span>{{ avatarText }}</span>
      </div>
      <div class="wb-copy">
        <strong>{{ greetingText }}</strong>
        <p v-if="subtitleText">
          {{ subtitleText }}
        </p>
        <span v-if="showDate" class="wb-date">{{ dateText }}</span>
      </div>
    </div>
    <n-button
      v-if="showCta && ctaText"
      size="small"
      type="primary"
      secondary
      :disabled="!interactive"
      @click="onCta"
    >
      {{ ctaText }}
    </n-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { NButton } from 'naive-ui'
import { useUserStore } from '@/store'
import { resolveSurfaceBackgroundStyle } from './gridBlockRendererUtils'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '今天也要高效推进工作' },
  greetingPrefix: { type: String, default: '' },
  tone: { type: String, default: 'primary' },
  compact: { type: Boolean, default: false },
  showAvatar: { type: Boolean, default: true },
  showDate: { type: Boolean, default: true },
  showCta: { type: Boolean, default: true },
  ctaText: { type: String, default: '去办理' },
  ctaPath: { type: String, default: '/flow/todo' },
  backgroundMode: { type: String, default: 'gradient' },
  backgroundColor: { type: String, default: '#eff6ff' },
  gradientFrom: { type: String, default: '#eff6ff' },
  gradientTo: { type: String, default: '#ffffff' },
  gradientAngle: { type: [Number, String], default: 135 },
  backgroundImage: { type: String, default: '' },
  backgroundSize: { type: String, default: 'cover' },
  interactive: { type: Boolean, default: true },
})

const emit = defineEmits(['cta'])

const router = useRouter()
const userStore = useUserStore()

const nickname = computed(() => String(userStore.nickname || userStore.username || '同事').trim() || '同事')

const greetingText = computed(() => {
  if (String(props.title || '').trim())
    return String(props.title).trim()
  const hour = new Date().getHours()
  let prefix = String(props.greetingPrefix || '').trim()
  if (!prefix) {
    if (hour < 11)
      prefix = '早上好'
    else if (hour < 14)
      prefix = '中午好'
    else if (hour < 18)
      prefix = '下午好'
    else
      prefix = '晚上好'
  }
  return `${prefix}，${nickname.value}`
})

const subtitleText = computed(() => String(props.subtitle || '').trim())

const dateText = computed(() => {
  const now = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][now.getDay()]
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${m}-${d} 周${week}`
})

const avatarText = computed(() => nickname.value.slice(0, 1))

const bannerStyle = computed(() => resolveSurfaceBackgroundStyle({
  backgroundMode: props.backgroundMode,
  backgroundColor: props.backgroundColor,
  gradientFrom: props.gradientFrom,
  gradientTo: props.gradientTo,
  gradientAngle: props.gradientAngle,
  backgroundImage: props.backgroundImage,
  backgroundSize: props.backgroundSize,
}))

function onCta() {
  emit('cta', { path: props.ctaPath })
  if (!props.interactive || !props.ctaPath)
    return
  router.push(props.ctaPath)
}
</script>

<style scoped>
.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 88px;
  padding: 16px 18px;
  border: 1px solid color-mix(in srgb, var(--border-light) 80%, transparent);
  border-radius: 12px;
  box-sizing: border-box;
}

.welcome-banner.is-compact {
  min-height: 72px;
  padding: 12px 14px;
}

.wb-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.wb-avatar {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: color-mix(in srgb, #2563eb 14%, #fff);
  color: #1d4ed8;
  font-size: 16px;
  font-weight: 700;
}

.welcome-banner.tone-success .wb-avatar {
  background: color-mix(in srgb, #16a34a 14%, #fff);
  color: #15803d;
}

.welcome-banner.tone-warm .wb-avatar {
  background: color-mix(in srgb, #ea580c 14%, #fff);
  color: #c2410c;
}

.wb-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.wb-copy strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
}

.wb-copy p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
}

.wb-date {
  color: var(--text-tertiary);
  font-size: 11px;
}

.welcome-banner.is-compact .wb-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 14px;
}

.welcome-banner.is-compact .wb-copy strong {
  font-size: 14px;
}
</style>
