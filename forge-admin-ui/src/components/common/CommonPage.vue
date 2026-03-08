<template>
  <!-- 通用页面布局组件 - 提供标准的页面结构包含头部、内容区和底部 -->
  <main class="h-full flex-col flex-1 overflow-hidden bg-#f5f6fb dark:bg-#121212">
    <!-- 页面头部区域 - 粘性定位在顶部 -->
    <AppCard
      v-if="showHeader"
      class="sticky top-0 z-1 min-h-38 flex items-center justify-between px-8 py-2"
      border-b="1px solid light_border dark:dark_border"
    >
      <!-- 优先使用自定义头部插槽内容 -->
      <slot v-if="$slots.header" name="header" />
      <!-- 默认头部内容 -->
      <template v-else>
        <div class="flex items-center">
          <!-- 标题前缀区域 - 可插入返回按钮等内容 -->
          <slot name="title-prefix">
            <!-- 返回按钮 - 当back属性为true时显示 -->
            <template v-if="back">
              <div
                class="mr-12 flex cursor-pointer items-center text-14 opacity-60 transition-all-300 hover:opacity-40"
                @click="router.back()"
              >
                <i class="i-material-symbols:arrow-left-alt text-16" />
                <span class="ml-2 text-14 text-#333333">返回</span>
              </div>
            </template>
          </slot>

          <!-- 标题装饰条 - 当存在标题时显示 -->
          <div class="mr-8 h-14 w-3 rounded-l-1 bg-primary" v-if="title ?? route.meta?.title"/>
          <!-- 页面标题 - 优先使用props传入的title，其次使用路由meta中的title -->
          <h2 class="text-14 font-normal" v-if="title ?? route.meta?.title">
            {{ title ?? route.meta?.title }}
          </h2>
          <!-- 标题后缀区域 - 可插入额外内容 -->
          <slot name="title-suffix" />
        </div>
        <!-- 操作按钮区域 - 可插入各种操作按钮 -->
        <slot name="action" />
      </template>
    </AppCard>

    <!-- 页面主要内容区域 - 可滚动内容区 -->
    <AppCard class="cus-scroll flex-1 rounded-t-0 p-8 pt-4">
      <slot />
    </AppCard>

    <!-- 页面底部区域 -->
    <slot name="footer">
      <!-- 默认底部 - 当showFooter为true时显示TheFooter组件 -->
      <AppCard v-if="showFooter" class="flex-shrink-0 py-8">
        <TheFooter />
      </AppCard>
    </slot>
  </main>
</template>

<script setup>
/**
 * 通用页面布局组件
 * @description 提供标准的页面结构，包含可配置的头部、内容区和底部
 * @props {boolean} back - 是否显示返回按钮，默认false
 * @props {boolean} showFooter - 是否显示底部区域，默认false
 * @props {boolean} showHeader - 是否显示头部区域，默认false
 * @props {string} title - 页面标题，优先级高于路由meta中的title
 */

// 组件属性定义
defineProps({
  // 是否显示返回按钮
  back: {
    type: Boolean,
    default: false,
  },
  // 是否显示底部区域
  showFooter: {
    type: Boolean,
    default: false,
  },
  // 是否显示头部区域
  showHeader: {
    type: Boolean,
    default: false,
  },
  // 页面标题 - 优先级高于路由meta中的title
  title: {
    type: String,
    default: undefined,
  },
})

// 获取当前路由信息 - 用于获取路由meta中的标题
const route = useRoute()
// 获取路由实例 - 用于返回按钮的点击事件
const router = useRouter()
</script>
