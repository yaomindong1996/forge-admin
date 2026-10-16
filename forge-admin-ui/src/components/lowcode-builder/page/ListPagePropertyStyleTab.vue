<template>
            <div v-show="propertyPanelTab === 'style'" class="property-tab-content">
              <div class="property-search-anchor" data-property-search="样式 位置 尺寸 坐标 左 上 x y 宽度 高度 字号 行高 文字颜色 排版 背景 背景色 边框 边框色 边框粗细 粗细 borderWidth 圆角 阴影 透明度 opacity 内边距 外边距 padding margin 外观 装饰 颜色 自定义 style customStyle" />
              <div class="position-control">
                <div class="position-axis-grid">
                  <label class="position-number-field">
                    <span>左 X</span>
                    <n-input-number
                      :value="selectedBlockFrame.x"
                      :min="0"
                      size="small"
                      :show-button="false"
                      @update:value="patchBlockFrame(selectedBlock.id, { x: $event ?? 0 })"
                    />
                    <em>px</em>
                  </label>
                  <label class="position-number-field">
                    <span>上 Y</span>
                    <n-input-number
                      :value="selectedBlockFrame.y"
                      :min="0"
                      size="small"
                      :show-button="false"
                      @update:value="patchBlockFrame(selectedBlock.id, { y: $event ?? 0 })"
                    />
                    <em>px</em>
                  </label>
                </div>

                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>宽度</span>
                    <label v-if="selectedBlockWidthMode === 'fixed'" class="position-inline-number">
                      <n-input-number
                        :value="selectedBlockFixedWidth"
                        :min="24"
                        size="tiny"
                        :show-button="false"
                        @update:value="patchBlockFrame(selectedBlock.id, { width: $event ?? 24 })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="designer-segmented-control width-mode-control">
                    <button
                      type="button"
                      :class="{ active: selectedBlockWidthMode === 'auto' }"
                      @click="setBlockWidthMode(selectedBlock.id, 'auto')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <RemoveOutline />
                      </n-icon>
                      <span>默认宽度</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockWidthMode === 'full' }"
                      @click="setBlockWidthMode(selectedBlock.id, 'full')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <SwapHorizontalOutline />
                      </n-icon>
                      <span>填充容器</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockWidthMode === 'fixed' }"
                      @click="setBlockWidthMode(selectedBlock.id, 'fixed')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <ResizeOutline />
                      </n-icon>
                      <span>固定宽度</span>
                    </button>
                  </div>
                </div>

                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>高度</span>
                    <label v-if="selectedBlockHeightMode === 'fixed'" class="position-inline-number">
                      <n-input-number
                        :value="selectedBlockFrame.height"
                        :min="24"
                        size="tiny"
                        :show-button="false"
                        @update:value="patchBlockFrame(selectedBlock.id, { height: $event ?? 24 })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="designer-segmented-control height-mode-control">
                    <button
                      type="button"
                      :class="{ active: selectedBlockHeightMode === 'fixed' }"
                      @click="setBlockHeightMode(selectedBlock.id, 'fixed')"
                    >
                      <n-icon class="designer-segmented-icon vertical-icon">
                        <RemoveOutline />
                      </n-icon>
                      <span>固定高度</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockHeightMode === 'auto' }"
                      @click="setBlockHeightMode(selectedBlock.id, 'auto')"
                    >
                      <n-icon class="designer-segmented-icon">
                        <ResizeOutline />
                      </n-icon>
                      <span>适应内容</span>
                    </button>
                    <button
                      type="button"
                      :class="{ active: selectedBlockHeightMode === 'full' }"
                      @click="setBlockHeightMode(selectedBlock.id, 'full')"
                    >
                      <n-icon class="designer-segmented-icon vertical-icon">
                        <SwapHorizontalOutline />
                      </n-icon>
                      <span>填充容器</span>
                    </button>
                  </div>
                </div>

                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>对齐方式</span>
                  </div>
                  <div class="designer-segmented-control align-mode-control">
                    <button type="button" :class="{ active: selectedBlockContentAlign === 'left' }" @click="setBlockContentAlign(selectedBlock.id, 'left')">
                      <n-icon class="designer-segmented-icon">
                        <ReorderThreeOutline />
                      </n-icon>
                      <span>左对齐</span>
                    </button>
                    <button type="button" :class="{ active: selectedBlockContentAlign === 'center' }" @click="setBlockContentAlign(selectedBlock.id, 'center')">
                      <n-icon class="designer-segmented-icon">
                        <ReorderThreeOutline />
                      </n-icon>
                      <span>居中对齐</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- 与列表设计器对齐：自由布局 panelOnly 也开放外观/排版（此前整段隐藏导致改不了边框） -->
              <n-divider>文字排版</n-divider>
              <div class="style-grid">
                <n-form-item label="字号">
                  <n-input-number
                    :value="toNumberOrNull(selectedBlockStyle.fontSize) ?? 14"
                    :min="10"
                    :max="48"
                    :show-button="false"
                    @update:value="patchBlockStyle(selectedBlock.id, { fontSize: $event ?? '' })"
                  />
                </n-form-item>
                <n-form-item label="行高">
                  <n-input
                    :value="String(selectedBlockStyle.lineHeight ?? '')"
                    clearable
                    placeholder="1.5 / 22px"
                    @update:value="patchBlockStyle(selectedBlock.id, { lineHeight: $event || '' })"
                  />
                </n-form-item>
              </div>
              <n-form-item label="文字颜色">
                <div class="appearance-control list-appearance-control">
                  <div class="appearance-field">
                    <div class="appearance-input-shell">
                      <label class="appearance-swatch" :style="{ backgroundColor: selectedBlockTextColorPreview }" title="选择文字颜色">
                        <input
                          type="color"
                          :value="selectedBlockTextColorInput"
                          @input="updateSelectedBlockTextColor($event.target.value)"
                        >
                      </label>
                      <input
                        :value="selectedBlockTextColorHex"
                        class="appearance-hex-input"
                        placeholder="默认"
                        @input="updateSelectedBlockTextColor($event.target.value)"
                      >
                      <button type="button" class="appearance-clear-btn" @click="patchBlockStyle(selectedBlock.id, { color: '' })">
                        默认
                      </button>
                    </div>
                  </div>
                </div>
              </n-form-item>

              <n-divider>外观样式</n-divider>
              <n-form-item label="临界值 px（最小 / 最大）">
                <div class="style-grid four">
                  <n-input-number
                    :value="toNumberOrNull(selectedBlockStyle.minWidth)"
                    :min="0"
                    :show-button="false"
                    placeholder="最小宽"
                    clearable
                    @update:value="patchBlockStyle(selectedBlock.id, { minWidth: $event ?? '' })"
                  />
                  <n-input-number
                    :value="toNumberOrNull(selectedBlockStyle.maxWidth)"
                    :min="0"
                    :show-button="false"
                    placeholder="最大宽"
                    clearable
                    @update:value="patchBlockStyle(selectedBlock.id, { maxWidth: $event ?? '' })"
                  />
                  <n-input-number
                    :value="toNumberOrNull(selectedBlockStyle.minHeight)"
                    :min="0"
                    :show-button="false"
                    placeholder="最小高"
                    clearable
                    @update:value="patchBlockStyle(selectedBlock.id, { minHeight: $event ?? '' })"
                  />
                  <n-input-number
                    :value="toNumberOrNull(selectedBlockStyle.maxHeight)"
                    :min="0"
                    :show-button="false"
                    placeholder="最大高"
                    clearable
                    @update:value="patchBlockStyle(selectedBlock.id, { maxHeight: $event ?? '' })"
                  />
                </div>
              </n-form-item>
              <div class="appearance-control list-appearance-control">
                <div class="appearance-field">
                  <label>背景色</label>
                  <div class="appearance-input-shell">
                    <label class="appearance-swatch" :style="{ backgroundColor: selectedBlockBackgroundPreview }" title="选择背景色">
                      <input
                        type="color"
                        :value="selectedBlockBackgroundColorInput"
                        @input="updateSelectedBlockBackground($event.target.value)"
                      >
                    </label>
                    <input
                      :value="selectedBlockBackgroundHex"
                      class="appearance-hex-input"
                      placeholder="透明"
                      @input="updateSelectedBlockBackground($event.target.value)"
                    >
                    <span class="appearance-percent">{{ selectedBlockOpacityPercent }}%</span>
                  </div>
                </div>
                <div class="appearance-field">
                  <label>边框 (Border)</label>
                  <div class="appearance-input-shell">
                    <select
                      :value="selectedBlockStyle.borderStyle"
                      class="appearance-select"
                      @change="updateSelectedBlockBorderStyle($event.target.value)"
                    >
                      <option value="solid">
                        实线
                      </option>
                      <option value="dashed">
                        虚线
                      </option>
                      <option value="none">
                        无
                      </option>
                    </select>
                    <label class="appearance-swatch" :style="{ backgroundColor: selectedBlockBorderPreview }" title="选择边框颜色">
                      <input
                        type="color"
                        :value="selectedBlockBorderPreview"
                        @input="updateSelectedBlockBorderColor($event.target.value)"
                      >
                    </label>
                    <input
                      :value="selectedBlockBorderHex"
                      class="appearance-hex-input"
                      placeholder="E4E4E7"
                      @input="updateSelectedBlockBorderColor($event.target.value)"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label>边框粗细 (Border Width)</label>
                  <div class="appearance-radius-shell">
                    <span>W</span>
                    <input
                      :value="toNumberOrNull(selectedBlockStyle.borderWidth) ?? 1"
                      type="number"
                      min="0"
                      max="32"
                      :disabled="selectedBlockStyle.borderStyle === 'none'"
                      @input="updateSelectedBlockBorderWidth($event.target.value)"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label>圆角 (Border Radius)</label>
                  <div class="appearance-radius-shell">
                    <span>R</span>
                    <input
                      :value="toNumberOrNull(selectedBlockStyle.borderRadius) ?? 0"
                      type="number"
                      min="0"
                      max="48"
                      @input="patchBlockStyle(selectedBlock.id, { borderRadius: toNumberOrNull($event.target.value) ?? 0 })"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label class="appearance-row-label">
                    <span>阴影 (Shadow)</span>
                    <select
                      :value="selectedBlockStyle.boxShadow"
                      class="appearance-plain-select"
                      @change="patchBlockStyle(selectedBlock.id, { boxShadow: $event.target.value || 'none' })"
                    >
                      <option
                        v-for="option in shadowOptions"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </option>
                    </select>
                  </label>
                </div>
                <div class="appearance-field">
                  <label>透明度 (Opacity)</label>
                  <div class="appearance-radius-shell">
                    <span>%</span>
                    <input
                      :value="selectedBlockOpacityPercent"
                      type="number"
                      min="20"
                      max="100"
                      step="5"
                      @input="updateSelectedBlockOpacity($event.target.value)"
                    >
                  </div>
                </div>
              </div>
              <n-form-item label="内边距 / 外边距">
                <div class="style-grid">
                  <n-input
                    :value="String(selectedBlockStyle.padding ?? 0)"
                    size="small"
                    placeholder="内边距，如 12 或 8px 12px"
                    @update:value="patchBlockStyle(selectedBlock.id, { padding: normalizeSpacingValue($event) })"
                  />
                  <n-input
                    :value="String(selectedBlockStyle.margin ?? 0)"
                    size="small"
                    placeholder="外边距，如 12 或 8px 12px"
                    @update:value="patchBlockStyle(selectedBlock.id, { margin: normalizeSpacingValue($event) })"
                  />
                </div>
              </n-form-item>
              <n-form-item label="自定义 style">
                <n-input
                  :value="selectedBlockStyle.customStyle"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：backdrop-filter: blur(8px);"
                  @update:value="patchBlockStyle(selectedBlock.id, { customStyle: $event })"
                />
              </n-form-item>
            </div>

</template>

<script>
import { useListPageDesignerApi } from './listPageDesignerContext'
import { listPageDesignerLocalComponents } from './listPageDesignerLocalComponents'

export default {
  components: { ...listPageDesignerLocalComponents },
  name: 'ListPagePropertyStyleTab',
  setup() {
    return useListPageDesignerApi()
  },
}
</script>

<style scoped src="./list-page-grid-designer-shell.css"></style>
<style scoped src="./list-page-grid-designer-panels.css"></style>
