<template>
  <n-spin :show="loading">
    <div v-if="bindings.length" class="binding-grid">
      <div v-for="item in bindings" :key="item.id" class="binding-item">
        <div class="binding-head">
          <DictTag dict-type="ai_business_binding_type" :value="item.bindingType" :bordered="false" />
          <DictTag dict-type="sys_enable_disable" :value="item.status" :bordered="false" />
        </div>
        <strong>{{ item.bindingName }}</strong>
        <p>{{ item.description || item.bindingKey }}</p>
      </div>
    </div>
    <n-empty v-else-if="!loading" description="暂无接入能力" />
  </n-spin>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { businessBindingList } from '@/api/business-app'
import DictTag from '@/components/DictTag.vue'

const props = defineProps({
  targetType: {
    type: String,
    required: true,
  },
  targetId: {
    type: [Number, String],
    default: null,
  },
  targetCode: {
    type: String,
    default: '',
  },
})

const loading = ref(false)
const bindings = ref([])

async function loadBindings() {
  if (!props.targetId && !props.targetCode) {
    bindings.value = []
    return
  }
  loading.value = true
  try {
    const res = await businessBindingList({
      targetType: props.targetType,
      targetId: props.targetId,
      targetCode: props.targetCode,
    })
    bindings.value = res.data || []
  }
  finally {
    loading.value = false
  }
}

watch(() => [props.targetType, props.targetId, props.targetCode], loadBindings)

onMounted(loadBindings)
</script>

<style scoped>
.binding-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.binding-item {
  min-height: 118px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.binding-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.binding-item strong {
  display: block;
  overflow: hidden;
  color: #111827;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.binding-item p {
  display: -webkit-box;
  margin: 7px 0 0;
  overflow: hidden;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
</style>
