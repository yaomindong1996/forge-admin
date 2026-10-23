<template>
  <!-- 表单分隔线 -->
  <AiFormSectionTitle
    v-if="fieldRuntimeVisible && isSectionTitleField"
    :title="field.label"
    :anchor-id="field.__sectionId"
    :description="field.props?.description || field.description"
    :badge="field.props?.badge || field.badge"
    :style="resolvedFormItemStyle || field.style || field.formItemStyle"
    :class="field.className"
  />

  <!-- 分组标题 -->
  <AiFormGroupTitle
    v-else-if="fieldRuntimeVisible && isGroupTitleField"
    :label="field.label"
    :title="field.props?.title || field.title"
    :style="resolvedFormItemStyle || field.style || field.formItemStyle"
    :class-name="field.className"
  />

  <!-- 普通表单项 -->
  <n-form-item
    v-else-if="fieldRuntimeVisible"
    v-bind="$attrs"
    ref="formItemRef"
    :label="field.label"
    :path="field.field"
    :label-width="field.labelWidth"
    :show-label="field.showLabel !== false"
    :show-feedback="field.showFeedback !== false"
    :required="isFieldRequired"
    :data-ai-field="field.field"
    :style="resolvedFormItemStyle || field.formItemStyle"
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
        <!-- 低代码页面展示组件 -->
        <PageWidgetRenderer
          v-if="isRuntimePageWidgetField"
          :component-key="runtimePageWidgetKey"
          :props-data="runtimePageWidgetProps"
          :data-context="formData || {}"
          :readonly="disabledHandler(field) || field.visibility?.readonly === true || field.readonly === true"
          @update:props-data="handleRuntimePageWidgetUpdate"
        />

        <AiFormArrayField
          v-else-if="field.type === 'array'"
          ref="arrayFieldRef"
          :model-value="Array.isArray(value) ? value : []"
          :field="field"
          :form-data="formData"
          :context="context"
          :disabled="disabledHandler(field)"
          @update:model-value="handleUpdate"
        />

        <!-- 数字/金额：必须先于 input，避免 type=input + componentKey=money 落到文本框后金额空白 -->
        <n-input-number
          v-else-if="isNumberLikeField(field)"
          :value="numberFieldValue"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :min="field.min ?? field.props?.min"
          :max="field.max ?? field.props?.max"
          :step="field.step || field.props?.step || (resolveNumberFieldType(field) === 'money' ? 0.01 : 1)"
          :precision="field.precision ?? field.props?.precision ?? (resolveNumberFieldType(field) === 'money' ? 2 : undefined)"
          :show-button="field.showButton !== false && field.props?.showButton !== false && field.props?.controls !== false"
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="controlProps"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        >
          <template v-if="numberFieldPrefixIcon || numberFieldPrefix" #prefix>
            <IconRenderer v-if="numberFieldPrefixIcon" :icon="numberFieldPrefixIcon" :size="16" />
            <span v-if="numberFieldPrefix">{{ numberFieldPrefix }}</span>
          </template>
          <template v-if="numberFieldSuffixIcon || numberFieldSuffix" #suffix>
            <span v-if="numberFieldSuffix">{{ numberFieldSuffix }}</span>
            <IconRenderer v-if="numberFieldSuffixIcon" :icon="numberFieldSuffixIcon" :size="16" />
          </template>
        </n-input-number>

        <!-- 输入框：Naive Input Slots（prefix/suffix/clear-icon/password-*/separator）；字数用 show-count -->
        <n-input
          v-else-if="field.type === 'input'"
          v-bind="controlProps"
          :type="inputFieldType"
          :value="value"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :maxlength="field.maxlength"
          :show-count="field.showCount"
          :size="field.size"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        >
          <template v-if="inputFieldPrefixIcon || inputFieldPrefix" #prefix>
            <IconRenderer v-if="inputFieldPrefixIcon" :icon="inputFieldPrefixIcon" :size="16" />
            <span v-if="inputFieldPrefix">{{ inputFieldPrefix }}</span>
          </template>
          <template v-if="inputFieldSuffixIcon || inputFieldSuffix" #suffix>
            <span v-if="inputFieldSuffix">{{ inputFieldSuffix }}</span>
            <IconRenderer v-if="inputFieldSuffixIcon" :icon="inputFieldSuffixIcon" :size="16" />
          </template>
          <template v-if="inputFieldClearIcon" #clear-icon>
            <IconRenderer :icon="inputFieldClearIcon" :size="16" />
          </template>
          <template v-if="inputFieldType === 'password' && inputFieldPasswordVisibleIcon" #password-visible-icon>
            <IconRenderer :icon="inputFieldPasswordVisibleIcon" :size="16" />
          </template>
          <template v-if="inputFieldType === 'password' && inputFieldPasswordInvisibleIcon" #password-invisible-icon>
            <IconRenderer :icon="inputFieldPasswordInvisibleIcon" :size="16" />
          </template>
          <template v-if="inputFieldSeparator" #separator>
            {{ inputFieldSeparator }}
          </template>
        </n-input>

        <!-- H5/企业微信条码扫描输入框 -->
        <div v-else-if="field.type === 'barcodeScanner'" class="ai-form-barcode-scanner-field">
          <n-input
            :value="value"
            :placeholder="getPlaceholder(field)"
            :disabled="disabledHandler(field)"
            :clearable="field.props?.allowManualInput !== false"
            :maxlength="field.props?.maxlength || 2048"
            v-bind="controlProps"
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
          v-bind="controlProps"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 下拉选择：弹层（modal/drawer）内虚拟列表首屏常算高却不渲染选项，关闭虚拟滚动保证可见 -->
        <n-select
          v-else-if="field.type === 'select'"
          v-bind="controlProps"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :options="currentOptions"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :loading="selectOptionLoading"
          :remote="field.remote"
          :on-search="field.onSearch"
          :disabled="disabledHandler(field)"
          :multiple="fieldMultiple"
          :virtual-scroll="false"
          @update:value="handleSelectUpdate"
          v-on="getComponentEvents(field)"
        >
          <template v-if="selectDropdownHeader" #header>
            <div class="ai-form-select-slot-text">
              {{ selectDropdownHeader }}
            </div>
          </template>
          <template v-if="selectDropdownAction" #action>
            <div class="ai-form-select-slot-text">
              {{ selectDropdownAction }}
            </div>
          </template>
          <template v-if="selectEmptyText" #empty>
            <div class="ai-form-select-slot-text">
              {{ selectEmptyText }}
            </div>
          </template>
          <template v-if="selectArrowIcon" #arrow>
            <IconRenderer :icon="selectArrowIcon" :size="16" />
          </template>
        </n-select>

        <!-- 字典选择器 -->
        <DictSelect
          v-else-if="field.type === 'dictSelect'"
          :value="resolveOptionValue(value)"
          :dict-type="field.dictType || field.props?.dictType"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :form-data="formData"
          :cascade="dictCascadeConfig"
          v-bind="controlProps"
          :multiple="fieldMultiple"
          @update:value="handleUpdate"
        />

        <!-- 单选框 -->
        <n-radio-group
          v-else-if="field.type === 'radio'"
          :value="resolveOptionValue(value)"
          :disabled="disabledHandler(field)"
          v-bind="controlProps"
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
          v-bind="controlProps"
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
          v-bind="controlProps"
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

        <!-- 开关：checked-value 必须在 v-bind 之后，避免 props 里的 boolean 覆盖 0/1 -->
        <n-switch
          v-else-if="field.type === 'switch'"
          class="ai-form-switch"
          :value="value"
          :disabled="disabledHandler(field)"
          v-bind="switchControlProps"
          :checked-value="resolveSwitchCheckedValue(field)"
          :unchecked-value="resolveSwitchUncheckedValue(field)"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        >
          <template v-if="field.checkedText || field.props?.checkedText" #checked>
            {{ field.checkedText || field.props?.checkedText }}
          </template>
          <template v-if="field.uncheckedText || field.props?.uncheckedText" #unchecked>
            {{ field.uncheckedText || field.props?.uncheckedText }}
          </template>
        </n-switch>

        <!-- 日期选择（AiFormDatePicker 统一接线 Naive DatePicker Slots） -->
        <AiFormDatePicker
          v-else-if="field.type === 'date'"
          v-bind="datePickerBindings"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="date"
          :placeholder="getPlaceholder(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          :disabled="disabledHandler(field)"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 日期时间选择 -->
        <AiFormDatePicker
          v-else-if="field.type === 'datetime'"
          v-bind="datePickerBindings"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="datetime"
          :placeholder="getPlaceholder(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          :disabled="disabledHandler(field)"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd HH:mm:ss'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd HH:mm:ss'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 日期范围选择：起止占位优先 props，再回退公共 placeholder -->
        <AiFormDatePicker
          v-else-if="field.type === 'daterange'"
          v-bind="datePickerBindings"
          :value="normalizeTimestampRangePickerValue(value)"
          :formatted-value="normalizeFormattedRangePickerValue(value)"
          type="daterange"
          :placeholder="getPlaceholder(field)"
          :start-placeholder="field.startPlaceholder || field.props?.startPlaceholder || getPlaceholder(field) || '开始日期'"
          :end-placeholder="field.endPlaceholder || field.props?.endPlaceholder || '结束日期'"
          :clearable="field.clearable !== false"
          style="width: 100%"
          :disabled="disabledHandler(field)"
          :default-value="resolvePickerDefaultValue(field, true)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 日期时间范围选择：起止占位优先 props，再回退公共 placeholder -->
        <AiFormDatePicker
          v-else-if="field.type === 'datetimerange'"
          v-bind="datePickerBindings"
          :value="normalizeTimestampRangePickerValue(value)"
          :formatted-value="normalizeFormattedRangePickerValue(value)"
          type="datetimerange"
          :placeholder="getPlaceholder(field)"
          :start-placeholder="field.startPlaceholder || field.props?.startPlaceholder || getPlaceholder(field) || '开始时间'"
          :end-placeholder="field.endPlaceholder || field.props?.endPlaceholder || '结束时间'"
          :clearable="field.clearable !== false"
          style="width: 100%"
          :disabled="disabledHandler(field)"
          :default-value="resolvePickerDefaultValue(field, true)"
          :format="field.props?.format || field.format || 'yyyy-MM-dd HH:mm:ss'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM-dd HH:mm:ss'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 月份选择 -->
        <AiFormDatePicker
          v-else-if="field.type === 'month'"
          v-bind="datePickerBindings"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="month"
          :placeholder="getPlaceholder(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          :disabled="disabledHandler(field)"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'yyyy-MM'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'yyyy-MM'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 年份选择 -->
        <AiFormDatePicker
          v-else-if="field.type === 'year'"
          v-bind="datePickerBindings"
          :value="normalizeTimestampPickerValue(value)"
          :formatted-value="normalizeFormattedPickerValue(value)"
          type="year"
          :placeholder="getPlaceholder(field)"
          :clearable="field.clearable !== false"
          style="width: 100%"
          :disabled="disabledHandler(field)"
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
          :clearable="field.clearable !== false"
          style="width: 100%"
          v-bind="controlProps"
          :disabled="disabledHandler(field)"
          :default-value="resolvePickerDefaultValue(field)"
          :format="field.props?.format || field.format || 'HH:mm:ss'"
          :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
          @update:formatted-value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 时间范围选择：起止占位优先 props，再回退公共 placeholder -->
        <div v-else-if="field.type === 'timerange'" class="time-range-picker">
          <n-time-picker
            :formatted-value="resolveFormattedRangeValue(value, 0)"
            :placeholder="field.startPlaceholder || field.props?.startPlaceholder || getPlaceholder(field) || '开始时间'"
            :clearable="field.clearable !== false"
            style="width: 100%"
            v-bind="controlProps"
            :disabled="disabledHandler(field)"
            :default-value="resolvePickerDefaultValue(field)"
            :format="field.props?.format || field.format || 'HH:mm:ss'"
            :value-format="field.props?.valueFormat || field.valueFormat || 'HH:mm:ss'"
            @update:formatted-value="handleRangeUpdate(0, $event)"
            v-on="getComponentEvents(field)"
          />
          <span class="time-range-separator">至</span>
          <n-time-picker
            :formatted-value="resolveFormattedRangeValue(value, 1)"
            :placeholder="field.endPlaceholder || field.props?.endPlaceholder || '结束时间'"
            :clearable="field.clearable !== false"
            style="width: 100%"
            v-bind="controlProps"
            :disabled="disabledHandler(field)"
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
          v-bind="controlProps"
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
          v-bind="controlProps"
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
          v-bind="controlProps"
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
          v-bind="controlProps"
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
          v-bind="controlProps"
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
          v-bind="controlProps"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 级联选择 -->
        <n-cascader
          v-else-if="field.type === 'cascader'"
          v-bind="controlProps"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :options="currentOptions"
          :clearable="field.clearable !== false"
          :filterable="field.filterable"
          :multiple="field.multiple"
          :cascade="field.cascade !== false"
          :show-path="field.showPath !== false"
          :disabled="disabledHandler(field)"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 系统组织树选择 -->
        <n-tree-select
          v-else-if="isOrgTreeSelectField(field)"
          v-bind="controlProps"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field)"
          :options="currentOptions"
          :loading="remoteLoading"
          :clearable="isFieldClearable(field)"
          :filterable="field.filterable !== false && field.props?.filterable !== false"
          :cascade="resolveNaiveTreeCascade(field)"
          :multiple="fieldMultiple"
          @update:value="handleTreeSelectUpdate(field, $event)"
          v-on="getComponentEvents(field)"
        />

        <!-- 系统用户选择 -->
        <UserSelectPicker
          v-else-if="isUserSelectField(field)"
          v-bind="controlProps"
          :model-value="value"
          :label-value="resolveUserSelectLabel(field)"
          :placeholder="getPlaceholder(field)"
          :disabled="disabledHandler(field) || isCascadeDisabledByEmptyParent()"
          :clearable="isFieldClearable(field)"
          :size="field.size || field.props?.size"
          :org-id="userSelectCascadeOrgId"
          :include-children="userSelectCascadeIncludeChildren"
          :multiple="fieldMultiple"
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
          v-bind="controlProps"
          @update:model-value="handleRegionTreeSelectUpdate(field, $event)"
        />

        <!-- 树形选择 -->
        <n-tree-select
          v-else-if="field.type === 'treeSelect'"
          v-bind="controlProps"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :options="currentOptions"
          :loading="remoteLoading"
          :clearable="field.clearable !== false"
          :filterable="field.filterable"
          :multiple="field.multiple"
          :cascade="field.cascade !== false"
          :show-path="field.showPath !== false"
          :on-load="treeSelectLazyLoadHandler"
          :disabled="disabledHandler(field)"
          @update:value="handleTreeSelectUpdate(field, $event)"
          v-on="getComponentEvents(field)"
        />

        <!-- 穿梭框：Naive Transfer 用 source/target-filter-placeholder，不吃通用 placeholder -->
        <n-transfer
          v-else-if="field.type === 'transfer'"
          :value="resolveOptionValue(value)"
          :filterable="field.filterable !== false && field.props?.filterable !== false"
          v-bind="controlProps"
          :options="currentOptions"
          :source-filter-placeholder="field.sourceFilterPlaceholder || field.props?.sourceFilterPlaceholder || getPlaceholder(field) || '请输入'"
          :target-filter-placeholder="field.targetFilterPlaceholder || field.props?.targetFilterPlaceholder || getPlaceholder(field) || '请输入'"
          :disabled="disabledHandler(field)"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 远程搜索下拉框：静态选项走 currentOptions；:options 必须在 v-bind 之后，避免被盖成空 -->
        <AiCustomSelect
          v-else-if="field.type === 'customSelect'"
          :value="value"
          :placeholder="getPlaceholder(field)"
          :clearable="field.clearable !== false"
          :api="field.api || field.props?.api"
          :method="field.method || field.props?.method"
          :label-field="field.labelField || field.props?.labelName || field.props?.labelField || 'label'"
          :value-field="field.valueField || field.props?.valueName || field.props?.valueField || 'value'"
          :filterable="field.filterable !== false"
          :multiple="fieldMultiple"
          :remote="field.remote ?? field.props?.remote"
          :params="field.params || field.props?.params"
          :transform="field.transform || field.props?.transform"
          v-bind="controlProps"
          :options="currentOptions"
          :disabled="disabledHandler(field)"
          @update:value="handleUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 关联选择（下拉模式）：对象引用/记录选择器统一渲染 -->
        <n-select
          v-else-if="isRelationSelectorField && relationSelectorMode === 'dropdown'"
          v-bind="controlProps"
          :value="resolveOptionValue(value)"
          :placeholder="getPlaceholder(field)"
          :options="currentOptions"
          :loading="remoteLoading"
          :clearable="field.clearable !== false"
          :filterable="field.filterable !== false"
          :remote="objectReferenceRemoteEnabled"
          :disabled="disabledHandler(field)"
          :multiple="fieldMultiple"
          @search="handleObjectReferenceSearch"
          @update:value="handleObjectReferenceUpdate"
          v-on="getComponentEvents(field)"
        />

        <!-- 关联选择（弹窗模式）：支持展示列/筛选条件/字段映射 -->
        <n-input-group v-else-if="isRelationSelectorField && relationSelectorMode === 'popup'">
          <n-input
            :value="recordSelectorDisplayText"
            :placeholder="getPlaceholder(field)"
            :disabled="disabledHandler(field)"
            readonly
            clearable
            v-bind="controlProps"
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
          v-bind="controlProps"
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
    v-if="isRelationSelectorField && relationSelectorMode === 'popup'"
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
    :multiple="recordSelectorConfig.multiple === true"
    :display-fields="recordSelectorConfig.displayFields"
    :keyword-fields="recordSelectorConfig.keywordFields"
    :field-mappings="recordSelectorConfig.fieldMappings"
    :search-params="recordSelectorConfig.searchParams"
    :filter-fields="recordSelectorConfig.filterFields"
    :runtime-context="recordSelectorRuntimeContext"
    @confirm="handleRecordSelectorConfirm"
  />
</template>

<script>
import { aiFormItemLocalComponents } from './aiFormItemLocalComponents'
import { useAiFormItem } from './composables/useAiFormItem'

export default {
  inheritAttrs: false,
  name: 'AiFormItem',
  components: {
    ...aiFormItemLocalComponents,
  },
  props: {
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
},
  emits: ['update:value'],
  setup(props, { emit }) {
    return useAiFormItem(props, emit)
  },
}
</script>

<style scoped src="./aiFormItem.css"></style>
