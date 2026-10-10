<template>
  <section class="plugin-license" aria-label="当前插件授权状态">
    <header>
      <h3>授权状态</h3>
      <NButton v-if="store.canQuery" secondary size="small" :loading="store.loading" @click="store.load">
        <template #icon>
          <i class="i-lucide:refresh-cw" />
        </template>
        刷新状态
      </NButton>
    </header>
    <!-- 未获权或请求失败时不沿用上一次的授权结果 -->
    <NAlert v-if="!store.canQuery" type="info" :bordered="false">
      当前账号无权查看实例授权，请联系系统管理员核对。
    </NAlert>
    <NSkeleton v-else-if="store.loading" text :repeat="5" />
    <NAlert v-else-if="store.error" type="error" title="授权状态加载失败" :bordered="false">
      {{ store.error }}
      <NButton text type="primary" @click="store.load">
        重新加载
      </NButton>
    </NAlert>
    <template v-else-if="store.data">
      <div class="plugin-license__mode">
        <span>当前授权模式</span>
        <DictTag :options="dict.sys_runtime_license_mode" :value="store.data.mode" />
      </div>
      <NAlert v-if="store.data.mode === 'community'" type="info" :bordered="false">
        当前使用社区授权机制，未加载商业许可证。基础功能以项目发行内容为准，商业功能不会因此自动授权。
      </NAlert>
      <NAlert v-else-if="store.data.mode === 'custom'" type="info" :bordered="false">
        当前项目使用自定义授权机制，无法在此显示签名许可证期限。请联系项目管理员确认。
      </NAlert>
      <NAlert v-else-if="!store.data.report" type="warning" :bordered="false">
        当前服务未提供可查询的许可证状态，暂时无法确认此插件的授权。
      </NAlert>
      <template v-else>
        <NAlert v-if="view.configurationInvalid || view.hasUnattributedIssue" type="warning" :bordered="false">
          实例中存在未通过校验的授权配置或文件，无法确认其插件归属，请联系管理员处理。
        </NAlert>
        <NEmpty v-if="!view.entries.length" description="未查询到此插件的匹配许可证" size="small" />
        <article v-for="entry in view.entries" :key="entry.position" class="plugin-license__entry">
          <div class="plugin-license__entry-title">
            <span>{{ plugin.name || plugin.id }}</span>
            <DictTag :options="dict.sys_runtime_license_state" :value="entry.state" />
          </div>
          <dl>
            <dt>使用期限</dt><dd>{{ licenseUsePeriod(entry.terms) }}</dd>
            <dt>维护截止</dt><dd>{{ licenseTime(entry.terms?.maintenanceUntil) }}</dd>
            <dt>授权范围</dt><dd>{{ plugin.name || plugin.id }}</dd>
          </dl>
          <div v-if="entry.features.length" class="plugin-license__features">
            <span>本插件授权功能</span>
            <code v-for="code in entry.features" :key="code">{{ code }}</code>
          </div>
        </article>
        <p class="plugin-license__note">
          检查时间：{{ licenseTime(store.data.report.checkedAt) }}
        </p>
      </template>
      <p class="plugin-license__note">
        此处仅展示当前实例的运行授权，不代表源码下载权益。修改授权文件后需重启服务；授权不替代菜单和操作权限。
      </p>
    </template>
  </section>
</template>

<script setup>
import { NAlert, NButton, NEmpty, NSkeleton } from 'naive-ui'
import { computed, onBeforeUnmount, onMounted } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useRuntimeLicenseStore } from '@/stores/plugin/runtimeLicenseStore'
import { pluginLicenseView } from '../pluginLicenseView'
import { licenseTime, licenseUsePeriod } from '../runtimeLicenseUtils'

const props = defineProps({ plugin: { type: Object, required: true } })
const store = useRuntimeLicenseStore()
const { dict } = useDict('sys_runtime_license_mode', 'sys_runtime_license_state')
const view = computed(() => pluginLicenseView(store.data, props.plugin))
onMounted(store.load)
onBeforeUnmount(store.clear)
</script>

<style scoped>
.plugin-license {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.plugin-license header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.plugin-license h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}
.plugin-license__mode,
.plugin-license__entry-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.plugin-license__mode {
  color: var(--text-tertiary);
  font-size: 12px;
}
.plugin-license__entry {
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
}
.plugin-license__entry-title {
  justify-content: space-between;
  font-size: 14px;
}
.plugin-license dl {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  font-size: 13px;
}
.plugin-license dt {
  color: var(--text-tertiary);
}
.plugin-license dd {
  margin: 0;
  overflow-wrap: anywhere;
}
.plugin-license__features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  overflow-wrap: anywhere;
}
.plugin-license__features > span {
  color: var(--text-tertiary);
}
.plugin-license__note {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.7;
}
</style>
