# FileUpload 文件上传

通用文件上传组件，支持多文件、类型限制、大小限制。

## 基础用法

```vue
<template>
  <FileUpload 
    v-model="fileList" 
    :limit="5"
    :file-size="10"
    :file-type="['pdf', 'doc', 'docx']"
  />
</template>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | String/Array | '' | v-model 绑定值 |
| action | String | '/api/file/upload' | 上传地址 |
| businessType | String | 'common' | 业务类型 |
| businessId | String | '' | 业务 ID |
| storageType | String | 'local' | 存储类型 |
| limit | Number | 5 | 数量限制 |
| fileSize | Number | 10 | 大小限制 (MB) |
| fileType | Array | [] | 文件类型限制 |
| multiple | Boolean | true | 多选 |
| showFileList | Boolean | true | 显示文件列表 |
| showTip | Boolean | true | 显示提示 |
| valueType | String | 'string' | 返回值类型：string/array/object |

## 返回值类型

| 类型 | 说明 |
|------|------|
| string | 逗号分隔的 fileId 字符串 |
| array | fileId 数组 |
| object | 完整文件对象数组 |

## 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| success | fileData | 上传成功 |
| error | error | 上传失败 |
| remove | file | 删除文件 |

## 方法

```js
// 手动提交
fileUploadRef.value.submit()

// 清空文件
fileUploadRef.value.clear()
```

## 示例

```vue
<script setup>
import { ref } from 'vue'
import FileUpload from '@/components/file-upload/index.vue'

const formData = ref({
  attachments: ''
})
</script>

<template>
  <n-form :model="formData">
    <n-form-item label="附件">
      <FileUpload 
        v-model="formData.attachments"
        :limit="3"
        :file-size="20"
        :file-type="['pdf', 'doc', 'docx', 'xls', 'xlsx']"
        business-type="contract"
      />
    </n-form-item>
  </n-form>
</template>
```