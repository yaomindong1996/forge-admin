<template>
  <!-- 标准 CRUD 模板：直接复用 AiCrudPage 组件 -->
  <AiCrudPage ref="crudRef" v-bind="crudProps" />
</template>

<script setup>
import { ref } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'

/**
 * SimpleCrudTemplate - 标准 CRUD 页面模板
 * 适合平坦型数据的属性增删改查
 * 接收标准的 crudProps 对象，直接传递给 AiCrudPage
 */
const props = defineProps({
  crudProps: {
    type: Object,
    required: true,
  },
})

const crudRef = ref(null)

defineExpose({
  showAdd: (...args) => crudRef.value?.showAdd?.(...args),
  showDetail: (...args) => crudRef.value?.showDetail?.(...args),
  refresh: (...args) => crudRef.value?.refresh?.(...args),
})
</script>
