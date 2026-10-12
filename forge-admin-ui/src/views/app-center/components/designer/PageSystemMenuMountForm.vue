<template>
  <div class="page-system-menu-mount-form">
    <n-form label-placement="left" :label-width="labelWidth" size="small">
      <n-form-item label="挂载位置">
        <n-radio-group :value="draft.mountTarget" size="small" @update:value="patch({ mountTarget: $event })">
          <n-radio-button value="ADMIN">
            管理端
          </n-radio-button>
          <n-radio-button value="MOBILE">
            移动端
          </n-radio-button>
          <n-radio-button value="BOTH">
            两端同时
          </n-radio-button>
        </n-radio-group>
      </n-form-item>
      <n-form-item label="菜单名称">
        <n-input
          :value="draft.menuName"
          placeholder="留空则使用页面名称"
          size="small"
          @update:value="patch({ menuName: $event })"
        />
      </n-form-item>
      <n-form-item v-if="showAdmin" label="管理端父级">
        <MenuParentSelect
          :value="draft.menuParentId"
          client-code="pc"
          @update:value="patch({ menuParentId: $event })"
        />
      </n-form-item>
      <n-form-item v-if="showMobile" label="移动端父级">
        <MenuParentSelect
          :value="draft.mobileMenuParentId"
          client-code="h5"
          placeholder="请选择移动端菜单目录"
          @update:value="patch({ mobileMenuParentId: $event })"
        />
      </n-form-item>
      <n-form-item v-if="showAdmin || showMobile" label="菜单排序">
        <n-input-number
          :value="draft.menuSort"
          :min="0"
          size="small"
          style="width: 100%;"
          @update:value="patch({ menuSort: $event ?? 0 })"
        />
      </n-form-item>
    </n-form>
    <p v-if="hint" class="page-system-menu-mount-form__hint">
      {{ hint }}
    </p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import MenuParentSelect from '@/components/lowcode-builder/shared/MenuParentSelect.vue'
import {
  createPageMountDraft,
  showAdminMenuMountConfig,
  showMobileMenuMountConfig,
} from './page-system-menu-mount'

const props = defineProps({
  /** 当前挂载草稿（v-model） */
  modelValue: {
    type: Object,
    default: () => createPageMountDraft(),
  },
  labelWidth: {
    type: [Number, String],
    default: 92,
  },
  hint: {
    type: String,
    default: '选择此页面在菜单中出现的位置，应用发布后生效。',
  },
})

const emit = defineEmits(['update:modelValue'])

const draft = computed(() => ({
  ...createPageMountDraft(),
  ...(props.modelValue || {}),
}))

const showAdmin = computed(() => showAdminMenuMountConfig(draft.value.mountTarget))
const showMobile = computed(() => showMobileMenuMountConfig(draft.value.mountTarget))

function patch(partial = {}) {
  emit('update:modelValue', {
    ...draft.value,
    ...partial,
  })
}
</script>

<style scoped>
.page-system-menu-mount-form__hint {
  margin: 8px 0 0;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
