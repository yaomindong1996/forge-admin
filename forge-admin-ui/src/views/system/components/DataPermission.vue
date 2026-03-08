<!--
  数据权限组件
  用于配置角色的数据访问权限
-->

<template>
  <div class="data-permission">
    <n-spin :show="loading">
      <n-form
        ref="formRef"
        :model="formData"
        label-placement="left"
        label-width="120"
      >
        <n-form-item label="数据权限范围">
          <n-radio-group v-model:value="formData.dataScope" @update:value="handleDataScopeChange">
            <n-space vertical>
              <n-radio value="1">全部数据权限</n-radio>
              <n-radio value="2">自定义数据权限</n-radio>
              <n-radio value="3">本部门数据权限</n-radio>
              <n-radio value="4">本部门及以下数据权限</n-radio>
              <n-radio value="5">仅本人数据权限</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <n-form-item v-if="formData.dataScope === '2'" label="部门权限">
          <n-tree
            :data="deptTreeData"
            :checked-keys="formData.deptIds"
            checkable
            cascade
            selectable
            @update:checked-keys="handleDeptChange"
          />
        </n-form-item>

        <n-form-item>
          <n-space>
            <n-button type="primary" @click="handleSave" :loading="saving">
              保存
            </n-button>
            <n-button @click="handleReset">
              重置
            </n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-spin>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { request } from '@/utils'

const props = defineProps({
  // 角色ID
  roleId: {
    type: [String, Number],
    required: true
  }
})

const message = window.$message

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const deptTreeData = ref([])

const formData = ref({
  dataScope: '1',
  deptIds: []
})

const originalData = ref({})

/**
 * 数据范围变化
 */
function handleDataScopeChange(value) {
  if (value !== '2') {
    formData.value.deptIds = []
  }
}

/**
 * 部门选择变化
 */
function handleDeptChange(keys) {
  formData.value.deptIds = keys
}

/**
 * 保存
 */
async function handleSave() {
  try {
    saving.value = true

    await request.put('/role/dataPermission', {
      roleId: props.roleId,
      dataScope: formData.value.dataScope,
      deptIds: formData.value.deptIds
    })

    message.success('保存成功')
    originalData.value = { ...formData.value }
  } catch (error) {
    console.error('保存失败:', error)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 重置
 */
function handleReset() {
  formData.value = { ...originalData.value }
}

/**
 * 加载部门树数据
 */
async function loadDeptTree() {
  try {
    const res = await request.get('/department/tree')
    deptTreeData.value = convertToTreeData(res.data || [])
  } catch (error) {
    console.error('加载部门树失败:', error)
    message.error('加载部门树失败')
  }
}

/**
 * 加载数据权限
 */
async function loadDataPermission() {
  try {
    loading.value = true

    const res = await request.get('/role/dataPermission', {
      params: { roleId: props.roleId }
    })

    if (res.data) {
      formData.value = {
        dataScope: res.data.dataScope || '1',
        deptIds: res.data.deptIds || []
      }
      originalData.value = { ...formData.value }
    }
  } catch (error) {
    console.error('加载数据权限失败:', error)
    message.error('加载数据权限失败')
  } finally {
    loading.value = false
  }
}

/**
 * 转换为树形数据
 */
function convertToTreeData(data) {
  if (!Array.isArray(data)) return []

  return data.map(item => ({
    key: item.id || item.key,
    label: item.name || item.label || item.title,
    children: item.children ? convertToTreeData(item.children) : undefined
  }))
}

// 监听角色ID变化
watch(() => props.roleId, () => {
  if (props.roleId) {
    loadDataPermission()
  }
}, { immediate: true })

// 组件挂载时加载数据
onMounted(() => {
  loadDeptTree()
  if (props.roleId) {
    loadDataPermission()
  }
})
</script>

<style scoped>
.data-permission {
  padding: 16px;
}
</style>
