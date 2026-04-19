<script>
</script>

<template>
  <CommonPage show-footer>
    <n-card title="AI Form 表单组件示例">
      <n-tabs type="line">
        <n-tab-pane name="basic" tab="基础用法">
          <n-card title="用户注册表单">
            <AiForm
              ref="basicFormRef"
              v-model:value="basicFormData"
              :schema="basicFormSchema"
              :grid-cols="2"
              @submit="handleBasicSubmit"
            />
          </n-card>

          <n-card title="表单数据" style="margin-top: 16px">
            <pre>{{ JSON.stringify(basicFormData, null, 2) }}</pre>
          </n-card>
        </n-tab-pane>

        <n-tab-pane name="advanced" tab="高级用法">
          <n-card title="复杂表单示例">
            <AiForm
              v-model:value="advancedFormData"
              :schema="advancedFormSchema"
              :grid-cols="3"
              label-placement="top"
              @submit="handleAdvancedSubmit"
            />
          </n-card>

          <n-card title="表单数据" style="margin-top: 16px">
            <pre>{{ JSON.stringify(advancedFormData, null, 2) }}</pre>
          </n-card>
        </n-tab-pane>

        <n-tab-pane name="factory" tab="FieldFactory 用法">
          <n-card title="使用 FieldFactory 创建表单">
            <AiForm
              v-model:value="factoryFormData"
              :schema="factoryFormSchema"
              :grid-cols="2"
              @submit="handleFactorySubmit"
            />
          </n-card>
        </n-tab-pane>

        <n-tab-pane name="advanced-features" tab="高级功能">
          <n-card title="远程搜索、折叠、文本展示">
            <AiForm
              v-model:value="advancedFeaturesData"
              :schema="advancedFeaturesSchema"
              :grid-cols="3"
              :enable-collapse="true"
              :max-visible-fields="6"
              :show-submit="false"
              :show-reset="false"
            >
              <template #formAction="{ formData }">
                <n-button type="primary" @click="handleSearch(formData)">
                  查询
                </n-button>
                <n-button @click="handleReset">
                  重置
                </n-button>
              </template>
            </AiForm>
          </n-card>

          <n-card title="表单数据" style="margin-top: 16px">
            <pre>{{ JSON.stringify(advancedFeaturesData, null, 2) }}</pre>
          </n-card>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </CommonPage>
</template>

<script setup>
import { ref } from 'vue'
import { AiForm, FieldFactory } from '@/components/ai-form'

export default {
  name: 'AiFormDemo',
  title: 'AI 表单示例',
}

// ==================== 基础用法 ====================
const basicFormRef = ref(null)
const basicFormData = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  gender: 'male',
  age: 18,
  agree: false,
})

const basicFormSchema = [
  {
    field: 'username',
    label: '用户名',
    type: 'input',
    placeholder: '请输入用户名',
    required: true,
    maxlength: 20,
    showCount: true,
    span: 2,
  },
  {
    field: 'email',
    label: '邮箱',
    type: 'input',
    placeholder: '请输入邮箱',
    required: true,
    rules: [
      {
        required: true,
        message: '请输入邮箱',
      },
      {
        type: 'email',
        message: '请输入正确的邮箱格式',
      },
    ],
    span: 2,
  },
  {
    field: 'password',
    label: '密码',
    type: 'input',
    placeholder: '请输入密码',
    required: true,
    props: {
      type: 'password',
      showPasswordOn: 'click',
    },
    span: 1,
  },
  {
    field: 'confirmPassword',
    label: '确认密码',
    type: 'input',
    placeholder: '请再次输入密码',
    required: true,
    props: {
      type: 'password',
      showPasswordOn: 'click',
    },
    span: 1,
    rules: [
      {
        required: true,
        message: '请再次输入密码',
      },
      {
        validator: (rule, value) => {
          return value === basicFormData.value.password
        },
        message: '两次密码输入不一致',
        trigger: ['blur', 'change'],
      },
    ],
  },
  {
    field: 'gender',
    label: '性别',
    type: 'radio',
    required: true,
    options: [
      { label: '男', value: 'male' },
      { label: '女', value: 'female' },
    ],
    span: 1,
  },
  {
    field: 'age',
    label: '年龄',
    type: 'number',
    placeholder: '请输入年龄',
    required: true,
    min: 1,
    max: 150,
    span: 1,
  },
]

function handleBasicSubmit(data) {
  $message.success('提交成功！')
  console.log('基础表单提交:', data)
}

// ==================== 高级用法 ====================
const advancedFormData = ref({
  name: '',
  role: null,
  department: [],
  hobbies: [],
  birthday: null,
  workTime: null,
  active: true,
  salary: 5000,
  remark: '',
})

const advancedFormSchema = [
  {
    field: 'name',
    label: '姓名',
    type: 'input',
    placeholder: '请输入姓名',
    required: true,
    span: 1,
  },
  {
    field: 'role',
    label: '角色',
    type: 'select',
    placeholder: '请选择角色',
    required: true,
    options: [
      { label: '管理员', value: 'admin' },
      { label: '开发', value: 'developer' },
      { label: '测试', value: 'tester' },
      { label: '运维', value: 'ops' },
    ],
    span: 1,
  },
  {
    field: 'department',
    label: '部门',
    type: 'select',
    placeholder: '请选择部门',
    multiple: true,
    options: [
      { label: '技术部', value: 'tech' },
      { label: '产品部', value: 'product' },
      { label: '运营部', value: 'operation' },
      { label: '市场部', value: 'marketing' },
    ],
    span: 1,
  },
  {
    field: 'hobbies',
    label: '爱好',
    type: 'checkbox',
    options: [
      { label: '阅读', value: 'reading' },
      { label: '运动', value: 'sports' },
      { label: '音乐', value: 'music' },
      { label: '旅游', value: 'travel' },
      { label: '编程', value: 'coding' },
    ],
    span: 3,
  },
  {
    field: 'birthday',
    label: '生日',
    type: 'date',
    placeholder: '请选择生日',
    format: 'yyyy-MM-dd',
    span: 1,
  },
  {
    field: 'workTime',
    label: '上班时间',
    type: 'time',
    placeholder: '请选择上班时间',
    format: 'HH:mm',
    span: 1,
  },
  {
    field: 'active',
    label: '是否在职',
    type: 'switch',
    checkedText: '在职',
    uncheckedText: '离职',
    span: 1,
  },
  {
    field: 'salary',
    label: '期望薪资',
    type: 'number',
    placeholder: '请输入期望薪资',
    min: 0,
    step: 1000,
    span: 3,
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    placeholder: '请输入备注信息',
    rows: 4,
    maxlength: 200,
    showCount: true,
    span: 3,
  },
]

function handleAdvancedSubmit(data) {
  $message.success('提交成功！')
  console.log('高级表单提交:', data)
}

// ==================== FieldFactory 用法 ====================
const factoryFormData = ref({})

const factoryFormSchema = [
  FieldFactory.input('companyName', '公司名称', {
    required: true,
    span: 2,
  }),
  FieldFactory.select('industry', '所属行业', [
    { label: '互联网', value: 'internet' },
    { label: '金融', value: 'finance' },
    { label: '教育', value: 'education' },
    { label: '医疗', value: 'medical' },
  ], {
    required: true,
    span: 1,
  }),
  FieldFactory.number('employeeCount', '员工人数', {
    min: 1,
    step: 10,
    span: 1,
  }),
  FieldFactory.date('foundedDate', '成立日期', {
    type: 'date',
    span: 1,
  }),
  FieldFactory.switch('listed', '是否上市', {
    span: 1,
  }),
  FieldFactory.textarea('introduction', '公司简介', {
    rows: 5,
    maxlength: 500,
    showCount: true,
    span: 2,
  }),
]

function handleFactorySubmit(data) {
  $message.success('提交成功！')
  console.log('FieldFactory 表单提交:', data)
}

// ==================== 高级功能 ====================
const advancedFeaturesData = ref({
  keyword: '',
  category: null,
  status: null,
  dateRange: null,
  minPrice: null,
  maxPrice: null,
  tags: [],
  region: null,
  creator: '',
})

const advancedFeaturesSchema = [
  FieldFactory.input('keyword', '关键词', {
    placeholder: '请输入搜索关键词',
    clearable: true,
    span: 1,
  }),
  FieldFactory.select('category', '分类', [
    { label: '电子产品', value: 'electronics' },
    { label: '服装', value: 'clothing' },
    { label: '食品', value: 'food' },
    { label: '图书', value: 'books' },
  ], { span: 1 }),
  FieldFactory.select('status', '状态', [
    { label: '全部', value: null },
    { label: '待审核', value: 'pending' },
    { label: '已通过', value: 'approved' },
    { label: '已拒绝', value: 'rejected' },
  ], { span: 1 }),
  {
    field: 'dateRange',
    label: '创建日期',
    type: 'daterange',
    span: 1,
  },
  FieldFactory.number('minPrice', '最低价格', {
    min: 0,
    placeholder: '请输入最低价格',
    span: 1,
  }),
  FieldFactory.number('maxPrice', '最高价格', {
    min: 0,
    placeholder: '请输入最高价格',
    span: 1,
  }),
  // 远程搜索下拉框示例
  FieldFactory.customSelect('region', '所在地区', {
    // api: '/api/regions',  // 实际项目中替换为真实接口
    // remote: true,
    // filterable: true,
    // 暂时使用静态数据
    options: [
      { label: '北京', value: 'beijing' },
      { label: '上海', value: 'shanghai' },
      { label: '广州', value: 'guangzhou' },
      { label: '深圳', value: 'shenzhen' },
    ],
    span: 1,
  }),
  FieldFactory.checkbox('tags', '标签', [
    { label: '热门', value: 'hot' },
    { label: '新品', value: 'new' },
    { label: '推荐', value: 'recommended' },
    { label: '特价', value: 'sale' },
  ], { span: 2 }),
  // 纯文本展示示例
  FieldFactory.text('creator', '创建人', {
    defaultValue: 'Admin',
    copy: true, // 显示复制按钮
    formatter: value => `创建人: ${value}`,
    span: 1,
  }),
]

function handleSearch(data) {
  $message.success('查询成功！')
  console.log('查询条件:', data)
}

function handleReset() {
  advancedFeaturesData.value = {
    keyword: '',
    category: null,
    status: null,
    dateRange: null,
    minPrice: null,
    maxPrice: null,
    tags: [],
    region: null,
    creator: 'Admin',
  }
  $message.info('已重置')
}
</script>

<style scoped>
pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow: auto;
}

:deep(.n-card) {
  margin-bottom: 16px;
}

:deep(.n-card:last-child) {
  margin-bottom: 0;
}
</style>
