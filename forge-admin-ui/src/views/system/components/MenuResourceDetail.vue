<template>
  <!-- 按需详情，不挤占资源列表宽度；所有写操作沿用页面原处理函数。 -->
  <NDrawer v-model:show="workspace.detailVisible" width="min(420px, 100vw)" placement="right">
    <NDrawerContent :title="resource?.resourceName || '资源详情'" closable>
      <template v-if="resource">
        <div class="resource-identity">
          <IconRenderer v-if="icon" :icon="icon" :font-size="18" />
          <span>{{ typeLabel }} · {{ clientLabel }}</span>
          <DictTag :options="visibleOptions" :value="resource.visible" size="small" :bordered="false" force-tag />
        </div>
        <NDescriptions :column="1" label-placement="left" size="small" bordered>
          <NDescriptionsItem label="排序">
            {{ resource.sort ?? 0 }}
          </NDescriptionsItem>
          <NDescriptionsItem label="路由">
            {{ resource.path || '-' }}
          </NDescriptionsItem>
          <NDescriptionsItem label="组件">
            {{ resource.component || '-' }}
          </NDescriptionsItem>
          <NDescriptionsItem label="权限标识">
            {{ resource.perms || '-' }}
          </NDescriptionsItem>
          <NDescriptionsItem label="API">
            {{ resource.apiMethod || '-' }} {{ resource.apiUrl || '' }}
          </NDescriptionsItem>
          <NDescriptionsItem v-if="resource.remark" label="备注">
            {{ resource.remark }}
          </NDescriptionsItem>
        </NDescriptions>
        <div class="child-summary">
          <span>目录/菜单 {{ childSummary.menu }}</span>
          <span>按钮 {{ childSummary.button }}</span>
          <span>API {{ childSummary.api }}</span>
        </div>
        <NButton v-if="[1, 2].includes(Number(resource.resourceType))" text type="primary" @click="emit('enter', resource)">
          查看下级资源 <i class="i-lucide:arrow-right" />
        </NButton>
      </template>
      <template #footer>
        <div v-if="resource" class="detail-actions">
          <NDropdown :options="moreOptions" trigger="click" @select="key => emit(key, resource)">
            <NButton size="small">
              更多操作 <i class="i-lucide:chevron-down" />
            </NButton>
          </NDropdown>
          <NButton type="primary" size="small" @click="emit('edit', resource)">
            编辑资源
          </NButton>
        </div>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<script setup>
import { NButton, NDescriptions, NDescriptionsItem, NDrawer, NDrawerContent, NDropdown } from 'naive-ui'
import DictTag from '@/components/DictTag.vue'
import IconRenderer from '@/components/IconRenderer.vue'
import { useMenuWorkspaceStore } from '@/stores/system/menuWorkspaceStore'

defineProps({
  resource: { type: Object, default: null },
  icon: { type: String, default: '' },
  typeLabel: { type: String, default: '' },
  clientLabel: { type: String, default: '' },
  visibleOptions: { type: Array, default: () => [] },
  childSummary: { type: Object, default: () => ({ menu: 0, button: 0, api: 0 }) },
})
const emit = defineEmits(['edit', 'add', 'delete', 'enter'])
const workspace = useMenuWorkspaceStore()
const moreOptions = [{ key: 'add', label: '新增子项' }, { key: 'delete', label: '删除资源' }]
</script>

<style scoped>
.resource-identity,
.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.resource-identity {
  flex-wrap: wrap;
  margin-bottom: 16px;
  color: var(--text-secondary);
  font-size: 12px;
}
.detail-actions {
  justify-content: flex-end;
}
.child-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-block: 16px;
  color: var(--text-tertiary);
  font-size: 12px;
}
:deep(.n-descriptions-table-content) {
  overflow-wrap: anywhere;
}
</style>
