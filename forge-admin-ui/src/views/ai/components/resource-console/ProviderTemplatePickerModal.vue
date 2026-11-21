<script setup>
import { NButton, NEmpty, NInput, NModal, NSpin, NTag } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { providerTemplates } from '@/api/ai'

const props = defineProps({
  show: { type: Boolean, default: false },
})

const emit = defineEmits(['update:show', 'select'])

const loading = ref(false)
const keyword = ref('')
const templates = ref([])

const filteredTemplates = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q)
    return templates.value
  return templates.value.filter(item =>
    `${item.name || ''} ${item.providerType || ''} ${item.description || ''}`.toLowerCase().includes(q),
  )
})

async function loadTemplates() {
  loading.value = true
  try {
    const res = await providerTemplates()
    templates.value = res.code === 200 && Array.isArray(res.data) ? res.data : []
  }
  catch {
    templates.value = []
  }
  finally {
    loading.value = false
  }
}

watch(() => props.show, (visible) => {
  if (visible) {
    keyword.value = ''
    void loadTemplates()
  }
})

function choose(tpl) {
  emit('select', tpl)
  emit('update:show', false)
}
</script>

<template>
  <NModal
    :show="show"
    preset="card"
    title="从预设模板快速创建供应商（OpenAI 兼容）"
    :style="{ maxWidth: '860px', width: 'calc(100vw - 32px)' }"
    @update:show="emit('update:show', $event)"
  >
    <div class="tpl-picker">
      <NInput v-model:value="keyword" clearable placeholder="搜索厂商名称、编码或说明（支持 19 家主流厂商）" />
      <NSpin :show="loading">
        <div v-if="filteredTemplates.length" class="tpl-grid">
          <button
            v-for="tpl in filteredTemplates"
            :key="tpl.templateKey"
            type="button"
            class="tpl-card"
            @click="choose(tpl)"
          >
            <div class="tpl-card__head">
              <strong>{{ tpl.name }}</strong>
              <NTag v-if="tpl.configuredCount > 0" size="tiny" type="success" :bordered="false">
                已配 {{ tpl.configuredCount }}
              </NTag>
            </div>
            <p class="tpl-card__desc">
              {{ tpl.description || tpl.defaultBaseUrl }}
            </p>
            <div class="tpl-card__url">
              {{ tpl.defaultBaseUrl || '自定义 Base URL' }}
            </div>
          </button>
        </div>
        <NEmpty v-else description="没有匹配的预设供应商" />
      </NSpin>
    </div>
    <template #action>
      <div class="tpl-footer">
        <NButton @click="emit('update:show', false)">
          关闭
        </NButton>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
.tpl-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tpl-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 10px;
  max-height: 460px;
  overflow: auto;
  padding: 2px;
}

.tpl-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.tpl-card:hover {
  border-color: var(--primary-color, #2563eb);
}

.tpl-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  color: var(--text-primary, #111827);
}

.tpl-card__desc {
  margin: 0;
  font-size: 12px;
  color: var(--text-tertiary, #6b7280);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tpl-card__url {
  font-size: 11px;
  color: var(--text-tertiary, #6b7280);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tpl-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
