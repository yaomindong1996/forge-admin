<template>
  <AiCrudPage ref="crudRef" v-bind="mergedCrudProps" />
</template>

<script setup>
import { computed, ref } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'

const props = defineProps({
  crudProps: {
    type: Object,
    required: true,
  },
})

const mergedCrudProps = computed(() => ({
  ...props.crudProps,
  loadDetailOnEdit: true,
  modalWidth: props.crudProps.modalWidth || props.crudProps.options?.modalWidth || '1080px',
  childrenConfig: props.crudProps.childrenConfig || props.crudProps.options?.masterDetailConfig?.children || [],
}))

const crudRef = ref(null)

defineExpose({
  showAdd: (...args) => crudRef.value?.showAdd?.(...args),
  showDetail: (...args) => crudRef.value?.showDetail?.(...args),
  refresh: (...args) => crudRef.value?.refresh?.(...args),
})
</script>
