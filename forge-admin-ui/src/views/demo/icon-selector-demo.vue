<template>
  <div class="icon-selector-demo">
    <n-card title="图标选择器演示">
      <n-space vertical>
        <n-form :model="formValue" :rules="rules" ref="formRef">
          <n-form-item label="菜单名称" path="name">
            <n-input v-model:value="formValue.name" placeholder="请输入菜单名称" />
          </n-form-item>
          
          <n-form-item label="图标" path="icon">
            <IconSelector v-model="formValue.icon" />
          </n-form-item>
          
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleSubmit">提交</n-button>
              <n-button @click="handleReset">重置</n-button>
            </n-space>
          </n-form-item>
        </n-form>
        
        <n-divider />
        
        <div>
          <h3>选中的图标预览:</h3>
          <div class="preview">
            <n-icon v-if="formValue.icon && getIconComponent(formValue.icon)" 
                    :component="getIconComponent(formValue.icon)" 
                    size="64" />
            <div v-else-if="formValue.icon" class="icon-text-preview">
              {{ formValue.icon }}
            </div>
            <div v-else class="no-icon">
              未选择图标
            </div>
            <div class="icon-name-preview">{{ formValue.icon || '无' }}</div>
          </div>
        </div>
      </n-space>
    </n-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import * as ionicons from '@vicons/ionicons5'
import IconSelector from '@/components/IconSelector.vue'

// 使用动态导入方式加载本地 SVG 图标
const localIcons = import.meta.glob('@/assets/icons/**/*.svg', { eager: true, as: 'component' })

// 将 glob 导入的结果转换为可以在 getIconComponent 中使用的格式
const localIconComponents = {}

// 处理所有本地图标
for (const path in localIcons) {
  // 匹配图标文件名，支持子目录结构
  const match = path.match(/icons\/([\w-]+)\/([\w-]+)\.svg$/)
  if (match) {
    // 生成图标名称，格式为 '目录名:文件名'
    const dirName = match[1]
    const fileName = match[2]
    localIconComponents[`${dirName}:${fileName}`] = localIcons[path]
  } else {
    // 如果没有子目录，直接使用文件名
    const matchRoot = path.match(/icons\/([\w-]+)\.svg$/)
    if (matchRoot) {
      localIconComponents[matchRoot[1]] = localIcons[path]
    }
  }
}

const formRef = ref(null)
// 使用全局$message实例而不是useMessage()
const message = window.$message

const formValue = ref({
  name: '',
  icon: ''
})

const rules = {
  name: {
    required: true,
    message: '请输入菜单名称',
    trigger: 'blur'
  }
}

// 获取图标组件用于预览
function getIconComponent(name) {
  if (!name) return null
  
  // 如果是 Ionicons 图标
  if (Object.keys(ionicons).includes(name)) {
    return ionicons[name]
  }
  
  // 如果是本地图标
  if (localIconComponents[name]) {
    return localIconComponents[name]
  }
  
  return null
}

function handleSubmit() {
  formRef.value?.validate((errors) => {
    if (!errors) {
      message.success('提交成功: ' + JSON.stringify(formValue.value))
    } else {
      message.error('请填写必填项')
    }
  })
}

function handleReset() {
  formValue.value = {
    name: '',
    icon: ''
  }
}
</script>

<style scoped>
.icon-selector-demo {
  padding: 16px;
  max-width: 600px;
  margin: 0 auto;
}

.preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px;
  border: 1px dashed #ddd;
  border-radius: 8px;
}

.icon-text-preview {
  font-size: var(--font-size-base);
  color: #666;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 10px;
}

.no-icon {
  color: #999;
  font-style: italic;
}

.icon-name-preview {
  margin-top: 10px;
  font-size: var(--font-size-base);
  color: #666;
}
</style>