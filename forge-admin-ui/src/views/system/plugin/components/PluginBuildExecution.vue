<template>
  <section v-if="execution" class="plugin-execution">
    <!-- 实际执行记录，不把租约/构建报告冒充安装或部署 -->
    <div class="plugin-execution__header">
      <span>构建执行记录</span>
      <NButton size="small" :disabled="busy" @click="emit('refresh')">
        刷新执行状态
      </NButton>
    </div>
    <NAlert v-if="execution.leaseExpired" type="warning" title="执行器租约已到期">
      已停止接收续期和结果，插件占用仍保留。请核查执行器及本次容器；系统不会自动抢占或重试。
    </NAlert>
    <NDescriptions :column="1" bordered size="small" label-placement="left">
      <NDescriptionsItem label="执行器">
        {{ execution.workerId }}
      </NDescriptionsItem>
      <NDescriptionsItem label="当前阶段">
        <DictTag :options="dict.sys_plugin_build_phase" :value="execution.phase" />
      </NDescriptionsItem>
      <NDescriptionsItem label="源码提交">
        <code>{{ execution.sourceCommit }}</code>
      </NDescriptionsItem>
      <NDescriptionsItem label="构建镜像">
        <code>{{ execution.image }}</code>
      </NDescriptionsItem>
      <NDescriptionsItem label="开始 / 结束">
        {{ pluginTime(execution.startedTime) }} / {{ pluginTime(execution.finishedTime) }}
      </NDescriptionsItem>
      <NDescriptionsItem v-if="!execution.finishedTime" label="租约 / 硬期限">
        {{ pluginTime(execution.leaseExpiresTime) }} / {{ pluginTime(execution.deadlineTime) }}
      </NDescriptionsItem>
      <NDescriptionsItem v-if="execution.result?.failureCode" label="失败代码">
        {{ execution.result.failureCode }}
      </NDescriptionsItem>
      <NDescriptionsItem v-if="execution.result?.sourceSha256" label="源码快照 SHA-256">
        <code>{{ execution.result.sourceSha256 }}</code>
      </NDescriptionsItem>
      <NDescriptionsItem v-if="execution.result?.success" label="产物摘要">
        {{ execution.result.artifactCount }} 个文件 ·
        {{ (execution.result.artifactBytes / 1024 / 1024).toFixed(1) }} MiB
        <div><code>{{ execution.result.artifactManifestSha256 }}</code></div>
      </NDescriptionsItem>
    </NDescriptions>
    <p>以上是独立执行器报告。产物保留在私有工作区，尚未部署，也未完成运行健康或授权核验。</p>
  </section>
</template>

<script setup>
import { NAlert, NButton, NDescriptions, NDescriptionsItem } from 'naive-ui'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { pluginTime } from '../pluginTaskUtils'

defineProps({
  execution: { type: Object, default: null },
  busy: Boolean,
})
const emit = defineEmits(['refresh'])
const { dict } = useDict('sys_plugin_build_phase')
</script>

<style scoped>
.plugin-execution {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.plugin-execution__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.plugin-execution code {
  overflow-wrap: anywhere;
}
.plugin-execution p {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
}
</style>
