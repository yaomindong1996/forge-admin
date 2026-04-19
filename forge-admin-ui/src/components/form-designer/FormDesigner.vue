<template>
  <div class="form-designer-container">
    <!-- 左侧组件面板 -->
    <div class="components-panel">
      <div class="panel-header">
        <span>表单组件</span>
      </div>
      <div class="panel-content">
        <!-- 基础组件 -->
        <div class="component-group">
          <div class="group-title">
            基础字段
          </div>
          <div class="component-list">
            <div
              v-for="comp in basicComponents"
              :key="comp.type"
              class="component-item"
              draggable="true"
              @dragstart="handleDragStart($event, comp)"
            >
              <i :class="comp.icon" />
              <span>{{ comp.name }}</span>
            </div>
          </div>
        </div>

        <!-- 高级组件 -->
        <div class="component-group">
          <div class="group-title">
            高级字段
          </div>
          <div class="component-list">
            <div
              v-for="comp in advancedComponents"
              :key="comp.type"
              class="component-item"
              draggable="true"
              @dragstart="handleDragStart($event, comp)"
            >
              <i :class="comp.icon" />
              <span>{{ comp.name }}</span>
            </div>
          </div>
        </div>

        <!-- 布局组件 -->
        <div class="component-group">
          <div class="group-title">
            布局组件
          </div>
          <div class="component-list">
            <div
              v-for="comp in layoutComponents"
              :key="comp.type"
              class="component-item"
              draggable="true"
              @dragstart="handleDragStart($event, comp)"
            >
              <i :class="comp.icon" />
              <span>{{ comp.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间设计区域 -->
    <div class="design-area">
      <div class="design-toolbar">
        <n-space>
          <n-button size="small" @click="handleClear">
            <template #icon>
              <i class="i-material-symbols:delete" />
            </template>
            清空
          </n-button>
          <n-button size="small" @click="handlePreview">
            <template #icon>
              <i class="i-material-symbols:preview" />
            </template>
            预览
          </n-button>
          <n-button size="small" @click="handleExport">
            <template #icon>
              <i class="i-material-symbols:download" />
            </template>
            导出JSON
          </n-button>
          <n-button size="small" type="primary" @click="handleSave">
            <template #icon>
              <i class="i-material-symbols:save" />
            </template>
            保存
          </n-button>
        </n-space>
      </div>

      <div
        class="form-canvas"
        :class="{ 'is-empty': formItems.length === 0 }"
        @dragover.prevent
        @drop="handleDrop"
      >
        <div v-if="formItems.length === 0" class="empty-placeholder">
          <i class="i-material-symbols:add-box-outline" />
          <p>从左侧拖拽组件到此处</p>
        </div>

        <draggable
          v-else
          v-model="formItems"
          item-key="field"
          handle=".drag-handle"
          animation="200"
          class="form-items-list"
        >
          <template #item="{ element, index }">
            <div
              class="form-item-wrapper"
              :class="{ 'is-selected': selectedIndex === index }"
              @click="selectItem(index)"
            >
              <div class="form-item-actions">
                <n-button text size="tiny" class="drag-handle">
                  <i class="i-material-symbols:drag-indicator" />
                </n-button>
                <n-button text size="tiny" @click.stop="copyItem(index)">
                  <i class="i-material-symbols:content-copy" />
                </n-button>
                <n-button text size="tiny" type="error" @click.stop="removeItem(index)">
                  <i class="i-material-symbols:delete" />
                </n-button>
              </div>
              <div class="form-item-content">
                <FormItemRender :item="element" :preview="false" />
              </div>
            </div>
          </template>
        </draggable>
      </div>
    </div>

    <!-- 右侧属性面板 -->
    <div class="properties-panel">
      <div class="panel-header">
        <span>属性配置</span>
      </div>
      <div class="panel-content">
        <template v-if="currentItem">
          <n-form :model="currentItem" label-placement="top" size="small">
            <!-- 基础属性 -->
            <n-divider>基础属性</n-divider>
            <n-form-item label="字段名">
              <n-input :value="currentItem.field" placeholder="请输入字段名" @update:value="updateField('field', $event)" />
            </n-form-item>
            <n-form-item label="标签">
              <n-input :value="currentItem.label" placeholder="请输入标签" @update:value="updateField('label', $event)" />
            </n-form-item>
            <n-form-item label="占位提示">
              <n-input :value="currentItem.props.placeholder" placeholder="请输入占位提示" @update:value="updateProps('placeholder', $event)" />
            </n-form-item>
            <n-form-item label="默认值">
              <n-input :value="currentItem.defaultValue" placeholder="请输入默认值" @update:value="updateField('defaultValue', $event)" />
            </n-form-item>
            <n-form-item label="是否必填">
              <n-switch :value="currentItem.required" @update:value="updateField('required', $event)" />
            </n-form-item>
            <n-form-item label="是否禁用">
              <n-switch :value="currentItem.disabled" @update:value="updateField('disabled', $event)" />
            </n-form-item>

            <!-- 组件特有属性 -->
            <template v-if="currentItem.type === 'input'">
              <n-divider>输入框属性</n-divider>
              <n-form-item label="最大长度">
                <n-input-number :value="currentItem.props.maxLength" :min="0" @update:value="updateProps('maxLength', $event)" />
              </n-form-item>
              <n-form-item label="显示字数统计">
                <n-switch :value="currentItem.props.showCount" @update:value="updateProps('showCount', $event)" />
              </n-form-item>
              <n-form-item label="允许清空">
                <n-switch :value="currentItem.props.clearable" @update:value="updateProps('clearable', $event)" />
              </n-form-item>
            </template>

            <template v-if="currentItem.type === 'textarea'">
              <n-divider>文本域属性</n-divider>
              <n-form-item label="行数">
                <n-input-number :value="currentItem.props.rows" :min="2" :max="10" @update:value="updateProps('rows', $event)" />
              </n-form-item>
              <n-form-item label="最大长度">
                <n-input-number :value="currentItem.props.maxLength" :min="0" @update:value="updateProps('maxLength', $event)" />
              </n-form-item>
            </template>

            <template v-if="currentItem.type === 'inputNumber'">
              <n-divider>数字输入属性</n-divider>
              <n-form-item label="最小值">
                <n-input-number :value="currentItem.props.min" @update:value="updateProps('min', $event)" />
              </n-form-item>
              <n-form-item label="最大值">
                <n-input-number :value="currentItem.props.max" @update:value="updateProps('max', $event)" />
              </n-form-item>
              <n-form-item label="精度">
                <n-input-number :value="currentItem.props.precision" :min="0" :max="10" @update:value="updateProps('precision', $event)" />
              </n-form-item>
              <n-form-item label="步长">
                <n-input-number :value="currentItem.props.step" :min="0.1" @update:value="updateProps('step', $event)" />
              </n-form-item>
            </template>

            <template v-if="currentItem.type === 'select'">
              <n-divider>下拉选择属性</n-divider>
              <n-form-item label="选项配置">
                <div class="options-editor">
                  <div v-for="(opt, idx) in currentItem.props.options" :key="idx" class="option-item">
                    <n-input :value="opt.label" placeholder="标签" size="small" @update:value="updateOption(idx, 'label', $event)" />
                    <n-input :value="opt.value" placeholder="值" size="small" @update:value="updateOption(idx, 'value', $event)" />
                    <n-button text size="tiny" type="error" @click="removeOption(idx)">
                      <i class="i-material-symbols:close" />
                    </n-button>
                  </div>
                  <n-button dashed block size="small" @click="addOption">
                    <template #icon>
                      <i class="i-material-symbols:add" />
                    </template>
                    添加选项
                  </n-button>
                </div>
              </n-form-item>
              <n-form-item label="多选">
                <n-switch :value="currentItem.props.multiple" @update:value="updateProps('multiple', $event)" />
              </n-form-item>
              <n-form-item label="可搜索">
                <n-switch :value="currentItem.props.filterable" @update:value="updateProps('filterable', $event)" />
              </n-form-item>
            </template>

            <template v-if="currentItem.type === 'radio' || currentItem.type === 'checkbox'">
              <n-divider>选项配置</n-divider>
              <n-form-item label="选项">
                <div class="options-editor">
                  <div v-for="(opt, idx) in currentItem.props.options" :key="idx" class="option-item">
                    <n-input :value="opt.label" placeholder="标签" size="small" @update:value="updateOption(idx, 'label', $event)" />
                    <n-input :value="opt.value" placeholder="值" size="small" @update:value="updateOption(idx, 'value', $event)" />
                    <n-button text size="tiny" type="error" @click="removeOption(idx)">
                      <i class="i-material-symbols:close" />
                    </n-button>
                  </div>
                  <n-button dashed block size="small" @click="addOption">
                    <template #icon>
                      <i class="i-material-symbols:add" />
                    </template>
                    添加选项
                  </n-button>
                </div>
              </n-form-item>
            </template>

            <template v-if="currentItem.type === 'datePicker'">
              <n-divider>日期选择属性</n-divider>
              <n-form-item label="日期类型">
                <n-select
                  :value="currentItem.props.type"
                  :options="dateTypeOptions"
                  @update:value="updateProps('type', $event)"
                />
              </n-form-item>
              <n-form-item label="日期格式">
                <n-input :value="currentItem.props.format" placeholder="yyyy-MM-dd" @update:value="updateProps('format', $event)" />
              </n-form-item>
            </template>

            <template v-if="currentItem.type === 'upload'">
              <n-divider>上传属性</n-divider>
              <n-form-item label="最大数量">
                <n-input-number :value="currentItem.props.maxCount" :min="1" @update:value="updateProps('maxCount', $event)" />
              </n-form-item>
              <n-form-item label="最大大小(MB)">
                <n-input-number :value="currentItem.props.maxSize" :min="1" @update:value="updateProps('maxSize', $event)" />
              </n-form-item>
              <n-form-item label="接受类型">
                <n-input :value="currentItem.props.accept" placeholder="image/*" @update:value="updateProps('accept', $event)" />
              </n-form-item>
              <n-form-item label="多选">
                <n-switch :value="currentItem.props.multiple" @update:value="updateProps('multiple', $event)" />
              </n-form-item>
            </template>

            <!-- 校验规则 -->
            <n-divider>校验规则</n-divider>
            <n-form-item label="校验提示">
              <n-input
                :value="currentItem.rules[0]?.message"
                placeholder="请输入校验提示信息"
                @update:value="updateRuleMessage($event)"
              />
            </n-form-item>
          </n-form>
        </template>
        <n-empty v-else description="请选择一个组件" />
      </div>
    </div>

    <!-- 预览弹窗 -->
    <n-modal v-model:show="showPreview" preset="card" title="表单预览" style="width: 600px">
      <FormPreview :form-items="formItems" />
    </n-modal>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import draggable from 'vuedraggable'
import FormItemRender from './FormItemRender.vue'
import FormPreview from './FormPreview.vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  initialSchema: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue', 'save', 'cancel'])

// 基础组件列表
const basicComponents = [
  { type: 'input', name: '输入框', icon: 'i-material-symbols:text-fields', defaultProps: { placeholder: '请输入', clearable: true, maxLength: null, showCount: false } },
  { type: 'textarea', name: '文本域', icon: 'i-material-symbols:subject', defaultProps: { placeholder: '请输入', rows: 4, maxLength: null } },
  { type: 'inputNumber', name: '数字输入', icon: 'i-material-symbols:pin', defaultProps: { placeholder: '请输入数字', min: null, max: null, precision: 0, step: 1 } },
  { type: 'select', name: '下拉选择', icon: 'i-material-symbols:arrow-drop-down-circle', defaultProps: { placeholder: '请选择', options: [], multiple: false, filterable: true } },
  { type: 'radio', name: '单选框', icon: 'i-material-symbols:radio-button-checked', defaultProps: { options: [] } },
  { type: 'checkbox', name: '复选框', icon: 'i-material-symbols:check-box', defaultProps: { options: [] } },
  { type: 'datePicker', name: '日期选择', icon: 'i-material-symbols:calendar-month', defaultProps: { type: 'date', format: 'yyyy-MM-dd', placeholder: '请选择日期' } },
  { type: 'timePicker', name: '时间选择', icon: 'i-material-symbols:schedule', defaultProps: { format: 'HH:mm:ss', placeholder: '请选择时间' } },
  { type: 'switch', name: '开关', icon: 'i-material-symbols:toggle-on', defaultProps: { checkedText: '', uncheckedText: '' } },
  { type: 'slider', name: '滑块', icon: 'i-material-symbols:tune', defaultProps: { min: 0, max: 100, step: 1 } },
]

// 高级组件列表
const advancedComponents = [
  { type: 'upload', name: '文件上传', icon: 'i-material-symbols:upload', defaultProps: { maxCount: 1, maxSize: 10, accept: '', multiple: false } },
  { type: 'richText', name: '富文本', icon: 'i-material-symbols:article', defaultProps: { placeholder: '请输入内容' } },
  { type: 'cascader', name: '级联选择', icon: 'i-material-symbols:account-tree', defaultProps: { options: [], placeholder: '请选择' } },
  { type: 'treeSelect', name: '树选择', icon: 'i-material-symbols:forest', defaultProps: { options: [], placeholder: '请选择' } },
  { type: 'colorPicker', name: '颜色选择', icon: 'i-material-symbols:palette', defaultProps: {} },
  { type: 'rate', name: '评分', icon: 'i-material-symbols:star', defaultProps: { count: 5, allowHalf: true } },
]

// 布局组件列表
const layoutComponents = [
  { type: 'divider', name: '分割线', icon: 'i-material-symbols:horizontal-rule', defaultProps: { title: '' } },
  { type: 'title', name: '标题', icon: 'i-material-symbols:title', defaultProps: { text: '标题', level: 3 } },
  { type: 'description', name: '描述文本', icon: 'i-material-symbols:description', defaultProps: { text: '描述内容' } },
]

// 日期类型选项
const dateTypeOptions = [
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '日期范围', value: 'daterange' },
  { label: '月份', value: 'month' },
  { label: '年份', value: 'year' },
]

// 表单项列表 - 优先使用 modelValue，其次使用 initialSchema
const formItems = ref([...(props.modelValue.length > 0 ? props.modelValue : props.initialSchema)])

// 监听 initialSchema 变化
watch(() => props.initialSchema, (newSchema) => {
  if (newSchema && newSchema.length > 0 && formItems.value.length === 0) {
    formItems.value = [...newSchema]
  }
}, { immediate: true })

// 选中的索引
const selectedIndex = ref(null)

// 当前编辑项的本地副本
const currentItem = ref(null)

// 监听选中索引变化，同步当前编辑项
watch(selectedIndex, (newIndex) => {
  if (newIndex !== null && formItems.value[newIndex]) {
    // 创建深拷贝以避免直接修改
    currentItem.value = JSON.parse(JSON.stringify(formItems.value[newIndex]))
  }
  else {
    currentItem.value = null
  }
})

// 更新字段属性
function updateField(field, value) {
  if (currentItem.value && selectedIndex.value !== null) {
    currentItem.value[field] = value
    formItems.value[selectedIndex.value][field] = value
    emitChange()
  }
}

// 更新props属性
function updateProps(key, value) {
  if (currentItem.value && selectedIndex.value !== null) {
    currentItem.value.props[key] = value
    formItems.value[selectedIndex.value].props[key] = value
    emitChange()
  }
}

// 更新选项
function updateOption(index, key, value) {
  if (currentItem.value && selectedIndex.value !== null) {
    currentItem.value.props.options[index][key] = value
    formItems.value[selectedIndex.value].props.options[index][key] = value
    emitChange()
  }
}

// 删除选项
function removeOption(index) {
  if (currentItem.value && selectedIndex.value !== null) {
    currentItem.value.props.options.splice(index, 1)
    formItems.value[selectedIndex.value].props.options.splice(index, 1)
    emitChange()
  }
}

// 更新校验消息
function updateRuleMessage(message) {
  if (currentItem.value && selectedIndex.value !== null) {
    if (!currentItem.value.rules || currentItem.value.rules.length === 0) {
      currentItem.value.rules = [{ required: false, message }]
      formItems.value[selectedIndex.value].rules = [{ required: false, message }]
    }
    else {
      currentItem.value.rules[0].message = message
      formItems.value[selectedIndex.value].rules[0].message = message
    }
    emitChange()
  }
}

// 预览弹窗
const showPreview = ref(false)

// 字段计数器
let fieldCounter = 1

// 拖拽开始
function handleDragStart(event, component) {
  event.dataTransfer.setData('component', JSON.stringify(component))
}

// 拖拽放置
function handleDrop(event) {
  const data = event.dataTransfer.getData('component')
  if (!data)
    return

  const component = JSON.parse(data)
  const newItem = createFormItem(component)
  formItems.value.push(newItem)
  selectedIndex.value = formItems.value.length - 1

  emitChange()
}

// 创建表单项
function createFormItem(component) {
  const field = `field_${fieldCounter++}`
  return {
    type: component.type,
    field,
    label: component.name,
    defaultValue: null,
    required: false,
    disabled: false,
    props: { ...component.defaultProps },
    rules: [{ required: false, message: `请输入${component.name}` }],
  }
}

// 选择项
function selectItem(index) {
  selectedIndex.value = index
}

// 复制项
function copyItem(index) {
  const item = JSON.parse(JSON.stringify(formItems.value[index]))
  item.field = `field_${fieldCounter++}`
  item.label = `${item.label}_副本`
  formItems.value.splice(index + 1, 0, item)
  emitChange()
}

// 删除项
function removeItem(index) {
  formItems.value.splice(index, 1)
  if (selectedIndex.value === index) {
    selectedIndex.value = null
  }
  else if (selectedIndex.value > index) {
    selectedIndex.value--
  }
  emitChange()
}

// 添加选项
function addOption() {
  if (currentItem.value && selectedIndex.value !== null) {
    const newOption = { label: '', value: '' }
    currentItem.value.props.options.push(newOption)
    formItems.value[selectedIndex.value].props.options.push(newOption)
    emitChange()
  }
}

// 清空
function handleClear() {
  formItems.value = []
  selectedIndex.value = null
  emitChange()
}

// 预览
function handlePreview() {
  showPreview.value = true
}

// 导出JSON
function handleExport() {
  const json = JSON.stringify(formItems.value, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'form-schema.json'
  a.click()
  URL.revokeObjectURL(url)
}

// 触发变更
function emitChange() {
  emit('update:modelValue', [...formItems.value])
}

// 保存表单
function handleSave() {
  emit('save', [...formItems.value])
}

// 获取表单配置
function getFormSchema() {
  return [...formItems.value]
}

// 设置表单配置
function setFormSchema(schema) {
  formItems.value = [...schema]
}

// 暴露方法
defineExpose({
  getFormSchema,
  setFormSchema,
})
</script>

<style scoped>
.form-designer-container {
  height: 100%;
  display: flex;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
}

.components-panel,
.properties-panel {
  width: 280px;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e0e0e0;
}

.properties-panel {
  border-right: none;
  border-left: 1px solid #e0e0e0;
}

.panel-header {
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid #e0e0e0;
  background: #fafafa;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.component-group {
  margin-bottom: 16px;
}

.group-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  padding-left: 4px;
}

.component-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.component-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  background: #fafafa;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.2s;
}

.component-item:hover {
  background: #e8f4ff;
  border-color: #1890ff;
}

.component-item:active {
  cursor: grabbing;
}

.component-item i {
  font-size: 20px;
  color: #1890ff;
}

.component-item span {
  font-size: 12px;
  color: #333;
}

.design-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px;
}

.design-toolbar {
  margin-bottom: 12px;
}

.form-canvas {
  flex: 1;
  background: #fff;
  border: 2px dashed #d9d9d9;
  border-radius: 4px;
  overflow-y: auto;
  padding: 16px;
}

.form-canvas.is-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-placeholder {
  text-align: center;
  color: #999;
}

.empty-placeholder i {
  font-size: 48px;
  margin-bottom: 8px;
}

.form-items-list {
  min-height: 100%;
}

.form-item-wrapper {
  position: relative;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  transition: all 0.2s;
  cursor: pointer;
}

.form-item-wrapper:hover {
  background: #f5f5f5;
}

.form-item-wrapper.is-selected {
  background: #e8f4ff;
  border-color: #1890ff;
}

.form-item-actions {
  position: absolute;
  top: 4px;
  right: 4px;
  display: none;
  gap: 4px;
}

.form-item-wrapper:hover .form-item-actions,
.form-item-wrapper.is-selected .form-item-actions {
  display: flex;
}

.form-item-content {
  pointer-events: none;
}

.options-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.option-item .n-input {
  flex: 1;
}
</style>
