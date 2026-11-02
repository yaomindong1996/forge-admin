<!-- 手写业务弹窗标题区：配置表单按钮 + 弹窗 -->
<template>
  <span class="biz-form-config-entry">
    <n-button
      v-if="visible"
      size="small"
      quaternary
      @click="open"
    >
      <template #icon>
        <i class="i-lucide:settings-2" />
      </template>
      配置表单
    </n-button>
    <BizFormConfigModal
      v-model:show="show"
      :object-type="objectType"
      @saved="$emit('saved')"
    />
  </span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import BizFormConfigModal from './BizFormConfigModal.vue'

const props = defineProps({
  objectType: { type: String, required: true },
  canConfig: { type: Boolean, default: false },
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'saved'])

const show = ref(false)
watch(() => props.modelValue, (v) => { show.value = v })
watch(show, (v) => emit('update:modelValue', v))

const visible = computed(() => props.canConfig)

function open() {
  show.value = true
}
</script>
