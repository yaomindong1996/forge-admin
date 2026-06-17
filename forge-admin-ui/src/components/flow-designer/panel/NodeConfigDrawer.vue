<script setup>
/**
 * NodeConfigDrawer — 节点配置抽屉容器
 *
 * 职责：
 * - 右侧滑出抽屉（Naive UI NDrawer）
 * - 顶部：节点类型 icon + name 输入 + 关闭按钮
 * - 中部：根据 nodeType 调度对应配置 Tab（通过 panel/index 注册的 RENDERER_MAP）
 * - 底部：取消 / 保存按钮（保存时触发 emit('save', patch)）
 *
 * Props:
 *   - visible        v-model:visible
 *   - node           当前编辑节点（外层 useFlowDesigner.getNode 取）
 *   - readonly       只读模式（查看器场景）
 *   - width          抽屉宽度，默认 480
 *
 * Events:
 *   - update:visible (boolean)
 *   - save           (patch: object)  外层 useFlowDesigner.updateNode(node.id, patch)
 *
 * 实现策略：
 * - 内部维护 draftNode（深拷贝 props.node），用户修改不直接影响外层；点击 "保存" 才 emit
 * - 取消按钮 / 关闭按钮：丢弃 draft，emit('update:visible', false)
 * - 节点 name / id 编辑通过 BasicConfig 组件（在每个配置 Tab 之上始终显示）
 */
import { computed, ref, watch } from 'vue'
import BasicConfig from '../panel/BasicConfig.vue'
import { CONFIG_RENDERER_MAP } from '../panel/config-renderer-map.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null },
  readonly: { type: Boolean, default: false },
  width: { type: Number, default: 480 },
})

const emit = defineEmits(['update:visible', 'save'])

const draftNode = ref(null)

watch(
  () => [props.visible, props.node],
  ([v, n]) => {
    if (v && n)
      draftNode.value = cloneDeep(n)
    else if (!v)
      draftNode.value = null
  },
  { immediate: true },
)

const ConfigComponent = computed(() => {
  if (!draftNode.value)
    return null
  return CONFIG_RENDERER_MAP[draftNode.value.nodeType] || null
})

const headerIcon = computed(() => {
  const map = {
    start: 'i-mdi-flag-variant-outline',
    end: 'i-mdi-flag-checkered',
    approver: 'i-mdi-account-check',
    carbonCopy: 'i-mdi-email-outline',
    condition: 'i-mdi-source-branch',
    parallel: 'i-mdi-call-split',
    inclusive: 'i-mdi-set-merge',
    service: 'i-mdi-cog-outline',
    script: 'i-mdi-code-tags',
    subProcess: 'i-mdi-sitemap-outline',
    callActivity: 'i-mdi-phone-forward-outline',
    advanced: 'i-mdi-shield-alert-outline',
  }
  return map[draftNode.value?.nodeType] || 'i-mdi-square-outline'
})

function handleClose() {
  emit('update:visible', false)
}

function handleSave() {
  if (!draftNode.value)
    return
  const patch = {
    name: draftNode.value.name,
    config: draftNode.value.config,
  }
  emit('save', patch, draftNode.value.id)
  emit('update:visible', false)
}

function updateConfig(partial) {
  if (!draftNode.value)
    return
  draftNode.value.config = { ...draftNode.value.config, ...partial }
}

function cloneDeep(v) {
  return JSON.parse(JSON.stringify(v))
}
</script>

<template>
  <n-drawer
    :show="visible"
    :width="width"
    placement="right"
    :mask-closable="!readonly"
    @update:show="$emit('update:visible', $event)"
  >
    <n-drawer-content :native-scrollbar="false" closable>
      <template #header>
        <div class="flex items-center gap-2">
          <i :class="headerIcon" class="text-base text-primary" />
          <span>{{ readonly ? '查看节点' : '配置节点' }}</span>
          <span v-if="draftNode" class="text-xs text-gray-400">{{ draftNode.id }}</span>
        </div>
      </template>

      <div v-if="draftNode" class="space-y-4">
        <BasicConfig v-model:node="draftNode" :readonly="readonly" />
        <component
          :is="ConfigComponent"
          v-if="ConfigComponent"
          :node="draftNode"
          :readonly="readonly"
          @update:config="updateConfig"
        />
        <div v-else class="text-sm rounded bg-gray-50 p-4 text-gray-400">
          该节点类型暂无可配置项。
        </div>
      </div>
      <div v-else class="text-sm text-gray-400">
        请先选择一个节点。
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button @click="handleClose">
            {{ readonly ? '关闭' : '取消' }}
          </n-button>
          <n-button v-if="!readonly" type="primary" @click="handleSave">
            保存
          </n-button>
        </div>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
