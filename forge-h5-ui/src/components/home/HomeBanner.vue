<template>
  <view class="home-banner">
    <swiper
      class="home-banner__swiper"
      :autoplay="banners.length > 1"
      :interval="4000"
      :duration="400"
      circular
      @change="current = $event.detail.current"
    >
      <swiper-item v-for="item in banners" :key="item.key">
        <view class="home-banner__slide" @click="item.open()">
          <image class="home-banner__image" :src="item.image" mode="aspectFill" />
          <view class="home-banner__copy">
            <text class="home-banner__title">{{ item.title }}</text>
            <text class="home-banner__desc">{{ item.desc }}</text>
            <view class="home-banner__action" :style="{ color: item.accent }">
              <text>{{ item.action }}</text>
              <AiIcon icon="/static/icons/ai-icon/chevron-right.svg" :color="item.accent" size="sm" />
            </view>
          </view>
        </view>
      </swiper-item>
    </swiper>
    <!-- 指示器放在文字区下方，避免压住右侧插画主体 -->
    <view v-if="banners.length > 1" class="home-banner__dots">
      <view
        v-for="(item, index) in banners"
        :key="item.key"
        class="home-banner__dot"
        :class="{ 'is-active': index === current }"
      />
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import AiIcon from '@/components/AiIcon.vue'
import approvalBanner from '@/static/banners/approval.jpg'
import teamBanner from '@/static/banners/team.jpg'

const banners = [
  {
    key: 'approval',
    image: approvalBanner,
    title: '移动审批，随时处理',
    desc: '待办、发起、催办一站完成',
    action: '发起审批',
    accent: '#0066ff',
    open: () => uni.navigateTo({ url: '/pages/approval/start' }),
  },
  {
    key: 'team',
    image: teamBanner,
    title: '找同事、看通知',
    desc: '通讯录与公告随手可查',
    action: '查看通讯录',
    accent: '#0f9d8a',
    open: () => uni.switchTab({ url: '/pages/contacts/index' }),
  },
]
const current = ref(0)
</script>

<style lang="scss" scoped>
/* swiper 需要确定高度，用 padding-top 撑出 2.2:1，兼容不支持 aspect-ratio 的小程序基础库 */
.home-banner {
  position: relative;
  overflow: hidden;
  height: 0;
  padding-top: 45.45%;
  border-radius: var(--forge-radius-card);
  background: var(--forge-surface-subtle, #f5f6f7);
}

.home-banner__swiper {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  height: 100%;
}

.home-banner__slide {
  position: relative;
  width: 100%;
  height: 100%;
}

.home-banner__image {
  display: block;
  width: 100%;
  height: 100%;
}

.home-banner__copy {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  display: flex;
  width: 58%;
  flex-direction: column;
  justify-content: center;
  padding: 0 0 12px 18px;
  box-sizing: border-box;
}

.home-banner__title {
  color: var(--forge-text-primary, #171a1d);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
}

.home-banner__desc {
  margin-top: 4px;
  color: var(--forge-text-secondary, #747677);
  font-size: 12px;
  line-height: 1.5;
}

.home-banner__action {
  display: inline-flex;
  align-self: flex-start;
  align-items: center;
  gap: 2px;
  height: 26px;
  margin-top: 12px;
  padding: 0 8px 0 12px;
  border-radius: 13px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.85);
}

.home-banner__action :deep(.ai-icon) {
  width: 14px;
  height: 14px;
}

.home-banner__dots {
  position: absolute;
  bottom: 10px;
  left: 18px;
  display: flex;
  gap: 4px;
}

.home-banner__dot {
  width: 6px;
  height: 3px;
  border-radius: 2px;
  background: rgba(23, 26, 29, 0.18);
  transition: width .2s ease, background-color .2s ease;
}

.home-banner__dot.is-active {
  width: 14px;
  background: var(--forge-text-primary, #171a1d);
}
</style>
