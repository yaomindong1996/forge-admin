<script setup>
import { NAlert, NButton, NCard, NDescriptions, NDescriptionsItem, NSkeleton, NSpace } from 'naive-ui'
import { onMounted, ref } from 'vue'
import { getHelloPluginInfo } from './api/info'

const loading = ref(false)
const info = ref(null)
const error = ref('')

// 只有接口真实成功后才展示安装信息；刷新失败清空旧数据，避免误判当前装配状态。
async function loadInfo() {
  if (loading.value) {
    return
  }
  loading.value = true
  info.value = null
  error.value = ''
  try {
    const response = await getHelloPluginInfo()
    if (response?.code !== 200) {
      throw new Error(response?.message || '加载插件信息失败')
    }
    const values = [response.data?.id, response.data?.version, response.data?.coreVersion]
    if (!values.every(value => typeof value === 'string' && value.trim())) {
      throw new Error('插件信息响应不完整')
    }
    info.value = response.data
  }
  catch (cause) {
    error.value = cause?.message || '加载插件信息失败，请检查服务与访问权限后重试'
  }
  finally {
    loading.value = false
  }
}

onMounted(loadInfo)
</script>

<template>
  <section class="hello-plugin-page" aria-label="示例插件">
    <!-- 标题与刷新 -->
    <NCard title="示例插件" size="small" :bordered="true">
      <template #header-extra>
        <NButton size="small" :loading="loading" :disabled="loading" @click="loadInfo">
          刷新
        </NButton>
      </template>
      <!-- 接口加载/失败/真实结果，不使用本地描述伪造接口成功 -->
      <NSpace v-if="loading" vertical aria-busy="true" aria-label="正在加载插件信息">
        <NSkeleton text :repeat="3" />
      </NSpace>
      <NAlert v-else-if="error" type="error" title="加载插件信息失败" role="alert">
        {{ error }}
        <div class="hello-plugin-retry">
          <NButton size="small" @click="loadInfo">
            重试
          </NButton>
        </div>
      </NAlert>
      <NDescriptions v-else-if="info" :column="1" label-placement="left" size="small" bordered>
        <NDescriptionsItem label="插件 ID">
          {{ info.id }}
        </NDescriptionsItem>
        <NDescriptionsItem label="插件版本">
          {{ info.version }}
        </NDescriptionsItem>
        <NDescriptionsItem label="核心版本">
          {{ info.coreVersion }}
        </NDescriptionsItem>
      </NDescriptions>
      <!-- 版本语义说明 -->
      <p class="hello-plugin-note">
        插件版本来自运行描述，核心版本来自宿主。此页面只读取信息，不修改业务数据。
      </p>
    </NCard>
  </section>
</template>

<style scoped>
.hello-plugin-page {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  color: var(--text-primary);
}

.hello-plugin-note {
  margin: 12px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.hello-plugin-retry {
  margin-top: 8px;
}
</style>
