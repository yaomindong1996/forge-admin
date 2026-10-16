<template>
    <n-tabs v-model:value="formPropertyActiveTab" type="line" size="medium" animated class="property-tabs form-property-tabs">
      <n-tab-pane name="basic">
        <template #tab>
          <span class="property-tab-label">
            <n-icon><LayersOutline /></n-icon>
            表单属性
          </span>
        </template>
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <n-collapse v-model:expanded-names="formBasicExpandedNames" class="form-property-collapse">
            <n-collapse-item title="多表单管理" name="assets">
              <FormAssetsPanel />
            </n-collapse-item>

            <n-collapse-item title="主子表配置" name="subTables">
              <FormSubTablePanel />
            </n-collapse-item>

            <n-collapse-item title="表单项配置" name="layout">
              <FormLayoutPanel />
            </n-collapse-item>

            <n-collapse-item title="表单权限控制" name="permissions">
              <section class="panel-item form-permission-panel">
                <div class="compact-field">
                  <label>查看权限码</label>
                  <n-input
                    :value="formPermissionConfig.viewPermission || ''"
                    clearable
                    placeholder="例如: ai:business:customer:query"
                    size="small"
                    @update:value="updateFormPermission({ viewPermission: $event || '' })"
                  />
                </div>
                <div class="compact-field">
                  <label>编辑权限码</label>
                  <n-input
                    :value="formPermissionConfig.editPermission || ''"
                    clearable
                    placeholder="例如: ai:business:customer:edit"
                    size="small"
                    @update:value="updateFormPermission({ editPermission: $event || '' })"
                  />
                </div>
                <div class="field-permission-rules">
                  <div class="field-permission-head">
                    <span>字段权限覆盖</span>
                    <n-button size="tiny" text type="primary" @click="addFormFieldRule">
                      添加
                    </n-button>
                  </div>
                  <div v-for="(rule, idx) in formFieldRuleRows" :key="rule.id || idx" class="field-permission-card">
                    <n-select
                      :value="rule.field || ''"
                      :options="formFieldOptions"
                      filterable
                      clearable
                      placeholder="选择字段"
                      size="small"
                      @update:value="updateFormFieldRule(idx, { field: $event || '' })"
                    />
                    <div class="field-rule-switches">
                      <label>
                        <span>必填</span>
                        <n-switch size="small" :value="!!rule.required" @update:value="updateFormFieldRule(idx, { required: $event })" />
                      </label>
                      <label>
                        <span>只读</span>
                        <n-switch size="small" :value="!!rule.readonly" @update:value="updateFormFieldRule(idx, { readonly: $event })" />
                      </label>
                      <label>
                        <span>隐藏</span>
                        <n-switch size="small" :value="!!rule.hidden" @update:value="updateFormFieldRule(idx, { hidden: $event })" />
                      </label>
                    </div>
                    <div class="field-permission-footer">
                      <n-input
                        :value="rule.defaultValue ?? ''"
                        clearable
                        placeholder="默认值"
                        size="small"
                        @update:value="updateFormFieldRule(idx, { defaultValue: $event })"
                      />
                      <n-button size="tiny" quaternary type="error" @click="removeFormFieldRule(idx)">
                        删除
                      </n-button>
                    </div>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="本地草稿与断网提交" name="offline">
              <section class="panel-item form-offline-panel">
                <div class="compact-config-row">
                  <label>启用本地草稿</label>
                  <n-switch
                    size="small"
                    :value="formOfflineDraftConfig.enabled"
                    @update:value="updateFormOfflineDraft({ enabled: $event })"
                  />
                </div>
                <p class="offline-draft-description">
                  启用后，新增和编辑内容会保存在当前用户的浏览器中；恢复网络后仍需检查并确认提交。
                </p>
                <template v-if="formOfflineDraftConfig.enabled">
                  <div class="compact-field">
                    <label>草稿所属表单</label>
                    <n-input
                      :value="formOfflineDraftConfig.formCode"
                      clearable
                      placeholder="默认使用当前表单编码"
                      size="small"
                      @update:value="updateFormOfflineDraft({ formCode: $event || schema.formKey || 'default' })"
                    />
                  </div>
                  <div class="compact-field">
                    <label>联网后提交动作</label>
                    <n-input
                      :value="formOfflineDraftConfig.replayActionCode"
                      clearable
                      placeholder="可选；填写已发布的业务动作编码"
                      size="small"
                      @update:value="updateFormOfflineDraft({ replayActionCode: $event || '' })"
                    />
                  </div>
                  <div class="compact-field">
                    <label>记录版本字段</label>
                    <n-input
                      :value="formOfflineDraftConfig.recordVersionField"
                      clearable
                      placeholder="默认 updateTime"
                      size="small"
                      @update:value="updateFormOfflineDraft({ recordVersionField: $event || 'updateTime' })"
                    />
                  </div>
                  <p class="offline-draft-description">
                    未配置提交动作时，断网提交只保存草稿；配置动作后也不会自动重放，必须由用户联网后确认。
                  </p>
                </template>
              </section>
            </n-collapse-item>

            <n-collapse-item title="校验反馈" name="validation">
              <section class="panel-item">
                <div class="switch-list">
                  <label>
                    <span>显示校验反馈</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showFeedback !== false"
                      @update:value="updateFormLayout({ showFeedback: $event })"
                    />
                  </label>
                  <label>
                    <span>隐藏必填星号</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.hideRequiredAsterisk"
                      @update:value="updateFormLayout({ hideRequiredAsterisk: $event })"
                    />
                  </label>
                  <label>
                    <span>行内反馈</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.inlineFeedback"
                      @update:value="updateFormLayout({ inlineFeedback: $event })"
                    />
                  </label>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="操作按钮" name="actions">
              <section class="panel-item">
                <div class="panel-item-title">
                  操作按钮
                </div>
                <div class="switch-list">
                  <label>
                    <span>显示操作区</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showActions !== false"
                      @update:value="updateFormLayout({ showActions: $event })"
                    />
                  </label>
                  <label>
                    <span>提交按钮</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showSubmit !== false"
                      @update:value="updateFormLayout({ showSubmit: $event })"
                    />
                  </label>
                  <label>
                    <span>重置按钮</span>
                    <n-switch
                      size="small"
                      :value="schema.layout?.showReset !== false"
                      @update:value="updateFormLayout({ showReset: $event })"
                    />
                  </label>
                  <label>
                    <span>取消按钮</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.showCancel"
                      @update:value="updateFormLayout({ showCancel: $event })"
                    />
                  </label>
                </div>
                <div class="crud-inline-grid form-action-text-grid">
                  <n-form-item label="提交文案">
                    <n-input
                      :value="schema.layout?.submitText || '提交'"
                      placeholder="提交"
                      @update:value="updateFormLayout({ submitText: $event || '提交' })"
                    />
                  </n-form-item>
                  <n-form-item label="重置文案">
                    <n-input
                      :value="schema.layout?.resetText || '重置'"
                      placeholder="重置"
                      @update:value="updateFormLayout({ resetText: $event || '重置' })"
                    />
                  </n-form-item>
                  <n-form-item label="取消文案">
                    <n-input
                      :value="schema.layout?.cancelText || '取消'"
                      placeholder="取消"
                      @update:value="updateFormLayout({ cancelText: $event || '取消' })"
                    />
                  </n-form-item>
                </div>
              </section>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </n-tab-pane>

      <n-tab-pane name="events">
        <template #tab>
          <span class="property-tab-label">
            <n-icon><FlashOutline /></n-icon>
            自动化
            <i v-if="formFieldEventRows.length || formFieldLinkageRows.length || formEventRows.length || hasFormInitConfig" class="property-tab-configured-dot" title="已有自动化配置" />
          </span>
        </template>
        <div class="form-event-primary-panel">
          <p class="form-automation-intro">
            表单的自动行为都在这里配置，按场景分为四类：表单初始化、字段自动查询、字段联动、表单打开或提交时执行动作。
          </p>
          <FormInitPanel />
          <FieldEventRulesEditor
            :model-value="formFieldEventRows"
            :field-options="formFieldOptions"
            @update:model-value="updateFormFieldEvents"
          />
          <FieldLinkageRulesEditor
            :model-value="formFieldLinkageRows"
            :fields="formFieldCatalog"
            :relations="relations"
            @update:model-value="updateFormFieldLinkages"
          />

          <section class="form-lifecycle-panel">
            <div class="form-lifecycle-panel__head">
              <div>
                <strong>打开或提交时自动执行</strong>
                <p>表单打开前后、提交前后自动执行动作，例如调接口取数并回填、填入默认值。</p>
              </div>
              <n-button size="tiny" type="primary" secondary @click="addFormEvent">
                添加动作
              </n-button>
            </div>

            <div v-if="formEventRows.length" class="form-lifecycle-list">
              <div v-for="(eventItem, idx) in formEventRows" :key="eventItem.id || idx" class="lifecycle-event-card">
                <div class="lifecycle-event-card__head">
                  <strong>{{ formEventSummaryLabel(eventItem) }}</strong>
                  <button type="button" class="event-delete-icon" title="删除动作" @click="removeFormEvent(idx)">
                    ×
                  </button>
                </div>

                <div class="compact-field">
                  <label>什么时候执行</label>
                  <n-select
                    :value="eventItem.hook || 'beforeLoad'"
                    :options="formEventHookOptions"
                    placeholder="选择时机"
                    size="small"
                    @update:value="updateFormEvent(idx, { hook: $event || 'beforeLoad' })"
                  />
                </div>

                <div class="compact-field">
                  <label>做什么</label>
                  <n-select
                    :value="eventItem.action || 'customScript'"
                    :options="formEventActionOptions"
                    size="small"
                    @update:value="handleFormEventActionChange(idx, $event)"
                  />
                </div>

                <!-- customScript：内置动作下拉 -->
                <div v-if="(eventItem.action || 'customScript') === 'customScript'" class="compact-field">
                  <label>选择内置动作</label>
                  <n-select
                    :value="eventItem.handler || ''"
                    :options="formScriptOptions"
                    placeholder="选择一个动作"
                    size="small"
                    clearable
                    @update:value="updateFormEvent(idx, { handler: $event || '' })"
                  />
                </div>

                <!-- setFieldValue：字段下拉 + 值输入 -->
                <template v-else-if="eventItem.action === 'setFieldValue'">
                  <div class="compact-field-set-grid">
                    <div class="compact-field">
                      <label>目标字段</label>
                      <n-select
                        :value="parseSetFieldValueHandler(eventItem.handler).field"
                        :options="formFieldOptions"
                        filterable
                        placeholder="选择字段"
                        size="small"
                        @update:value="updateFormEvent(idx, { handler: composeSetFieldValueHandler($event || '', parseSetFieldValueHandler(eventItem.handler).value) })"
                      />
                    </div>
                    <div class="compact-field">
                      <label>要填的值</label>
                      <n-input
                        :value="parseSetFieldValueHandler(eventItem.handler).value"
                        placeholder="例如 APPROVED"
                        size="small"
                        @update:value="updateFormEvent(idx, { handler: composeSetFieldValueHandler(parseSetFieldValueHandler(eventItem.handler).field, $event) })"
                      />
                    </div>
                  </div>
                </template>

                <!-- request：请求方式 + 接口地址 + 结果回填 -->
                <template v-else-if="eventItem.action === 'request'">
                  <div class="compact-field-set-grid">
                    <div class="compact-field">
                      <label>请求方式</label>
                      <n-select
                        :value="parseRequestHandler(eventItem.handler).method"
                        :options="requestMethodOptions"
                        size="small"
                        @update:value="updateFormEvent(idx, { handler: composeRequestHandler($event, parseRequestHandler(eventItem.handler).url) })"
                      />
                    </div>
                    <div class="compact-field">
                      <label>接口地址</label>
                      <n-input
                        :value="parseRequestHandler(eventItem.handler).url"
                        clearable
                        placeholder="例如 /api/v1/customer/init"
                        size="small"
                        @update:value="updateFormEvent(idx, { handler: composeRequestHandler(parseRequestHandler(eventItem.handler).method, $event) })"
                      />
                    </div>
                  </div>
                  <div class="compact-field">
                    <label>接口返回后，回填到哪些表单字段</label>
                    <div
                      v-if="parseResultMappingRows(eventItem.resultMapping).length"
                      class="result-mapping-head"
                    >
                      <span>接口返回字段</span>
                      <span />
                      <span>填入表单字段</span>
                    </div>
                    <div class="result-mapping-list">
                      <div
                        v-for="(row, rowIdx) in parseResultMappingRows(eventItem.resultMapping)"
                        :key="rowIdx"
                        class="result-mapping-row"
                      >
                        <n-input
                          :value="row.from"
                          placeholder="例如 data.name"
                          size="small"
                          @update:value="updateResultMappingRow(idx, rowIdx, { from: $event || '' })"
                        />
                        <span class="result-mapping-arrow">→</span>
                        <n-select
                          :value="row.to || null"
                          :options="formFieldOptions"
                          filterable
                          placeholder="选择要填入的表单字段"
                          size="small"
                          @update:value="updateResultMappingRow(idx, rowIdx, { to: $event || '' })"
                        />
                        <button type="button" class="result-mapping-remove" title="删除这条" @click="removeResultMappingRow(idx, rowIdx)">
                          ×
                        </button>
                      </div>
                      <n-button size="tiny" dashed block @click="addResultMappingRow(idx)">
                        + 添加一条回填
                      </n-button>
                    </div>
                    <small class="result-mapping-hint">示例：接口返回 {"data":{"name":"张三"}}，左边填 data.name，右边选要填入的表单字段；不需要回填可以不加。</small>
                  </div>
                </template>
              </div>
            </div>
            <n-empty v-else size="small" description="还没有自动执行动作" />
          </section>
        </div>
      </n-tab-pane>

      <n-tab-pane name="style">
        <template #tab>
          <span class="property-tab-label">
            <n-icon><ColorPaletteOutline /></n-icon>
            样式
          </span>
        </template>
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <n-collapse v-model:expanded-names="formStyleExpandedNames" class="form-property-collapse">
            <n-collapse-item title="位置与尺寸" name="position">
              <section class="panel-item position-control">
                <div class="position-axis-grid">
                  <label class="position-number-field">
                    <span>左 X</span>
                    <n-input-number
                      :value="formTranslate.x"
                      size="small"
                      :show-button="false"
                      @update:value="updateFormTranslate('x', $event)"
                    />
                    <em>px</em>
                  </label>
                  <label class="position-number-field">
                    <span>上 Y</span>
                    <n-input-number
                      :value="formTranslate.y"
                      size="small"
                      :show-button="false"
                      @update:value="updateFormTranslate('y', $event)"
                    />
                    <em>px</em>
                  </label>
                </div>
                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>页面宽度</span>
                    <label class="position-inline-number">
                      <n-input-number
                        :value="resolvePxNumber(formStyle.maxWidth, 960)"
                        size="tiny"
                        :min="320"
                        :show-button="false"
                        @update:value="updateFormStyle({ maxWidth: valueToPx($event) })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="segmented-mini">
                    <button
                      type="button"
                      :class="{ active: formStyle.maxWidth !== '100%' }"
                      @click="updateFormStyle({ maxWidth: formStyle.maxWidth === '100%' ? '960px' : formStyle.maxWidth || '960px' })"
                    >
                      默认宽度
                    </button>
                    <button
                      type="button"
                      :class="{ active: formStyle.maxWidth === '100%' }"
                      @click="updateFormStyle({ maxWidth: '100%' })"
                    >
                      填充容器
                    </button>
                  </div>
                </div>
                <div class="position-rule">
                  <div class="position-rule-head">
                    <span>页面高度</span>
                    <label class="position-inline-number">
                      <n-input-number
                        :value="resolvePxNumber(formStyle.minHeight, 320)"
                        size="tiny"
                        :min="0"
                        :show-button="false"
                        @update:value="updateFormStyle({ minHeight: valueToPx($event) })"
                      />
                      <em>px</em>
                    </label>
                  </div>
                  <div class="segmented-mini">
                    <button
                      type="button"
                      :class="{ active: formStyle.minHeight !== 'auto' }"
                      @click="updateFormStyle({ minHeight: formStyle.minHeight === 'auto' ? '320px' : formStyle.minHeight || '320px' })"
                    >
                      默认高度
                    </button>
                    <button
                      type="button"
                      :class="{ active: formStyle.minHeight === 'auto' }"
                      @click="updateFormStyle({ minHeight: 'auto' })"
                    >
                      适应内容
                    </button>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="布局与边距" name="spacing">
              <section class="panel-item">
                <div class="panel-item-title">
                  布局与边距
                </div>
                <div class="spacing-editor">
                  <div class="spacing-editor-title">
                    Padding
                  </div>
                  <div class="spacing-grid">
                    <label v-for="item in spacingSides" :key="`form-padding-${item.key}`">
                      <span>{{ item.label }}</span>
                      <n-input-number
                        :value="resolvePxNumber(formStyle[`padding${item.key}`], 0)"
                        :min="0"
                        :max="120"
                        :show-button="false"
                        size="small"
                        @update:value="updateFormSpacing(`padding${item.key}`, $event)"
                      />
                    </label>
                  </div>
                </div>
                <div class="spacing-editor">
                  <div class="spacing-editor-title">
                    Margin
                  </div>
                  <div class="spacing-grid">
                    <label v-for="item in spacingSides" :key="`form-margin-${item.key}`">
                      <span>{{ item.label }}</span>
                      <n-input-number
                        :value="resolvePxNumber(formStyle[`margin${item.key}`], 0)"
                        :min="-80"
                        :max="120"
                        :show-button="false"
                        size="small"
                        @update:value="updateFormSpacing(`margin${item.key}`, $event)"
                      />
                    </label>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="文字排版" name="typography">
              <section class="panel-item">
                <div class="panel-item-title">
                  文字排版
                </div>
                <div class="crud-inline-grid">
                  <n-form-item label="字号">
                    <n-input-number
                      :value="resolvePxNumber(formStyle.fontSize, 14)"
                      :min="10"
                      :max="48"
                      :show-button="false"
                      @update:value="updateFormStyle({ fontSize: valueToPx($event) })"
                    />
                  </n-form-item>
                  <n-form-item label="行高">
                    <n-input
                      :value="formStyle.lineHeight || ''"
                      clearable
                      placeholder="1.5 / 22px"
                      @update:value="updateFormStyle({ lineHeight: $event || undefined })"
                    />
                  </n-form-item>
                </div>
                <n-form-item label="文字颜色">
                  <div class="color-control">
                    <n-color-picker
                      :value="formStyle.color || ''"
                      :show-alpha="true"
                      :modes="['hex']"
                      :swatches="colorSwatches"
                      @update:value="updateFormStyle({ color: $event || undefined })"
                    />
                    <n-button size="small" quaternary @click="updateFormStyle({ color: undefined })">
                      默认
                    </n-button>
                  </div>
                </n-form-item>
              </section>
            </n-collapse-item>

            <n-collapse-item title="外观与装饰" name="appearance">
              <section class="panel-item appearance-control">
                <div class="appearance-field">
                  <label>背景色</label>
                  <div class="appearance-input-shell">
                    <label class="appearance-swatch" :style="{ backgroundColor: formAppearanceBackgroundPreview }" title="选择背景色">
                      <input
                        type="color"
                        :value="formAppearanceBackgroundColorInput"
                        @input="updateFormAppearanceBackground($event.target.value)"
                      >
                    </label>
                    <input
                      :value="formAppearanceBackgroundHex"
                      class="appearance-hex-input"
                      placeholder="透明"
                      @input="updateFormAppearanceBackground($event.target.value)"
                    >
                    <span class="appearance-percent">{{ formOpacityPercent }}%</span>
                  </div>
                </div>
                <div class="appearance-field">
                  <label>边框 (Border)</label>
                  <div class="appearance-input-shell">
                    <select
                      :value="formStyle.borderStyle || 'solid'"
                      class="appearance-select"
                      @change="updateFormBorderStyle($event.target.value)"
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
                    <label class="appearance-swatch" :style="{ backgroundColor: formAppearanceBorderPreview }" title="选择边框颜色">
                      <input
                        type="color"
                        :value="formAppearanceBorderPreview"
                        @input="updateFormAppearanceBorder($event.target.value)"
                      >
                    </label>
                    <input
                      :value="formAppearanceBorderHex"
                      class="appearance-hex-input"
                      placeholder="E4E4E7"
                      @input="updateFormAppearanceBorder($event.target.value)"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label>边框粗细 (Border Width)</label>
                  <div class="appearance-radius-shell">
                    <span>W</span>
                    <input
                      :value="resolvePxNumber(formStyle.borderWidth, 1)"
                      type="number"
                      min="0"
                      max="32"
                      :disabled="(formStyle.borderStyle || 'solid') === 'none'"
                      @input="updateFormBorderWidth($event.target.value)"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label>圆角 (Border Radius)</label>
                  <div class="appearance-radius-shell">
                    <span>R</span>
                    <input
                      :value="resolvePxNumber(formStyle.borderRadius, 0)"
                      type="number"
                      min="0"
                      max="32"
                      @input="updateFormStyle({ borderRadius: `${$event.target.value || 0}px` })"
                    >
                  </div>
                </div>
                <div class="appearance-field">
                  <label class="appearance-row-label">
                    <span>阴影 (Shadow)</span>
                    <select
                      :value="formStyle.boxShadow || ''"
                      class="appearance-plain-select"
                      @change="updateFormStyle({ boxShadow: $event.target.value || undefined })"
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
                      :value="formOpacityPercent"
                      type="number"
                      min="20"
                      max="100"
                      step="5"
                      @input="updateFormAppearanceOpacity($event.target.value)"
                    >
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="自定义样式" name="custom-style">
              <section class="panel-item">
                <div class="panel-item-title">
                  自定义样式
                </div>
                <n-form-item label="表单 Class">
                  <n-input
                    :value="schema.layout?.formClass"
                    clearable
                    placeholder="自定义 className"
                    @update:value="updateFormLayout({ formClass: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="CSS Style">
                  <n-input
                    :value="schema.layout?.formStyleText || stringifyStyle(formStyle)"
                    type="textarea"
                    :autosize="{ minRows: 3, maxRows: 6 }"
                    placeholder="例如 padding:12px; background:var(--bg-primary);"
                    @update:value="updateFormStyleText"
                  />
                </n-form-item>
              </section>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </n-tab-pane>

      <n-tab-pane v-if="false" name="source">
        <template #tab>
          <span class="property-tab-label">
            <n-icon><CodeSlashOutline /></n-icon>
            源码
          </span>
        </template>
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <section class="panel-item source-panel">
            <div class="panel-title-row">
              <div>
                <div class="panel-item-title">
                  表单 Schema JSON
                </div>
                <div class="source-path">
                  {{ schema.formKey || 'formDesignerSchema' }}
                </div>
              </div>
              <div class="source-actions">
                <n-button size="tiny" tertiary @click="resetSchemaCodeDraft">
                  重置
                </n-button>
                <n-button size="tiny" type="primary" secondary @click="applySchemaCode">
                  应用
                </n-button>
              </div>
            </div>
            <n-input
              :value="schemaCodeText"
              type="textarea"
              class="source-editor"
              :autosize="{ minRows: 18, maxRows: 32 }"
              @update:value="updateSchemaCodeDraft"
            />
            <div v-if="sourceError" class="source-error">
              {{ sourceError }}
            </div>
          </section>
        </n-form>
      </n-tab-pane>
    </n-tabs>

</template>

<script>
import { useForgePropertyPanelApi } from '../forgePropertyPanelContext'
import { forgePropertyPanelLocalComponents } from '../forgePropertyPanelLocalComponents'

export default {
  name: 'FormPropertyTabs',
  components: { ...forgePropertyPanelLocalComponents },
  setup() {
    return useForgePropertyPanelApi()
  },
}
</script>

<style scoped src="../forge-property-panel-shell.css"></style>
<style scoped src="../forge-property-panel-fields.css"></style>
