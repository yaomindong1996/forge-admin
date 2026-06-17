<template>
  <div class="forge-property-panel">
    <div class="edit-panel-header">
      <div class="edit-panel-title">
        <strong>{{ selectedComponent ? selectedLabel : '表单属性' }}</strong>
        <span>{{ panelDescription }}</span>
      </div>
      <div class="edit-panel-tools">
        <n-button v-if="selectedComponent" size="tiny" secondary @click="showFormSettings">
          编辑表单
        </n-button>
        <n-button v-if="selectedComponent" size="tiny" secondary type="info" @click="propertyActiveTab = 'interaction'">
          交互
        </n-button>
        <n-button v-if="selectedComponent" size="tiny" secondary type="warning" @click="propertyActiveTab = 'source'">
          源码
        </n-button>
        <n-button v-if="!selectedComponent" size="tiny" secondary type="warning" @click="formPropertyActiveTab = 'source'">
          源码
        </n-button>
        <button type="button" class="panel-close-button" title="收起" @click="$emit('close')">
          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20.207 20.207a.99.99 0 0 0 .003-1.403L13.406 12l6.804-6.804a.99.99 0 0 0-.003-1.403.99.99 0 0 0-1.403-.003L12 10.594 5.196 3.79a.99.99 0 0 0-1.403.003.99.99 0 0 0-.003 1.403L10.594 12 3.79 18.804a.99.99 0 0 0 .003 1.403.99.99 0 0 0 1.403.003L12 13.406l6.804 6.804a.99.99 0 0 0 1.403-.003Z" fill="currentColor" />
          </svg>
        </button>
      </div>
    </div>

    <template v-if="selectedComponent">
      <n-tabs v-model:value="propertyActiveTab" type="line" size="medium" animated class="property-tabs">
        <n-tab-pane name="basic" tab="基础配置">
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <n-collapse class="config-collapse" :default-expanded-names="basicExpandedNames">
              <n-collapse-item title="标识" name="identity">
                <section class="panel-item">
                  <n-form-item label="显示名称">
                    <n-input :value="selectedComponent.label" placeholder="请输入" @update:value="updateLabel" />
                  </n-form-item>
                  <n-form-item v-if="selectedComponent.fieldBinding?.fieldCode" label="绑定字段">
                    <n-input
                      :value="selectedComponent.fieldBinding.fieldCode"
                      clearable
                      placeholder="请输入字段编码"
                      @update:value="updateFieldBindingCode"
                    />
                  </n-form-item>
                  <n-form-item v-if="isCrudBlock" label="区块说明">
                    <n-input
                      :value="selectedComponent.props?.description"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 3 }"
                      clearable
                      @update:value="updateComponent({ props: { description: $event } })"
                    />
                  </n-form-item>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isField" title="字段组件" name="field">
                <section class="panel-item">
                  <n-form-item label="占位提示">
                    <n-input
                      :value="selectedComponent.props?.placeholder"
                      clearable
                      placeholder="请输入"
                      @update:value="updateComponent({ props: { placeholder: $event } })"
                    />
                  </n-form-item>
                  <n-form-item label="默认值">
                    <n-input
                      :value="selectedComponent.props?.defaultValue"
                      clearable
                      placeholder="请输入"
                      @update:value="updateComponent({ props: { defaultValue: $event } })"
                    />
                  </n-form-item>
                  <n-form-item label="组件尺寸">
                    <n-select
                      :value="selectedComponent.props?.size || ''"
                      :options="componentSizeOptions"
                      @update:value="updateComponent({ props: { size: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item v-if="isDictLikeField" label="字典类型">
                    <n-input
                      :value="selectedComponent.props?.dictType"
                      clearable
                      placeholder="例如 crm_follow_type"
                      @update:value="updateComponent({ props: { dictType: $event } })"
                    />
                  </n-form-item>
                  <n-form-item v-if="activePropGroups.length" label="组件属性">
                    <n-button class="more-config-button" secondary block @click="componentPropsVisible = true">
                      <template #icon>
                        <span class="button-icon">
                          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M12 15.5A3.5 3.5 0 1 0 12 8a3.5 3.5 0 0 0 0 7.5Zm8.25-3.5c0-.48-.05-.95-.14-1.4l2.02-1.57-1.9-3.3-2.39.96a8.57 8.57 0 0 0-2.43-1.4L15.05 2h-6.1l-.36 3.29a8.57 8.57 0 0 0-2.43 1.4l-2.39-.96-1.9 3.3 2.02 1.57a7.22 7.22 0 0 0 0 2.8L1.87 14.97l1.9 3.3 2.39-.96a8.57 8.57 0 0 0 2.43 1.4l.36 3.29h6.1l.36-3.29a8.57 8.57 0 0 0 2.43-1.4l2.39.96 1.9-3.3-2.02-1.57c.09-.45.14-.92.14-1.4Z" fill="currentColor" />
                          </svg>
                        </span>
                      </template>
                      更多属性
                    </n-button>
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>可清空</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.clearable !== false"
                        @update:value="updateComponent({ props: { clearable: $event } })"
                      />
                    </label>
                    <label>
                      <span>显示反馈</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.showFeedback !== false"
                        @update:value="updateComponent({ props: { showFeedback: $event } })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isButtonComponent" title="按钮组件" name="button">
                <section class="panel-item">
                  <n-form-item label="按钮文字">
                    <n-input
                      :value="selectedComponent.props?.text || selectedComponent.label"
                      clearable
                      placeholder="请输入"
                      @update:value="updateComponent({ label: $event || '按钮', props: { text: $event || '按钮' } })"
                    />
                  </n-form-item>
                  <n-form-item label="按钮类型">
                    <n-select
                      :value="selectedComponent.props?.type || 'primary'"
                      :options="buttonTypeOptions"
                      @update:value="updateComponent({ props: { type: $event || 'primary' } })"
                    />
                  </n-form-item>
                  <n-form-item label="组件尺寸">
                    <n-select
                      :value="selectedComponent.props?.size || 'medium'"
                      :options="componentSizeOptions.filter(item => item.value)"
                      @update:value="updateComponent({ props: { size: $event || 'medium' } })"
                    />
                  </n-form-item>
                  <n-form-item v-if="activePropGroups.length" label="组件属性">
                    <n-button class="more-config-button" secondary block @click="componentPropsVisible = true">
                      <template #icon>
                        <span class="button-icon">
                          <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M12 15.5A3.5 3.5 0 1 0 12 8a3.5 3.5 0 0 0 0 7.5Zm8.25-3.5c0-.48-.05-.95-.14-1.4l2.02-1.57-1.9-3.3-2.39.96a8.57 8.57 0 0 0-2.43-1.4L15.05 2h-6.1l-.36 3.29a8.57 8.57 0 0 0-2.43 1.4l-2.39-.96-1.9 3.3 2.02 1.57a7.22 7.22 0 0 0 0 2.8L1.87 14.97l1.9 3.3 2.39-.96a8.57 8.57 0 0 0 2.43 1.4l.36 3.29h6.1l.36-3.29a8.57 8.57 0 0 0 2.43-1.4l2.39.96 1.9-3.3-2.02-1.57c.09-.45.14-.92.14-1.4Z" fill="currentColor" />
                          </svg>
                        </span>
                      </template>
                      更多属性
                    </n-button>
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>块级按钮</span>
                      <n-switch
                        size="small"
                        :value="!!selectedComponent.props?.block"
                        @update:value="updateComponent({ props: { block: $event } })"
                      />
                    </label>
                    <label>
                      <span>禁用</span>
                      <n-switch
                        size="small"
                        :value="!!selectedComponent.props?.disabled"
                        @update:value="updateComponent({ props: { disabled: $event } })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isOptionField" title="选项配置" name="options">
                <section class="panel-item">
                  <div class="option-list">
                    <div v-for="(option, optionIndex) in selectedOptions" :key="`option-${optionIndex}`" class="option-editor-row">
                      <n-input
                        :value="option.label"
                        size="small"
                        placeholder="选项名"
                        @update:value="updateOption(optionIndex, { label: $event })"
                      />
                      <n-input
                        :value="String(option.value ?? '')"
                        size="small"
                        placeholder="值"
                        @update:value="updateOption(optionIndex, { value: $event })"
                      />
                      <n-switch
                        size="small"
                        :value="!!option.disabled"
                        title="禁用"
                        @update:value="updateOption(optionIndex, { disabled: $event })"
                      />
                      <n-button size="tiny" quaternary type="error" @click="removeOption(optionIndex)">
                        删除
                      </n-button>
                      <template v-if="selectedComponent.componentKey === 'checkbox'">
                        <n-switch
                          size="small"
                          :value="!!option.indeterminate"
                          title="半选"
                          @update:value="updateOption(optionIndex, { indeterminate: $event })"
                        />
                        <n-switch
                          size="small"
                          :value="option.focusable !== false"
                          title="可聚焦"
                          @update:value="updateOption(optionIndex, { focusable: $event })"
                        />
                        <n-input
                          class="option-props-input"
                          :value="stringifyJsonProp(option.props)"
                          type="textarea"
                          :autosize="{ minRows: 1, maxRows: 3 }"
                          placeholder="Checkbox props JSON，例如 {&quot;checkedValue&quot;:true}"
                          @update:value="updateOptionJsonProps(optionIndex, $event)"
                        />
                      </template>
                    </div>
                  </div>
                  <n-button size="small" secondary block @click="addOption">
                    新增选项
                  </n-button>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="selectedCrudFieldConfig" title="CRUD 字段配置" name="crud-field">
                <section class="panel-item">
                  <div class="crud-field-config-title">
                    查询条件
                  </div>
                  <n-form-item label="查询标签">
                    <n-input
                      :value="selectedCrudFieldConfig.search?.label || ''"
                      clearable
                      placeholder="默认使用字段名称"
                      @update:value="updateCrudFieldConfig('search', { label: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="查询占位提示">
                    <n-input
                      :value="selectedCrudFieldConfig.search?.placeholder || ''"
                      clearable
                      placeholder="默认使用字段占位提示"
                      @update:value="updateCrudFieldConfig('search', { placeholder: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="查询控件跨度">
                    <n-input-number
                      :value="selectedCrudFieldConfig.search?.span || selectedComponent.layout?.span || 1"
                      :min="1"
                      :max="maxFormGridColumns"
                      @update:value="updateCrudFieldConfig('search', { span: $event || 1 })"
                    />
                  </n-form-item>

                  <div class="crud-field-config-title">
                    表格列
                  </div>
                  <n-form-item label="列标题">
                    <n-input
                      :value="selectedCrudFieldConfig.table?.title || ''"
                      clearable
                      placeholder="默认使用字段名称"
                      @update:value="updateCrudFieldConfig('table', { title: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="列宽">
                    <n-input-number
                      :value="selectedCrudFieldConfig.table?.width"
                      clearable
                      :min="60"
                      :max="800"
                      @update:value="updateCrudFieldConfig('table', { width: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="最小宽度">
                    <n-input-number
                      :value="selectedCrudFieldConfig.table?.minWidth || 120"
                      :min="60"
                      :max="800"
                      @update:value="updateCrudFieldConfig('table', { minWidth: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="对齐方式">
                    <n-select
                      :value="selectedCrudFieldConfig.table?.align || 'left'"
                      :options="tableAlignOptions"
                      @update:value="updateCrudFieldConfig('table', { align: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="固定列">
                    <n-select
                      :value="selectedCrudFieldConfig.table?.fixed || ''"
                      :options="tableFixedOptions"
                      @update:value="updateCrudFieldConfig('table', { fixed: $event || undefined })"
                    />
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>文字省略</span>
                      <n-switch
                        size="small"
                        :value="selectedCrudFieldConfig.table?.ellipsis !== false"
                        @update:value="updateCrudFieldConfig('table', { ellipsis: $event })"
                      />
                    </label>
                    <label>
                      <span>可排序</span>
                      <n-switch
                        size="small"
                        :value="!!selectedCrudFieldConfig.table?.sorter"
                        @update:value="updateCrudFieldConfig('table', { sorter: $event })"
                      />
                    </label>
                  </div>

                  <div class="crud-field-config-title">
                    编辑弹窗
                  </div>
                  <n-form-item label="编辑标签">
                    <n-input
                      :value="selectedCrudFieldConfig.edit?.label || ''"
                      clearable
                      placeholder="默认使用字段名称"
                      @update:value="updateCrudFieldConfig('edit', { label: $event || undefined })"
                    />
                  </n-form-item>
                  <n-form-item label="编辑跨度">
                    <n-input-number
                      :value="selectedCrudFieldConfig.edit?.span || selectedComponent.layout?.span || 1"
                      :min="1"
                      :max="maxFormGridColumns"
                      @update:value="updateCrudFieldConfig('edit', { span: $event || 1 })"
                    />
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>编辑只读</span>
                      <n-switch
                        size="small"
                        :value="!!selectedCrudFieldConfig.edit?.readonly"
                        @update:value="updateCrudFieldConfig('edit', { readonly: $event })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item v-if="isTemporalField" title="日期时间组件" name="temporal">
                <section class="panel-item">
                  <n-form-item v-if="isDatePickerField" label="选择器类型">
                    <n-select
                      :value="selectedComponent.props?.type || datePickerType"
                      :options="datePickerTypeOptions"
                      @update:value="updateComponent({ props: { type: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item label="显示格式">
                    <n-input
                      :value="selectedComponent.props?.format"
                      clearable
                      :placeholder="isTimePickerField ? 'HH:mm:ss' : 'yyyy-MM-dd'"
                      @update:value="updateComponent({ props: { format: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item label="值格式">
                    <n-input
                      :value="selectedComponent.props?.valueFormat"
                      clearable
                      :placeholder="isTimePickerField ? 'HH:mm:ss' : 'yyyy-MM-dd HH:mm:ss'"
                      @update:value="updateComponent({ props: { valueFormat: $event || undefined } })"
                    />
                  </n-form-item>
                  <n-form-item label="弹出位置">
                    <n-select
                      :value="selectedComponent.props?.placement || 'bottom-start'"
                      :options="pickerPlacementOptions"
                      @update:value="updateComponent({ props: { placement: $event || 'bottom-start' } })"
                    />
                  </n-form-item>
                  <n-form-item label="底部动作">
                    <n-select
                      multiple
                      clearable
                      :value="selectedComponent.props?.actions || []"
                      :options="pickerActionOptions"
                      @update:value="updateComponent({ props: { actions: $event?.length ? $event : undefined } })"
                    />
                  </n-form-item>
                  <div class="switch-list">
                    <label>
                      <span>显示边框</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.bordered !== false"
                        @update:value="updateComponent({ props: { bordered: $event } })"
                      />
                    </label>
                    <label>
                      <span>输入只读</span>
                      <n-switch
                        size="small"
                        :value="!!selectedComponent.props?.inputReadonly"
                        @update:value="updateComponent({ props: { inputReadonly: $event } })"
                      />
                    </label>
                    <label v-if="isTimePickerField">
                      <span>显示图标</span>
                      <n-switch
                        size="small"
                        :value="selectedComponent.props?.showIcon !== false"
                        @update:value="updateComponent({ props: { showIcon: $event } })"
                      />
                    </label>
                  </div>
                </section>
              </n-collapse-item>

              <n-collapse-item title="辅助展示" name="assist">
                <section class="panel-item">
                  <n-form-item label="说明文本">
                    <n-input
                      :value="selectedComponent.props?.description"
                      type="textarea"
                      :autosize="{ minRows: 2, maxRows: 3 }"
                      clearable
                      placeholder="显示在组件下方"
                      @update:value="updateComponent({ props: { description: $event } })"
                    />
                  </n-form-item>
                  <n-form-item label="角标">
                    <n-input
                      :value="selectedComponent.props?.badge"
                      clearable
                      placeholder="例如 推荐"
                      @update:value="updateComponent({ props: { badge: $event } })"
                    />
                  </n-form-item>
                </section>
              </n-collapse-item>
            </n-collapse>
          </n-form>
        </n-tab-pane>

        <n-tab-pane name="style" tab="样式配置">
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                背景
              </div>
              <n-form-item label="背景色">
                <div class="color-control">
                  <n-color-picker
                    :value="selectedDesignerStyle.backgroundColor || ''"
                    :show-alpha="true"
                    :modes="['hex']"
                    :swatches="colorSwatches"
                    @update:value="updateDesignerStyle({ backgroundColor: $event || undefined })"
                  />
                  <n-button size="small" quaternary @click="updateDesignerStyle({ backgroundColor: undefined })">
                    默认
                  </n-button>
                </div>
              </n-form-item>
              <n-form-item label="透明度">
                <n-slider
                  :value="selectedOpacityPercent"
                  :min="20"
                  :max="100"
                  :step="5"
                  @update:value="updateDesignerStyle({ opacity: Number($event || 100) / 100 })"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                边框
              </div>
              <n-form-item label="圆角">
                <n-input-number
                  :value="resolvePxNumber(selectedDesignerStyle.borderRadius, 6)"
                  :min="0"
                  :max="32"
                  @update:value="updateDesignerStyle({ borderRadius: `${$event ?? 6}px` })"
                />
              </n-form-item>
              <n-form-item label="边框样式">
                <n-radio-group
                  :value="selectedDesignerStyle.borderStyle || 'solid'"
                  size="small"
                  @update:value="updateDesignerStyle({ borderStyle: $event })"
                >
                  <n-radio-button value="solid">
                    实线
                  </n-radio-button>
                  <n-radio-button value="dashed">
                    虚线
                  </n-radio-button>
                  <n-radio-button value="none">
                    无
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item label="边框颜色">
                <div class="color-control">
                  <n-color-picker
                    :value="selectedDesignerStyle.borderColor || ''"
                    :show-alpha="true"
                    :modes="['hex']"
                    :swatches="colorSwatches"
                    @update:value="updateDesignerStyle({ borderColor: $event || undefined })"
                  />
                  <n-button size="small" quaternary @click="updateDesignerStyle({ borderColor: undefined })">
                    默认
                  </n-button>
                </div>
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                阴影与尺寸
              </div>
              <n-form-item label="阴影">
                <n-select
                  :value="selectedDesignerStyle.boxShadow || ''"
                  :options="shadowOptions"
                  @update:value="updateDesignerStyle({ boxShadow: $event || undefined })"
                />
              </n-form-item>
              <div class="size-setting-row">
                <div class="size-setting-label">
                  宽度
                </div>
                <n-radio-group
                  :value="selectedDesignerStyle.widthMode || 'default'"
                  size="small"
                  @update:value="updateWidthMode"
                >
                  <n-radio-button value="default">
                    默认宽度
                  </n-radio-button>
                  <n-radio-button value="fill">
                    填充容器
                  </n-radio-button>
                </n-radio-group>
              </div>
              <div class="size-setting-row">
                <div class="size-setting-label">
                  高度
                </div>
                <n-radio-group
                  :value="selectedDesignerStyle.heightMode || 'default'"
                  size="small"
                  @update:value="updateHeightMode"
                >
                  <n-radio-button value="default">
                    默认高度
                  </n-radio-button>
                  <n-radio-button value="fit">
                    适应内容
                  </n-radio-button>
                  <n-radio-button value="fill">
                    填充容器
                  </n-radio-button>
                </n-radio-group>
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                内外边距
              </div>
              <div class="spacing-editor">
                <div class="spacing-editor-title">
                  Padding
                </div>
                <div class="spacing-grid">
                  <label v-for="item in spacingSides" :key="`component-padding-${item.key}`">
                    <span>{{ item.label }}</span>
                    <n-input-number
                      :value="resolvePxNumber(selectedDesignerStyle.customStyle?.[`padding${item.key}`], 0)"
                      :min="0"
                      :max="120"
                      :show-button="false"
                      size="small"
                      @update:value="updateDesignerSpacing(`padding${item.key}`, $event)"
                    />
                  </label>
                </div>
              </div>
              <div class="spacing-editor">
                <div class="spacing-editor-title">
                  Margin
                </div>
                <div class="spacing-grid">
                  <label v-for="item in spacingSides" :key="`component-margin-${item.key}`">
                    <span>{{ item.label }}</span>
                    <n-input-number
                      :value="resolvePxNumber(selectedDesignerStyle.customStyle?.[`margin${item.key}`], 0)"
                      :min="-80"
                      :max="120"
                      :show-button="false"
                      size="small"
                      @update:value="updateDesignerSpacing(`margin${item.key}`, $event)"
                    />
                  </label>
                </div>
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                自定义样式
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
          </n-form>
        </n-tab-pane>

        <n-tab-pane v-if="isCrudBlock" name="crud" tab="CRUD">
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item panel-item-strong">
              <div class="panel-title-row">
                <div class="panel-item-title">
                  系统 CRUD 组件
                </div>
                <n-button class="more-config-button" size="tiny" type="primary" @click="advancedConfigVisible = true">
                  更多配置
                </n-button>
              </div>
              <n-form-item label="接口基础路径">
                <n-input
                  :value="selectedComponent.props?.apiBase"
                  clearable
                  placeholder="/employee"
                  @update:value="updateCrudApiBase"
                />
              </n-form-item>
              <n-form-item label="行主键">
                <n-input
                  :value="selectedComponent.props?.rowKey || 'id'"
                  placeholder="id"
                  @update:value="updateComponent({ props: { rowKey: $event || 'id' } })"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                字段与列配置
              </div>
              <div v-if="crudConfigFields.length" class="crud-field-config-list">
                <div v-for="item in crudConfigFields" :key="item.id" class="crud-field-config-card">
                  <div class="crud-field-card-head">
                    <button type="button" class="crud-field-name" @click="$emit('update:selectedId', item.id)">
                      <strong>{{ item.label }}</strong>
                      <small>{{ item.fieldCode }}</small>
                    </button>
                    <div class="crud-role-switches">
                      <label>
                        <span>查询</span>
                        <n-switch
                          size="small"
                          :value="item.roles.search"
                          @update:value="updateCrudFieldRole(item.id, 'search', $event)"
                        />
                      </label>
                      <label>
                        <span>表格列</span>
                        <n-switch
                          size="small"
                          :value="item.roles.table"
                          @update:value="updateCrudFieldRole(item.id, 'table', $event)"
                        />
                      </label>
                      <label>
                        <span>编辑</span>
                        <n-switch
                          size="small"
                          :value="item.roles.edit"
                          @update:value="updateCrudFieldRole(item.id, 'edit', $event)"
                        />
                      </label>
                    </div>
                  </div>
                  <div class="crud-inline-grid">
                    <n-form-item label="列标题">
                      <n-input
                        :value="item.config.table?.title || ''"
                        size="small"
                        clearable
                        placeholder="默认字段名"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { title: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="列宽">
                      <n-input-number
                        :value="item.config.table?.width"
                        size="small"
                        clearable
                        :min="60"
                        :max="800"
                        :show-button="false"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { width: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="对齐">
                      <n-select
                        :value="item.config.table?.align || 'left'"
                        size="small"
                        :options="tableAlignOptions"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { align: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="固定">
                      <n-select
                        :value="item.config.table?.fixed || ''"
                        size="small"
                        :options="tableFixedOptions"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { fixed: $event || undefined })"
                      />
                    </n-form-item>
                  </div>
                  <div class="crud-compact-switches">
                    <label>
                      <span>省略</span>
                      <n-switch
                        size="small"
                        :value="item.config.table?.ellipsis !== false"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { ellipsis: $event })"
                      />
                    </label>
                    <label>
                      <span>排序</span>
                      <n-switch
                        size="small"
                        :value="!!item.config.table?.sorter"
                        @update:value="updateCrudFieldConfigById(item.id, 'table', { sorter: $event })"
                      />
                    </label>
                    <n-button size="tiny" tertiary @click="openCrudFieldDrawer(item.id)">
                      更多字段配置
                    </n-button>
                  </div>
                </div>
              </div>
              <div v-else class="crud-field-empty">
                先把字段拖入 CRUD 区块，字段会自动生成查询条件、表格列和编辑弹窗字段。
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                查询表单
              </div>
              <n-form-item label="搜索列数">
                <n-input-number
                  :value="crudOptions.searchGridCols || 4"
                  :min="1"
                  :max="6"
                  @update:value="updateCrudOption('searchGridCols', $event || 4)"
                />
              </n-form-item>
              <n-form-item label="最大显示字段数">
                <n-input-number
                  :value="crudOptions.searchMaxVisibleFields || 3"
                  :min="1"
                  :max="12"
                  @update:value="updateCrudOption('searchMaxVisibleFields', $event || 3)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                表格
              </div>
              <n-form-item label="表格尺寸">
                <n-select
                  :value="crudOptions.tableSize || 'small'"
                  :options="componentSizeOptions.filter(item => item.value)"
                  @update:value="updateCrudOption('tableSize', $event || 'small')"
                />
              </n-form-item>
              <n-form-item label="渲染模式">
                <n-radio-group
                  :value="crudOptions.renderMode || 'table'"
                  size="small"
                  @update:value="updateCrudOption('renderMode', $event)"
                >
                  <n-radio-button value="table">
                    表格
                  </n-radio-button>
                  <n-radio-button value="card">
                    卡片
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                编辑表单
              </div>
              <n-form-item label="编辑列数">
                <n-input-number
                  :value="crudOptions.editGridCols || 1"
                  :min="1"
                  :max="maxFormGridColumns"
                  @update:value="updateCrudOption('editGridCols', $event || 1)"
                />
              </n-form-item>
              <n-form-item label="标签位置">
                <n-radio-group
                  :value="crudOptions.editLabelPlacement || 'left'"
                  size="small"
                  @update:value="updateCrudOption('editLabelPlacement', $event)"
                >
                  <n-radio-button value="left">
                    左侧
                  </n-radio-button>
                  <n-radio-button value="top">
                    顶部
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item label="编辑表单尺寸">
                <n-select
                  :value="crudOptions.editSize || 'medium'"
                  :options="componentSizeOptions.filter(item => item.value)"
                  @update:value="updateCrudOption('editSize', $event || 'medium')"
                />
              </n-form-item>
            </section>
          </n-form>
        </n-tab-pane>

        <n-tab-pane name="layout" tab="布局">
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                栅格
              </div>
              <n-form-item v-if="!isRowLayout" label="占据列数">
                <div class="slider-control">
                  <n-slider
                    :value="selectedComponent.layout?.span || 1"
                    :min="1"
                    :max="normalizedFormGridColumns"
                    :step="1"
                    @update:value="updateComponent({ layout: { span: $event || 1 } })"
                  />
                  <n-input-number
                    :value="selectedComponent.layout?.span || 1"
                    :min="1"
                    :max="normalizedFormGridColumns"
                    :show-button="false"
                    size="small"
                    @update:value="updateComponent({ layout: { span: $event || 1 } })"
                  />
                </div>
              </n-form-item>
              <n-form-item v-if="isRowLayout" label="栅格列数">
                <n-radio-group
                  :value="rowColumnCount"
                  size="small"
                  @update:value="updateRowColumns"
                >
                  <n-radio-button v-for="item in gridColumnOptions" :key="item" :value="item">
                    {{ item }} 列
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item v-if="isRowLayout" label="列间距">
                <n-input-number
                  :value="selectedComponent.props?.gutter ?? 16"
                  :min="0"
                  :max="40"
                  @update:value="updateComponent({ props: { gutter: $event ?? 16 } })"
                />
              </n-form-item>
              <n-form-item v-if="isColumnLayout" label="栅格列宽">
                <n-radio-group
                  :value="selectedComponent.layout?.span || 1"
                  size="small"
                  @update:value="updateComponent({ layout: { span: $event || 1 }, props: { span: $event || 1 } })"
                >
                  <n-radio-button v-for="item in gridColumnOptions" :key="item" :value="item">
                    {{ item }} 格
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item v-if="isField" label="标签宽度">
                <n-input-number
                  :value="selectedComponent.layout?.labelWidth"
                  clearable
                  :min="60"
                  :max="260"
                  @update:value="updateComponent({ layout: { labelWidth: $event || undefined } })"
                />
              </n-form-item>
            </section>
            <section v-if="isCardLayout" class="panel-item">
              <div class="panel-item-title">
                Card 属性
              </div>
              <n-form-item label="size">
                <n-select
                  :value="selectedComponent.props?.size || 'small'"
                  :options="cardSizeOptions"
                  @update:value="updateComponent({ props: { size: $event || 'small' } })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>bordered</span>
                  <n-switch
                    size="small"
                    :value="selectedComponent.props?.bordered !== false"
                    @update:value="updateComponent({ props: { bordered: $event } })"
                  />
                </label>
                <label>
                  <span>embedded</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.embedded"
                    @update:value="updateComponent({ props: { embedded: $event } })"
                  />
                </label>
                <label>
                  <span>segmented</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.segmented"
                    @update:value="updateComponent({ props: { segmented: $event } })"
                  />
                </label>
                <label>
                  <span>hoverable</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.hoverable"
                    @update:value="updateComponent({ props: { hoverable: $event } })"
                  />
                </label>
              </div>
            </section>

            <section v-if="isTabsLayout" class="panel-item">
              <div class="panel-item-title">
                Tabs 属性
              </div>
              <n-form-item label="type">
                <n-select
                  :value="selectedComponent.props?.type || 'line'"
                  :options="tabsTypeOptions"
                  @update:value="updateComponent({ props: { type: $event || 'line' } })"
                />
              </n-form-item>
              <n-form-item label="placement">
                <n-select
                  :value="selectedComponent.props?.placement || 'top'"
                  :options="tabsPlacementOptions"
                  @update:value="updateComponent({ props: { placement: $event || 'top' } })"
                />
              </n-form-item>
              <n-form-item label="trigger">
                <n-select
                  :value="selectedComponent.props?.trigger || 'click'"
                  :options="tabsTriggerOptions"
                  @update:value="updateComponent({ props: { trigger: $event || 'click' } })"
                />
              </n-form-item>
              <n-form-item label="size">
                <n-select
                  :value="selectedComponent.props?.size || 'medium'"
                  :options="componentSizeOptions.filter(item => item.value)"
                  @update:value="updateComponent({ props: { size: $event || 'medium' } })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>animated</span>
                  <n-switch
                    size="small"
                    :value="selectedComponent.props?.animated !== false"
                    @update:value="updateComponent({ props: { animated: $event } })"
                  />
                </label>
                <label>
                  <span>closable</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.closable"
                    @update:value="updateComponent({ props: { closable: $event } })"
                  />
                </label>
              </div>
              <div class="layout-child-manager">
                <div class="panel-title-row">
                  <div class="panel-item-title">
                    页签管理
                  </div>
                  <n-button size="tiny" type="primary" secondary @click="addLayoutChild('tabPane')">
                    新增页签
                  </n-button>
                </div>
                <div v-for="(child, childIndex) in layoutChildren" :key="child.id" class="layout-child-card">
                  <div class="layout-child-card-main">
                    <n-input
                      :value="child.props?.label || child.label"
                      size="small"
                      placeholder="页签名称"
                      @update:value="updateLayoutChild(childIndex, { label: $event || `标签 ${childIndex + 1}`, props: { label: $event || `标签 ${childIndex + 1}` } })"
                    />
                    <n-input
                      :value="child.props?.name || child.id"
                      size="small"
                      placeholder="name"
                      @update:value="updateLayoutChild(childIndex, { props: { name: $event || undefined } })"
                    />
                  </div>
                  <div class="layout-child-actions">
                    <n-button size="tiny" tertiary @click="$emit('update:selectedId', child.id)">
                      配置内容
                    </n-button>
                    <n-button size="tiny" tertiary :disabled="childIndex === 0" @click="moveLayoutChild(childIndex, -1)">
                      上移
                    </n-button>
                    <n-button size="tiny" tertiary :disabled="childIndex === layoutChildren.length - 1" @click="moveLayoutChild(childIndex, 1)">
                      下移
                    </n-button>
                    <n-button size="tiny" quaternary type="error" :disabled="layoutChildren.length <= 1" @click="removeLayoutChild(childIndex)">
                      删除
                    </n-button>
                  </div>
                </div>
              </div>
            </section>

            <section v-if="isCollapseLayout" class="panel-item">
              <div class="panel-item-title">
                Collapse 属性
              </div>
              <n-form-item label="arrowPlacement">
                <n-select
                  :value="selectedComponent.props?.arrowPlacement || 'left'"
                  :options="collapseArrowPlacementOptions"
                  @update:value="updateComponent({ props: { arrowPlacement: $event || 'left' } })"
                />
              </n-form-item>
              <n-form-item label="displayDirective">
                <n-select
                  :value="selectedComponent.props?.displayDirective || 'if'"
                  :options="collapseDisplayDirectiveOptions"
                  @update:value="updateComponent({ props: { displayDirective: $event || 'if' } })"
                />
              </n-form-item>
              <n-form-item label="triggerAreas">
                <n-select
                  multiple
                  :value="selectedComponent.props?.triggerAreas || ['main', 'arrow']"
                  :options="collapseTriggerAreaOptions"
                  @update:value="updateComponent({ props: { triggerAreas: $event?.length ? $event : ['main', 'arrow'] } })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>accordion</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.accordion"
                    @update:value="updateComponent({ props: { accordion: $event } })"
                  />
                </label>
              </div>
              <div class="layout-child-manager">
                <div class="panel-title-row">
                  <div class="panel-item-title">
                    面板管理
                  </div>
                  <n-button size="tiny" type="primary" secondary @click="addLayoutChild('collapseItem')">
                    新增面板
                  </n-button>
                </div>
                <div v-for="(child, childIndex) in layoutChildren" :key="child.id" class="layout-child-card">
                  <div class="layout-child-card-main">
                    <n-input
                      :value="child.props?.title || child.label"
                      size="small"
                      placeholder="面板标题"
                      @update:value="updateLayoutChild(childIndex, { label: $event || `分组 ${childIndex + 1}`, props: { title: $event || `分组 ${childIndex + 1}` } })"
                    />
                    <n-input
                      :value="child.props?.name || child.id"
                      size="small"
                      placeholder="name"
                      @update:value="updateLayoutChild(childIndex, { props: { name: $event || undefined } })"
                    />
                  </div>
                  <div class="layout-child-actions">
                    <n-button size="tiny" tertiary @click="$emit('update:selectedId', child.id)">
                      配置内容
                    </n-button>
                    <n-button size="tiny" tertiary :disabled="childIndex === 0" @click="moveLayoutChild(childIndex, -1)">
                      上移
                    </n-button>
                    <n-button size="tiny" tertiary :disabled="childIndex === layoutChildren.length - 1" @click="moveLayoutChild(childIndex, 1)">
                      下移
                    </n-button>
                    <n-button size="tiny" quaternary type="error" :disabled="layoutChildren.length <= 1" @click="removeLayoutChild(childIndex)">
                      删除
                    </n-button>
                  </div>
                </div>
              </div>
            </section>
          </n-form>
        </n-tab-pane>

        <n-tab-pane name="state" tab="状态">
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                可见性与校验
              </div>
              <div class="switch-list">
                <label v-if="isField">
                  <span>必填</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.validation?.required"
                    @update:value="updateComponent({ validation: { required: $event } })"
                  />
                </label>
                <label v-if="isField">
                  <span>只读</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.visibility?.readonly"
                    @update:value="updateComponent({ visibility: { readonly: $event } })"
                  />
                </label>
                <label v-if="isField || isButtonComponent">
                  <span>禁用</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.props?.disabled"
                    @update:value="updateComponent({ props: { disabled: $event } })"
                  />
                </label>
                <label>
                  <span>隐藏</span>
                  <n-switch
                    size="small"
                    :value="!!selectedComponent.visibility?.hidden"
                    @update:value="updateComponent({ visibility: { hidden: $event } })"
                  />
                </label>
              </div>
              <n-form-item v-if="isField" label="必填提示">
                <n-input
                  :value="selectedComponent.validation?.requiredMessage"
                  clearable
                  placeholder="为空时使用默认提示"
                  @update:value="updateComponent({ validation: { requiredMessage: $event } })"
                />
              </n-form-item>
            </section>
          </n-form>
        </n-tab-pane>

        <n-tab-pane name="interaction" tab="交互">
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item">
              <div class="panel-title-row">
                <div class="panel-item-title">
                  事件规则
                </div>
                <n-button size="tiny" type="primary" secondary @click="addInteractionRule">
                  新增规则
                </n-button>
              </div>
              <div class="interaction-presets">
                <button
                  v-for="preset in interactionPresets"
                  :key="preset.key"
                  type="button"
                  class="interaction-preset-card"
                  @click="addInteractionPreset(preset.key)"
                >
                  <strong>{{ preset.title }}</strong>
                  <span>{{ preset.description }}</span>
                </button>
              </div>
              <div v-if="interactionRules.length" class="interaction-rule-list">
                <details v-for="(rule, ruleIndex) in interactionRules" :key="rule.id || ruleIndex" class="interaction-rule-card" open>
                  <summary class="interaction-rule-head">
                    <div>
                      <strong>{{ resolveTriggerLabel(rule.trigger) }}</strong>
                      <span>{{ resolveActionLabel(rule.action) }}</span>
                    </div>
                    <n-button size="tiny" quaternary type="error" @click.stop.prevent="removeInteractionRule(ruleIndex)">
                      删除
                    </n-button>
                  </summary>
                  <div class="interaction-grid">
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          触发事件
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            什么时候执行这条规则。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.trigger || defaultTrigger"
                        size="small"
                        :consistent-menu-width="false"
                        :options="triggerOptions"
                        @update:value="updateInteractionRule(ruleIndex, { trigger: $event || defaultTrigger })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          执行动作
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            规则触发后对目标组件做什么。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.action || 'setValue'"
                        size="small"
                        :consistent-menu-width="false"
                        :options="actionOptions"
                        @update:value="updateInteractionRule(ruleIndex, { action: $event || 'setValue' })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          目标组件
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            被这条规则影响的字段、按钮或区块。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.targetId || ''"
                        size="small"
                        filterable
                        clearable
                        :consistent-menu-width="false"
                        :options="componentTargetOptions"
                        @update:value="updateInteractionRule(ruleIndex, { targetId: $event || '' })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          触发值等于
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            用于下拉联动，例如选择“省份A”时才更新城市。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-input
                        :value="rule.whenValue ?? ''"
                        size="small"
                        clearable
                        placeholder="为空表示任何值都触发"
                        @update:value="updateInteractionRule(ruleIndex, { whenValue: $event || undefined })"
                      />
                    </n-form-item>
                  </div>
                  <template v-if="rule.action === 'setOptions'">
                    <n-form-item label="选项来源 API">
                      <n-input
                        :value="rule.api || ''"
                        size="small"
                        clearable
                        placeholder="get@/api/options?parent=:value"
                        @update:value="updateInteractionRule(ruleIndex, { api: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item label="静态选项 JSON">
                      <n-input
                        :value="rule.optionsJson || stringifyJsonProp(rule.options)"
                        type="textarea"
                        :autosize="{ minRows: 2, maxRows: 5 }"
                        placeholder="[{&quot;label&quot;:&quot;选项&quot;,&quot;value&quot;:&quot;A&quot;}]"
                        @update:value="updateInteractionRuleJson(ruleIndex, 'options', $event)"
                      />
                    </n-form-item>
                  </template>
                  <n-form-item v-else-if="['setValue', 'showHide', 'enableDisable'].includes(rule.action)">
                    <template #label>
                      <span class="field-label-with-help">
                        {{ resolveActionValueMeta(rule.action).label }}
                        <n-tooltip trigger="hover">
                          <template #trigger>
                            <span class="help-icon">?</span>
                          </template>
                          {{ resolveActionValueMeta(rule.action).help }}
                        </n-tooltip>
                      </span>
                    </template>
                    <n-input
                      :value="rule.value ?? ''"
                      size="small"
                      clearable
                      :placeholder="resolveActionValueMeta(rule.action).placeholder"
                      @update:value="updateInteractionRule(ruleIndex, { value: $event || undefined })"
                    />
                  </n-form-item>
                  <template v-else-if="rule.action === 'openModal'">
                    <n-form-item label="弹窗标题">
                      <n-input
                        :value="rule.modalTitle || ''"
                        size="small"
                        clearable
                        placeholder="请输入弹窗标题"
                        @update:value="updateInteractionRule(ruleIndex, { modalTitle: $event || undefined })"
                      />
                    </n-form-item>
                    <n-form-item>
                      <template #label>
                        <span class="field-label-with-help">
                          弹窗内容
                          <n-tooltip trigger="hover">
                            <template #trigger>
                              <span class="help-icon">?</span>
                            </template>
                            不需要手写 JSON。可以复用当前表单，或选择画布里的某个组件作为弹窗内容。
                          </n-tooltip>
                        </span>
                      </template>
                      <n-select
                        :value="rule.modalContentMode || 'currentForm'"
                        :options="modalContentModeOptions"
                        @update:value="updateInteractionRule(ruleIndex, { modalContentMode: $event || 'currentForm' })"
                      />
                    </n-form-item>
                    <n-form-item v-if="rule.modalContentMode === 'component'" label="弹窗组件">
                      <n-select
                        :value="rule.modalComponentId || ''"
                        filterable
                        clearable
                        :consistent-menu-width="false"
                        :options="componentTargetOptions"
                        @update:value="updateInteractionRule(ruleIndex, { modalComponentId: $event || '' })"
                      />
                    </n-form-item>
                    <n-form-item v-if="rule.modalContentMode === 'formAsset'" label="引用表单">
                      <n-select
                        :value="rule.modalFormKey || 'current'"
                        filterable
                        :consistent-menu-width="false"
                        :options="formAssetOptions"
                        @update:value="updateInteractionRule(ruleIndex, { modalFormKey: $event || 'current' })"
                      />
                    </n-form-item>
                  </template>
                  <n-form-item v-else-if="rule.action === 'apiRequest'" label="请求接口">
                    <n-input
                      :value="rule.api || ''"
                      size="small"
                      clearable
                      placeholder="post@/api/action"
                      @update:value="updateInteractionRule(ruleIndex, { api: $event || undefined })"
                    />
                  </n-form-item>
                </details>
              </div>
              <div v-else class="empty-config-box">
                暂无事件规则
              </div>
            </section>
          </n-form>
        </n-tab-pane>

        <n-tab-pane name="source" tab="源码">
          <n-form label-placement="top" :show-feedback="false" class="property-form">
            <section class="panel-item source-panel">
              <div class="panel-title-row">
                <div>
                  <div class="panel-item-title">
                    当前组件 JSON
                  </div>
                  <div class="source-path">
                    {{ selectedComponent.id }}
                  </div>
                </div>
                <div class="source-actions">
                  <n-button size="tiny" tertiary @click="resetSelectedCodeDraft">
                    重置
                  </n-button>
                  <n-button size="tiny" type="primary" secondary @click="applySelectedCode">
                    应用
                  </n-button>
                </div>
              </div>
              <n-input
                :value="selectedCodeText"
                type="textarea"
                class="source-editor"
                :autosize="{ minRows: 16, maxRows: 28 }"
                @update:value="updateSelectedCodeDraft"
              />
              <div v-if="sourceError" class="source-error">
                {{ sourceError }}
              </div>
            </section>
          </n-form>
        </n-tab-pane>
      </n-tabs>

      <n-drawer
        v-if="isCrudBlock"
        v-model:show="advancedConfigVisible"
        :width="380"
        placement="right"
        :trap-focus="false"
        :block-scroll="false"
      >
        <n-drawer-content title="CRUD 更多配置" closable>
          <n-form label-placement="top" :show-feedback="false" class="property-form drawer-property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                AiCrudPage API
              </div>
              <n-form-item v-for="item in crudApiFields" :key="item.key" :label="item.label">
                <n-input
                  :value="crudApiConfig[item.key]"
                  clearable
                  :placeholder="item.placeholder"
                  @update:value="updateCrudApiConfig(item.key, $event)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                查询表单细节
              </div>
              <n-form-item label="搜索标签宽度">
                <n-input
                  :value="crudOptions.searchLabelWidth || 'auto'"
                  placeholder="auto / 100"
                  @update:value="updateCrudOption('searchLabelWidth', $event || 'auto')"
                />
              </n-form-item>
              <n-form-item label="搜索行间距">
                <n-input-number
                  :value="crudOptions.searchYGap || 16"
                  :min="0"
                  :max="40"
                  @update:value="updateCrudOption('searchYGap', $event || 16)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                表格细节
              </div>
              <n-form-item label="表格最大高度">
                <n-input
                  :value="crudOptions.maxHeight || ''"
                  clearable
                  placeholder="例如 520 / 60vh"
                  @update:value="updateCrudOption('maxHeight', $event || undefined)"
                />
              </n-form-item>
              <n-form-item label="横向滚动宽度">
                <n-input-number
                  :value="crudOptions.scrollX"
                  clearable
                  :min="0"
                  :max="5000"
                  @update:value="updateCrudOption('scrollX', $event || undefined)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                编辑弹窗
              </div>
              <n-form-item label="编辑标签宽度">
                <n-input
                  :value="crudOptions.editLabelWidth || 'auto'"
                  placeholder="auto / 100"
                  @update:value="updateCrudOption('editLabelWidth', $event || 'auto')"
                />
              </n-form-item>
              <n-form-item label="列间距">
                <n-input-number
                  :value="crudOptions.editXGap || 16"
                  :min="0"
                  :max="40"
                  @update:value="updateCrudOption('editXGap', $event || 16)"
                />
              </n-form-item>
              <n-form-item label="行间距">
                <n-input-number
                  :value="crudOptions.editYGap || 8"
                  :min="0"
                  :max="40"
                  @update:value="updateCrudOption('editYGap', $event || 8)"
                />
              </n-form-item>
              <n-form-item label="编辑弹窗">
                <n-radio-group
                  :value="crudOptions.modalType || 'modal'"
                  size="small"
                  @update:value="updateCrudOption('modalType', $event)"
                >
                  <n-radio-button value="modal">
                    弹窗
                  </n-radio-button>
                  <n-radio-button value="drawer">
                    抽屉
                  </n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item label="弹窗宽度">
                <n-input
                  :value="crudOptions.modalWidth || '900px'"
                  placeholder="900px"
                  @update:value="updateCrudOption('modalWidth', $event || '900px')"
                />
              </n-form-item>
              <n-form-item label="详情弹窗宽度">
                <n-input
                  :value="crudOptions.detailModalWidth || 'min(1080px, 92vw)'"
                  placeholder="min(1080px, 92vw)"
                  @update:value="updateCrudOption('detailModalWidth', $event || 'min(1080px, 92vw)')"
                />
              </n-form-item>
              <n-form-item label="每页条数">
                <n-input-number
                  :value="crudOptions.pageSize || 10"
                  :min="1"
                  :max="200"
                  @update:value="updateCrudOption('pageSize', $event || 10)"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                开关
              </div>
              <div class="switch-list">
                <label v-for="item in crudSwitchFields" :key="item.key">
                  <span>{{ item.label }}</span>
                  <n-switch
                    size="small"
                    :value="resolveCrudSwitchValue(item.key, item.defaultValue)"
                    @update:value="updateCrudOption(item.key, $event)"
                  />
                </label>
              </div>
            </section>
          </n-form>
        </n-drawer-content>
      </n-drawer>

      <n-drawer
        v-if="isCrudBlock"
        v-model:show="crudFieldDrawerVisible"
        :width="420"
        placement="right"
        :trap-focus="false"
        :block-scroll="false"
      >
        <n-drawer-content :title="`${editingCrudField?.label || '字段'} 配置`" closable>
          <n-form label-placement="top" :show-feedback="false" class="property-form drawer-property-form">
            <section class="panel-item">
              <div class="panel-item-title">
                查询条件
              </div>
              <n-form-item label="查询标签">
                <n-input
                  :value="editingCrudConfig.search?.label || ''"
                  clearable
                  placeholder="默认使用字段名称"
                  @update:value="updateEditingCrudFieldConfig('search', { label: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="查询占位提示">
                <n-input
                  :value="editingCrudConfig.search?.placeholder || ''"
                  clearable
                  placeholder="默认使用字段占位提示"
                  @update:value="updateEditingCrudFieldConfig('search', { placeholder: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="查询控件跨度">
                <n-input-number
                  :value="editingCrudConfig.search?.span || editingCrudField?.layout?.span || 1"
                  :min="1"
                  :max="maxFormGridColumns"
                  @update:value="updateEditingCrudFieldConfig('search', { span: $event || 1 })"
                />
              </n-form-item>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                表格列
              </div>
              <n-form-item label="列标题">
                <n-input
                  :value="editingCrudConfig.table?.title || ''"
                  clearable
                  placeholder="默认使用字段名称"
                  @update:value="updateEditingCrudFieldConfig('table', { title: $event || undefined })"
                />
              </n-form-item>
              <div class="crud-inline-grid">
                <n-form-item label="列宽">
                  <n-input-number
                    :value="editingCrudConfig.table?.width"
                    clearable
                    :min="60"
                    :max="800"
                    @update:value="updateEditingCrudFieldConfig('table', { width: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="最小宽度">
                  <n-input-number
                    :value="editingCrudConfig.table?.minWidth || 120"
                    :min="60"
                    :max="800"
                    @update:value="updateEditingCrudFieldConfig('table', { minWidth: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="对齐方式">
                  <n-select
                    :value="editingCrudConfig.table?.align || 'left'"
                    :options="tableAlignOptions"
                    @update:value="updateEditingCrudFieldConfig('table', { align: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="固定列">
                  <n-select
                    :value="editingCrudConfig.table?.fixed || ''"
                    :options="tableFixedOptions"
                    @update:value="updateEditingCrudFieldConfig('table', { fixed: $event || undefined })"
                  />
                </n-form-item>
              </div>
              <div class="switch-list">
                <label>
                  <span>文字省略</span>
                  <n-switch
                    size="small"
                    :value="editingCrudConfig.table?.ellipsis !== false"
                    @update:value="updateEditingCrudFieldConfig('table', { ellipsis: $event })"
                  />
                </label>
                <label>
                  <span>可排序</span>
                  <n-switch
                    size="small"
                    :value="!!editingCrudConfig.table?.sorter"
                    @update:value="updateEditingCrudFieldConfig('table', { sorter: $event })"
                  />
                </label>
              </div>
            </section>

            <section class="panel-item">
              <div class="panel-item-title">
                编辑弹窗
              </div>
              <n-form-item label="编辑标签">
                <n-input
                  :value="editingCrudConfig.edit?.label || ''"
                  clearable
                  placeholder="默认使用字段名称"
                  @update:value="updateEditingCrudFieldConfig('edit', { label: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="编辑占位提示">
                <n-input
                  :value="editingCrudConfig.edit?.placeholder || ''"
                  clearable
                  placeholder="默认使用字段占位提示"
                  @update:value="updateEditingCrudFieldConfig('edit', { placeholder: $event || undefined })"
                />
              </n-form-item>
              <n-form-item label="编辑控件跨度">
                <n-input-number
                  :value="editingCrudConfig.edit?.span || editingCrudField?.layout?.span || 1"
                  :min="1"
                  :max="maxFormGridColumns"
                  @update:value="updateEditingCrudFieldConfig('edit', { span: $event || 1 })"
                />
              </n-form-item>
              <div class="switch-list">
                <label>
                  <span>只读</span>
                  <n-switch
                    size="small"
                    :value="!!editingCrudConfig.edit?.readonly"
                    @update:value="updateEditingCrudFieldConfig('edit', { readonly: $event })"
                  />
                </label>
              </div>
            </section>
          </n-form>
        </n-drawer-content>
      </n-drawer>

      <n-drawer
        v-model:show="componentPropsVisible"
        :width="380"
        placement="right"
        :trap-focus="false"
        :block-scroll="false"
      >
        <n-drawer-content :title="`${selectedLabel} 更多属性`" closable>
          <n-form label-placement="top" :show-feedback="false" class="property-form drawer-property-form">
            <section v-for="propGroup in activePropGroups" :key="propGroup.title" class="panel-item">
              <div class="panel-item-title">
                {{ propGroup.title }}
              </div>
              <n-form-item v-for="item in propGroup.fields" :key="item.key" :label="item.label || item.key">
                <n-switch
                  v-if="item.type === 'boolean'"
                  size="small"
                  :value="resolvePropValue(item)"
                  @update:value="updateSelectedProp(item, $event)"
                />
                <n-input-number
                  v-else-if="item.type === 'number'"
                  :value="resolvePropValue(item)"
                  clearable
                  :min="item.min"
                  :max="item.max"
                  :step="item.step || 1"
                  @update:value="updateSelectedProp(item, $event)"
                />
                <n-select
                  v-else-if="item.type === 'select'"
                  :value="resolvePropValue(item)"
                  clearable
                  :options="item.options"
                  @update:value="updateSelectedProp(item, $event)"
                />
                <n-select
                  v-else-if="item.type === 'multiSelect'"
                  multiple
                  clearable
                  :value="resolvePropValue(item) || []"
                  :options="item.options"
                  @update:value="updateSelectedProp(item, $event?.length ? $event : undefined)"
                />
                <n-input
                  v-else-if="item.type === 'json'"
                  :value="stringifyJsonProp(resolvePropValue(item))"
                  type="textarea"
                  :autosize="{ minRows: 3, maxRows: 8 }"
                  placeholder="JSON"
                  @update:value="updateJsonProp(item, $event)"
                />
                <n-input
                  v-else
                  :value="resolvePropValue(item)"
                  clearable
                  :placeholder="item.placeholder"
                  @update:value="updateSelectedProp(item, $event || undefined)"
                />
              </n-form-item>
            </section>
          </n-form>
        </n-drawer-content>
      </n-drawer>
    </template>

    <n-tabs v-else v-model:value="formPropertyActiveTab" type="line" size="medium" animated class="property-tabs form-property-tabs">
      <n-tab-pane name="basic" tab="基础配置">
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <n-collapse class="form-property-collapse" :default-expanded-names="['assets']">
            <n-collapse-item title="表单资产" name="assets">
              <section class="panel-item form-asset-panel">
                <div class="panel-title-row">
                  <div>
                    <div class="panel-item-title">
                      表单资产
                    </div>
                    <p class="panel-item-desc">
                      一个业务对象可以维护多个表单，弹窗、详情和流程可以引用不同表单。
                    </p>
                  </div>
                  <div class="form-asset-actions">
                    <button type="button" class="form-asset-icon-button" title="新建空白表单" @click="createBlankFormAsset">
                      <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M11 4a1 1 0 1 1 2 0v7h7a1 1 0 1 1 0 2h-7v7a1 1 0 1 1-2 0v-7H4a1 1 0 1 1 0-2h7V4Z" fill="currentColor" />
                      </svg>
                      新建
                    </button>
                    <button type="button" class="form-asset-icon-button" title="复制当前表单" @click="duplicateCurrentFormAsset">
                      <svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M9 3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V4h-9a1 1 0 0 1-1-1Z" fill="currentColor" />
                        <path d="M5 6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2H5Zm0 2h10v12H5V8Z" fill="currentColor" />
                      </svg>
                      复制
                    </button>
                  </div>
                </div>
                <div :key="schema.formKey" class="current-form-banner">
                  <span>正在编辑</span>
                  <strong>{{ schema.formName || '主表单' }}</strong>
                </div>
                <div class="form-asset-tabs">
                  <button type="button" class="form-asset-tab active" @click="showFormSettings">
                    <em>1</em>
                    <span>{{ schema.formName || '主表单' }}</span>
                    <strong>当前</strong>
                  </button>
                  <button
                    v-for="(asset, assetIndex) in formAssets"
                    :key="asset.formKey"
                    type="button"
                    class="form-asset-tab"
                    @click="switchFormAsset(asset.formKey)"
                  >
                    <em>{{ assetIndex + 2 }}</em>
                    <span>{{ asset.formName || `表单 ${assetIndex + 2}` }}</span>
                  </button>
                </div>
                <div class="form-asset-edit-grid">
                  <n-form-item label="当前表单名称">
                    <n-input
                      :value="schema.formName || ''"
                      placeholder="请输入表单名称"
                      @update:value="updateCurrentFormMeta({ formName: $event || '业务表单' })"
                    />
                  </n-form-item>
                  <n-form-item label="当前表单编码">
                    <n-input
                      :value="schema.formKey || ''"
                      placeholder="form_key"
                      @update:value="updateCurrentFormMeta({ formKey: $event || schema.formKey })"
                    />
                  </n-form-item>
                </div>
                <div v-if="formAssets.length" class="form-asset-list">
                  <div v-for="asset in formAssets" :key="asset.formKey" class="form-asset-card">
                    <div class="form-asset-main">
                      <n-input
                        :value="asset.formName"
                        size="small"
                        placeholder="表单名称"
                        @update:value="updateFormAssetMeta(asset.formKey, { formName: $event || '未命名表单' })"
                      />
                      <button type="button" class="form-asset-switch" @click="switchFormAsset(asset.formKey)">
                        切换编辑
                      </button>
                    </div>
                    <n-button size="tiny" quaternary type="error" @click="removeFormAsset(asset.formKey)">
                      删除
                    </n-button>
                  </div>
                </div>
              </section>
            </n-collapse-item>

            <n-collapse-item title="AiForm 布局" name="layout">
              <section class="panel-item">
                <div class="panel-item-title">
                  AiForm 布局
                </div>
                <n-form-item label="编辑打开方式">
                  <n-radio-group
                    :value="schema.layout?.modalType || 'modal'"
                    size="small"
                    @update:value="updateFormLayout({ modalType: $event || 'modal' })"
                  >
                    <n-radio-button value="modal">
                      弹出框
                    </n-radio-button>
                    <n-radio-button value="drawer">
                      抽屉
                    </n-radio-button>
                  </n-radio-group>
                </n-form-item>
                <n-form-item label="表单列数">
                  <div class="slider-control">
                    <n-slider
                      :value="normalizedFormGridColumns"
                      :min="1"
                      :max="maxFormGridColumns"
                      :step="1"
                      :marks="gridColumnMarks"
                      @update:value="updateFormLayout({ gridColumns: $event, gridCols: $event })"
                    />
                    <n-input-number
                      :value="normalizedFormGridColumns"
                      :min="1"
                      :max="maxFormGridColumns"
                      :show-button="false"
                      size="small"
                      @update:value="updateFormLayout({ gridColumns: $event || 1, gridCols: $event || 1 })"
                    />
                  </div>
                </n-form-item>
                <n-form-item label="表单大小">
                  <n-select
                    :value="schema.layout?.size || 'medium'"
                    :options="componentSizeOptions.filter(item => item.value)"
                    @update:value="updateFormLayout({ size: $event || 'medium' })"
                  />
                </n-form-item>
                <n-form-item label="标签位置">
                  <n-radio-group :value="schema.layout?.labelPlacement || 'left'" size="small" @update:value="updateFormLayout({ labelPlacement: $event })">
                    <n-radio-button value="left">
                      左侧
                    </n-radio-button>
                    <n-radio-button value="top">
                      顶部
                    </n-radio-button>
                  </n-radio-group>
                </n-form-item>
                <n-form-item label="标签对齐">
                  <n-radio-group :value="schema.layout?.labelAlign || 'right'" size="small" @update:value="updateFormLayout({ labelAlign: $event })">
                    <n-radio-button value="left">
                      左对齐
                    </n-radio-button>
                    <n-radio-button value="right">
                      右对齐
                    </n-radio-button>
                  </n-radio-group>
                </n-form-item>
                <n-form-item label="标签宽度">
                  <n-input
                    :value="String(schema.layout?.labelWidth ?? 'auto')"
                    placeholder="auto / 100"
                    @update:value="updateFormLayout({ labelWidth: normalizeLabelWidthInput($event) })"
                  />
                </n-form-item>
              </section>
            </n-collapse-item>

            <n-collapse-item title="间距与反馈" name="spacing">
              <section class="panel-item">
                <div class="panel-item-title">
                  间距与反馈
                </div>
                <n-form-item label="行间距">
                  <n-input-number
                    :value="schema.layout?.rowGap || 16"
                    :min="0"
                    :max="48"
                    @update:value="updateFormLayout({ rowGap: $event || 16, yGap: $event || 16 })"
                  />
                </n-form-item>
                <n-form-item label="列间距">
                  <n-input-number
                    :value="schema.layout?.columnGap || 16"
                    :min="0"
                    :max="48"
                    @update:value="updateFormLayout({ columnGap: $event || 16, xGap: $event || 16 })"
                  />
                </n-form-item>
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
                  <label>
                    <span>启用折叠</span>
                    <n-switch
                      size="small"
                      :value="!!schema.layout?.enableCollapse"
                      @update:value="updateFormLayout({ enableCollapse: $event })"
                    />
                  </label>
                </div>
                <n-form-item label="最大显示字段数">
                  <n-input-number
                    :value="schema.layout?.maxVisibleFields || 6"
                    :min="1"
                    :max="50"
                    @update:value="updateFormLayout({ maxVisibleFields: $event || 6 })"
                  />
                </n-form-item>
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

      <n-tab-pane name="style" tab="样式配置">
        <n-form label-placement="top" :show-feedback="false" class="property-form">
          <n-collapse class="form-property-collapse" :default-expanded-names="['appearance']">
            <n-collapse-item title="表单外观" name="appearance">
              <section class="panel-item">
                <div class="panel-item-title">
                  表单外观
                </div>
                <n-form-item label="背景">
                  <div class="color-control">
                    <n-color-picker
                      :value="formStyle.backgroundColor || ''"
                      :show-alpha="true"
                      :modes="['hex']"
                      :swatches="colorSwatches"
                      @update:value="updateFormStyle({ backgroundColor: $event || undefined })"
                    />
                    <n-button size="small" quaternary @click="updateFormStyle({ backgroundColor: undefined })">
                      默认
                    </n-button>
                  </div>
                </n-form-item>
                <n-form-item label="圆角">
                  <n-input-number
                    :value="resolvePxNumber(formStyle.borderRadius, 0)"
                    :min="0"
                    :max="32"
                    @update:value="updateFormStyle({ borderRadius: `${$event ?? 0}px` })"
                  />
                </n-form-item>
                <n-form-item label="边框颜色">
                  <div class="color-control">
                    <n-color-picker
                      :value="formStyle.borderColor || ''"
                      :show-alpha="true"
                      :modes="['hex']"
                      :swatches="colorSwatches"
                      @update:value="updateFormStyle({ borderColor: $event || undefined })"
                    />
                    <n-button size="small" quaternary @click="updateFormStyle({ borderColor: undefined })">
                      默认
                    </n-button>
                  </div>
                </n-form-item>
                <n-form-item label="阴影">
                  <n-select
                    :value="formStyle.boxShadow || ''"
                    :options="shadowOptions"
                    @update:value="updateFormStyle({ boxShadow: $event || undefined })"
                  />
                </n-form-item>
                <n-form-item label="透明度">
                  <n-slider
                    :value="formOpacityPercent"
                    :min="20"
                    :max="100"
                    :step="5"
                    @update:value="updateFormStyle({ opacity: Number($event || 100) / 100 })"
                  />
                </n-form-item>
              </section>
            </n-collapse-item>

            <n-collapse-item title="内外边距" name="spacing">
              <section class="panel-item">
                <div class="panel-item-title">
                  内外边距
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
                    placeholder="例如 padding:12px; background:#fff;"
                    @update:value="updateFormStyleText"
                  />
                </n-form-item>
              </section>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </n-tab-pane>

      <n-tab-pane name="source" tab="源码">
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
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { cloneValue, findDesignerComponentPath, getDesignerComponent, isFieldComponent, isLayoutComponent, normalizeFormDesignerSchema, updateDesignerComponent, updateDesignerLayout } from '../form-first/formDesignerSchema'

const props = defineProps({
  schema: {
    type: Object,
    required: true,
  },
  selectedId: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:schema', 'update:selectedId', 'close'])

const advancedConfigVisible = ref(false)
const componentPropsVisible = ref(false)
const crudFieldDrawerVisible = ref(false)
const editingCrudFieldId = ref('')
const propertyActiveTab = ref('basic')
const formPropertyActiveTab = ref('basic')
const selectedCodeDraft = ref('')
const selectedCodeDraftTarget = ref('')
const schemaCodeDraft = ref('')
const schemaCodeDirty = ref(false)
const sourceError = ref('')
const selectedComponent = computed(() => getDesignerComponent(props.schema, props.selectedId))
const isField = computed(() => selectedComponent.value ? isFieldComponent(selectedComponent.value) : false)
const isLayout = computed(() => selectedComponent.value ? isLayoutComponent(selectedComponent.value) : false)
const isCrudBlock = computed(() => ['AiCrudPage', 'crudBlock'].includes(selectedComponent.value?.componentKey))
const isRowLayout = computed(() => ['row', 'fcRow'].includes(selectedComponent.value?.componentKey))
const isColumnLayout = computed(() => selectedComponent.value?.componentKey === 'col')
const isCardLayout = computed(() => ['card', 'elCard'].includes(selectedComponent.value?.componentKey))
const isTabsLayout = computed(() => ['tabs', 'elTabs'].includes(selectedComponent.value?.componentKey))
const isCollapseLayout = computed(() => ['collapse', 'elCollapse'].includes(selectedComponent.value?.componentKey))
const isButtonComponent = computed(() => ['button', 'elButton'].includes(selectedComponent.value?.componentKey))
const selectedLabel = computed(() => selectedComponent.value?.label || selectedComponent.value?.props?.header || selectedComponent.value?.props?.title || '未命名组件')
const componentTypeLabel = computed(() => isCrudBlock.value ? '系统 AiCrudPage 组件' : isField.value ? 'AiForm 字段组件' : isLayout.value ? '布局组件' : selectedComponent.value?.componentKey || '组件')
const panelDescription = computed(() => selectedComponent.value ? componentTypeLabel.value : 'AiForm / AiCrudPage 通用配置')
const crudApiConfig = computed(() => selectedComponent.value?.props?.apiConfig || {})
const crudOptions = computed(() => selectedComponent.value?.props?.crudOptions || {})
const selectedDesignerStyle = computed(() => selectedComponent.value?.props?.__designerStyle || {})
const formStyle = computed(() => props.schema.layout?.formStyle || {})
const formAssets = computed(() => Array.isArray(props.schema.settings?.formAssets) ? props.schema.settings.formAssets : [])
const formAssetOptions = computed(() => [
  { label: `${props.schema.formName || '主表单'}（当前表单）`, value: 'current' },
  ...formAssets.value.map(asset => ({
    label: `${asset.formName || asset.formKey}（${asset.formKey}）`,
    value: asset.formKey,
  })),
])
const layoutChildren = computed(() => selectedComponent.value?.children || [])
const interactionRules = computed(() => Array.isArray(selectedComponent.value?.props?.__events) ? selectedComponent.value.props.__events : [])
const defaultTrigger = computed(() => selectedComponent.value?.componentKey === 'button' ? 'click' : 'change')
const componentTargetOptions = computed(() => collectComponentTargetOptions(props.schema.components || []))
const selectedComponentCodeRaw = computed(() => JSON.stringify(selectedComponent.value || {}, null, 2))
const selectedCodeText = computed(() => selectedCodeDraftTarget.value === props.selectedId ? selectedCodeDraft.value : selectedComponentCodeRaw.value)
const schemaCodeRaw = computed(() => JSON.stringify(props.schema || {}, null, 2))
const schemaCodeText = computed(() => schemaCodeDirty.value ? schemaCodeDraft.value : schemaCodeRaw.value)
const selectedOpacityPercent = computed(() => Math.round(Number(selectedDesignerStyle.value.opacity ?? 1) * 100))
const formOpacityPercent = computed(() => Math.round(Number(formStyle.value.opacity ?? 1) * 100))
const maxFormGridColumns = 6
const normalizedFormGridColumns = computed(() => normalizeGridCount(props.schema.layout?.gridColumns || 2))
const isDatePickerField = computed(() => ['date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'quarter'].includes(selectedComponent.value?.componentKey))
const isTimePickerField = computed(() => ['time', 'timerange'].includes(selectedComponent.value?.componentKey))
const isTemporalField = computed(() => isDatePickerField.value || isTimePickerField.value)
const datePickerType = computed(() => {
  const key = selectedComponent.value?.componentKey
  const map = {
    date: 'date',
    datetime: 'datetime',
    daterange: 'daterange',
    datetimerange: 'datetimerange',
    month: 'month',
    year: 'year',
    quarter: 'quarter',
  }
  return map[key] || 'date'
})

const basicExpandedNames = ['identity']
const gridColumnOptions = Array.from({ length: maxFormGridColumns }).map((_, index) => index + 1)
const gridColumnMarks = gridColumnOptions.reduce((marks, item) => {
  marks[item] = `${item}`
  return marks
}, {})
const componentSizeOptions = [
  { label: '默认', value: '' },
  { label: '小', value: 'small' },
  { label: '中', value: 'medium' },
  { label: '大', value: 'large' },
]
const buttonTypeOptions = [
  { label: '默认 default', value: 'default' },
  { label: '主要 primary', value: 'primary' },
  { label: '信息 info', value: 'info' },
  { label: '成功 success', value: 'success' },
  { label: '警告 warning', value: 'warning' },
  { label: '错误 error', value: 'error' },
]
const cardSizeOptions = [
  { label: '小 small', value: 'small' },
  { label: '中 medium', value: 'medium' },
  { label: '大 large', value: 'large' },
  { label: '巨大 huge', value: 'huge' },
]
const tabsTypeOptions = [
  { label: 'line', value: 'line' },
  { label: 'bar', value: 'bar' },
  { label: 'card', value: 'card' },
  { label: 'segment', value: 'segment' },
]
const tabsPlacementOptions = [
  { label: 'top', value: 'top' },
  { label: 'bottom', value: 'bottom' },
  { label: 'left', value: 'left' },
  { label: 'right', value: 'right' },
]
const tabsTriggerOptions = [
  { label: 'click', value: 'click' },
  { label: 'hover', value: 'hover' },
]
const collapseArrowPlacementOptions = [
  { label: 'left', value: 'left' },
  { label: 'right', value: 'right' },
]
const collapseDisplayDirectiveOptions = [
  { label: 'if', value: 'if' },
  { label: 'show', value: 'show' },
]
const collapseTriggerAreaOptions = [
  { label: 'main', value: 'main' },
  { label: 'arrow', value: 'arrow' },
  { label: 'extra', value: 'extra' },
]
const datePickerTypeOptions = [
  { label: 'date', value: 'date' },
  { label: 'datetime', value: 'datetime' },
  { label: 'daterange', value: 'daterange' },
  { label: 'datetimerange', value: 'datetimerange' },
  { label: 'month', value: 'month' },
  { label: 'year', value: 'year' },
  { label: 'quarter', value: 'quarter' },
]
const pickerPlacementOptions = [
  { label: 'bottom-start', value: 'bottom-start' },
  { label: 'bottom', value: 'bottom' },
  { label: 'bottom-end', value: 'bottom-end' },
  { label: 'top-start', value: 'top-start' },
  { label: 'top', value: 'top' },
  { label: 'top-end', value: 'top-end' },
]
const pickerActionOptions = [
  { label: 'clear', value: 'clear' },
  { label: 'now', value: 'now' },
  { label: 'confirm', value: 'confirm' },
]
const booleanValueOptions = [
  { label: 'true', value: true },
  { label: 'false', value: false },
]
const tableAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
]
const tableFixedOptions = [
  { label: '不固定', value: '' },
  { label: '左固定', value: 'left' },
  { label: '右固定', value: 'right' },
]
const triggerOptions = [
  { label: '点击（click）', value: 'click' },
  { label: '值变化（change）', value: 'change' },
  { label: '获得焦点（focus）', value: 'focus' },
  { label: '失去焦点（blur）', value: 'blur' },
  { label: '清空（clear）', value: 'clear' },
  { label: '初始化（mounted）', value: 'mounted' },
]
const actionOptions = [
  { label: '设置目标值（setValue）', value: 'setValue' },
  { label: '清空目标值（clearValue）', value: 'clearValue' },
  { label: '设置下拉选项（setOptions）', value: 'setOptions' },
  { label: '显示/隐藏（showHide）', value: 'showHide' },
  { label: '启用/禁用（enableDisable）', value: 'enableDisable' },
  { label: '打开弹窗（openModal）', value: 'openModal' },
  { label: '调用接口（apiRequest）', value: 'apiRequest' },
]
const modalContentModeOptions = [
  { label: '复用当前表单', value: 'currentForm' },
  { label: '引用表单资产', value: 'formAsset' },
  { label: '选择画布组件', value: 'component' },
  { label: '空白弹窗，后续配置', value: 'empty' },
]
const interactionPresets = [
  { key: 'cascade', title: '下拉联动', description: '当前字段变化后，更新另一个下拉的选项。' },
  { key: 'openModal', title: '打开弹窗', description: '按钮点击后打开表单弹窗或业务弹窗。' },
  { key: 'showHide', title: '显示隐藏', description: '根据当前值控制另一个字段是否显示。' },
  { key: 'apiRequest', title: '调用接口', description: '点击按钮或值变化后提交接口请求。' },
]
const propChineseLabels = {
  size: '组件尺寸',
  clearable: '可清空',
  disabled: '禁用',
  text: '按钮文字',
  secondary: '次要按钮',
  tertiary: '三级按钮',
  quaternary: '四级按钮',
  dashed: '虚线按钮',
  loading: '加载中',
  block: '块级按钮',
  placeholder: '占位提示',
  status: '校验状态',
  maxlength: '最大长度',
  showCount: '显示字数',
  round: '圆角',
  readonly: '只读',
  inputProps: '原生输入属性',
  rows: '行数',
  autosize: '自适应高度',
  min: '最小值',
  max: '最大值',
  step: '步长',
  precision: '精度',
  showButton: '显示加减按钮',
  multiple: '多选',
  filterable: '可搜索',
  remote: '远程搜索',
  loading: '加载中',
  maxTagCount: '最多显示标签数',
  placement: '弹出位置',
  fallbackOption: '保留回填选项',
  dictType: '字典类型',
  name: '表单控件名称',
  defaultValue: '默认值',
  value: '受控值',
  checkedValue: '选中值',
  uncheckedValue: '未选中值',
  checkedText: '选中文案',
  uncheckedText: '未选中文案',
  rubberBand: '橡皮筋动效',
  range: '范围选择',
  vertical: '垂直显示',
  tooltip: '显示提示',
  marks: '刻度标记',
  count: '数量',
  allowHalf: '允许半选',
  showAlpha: '显示透明度',
  modes: '颜色模式',
  swatches: '预设色',
  actions: '底部动作',
  cascade: '级联勾选',
  checkStrategy: '勾选策略',
  showPath: '显示路径',
  checkable: '显示复选框',
  bordered: '显示边框',
  embedded: '嵌入模式',
  segmented: '分段线',
  hoverable: '悬浮阴影',
  contentScrollable: '内容可滚动',
  role: 'ARIA 角色',
  headerStyle: '头部样式',
  contentStyle: '内容样式',
  footerStyle: '底部样式',
  type: '类型',
  trigger: '触发方式',
  animated: '动画',
  closable: '可关闭',
  addable: '可新增',
  justifyContent: '对齐分布',
  tabsPadding: '标签栏内边距',
  paneStyle: '面板样式',
  tabStyle: '标签样式',
  accordion: '手风琴模式',
  arrowPlacement: '箭头位置',
  displayDirective: '渲染策略',
  triggerAreas: '可触发区域',
  defaultExpandedNames: '默认展开项',
  expandedNames: '受控展开项',
  format: '显示格式',
  valueFormat: '值格式',
  inputReadonly: '输入框只读',
  closeOnSelect: '选择后关闭',
  updateValueOnClose: '关闭时更新值',
  defaultTime: '默认时间',
  separator: '分隔符',
  startPlaceholder: '开始占位',
  endPlaceholder: '结束占位',
  firstDayOfWeek: '每周起始日',
  showIcon: '显示图标',
  hours: '小时选项',
  minutes: '分钟选项',
  seconds: '秒选项',
}
const groupChineseLabels = {
  'NInput Props': '输入框属性',
  'NInput Textarea Props': '多行文本属性',
  'NInputNumber Props': '数字输入属性',
  'NSelect Props': '下拉选择属性',
  'DictSelect / NSelect Props': '字典下拉属性',
  'RadioGroup Props': '单选组属性',
  'CheckboxGroup Props': '多选组属性',
  'NSwitch Props': '开关属性',
  'NSlider Props': '滑块属性',
  'NRate Props': '评分属性',
  'NColorPicker Props': '颜色选择属性',
  'NCascader Props': '级联选择属性',
  'NTreeSelect Props': '树形选择属性',
  'Card Props': '卡片属性',
  'Tabs Props': '标签页属性',
  'Collapse Props': '折叠面板属性',
  'DatePicker Props': '日期选择属性',
  'TimePicker Props': '时间选择属性',
  'Button Props': '按钮属性',
}
const colorSwatches = ['#ffffff', '#f8fafc', '#eff6ff', '#ecfdf5', '#fffbeb', '#fef2f2', '#dbe3ee', '#94a3b8', '#2563eb', '#16a34a', '#f59e0b', '#dc2626', 'rgba(255, 255, 255, 0)']
const shadowOptions = [
  { label: '无', value: '' },
  { label: '轻微', value: '0 4px 12px rgba(15, 23, 42, 0.06)' },
  { label: '标准', value: '0 10px 24px rgba(15, 23, 42, 0.10)' },
  { label: '强调', value: '0 16px 36px rgba(15, 23, 42, 0.16)' },
]
const spacingSides = [
  { key: 'Top', label: '上' },
  { key: 'Right', label: '右' },
  { key: 'Bottom', label: '下' },
  { key: 'Left', label: '左' },
]
const crudApiFields = [
  { key: 'list', label: '分页列表', placeholder: 'get@/employee/page' },
  { key: 'detail', label: '详情', placeholder: 'post@/employee/getById' },
  { key: 'add', label: '新增', placeholder: 'post@/employee/add' },
  { key: 'update', label: '更新', placeholder: 'post@/employee/edit' },
  { key: 'delete', label: '删除', placeholder: 'post@/employee/remove/:id' },
  { key: 'import', label: '导入', placeholder: 'post@/employee/import' },
  { key: 'export', label: '导出', placeholder: 'post@/employee/export' },
  { key: 'importTemplate', label: '导入模板', placeholder: 'get@/employee/importTemplate' },
]
const crudSwitchFields = [
  { key: 'showSearch', label: '显示查询区', defaultValue: true },
  { key: 'searchEnableCollapse', label: '查询折叠', defaultValue: true },
  { key: 'showPagination', label: '显示分页', defaultValue: true },
  { key: 'loadDetailOnEdit', label: '编辑前加载详情', defaultValue: true },
  { key: 'hideToolbar', label: '隐藏工具栏', defaultValue: false },
  { key: 'hideAdd', label: '隐藏新增', defaultValue: false },
  { key: 'hideBatchDelete', label: '隐藏批量删除', defaultValue: false },
  { key: 'hideSelection', label: '隐藏多选', defaultValue: false },
  { key: 'striped', label: '斑马纹', defaultValue: false },
  { key: 'bordered', label: '表格边框', defaultValue: false },
  { key: 'showRenderModeSwitch', label: '列表/卡片切换', defaultValue: true },
  { key: 'showImport', label: '显示导入', defaultValue: false },
  { key: 'showExport', label: '显示导出', defaultValue: false },
  { key: 'showExportTasks', label: '导出任务入口', defaultValue: true },
  { key: 'editShowFeedback', label: '编辑校验反馈', defaultValue: true },
  { key: 'hideModalFooter', label: '隐藏弹窗底部', defaultValue: false },
]
const rowColumnCount = computed(() => normalizeGridCount(selectedComponent.value?.props?.columns || selectedComponent.value?.children?.length || props.schema.layout?.gridColumns || 2))
const crudConfigFields = computed(() => collectCrudConfigFields(selectedComponent.value?.children || []))
const editingCrudField = computed(() => editingCrudFieldId.value ? getDesignerComponent(props.schema, editingCrudFieldId.value) : null)
const editingCrudConfig = computed(() => editingCrudField.value?.props?.__crudConfig || {})
const selectedOptions = computed(() => selectedComponent.value?.props?.options || [])
const isFieldInsideCrud = computed(() => isField.value && hasAncestorComponent(props.schema, props.selectedId, ['AiCrudPage', 'crudBlock']))
const selectedCrudFieldConfig = computed(() => isFieldInsideCrud.value ? selectedComponent.value?.props?.__crudConfig || {} : null)
const isOptionField = computed(() => ['select', 'radio', 'radioButton', 'checkbox', 'cascader', 'treeSelect'].includes(selectedComponent.value?.componentKey || ''))
const activePropGroups = computed(() => buildNaivePropGroups(selectedComponent.value?.componentKey || ''))
const isDictLikeField = computed(() => {
  const key = selectedComponent.value?.componentKey || ''
  return ['select', 'dictSelect', 'radio', 'checkbox', 'cascader'].includes(key)
})

function updateComponent(patch) {
  if (!props.selectedId)
    return
  emit('update:schema', updateDesignerComponent(props.schema, props.selectedId, patch))
}

function updateFieldBindingCode(value = '') {
  const fieldCode = String(value || '').trim()
  if (!selectedComponent.value?.fieldBinding)
    return
  updateComponent({
    fieldBinding: {
      ...(selectedComponent.value.fieldBinding || {}),
      fieldCode,
      columnName: fieldCode,
    },
  })
}

function showFormSettings() {
  emit('update:selectedId', '')
  formPropertyActiveTab.value = 'basic'
  sourceError.value = ''
}

function updateCurrentFormMeta(patch = {}) {
  emit('update:schema', {
    ...props.schema,
    ...patch,
  })
}

function updateFormAssetMeta(formKey = '', patch = {}) {
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      formAssets: formAssets.value.map((asset) => {
        if (asset.formKey !== formKey)
          return asset
        return {
          ...asset,
          ...patch,
          schema: asset.schema
            ? {
                ...asset.schema,
                ...patch,
              }
            : asset.schema,
        }
      }),
    },
  })
}

function duplicateCurrentFormAsset() {
  const nextAssetKey = `${props.schema.formKey || 'form'}_dialog_${Date.now()}`
  const assetSchema = cloneValue(props.schema)
  assetSchema.formKey = nextAssetKey
  assetSchema.formName = `${props.schema.formName || '表单'}弹窗`
  assetSchema.settings = {
    ...(assetSchema.settings || {}),
    formAssets: [],
  }
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function createBlankFormAsset() {
  const nextAssetIndex = formAssets.value.length + 2
  const nextAssetKey = `${props.schema.formKey || 'form'}_form_${Date.now()}`
  const assetSchema = {
    ...cloneValue(props.schema),
    formKey: nextAssetKey,
    formName: `表单 ${nextAssetIndex}`,
    components: [],
    settings: {
      ...(props.schema.settings || {}),
      formAssets: [],
    },
  }
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      formAssets: [
        {
          formKey: nextAssetKey,
          formName: assetSchema.formName,
          schema: assetSchema,
        },
        ...formAssets.value,
      ],
    },
  })
}

function switchFormAsset(formKey = '') {
  const asset = formAssets.value.find(item => item.formKey === formKey)
  if (!asset?.schema)
    return
  const currentAsset = {
    formKey: props.schema.formKey,
    formName: props.schema.formName,
    schema: {
      ...cloneValue(props.schema),
      settings: {
        ...(props.schema.settings || {}),
        formAssets: [],
      },
    },
  }
  const nextAssets = formAssets.value
    .filter(item => item.formKey !== formKey && item.formKey !== currentAsset.formKey)
    .concat(currentAsset)
  emit('update:selectedId', '')
  emit('update:schema', normalizeFormDesignerSchema({
    ...cloneValue(asset.schema),
    settings: {
      ...(asset.schema.settings || {}),
      formAssets: nextAssets,
    },
  }))
}

function removeFormAsset(formKey = '') {
  emit('update:schema', {
    ...props.schema,
    settings: {
      ...(props.schema.settings || {}),
      formAssets: formAssets.value.filter(item => item.formKey !== formKey),
    },
  })
}

function updateLabel(value) {
  const patch = { label: value }
  if (isCrudBlock.value) {
    patch.props = {
      title: value,
    }
  }
  else if (isLayout.value) {
    patch.props = {
      header: value,
      title: value,
      label: value,
    }
  }
  updateComponent(patch)
}

function addLayoutChild(componentKey = '') {
  if (!selectedComponent.value)
    return
  const nextChildren = cloneValue(layoutChildren.value || [])
  const index = nextChildren.length + 1
  const isTabsChild = componentKey === 'tabPane'
  const label = isTabsChild ? `标签 ${index}` : `分组 ${index}`
  const id = `${selectedComponent.value.id}_${isTabsChild ? 'pane' : 'item'}_${Date.now()}`
  nextChildren.push({
    id,
    componentKey: isTabsChild ? 'tabPane' : 'collapseItem',
    label,
    props: isTabsChild ? { label, name: id } : { title: label, name: id },
    layout: {
      span: selectedComponent.value.layout?.span || normalizedFormGridColumns.value,
      align: 'left',
    },
    children: [],
  })
  updateComponent({ children: nextChildren })
  emit('update:selectedId', id)
}

function updateLayoutChild(index, patch = {}) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  if (!nextChildren[index])
    return
  nextChildren[index] = {
    ...nextChildren[index],
    ...patch,
    props: patch.props
      ? {
          ...(nextChildren[index].props || {}),
          ...patch.props,
        }
      : nextChildren[index].props,
    layout: patch.layout
      ? {
          ...(nextChildren[index].layout || {}),
          ...patch.layout,
        }
      : nextChildren[index].layout,
  }
  updateComponent({ children: nextChildren })
}

function moveLayoutChild(index, delta) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  const nextIndex = index + delta
  if (!nextChildren[index] || nextIndex < 0 || nextIndex >= nextChildren.length)
    return
  const [item] = nextChildren.splice(index, 1)
  nextChildren.splice(nextIndex, 0, item)
  updateComponent({ children: nextChildren })
}

function removeLayoutChild(index) {
  const nextChildren = cloneValue(layoutChildren.value || [])
  if (nextChildren.length <= 1 || !nextChildren[index])
    return
  const [removed] = nextChildren.splice(index, 1)
  updateComponent({ children: nextChildren })
  if (removed?.id === props.selectedId)
    emit('update:selectedId', selectedComponent.value?.id || '')
}

function addInteractionRule() {
  const nextRules = cloneValue(interactionRules.value || [])
  nextRules.push({
    id: `evt_${Date.now()}`,
    trigger: defaultTrigger.value,
    action: defaultTrigger.value === 'click' ? 'openModal' : 'setValue',
    targetId: '',
    condition: '',
  })
  updateComponent({ props: { __events: nextRules } })
}

function addInteractionPreset(key = '') {
  const nextRules = cloneValue(interactionRules.value || [])
  const base = {
    id: `evt_${Date.now()}`,
    trigger: defaultTrigger.value,
    action: 'setValue',
    targetId: '',
    whenValue: '',
  }
  const map = {
    cascade: {
      trigger: 'change',
      action: 'setOptions',
      api: 'get@/api/options?parent=:value',
    },
    openModal: {
      trigger: 'click',
      action: 'openModal',
      modalTitle: '业务弹窗',
      modalContentMode: 'currentForm',
    },
    showHide: {
      trigger: 'change',
      action: 'showHide',
      value: 'true',
    },
    apiRequest: {
      trigger: defaultTrigger.value,
      action: 'apiRequest',
      api: 'post@/api/action',
    },
  }
  nextRules.push({
    ...base,
    ...(map[key] || {}),
  })
  updateComponent({ props: { __events: nextRules } })
}

function updateInteractionRule(index, patch = {}) {
  const nextRules = cloneValue(interactionRules.value || [])
  if (!nextRules[index])
    return
  nextRules[index] = {
    ...nextRules[index],
    ...patch,
  }
  updateComponent({ props: { __events: nextRules } })
}

function updateInteractionRuleJson(index, key, value = '') {
  const patch = { [`${key}Json`]: value }
  try {
    patch[key] = value?.trim() ? JSON.parse(value) : undefined
  }
  catch {
    patch[key] = value
  }
  updateInteractionRule(index, patch)
}

function removeInteractionRule(index) {
  const nextRules = cloneValue(interactionRules.value || [])
  nextRules.splice(index, 1)
  updateComponent({ props: { __events: nextRules } })
}

function resolveTriggerLabel(value = '') {
  return triggerOptions.find(item => item.value === value)?.label || '事件规则'
}

function resolveActionLabel(value = '') {
  return actionOptions.find(item => item.value === value)?.label || '未选择动作'
}

function resolveActionValueMeta(action = '') {
  const map = {
    setValue: {
      label: '要填入目标的值',
      placeholder: '例如：已处理',
      help: '触发后会把这个值写入目标组件。',
    },
    showHide: {
      label: '是否显示目标',
      placeholder: 'true 显示，false 隐藏',
      help: '用于根据当前字段值显示或隐藏目标组件。',
    },
    enableDisable: {
      label: '是否启用目标',
      placeholder: 'true 启用，false 禁用',
      help: '用于根据当前字段值启用或禁用目标组件。',
    },
  }
  return map[action] || {
    label: '动作值',
    placeholder: '请输入',
    help: '这条动作执行时使用的值。',
  }
}

function openCrudFieldDrawer(componentId = '') {
  editingCrudFieldId.value = componentId
  crudFieldDrawerVisible.value = true
}

function updateEditingCrudFieldConfig(area, patch = {}) {
  if (!editingCrudFieldId.value)
    return
  updateCrudFieldConfigById(editingCrudFieldId.value, area, patch)
}

function collectComponentTargetOptions(components = [], result = [], depth = 0) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component?.id)
      return
    const prefix = depth ? `${'　'.repeat(depth)}` : ''
    result.push({
      label: `${prefix}${component.label || component.props?.label || component.props?.title || component.componentKey} (${component.fieldBinding?.fieldCode || component.id})`,
      value: component.id,
    })
    collectComponentTargetOptions(component.children || [], result, depth + 1)
  })
  return result
}

function updateSelectedCodeDraft(value) {
  selectedCodeDraftTarget.value = props.selectedId
  selectedCodeDraft.value = value
  sourceError.value = ''
}

function resetSelectedCodeDraft() {
  selectedCodeDraftTarget.value = props.selectedId
  selectedCodeDraft.value = selectedComponentCodeRaw.value
  sourceError.value = ''
}

function applySelectedCode() {
  if (!props.selectedId)
    return
  try {
    const parsed = JSON.parse(selectedCodeText.value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      throw new Error('当前组件 JSON 必须是对象')
    const nextSchema = replaceComponentInSchema(props.schema, props.selectedId, parsed)
    emit('update:schema', normalizeFormDesignerSchema(nextSchema))
    sourceError.value = ''
    selectedCodeDraftTarget.value = ''
    selectedCodeDraft.value = ''
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
  }
}

function updateSchemaCodeDraft(value) {
  schemaCodeDirty.value = true
  schemaCodeDraft.value = value
  sourceError.value = ''
}

function resetSchemaCodeDraft() {
  schemaCodeDirty.value = true
  schemaCodeDraft.value = schemaCodeRaw.value
  sourceError.value = ''
}

function applySchemaCode() {
  try {
    const parsed = JSON.parse(schemaCodeText.value || '{}')
    emit('update:schema', normalizeFormDesignerSchema(parsed))
    sourceError.value = ''
    schemaCodeDirty.value = false
    schemaCodeDraft.value = ''
  }
  catch (error) {
    sourceError.value = error?.message || 'JSON 解析失败'
  }
}

function replaceComponentInSchema(schema = {}, componentId = '', replacement = {}) {
  const nextSchema = cloneValue(schema)
  const path = findDesignerComponentPath(nextSchema, componentId)
  if (!path)
    return nextSchema
  let children = nextSchema.components || []
  for (let index = 0; index < path.length - 1; index += 1)
    children = children[path[index]].children || []
  children[path[path.length - 1]] = replacement
  return nextSchema
}

function updateDesignerStyle(stylePatch = {}) {
  updateComponent({
    props: {
      __designerStyle: {
        ...selectedDesignerStyle.value,
        ...stylePatch,
      },
    },
  })
}

function updateWidthMode(value) {
  updateDesignerStyle({
    widthMode: value,
    width: value === 'fill' ? '100%' : undefined,
  })
}

function updateHeightMode(value) {
  updateDesignerStyle({
    heightMode: value,
    minHeight: value === 'fill' ? '100%' : undefined,
    height: value === 'default' ? undefined : 'auto',
  })
}

function updateComponentStyleText(value) {
  updateDesignerStyle({
    customStyleText: value,
    customStyle: parseStyleText(value),
  })
}

function updateDesignerSpacing(key, value) {
  const nextCustomStyle = applyStyleValue(selectedDesignerStyle.value.customStyle || {}, key, valueToPx(value))
  updateDesignerStyle({
    customStyle: nextCustomStyle,
    customStyleText: stringifyStyle(nextCustomStyle),
  })
}

function updateFormStyle(stylePatch = {}) {
  updateFormLayout({
    formStyle: {
      ...formStyle.value,
      ...stylePatch,
    },
  })
}

function updateFormStyleText(value) {
  updateFormLayout({
    formStyleText: value,
    formStyle: parseStyleText(value),
  })
}

function updateFormSpacing(key, value) {
  const nextStyle = applyStyleValue(formStyle.value || {}, key, valueToPx(value))
  updateFormLayout({
    formStyle: nextStyle,
    formStyleText: stringifyStyle(nextStyle),
  })
}

function updateCrudApiBase(value) {
  const apiBase = normalizeApiBase(value)
  updateComponent({
    props: {
      apiBase,
      apiConfig: buildDefaultCrudApiConfig(apiBase),
    },
  })
}

function updateCrudApiConfig(key, value) {
  updateComponent({
    props: {
      apiConfig: {
        ...crudApiConfig.value,
        [key]: value,
      },
    },
  })
}

function updateCrudOption(key, value) {
  updateComponent({
    props: {
      crudOptions: {
        ...crudOptions.value,
        [key]: value,
      },
    },
  })
}

function updateCrudFieldRole(componentId, role, value) {
  if (!componentId)
    return
  const field = getDesignerComponent(props.schema, componentId)
  const currentRoles = field?.props?.__crudRoles || {}
  emit('update:schema', updateDesignerComponent(props.schema, componentId, {
    props: {
      __crudRoles: {
        ...currentRoles,
        [role]: Boolean(value),
      },
    },
  }))
}

function updateCrudFieldConfig(area, patch = {}) {
  if (!props.selectedId || !area)
    return
  const current = selectedComponent.value?.props?.__crudConfig || {}
  updateComponent({
    props: {
      __crudConfig: {
        ...current,
        [area]: {
          ...(current[area] || {}),
          ...patch,
        },
      },
    },
  })
}

function updateCrudFieldConfigById(componentId, area, patch = {}) {
  if (!componentId || !area)
    return
  const field = getDesignerComponent(props.schema, componentId)
  const current = field?.props?.__crudConfig || {}
  emit('update:schema', updateDesignerComponent(props.schema, componentId, {
    props: {
      __crudConfig: {
        ...current,
        [area]: {
          ...(current[area] || {}),
          ...patch,
        },
      },
    },
  }))
}

function updateOption(index, patch = {}) {
  const nextOptions = cloneValue(selectedOptions.value || [])
  if (!nextOptions[index])
    return
  nextOptions[index] = {
    ...nextOptions[index],
    ...patch,
  }
  updateComponent({ props: { options: nextOptions } })
}

function updateOptionJsonProps(index, value = '') {
  try {
    updateOption(index, { props: value?.trim() ? JSON.parse(value) : undefined })
  }
  catch {
    updateOption(index, { props: value })
  }
}

function addOption() {
  const nextOptions = cloneValue(selectedOptions.value || [])
  const nextIndex = nextOptions.length + 1
  nextOptions.push({ label: `选项${nextIndex}`, value: `${nextIndex}` })
  updateComponent({ props: { options: nextOptions } })
}

function removeOption(index) {
  const nextOptions = cloneValue(selectedOptions.value || [])
  nextOptions.splice(index, 1)
  updateComponent({ props: { options: nextOptions } })
}

function updateSelectedProp(item = {}, value) {
  if (!item.key)
    return
  updateComponent({
    props: {
      [item.key]: normalizePropValue(item, value),
    },
  })
}

function updateJsonProp(item = {}, value = '') {
  if (!item.key)
    return
  try {
    updateSelectedProp(item, value?.trim() ? JSON.parse(value) : undefined)
  }
  catch {
    updateComponent({
      props: {
        [item.key]: value,
      },
    })
  }
}

function resolvePropValue(item = {}) {
  const value = selectedComponent.value?.props?.[item.key]
  return value === undefined ? item.defaultValue : value
}

function normalizePropValue(item = {}, value) {
  if (value === '' || value === null)
    return undefined
  if (item.type === 'number')
    return value === undefined ? undefined : Number(value)
  return value
}

function stringifyJsonProp(value) {
  if (value === undefined || value === null || value === '')
    return ''
  if (typeof value === 'string')
    return value
  return JSON.stringify(value, null, 2)
}

function resolveCrudSwitchValue(key, defaultValue = false) {
  const value = crudOptions.value?.[key]
  return value === undefined ? defaultValue : Boolean(value)
}

function collectCrudConfigFields(components = [], result = []) {
  ;(Array.isArray(components) ? components : []).forEach((component) => {
    if (!component)
      return
    if (isFieldComponent(component)) {
      const index = result.length
      result.push({
        id: component.id,
        label: component.label || component.fieldBinding?.fieldCode || component.componentKey,
        fieldCode: component.fieldBinding?.fieldCode || component.id,
        componentKey: component.componentKey,
        roles: resolveCrudFieldRoles(component, index),
        config: component.props?.__crudConfig || {},
      })
      return
    }
    collectCrudConfigFields(component.children || [], result)
  })
  return result
}

function resolveCrudFieldRoles(component = {}, index = 0) {
  const roles = component.props?.__crudRoles || {}
  return {
    search: Object.prototype.hasOwnProperty.call(roles, 'search') ? roles.search !== false : index < 3,
    table: Object.prototype.hasOwnProperty.call(roles, 'table') ? roles.table !== false : true,
    edit: Object.prototype.hasOwnProperty.call(roles, 'edit') ? roles.edit !== false : true,
  }
}

function updateFormLayout(patch = {}) {
  emit('update:schema', updateDesignerLayout(props.schema, patch))
}

function updateRowColumns(value) {
  const count = normalizeGridCount(value)
  const row = selectedComponent.value
  if (!row)
    return

  const currentChildren = Array.isArray(row.children) ? cloneValue(row.children) : []
  const currentColumns = currentChildren.filter(child => child?.componentKey === 'col')
  const directChildren = currentChildren.filter(child => child?.componentKey !== 'col')
  if (!currentColumns.length && directChildren.length) {
    currentColumns.push(createColumn(row.id, 0))
    currentColumns[0].children = directChildren
  }
  else if (currentColumns.length && directChildren.length) {
    currentColumns[0].children = [
      ...(currentColumns[0].children || []),
      ...directChildren,
    ]
  }

  const nextColumns = currentColumns.slice(0, count)
  while (nextColumns.length < count)
    nextColumns.push(createColumn(row.id, nextColumns.length))

  const overflowColumns = currentColumns.slice(count)
  if (overflowColumns.length && nextColumns.length) {
    const lastColumn = nextColumns[nextColumns.length - 1]
    lastColumn.children = [
      ...(lastColumn.children || []),
      ...overflowColumns.flatMap(column => column.children || []),
    ]
  }

  nextColumns.forEach((column, index) => {
    column.label = `第 ${index + 1} 列`
    column.layout = { ...(column.layout || {}), span: 1 }
    column.props = { ...(column.props || {}), span: 1 }
  })

  updateComponent({
    label: `${count} 列栅格`,
    props: {
      columns: count,
      gutter: row.props?.gutter ?? 16,
    },
    children: nextColumns,
  })
}

function createColumn(rowId, index) {
  return {
    id: `${rowId || 'row'}_col_${Date.now()}_${index + 1}`,
    componentKey: 'col',
    label: `第 ${index + 1} 列`,
    props: { span: 1 },
    layout: { span: 1, align: 'left' },
    children: [],
  }
}

function normalizeGridCount(value) {
  const number = Number(value)
  return Math.max(1, Math.min(maxFormGridColumns, Number.isFinite(number) ? number : 2))
}

function normalizeLabelWidthInput(value) {
  const text = String(value ?? '').trim()
  if (!text || text === 'auto')
    return 'auto'
  const number = Number(text)
  return Number.isFinite(number) ? number : text
}

function normalizeApiBase(value) {
  const text = String(value || '').trim().replace(/\/+/g, '/')
  if (!text)
    return '/business/object'
  return text.startsWith('/') ? text : `/${text}`
}

function buildDefaultCrudApiConfig(apiBase = '/business/object') {
  return {
    list: `get@${apiBase}/page`,
    detail: `post@${apiBase}/getById`,
    add: `post@${apiBase}/add`,
    update: `post@${apiBase}/edit`,
    delete: `post@${apiBase}/remove/:id`,
  }
}

function hasAncestorComponent(schema = {}, componentId = '', componentKeys = []) {
  const path = findDesignerComponentPath(schema, componentId)
  if (!path || path.length <= 1)
    return false
  const wanted = new Set(componentKeys)
  let children = schema.components || []
  for (let index = 0; index < path.length - 1; index += 1) {
    const component = children[path[index]]
    if (!component)
      return false
    if (wanted.has(component.componentKey))
      return true
    children = component.children || []
  }
  return false
}

function buildNaivePropGroups(componentKey = '') {
  const key = componentKey || ''
  const commonInputProps = [
    prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
    prop('clearable', 'clearable', 'boolean', null, true),
    prop('disabled', 'disabled', 'boolean', null, false),
    prop('placeholder', 'placeholder'),
    prop('status', 'status', 'select', [
      { label: 'default', value: '' },
      { label: 'success', value: 'success' },
      { label: 'warning', value: 'warning' },
      { label: 'error', value: 'error' },
    ]),
  ]
  const maps = {
    input: [
      group('NInput Props', [
        ...commonInputProps,
        prop('maxlength', 'maxlength', 'number', null, undefined, { min: 0 }),
        prop('showCount', 'show-count', 'boolean', null, false),
        prop('round', 'round', 'boolean', null, false),
        prop('readonly', 'readonly', 'boolean', null, false),
        prop('inputProps', 'input-props', 'json'),
      ]),
    ],
    textarea: [
      group('NInput Textarea Props', [
        ...commonInputProps,
        prop('rows', 'rows', 'number', null, 3, { min: 1, max: 20 }),
        prop('autosize', 'autosize', 'json'),
        prop('maxlength', 'maxlength', 'number', null, undefined, { min: 0 }),
        prop('showCount', 'show-count', 'boolean', null, false),
        prop('readonly', 'readonly', 'boolean', null, false),
      ]),
    ],
    number: [
      group('NInputNumber Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('min', 'min', 'number'),
        prop('max', 'max', 'number'),
        prop('step', 'step', 'number', null, 1),
        prop('precision', 'precision', 'number', null, undefined, { min: 0 }),
        prop('showButton', 'show-button', 'boolean', null, true),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('placeholder', 'placeholder'),
      ]),
    ],
    money: [
      group('NInputNumber Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('min', 'min', 'number'),
        prop('max', 'max', 'number'),
        prop('step', 'step', 'number', null, 0.01),
        prop('precision', 'precision', 'number', null, 2, { min: 0 }),
        prop('showButton', 'show-button', 'boolean', null, true),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('placeholder', 'placeholder'),
      ]),
    ],
    select: [
      group('NSelect Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('filterable', 'filterable', 'boolean', null, true),
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('remote', 'remote', 'boolean', null, false),
        prop('loading', 'loading', 'boolean', null, false),
        prop('maxTagCount', 'max-tag-count', 'number', null, undefined, { min: 1 }),
        prop('placement', 'placement', 'select', pickerPlacementOptions),
        prop('fallbackOption', 'fallback-option', 'boolean', null, true),
      ]),
    ],
    dictSelect: [
      group('DictSelect / NSelect Props', [
        prop('dictType', 'dict-type'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('filterable', 'filterable', 'boolean', null, true),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
    radio: [
      group('RadioGroup Props', [
        prop('name', 'name'),
        prop('defaultValue', 'default-value'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
    radioButton: [
      group('RadioGroup Props', [
        prop('name', 'name'),
        prop('defaultValue', 'default-value'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
    checkbox: [
      group('CheckboxGroup Props', [
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('defaultValue', 'default-value', 'json'),
        prop('value', 'value（受控，高级）', 'json'),
        prop('min', 'min', 'number', null, undefined, { min: 0 }),
        prop('max', 'max', 'number', null, undefined, { min: 0 }),
      ]),
    ],
    switch: [
      group('NSwitch Props', [
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
        prop('disabled', 'disabled', 'boolean', null, false),
        prop('round', 'round', 'boolean', null, true),
        prop('rubberBand', 'rubber-band', 'boolean', null, true),
        prop('checkedValue', 'checked-value', 'select', booleanValueOptions, true),
        prop('uncheckedValue', 'unchecked-value', 'select', booleanValueOptions, false),
        prop('checkedText', 'checked 文案'),
        prop('uncheckedText', 'unchecked 文案'),
      ]),
    ],
    slider: [
      group('NSlider Props', [
        prop('min', 'min', 'number', null, 0),
        prop('max', 'max', 'number', null, 100),
        prop('step', 'step', 'number', null, 1),
        prop('range', 'range', 'boolean', null, false),
        prop('vertical', 'vertical', 'boolean', null, false),
        prop('tooltip', 'tooltip', 'boolean', null, true),
        prop('marks', 'marks', 'json'),
      ]),
    ],
    rate: [
      group('NRate Props', [
        prop('count', 'count', 'number', null, 5, { min: 1, max: 10 }),
        prop('allowHalf', 'allow-half', 'boolean', null, false),
        prop('readonly', 'readonly', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, false),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
      ]),
    ],
    color: [
      group('NColorPicker Props', [
        prop('showAlpha', 'show-alpha', 'boolean', null, true),
        prop('modes', 'modes', 'multiSelect', [
          { label: 'hex', value: 'hex' },
          { label: 'rgb', value: 'rgb' },
          { label: 'hsl', value: 'hsl' },
          { label: 'hsv', value: 'hsv' },
        ]),
        prop('swatches', 'swatches', 'json'),
        prop('actions', 'actions', 'multiSelect', pickerActionOptions),
        prop('placement', 'placement', 'select', pickerPlacementOptions),
      ]),
    ],
    cascader: [
      group('NCascader Props', [
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('cascade', 'cascade', 'boolean', null, true),
        prop('checkStrategy', 'check-strategy', 'select', [
          { label: 'all', value: 'all' },
          { label: 'parent', value: 'parent' },
          { label: 'child', value: 'child' },
        ]),
        prop('showPath', 'show-path', 'boolean', null, true),
        prop('filterable', 'filterable', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('placement', 'placement', 'select', pickerPlacementOptions),
      ]),
    ],
    treeSelect: [
      group('NTreeSelect Props', [
        prop('multiple', 'multiple', 'boolean', null, false),
        prop('cascade', 'cascade', 'boolean', null, true),
        prop('checkable', 'checkable', 'boolean', null, false),
        prop('filterable', 'filterable', 'boolean', null, false),
        prop('clearable', 'clearable', 'boolean', null, true),
        prop('showPath', 'show-path', 'boolean', null, true),
        prop('maxTagCount', 'max-tag-count', 'number', null, undefined, { min: 1 }),
      ]),
    ],
    card: [
      group('Card Props', [
        prop('size', 'size', 'select', cardSizeOptions, 'small'),
        prop('bordered', 'bordered', 'boolean', null, true),
        prop('embedded', 'embedded', 'boolean', null, false),
        prop('segmented', 'segmented', 'boolean', null, false),
        prop('hoverable', 'hoverable', 'boolean', null, false),
        prop('contentScrollable', 'content-scrollable', 'boolean', null, false),
        prop('role', 'role'),
        prop('headerStyle', 'header-style', 'json'),
        prop('contentStyle', 'content-style', 'json'),
        prop('footerStyle', 'footer-style', 'json'),
      ]),
    ],
    tabs: [
      group('Tabs Props', [
        prop('type', 'type', 'select', tabsTypeOptions, 'line'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value), 'medium'),
        prop('placement', 'placement', 'select', tabsPlacementOptions, 'top'),
        prop('trigger', 'trigger', 'select', tabsTriggerOptions, 'click'),
        prop('animated', 'animated', 'boolean', null, true),
        prop('closable', 'closable', 'boolean', null, false),
        prop('addable', 'addable', 'boolean', null, false),
        prop('justifyContent', 'justify-content', 'select', [
          { label: 'start', value: 'start' },
          { label: 'center', value: 'center' },
          { label: 'end', value: 'end' },
          { label: 'space-between', value: 'space-between' },
          { label: 'space-around', value: 'space-around' },
          { label: 'space-evenly', value: 'space-evenly' },
        ]),
        prop('tabsPadding', 'tabs-padding', 'number', null, 0, { min: 0 }),
        prop('paneStyle', 'pane-style', 'json'),
        prop('tabStyle', 'tab-style', 'json'),
      ]),
    ],
    collapse: [
      group('Collapse Props', [
        prop('accordion', 'accordion', 'boolean', null, false),
        prop('arrowPlacement', 'arrow-placement', 'select', collapseArrowPlacementOptions, 'left'),
        prop('displayDirective', 'display-directive', 'select', collapseDisplayDirectiveOptions, 'if'),
        prop('triggerAreas', 'trigger-areas', 'multiSelect', collapseTriggerAreaOptions, ['main', 'arrow']),
        prop('defaultExpandedNames', 'default-expanded-names', 'json'),
        prop('expandedNames', 'expanded-names（受控，高级）', 'json'),
      ]),
    ],
    button: [
      group('Button Props', [
        prop('text', 'text'),
        prop('type', 'type', 'select', buttonTypeOptions, 'primary'),
        prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value), 'medium'),
        prop('secondary', 'secondary', 'boolean', null, false),
        prop('tertiary', 'tertiary', 'boolean', null, false),
        prop('quaternary', 'quaternary', 'boolean', null, false),
        prop('dashed', 'dashed', 'boolean', null, false),
        prop('round', 'round', 'boolean', null, false),
        prop('block', 'block', 'boolean', null, false),
        prop('loading', 'loading', 'boolean', null, false),
        prop('disabled', 'disabled', 'boolean', null, false),
      ]),
    ],
  }
  if (isDateLikeComponent(key))
    return [group('DatePicker Props', datePickerPropFields())]
  if (isTimeLikeComponent(key))
    return [group('TimePicker Props', timePickerPropFields())]
  return maps[key] || []
}

function datePickerPropFields() {
  return [
    prop('type', 'type', 'select', datePickerTypeOptions),
    prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
    prop('format', 'format'),
    prop('valueFormat', 'value-format'),
    prop('placement', 'placement', 'select', pickerPlacementOptions, 'bottom-start'),
    prop('actions', 'actions', 'multiSelect', pickerActionOptions),
    prop('bordered', 'bordered', 'boolean', null, true),
    prop('clearable', 'clearable', 'boolean', null, true),
    prop('inputReadonly', 'input-readonly', 'boolean', null, false),
    prop('closeOnSelect', 'close-on-select', 'boolean', null, false),
    prop('updateValueOnClose', 'update-value-on-close', 'boolean', null, false),
    prop('defaultTime', 'default-time', 'json'),
    prop('separator', 'separator'),
    prop('startPlaceholder', 'start-placeholder'),
    prop('endPlaceholder', 'end-placeholder'),
    prop('firstDayOfWeek', 'first-day-of-week', 'number', null, undefined, { min: 0, max: 6 }),
  ]
}

function timePickerPropFields() {
  return [
    prop('size', 'size', 'select', componentSizeOptions.filter(item => item.value)),
    prop('format', 'format'),
    prop('valueFormat', 'value-format'),
    prop('placement', 'placement', 'select', pickerPlacementOptions, 'bottom-start'),
    prop('actions', 'actions', 'multiSelect', pickerActionOptions),
    prop('bordered', 'bordered', 'boolean', null, true),
    prop('clearable', 'clearable', 'boolean', null, true),
    prop('inputReadonly', 'input-readonly', 'boolean', null, false),
    prop('showIcon', 'show-icon', 'boolean', null, true),
    prop('hours', 'hours', 'json'),
    prop('minutes', 'minutes', 'json'),
    prop('seconds', 'seconds', 'json'),
  ]
}

function isDateLikeComponent(key = '') {
  return ['date', 'datetime', 'daterange', 'datetimerange', 'month', 'year', 'quarter'].includes(key)
}

function isTimeLikeComponent(key = '') {
  return ['time', 'timerange'].includes(key)
}

function group(title, fields = []) {
  const zh = groupChineseLabels[title]
  return { title: zh ? `${zh} ${title}` : title, fields }
}

function prop(key, label, type = 'text', options = null, defaultValue = undefined, extra = {}) {
  return { key, label: formatPropLabel(key, label), type, options, defaultValue, ...extra }
}

function formatPropLabel(key = '', label = '') {
  const zh = propChineseLabels[key] || propChineseLabels[label] || ''
  const propName = label || key
  if (!zh)
    return propName
  if (propName.includes('（'))
    return `${zh}（${propName}）`
  return `${zh}（${propName}）`
}

function resolvePxNumber(value, fallback = 0) {
  const match = String(value ?? '').match(/-?\d+(\.\d+)?/)
  if (!match)
    return fallback
  return Number(match[0])
}

function parseStyleText(value = '') {
  return String(value || '')
    .split(';')
    .map(item => item.trim())
    .filter(Boolean)
    .reduce((style, item) => {
      const [rawKey, ...rawValue] = item.split(':')
      const key = rawKey?.trim()
      const nextValue = rawValue.join(':').trim()
      if (!key || !nextValue)
        return style
      style[toCamelCase(key)] = nextValue
      return style
    }, {})
}

function stringifyStyle(style = {}) {
  return Object.entries(style || {})
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${toKebabCase(key)}: ${value}`)
    .join('; ')
}

function applyStyleValue(source = {}, key, value) {
  const next = { ...(source || {}) }
  if (value === undefined || value === null || value === '')
    delete next[key]
  else
    next[key] = value
  return next
}

function valueToPx(value) {
  if (value === undefined || value === null || value === '')
    return undefined
  const number = Number(value)
  return Number.isFinite(number) ? `${number}px` : undefined
}

function toCamelCase(value = '') {
  return String(value).replace(/-([a-z])/g, (_, letter) => letter.toUpperCase())
}

function toKebabCase(value = '') {
  return String(value).replace(/[A-Z]/g, letter => `-${letter.toLowerCase()}`)
}
</script>

<style scoped>
.forge-property-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f7f8fa;
}

.edit-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 48px;
  padding: 0 12px 0 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.edit-panel-title {
  min-width: 0;
}

.edit-panel-title strong,
.edit-panel-title span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-panel-title strong {
  color: #1f2329;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.edit-panel-title span {
  color: #646a73;
  font-size: 12px;
  line-height: 18px;
}

.edit-panel-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.panel-close-button {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  cursor: pointer;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #646a73;
  font-size: 14px;
}

.panel-close-button:hover {
  background: #f2f3f5;
  color: #1f2329;
}

.property-tabs {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.form-property-tabs {
  margin-top: 0;
}

.property-tabs :deep(.n-tabs-nav) {
  background: #fff;
  padding: 0 14px;
}

.property-tabs :deep(.n-tabs-tab) {
  padding: 13px 0 11px;
  color: #646a73;
  font-size: 13px;
}

.property-tabs :deep(.n-tabs-tab--active) {
  color: #1f2329;
  font-weight: 600;
}

.property-tabs :deep(.n-tabs-pane-wrapper) {
  min-height: 0;
  overflow: auto;
}

.property-form {
  min-height: 0;
  overflow: auto;
  padding: 12px 14px 20px;
}

.panel-item {
  margin-bottom: 20px;
}

.form-property-collapse,
.config-collapse {
  display: grid;
  gap: 8px;
}

.form-property-collapse :deep(.n-collapse-item),
.config-collapse :deep(.n-collapse-item) {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.form-property-collapse :deep(.n-collapse-item__header),
.config-collapse :deep(.n-collapse-item__header) {
  min-height: 38px;
  padding: 0 12px;
  border-bottom: 0;
}

.form-property-collapse :deep(.n-collapse-item__header-main),
.config-collapse :deep(.n-collapse-item__header-main) {
  color: #1f2329;
  font-size: 13px;
  font-weight: 650;
}

.form-property-collapse :deep(.n-collapse-item__content-inner),
.config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 0 12px 12px;
}

.form-property-collapse :deep(.n-collapse-item__content-inner > .panel-item),
.config-collapse :deep(.n-collapse-item__content-inner > .panel-item) {
  margin-bottom: 0;
  border: 0;
  border-radius: 0;
  padding: 0;
}

.form-property-collapse :deep(.n-collapse-item__content-inner > .panel-item > .panel-item-title),
.config-collapse :deep(.n-collapse-item__content-inner > .panel-item > .panel-item-title) {
  display: none;
}

.property-form > .panel-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.panel-item-strong {
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  padding: 12px;
}

.more-config-button {
  --n-color: #eef6ff !important;
  --n-color-hover: #dbeafe !important;
  --n-color-pressed: #bfdbfe !important;
  --n-color-focus: #eef6ff !important;
  --n-border: 1px solid #bfdbfe !important;
  --n-border-hover: 1px solid #93c5fd !important;
  --n-border-pressed: 1px solid #60a5fa !important;
  --n-border-focus: 1px solid #93c5fd !important;
  --n-text-color: #1d4ed8 !important;
  --n-text-color-hover: #1e40af !important;
  --n-text-color-pressed: #1e3a8a !important;
  --n-text-color-focus: #1d4ed8 !important;
  font-weight: 600;
}

.button-icon {
  display: inline-grid;
  place-items: center;
  color: currentColor;
}

.panel-item-title {
  display: flex;
  align-items: center;
  min-height: 20px;
  margin-bottom: 8px;
  color: #1f2329;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.panel-title-row > :first-child {
  min-width: 0;
}

.panel-title-row > .n-button,
.panel-title-row > .source-actions {
  flex-shrink: 0;
}

.panel-title-row .panel-item-title {
  margin-bottom: 0;
}

.panel-item-desc {
  margin: 3px 0 0;
  color: #8f959e;
  font-size: 11px;
  line-height: 16px;
}

.panel-item :deep(.n-form-item) {
  margin-bottom: 12px;
}

.panel-item :deep(.n-form-item:last-child) {
  margin-bottom: 0;
}

.panel-item :deep(.n-form-item-label) {
  min-height: 20px;
  color: #646a73;
  font-size: 12px;
  font-weight: 500;
}

.panel-item :deep(.n-radio-group),
.panel-item :deep(.n-input-number),
.panel-item :deep(.n-select) {
  width: 100%;
}

.drawer-property-form {
  padding: 2px 2px 18px;
}

.color-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.slider-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 0 2px 8px;
}

.spacing-editor {
  border: 1px solid #eff0f1;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.spacing-editor + .spacing-editor {
  margin-top: 10px;
}

.spacing-editor-title {
  margin-bottom: 8px;
  color: #646a73;
  font-size: 12px;
  font-weight: 600;
}

.spacing-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.spacing-grid label {
  display: grid;
  grid-template-columns: 20px minmax(86px, 1fr);
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.spacing-grid span {
  color: #646a73;
  font-size: 12px;
}

.form-asset-panel {
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
}

.form-asset-list {
  display: grid;
  gap: 8px;
}

.form-asset-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.form-asset-icon-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  cursor: pointer;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  padding: 0 8px;
}

.form-asset-icon-button:hover {
  border-color: #93c5fd;
  background: #dbeafe;
}

.form-asset-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  overflow-x: auto;
  padding-bottom: 3px;
}

.form-asset-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 168px;
  flex: 0 0 auto;
  height: 32px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #475569;
  padding: 0 9px;
}

.form-asset-tab:hover {
  border-color: #bfdbfe;
  background: #f8fafc;
}

.form-asset-tab.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.form-asset-tab em {
  display: inline-grid;
  place-items: center;
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-style: normal;
  font-size: 11px;
  font-weight: 700;
}

.form-asset-tab.active em {
  background: #2563eb;
  color: #fff;
}

.form-asset-tab span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
}

.form-asset-tab strong {
  flex: 0 0 auto;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 10px;
  line-height: 16px;
  padding: 0 5px;
}

.current-form-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 9px 10px;
  animation: current-form-pulse 420ms ease;
}

.current-form-banner span {
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  padding: 0 7px;
}

.current-form-banner strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.form-asset-edit-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}

.form-asset-edit-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.form-asset-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 7px;
  background: #fff;
  color: #1f2329;
  padding: 9px 10px;
  text-align: left;
}

button.form-asset-card,
.form-asset-main {
  cursor: pointer;
}

.form-asset-card.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.form-asset-main {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.form-asset-switch {
  height: 28px;
  cursor: pointer;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  padding: 0 8px;
}

.form-asset-switch:hover {
  border-color: #93c5fd;
  background: #dbeafe;
}

.form-asset-card strong,
.form-asset-card span,
.form-asset-main strong,
.form-asset-main span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-asset-card strong,
.form-asset-main strong {
  font-size: 12px;
  font-weight: 700;
}

.form-asset-card span,
.form-asset-main span {
  margin-top: 2px;
  color: #8f959e;
  font-size: 11px;
  line-height: 16px;
}

@keyframes current-form-pulse {
  0% {
    transform: translateY(-2px);
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.24);
  }
  100% {
    transform: translateY(0);
    box-shadow: 0 0 0 8px rgba(37, 99, 235, 0);
  }
}

.layout-child-manager {
  display: grid;
  gap: 8px;
  margin-top: 12px;
  border-top: 1px solid #eff0f1;
  padding-top: 12px;
}

.layout-child-card,
.interaction-rule-card {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.layout-child-card {
  display: grid;
  gap: 8px;
}

.layout-child-card-main {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 8px;
}

.layout-child-actions,
.source-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.interaction-rule-list {
  display: grid;
  gap: 10px;
}

.interaction-presets {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.interaction-preset-card {
  display: grid;
  gap: 4px;
  min-height: 76px;
  cursor: pointer;
  border: 1px solid #dbeafe;
  border-radius: 7px;
  background: linear-gradient(135deg, #eff6ff 0%, #fff 72%);
  color: #1f2329;
  padding: 10px;
  text-align: left;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.interaction-preset-card:hover {
  border-color: #60a5fa;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}

.interaction-preset-card strong {
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.interaction-preset-card span {
  color: #64748b;
  font-size: 11px;
  line-height: 16px;
}

.interaction-rule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  list-style: none;
}

.interaction-rule-head::-webkit-details-marker {
  display: none;
}

.interaction-rule-head::before {
  content: '';
  width: 0;
  height: 0;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  border-left: 5px solid #64748b;
  transform: rotate(90deg);
  transition: transform 160ms ease;
}

.interaction-rule-card:not([open]) .interaction-rule-head {
  margin-bottom: 0;
}

.interaction-rule-card:not([open]) .interaction-rule-head::before {
  transform: rotate(0deg);
}

.interaction-rule-head > div {
  flex: 1;
  min-width: 0;
}

.interaction-rule-head strong {
  display: block;
  color: #1f2329;
  font-size: 12px;
  font-weight: 600;
}

.interaction-rule-head span {
  display: block;
  margin-top: 2px;
  color: #64748b;
  font-size: 11px;
  line-height: 16px;
}

.interaction-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
}

.interaction-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.interaction-rule-card :deep(.n-form-item-blank) {
  min-width: 0;
}

.interaction-rule-card :deep(.n-select),
.interaction-rule-card :deep(.n-input),
.interaction-rule-card :deep(.n-input-wrapper) {
  width: 100%;
}

.field-help {
  margin-top: 4px;
  color: #8f959e;
  font-size: 11px;
  line-height: 16px;
}

.field-label-with-help {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.help-icon {
  display: inline-grid;
  place-items: center;
  width: 15px;
  height: 15px;
  cursor: help;
  border-radius: 50%;
  background: #eef2ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.help-icon:hover {
  background: #dbeafe;
}

.empty-config-box {
  border: 1px dashed #dbe3ee;
  border-radius: 6px;
  background: #f8fafc;
  color: #646a73;
  font-size: 12px;
  line-height: 18px;
  padding: 12px;
  text-align: center;
}

.source-editor {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.source-panel {
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.source-path {
  margin-top: 2px;
  color: #8f959e;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  line-height: 16px;
}

.source-editor :deep(textarea) {
  font-family: inherit;
  font-size: 12px;
  line-height: 1.55;
}

.source-error {
  margin-top: 8px;
  border: 1px solid #fecaca;
  border-radius: 6px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 12px;
  line-height: 18px;
  padding: 8px 10px;
}

.crud-field-config-list {
  display: grid;
  gap: 10px;
}

.crud-field-config-card {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.crud-field-card-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 10px;
  margin-bottom: 10px;
}

.crud-field-name {
  min-width: 0;
  cursor: pointer;
  border: 0;
  background: transparent;
  color: #1f2329;
  text-align: left;
  padding: 0;
}

.crud-field-name strong,
.crud-field-name small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.crud-field-name strong {
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
}

.crud-field-name small {
  color: #8f959e;
  font-size: 11px;
  line-height: 16px;
}

.crud-field-name:hover strong {
  color: #2563eb;
}

.crud-role-switches,
.crud-compact-switches {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.crud-role-switches label,
.crud-compact-switches label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #475569;
  font-size: 12px;
  line-height: 22px;
}

.crud-inline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.crud-inline-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.crud-compact-switches {
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  border-top: 1px solid #f1f5f9;
  padding-top: 8px;
}

.crud-field-empty {
  border: 1px dashed #dbe3ee;
  border-radius: 6px;
  background: #f8fafc;
  color: #646a73;
  font-size: 12px;
  line-height: 18px;
  padding: 12px;
}

.option-list {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.option-editor-row {
  display: grid;
  grid-template-columns: minmax(72px, 1fr) minmax(64px, 0.8fr) 42px 44px;
  align-items: center;
  gap: 6px;
  border: 1px solid #eff0f1;
  border-radius: 6px;
  background: #fff;
  padding: 7px;
}

.option-editor-row > .n-switch:nth-last-child(-n + 2) {
  justify-self: center;
}

.option-props-input {
  grid-column: 1 / -1;
}

.crud-field-config-title {
  margin: 14px 0 8px;
  border-top: 1px solid #eff0f1;
  padding-top: 12px;
  color: #1f2329;
  font-size: 12px;
  font-weight: 700;
}

.crud-field-config-title:first-child {
  margin-top: 0;
  border-top: 0;
  padding-top: 0;
}

.switch-list {
  display: grid;
  gap: 8px;
}

.switch-list + :deep(.n-form-item) {
  margin-top: 12px;
}

.switch-list label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 32px;
  border: 1px solid #eff0f1;
  border-radius: 6px;
  background: #fff;
  color: #1f2329;
  font-size: 12px;
  padding: 0 9px;
}

.switch-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.size-setting-row {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.size-setting-row:last-child {
  margin-bottom: 0;
}

.size-setting-label {
  color: #646a73;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
}
</style>
