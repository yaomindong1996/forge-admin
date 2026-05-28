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
        <div class="binding-foot">
          <span>{{ item.statusMessage || '能力入口待确认' }}</span>
          <n-button
            text
            type="primary"
            size="small"
            :disabled="!item.canOpen"
            @click="openBinding(item)"
          >
            <template #icon>
              <n-icon><OpenOutline /></n-icon>
            </template>
            {{ item.actionLabel || '打开配置' }}
          </n-button>
        </div>
      </div>
    </div>
    <n-empty v-else-if="!loading" description="暂无接入能力" />
  </n-spin>
</template>

<script setup>
import { OpenOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
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

const router = useRouter()
const message = useMessage()
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

function openBinding(item) {
  if (!item?.canOpen || !item.entryUrl) {
    message.warning(item?.statusMessage || '能力入口暂不可打开')
    return
  }
  const openType = String(item.openType || 'ROUTE').toUpperCase()
  if (openType === 'EXTERNAL' || /^https?:\/\//i.test(item.entryUrl)) {
    window.open(item.entryUrl, '_blank', 'noopener,noreferrer')
    return
  }
  router.push(item.entryUrl)
}
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

.binding-foot {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eef2f7;
}

.binding-foot span {
  min-width: 0;
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
