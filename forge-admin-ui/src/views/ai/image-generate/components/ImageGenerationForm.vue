<template>
  <NForm label-placement="top" size="medium" @submit.prevent="emit('generate')">
    <!-- 文本与模型配置 -->
    <NFormItem label="提示词" required>
      <NInput
        :value="form.prompt" type="textarea" :rows="3" :maxlength="6000" show-count
        placeholder="描述你想要生成的图片内容" :disabled="submitting"
        @update:value="update('prompt', $event)"
      />
    </NFormItem>
    <NGrid cols="1 s:2" responsive="screen" :x-gap="16">
      <NFormItemGi label="负面提示词">
        <NInput
          :value="form.negativePrompt" :maxlength="3000" placeholder="不希望出现的内容（可选）"
          :disabled="submitting" @update:value="update('negativePrompt', $event)"
        />
      </NFormItemGi>
      <NFormItemGi label="尺寸">
        <NSelect
          :value="form.size" :options="sizes" :disabled="submitting" @update:value="update('size', $event)"
        />
      </NFormItemGi>
    </NGrid>
    <NFormItem label="模型" required>
      <NSelect
        :value="form.modelId" :options="modelOptions" placeholder="选择图片生成模型" filterable
        :disabled="submitting" @update:value="update('modelId', $event)"
      />
    </NFormItem>
    <!-- 只负责显式提交，查询恢复绝不调用此事件 -->
    <NButton type="primary" attr-type="submit" :loading="submitting" :disabled="!allowed">
      生成图片
    </NButton>
  </NForm>
</template>

<script setup>
import { NButton, NForm, NFormItem, NFormItemGi, NGrid, NInput, NSelect } from 'naive-ui'

const props = defineProps({
  form: { type: Object, required: true },
  modelOptions: { type: Array, default: () => [] },
  submitting: Boolean,
  allowed: Boolean,
})
const emit = defineEmits(['update:form', 'generate'])
const sizes = [[1024, 1024], [512, 512], [1792, 1024], [1024, 1792]].map(([width, height]) => ({
  label: `${width} × ${height}`,
  value: `${width}x${height}`,
}))
function update(key, value) {
  emit('update:form', { ...props.form, [key]: value })
}
</script>
