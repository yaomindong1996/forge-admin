<template>
  <!-- 横条独立配置，不要求关闭文字与背景的自动配色。 -->
  <section class="indicator-control" aria-label="顶部菜单选中横条">
    <div class="indicator-heading">
      <span>顶部菜单选中横条</span>
      <!-- 不用 label 包 n-switch，避免点击被原生 label 吞掉导致“没反应” -->
      <div class="indicator-follow" @click.stop>
        <span>跟随品牌主色</span>
        <n-switch
          :value="followPrimary"
          aria-label="横条跟随品牌主色"
          @update:value="setFollowPrimary"
        />
      </div>
    </div>
    <div role="group" aria-label="顶部菜单选中横条颜色" @click.stop>
      <n-color-picker
        :value="color"
        :disabled="followPrimary"
        :show-alpha="false"
        :modes="['hex']"
        @update:value="setColor"
      />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { resolveTopMenuActiveBarColor, solidColor } from '@/utils/navigation-theme'

const props = defineProps({ dark: Boolean })
const model = defineModel({ type: Object, required: true })
const group = computed(() => props.dark ? 'topMenuDark' : 'topMenu')
const headerGroup = computed(() => props.dark ? 'headerDark' : 'header')
const followPrimary = computed(() => !String(model.value?.[group.value]?.activeBarColor || '').trim())
const color = computed(() => resolveTopMenuActiveBarColor(
  model.value?.[group.value]?.activeBarColor,
  model.value?.primaryColor,
  model.value?.[headerGroup.value]?.backgroundColor,
))

function setColor(value) {
  const key = group.value
  const nextBar = value == null ? '' : String(value).trim()
  model.value = {
    ...model.value,
    [key]: {
      ...(model.value?.[key] || {}),
      activeBarColor: nextBar,
    },
  }
}
function setFollowPrimary(enabled) {
  // 空值保留“跟随”语义，后续改主色或进入手动模式不会冻结为旧颜色。
  setColor(enabled ? '' : color.value)
}
</script>

<style scoped>
.indicator-control {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
  padding-block: 12px;
  border-block: 1px solid var(--border-light);
  color: var(--text-secondary);
  font-size: 12px;
}
.indicator-heading,
.indicator-follow {
  display: flex;
  align-items: center;
  gap: 8px;
}
.indicator-heading {
  justify-content: space-between;
  flex-wrap: wrap;
}
.indicator-follow {
  position: relative;
  z-index: 2;
}
</style>
