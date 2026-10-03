<template>
  <aside class="login-brand">
    <!-- 系统品牌沿用后端配置 -->
    <div class="brand-lockup">
      <img :src="logo" :alt="systemName" @error="$emit('logoError')">
      <div><strong>{{ systemName }}</strong><span>企业级中后台管理平台</span></div>
    </div>
    <NCarousel
      v-model:current-index="currentIndex"
      :autoplay="!reducedMotion"
      :interval="6000"
      :show-dots="false"
      class="brand-carousel"
    >
      <section v-for="slide in slides" :key="slide.key" class="brand-slide">
        <img :src="slide.image" :alt="slide.title" class="brand-artwork">
        <h2>{{ slide.title }}</h2>
        <p>{{ slide.description }}</p>
        <div class="brand-tags">
          <span v-for="tag in slide.tags" :key="tag">{{ tag }}</span>
        </div>
      </section>
    </NCarousel>
    <!-- 控件独立占位，不随轮播内容裁切，也不被右侧表单挤出首屏 -->
    <div class="brand-controls">
      <button class="carousel-arrow" type="button" aria-label="上一张轮播图" @click="changeSlide(-1)">
        <NIcon :component="ChevronBackOutline" />
      </button>
      <div class="brand-dots" aria-label="登录功能轮播">
        <button
          v-for="(slide, index) in slides"
          :key="slide.key"
          type="button"
          :aria-label="slide.title"
          :aria-current="currentIndex === index ? 'true' : undefined"
          :class="{ active: currentIndex === index }"
          @click="currentIndex = index"
        />
      </div>
      <button class="carousel-arrow" type="button" aria-label="下一张轮播图" @click="changeSlide(1)">
        <NIcon :component="ChevronForwardOutline" />
      </button>
    </div>
    <div class="brand-footer">
      <span>从业务搭建到流程协作</span><span>统一管理 · 高效交付</span>
    </div>
  </aside>
</template>

<script setup>
import { ChevronBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import { useMediaQuery } from '@vueuse/core'
import { NCarousel, NIcon } from 'naive-ui'
import { ref } from 'vue'
import lowcodeImage from '@/assets/images/login-lowcode-v2.webp'
import securityImage from '@/assets/images/login-security-v2.webp'
import workflowImage from '@/assets/images/login-workflow-v2.webp'

defineProps({ logo: { type: String, required: true }, systemName: { type: String, required: true } })
defineEmits(['logoError'])
const reducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)')
const currentIndex = ref(0)
const slides = [
  {
    key: 'security',
    image: securityImage,
    title: '安全可控，统一治理',
    description: '连接账户、租户与数据权限，让每一次业务访问都有清晰边界。',
    tags: ['多租户隔离', '精细化权限', '操作审计'],
  },
  {
    key: 'lowcode',
    image: lowcodeImage,
    title: '业务搭建，轻松起步',
    description: '从业务对象到页面配置，让表单、列表与数据在同一平台协同。',
    tags: ['低代码搭建', '数据集成', '组件复用'],
  },
  {
    key: 'workflow',
    image: workflowImage,
    title: '流程协作，高效流转',
    description: '将业务数据与审批流程连接起来，让任务处理更有序、更直观。',
    tags: ['流程编排', '动态表单', '任务协作'],
  },
]

function changeSlide(step) {
  currentIndex.value = (currentIndex.value + step + slides.length) % slides.length
}
</script>

<style scoped>
.login-brand {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  gap: 20px;
  min-width: 0;
  min-height: 0;
  height: 100dvh;
  box-sizing: border-box;
  padding: 40px 48px;
  color: #fff;
  background: linear-gradient(145deg, #1d4ed8 0%, #2563eb 52%, #1e40af 100%);
}
.brand-lockup {
  display: flex;
  align-items: center;
  gap: 12px;
}
.brand-lockup img {
  width: 38px;
  height: 38px;
  object-fit: contain;
}
.brand-lockup strong {
  display: block;
  font-size: 20px;
  font-weight: 600;
}
.brand-lockup span {
  color: #dbeafe;
  font-size: 11px;
  letter-spacing: 1px;
}
.brand-carousel {
  min-height: 0;
}
.brand-carousel :deep(.n-carousel__slides) {
  height: 100%;
}
.brand-slide {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.brand-artwork {
  width: min(100%, 560px);
  height: clamp(120px, 30dvh, 320px);
  min-height: 0;
  object-fit: contain;
}
.brand-slide h2 {
  margin: 20px 0 12px;
  font-size: clamp(24px, 2.2vw, 32px);
  font-weight: 600;
  letter-spacing: 1px;
}
.brand-slide p {
  max-width: 400px;
  margin: 0;
  color: #dbeafe;
  text-align: center;
  font-size: 14px;
  line-height: 1.8;
}
.brand-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin: 20px 0 0;
}
.brand-tags span {
  padding: 5px 12px;
  border: 1px solid rgb(255 255 255 / 20%);
  border-radius: 6px;
  font-size: 12px;
}
.brand-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}
.brand-dots {
  display: flex;
  align-items: center;
}
.brand-dots button {
  width: 36px;
  height: 40px;
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
}
.brand-dots button::after {
  content: '';
  display: block;
  width: 8px;
  height: 6px;
  margin: auto;
  border-radius: 3px;
  background: rgb(255 255 255 / 65%);
}
.brand-dots button.active::after {
  width: 28px;
  background: #fff;
}
.carousel-arrow {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 40%);
  border-radius: 6px;
  background: transparent;
  color: #fff;
  cursor: pointer;
}
.carousel-arrow:hover {
  background: rgb(255 255 255 / 12%);
}
.brand-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #bfdbfe;
  font-size: 11px;
}
button:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 2px;
}
@media (max-width: 959px) {
  .login-brand {
    display: none;
  }
}
@media (min-width: 960px) and (max-height: 720px) {
  .login-brand {
    padding: 24px 32px;
    gap: 16px;
  }
  .brand-artwork {
    height: clamp(120px, 26dvh, 220px);
  }
}
@media (min-width: 960px) and (max-height: 600px) {
  .login-brand {
    padding: 20px 32px;
    gap: 12px;
  }
  .brand-artwork {
    height: clamp(100px, 22dvh, 160px);
  }
  .brand-slide h2 {
    margin: 12px 0 8px;
    font-size: 24px;
  }
  .brand-slide p {
    font-size: 13px;
  }
  .brand-tags {
    margin-top: 12px;
  }
}
@media (min-width: 960px) and (max-height: 520px) {
  .brand-tags {
    display: none;
  }
}
</style>
