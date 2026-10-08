<template>
  <section class="license-status">
    <!-- 当前实例只读查询 -->
    <header>
      <div>
        <h3>运行时授权</h3>
        <p>只读诊断当前后端，不代表源码下载权益</p>
      </div>
      <NButton v-if="store.canQuery" :loading="store.loading" @click="store.load">
        <template #icon>
          <i class="i-lucide:refresh-cw" />
        </template>
        刷新状态
      </NButton>
    </header>
    <!-- 无权限、加载与错误各自独立，不显示上一份敏感数据 -->
    <NAlert v-if="!store.canQuery" type="warning">
      只有平台超级管理员且具备插件查询权限才能查看当前实例的授权状态。
    </NAlert>
    <NAlert v-else-if="store.error" type="error" title="加载运行时授权失败">
      {{ store.error }}
    </NAlert>
    <NSkeleton v-if="store.loading" text :repeat="4" />
    <template v-else-if="store.canQuery && store.data">
      <NSpace align="center">
        <span>授权模式</span>
        <DictTag :value="store.data.mode" :options="dict.sys_runtime_license_mode" />
        <span>当前发行版</span>
        <DictTag :value="store.data.edition" :options="dict.sys_plugin_edition" />
      </NSpace>
      <!-- 自定义 Gate 不能伪装为文件验签成功，也不读取授权文件 -->
      <NAlert v-if="!store.data.report" type="info">
        当前实例未使用文件许可证验证组件。
        社区模式不放行商业功能；自定义模式请核对自己的授权实现。
      </NAlert>
      <template v-else>
        <RuntimeLicenseConfiguration :configuration="store.data.report.configuration" />
        <!-- 按启动时配置的文件顺序诊断，不公开文件名或路径 -->
        <NDataTable
          :columns="columns" :data="store.data.report.entries" :row-key="row => row.position"
          :bordered="false" :scroll-x="820" size="medium"
        >
          <template #empty>
            <NEmpty description="当前启动快照没有已加载的授权文件" />
          </template>
        </NDataTable>
        <p>状态检查时间：{{ licenseTime(store.data.report.checkedAt) }}（本地时间）</p>
      </template>
    </template>
    <!-- 固定边界与排查说明，不增加在线开通或本机文件写入入口 -->
    <p class="license-status__note">
      文件序号对应 forge.license.files 的配置顺序。
      无效或绑定不符的文件不展示其声明信息；
      “文件无效”请在服务端核对文件、公钥、格式及签名，
      路径和异常详情不会回传到浏览器。
      刷新仅检查当前期限；修改文件、公钥或绑定后需要重启。维护到期不停止永久使用，
      网站撤销也不能即时使已交付的离线文件失效。许可证不替代 RBAC 与租户权限。
    </p>
  </section>
</template>

<script setup>
import { NAlert, NButton, NDataTable, NEmpty, NSkeleton, NSpace } from 'naive-ui'
import { computed, h, onBeforeUnmount, onMounted } from 'vue'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { useRuntimeLicenseStore } from '@/stores/plugin/runtimeLicenseStore'
import { licenseTime, licenseUsePeriod } from '../runtimeLicenseUtils'
import RuntimeLicenseConfiguration from './RuntimeLicenseConfiguration.vue'
import RuntimeLicenseScope from './RuntimeLicenseScope.vue'

const store = useRuntimeLicenseStore()
const { dict } = useDict('sys_runtime_license_mode', 'sys_runtime_license_state', 'sys_plugin_edition')
const columns = computed(() => [
  { title: '授权文件', key: 'position', minWidth: 210, render: row => h(SystemTableCell, {
    title: `配置文件 ${row.position}`,
    subtitle: row.licenseId || '未展示声明信息',
  }) },
  { title: '状态', key: 'state', width: 130, render: row =>
    h(DictTag, { value: row.state, options: dict.value.sys_runtime_license_state }) },
  {
    title: '精确授权范围',
    key: 'scope',
    minWidth: 180,
    render: row => h(RuntimeLicenseScope, { scope: row.scope }),
  },
  { title: '使用期限', key: 'terms', minWidth: 240, render: row => licenseUsePeriod(row.terms) },
  { title: '维护截止', key: 'maintenance', width: 170, render: row => licenseTime(row.terms?.maintenanceUntil) },
])
onMounted(store.load)
onBeforeUnmount(store.clear)
</script>

<style scoped>
.license-status {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 0;
}
.license-status header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.license-status h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}
.license-status p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-tertiary);
}
.license-status__note {
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
</style>
