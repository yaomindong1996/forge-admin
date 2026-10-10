<template>
  <section class="plugin-installation" aria-label="安装说明">
    <h3>{{ builtin ? '随项目提供，无需重复安装' : '源码交付与安装' }}</h3>
    <p>
      {{ builtin
        ? '当前服务已经包含该内置模块。首次使用前，按实际业务配置菜单权限和对应服务。'
        : '外部插件通过源码交付。安装及升级需要在开发工程中完成，浏览器不会直接加载新代码。' }}
    </p>
    <!-- 说明不是执行器：不提供即时安装或后台发布入口 -->
    <NTimeline>
      <template v-if="builtin">
        <NTimelineItem title="核对当前版本">
          当前模块版本：{{ plugin.version || '未提供' }}。升级请使用与宿主工程兼容的发行版本。
        </NTimelineItem>
        <NTimelineItem title="完成业务配置">
          按使用说明配置模块需要的服务连接和参数，再由管理员分配菜单与操作权限。
        </NTimelineItem>
        <NTimelineItem title="升级后重新核验">
          备份配置与数据，完成构建和部署后返回本页刷新。模块已加载不等于业务健康检查通过。
        </NTimelineItem>
      </template>
      <template v-else>
        <NTimelineItem title="取得匹配的源码版本">
          从插件市场或发布方获取交付包，阅读包内 README、版本兼容要求与授权说明。
        </NTimelineItem>
        <NTimelineItem title="在开发工程中预检">
          保留本地改动与数据备份，按源码插件文档核对依赖、目标文件和数据库迁移。
        </NTimelineItem>
        <NTimelineItem title="安装源码并构建验证">
          在独立开发环境完成源码集成及测试；存在定制冲突时先人工处理，不直接覆盖。
        </NTimelineItem>
        <NTimelineItem title="部署并检查授权">
          完成审批部署后刷新当前清单。需要许可证的插件应按客户与项目绑定配置有效授权。
        </NTimelineItem>
      </template>
    </NTimeline>
    <NButton v-if="pluginGuideUrl" tag="a" :href="pluginGuideUrl" target="_blank" rel="noopener noreferrer">
      <template #icon>
        <i class="i-lucide:book-open" />
      </template>
      查看完整插件文档
    </NButton>
  </section>
</template>

<script setup>
import { NButton, NTimeline, NTimelineItem } from 'naive-ui'
import { computed } from 'vue'
import { pluginGuideUrl } from '../pluginLinks'

const props = defineProps({ plugin: { type: Object, required: true } })
const builtin = computed(() => props.plugin.origin === 'builtin')
</script>

<style scoped>
.plugin-installation {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;
}
.plugin-installation h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}
.plugin-installation p {
  margin: -8px 0 0;
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 1.8;
}
.plugin-installation :deep(.n-timeline) {
  width: 100%;
}
</style>
