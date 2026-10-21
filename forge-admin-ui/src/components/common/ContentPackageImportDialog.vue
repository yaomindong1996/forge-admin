<template>
  <n-modal
    :show="show"
    preset="card"
    :title="copy.title"
    :style="{ width: 'min(480px, 94vw)' }"
    :mask-closable="!busy"
    :closable="!busy"
    @update:show="emit('update:show', $event)"
  >
    <div class="pkg-intro">
      <p class="pkg-intro__lead">{{ copy.lead }}</p>
      <ul class="pkg-intro__list">
        <li v-for="(item, index) in copy.points" :key="index">
          {{ item }}
        </li>
      </ul>
    </div>

    <n-upload
      :default-upload="false"
      :max="1"
      accept=".json,application/json"
      :disabled="busy"
      @change="handleFileChange"
    >
      <n-upload-dragger class="pkg-dragger">
        <div class="pkg-upload">
          <i class="i-lucide:file-json pkg-upload__icon" aria-hidden="true" />
          <strong>{{ fileName || copy.dropTitle }}</strong>
          <small>{{ copy.dropHint }}</small>
        </div>
      </n-upload-dragger>
    </n-upload>

    <template #footer>
      <n-space justify="end">
        <n-button :disabled="busy" @click="emit('update:show', false)">
          取消
        </n-button>
        <n-button type="primary" :loading="busy" :disabled="!selectedFile" @click="confirm">
          {{ copy.confirm }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  kind: { type: String, required: true },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['update:show', 'confirm'])

const selectedFile = ref(null)
const fileName = ref('')

const COPY = {
  print: {
    title: '安装打印模板',
    lead: '把市场下载的打印模板 JSON 装进「我的模板」库，之后各业务页可反复选用。',
    points: [
      '安装到当前租户模板库，不是一次性塞进画布',
      '同编码再次安装会更新已有包',
      '不包含业务单据数据，只含版式和字段绑定',
    ],
    dropTitle: '选择或拖入打印模板文件',
    dropHint: '支持 .json 打印模板包',
    confirm: '安装到我的模板',
  },
  form: {
    title: '安装表单模板',
    lead: '把市场下载的表单 JSON 装进「我的模板」库，其它页面可直接选用。',
    points: [
      '先进入租户模板库，再按需套用到当前表单',
      '同编码再次安装会更新已有包',
      '不会写入本地工程源码',
    ],
    dropTitle: '选择或拖入表单模板文件',
    dropHint: '支持 .json 表单模板包',
    confirm: '安装到我的模板',
  },
  'print-component': {
    title: '安装打印组件',
    lead: '把市场下载的打印组件 JSON 装进「我的模板」库，设计时选用插入。',
    points: [
      '用于审批章、签章等可复用组件',
      '同编码再次安装会更新已有包',
      '不会写入本地工程源码',
    ],
    dropTitle: '选择或拖入打印组件文件',
    dropHint: '支持 .json 打印组件包',
    confirm: '安装到我的模板',
  },
}

const copy = computed(() => COPY[props.kind] || {
  title: '导入模板',
  lead: '选择从模板市场下载的 JSON 文件。',
  points: ['不会写入本地工程源码'],
  dropTitle: '选择或拖入 JSON 文件',
  dropHint: '支持 .json',
  confirm: '开始导入',
})

watch(() => props.show, (visible) => {
  if (!visible)
    return
  selectedFile.value = null
  fileName.value = ''
})

function handleFileChange({ fileList }) {
  const item = fileList?.[0]
  selectedFile.value = item?.file || null
  fileName.value = item?.name || selectedFile.value?.name || ''
}

function confirm() {
  if (!selectedFile.value)
    return
  emit('confirm', { file: selectedFile.value, kind: props.kind })
}
</script>

<style scoped>
.pkg-intro {
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.pkg-intro__lead {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.65;
}

.pkg-intro__list {
  margin: 0;
  padding-left: 18px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.75;
}

.pkg-dragger {
  padding: 6px;
}

.pkg-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 12px;
  text-align: center;
}

.pkg-upload__icon {
  font-size: 28px;
  color: #2563eb;
}

.pkg-upload strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.pkg-upload small {
  color: #94a3b8;
  font-size: 12px;
}
</style>
