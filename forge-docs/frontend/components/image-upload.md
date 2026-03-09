# ImageUpload 图片上传

图片上传组件，支持图片预览、多图上传、大小限制。

## 基础用法

```vue
<template>
  <ImageUpload 
    v-model="imageList" 
    :limit="5"
    :file-size="5"
  />
</template>
```

## Props

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | String/Array | '' | v-model 绑定值 |
| action | String | '/api/file/upload' | 上传地址 |
| businessType | String | 'image' | 业务类型 |
| limit | Number | 5 | 图片数量限制 |
| fileSize | Number | 5 | 大小限制 (MB) |
| fileType | Array | ['png','jpg','jpeg','webp','gif'] | 图片类型 |
| multiple | Boolean | true | 多选 |
| showTip | Boolean | true | 显示提示 |
| disabled | Boolean | false | 禁用 |
| valueType | String | 'string' | 返回值类型：string/array |

## 事件

| 事件 | 参数 | 说明 |
|------|------|------|
| success | fileData | 上传成功 |
| error | error | 上传失败 |
| remove | file | 删除图片 |

## 方法

```js
// 手动提交
imageUploadRef.value.submit()

// 清空图片
imageUploadRef.value.clear()
```

## 示例

```vue
<script setup>
import { ref } from 'vue'
import ImageUpload from '@/components/image-upload/index.vue'

const formData = ref({
  avatar: '',
  images: []
})
</script>

<template>
  <!-- 单图 -->
  <n-form-item label="头像">
    <ImageUpload 
      v-model="formData.avatar" 
      :limit="1"
    />
  </n-form-item>
  
  <!-- 多图 -->
  <n-form-item label="相册">
    <ImageUpload 
      v-model="formData.images"
      :limit="9"
      value-type="array"
    />
  </n-form-item>
</template>
```