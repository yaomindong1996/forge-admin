<template>
  <!-- 左侧所有组件的展示列表 -->
  <content-box class="go-content-charts" :class="{ scoped: !getCharts }" title="组件" :depth="1" :backIcon="false">
    <template #icon>
      <n-icon size="14" :depth="2">
        <bar-chart-icon></bar-chart-icon>
      </n-icon>
    </template>

    <template #top-right>
      <template v-if="isAIMode">
        <n-tooltip placement="bottom" trigger="hover">
          <template #trigger>
            <n-button size="tiny" quaternary @click="clearAIChat">
              <template #icon>
                <n-icon size="14"><trash-icon /></n-icon>
              </template>
            </n-button>
          </template>
          清空对话
        </n-tooltip>
        <n-tooltip placement="bottom" trigger="hover">
          <template #trigger>
            <n-button size="tiny" quaternary @click="toggleAIMode">
              <template #icon>
                <n-icon size="14"><close-icon /></n-icon>
              </template>
            </n-button>
          </template>
          关闭AI助手
        </n-tooltip>
      </template>
      <charts-search v-else v-show="getCharts" :menuOptions="menuOptions"></charts-search>
    </template>

    <!-- 图表 -->
    <aside>
      <div class="menu-width-box">
        <!-- 左侧图标分类菜单（始终显示） -->
        <div class="menu-width">
          <n-menu
            v-model:value="selectValue"
            :options="menuOptions"
            :icon-size="16"
            :indent="18"
            @update:value="clickItemHandle"
          ></n-menu>
          <!-- AI 助手入口 -->
          <div class="ai-entry" :class="{ active: isAIMode }" @click="toggleAIMode">
            <n-icon size="18" :color="isAIMode ? '#51d6a9' : undefined"><sparkles-icon /></n-icon>
            <span>AI助手</span>
          </div>
        </div>

        <!-- 右侧内容区 -->
        <div class="menu-component-box">
          <!-- AI 模式 -->
          <AIChatPanel v-if="isAIMode" />
          <!-- 普通模式 -->
          <template v-else>
            <go-skeleton :load="!selectOptions" round text :repeat="2" style="width: 90%"></go-skeleton>
            <charts-option-content
              v-if="selectOptions"
              :selectOptions="selectOptions"
              :key="selectValue"
            ></charts-option-content>
          </template>
        </div>
      </div>
    </aside>
  </content-box>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ContentBox } from '../ContentBox/index'
import { ChartsOptionContent } from './components/ChartsOptionContent'
import { ChartsSearch } from './components/ChartsSearch'
import { useAsideHook } from './hooks/useAside.hook'
import { useAIStore } from '@/store/modules/aiStore/aiStore'
import { icon } from '@/plugins'
import AIChatPanel from '@/components/GoAI/AIChatPanel.vue'

const { TrashIcon, CloseIcon, SparklesIcon } = icon.ionicons5

const { getCharts, BarChartIcon, themeColor, selectOptions, selectValue, clickItemHandle, menuOptions } = useAsideHook()
const aiStore = useAIStore()
const isAIMode = ref(false)

function toggleAIMode() {
  isAIMode.value = !isAIMode.value
  aiStore.setAIPanelVisible(isAIMode.value)
}

function clearAIChat() {
  aiStore.clearChat()
}
</script>

<style lang="scss" scoped>
/* 整体宽度 */
$width: 330px;
/* 列表的宽度 */
$widthScoped: 65px;
/* 此高度与 ContentBox 组件关联 */
$topHeight: 40px;

@include go(content-charts) {
  width: $width;
  @extend .go-transition;
  &.scoped,
  .menu-width {
    width: $widthScoped;
  }
  .menu-width-box {
    display: flex;
    height: calc(100vh - #{$--header-height} - #{$topHeight});
    .menu-width {
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      @include fetch-bg-color('background-color2');

      :deep(.n-menu) {
        flex: 1;
      }

      .ai-entry {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 6px 4px;
        cursor: pointer;
        min-height: 48px;
        border-top: 1px solid;
        @include fetch-border-color('hover-border-color');
        span {
          font-size: 11px;
          margin-top: 2px;
          @include fetch-theme('color');
          opacity: 0.7;
        }
        &:hover, &.active {
          @include fetch-bg-color('background-color3');
          span { opacity: 1; }
        }
      }
    }
    .menu-component-box {
      flex-shrink: 0;
      width: $width - $widthScoped;
      overflow: hidden;
    }
  }
  @include deep() {
    .menu-width {
      .n-menu-item {
        height: auto !important;
        &.n-menu-item--selected {
          &::after {
            content: '';
            position: absolute;
            left: 2px;
            top: 0;
            height: 100%;
            width: 3px;
            background-color: v-bind('themeColor');
            border-top-right-radius: 3px;
            border-bottom-right-radius: 3px;
          }
        }
        .n-menu-item-content {
          display: flex;
          flex-direction: column;
          padding: 6px 12px !important;
          font-size: 14px !important;
        }
        .n-menu-item-content__icon {
          font-size: 18px !important;
          margin-right: 0 !important;
        }
      }
    }
  }
}
</style>
