<!--
  AI 表单项组件 - 根据配置动态渲染不同类型的表单字段
-->

<template>
  <!-- 表单分隔线 -->
  <AiFormSectionTitle
    v-if="fieldRuntimeVisible && isSectionTitleField"
    :title="field.label"
    :anchor-id="field.__sectionId"
    :description="field.props?.description || field.description"
    :badge="field.props?.badge || field.badge"
    :style="field.style || field.formItemStyle"
    :class="field.className"
  />

  <!-- 分组标题 -->
  <AiFormGroupTitle
    v-else-if="fieldRuntimeVisible && isGroupTitleField"
    :label="field.label"
    :title="field.props?.title || field.title"
    :style="field.style || field.formItemStyle"
    :class-name="field.className"
  />

  <!-- 普通表单项 -->
  <n-form-item
    v-else-if="fieldRuntimeVisible"
    :label="field.label"
    :path="field.field"
    :label-width="field.labelWidth"
    :show-label="field.showLabel !== false"
    :show-feedback="field.showFeedback !== false"
    :required="isFieldRequired"
    :data-ai-field="field.field"
    :style="field.formItemStyle"
    :class="formItemClass"
  >
    <template #label>
      <span class="ai-form-item-label">
        <span class="ai-form-item-label__text">{{ field.label }}</span>
        <span v-if="fieldBadge" class="ai-form-item-label__badge">{{ fieldBadge }}</span>
        <n-tooltip v-if="fieldLabelTip" trigger="hover" placement="top" :style="{ maxWidth: '360px' }">
          <template #trigger>
            <span class="ai-form-item-label__tip">?</span>
          </template>
          <span class="ai-form-item-label__tip-content">{{ fieldLabelTip }}</span>
        </n-tooltip>
      </span>
    </template>

    <div class="ai-form-item-body">
      <div
        class="ai-form-control"
        :style="componentControlStyle"
        :class="componentControlClass"
        @focusout="handleFieldFocusout"
        @keyup="handleFieldKeyup"
      >
        <div
          v-if="shouldRenderReadonlySelectionText(field)"
          class="ai-form-readonly-text"
          :title="resolveReadonlySelectionText(field)"
        >
          {{ resolveReadonlySelectionText(field) }}
        </div>

        <!-- 低代码页面展示组件 -->
        <PageWidgetRenderer
          v-else-if="isRuntimePageWidgetField"
          :component-key="runtimePageWidgetKey"
          :props-data="runtimePageWidgetProps"
          :data-context="formData || {}"
          :readonly="disabledHandler(field) || field.visibility?.readonly === true || field.readonly === true"
          @update:props-data="handleRuntimePageWidgetUpdate"
        />

        <!-- 输入框 -->
        <n-input
          v-else-if="field.type === 'input'"
          :value="value"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :maxlength="field.maxlength"
          :show-count="field.showCount"
          :size="field.size"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- H5/企业微信条码扫描输入框 -->
        <div v-else-if="field.type === 'barcodeScanner'" class="ai-form-barcode-scanner-field">
          <n-input
            :value="value"
            :placeholder="getPlaceholder(field)"
            :disabled="disabledHandler(field)"
            :clearable="field.props?.allowManualInput !== false"
            :maxlength="field.props?.maxlength || 2048"
            v-bind="field.props"
            :readonly="field.props?.allowManualInput === false || fieldRuntimeControl.readonly"
            @update:value="handleUpdate"
            v-on="getComponentEvents(field)"
          />
          <n-button
            type="primary"
            secondary
            :loading="scanLoading"
            :disabled="disabledHandler(field) || scanLoading"
            @click="handleScanFieldEvent"
          >
            {{ scanLoading ? '扫描中' : '扫描条码' }}
          </n-button>
        </div>

        <!-- 多行文本 -->
        <n-input
          v-else-if="field.type === 'textarea'"
          type="textarea"
          :value="value"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :rows="field.rows || 3"
          :maxlength="field.maxlength"
          :show-count="field.showCount"
          :autosize="field.autosize"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 数字输入框 -->
        <n-input-number
          v-else-if="isNumberFieldType(field.type)"
          :value="value"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :min="field.min"
          :max="field.max"
          :step="field.step || 1"
          :precision="field.precision"
          :show-button="field.showButton !== false"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 下拉选择 -->
        <n-select
          v-else-if="field.type === 'select'"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :options="currentOptions"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :multiple="field.multiple"
          :loading="field.loading"
          :remote="field.remote"
          :on-search="field.onSearch"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 字典选择器 -->
        <DictSelect
          v-else-if="field.type === 'dictSelect'"
          :value="value"
          :dict-type="field.dictType || field.props?.dictType"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :multiple="field.multiple"
          :form-data="formData"
          :cascade="dictCascadeConfig"
          v-bind="field.props"
          @update:value="handleUpdate"
        />

        <!-- 单选框 -->
        <n-radio-group
          v-else-if="field.type === 'radio'"
          :value="resolveOptionValue(value)"
          :disabled="disabledHandler(field)"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        >
          <n-space :vertical="field.props?.direction === 'vertical'">
            <n-radio
              v-for="option in currentOptions"
              :key="option.value"
              :value="option.value"
              :disabled="option.disabled"
            >
              {{ option.label }}
            </n-radio>
          </n-space>
        </n-radio-group>

        <!-- 单选按钮组 -->
        <n-radio-group
          v-else-if="field.type === 'radioButton'"
          :value="resolveOptionValue(value)"
          :disabled="disabledHandler(field)"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        >
          <n-space :vertical="field.props?.direction === 'vertical'">
            <n-radio-button
              v-for="option in currentOptions"
              :key="option.value"
              :value="option.value"
              :disabled="option.disabled"
            >
              {{ option.label }}
            </n-radio-button>
          </n-space>
        </n-radio-group>

        <!-- 多选框 -->
        <n-checkbox-group
          v-else-if="field.type === 'checkbox'"
          :value="resolveOptionValue(value)"
          :disabled="disabledHandler(field)"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        >
          <n-space>
            <n-checkbox
              v-for="option in currentOptions"
              :key="option.value"
              :value="option.value"
              :disabled="option.disabled"
              :label="option.props?.label"
              :indeterminate="!!option.indeterminate"
              :focusable="option.focusable !== false"
              v-bind="option.props"
            >
              {{ option.label }}
            </n-checkbox>
          </n-space>
        </n-checkbox-group>

        <!-- 开关 -->
        <n-switch
          v-else-if="field.type === 'switch'"
          :value="value"
          :disabled="disabledHandler(field)"
          :checked-value="field.checkedValue ?? true"
          :unchecked-value="field.uncheckedValue ?? false"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        >
          <template v-if="field.checkedText" #checked>
            {{ field.checkedText }}
          </template>
          <template v-if="field.uncheckedText" #unchecked>
            {{ field.uncheckedText }}
          </template>
        </n-switch>

        <!-- 日期选择 -->
        <n-date-picker
          v-else-if="field.type === 'date'"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="date"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 日期时间选择 -->
        <n-date-picker
          v-else-if="field.type === 'datetime'"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="datetime"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd HH:mm:ss'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd HH:mm:ss'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 日期范围选择 -->
        <n-date-picker
          v-else-if="field.type === 'daterange'"
          :value="normalizeTimestampRangePickerValue(value)"
          :formatted-value="normalizeFormattedRangePickerValue(value)"
          type="daterange"
          :placeholder="field.placeholder"
          :start-placeholder="field.startPlaceholder || '开始日期'"
          :end-placeholder="field.endPlaceholder || '结束日期'"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          :default-value="resolvePickerDefaultValue(field, true)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 日期时间范围选择 -->
        <n-date-picker
          v-else-if="field.type === 'datetimerange'"
          :value="normalizeTimestampRangePickerValue(value)"
          :formatted-value="normalizeFormattedRangePickerValue(value)"
          type="datetimerange"
          :placeholder="field.placeholder"
          :start-placeholder="field.startPlaceholder || '开始时间'"
          :end-placeholder="field.endPlaceholder || '结束时间'"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          :default-value="resolvePickerDefaultValue(field, true)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd HH:mm:ss'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd HH:mm:ss'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 月份选择 -->
        <n-date-picker
          v-else-if="field.type === 'month'"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="month"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'yyyy-MM'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 年份选择 -->
        <n-date-picker
          v-else-if="field.type === 'year'"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="year"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'yyyy'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 时间选择 -->
        <n-time-picker
          v-else-if="field.type === 'time'"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="field.props"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'HH:mm:ss'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 时间范围选择 -->
        <div v-else-if="field.type === 'timerange'" class="time-range-picker">
          <n-time-picker
            :formatted-value="resolveFormattedRangeValue(value, 0)"
            :placeholder="field.startPlaceholder || '开始时间'"
            :disabled="disabledHandler(field)"
            :clearable="field.clearable !== false"
            style="width: 100%"
            v-bind="field.props"
            :default-value="resolvePickerDefaultValue(field)"
            :format="field.props?.format || field.format || 'HH:mm:ss'"
            :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
            @update:formatted-value="handleRangeUpdate(0, $event)"
            v-on="getComponentEvents(field)"
          />
          <span class="time-range-separator">至</span>
          <n-time-picker
            :formatted-value="resolveFormattedRangeValue(value, 1)"
            :placeholder="field.endPlaceholder || '结束时间'"
            :disabled="disabledHandler(field)"
            :clearable="field.clearable !== false"
            style="width: 100%"
            v-bind="field.props"
            :default-value="resolvePickerDefaultValue(field)"
            :format="field.props?.format || field.format || 'HH:mm:ss'"
            :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
            @update:formatted-value="handleRangeUpdate(1, $event)"
            v-on="getComponentEvents(field)"
          />
        </div>

        <!-- 文件上传 -->
        <n-upload
          v-else-if="field.type === 'upload'"
          :action="field.action"
          :headers="field.headers"
          :data="field.data"
          :max="field.max"
          :accept="field.accept"
          :multiple="field.multiple"
          :disabled="disabledHandler(field)"
          :list-type="field.listType || 'text'"
          :show-file-list="field.showFileList !== false"
          :on-change="handleUploadChange"
          v-bind="field.props"
          v-on="getComponentEvents(field)"
        >
          <n-button>{{ field.uploadText || '点击上传' }}</n-button>
        </n-upload>

        <!-- 文件上传组件 -->
        <FileUpload
          v-else-if="field.type === 'fileUpload'"
          :model-value="value"
          :action="field.action"
          :business-type="field.businessType"
          :business-id="field.businessId"
          :storage-type="field.storageType"
          :limit="field.limit"
          :file-size="field.fileSize"
          :file-type="field.fileType"
          :multiple="field.multiple"
          :show-file-list="field.showFileList"
          :show-tip="field.showTip"
          :upload-button-text="field.uploadButtonText"
          :disabled="disabledHandler(field)"
          :value-type="field.valueType"
          v-bind="field.props"
          @update:model-value="handleUpdate"
          @success="(data) => handleUploadSuccess(field, data)"
          @error="(error) => handleUploadError(field, error)"
          @remove="(file) => handleUploadRemove(field, file)"
        />

        <!-- 图片上传组件 -->
        <ImageUpload
          v-else-if="field.type === 'imageUpload'"
          :model-value="value"
          :action="field.action"
          :business-type="field.businessType"
          :business-id="field.businessId"
          :storage-type="field.storageType"
          :limit="field.limit"
          :file-size="field.fileSize"
          :file-type="field.fileType"
          :multiple="field.multiple"
          :show-tip="field.showTip"
          :disabled="disabledHandler(field)"
          :value-type="field.valueType"
          v-bind="field.props"
          @update:model-value="handleUpdate"
          @success="(data) => handleUploadSuccess(field, data)"
          @error="(error) => handleUploadError(field, error)"
          @remove="(file) => handleUploadRemove(field, file)"
        />

        <!-- 滑块 -->
        <n-slider
          v-else-if="field.type === 'slider'"
          :value="resolveSliderValue(value, field)"
          :disabled="disabledHandler(field)"
          :min="field.min || 0"
          :max="field.max || 100"
          :step="field.step || 1"
          :marks="field.marks || undefined"
          :tooltip="field.tooltip !== false"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 评分 -->
        <n-rate
          v-else-if="field.type === 'rate'"
          :value="value"
          :disabled="disabledHandler(field)"
          :count="field.count || 5"
          :allow-half="field.allowHalf"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 颜色选择器 -->
        <n-color-picker
          v-else-if="field.type === 'color'"
          :value="value"
          :disabled="disabledHandler(field)"
          :show-alpha="field.showAlpha"
          :modes="field.modes || ['hex']"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 级联选择 -->
        <n-cascader
          v-else-if="field.type === 'cascader'"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :options="currentOptions"
          :clearable="field.clearable !== false"
          :filterable="field.filterable"
          :multiple="field.multiple"
          :cascade="field.cascade !== false"
          :show-path="field.showPath !== false"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 系统组织树选择 -->
        <n-tree-select
          v-else-if="isOrgTreeSelectField(field)"
          v-bind="field.props"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :options="currentOptions"
          :loading="remoteLoading"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :multiple="field.multiple"
          :cascade="field.cascade !== false"
          @update:value="handleTreeSelectUpdate(field, $event)"
          v-on="getComponentEvents(field)"
        />

        <!-- 系统用户选择 -->
        <UserSelectPicker
          v-else-if="isUserSelectField(field)"
          v-bind="field.props"
          :model-value="value"
          :label-value="resolveUserSelectLabel(field)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :multiple="field.multiple"
          :size="field.size"
          @update:model-value="handleUpdate"
          @update:label-value="handleUserSelectLabelUpdate(field, $event)"
          @select="handleUserSelect(field, $event)"
        />

        <!-- 行政区划树选择 -->
        <RegionTreeSelect
          v-else-if="field.type === 'regionTreeSelect'"
          :model-value="value"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :virtual-disabled="field.props?.virtualDisabled ?? !context?.isSearch"
          v-bind="field.props"
          @update:model-value="handleRegionTreeSelectUpdate(field, $event)"
        />

        <!-- 树形选择 -->
        <n-tree-select
          v-else-if="field.type === 'treeSelect'"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :options="currentOptions"
          :loading="remoteLoading"
          :clearable="field.clearable !== false"
          :filterable="field.filterable"
          :multiple="field.multiple"
          :cascade="field.cascade !== false"
          :show-path="field.showPath !== false"
          v-bind="field.props"
          @update:value="handleTreeSelectUpdate(field, $event)"
          v-on="getComponentEvents(field)"
        />

        <!-- 穿梭框 -->
        <n-transfer
          v-else-if="field.type === 'transfer'"
          :value="resolveOptionValue(value)"
          :disabled="disabledHandler(field)"
          :options="currentOptions"
          :filterable="field.filterable"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 远程搜索下拉框 -->
        <AiCustomSelect
          v-else-if="field.type === 'customSelect'"
          :value="value"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :api="field.api"
          :method="field.method"
          :label-field="field.labelField || field.props?.labelName || 'label'"
          :value-field="field.valueField || field.props?.valueName || 'value'"
          :filterable="field.filterable !== false"
          :multiple="field.multiple"
          :remote="field.remote"
          :options="field.options"
          :params="field.params"
          :transform="field.transform"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 业务对象引用选择 -->
        <n-select
          v-else-if="field.type === 'objectReference'"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :options="currentOptions"
          :loading="remoteLoading"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :remote="objectReferenceRemoteEnabled"
          :multiple="field.multiple"
          v-bind="field.props"
          @search="handleObjectReferenceSearch"
          @update:value="handleObjectReferenceUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 通用业务记录选择器 -->
        <n-input-group v-else-if="field.type === 'recordSelector'">
          <n-input
            :value="recordSelectorDisplayText"
            :placeholder="getPlaceholder(field)"
            :disabled="disabledHandler(field)"
            readonly
            clearable
            v-bind="field.props"
            @clear="clearRecordSelectorValue"
          />
          <n-button :disabled="disabledHandler(field)" @click="openRecordSelector">
            选择
          </n-button>
        </n-input-group>

        <!-- 纯文本展示 -->
        <div
          v-else-if="field.type === 'text'"
          class="ai-form-readonly-text"
          :style="field.style"
        >
          <span v-if="field.formatter">
            {{ field.formatter(value, field, formData) }}
          </span>
          <FieldValueRenderer
            v-else
            :value="value"
            :row="formData"
            :field="field"
            :setting="field.renderConfig || field.props?.renderConfig || {}"
            :context="context"
          />
          <n-button
            v-if="field.copy"
            text
            size="small"
            style="margin-left: 8px"
            @click="handleCopy(value)"
          >
            <template #icon>
              <n-icon><CopyOutline /></n-icon>
            </template>
          </n-button>
        </div>

        <!-- 自定义插槽 -->
        <slot
          v-else-if="field.type === 'slot'"
          :name="field.slotName || field.field"
          :value="value"
          :field="field"
          :form-data="formData"
          :update-value="handleUpdate"
        />

        <!-- 默认为输入框 -->
        <n-input
          v-else
          :value="value"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          v-bind="field.props"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />
      </div>
      <div v-if="showFieldEventFeedback" class="ai-form-field-event">
        <n-button
          v-if="scanFieldEventEnabled && field.type !== 'barcodeScanner'"
          class="ai-form-field-event__action"
          text
          type="primary"
          size="tiny"
          :loading="scanLoading"
          :disabled="disabledHandler(field) || scanLoading"
          @mousedown.prevent
          @click="handleScanFieldEvent"
        >
          {{ scanLoading ? '扫码中' : '扫码' }}
        </n-button>
        <n-button
          v-if="manualFieldEventEnabled"
          class="ai-form-field-event__action"
          text
          type="primary"
          size="tiny"
          :loading="fieldEventState.loading"
          :disabled="disabledHandler(field) || fieldEventState.loading"
          @mousedown.prevent
          @click="handleManualFieldEvent"
        >
          {{ fieldEventState.loading ? '查询中' : '查询' }}
        </n-button>
        <span
          v-if="scanFeedback.message"
          class="ai-form-field-event__message"
          :class="`is-${scanFeedback.status}`"
        >
          {{ scanFeedback.message }}
        </span>
        <span
          v-if="fieldEventState.message"
          class="ai-form-field-event__message"
          :class="`is-${fieldEventState.status}`"
        >
          {{ fieldEventState.message }}
        </span>
        <span v-else-if="fieldEventState.loading && !manualFieldEventEnabled" class="ai-form-field-event__message is-loading">
          查询中…
        </span>
      </div>
      <p v-if="fieldDescription" class="ai-form-item-description">
        {{ fieldDescription }}
      </p>
    </div>
  </n-form-item>

  <AiRecordSelectorModal
    v-if="field.type === 'recordSelector'"
    v-model:show="recordSelectorVisible"
    :title="field.props?.selectorTitle || field.selectorTitle || `选择${field.label || '记录'}`"
    :suite-code="recordSelectorConfig.suiteCode"
    :object-code="recordSelectorConfig.objectCode"
    :business-object-code="recordSelectorConfig.businessObjectCode"
    :target-object-code="recordSelectorConfig.targetObjectCode"
    :target-entity-code="recordSelectorConfig.targetEntityCode"
    :candidate-object-code="recordSelectorConfig.candidateObjectCode"
    :reference-object-code="recordSelectorConfig.referenceObjectCode"
    :ref-object-code="recordSelectorConfig.refObjectCode"
    :source-object-code="recordSelectorConfig.sourceObjectCode"
    :target-code="recordSelectorConfig.targetCode"
    :multiple="false"
    :display-fields="recordSelectorConfig.displayFields"
    :keyword-fields="recordSelectorConfig.keywordFields"
    :field-mappings="recordSelectorConfig.fieldMappings"
    :search-params="recordSelectorConfig.searchParams"
    :runtime-context="recordSelectorRuntimeContext"
    @confirm="handleRecordSelectorConfirm"
  />
</template>

<script setup>
import { CopyOutline } from '@vicons/ionicons5'
import { useClipboard } from '@vueuse/core'
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { queryBusinessRecordSelector } from '@/api/business-app'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import DictSelect from '@/components/DictSelect.vue'
import FileUpload from '@/components/file-upload/index.vue'
import ImageUpload from '@/components/image-upload/index.vue'
import FieldValueRenderer from '@/components/lowcode-builder/shared/FieldValueRenderer.vue'
import { isPageWidgetComponentKey } from '@/components/lowcode-builder/shared/page-widget-schema'
import PageWidgetRenderer from '@/components/lowcode-builder/shared/PageWidgetRenderer.vue'
import { resolveRuntimeControl } from '@/components/lowcode-builder/shared/runtime-rules'
import RegionTreeSelect from '@/components/RegionTreeSelect.vue'
import { getDictData } from '@/composables/useDict'
import { request } from '@/utils'
import AiCustomSelect from './AiCustomSelect.vue'
import AiFormGroupTitle from './AiFormGroupTitle.vue'
import AiFormSectionTitle from './AiFormSectionTitle.vue'
import AiRecordSelectorModal from './AiRecordSelectorModal.vue'
import { isInputLikeFieldType, isNumberFieldType } from './field-type-utils'
import { applyRecordFieldMappings, extractSelectorRawRecord, normalizeRecordSelectorConfig } from './record-selector-utils'
import { resolveSelectionLabelFields as buildSelectionLabelFields } from './selection-label-fields'

const props = defineProps({
  field: {
    type: Object,
    required: true,
  },
  value: {
    type: [String, Number, Boolean, Array, Object],
    default: null,
  },
  formData: {
    type: Object,
    default: () => ({}),
  },
  context: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:value'])

const route = useRoute()
const { copy } = useClipboard()
const remoteOptions = ref([])
const remoteLoading = ref(false)
const dictOptions = ref([])
const sourceDictOptions = ref([])
const recordSelectorVisible = ref(false)
const scanLoading = ref(false)
const scanFeedback = ref({ status: 'idle', message: '' })
const pickerDefaultTimestamp = Date.now()
let remoteRequestSeq = 0

const ORG_TREE_SELECT_TYPES = new Set([
  'orgTreeSelect',
  'orgSelect',
  'organizationSelect',
  'departmentSelect',
  'departmentTreeSelect',
  'deptSelect',
  'deptTreeSelect',
  'elTreeSelect',
  'orgName',
  'deptName',
  'forgeOrgTreeSelect',
])
const USER_SELECT_TYPES = new Set([
  'userSelect',
  'userPicker',
  'user',
  'userName',
  'sysUserSelect',
  'forgeUserSelect',
])
const READONLY_SELECTION_TYPES = new Set([
  'select',
  'dictSelect',
  'radio',
  'radioButton',
  'checkbox',
  'cascader',
  'treeSelect',
  'orgTreeSelect',
  'transfer',
  'objectReference',
])

const fieldRuntimeControl = computed(() => resolveRuntimeControl(props.field || {}, {
  ...(props.context || {}),
  record: props.formData || {},
  row: props.context?.currentRow || props.context?.row || props.formData || {},
  formData: props.formData || {},
  data: props.formData || {},
  route: {
    query: route.query || {},
    params: route.params || {},
    path: route.path,
    fullPath: route.fullPath,
    name: route.name,
  },
}))
const fieldRuntimeVisible = computed(() => fieldRuntimeControl.value.visible !== false)

/**
 * 获取占位符文本
 */
function getPlaceholder(field) {
  if (field.placeholder) {
    return field.placeholder
  }

  const prefix = isInputLikeFieldType(field.type) ? '请输入' : '请选择'
  return `${prefix}${field.label}`
}

/**
 * 处理禁用状态
 */
function disabledHandler(field) {
  if (isCascadeDisabledByEmptyParent())
    return true
  if (fieldRuntimeControl.value.readonly || fieldRuntimeControl.value.disabled)
    return true
  if (typeof field.disabled === 'boolean') {
    return field.disabled
  }
  if (typeof field.disabled === 'function') {
    return field.disabled({
      formData: props.formData,
      field,
      context: props.context,
    })
  }
  return false
}

const fieldAlign = computed(() => normalizeAlign(props.field?.align || props.field?.textAlign || props.field?.props?.align))
const fieldAlignClass = computed(() => fieldAlign.value === 'left' ? '' : `ai-form-item-align-${fieldAlign.value}`)
const formItemClass = computed(() => [
  `ai-form-item--${props.field?.type || 'input'}`,
  (props.field?.readonly || props.field?.props?.readonly) ? 'ai-form-item--readonly' : '',
  fieldAlignClass.value,
  props.field?.field ? `ai-form-item-field-${props.field.field}` : '',
  props.field?.formItemClass,
].filter(Boolean))
const componentControlStyle = computed(() => {
  const runtimeStyle = fieldRuntimeControl.value.style || {}
  return Object.keys(runtimeStyle).length
    ? { ...(props.field?.componentStyle || props.field?.style || {}), ...runtimeStyle }
    : props.field?.componentStyle || props.field?.style || undefined
})
const componentControlClass = computed(() => [
  `ai-form-control--${props.field?.type || 'input'}`,
  fieldRuntimeControl.value.className,
  props.field?.componentClass,
].filter(Boolean))
const recordSelectorConfig = computed(() => normalizeRecordSelectorConfig(props.field))
const recordSelectorRuntimeContext = computed(() => ({
  ...(props.context || {}),
  formData: props.formData || {},
  form: props.formData || {},
  record: props.context?.record || props.formData || {},
  row: props.context?.currentRow || props.context?.row || props.formData || {},
  query: route.query || {},
  params: route.params || {},
  route: {
    query: route.query || {},
    params: route.params || {},
    path: route.path,
    fullPath: route.fullPath,
    name: route.name,
  },
}))
const recordSelectorDisplayText = computed(() => {
  const config = recordSelectorConfig.value
  const labelField = props.field?.labelField || props.field?.props?.labelField || props.field?.props?.targetLabelField || config.targetLabelField || config.labelTargetField
  if (labelField && props.formData?.[labelField])
    return props.formData[labelField]
  return normalizeDisplayText(props.value)
})
const fieldDescription = computed(() => props.field?.props?.description || props.field?.description || '')
const fieldLabelTip = computed(() => props.field?.props?.labelTip || props.field?.labelTip || '')
const fieldBadge = computed(() => props.field?.props?.badge || props.field?.badge || '')
const manualFieldEventEnabled = computed(() => props.context?.hasFieldEvent?.('MANUAL', props.field?.field) === true)
const blurFieldEventEnabled = computed(() => props.context?.hasFieldEvent?.('BLUR', props.field?.field) === true)
const scanFieldEventEnabled = computed(() => props.context?.hasFieldEvent?.('SCAN_COMPLETE', props.field?.field) === true)
const fieldEventState = computed(() => props.context?.getFieldEventState?.(props.field?.field) || {
  status: 'idle',
  loading: false,
  message: '',
})
const showFieldEventFeedback = computed(() => manualFieldEventEnabled.value
  || scanFieldEventEnabled.value
  || scanFeedback.value.message
  || fieldEventState.value.loading
  || Boolean(fieldEventState.value.message))
const isSectionTitleField = computed(() => {
  return !isLegacyGroupTitleField(props.field) && ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
    .includes(props.field?.type || props.field?.componentKey || props.field?.nodeType)
})
const isGroupTitleField = computed(() => {
  return isLegacyGroupTitleField(props.field) || ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
    .includes(props.field?.type || props.field?.componentKey || props.field?.nodeType)
})
const isFieldRequired = computed(() => {
  if (Object.prototype.hasOwnProperty.call(fieldRuntimeControl.value, 'required') && fieldRuntimeControl.value.required !== undefined)
    return fieldRuntimeControl.value.required === true
  if (props.field?.required === true)
    return true
  const rules = props.field?.rules
  if (!rules)
    return false
  return (Array.isArray(rules) ? rules : [rules]).some(rule => rule?.required === true)
})
const fieldDictType = computed(() => props.field?.dictType || props.field?.props?.dictType || '')
const cascadeConfig = computed(() => resolveCascadeConfig(props.field))
const dictCascadeConfig = computed(() => {
  if (!cascadeConfig.value)
    return null
  return {
    ...cascadeConfig.value,
    sourceOptions: sourceDictOptions.value,
  }
})
const remoteOptionSource = computed(() => {
  if (shouldSuppressDesignerRemoteOptions(props.field))
    return null
  return resolveDynamicOptionSource(props.field)
})
const runtimePageWidgetKey = computed(() => {
  const field = props.field || {}
  const componentKey = String(field.componentKey || '').trim()
  if (componentKey && isPageWidgetComponentKey(componentKey))
    return componentKey
  const type = String(field.type || '').trim()
  return isPageWidgetComponentKey(type) ? type : ''
})
const isRuntimePageWidgetField = computed(() => {
  if (!runtimePageWidgetKey.value)
    return false
  const bindingMode = props.field?.fieldBinding?.mode
  // 显式 virtual 绑定 或 未配置 fieldBinding 的挂件组件（预览模式兜底）
  if (bindingMode === 'virtual' || props.field?.virtual === true || props.field?.isVirtual === true)
    return true
  return !props.field?.fieldBinding && !props.field?.field
})
const runtimePageWidgetProps = computed(() => {
  const field = props.field || {}
  const widgetKey = runtimePageWidgetKey.value
  const fieldProps = field.props && typeof field.props === 'object' ? field.props : {}
  const next = {
    ...fieldProps,
  }
  if (field.label && !next.title && ['rich-text', 'markdown', 'barcode', 'qrcode', 'transfer', 'audio-player', 'video-player', 'iframe'].includes(widgetKey))
    next.title = field.label
  if (isFilledValue(props.value)) {
    if (widgetKey === 'rich-text' || widgetKey === 'markdown')
      next.content = props.value
    else if (widgetKey === 'transfer')
      next.value = Array.isArray(props.value) ? props.value : String(props.value).split(',').map(item => item.trim()).filter(Boolean)
    else if (widgetKey === 'barcode' || widgetKey === 'qrcode')
      next.value = props.value
    else if (widgetKey === 'audio-player' || widgetKey === 'video-player' || widgetKey === 'iframe' || widgetKey === 'avatar')
      next.src = props.value
  }
  return next
})

function isLegacyGroupTitleField(field = {}) {
  const fieldProps = field?.props || {}
  return field?.nodeType === 'divider'
    && !field?.componentKey
    && Object.prototype.hasOwnProperty.call(fieldProps, 'description')
    && !Object.prototype.hasOwnProperty.call(fieldProps, 'title')
}
const cascadeSourceValue = computed(() => {
  const cascade = cascadeConfig.value
  return cascade?.enabled && cascade.sourceField ? props.formData?.[cascade.sourceField] : undefined
})
const sourceFieldConfig = computed(() => findSchemaField(cascadeConfig.value?.sourceField))
const sourceDictType = computed(() => cascadeConfig.value?.sourceDictType || sourceFieldConfig.value?.dictType || sourceFieldConfig.value?.props?.dictType || '')

function isCascadeDisabledByEmptyParent() {
  const cascade = cascadeConfig.value
  if (!cascade?.enabled || cascade.emptyStrategy !== 'disabled' || !cascade.sourceField)
    return false
  const sourceValue = props.formData?.[cascade.sourceField]
  return sourceValue === null || sourceValue === undefined || sourceValue === ''
}

watch(
  remoteOptionSource,
  (source) => {
    if (!source) {
      remoteOptions.value = []
      return
    }
    loadRemoteOptions(source)
  },
  { immediate: true, deep: true },
)

watch(fieldDictType, loadDictOptions, { immediate: true })
watch(sourceDictType, loadSourceDictOptions, { immediate: true })
watch(cascadeSourceValue, (value, oldValue) => {
  if (oldValue === undefined || value === oldValue || !cascadeConfig.value?.clearOnParentChange)
    return
  clearCurrentValue()
})

/**
 * 获取选项数据 - 使用 computed 确保响应式
 */
const currentOptions = computed(() => {
  const field = props.field

  // 优先使用 options 函数
  if (typeof field.options === 'function') {
    const result = field.options({
      formData: props.formData,
      field,
      context: props.context,
    })

    // 如果返回的是 Promise，需要在外部处理
    // 这里我们检查是否有缓存的选项
    if (result instanceof Promise) {
      // 如果有缓存的选项，使用缓存
      if (field._cachedOptions && Array.isArray(field._cachedOptions)) {
        return withCurrentValueOption(resolveCascadedOptions(field._cachedOptions))
      }
      // 否则返回空数组，并异步加载
      cacheAsyncOptions(field, result)
      return []
    }

    return withCurrentValueOption(resolveCascadedOptions(result))
  }

  // 其次使用 options 数组
  if (field.options && Array.isArray(field.options) && field.options.length > 0) {
    return withCurrentValueOption(resolveCascadedOptions(field.options))
  }

  // 检查 props.options（兼容旧的配置方式）
  if (field.props?.options && Array.isArray(field.props.options) && field.props.options.length > 0) {
    return withCurrentValueOption(resolveCascadedOptions(field.props.options))
  }

  const currentChildrenSource = resolveCurrentChildrenSource(field)
  if (currentChildrenSource) {
    return withCurrentValueOption(resolveCascadedOptions(
      buildCurrentChildrenOptions(currentChildrenSource, props.context),
    ))
  }

  if (fieldDictType.value) {
    const options = resolveCascadedOptions(dictOptions.value)
    if (field.type === 'cascader')
      return buildDictTreeOptions(options)
    return withCurrentValueOption(options)
  }

  if (remoteOptionSource.value) {
    return withCurrentValueOption(resolveCascadedOptions(remoteOptions.value))
  }

  // 最后处理 enumType (仅当 options 为空时)
  if (field.enumType) {
    // 这里应该根据 enumType 获取对应的枚举数据
    // 由于这是一个示例,我们返回一个空数组
    // 在实际项目中,这里应该从父组件传递的 context 中获取数据
    // 或者通过 props 传递具体的选项数据
    console.warn(`字段 ${field.field} 使用了 enumType: ${field.enumType},但未提供具体选项数据`)
    return []
  }

  return []
})

function withCurrentValueOption(options = []) {
  const result = Array.isArray(options) ? [...options] : []
  const field = props.field || {}
  if (props.value === null || props.value === undefined || props.value === '')
    return result
  const labelValue = resolveSelectionLabelValue(field)
  if (labelValue === null || labelValue === undefined || labelValue === '')
    return result
  const values = Array.isArray(props.value)
    ? props.value
    : field.multiple && typeof props.value === 'string'
      ? props.value.split(',').map(item => item.trim()).filter(Boolean)
      : [props.value]
  const labels = Array.isArray(labelValue)
    ? labelValue
    : String(labelValue).split(',').map(item => item.trim()).filter(Boolean)
  values.forEach((value, index) => {
    if (flattenOptionNodes(result).some(option => isSameOptionValue(option?.value ?? option?.key, value)))
      return
    result.unshift({
      value,
      key: value,
      label: labels[index] || labels[0] || String(value),
    })
  })
  return result
}

function cacheAsyncOptions(field, promise) {
  promise.then((options) => {
    field._cachedOptions = options
  })
}

async function loadDictOptions(dictType) {
  if (!dictType) {
    dictOptions.value = []
    return
  }
  dictOptions.value = await getDictData(dictType)
}

async function loadSourceDictOptions(dictType) {
  if (!dictType) {
    sourceDictOptions.value = []
    return
  }
  sourceDictOptions.value = await getDictData(dictType)
}

function resolveOptionSource(field = {}) {
  if (isUserSelectField(field))
    return null
  if (shouldSuppressDesignerRemoteOptions(field))
    return null
  const configuredSource = field.optionSource || field.props?.optionSource
  if (hasEffectiveOptionSource(configuredSource)) {
    const source = normalizeOptionSource(configuredSource)
    if (shouldSuppressDesignerRemoteOptions(field, source))
      return null
    return source
  }
  if (isObjectReferenceField(field)) {
    const generated = buildObjectReferenceOptionSource(field)
    if (generated)
      return generated
  }
  if (isOrgTreeSelectField(field)) {
    return {
      type: 'tree',
      api: 'get@/system/org/tree',
      valueField: 'id',
      keyField: 'id',
      labelField: 'orgName',
      fallbackLabelFields: ['name'],
      childrenField: 'children',
    }
  }
  return null
}

function shouldSuppressDesignerRemoteOptions(field = {}, source = null) {
  if (!isDesignerPreviewContext())
    return false
  if (isObjectReferenceField(field) || isRecordSelectorField(field))
    return true
  if (source?.type === 'businessRecordSelector')
    return true
  return String(source?.api || field?.optionSource?.api || field?.props?.optionSource?.api || '').includes('selector/query')
}

function isDesignerPreviewContext() {
  const context = props.context || {}
  const mode = String(context.mode || context.source || context.scene || '').trim()
  return context.designerPreview === true
    || context.designMode === true
    || ['designer', 'designer-preview', 'form-designer', 'design', 'canvas'].includes(mode)
}

function hasEffectiveOptionSource(source) {
  if (!source)
    return false
  if (typeof source === 'string')
    return source.trim() !== ''
  if (typeof source !== 'object')
    return false
  if (['CURRENT_CHILDREN', 'current_children', 'currentChildren'].includes(String(source.type || '')))
    return true
  return Boolean(
    String(source.api || source.url || '').trim()
    || Array.isArray(source.options)
    || Array.isArray(source.data),
  )
}

function resolveCurrentChildrenSource(field = {}) {
  const source = field.optionSource || field.props?.optionSource
  if (!source || typeof source !== 'object')
    return null
  return ['CURRENT_CHILDREN', 'current_children', 'currentChildren'].includes(String(source.type || ''))
    ? source
    : null
}

function buildCurrentChildrenOptions(source = {}, context = {}) {
  const relationKey = String(source.relationKey || source.childKey || '').trim()
  const collections = context?.childCollections && typeof context.childCollections === 'object'
    ? context.childCollections
    : {}
  const rows = relationKey && Array.isArray(collections[relationKey]) ? collections[relationKey] : []
  const valueField = source.valueField || 'id'
  const labelField = source.labelField || 'label'
  const disabledField = source.disabledField || ''
  const seen = new Set()
  return rows
    .filter(row => row && (!source.persistedOnly || hasPersistedOptionId(row)))
    .map((row) => {
      const value = row[valueField]
      if (value === null || value === undefined || value === '' || seen.has(String(value)))
        return null
      seen.add(String(value))
      const label = row[labelField] ?? row.name ?? row.title ?? value
      return {
        ...row,
        value,
        key: row.key ?? value,
        label: String(label),
        ...(disabledField ? { disabled: row[disabledField] === true } : {}),
      }
    })
    .filter(Boolean)
}

function hasPersistedOptionId(row = {}) {
  const value = row.id ?? row.ID ?? row.recordId
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function normalizeOptionSource(source) {
  if (typeof source === 'string')
    return { api: source }
  const next = { ...(source || {}) }
  if (!next.api && next.url)
    next.api = next.url
  if (!next.params && typeof next.paramsText === 'string')
    next.params = safeParseObject(next.paramsText)
  return next
}

function safeParseObject(value = '') {
  const text = String(value || '').trim()
  if (!text)
    return {}
  try {
    const parsed = JSON.parse(text)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return text.split(/[&\n]/).reduce((result, item) => {
      const content = item.trim()
      if (!content)
        return result
      const separator = content.includes('=') ? '=' : content.includes(':') ? ':' : ''
      if (!separator)
        return result
      const index = content.indexOf(separator)
      const key = content.slice(0, index).trim()
      const itemValue = content.slice(index + 1).trim()
      if (key && itemValue)
        result[key] = itemValue
      return result
    }, {})
  }
}

function resolveDynamicOptionSource(field = {}) {
  const source = resolveOptionSource(field)
  if (!source)
    return null
  const next = {
    ...source,
    params: resolveDynamicParams(source.params || {}),
  }
  ensureSelectorQueryObjectCode(next, field)
  const cascade = cascadeConfig.value
  if (cascade?.enabled && cascade.mode === 'remoteParam' && cascade.sourceField && cascade.paramName) {
    const sourceValue = props.formData?.[cascade.sourceField]
    if ((sourceValue === null || sourceValue === undefined || sourceValue === '') && cascade.emptyStrategy !== 'all') {
      next.waitForParent = true
    }
    next.params = {
      ...next.params,
      [cascade.paramName]: sourceValue,
    }
  }
  return next
}

function ensureSelectorQueryObjectCode(source = {}, field = {}) {
  if (!String(source.api || '').includes('selector/query'))
    return source
  const objectCode = isObjectReferenceField(field)
    ? resolveObjectReferenceConfig(field).objectCode
    : normalizeRecordSelectorConfig({
      ...field,
      ...source,
      params: source.params,
    }).objectCode
  if (!objectCode)
    return source
  source.params = {
    ...(source.params || {}),
    objectCode,
    businessObjectCode: source.params?.businessObjectCode || objectCode,
    targetObjectCode: source.params?.targetObjectCode || objectCode,
  }
  return source
}

function resolveDynamicParams(params = {}) {
  const result = {}
  Object.entries(params || {}).forEach(([key, value]) => {
    if (typeof value === 'string') {
      const matched = value.match(/^\$\{(.+)\}$/) || value.match(/^\$form\.(.+)$/)
      result[key] = matched ? props.formData?.[matched[1]] : value
      return
    }
    result[key] = value
  })
  return result
}

async function loadRemoteOptions(source, keyword = '') {
  if (!source)
    return
  if (source.waitForParent) {
    remoteOptions.value = []
    return
  }
  if (source.type === 'businessRecordSelector') {
    const objectCode = normalizeRecordSelectorConfig(source).objectCode
    if (!objectCode) {
      remoteOptions.value = []
      return
    }
  }
  const requestSeq = ++remoteRequestSeq
  remoteLoading.value = true
  try {
    if (source.type === 'businessRecordSelector') {
      const objectCode = normalizeRecordSelectorConfig(source).objectCode
      const selectorPayload = {
        ...(source.params || {}),
        objectCode,
        businessObjectCode: source.businessObjectCode || source.params?.businessObjectCode || objectCode,
        targetObjectCode: source.targetObjectCode || source.params?.targetObjectCode || objectCode,
        keyword: keyword || undefined,
        keywordFields: source.keywordFields || source.params?.keywordFields || [],
        displayFields: source.displayFields || source.params?.displayFields || [],
        searchParams: source.searchParams || source.params?.searchParams || {},
      }
      const res = await queryBusinessRecordSelector(selectorPayload, {
        pageNum: source.pageNum || 1,
        pageSize: source.pageSize || 100,
      })
      if (requestSeq !== remoteRequestSeq)
        return
      remoteOptions.value = normalizeRemoteOptions(res?.data || {}, source)
      return
    }

    if (!source.api)
      return
    if (isSelectorQueryApi(source.api)) {
      const selectorConfig = normalizeRecordSelectorConfig({
        ...props.field,
        ...source,
        ...(source.params || {}),
        params: source.params,
      })
      const objectCode = selectorConfig.objectCode
      if (!objectCode) {
        remoteOptions.value = []
        return
      }
      const selectorPayload = {
        ...(source.params || {}),
        objectCode,
        businessObjectCode: selectorConfig.businessObjectCode || objectCode,
        targetObjectCode: selectorConfig.targetObjectCode || objectCode,
        keyword: keyword || undefined,
        keywordFields: source.keywordFields || source.params?.keywordFields || [],
        displayFields: source.displayFields || source.params?.displayFields || [],
        searchParams: source.searchParams || source.params?.searchParams || {},
      }
      const res = await queryBusinessRecordSelector(selectorPayload, {
        pageNum: source.pageNum || 1,
        pageSize: source.pageSize || 100,
      })
      if (requestSeq !== remoteRequestSeq)
        return
      remoteOptions.value = normalizeRemoteOptions(res?.data || {}, source)
      return
    }
    const { method, url } = parseOptionApi(source.api)
    const params = {
      ...(source.params || {}),
    }
    if (keyword && source.keywordParam)
      params[source.keywordParam] = keyword
    const res = await request({
      method,
      url,
      params: method === 'get' ? params : undefined,
      data: method === 'get' ? undefined : params,
    })
    if (requestSeq !== remoteRequestSeq)
      return
    remoteOptions.value = normalizeRemoteOptions(res, source)
  }
  catch (error) {
    console.warn(`[AiFormItem] 加载 ${props.field?.field || ''} 选项失败:`, {
      error,
      field: props.field,
      source,
      selectorConfig: normalizeRecordSelectorConfig({
        ...props.field,
        ...source,
        ...(source?.params || {}),
        params: source?.params,
      }),
      context: props.context,
    })
    remoteOptions.value = []
  }
  finally {
    if (requestSeq === remoteRequestSeq)
      remoteLoading.value = false
  }
}

function isSelectorQueryApi(api = '') {
  return String(api || '').includes('selector/query')
}

function parseOptionApi(api) {
  const text = String(api || '')
  const [method, ...urlParts] = text.includes('@') ? text.split('@') : ['get', text]
  return {
    method: String(method || 'get').toLowerCase(),
    url: urlParts.join('@') || text,
  }
}

function normalizeRemoteOptions(data, source = {}) {
  const rows = extractOptionRows(data, source)
  if (!Array.isArray(rows))
    return []
  const isTree = source.type === 'tree'
  return rows.map(row => normalizeOptionNode(row, source, isTree)).filter(Boolean)
}

function extractOptionRows(data, source = {}, depth = 0) {
  if (Array.isArray(data))
    return data
  if (!data || typeof data !== 'object' || depth > 4)
    return []
  if (source.recordsField) {
    const nested = getNestedValue(data, source.recordsField)
    if (Array.isArray(nested))
      return nested
  }
  for (const key of ['records', 'list', 'rows', 'items']) {
    if (Array.isArray(data[key]))
      return data[key]
  }
  if (Array.isArray(data.data))
    return data.data
  if (data.data && typeof data.data === 'object')
    return extractOptionRows(data.data, source, depth + 1)
  return []
}

function normalizeOptionNode(row, source = {}, includeChildren = false) {
  if (!row || typeof row !== 'object')
    return null
  const valueField = source.valueField || source.keyField || 'value'
  const keyField = source.keyField || valueField
  const labelField = source.labelField || 'label'
  const childrenField = source.childrenField || 'children'
  const fallbackValueFields = source.fallbackValueFields || ['value', 'key', keyField, 'id', 'orgId', 'deptId', 'code']
  const fallbackLabelFields = source.fallbackLabelFields || ['label', 'name', 'title', 'orgName', 'deptName', 'orgShortName']
  const value = resolveFirstFilled(row, [valueField, ...fallbackValueFields])
  const label = resolveFirstFilled(row, [labelField, ...fallbackLabelFields])
  if (value === undefined || value === null || value === '')
    return null
  const option = {
    ...row,
    value,
    key: row.key ?? row[keyField] ?? value,
    label: label === undefined || label === null || label === '' ? String(value ?? '') : String(label),
  }
  if (includeChildren) {
    const children = Array.isArray(row[childrenField])
      ? row[childrenField]
      : Array.isArray(row.children)
        ? row.children
        : []
    option.children = children.map(child => normalizeOptionNode(child, source, true)).filter(Boolean)
  }
  return option
}

function resolveFirstFilled(source, fields = []) {
  const keys = fields.filter((field, index, all) => field && all.indexOf(field) === index)
  for (const key of keys) {
    const value = source?.[key]
    if (value !== undefined && value !== null && value !== '')
      return value
  }
  return undefined
}

function resolveCascadeConfig(field = {}) {
  const configured = [field.cascade, field.cascadeConfig, field.props?.cascade, field.props?.cascadeConfig]
    .find(item => item && typeof item === 'object' && item.sourceField)
  const raw = configured || {
    sourceField: field.sourceField || field.props?.sourceField,
    sourceDictType: field.sourceDictType || field.props?.sourceDictType,
    linkedDictType: field.linkedDictType || field.props?.linkedDictType,
    mode: field.matchMode || field.props?.matchMode || field.mode || field.props?.mode,
    paramName: field.paramName || field.props?.paramName,
    emptyStrategy: field.emptyStrategy || field.props?.emptyStrategy,
    clearOnParentChange: field.clearOnParentChange ?? field.clearOnSourceChange ?? field.props?.clearOnParentChange ?? field.props?.clearOnSourceChange,
  }
  if (!raw || raw.enabled === false || !raw.sourceField)
    return null
  return {
    enabled: true,
    sourceField: raw.sourceField,
    sourceDictType: raw.sourceDictType || '',
    linkedDictType: raw.linkedDictType || '',
    mode: raw.mode || raw.matchMode || 'linkedDict',
    paramName: raw.paramName || '',
    emptyStrategy: raw.emptyStrategy || 'empty',
    clearOnParentChange: raw.clearOnParentChange !== false && raw.clearOnSourceChange !== false,
  }
}

function resolveCascadedOptions(options = []) {
  const cascade = cascadeConfig.value
  if (!cascade?.enabled || !cascade.sourceField)
    return options
  const sourceValue = props.formData?.[cascade.sourceField]
  if (sourceValue === null || sourceValue === undefined || sourceValue === '')
    return cascade.emptyStrategy === 'all' ? options : []
  if (cascade.mode === 'remoteParam')
    return options
  return (Array.isArray(options) ? options : []).filter(option => matchesCascade(option, sourceValue, cascade))
}

function matchesCascade(option, sourceValue, cascade) {
  const raw = option.raw || option
  if (cascade.mode === 'parentDictCode') {
    const parentDictCode = raw.parentDictCode ?? raw.parent_dict_code
    const sourceDictCode = resolveSourceDictCode(sourceValue)
    return isSameOptionValue(parentDictCode, sourceDictCode) || isSameOptionValue(parentDictCode, sourceValue)
  }
  if (cascade.mode === 'linkedDict') {
    const linkedType = raw.linkedDictType ?? raw.linked_dict_type
    const linkedValue = raw.linkedDictValue ?? raw.linked_dict_value
    const expectedType = cascade.linkedDictType || cascade.sourceDictType || sourceDictType.value
    const typeMatched = !expectedType || isSameOptionValue(linkedType, expectedType)
    return typeMatched && isSameOptionValue(linkedValue, sourceValue)
  }
  return true
}

function resolveSourceDictCode(sourceValue) {
  const matched = sourceDictOptions.value.find(option => isSameOptionValue(option.value, sourceValue))
  return matched?.dictCode ?? matched?.raw?.dictCode ?? sourceValue
}

function buildDictTreeOptions(options = []) {
  const nodes = (Array.isArray(options) ? options : []).map(option => ({
    ...option,
    key: option.dictCode ?? option.key ?? option.value,
    value: option.value,
    label: option.label,
    children: [],
  }))
  const byCode = new Map(nodes.map(node => [String(node.dictCode ?? node.key), node]))
  const roots = []
  nodes.forEach((node) => {
    const parentCode = node.parentDictCode ?? node.raw?.parentDictCode
    if (parentCode !== null && parentCode !== undefined && parentCode !== '' && Number(parentCode) !== 0 && byCode.has(String(parentCode))) {
      byCode.get(String(parentCode)).children.push(node)
    }
    else {
      roots.push(node)
    }
  })
  nodes.forEach((node) => {
    if (!node.children.length)
      delete node.children
  })
  return roots
}

function findSchemaField(fieldName) {
  if (!fieldName)
    return null
  const schemas = [
    ...(props.context?.schema || []),
    ...(props.context?.allSchema || []),
  ]
  return schemas.find(item => item?.field === fieldName) || null
}

function normalizeAlign(value) {
  const align = String(value || '').toLowerCase()
  return ['left', 'center', 'right'].includes(align) ? align : 'left'
}

function clearCurrentValue() {
  if (props.value === null || props.value === undefined || props.value === '')
    return
  emit('update:value', props.field?.multiple ? [] : null)
}

function getNestedValue(source, path) {
  return String(path || '')
    .split('.')
    .filter(Boolean)
    .reduce((value, key) => value?.[key], source)
}

function resolveOptionValue(rawValue) {
  return normalizeOptionValue(rawValue, currentOptions.value, props.field?.multiple)
}

/**
 * n-slider range 模式要求 value 为二元数组；值为 null 时组件内部 (range ? value : [value]).map 会崩溃，
 * 归一化兜底：range 模式非法值回退 [min, max]
 */
function resolveSliderValue(rawValue, field = {}) {
  const isRange = field.range === true || field.props?.range === true
  if (isRange) {
    if (Array.isArray(rawValue) && rawValue.length === 2)
      return rawValue
    return [Number(field.min ?? field.props?.min ?? 0), Number(field.max ?? field.props?.max ?? 100)]
  }
  return rawValue
}

function normalizeOptionValue(rawValue, options = [], multiple = false) {
  if (rawValue === null || rawValue === undefined || rawValue === '' || !Array.isArray(options) || !options.length)
    return rawValue

  if (Array.isArray(rawValue)) {
    return rawValue.map(item => findOptionValue(options, item)).filter(item => item !== undefined)
  }

  if (multiple && typeof rawValue === 'string') {
    return rawValue.split(',').map(item => item.trim()).filter(Boolean).map(item => findOptionValue(options, item)).filter(item => item !== undefined)
  }

  return findOptionValue(options, rawValue)
}

function findOptionValue(options = [], rawValue) {
  const match = flattenOptionNodes(options).find(option => isSameOptionValue(option?.value ?? option?.key, rawValue))
  if (match)
    return match.value ?? match.key
  return rawValue
}

function flattenOptionNodes(options = []) {
  const result = []
  const walk = (nodes) => {
    ;(Array.isArray(nodes) ? nodes : []).forEach((node) => {
      if (!node || typeof node !== 'object')
        return
      result.push(node)
      if (Array.isArray(node.children))
        walk(node.children)
    })
  }
  walk(options)
  return result
}

function isSameOptionValue(left, right) {
  if (left === right)
    return true
  if (left === null || left === undefined || right === null || right === undefined)
    return false
  return String(left) === String(right)
}

function normalizeRuntimeFieldType(type) {
  const value = String(type || '')
  if (ORG_TREE_SELECT_TYPES.has(value))
    return 'orgTreeSelect'
  if (USER_SELECT_TYPES.has(value))
    return 'userSelect'
  return value
}

function isOrgTreeSelectField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType) === 'orgTreeSelect'
}

function isUserSelectField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType) === 'userSelect'
}

function isObjectReferenceField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType || field.componentKey) === 'objectReference'
}

function isRecordSelectorField(field = {}) {
  return normalizeRuntimeFieldType(field.type || field.componentType || field.componentKey) === 'recordSelector'
}

function resolveObjectReferenceConfig(field = {}) {
  const props = field.props || {}
  const objectCode = firstNonBlank(
    field.referenceObjectCode,
    props.referenceObjectCode,
    field.basicProps?.referenceObjectCode,
    field.referenceConfig?.referenceObjectCode,
    props.referenceConfig?.referenceObjectCode,
    field.basicProps?.referenceConfig?.referenceObjectCode,
    normalizeRecordSelectorConfig(field).objectCode,
  )
  return {
    objectCode,
    valueField: firstNonBlank(
      props.referenceValueField,
      props.valueField,
      props.targetValueField,
      field.referenceValueField,
      field.valueField,
      field.targetValueField,
      'id',
    ),
    labelField: firstNonBlank(
      props.referenceDisplayField,
      props.displayField,
      props.labelField,
      props.targetLabelField,
      field.referenceDisplayField,
      field.displayField,
      field.labelField,
      field.targetLabelField,
    ),
  }
}

function firstNonBlank(...values) {
  return values.map(value => String(value ?? '').trim()).find(Boolean) || ''
}

function buildObjectReferenceOptionSource(field = {}) {
  const config = resolveObjectReferenceConfig(field)
  if (!config.objectCode || !config.labelField)
    return null
  return {
    type: 'businessRecordSelector',
    objectCode: config.objectCode,
    valueField: config.valueField,
    labelField: config.labelField,
    recordsField: 'records',
    pageNum: 1,
    pageSize: 100,
    displayFields: [`${config.labelField}:${config.labelField}`],
    keywordFields: [config.labelField],
  }
}

// objectReference remote search support
const objectReferenceRemoteEnabled = computed(() => {
  return !isDesignerPreviewContext()
    && isObjectReferenceField(props.field)
    && Boolean(resolveObjectReferenceConfig(props.field).objectCode)
})
const objectReferenceSearchKeyword = ref('')
let objectReferenceSearchTimer = null

function handleObjectReferenceSearch(keyword) {
  objectReferenceSearchKeyword.value = keyword || ''
  if (objectReferenceSearchTimer)
    clearTimeout(objectReferenceSearchTimer)
  objectReferenceSearchTimer = setTimeout(() => {
    reloadObjectReferenceOptions(keyword)
  }, 300)
}

function handleObjectReferenceUpdate(value) {
  handleUpdate(value)
  // Sync label value from selected option
  syncSelectionLabelFromOptions(props.field, value)
}

async function reloadObjectReferenceOptions(keyword = '') {
  if (isDesignerPreviewContext())
    return
  const config = resolveObjectReferenceConfig(props.field)
  if (!config.objectCode || !config.labelField)
    return
  remoteLoading.value = true
  try {
    const res = await queryBusinessRecordSelector({
      objectCode: config.objectCode,
      keyword: keyword || undefined,
      keywordFields: [config.labelField],
      displayFields: [`${config.labelField}:${config.labelField}`],
    }, { pageNum: 1, pageSize: 50 })
    const records = res.data?.records || []
    remoteOptions.value = records.map(record => ({
      label: record[config.labelField] || record.name || String(record[config.valueField] || record.id || ''),
      value: record[config.valueField] || record.id,
    }))
  }
  catch {
    // Keep existing options on error
  }
  finally {
    remoteLoading.value = false
  }
}

function resolveSelectionLabelValue(field = {}) {
  for (const candidate of resolveSelectionLabelFields(field)) {
    const value = props.formData?.[candidate]
    if (isFilledValue(value))
      return value
  }
  return field.labelValue ?? field.props?.labelValue ?? ''
}

function resolveSelectionLabelFields(field = {}) {
  const selectionType = isUserSelectField(field) ? 'user' : isOrgTreeSelectField(field) ? 'org' : ''
  return buildSelectionLabelFields(field, selectionType)
}

function patchSelectionLabelValue(field = {}, labelValue) {
  const candidates = resolveSelectionLabelFields(field)
  if (!candidates.length || typeof props.context?.patchFormData !== 'function')
    return
  const normalizedLabel = normalizeLabelValue(labelValue)
  const patch = {}
  candidates.forEach((candidate, index) => {
    if (index === 0 || Object.prototype.hasOwnProperty.call(props.formData || {}, candidate))
      patch[candidate] = isFilledValue(normalizedLabel) ? normalizedLabel : undefined
  })
  props.context.patchFormData(patch)
}

function syncSelectionLabelFromOptions(field = {}, value) {
  const values = Array.isArray(value)
    ? value
    : field.multiple && typeof value === 'string'
      ? value.split(',').map(item => item.trim()).filter(Boolean)
      : [value]
  const labels = values
    .map(item => flattenOptionNodes(currentOptions.value).find(option => isSameOptionValue(option?.value ?? option?.key, item))?.label)
    .filter(Boolean)
  if (labels.length)
    patchSelectionLabelValue(field, field.multiple ? labels : labels[0])
}

function normalizeLabelValue(value) {
  if (Array.isArray(value))
    return value.map(item => String(item || '').trim()).filter(Boolean).join(',')
  return value === null || value === undefined ? '' : String(value).trim()
}

function shouldRenderReadonlySelectionText(field = {}) {
  const fieldType = normalizeRuntimeFieldType(field.type || field.componentType)
  return Boolean(field.readonly || field.props?.readonly) && READONLY_SELECTION_TYPES.has(fieldType)
}

function resolveReadonlySelectionText(field = {}) {
  const labels = resolveSelectionDisplayLabels(field)
  if (labels.length)
    return labels.join(', ')
  const labelValue = normalizeLabelValue(resolveSelectionLabelValue(field))
  if (isFilledValue(labelValue))
    return labelValue
  return normalizeDisplayText(props.value)
}

function resolveSelectionDisplayLabels(field = {}) {
  const normalizedValue = normalizeOptionValue(props.value, currentOptions.value, field?.multiple)
  const values = Array.isArray(normalizedValue)
    ? normalizedValue
    : field.multiple && typeof normalizedValue === 'string'
      ? normalizedValue.split(',').map(item => item.trim()).filter(Boolean)
      : [normalizedValue]
  return values
    .map(item => flattenOptionNodes(currentOptions.value).find(option => isSameOptionValue(option?.value ?? option?.key, item))?.label)
    .filter(Boolean)
}

function normalizeDisplayText(value) {
  if (Array.isArray(value)) {
    const text = value.map(item => String(item ?? '').trim()).filter(Boolean).join(', ')
    return text || '-'
  }
  if (value === null || value === undefined)
    return '-'
  const text = String(value).trim()
  return text || '-'
}

function resolveUserLabel(user = {}) {
  return String(user?.realName || user?.name || user?.nickname || user?.username || '').trim()
}

function isFilledValue(value) {
  if (Array.isArray(value))
    return value.length > 0
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function handleTreeSelectUpdate(field, newValue) {
  const normalizedValue = normalizeOptionValue(newValue, currentOptions.value, field?.multiple)
  syncIncludeChildrenFlag(field, normalizedValue)
  if (isOrgTreeSelectField(field) || field?.type === 'treeSelect') {
    if (isFilledValue(normalizedValue))
      syncSelectionLabelFromOptions(field, normalizedValue)
    else
      patchSelectionLabelValue(field, '')
  }
  emit('update:value', normalizedValue)
}

function handleRegionTreeSelectUpdate(field, newValue) {
  if (!props.context?.isSearch || !field?.field) {
    emit('update:value', newValue)
    return
  }
  const includeChildrenKey = `${field.field}_includeChildren`
  if (newValue === null || newValue === undefined || newValue === '') {
    props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
    emit('update:value', newValue)
    return
  }
  const textValue = String(newValue)
  if (textValue.endsWith('ALL')) {
    props.context?.patchFormData?.({ [includeChildrenKey]: true })
    emit('update:value', textValue.replace(/ALL$/, ''))
    return
  }
  props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
  emit('update:value', newValue)
}

function resolveUserSelectLabel(field) {
  return resolveSelectionLabelValue(field) ?? ''
}

function handleUserSelectLabelUpdate(field, labelValue) {
  patchSelectionLabelValue(field, labelValue)
}

function handleUserSelect(field, users) {
  const selectedUsers = Array.isArray(users) ? users : users ? [users] : []
  const labels = selectedUsers.map(resolveUserLabel).filter(Boolean)
  if (labels.length)
    patchSelectionLabelValue(field, field?.multiple ? labels : labels[0])
  const events = getComponentEvents(field)
  if (typeof events.select === 'function')
    events.select(users)
}

function syncIncludeChildrenFlag(field, value) {
  if (!props.context?.isSearch || !field?.field || !(field.type === 'treeSelect' || isOrgTreeSelectField(field)))
    return
  const includeChildrenKey = `${field.field}_includeChildren`
  if (Array.isArray(value) || value === null || value === undefined || value === '') {
    props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
    return
  }
  if (isOrgTreeSelectField(field)) {
    props.context?.patchFormData?.({ [includeChildrenKey]: true })
    return
  }
  const selectedNode = flattenOptionNodes(currentOptions.value).find(option => isSameOptionValue(option?.value ?? option?.key, value))
  if (selectedNode?.children?.length) {
    props.context?.patchFormData?.({ [includeChildrenKey]: true })
    return
  }
  props.context?.patchFormData?.({ [includeChildrenKey]: undefined })
}

/**
 * 获取组件事件
 */
function getComponentEvents(field) {
  if (!field.on)
    return {}

  const events = {}
  Object.keys(field.on).forEach((eventName) => {
    events[eventName] = (...args) => {
      if (typeof field.on[eventName] === 'function') {
        field.on[eventName]({
          field,
          formData: props.formData,
          context: props.context,
          args,
        })
      }
    }
  })
  return events
}

/**
 * 处理值更新
 */
function handleUpdate(newValue) {
  emit('update:value', newValue)
}

function handleFieldFocusout(event) {
  if (!blurFieldEventEnabled.value)
    return
  if (event?.currentTarget?.contains?.(event.relatedTarget))
    return
  props.context?.dispatchFieldEvent?.('BLUR', props.field?.field)
}

function handleFieldKeyup(event) {
  if (!scanFieldEventEnabled.value || event?.key !== 'Enter')
    return
  event.preventDefault?.()
  event.stopPropagation?.()
  props.context?.dispatchFieldEvent?.('SCAN_COMPLETE', props.field?.field)
}

async function handleScanFieldEvent() {
  if (scanLoading.value || disabledHandler(props.field))
    return
  const scanner = props.context?.scanField
  if (typeof scanner !== 'function') {
    setScanFeedback('error', '当前环境不支持扫码')
    return
  }
  scanLoading.value = true
  setScanFeedback('loading', '')
  try {
    const result = await scanner(props.field)
    if (!result?.value)
      throw Object.assign(new Error('扫码结果无效'), { code: 'SCAN_INVALID_RESULT' })
    handleUpdate(result.value)
    await nextTick()
    await props.context?.dispatchFieldEvent?.('SCAN_COMPLETE', props.field?.field, { scan: result })
    setScanFeedback('success', '')
  }
  catch (error) {
    setScanFeedback('error', resolveScanErrorMessage(error))
  }
  finally {
    scanLoading.value = false
  }
}

function setScanFeedback(status, message) {
  scanFeedback.value = { status, message: message || '' }
}

function resolveScanErrorMessage(error) {
  switch (error?.code) {
    case 'SCAN_CANCELLED':
      return '已取消扫码'
    case 'SCAN_TIMEOUT':
      return '扫码超时，请重试'
    case 'SCAN_UNSUPPORTED':
      return '当前环境不支持扫码'
    case 'SCAN_PERMISSION_DENIED':
      return '请允许浏览器使用摄像头'
    case 'SCAN_INVALID_RESULT':
      return '扫码结果无效'
    default:
      return '扫码失败，请重试'
  }
}

function handleManualFieldEvent() {
  if (fieldEventState.value.loading)
    return
  props.context?.dispatchFieldEvent?.('MANUAL', props.field?.field)
}

function openRecordSelector() {
  if (!recordSelectorConfig.value.objectCode) {
    window.$message?.warning('未配置选择器业务对象')
    return
  }
  recordSelectorVisible.value = true
}

function clearRecordSelectorValue() {
  const config = recordSelectorConfig.value
  const labelField = props.field?.labelField || props.field?.props?.labelField || props.field?.props?.targetLabelField || config.targetLabelField || config.labelTargetField
  const patch = { [props.field.field]: undefined }
  if (labelField)
    patch[labelField] = undefined
  props.context?.patchFormData?.(patch)
  emit('update:value', null)
}

function handleRecordSelectorConfirm({ rows = [], mappings = {} } = {}) {
  const selected = rows[0]
  if (!selected)
    return
  const rawRecord = extractSelectorRawRecord(selected)
  const config = recordSelectorConfig.value
  const valueField = props.field?.valueField || props.field?.props?.valueField || config.valueField || 'id'
  const labelField = props.field?.labelField || props.field?.props?.labelField || props.field?.props?.targetLabelField || config.targetLabelField || config.labelTargetField
  const labelSourceField = props.field?.labelSourceField || props.field?.props?.labelSourceField || config.labelField || config.labelSourceField
  const patch = {
    ...applyRecordFieldMappings(selected, mappings || config.fieldMappings),
    [props.field.field]: rawRecord[valueField] ?? selected[valueField] ?? selected.id,
  }
  if (labelField && labelSourceField)
    patch[labelField] = rawRecord[labelSourceField] ?? selected[labelSourceField]
  props.context?.patchFormData?.(patch)
  emit('update:value', patch[props.field.field])
}

function handleRuntimePageWidgetUpdate(nextProps = {}) {
  const widgetKey = runtimePageWidgetKey.value
  if (widgetKey === 'rich-text' || widgetKey === 'markdown') {
    emit('update:value', nextProps.content || '')
    return
  }
  if (widgetKey === 'transfer') {
    emit('update:value', Array.isArray(nextProps.value) ? nextProps.value : [])
  }
}

function resolveFormattedRangeValue(value, index) {
  if (!Array.isArray(value))
    return null
  return normalizeFormattedPickerValue(value[index]) ?? null
}

function normalizeTimestampPickerValue(value) {
  if (value instanceof Date)
    return Number.isNaN(value.getTime()) ? undefined : value.getTime()
  if (typeof value === 'number')
    return Number.isFinite(value) ? value : undefined
  return undefined
}

function normalizeFormattedPickerValue(value) {
  if (value === null || value === undefined || value === '')
    return null
  if (typeof value === 'string') {
    const text = value.trim()
    if (!text)
      return null
    if (isSupportedPickerText(text))
      return text
    return null
  }
  return undefined
}

function isSupportedPickerText(text = '') {
  return /^\d{4}$/.test(text)
    || /^\d{4}-\d{2}$/.test(text)
    || /^\d{4}-\d{2}-\d{2}$/.test(text)
    || /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(?::\d{2})?$/.test(text)
    || /^\d{2}:\d{2}(?::\d{2})?$/.test(text)
}

function normalizeTimestampRangePickerValue(value) {
  if (!Array.isArray(value))
    return undefined
  const normalized = value.map(item => normalizeTimestampPickerValue(item))
  return normalized.length === 2 && normalized.every(item => item !== undefined)
    ? normalized
    : undefined
}

function normalizeFormattedRangePickerValue(value) {
  if (value === null || value === undefined || value === '')
    return null
  if (!Array.isArray(value))
    return undefined
  const normalized = value.map(item => normalizeFormattedPickerValue(item))
  if (normalized.every(item => item === null))
    return null
  if (normalized.length === 2 && normalized.every(item => typeof item === 'string'))
    return normalized
  return value.some(item => typeof item === 'string') ? null : undefined
}

function resolvePickerDefaultValue(field, range = false) {
  const configured = field?.pickerDefaultValue ?? field?.props?.pickerDefaultValue ?? field?.props?.defaultPickerValue
  const value = configured ?? pickerDefaultTimestamp
  return range ? [value, value] : value
}

function handleRangeUpdate(index, nextValue) {
  const next = Array.isArray(props.value) ? [...props.value] : [null, null]
  next[index] = nextValue
  emit('update:value', next)
}

/**
 * 处理文件上传变化
 */
function handleUploadChange({ fileList }) {
  emit('update:value', fileList)
}

/**
 * 复制文本
 */
function handleCopy(text) {
  copy(text)
  window.$message?.success('复制成功')
}

/**
 * 文件上传成功回调
 */
function handleUploadSuccess(field, data) {
  if (field.onSuccess && typeof field.onSuccess === 'function') {
    field.onSuccess({
      data,
      field,
      formData: props.formData,
      context: props.context,
    })
  }
}

/**
 * 文件上传失败回调
 */
function handleUploadError(field, error) {
  if (field.onError && typeof field.onError === 'function') {
    field.onError({
      error,
      field,
      formData: props.formData,
      context: props.context,
    })
  }
}

/**
 * 文件删除回调
 */
function handleUploadRemove(field, file) {
  if (field.onRemove && typeof field.onRemove === 'function') {
    field.onRemove({
      file,
      field,
      formData: props.formData,
      context: props.context,
    })
  }
}
</script>

<style scoped>
.ai-form-control {
  width: 100%;
  min-width: 0;
}

.ai-form-item-body {
  display: grid;
  width: 100%;
  min-width: 0;
  gap: 6px;
}

.ai-form-item-label {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
  vertical-align: middle;
}

.ai-form-item-label__text {
  overflow: hidden;
  min-width: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-form-item-label__badge {
  display: inline-flex;
  align-items: center;
  height: 18px;
  max-width: 72px;
  flex: 0 0 auto;
  padding: 0 6px;
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 600;
  line-height: 16px;
}

.ai-form-item-description {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  word-break: break-word;
}

.ai-form-field-event {
  display: flex;
  min-height: 18px;
  align-items: center;
  gap: 8px;
  line-height: 18px;
}

.ai-form-field-event__action {
  flex: 0 0 auto;
  font-size: 12px;
}

.ai-form-field-event__message {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-form-field-event__message.is-error {
  color: #dc2626;
}

.ai-form-field-event__message.is-not_found {
  color: #b45309;
}

.ai-form-field-event__message.is-loading {
  color: #2563eb;
}

.ai-form-item-label__tip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
  border-radius: 50%;
  border: 1px solid #cbd5e1;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  cursor: help;
  transition: all 0.2s;
}

.ai-form-item-label__tip:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.ai-form-item-label__tip-content {
  white-space: pre-line;
  font-size: 13px;
  line-height: 1.7;
}

.ai-form-readonly-text {
  min-height: 34px;
  display: flex;
  align-items: center;
  color: var(--n-text-color);
  line-height: 1.6;
  word-break: break-all;
}

.ai-form-control :deep(.n-base-selection),
.ai-form-control :deep(.n-base-selection-label) {
  align-items: center;
}

.ai-form-control :deep(.n-base-selection__clear),
.ai-form-control :deep(.n-base-clear) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.time-range-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.time-range-separator {
  color: #64748b;
  font-size: 12px;
}

.ai-form-item-align-center :deep(.n-input__input-el),
.ai-form-item-align-center :deep(.n-input__textarea-el),
.ai-form-item-align-center :deep(.n-input-number-input),
.ai-form-item-align-center :deep(.n-base-selection-label__render-label) {
  text-align: center;
}

.ai-form-item-align-right :deep(.n-input__input-el),
.ai-form-item-align-right :deep(.n-input__textarea-el),
.ai-form-item-align-right :deep(.n-input-number-input),
.ai-form-item-align-right :deep(.n-base-selection-label__render-label) {
  text-align: right;
}

.ai-form-item-align-center :deep(.n-base-selection-label),
.ai-form-item-align-right :deep(.n-base-selection-label) {
  justify-content: center;
}

.ai-form-item-align-right :deep(.n-base-selection-label) {
  justify-content: flex-end;
}

.ai-form-field-flash {
  border-radius: 8px;
  animation: ai-form-field-flash 2.8s ease-in-out;
}

@keyframes ai-form-field-flash {
  0%,
  35%,
  70%,
  100% {
    background: transparent;
    box-shadow: none;
  }

  12%,
  47%,
  82% {
    background: rgba(245, 108, 108, 0.1);
    box-shadow: 0 0 0 4px rgba(245, 108, 108, 0.2);
  }
}
</style>
