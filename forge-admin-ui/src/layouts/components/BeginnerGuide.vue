<template>
  <n-tooltip trigger="hover">
    <template #trigger>
      <button
        class="chrome-icon-button" type="button" title="操作指引" aria-label="操作指引"
        @click="show = true"
      >
        <i class="i-lucide:book-open" />
        <span v-if="withLabel">操作指引</span>
      </button>
    </template>
    操作指引
  </n-tooltip>

  <Vue3IntroStep
    ref="myIntroStep"
    v-model:show="show"
    :config="config"
  >
    <template #prev="{ tipItem, index }">
      <NButton class="mr-12" type="primary" color="#fff" text-color="#fff" ghost round size="small" @click="prev(tipItem, index)">
        上一步
      </NButton>
    </template>
    <template #next="{ tipItem }">
      <NButton class="mr-12" type="primary" color="#fff" text-color="#fff" ghost round size="small" @click="next(tipItem)">
        下一步
      </NButton>
    </template>

    <template #skip>
      <NButton type="primary" color="#fff" text-color="#fff" ghost round size="small" @click="skip">
        跳过
      </NButton>
    </template>

    <template #done>
      <NButton type="primary" color="#fff" text-color="#fff" ghost round size="small" @click="done">
        完成
      </NButton>
    </template>
  </Vue3IntroStep>
</template>

<script setup>
import Vue3IntroStep from 'vue3-intro-step'

defineProps({ withLabel: Boolean })
const myIntroStep = shallowRef(null)
const show = shallowRef(false)
const config = {
  backgroundOpacity: 0.8,
  titleStyle: {
    textAlign: 'left',
    fontSize: '18px',
  },
  contentStyle: {
    textAlign: 'left',
    fontSize: 'var(--font-size-base)',
  },
  tips: [
    {
      el: '#toggleTheme',
      tipPosition: 'bottom',
      title: '切换系统主题',
      content: '在浅色和深色模式之间切换',
    },
    {
      el: '#user-dropdown',
      tipPosition: 'bottom',
      title: '个人中心',
      content: '查看个人资料和退出系统',
    },
    {
      el: '#layout-setting',
      tipPosition: 'left',
      title: '调整系统布局',
      content: '将系统布局调整为你喜欢的样子',
    },
  ],
}

function skip() {
  show.value = false
}

function done() {
  show.value = false
}

function next() {
  // tipItem当前的提示项信息
  // 调用vue3-intro-step的next方法 手动触发下一步
  myIntroStep.value.next()
}
function prev() {
  // 调用vue3-intro-step的prev方法 手动触发上一步
  myIntroStep.value.prev()
}
</script>
