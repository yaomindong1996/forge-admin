<template>
  <n-modal
    :show="show"
    preset="card"
    :title="dialogTitle"
    style="width: 520px"
    :mask-closable="false"
    @update:show="emit('update:show', $event)"
  >
    <div class="page-system-menu-mount-dialog">
      <p class="page-system-menu-mount-dialog__desc">
        选择此页面在系统菜单中出现的位置，必须显式指定父级目录；不会默认挂到「AI应用」或通用业务域。配置写入应用草稿，应用发布后生效。
      </p>
      <dl class="page-system-menu-mount-dialog__meta">
        <div>
          <dt>当前页面</dt>
          <dd>{{ pageTitle || '未命名页面' }}</dd>
        </div>
        <div>
          <dt>挂载状态</dt>
          <dd>{{ mounted ? '已挂载（可修改或取消）' : '未挂载' }}</dd>
        </div>
      </dl>
      <PageSystemMenuMountForm v-model="draft" :hint="''" />
    </div>
    <template #footer>
      <n-space justify="space-between" style="width: 100%">
        <n-button v-if="mounted" quaternary type="error" :loading="saving" @click="handleUnmount">
          取消挂载
        </n-button>
        <span v-else />
        <n-space>
          <n-button :disabled="saving" @click="emit('update:show', false)">
            关闭
          </n-button>
          <n-button type="primary" :loading="saving" @click="handleConfirm">
            {{ mounted ? '保存挂载' : '确认挂载' }}
          </n-button>
        </n-space>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import PageSystemMenuMountForm from './PageSystemMenuMountForm.vue'
import {
  createPageMountDraft,
  isPageSystemMenuMounted,
  showAdminMenuMountConfig,
  showMobileMenuMountConfig,
} from './page-system-menu-mount'

const props = defineProps({
  show: { type: Boolean, default: false },
  node: { type: Object, default: null },
  pageTitle: { type: String, default: '' },
  saving: { type: Boolean, default: false },
})

const emit = defineEmits(['update:show', 'confirm', 'unmount'])
const message = useMessage()
const draft = ref(createPageMountDraft())

const mounted = computed(() => isPageSystemMenuMounted(props.node))
const dialogTitle = computed(() => (mounted.value ? '修改系统菜单挂载' : '挂载系统菜单'))

watch(
  () => [props.show, props.node],
  ([visible]) => {
    if (!visible)
      return
    draft.value = createPageMountDraft(props.node)
  },
  { immediate: true },
)

function validateDraft(next) {
  const target = String(next.mountTarget || 'ADMIN').toUpperCase()
  if (showAdminMenuMountConfig(target) && !next.menuParentId) {
    message.warning('请选择管理端父级菜单')
    return false
  }
  if (showMobileMenuMountConfig(target) && !next.mobileMenuParentId) {
    message.warning('请选择移动端父级菜单')
    return false
  }
  return true
}

function handleConfirm() {
  const next = {
    ...draft.value,
    systemMenuVisible: true,
    mountTarget: String(draft.value.mountTarget || 'ADMIN').toUpperCase(),
    menuSort: Number(draft.value.menuSort) || 0,
  }
  if (!validateDraft(next))
    return
  emit('confirm', next)
}

function handleUnmount() {
  emit('unmount')
}
</script>

<style scoped>
.page-system-menu-mount-dialog {
  display: grid;
  gap: 16px;
}

.page-system-menu-mount-dialog__desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.page-system-menu-mount-dialog__meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--bg-secondary);
}

.page-system-menu-mount-dialog__meta dt {
  color: var(--text-tertiary);
  font-size: 12px;
}

.page-system-menu-mount-dialog__meta dd {
  margin: 4px 0 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}
</style>
