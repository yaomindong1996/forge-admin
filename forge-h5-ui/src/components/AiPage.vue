<template>
  <view class="page-detail" :style="{ background: backgroundColor }">
    <uni-nav-bar 
      :left-arrow="leftArrow" 
      @clickLeft="onClickLeft" 
      @clickRight="onClickRight"
      :fixed="true"
      :border="false"
      v-if="isNavBar">
      <template #default>
        <view style="display: flex;align-items: center">
          <slot name="title-left"></slot>
          <text>{{title}}</text>
        </view>
      </template>
      <template #right>
        <slot name="nav-bar-right"></slot>
      </template>
    </uni-nav-bar>
    
    <view class="page-detail_top" :style="{ padding: marginTop ? '0px' : '10px' }" v-if="$slots.top">
      <slot name="top"></slot>
    </view>
    
    <scroll-view
      class="page-detail_box"
      :scroll-y="true"
      @scrolltolower="scrollToLower"
      :lower-threshold="10"
      :style="{
        height: scrollHeight ? scrollHeight : ($slots.footer && $slots.top) ? `calc(100vh - 88px - 44px)` : (($slots.footer || $slots.top) ? `calc(100vh - 88px)` : (!isNavBar && !$slots.footer) ? '100vh' : `calc(100vh - 44px)`),
        marginTop: marginTop ? marginTop : ($slots.top ? '88px' : '0px')
      }">
      <slot></slot>
    </scroll-view>
    
    <view class="page-detail_footer" v-if="$slots.footer">
      <slot name="footer"></slot>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: "",
  },
  backgroundColor: {
    type: String,
    default: "var(--page-bg, #f2f3f5)",
  },
  isNavBar: {
    type: Boolean,
    default: true,
  },
  pullRefresh: {
    type: Boolean,
    default: false,
  },
  isPage: {
    type: Boolean,
    default: false
  },
  isRouterBack: {
    type: Boolean,
    default: false
  },
  leftArrow: {
    type: Boolean,
    default: true
  },
  scrollHeight: {
    type: String,
    default: "",
  },
  marginTop: {
    type: String,
    default: "",
  },
})

const emit = defineEmits(['refresh', 'load'])

const prevScrollTop = ref(0)

const onClickLeft = () => {
  if (props.isRouterBack) {
    uni.navigateBack()
  } else {
    uni.navigateBack()
  }
}

const onClickRight = () => {
  // 可以在这里添加右侧按钮点击事件
}

const scrollToLower = () => {
  if (props.isPage) {
    emit("load")
  }
}

const handleScroll = (event) => {
  if (props.isPage) {
    const scrollTop = event.detail.scrollTop
    prevScrollTop.value = scrollTop
  }
}

// uni-app 原生下拉刷新回调
const onPullDownRefresh = () => {
  emit("refresh")
}

onMounted(() => {
  if (props.pullRefresh) {
    uni.setPullDownRefreshEnabled && uni.setPullDownRefreshEnabled(true)
  }
})

onUnmounted(() => {
  uni.setPullDownRefreshEnabled && uni.setPullDownRefreshEnabled(false)
})

defineExpose({
  onPullDownRefresh
})
</script>

<style lang="scss" scoped>
.page-detail {
  height: 100vh;
  overflow: hidden;
  color: var(--text-color);

  .page-detail_top {
    position: fixed;
    width: 100%;
    padding: 10px;
    box-sizing: border-box;
    z-index: 10;
  }

  .page-detail_footer {
    position: fixed;
    bottom: 0;
    width: 100%;
    border-top: 1px solid var(--forge-border, #c9cdd4);
    background: #fff;
    box-sizing: border-box;
    padding: 16px;
    display: flex;
    justify-content: space-around;
    align-items: center;
    min-height: 64px;
    height: auto;
    z-index: 10;
  }
}

@media (min-width: 1024px) {
  .page-detail_box > :deep(*) {
    max-width: var(--forge-page-max-width);
    margin-right: auto;
    margin-left: auto;
  }
}
</style>
