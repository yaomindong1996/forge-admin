<template>
  <div class="encrypt-config-panel">
    <div class="panel-title">
      接口加解密配置
    </div>
    <div class="config-form">
      <div class="form-item">
        <span class="form-label">启用请求解密</span>
        <n-switch v-model:value="config.enableDecrypt" @update:value="emitUpdate" />
        <span class="form-hint">客户端加密请求体，服务端解密</span>
      </div>
      <div class="form-item">
        <span class="form-label">启用响应加密</span>
        <n-switch v-model:value="config.enableEncrypt" @update:value="emitUpdate" />
        <span class="form-hint">服务端加密响应体，客户端解密</span>
      </div>
      <div class="form-item">
        <span class="form-label">加解密范围</span>
        <n-checkbox-group v-model:value="config.operations" @update:value="emitUpdate">
          <n-space>
            <n-checkbox value="list">
              查询列表
            </n-checkbox>
            <n-checkbox value="detail">
              查询详情
            </n-checkbox>
            <n-checkbox value="create">
              新增
            </n-checkbox>
            <n-checkbox value="update">
              修改
            </n-checkbox>
            <n-checkbox value="delete">
              删除
            </n-checkbox>
          </n-space>
        </n-checkbox-group>
        <span class="form-hint">留空表示全部接口启用加解密</span>
      </div>
    </div>
    <div class="json-view">
      <n-collapse>
        <n-collapse-item title="查看 JSON">
          <pre class="json-code">{{ jsonValue }}</pre>
        </n-collapse-item>
      </n-collapse>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  value: { type: String, default: '' },
})

const emit = defineEmits(['update:value'])

const config = ref({
  enableEncrypt: false,
  enableDecrypt: false,
  operations: [],
})

function parseValue(val) {
  if (!val) {
    return { enableEncrypt: false, enableDecrypt: false, operations: [] }
  }
  try {
    return JSON.parse(val)
  }
  catch (e) {
    return { enableEncrypt: false, enableDecrypt: false, operations: [] }
  }
}

watch(() => props.value, (val) => {
  const parsed = parseValue(val)
  config.value = {
    enableEncrypt: !!parsed.enableEncrypt,
    enableDecrypt: !!parsed.enableDecrypt,
    operations: Array.isArray(parsed.operations) ? parsed.operations : [],
  }
}, { immediate: true })

function emitUpdate() {
  const payload = {
    enableEncrypt: config.value.enableEncrypt,
    enableDecrypt: config.value.enableDecrypt,
    operations: config.value.operations,
  }
  emit('update:value', JSON.stringify(payload, null, 2))
}

const jsonValue = computed(() => {
  return JSON.stringify(config.value, null, 2)
})
</script>

<style scoped>
.encrypt-config-panel {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  background: #fafafa;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #fff;
  border-radius: 6px;
}

.form-label {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.form-hint {
  font-size: 12px;
  color: #999;
}

.json-view {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ddd;
}

.json-code {
  font-size: 11px;
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 0;
}
</style>
