<template>
  <div class="license-configuration">
    <!-- 配置问题只展示固定说明，不返回非法配置原文 -->
    <NAlert v-if="!configuration.binding" type="warning" title="客户与项目绑定配置无效">
      请核对 forge.license 的数字客户 ID 和固定小写项目 UUID；
      不要在每次启动时重新生成标识。
    </NAlert>
    <NAlert v-if="!configuration.filesConfigurationValid" type="warning" title="授权文件配置未加载">
      请先修复绑定和文件列表配置，再重启服务。文件列表最多 100 份。
    </NAlert>
    <NAlert v-if="!configuration.trustedPublicKeys" type="warning" title="没有已加载的可信公钥">
      请通过可信渠道获取公钥并在服务端配置，
      不能使用许可证文件中未受信任的下载地址。
    </NAlert>
    <!-- 当前实例的合法绑定与启动快照 -->
    <NDescriptions bordered size="small" :column="1" label-placement="left">
      <NDescriptionsItem label="客户账号 ID">
        {{ configuration.binding?.customerId || '—' }}
      </NDescriptionsItem>
      <NDescriptionsItem label="项目安装标识">
        <span class="license-configuration__id">{{ configuration.binding?.installationId || '—' }}</span>
      </NDescriptionsItem>
      <NDescriptionsItem label="公钥加载">
        {{ configuration.trustedPublicKeys }} 份可用 · {{ configuration.rejectedPublicKeys }} 份未加载
      </NDescriptionsItem>
      <NDescriptionsItem label="启动加载时间">
        {{ licenseTime(configuration.loadedAt) }}
      </NDescriptionsItem>
    </NDescriptions>
  </div>
</template>

<script setup>
import { NAlert, NDescriptions, NDescriptionsItem } from 'naive-ui'
import { licenseTime } from '../runtimeLicenseUtils'

defineProps({ configuration: { type: Object, required: true } })
</script>

<style scoped>
.license-configuration {
  display: grid;
  gap: 12px;
  min-width: 0;
}
.license-configuration__id {
  overflow-wrap: anywhere;
}
</style>
