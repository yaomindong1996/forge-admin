<script setup>
/**
 * 布局预设线框：只画页面分区轮廓，不画图表乱线。
 */
defineProps({
  kind: {
    type: String,
    default: 'ops',
    validator: v => ['ops', 'entry', 'analytics', 'blank'].includes(v),
  },
})
</script>

<template>
  <div class="preset-thumb" :class="`kind-${kind}`" aria-hidden="true">
    <!-- 空白画布 -->
    <template v-if="kind === 'blank'">
      <div class="preset-frame blank">
        <span class="preset-blank-grid" />
        <em>空白</em>
      </div>
    </template>

    <!-- 运营总览：指标 → 入口 → 左右双图 -->
    <template v-else-if="kind === 'ops'">
      <div class="preset-frame">
        <div class="preset-row cols-4">
          <i /><i /><i /><i />
        </div>
        <div class="preset-row cols-3">
          <i /><i /><i />
        </div>
        <div class="preset-row cols-2 grow">
          <div class="preset-panel">
            <span class="preset-bars" />
          </div>
          <div class="preset-panel">
            <span class="preset-wave" />
          </div>
        </div>
      </div>
    </template>

    <!-- 入口工作台：标题 → 入口 → 列表 -->
    <template v-else-if="kind === 'entry'">
      <div class="preset-frame">
        <div class="preset-stack">
          <b class="w-40" />
          <b class="w-70 soft" />
        </div>
        <div class="preset-row cols-4">
          <i /><i /><i /><i />
        </div>
        <div class="preset-panel grow list">
          <em /><em /><em /><em />
        </div>
      </div>
    </template>

    <!-- 分析看板：指标 → 通栏 → 主图+侧栏 -->
    <template v-else>
      <div class="preset-frame">
        <div class="preset-row cols-4">
          <i /><i /><i /><i />
        </div>
        <div class="preset-panel mid">
          <span class="preset-wave" />
        </div>
        <div class="preset-row cols-split grow">
          <div class="preset-panel">
            <span class="preset-bars" />
          </div>
          <div class="preset-panel list">
            <em /><em /><em />
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.preset-thumb {
  width: 100%;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.preset-frame {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 5px;
  height: 100%;
  padding: 8px;
  box-sizing: border-box;
}

.preset-frame.blank {
  place-items: center;
  grid-template-rows: 1fr;
  background:
    radial-gradient(circle at 1px 1px, #cbd5e1 1px, transparent 1.5px) 0 0 / 10px 10px;
}

.preset-blank-grid {
  display: none;
}

.preset-frame.blank em {
  padding: 2px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, #fff 88%, transparent);
  color: #64748b;
  font-size: 10px;
  font-style: normal;
  letter-spacing: 0.04em;
}

.preset-row {
  display: grid;
  gap: 4px;
  min-height: 0;
}

.preset-row.cols-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  height: 10px;
}

.preset-row.cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  height: 12px;
}

.preset-row.cols-2 {
  grid-template-columns: 1fr 1fr;
}

.preset-row.cols-split {
  grid-template-columns: 1.6fr 1fr;
}

.preset-row.grow,
.preset-panel.grow {
  min-height: 0;
}

.preset-row i {
  display: block;
  border-radius: 2px;
  background: #93c5fd;
  border: 1px solid #60a5fa;
}

.preset-row.cols-3 i {
  background: #bfdbfe;
  border-color: #93c5fd;
}

.preset-stack {
  display: grid;
  gap: 3px;
}

.preset-stack b {
  display: block;
  height: 7px;
  border-radius: 2px;
  background: #64748b;
}

.preset-stack b.soft {
  height: 6px;
  background: #bae6fd;
}

.preset-stack b.w-40 {
  width: 42%;
}

.preset-stack b.w-70 {
  width: 72%;
}

.preset-panel {
  position: relative;
  min-height: 0;
  border-radius: 3px;
  background: #fff;
  border: 1px solid #cbd5e1;
  overflow: hidden;
}

.preset-panel.mid {
  height: 14px;
}

.preset-panel.list {
  display: grid;
  align-content: center;
  gap: 3px;
  padding: 4px 5px;
}

.preset-panel.list em {
  display: block;
  height: 3px;
  border-radius: 1px;
  background: #e2e8f0;
  font-style: normal;
}

.preset-bars {
  position: absolute;
  inset: 4px 6px 3px;
  background:
    linear-gradient(#3b82f6, #3b82f6) 12% 100% / 14% 45% no-repeat,
    linear-gradient(#60a5fa, #60a5fa) 38% 100% / 14% 70% no-repeat,
    linear-gradient(#93c5fd, #93c5fd) 64% 100% / 14% 55% no-repeat,
    linear-gradient(#bfdbfe, #bfdbfe) 90% 100% / 14% 35% no-repeat;
}

.preset-wave {
  position: absolute;
  inset: 3px 5px;
  border-radius: 2px;
  background:
    linear-gradient(transparent 0 42%, #93c5fd 42% 58%, transparent 58%) 0 55% / 100% 2px no-repeat,
    linear-gradient(transparent 0 28%, #3b82f6 28% 42%, transparent 42%) 0 40% / 55% 2px no-repeat;
  opacity: 0.9;
}
</style>
