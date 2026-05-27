<template>
  <n-spin :show="loading">
    <div v-if="relations.length" class="relation-list">
      <div v-for="relation in relations" :key="relation.id" class="relation-row">
        <div class="relation-main">
          <DictTag dict-type="ai_business_relation_type" :value="relation.relationType" :bordered="false" />
          <strong>{{ relation.relationName }}</strong>
          <span>{{ relation.sourceObjectName || relation.sourceObjectCode }} → {{ relation.targetObjectName || relation.targetObjectCode }}</span>
        </div>
        <p>{{ relation.description || relation.relationConfig || '对象关系配置' }}</p>
      </div>
    </div>
    <n-empty v-else-if="!loading" description="暂无对象关系" />
  </n-spin>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { businessObjectRelations } from '@/api/business-app'
import DictTag from '@/components/DictTag.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
})

const loading = ref(false)
const relations = ref([])

async function loadRelations() {
  if (!props.objectId) {
    relations.value = []
    return
  }
  loading.value = true
  try {
    const res = await businessObjectRelations(props.objectId)
    relations.value = res.data || []
  }
  finally {
    loading.value = false
  }
}

watch(() => props.objectId, loadRelations)

onMounted(loadRelations)
</script>

<style scoped>
.relation-list {
  display: grid;
  gap: 10px;
}

.relation-row {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.relation-main {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.relation-main strong {
  color: #111827;
  font-size: 14px;
}

.relation-main span {
  color: #6b7280;
  font-size: 13px;
}

.relation-row p {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}
</style>
