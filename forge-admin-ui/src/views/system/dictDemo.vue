<!--
  字典组件使用示例
  演示如何使用 useDict、DictTag 和 DictSelect
-->

<template>
  <div class="dict-demo-page">
    <n-card title="字典组件使用示例">
      <n-space vertical :size="24">
        <!-- 1. 字典数据加载 -->
        <n-card title="1. 字典数据加载（useDict）" size="small">
          <n-spin :show="loading">
            <n-space vertical>
              <div>
                <n-text strong>加载的字典类型：</n-text>
                <n-text>sys_yes_no, sys_normal_disable</n-text>
              </div>
              
              <n-divider />
              
              <div>
                <n-text strong>sys_yes_no 字典数据：</n-text>
                <pre>{{ JSON.stringify(dict.sys_yes_no, null, 2) }}</pre>
              </div>
              
              <div>
                <n-text strong>sys_normal_disable 字典数据：</n-text>
                <pre>{{ JSON.stringify(dict.sys_normal_disable, null, 2) }}</pre>
              </div>
              
              <n-button @click="reload()">重新加载字典</n-button>
            </n-space>
          </n-spin>
        </n-card>

        <!-- 2. 字典标签显示 -->
        <n-card title="2. 字典标签显示（DictTag）" size="small">
          <n-space vertical>
            <div>
              <n-text strong>使用 options 属性：</n-text>
              <n-space>
                <DictTag :options="dict.sys_yes_no" value="Y" />
                <DictTag :options="dict.sys_yes_no" value="N" />
              </n-space>
            </div>
            
            <div>
              <n-text strong>使用 dictType 属性：</n-text>
              <n-space>
                <DictTag dict-type="sys_yes_no" value="Y" />
                <DictTag dict-type="sys_yes_no" value="N" />
              </n-space>
            </div>
            
            <div>
              <n-text strong>自定义类型：</n-text>
              <n-space>
                <DictTag :options="dict.sys_normal_disable" value="0" type="success" />
                <DictTag :options="dict.sys_normal_disable" value="1" type="error" />
              </n-space>
            </div>
            
            <div>
              <n-text strong>所有标签类型：</n-text>
              <n-space>
                <DictTag :options="[{ label: '默认', value: '1', listClass: 'default' }]" value="1" />
                <DictTag :options="[{ label: '成功', value: '1', listClass: 'success' }]" value="1" />
                <DictTag :options="[{ label: '信息', value: '1', listClass: 'info' }]" value="1" />
                <DictTag :options="[{ label: '警告', value: '1', listClass: 'warning' }]" value="1" />
                <DictTag :options="[{ label: '错误', value: '1', listClass: 'error' }]" value="1" />
              </n-space>
            </div>
            
            <div>
              <n-text strong>不同尺寸：</n-text>
              <n-space>
                <DictTag :options="dict.sys_yes_no" value="Y" size="small" />
                <DictTag :options="dict.sys_yes_no" value="Y" size="medium" />
                <DictTag :options="dict.sys_yes_no" value="Y" size="large" />
              </n-space>
            </div>
            
            <div>
              <n-text strong>圆角和无边框：</n-text>
              <n-space>
                <DictTag :options="dict.sys_yes_no" value="Y" round />
                <DictTag :options="dict.sys_yes_no" value="Y" :bordered="false" />
                <DictTag :options="dict.sys_yes_no" value="Y" round :bordered="false" />
              </n-space>
            </div>
          </n-space>
        </n-card>

        <!-- 3. 字典选择器 -->
        <n-card title="3. 字典选择器（DictSelect）" size="small">
          <n-space vertical>
            <div>
              <n-text strong>单选：</n-text>
              <DictSelect 
                v-model:value="formData.yesNo" 
                dict-type="sys_yes_no"
                placeholder="请选择是否"
                style="width: 200px"
              />
              <n-text>选中值：{{ formData.yesNo }}</n-text>
            </div>
            
            <div>
              <n-text strong>多选：</n-text>
              <DictSelect 
                v-model:value="formData.status" 
                dict-type="sys_normal_disable"
                multiple
                placeholder="请选择状态"
                style="width: 300px"
              />
              <n-text>选中值：{{ formData.status }}</n-text>
            </div>
            
            <div>
              <n-text strong>禁用状态：</n-text>
              <DictSelect 
                v-model:value="formData.disabled" 
                dict-type="sys_yes_no"
                disabled
                style="width: 200px"
              />
            </div>
          </n-space>
        </n-card>

        <!-- 4. 在表格中使用 -->
        <n-card title="4. 在表格中使用" size="small">
          <n-data-table
            :columns="tableColumns"
            :data="tableData"
            :pagination="false"
          />
        </n-card>

        <!-- 5. 辅助函数 -->
        <n-card title="5. 辅助函数" size="small">
          <n-space vertical>
            <div>
              <n-text strong>getLabel('sys_yes_no', 'Y')：</n-text>
              <n-text>{{ getLabel('sys_yes_no', 'Y') }}</n-text>
            </div>
            
            <div>
              <n-text strong>getDict('sys_yes_no', 'N')：</n-text>
              <pre>{{ JSON.stringify(getDict('sys_yes_no', 'N'), null, 2) }}</pre>
            </div>
          </n-space>
        </n-card>
      </n-space>
    </n-card>
  </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { useDict } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'
import DictSelect from '@/components/DictSelect.vue'

defineOptions({ name: 'DictDemo' })

// 加载字典
const { dict, loading, reload, getLabel, getDict } = useDict('sys_yes_no', 'sys_normal_disable')

// 表单数据
const formData = ref({
  yesNo: '',
  status: [],
  disabled: 'Y'
})

// 表格数据
const tableData = ref([
  { id: 1, name: '张三', isActive: 'Y', status: '0' },
  { id: 2, name: '李四', isActive: 'N', status: '1' },
  { id: 3, name: '王五', isActive: 'Y', status: '0' }
])

// 表格列
const tableColumns = [
  { key: 'id', title: 'ID', width: 80 },
  { key: 'name', title: '姓名', width: 120 },
  {
    key: 'isActive',
    title: '是否激活',
    width: 120,
    render: (row) => h(DictTag, { options: dict.value.sys_yes_no, value: row.isActive })
  },
  {
    key: 'status',
    title: '状态',
    width: 120,
    render: (row) => h(DictTag, { options: dict.value.sys_normal_disable, value: row.status })
  }
]
</script>

<style scoped>
.dict-demo-page {
  padding: 16px;
}

pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  overflow-x: auto;
}
</style>
