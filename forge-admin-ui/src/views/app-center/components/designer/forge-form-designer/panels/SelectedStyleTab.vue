<template>
<n-form label-placement="top" :show-feedback="false" class="property-form">
            <n-collapse :default-expanded-names="['position', 'layout', 'typography', 'appearance']" class="config-collapse style-config-collapse">
              <n-collapse-item name="position">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:aspect-ratio-outline" />
                    位置与尺寸
                  </span>
                </template>
                <section class="panel-item position-control">
                  <div class="position-axis-grid">
                    <label class="position-number-field">
                      <span>左 X</span>
                      <n-input-number
                        :value="selectedDesignerTranslate.x"
                        size="small"
                        :show-button="false"
                        @update:value="updateDesignerTranslate('x', $event)"
                      />
                      <em>px</em>
                    </label>
                    <label class="position-number-field">
                      <span>上 Y</span>
                      <n-input-number
                        :value="selectedDesignerTranslate.y"
                        size="small"
                        :show-button="false"
                        @update:value="updateDesignerTranslate('y', $event)"
                      />
                      <em>px</em>
                    </label>
                  </div>

                  <div class="position-rule">
                    <div class="position-rule-head">
                      <span>宽度</span>
                      <label v-if="(selectedDesignerStyle.widthMode || 'default') === 'default'" class="position-inline-number">
                        <n-input-number
                          :value="resolvePxNumber(selectedDesignerStyle.width, 0)"
                          size="tiny"
                          :min="0"
                          :show-button="false"
                          placeholder="自动"
                          @update:value="updateDesignerStyle({ width: valueToPx($event) })"
                        />
                        <em>px</em>
                      </label>
                    </div>
                    <div class="segmented-mini">
                      <button
                        type="button"
                        :class="{ active: (selectedDesignerStyle.widthMode || 'default') === 'default' }"
                        @click="updateWidthMode('default')"
                      >
                        默认宽度
                      </button>
                      <button
                        type="button"
                        :class="{ active: selectedDesignerStyle.widthMode === 'fill' }"
                        @click="updateWidthMode('fill')"
                      >
                        填充容器
                      </button>
                    </div>
                  </div>

                  <div class="position-rule">
                    <div class="position-rule-head">
                      <span>高度</span>
                      <label v-if="(selectedDesignerStyle.heightMode || 'default') === 'default'" class="position-inline-number">
                        <n-input-number
                          :value="resolvePxNumber(selectedDesignerStyle.height, 0)"
                          size="tiny"
                          :min="0"
                          :show-button="false"
                          placeholder="自动"
                          @update:value="updateDesignerStyle({ height: valueToPx($event) })"
                        />
                        <em>px</em>
                      </label>
                    </div>
                    <div class="segmented-mini three">
                      <button
                        type="button"
                        :class="{ active: (selectedDesignerStyle.heightMode || 'default') === 'default' }"
                        @click="updateHeightMode('default')"
                      >
                        默认高度
                      </button>
                      <button
                        type="button"
                        :class="{ active: selectedDesignerStyle.heightMode === 'fit' }"
                        @click="updateHeightMode('fit')"
                      >
                        适应内容
                      </button>
                      <button
                        type="button"
                        :class="{ active: selectedDesignerStyle.heightMode === 'fill' }"
                        @click="updateHeightMode('fill')"
                      >
                        填充容器
                      </button>
                    </div>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item name="layout">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:padding-outline" />
                    布局与边距
                  </span>
                </template>
                <section class="panel-item">
                  <n-form-item label="内边距">
                    <SpacingBoxControl
                      :value="selectedPaddingBox"
                      label="内边距"
                      :min="0"
                      :max="120"
                      @update:value="updateDesignerBoxSpacing('padding', $event)"
                    />
                  </n-form-item>
                  <n-form-item label="外边距">
                    <SpacingBoxControl
                      :value="selectedMarginBox"
                      label="外边距"
                      :min="-80"
                      :max="120"
                      @update:value="updateDesignerBoxSpacing('margin', $event)"
                    />
                  </n-form-item>
                </section>
              </n-collapse-item>

              <n-collapse-item name="typography">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:text-fields" />
                    文字排版
                  </span>
                </template>
                <section class="panel-item">
                  <div class="crud-inline-grid">
                    <n-form-item label="字号">
                      <div class="appearance-metric-shell">
                        <span>px</span>
                        <input
                          :value="resolvePxNumber(selectedDesignerStyle.customStyle?.fontSize, 14)"
                          type="number"
                          min="10"
                          max="48"
                          @input="updateDesignerSpacing('fontSize', $event.target.value)"
                        >
                      </div>
                    </n-form-item>
                    <n-form-item label="行高">
                      <div class="appearance-metric-shell">
                        <span>LH</span>
                        <input
                          :value="selectedDesignerStyle.customStyle?.lineHeight || ''"
                          type="text"
                          placeholder="1.5 / 22px"
                          @input="updateDesignerCustomStyle({ lineHeight: $event.target.value || undefined })"
                        >
                      </div>
                    </n-form-item>
                  </div>
                  <n-form-item label="文字颜色">
                    <div class="color-control">
                      <n-color-picker
                        :value="selectedDesignerStyle.customStyle?.color || null"
                        :show-alpha="true"
                        :modes="['hex']"
                        :swatches="colorSwatches"
                        @update:value="updateDesignerCustomStyle({ color: $event || undefined })"
                      />
                      <n-button size="small" quaternary @click="updateDesignerCustomStyle({ color: undefined })">
                        默认
                      </n-button>
                    </div>
                  </n-form-item>
                </section>
              </n-collapse-item>

              <n-collapse-item name="appearance">
                <template #header>
                  <span class="collapse-section-title">
                    <i class="i-material-symbols:palette-outline" />
                    外观与装饰
                  </span>
                </template>
                <section class="panel-item appearance-control">
                  <div class="appearance-field">
                    <label>背景色</label>
                    <div class="appearance-input-shell">
                      <label class="appearance-swatch" :style="{ backgroundColor: selectedAppearanceBackgroundPreview }" title="选择背景色">
                        <input
                          type="color"
                          :value="selectedAppearanceBackgroundColorInput"
                          @input="updateSelectedAppearanceBackground($event.target.value)"
                        >
                      </label>
                      <input
                        :value="selectedAppearanceBackgroundHex"
                        class="appearance-hex-input"
                        placeholder="透明"
                        @input="updateSelectedAppearanceBackground($event.target.value)"
                      >
                      <span class="appearance-percent">{{ selectedOpacityPercent }}%</span>
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label>边框 (Border)</label>
                    <div class="appearance-input-shell">
                      <select
                        :value="selectedDesignerStyle.borderStyle || 'solid'"
                        class="appearance-select"
                        @change="updateDesignerBorderStyle($event.target.value)"
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
                      <label class="appearance-swatch" :style="{ backgroundColor: selectedAppearanceBorderPreview }" title="选择边框颜色">
                        <input
                          type="color"
                          :value="selectedAppearanceBorderPreview"
                          @input="updateSelectedAppearanceBorder($event.target.value)"
                        >
                      </label>
                      <input
                        :value="selectedAppearanceBorderHex"
                        class="appearance-hex-input"
                        placeholder="E4E4E7"
                        @input="updateSelectedAppearanceBorder($event.target.value)"
                      >
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label>边框粗细 (Border Width)</label>
                    <div class="appearance-radius-shell">
                      <span>W</span>
                      <input
                        :value="resolvePxNumber(selectedDesignerStyle.borderWidth, 1)"
                        type="number"
                        min="0"
                        max="32"
                        :disabled="(selectedDesignerStyle.borderStyle || 'solid') === 'none'"
                        @input="updateDesignerBorderWidth($event.target.value)"
                      >
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label>圆角 (Border Radius)</label>
                    <div class="appearance-radius-shell">
                      <span>R</span>
                      <input
                        :value="resolvePxNumber(selectedDesignerStyle.borderRadius, 6)"
                        type="number"
                        min="0"
                        max="32"
                        @input="updateDesignerStyle({ borderRadius: `${$event.target.value || 6}px` })"
                      >
                    </div>
                  </div>
                  <div class="appearance-field">
                    <label class="appearance-row-label">
                      <span>阴影 (Shadow)</span>
                      <select
                        :value="selectedDesignerStyle.boxShadow || ''"
                        class="appearance-plain-select"
                        @change="updateDesignerStyle({ boxShadow: $event.target.value || undefined })"
                      >
                        <option
                          v-for="option in shadowOptions"
                          :key="option.value || 'none'"
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
                        :value="selectedOpacityPercent"
                        type="number"
                        min="20"
                        max="100"
                        step="5"
                        @input="updateSelectedAppearanceOpacity($event.target.value)"
                      >
                    </div>
                  </div>
                  <n-form-item label="CSS Style">
                    <n-input
                      :value="selectedDesignerStyle.customStyleText || stringifyStyle(selectedDesignerStyle.customStyle)"
                      type="textarea"
                      :autosize="{ minRows: 3, maxRows: 6 }"
                      placeholder="例如 color:#111; padding:12px;"
                      @update:value="updateComponentStyleText"
                    />
                  </n-form-item>
                </section>
              </n-collapse-item>
            </n-collapse>
          </n-form>
</template>

<script>
import { useForgePropertyPanelApi } from '../forgePropertyPanelContext'
import { forgePropertyPanelLocalComponents } from '../forgePropertyPanelLocalComponents'

export default {
  name: 'SelectedStyleTab',
  components: { ...forgePropertyPanelLocalComponents },
  setup() {
    return useForgePropertyPanelApi()
  },
}
</script>

<style scoped src="../forge-property-panel-shell.css"></style>
<style scoped src="../forge-property-panel-fields.css"></style>
