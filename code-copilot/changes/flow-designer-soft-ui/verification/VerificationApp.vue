<script setup>
import { computed, onMounted, ref } from 'vue'
import { darkTheme } from 'naive-ui'
import DingFlowDesigner from '@/components/flow-designer/DingFlowDesigner.vue'
import FlowDesignToolbar from '@/views/flow/components/FlowDesignToolbar.vue'
import { buildNode } from '@/components/flow-designer/constants/default-configs.js'
import { approvalMergeFixture } from '@/components/flow-designer/utils/__tests__/approval-merge-fixture.js'

const designer = ref(null)
const dark = ref(false)
const mode = ref('design')
const name = ref('费用报销流程')
const notice = ref('本地组件验收 · 示例流程 · 不连接业务接口')
const fields = [{ field: 'amount', label: '报销金额', dataType: 'number' }]
const theme = computed(() => dark.value ? darkTheme : null)
const themeOverrides = computed(() => ({ common: {
  primaryColor: dark.value ? '#79a2f5' : '#356de0',
  primaryColorHover: dark.value ? '#9cbaff' : '#5b8def',
  primaryColorPressed: dark.value ? '#6490e4' : '#2f5fc4',
  primaryColorSuppl: dark.value ? '#9cbaff' : '#5b8def',
  borderRadius: '6px',
} }))
const snapshot = ref('')
function captureXml() {
  snapshot.value = designer.value.getXML()
  notice.value = `本地 XML 已生成（${snapshot.value.length} 字符），未提交服务器`
}
function loadMergeFixture() {
  name.value = '条件支路汇合示例'
  const flow = approvalMergeFixture()
  flow.processName = name.value
  designer.value.designer.loadJson(flow)
  notice.value = '合并节点本地验收 · 合成数据 · 不连接业务接口'
}
onMounted(() => {
  const nodes = [
    buildNode('start', { id: 'S', name: '发起人' }),
    buildNode('approver', { id: 'A', name: '部门主管审批', config: { assignee: '$' + '{deptManager}' } }),
    buildNode('condition', { id: 'G', name: '金额条件', config: { defaultFlowId: 'F4' } }),
    buildNode('approver', { id: 'L', name: '财务负责人', config: {
      taskType: 'candidateGroups', candidateGroups: ['finance_lead'], candidateGroupNames: ['财务负责人'],
    } }),
    buildNode('approver', { id: 'R', name: '财务专员', config: {
      taskType: 'candidateGroups', candidateGroups: ['finance'], candidateGroupNames: ['财务专员'],
    } }),
    buildNode('carbonCopy', { id: 'C', name: '抄送', config: {
      candidateGroups: ['finance'], candidateGroupNames: ['财务组'], mergeNode: true,
    } }),
    buildNode('end', { id: 'E', name: '流程结束' }),
  ]
  const edges = [
    { id: 'F1', source: 'S', target: 'A' }, { id: 'F2', source: 'A', target: 'G' },
    { id: 'F3', source: 'G', target: 'L', branchId: 'left', condition: '$' + '{amount >= 5000}' },
    { id: 'F4', source: 'G', target: 'R', branchId: 'right', isDefault: true },
    { id: 'F5', source: 'L', target: 'C' }, { id: 'F6', source: 'R', target: 'C' },
    { id: 'F7', source: 'C', target: 'E' },
  ]
  designer.value.designer.loadJson({ processId: 'LocalFixture', processName: name.value, nodes, edges, config: {} })
})
</script>

<template>
  <n-config-provider :theme="theme" :theme-overrides="themeOverrides">
    <div class="verification-app" :class="{ dark }">
      <FlowDesignToolbar
        :name="name" :status="{ label: '示例草稿', type: 'default' }" :mode="mode"
        @mode="mode = $event" @save="captureXml" @deploy="notice = '隔离验收页不发布真实流程'"
        @history="notice = '历史入口已触发；本页不请求真实历史'" @ai="notice = 'AI入口已触发；本页不调用真实模型'"
        @back="notice = '返回入口已触发'"
      />
      <main>
        <DingFlowDesigner v-show="mode === 'design'" ref="designer" :process-name="name" :form-field-catalog="fields" />
        <div v-if="mode === 'settings'" class="fixture-settings">更多设置入口已切换（本地组件验收）</div>
      </main>
      <footer class="fixture-notice">
        <span>{{ notice }}</span>
        <div class="fixture-actions">
          <button type="button" @click="loadMergeFixture">合并示例</button>
          <button type="button" @click="dark = !dark">切换{{ dark ? '浅色' : '深色' }}</button>
        </div>
      </footer>
    </div>
  </n-config-provider>
</template>

<style>
body { margin: 0; font-family: system-ui, -apple-system, 'PingFang SC', sans-serif; }
/* 系统主题变量实际在全局，传送到 body 的 Modal/下拉也应继承，不能只给示例容器赋值。 */
body {
  --bg-primary: #fff;
  --bg-secondary: #f6f8fb;
  --bg-tertiary: #f2f4f7;
  --text-primary: #1d2129;
  --text-secondary: #4e5969;
  --text-tertiary: #86909c;
  --border-light: #dfe4ec;
  --primary-color: #356de0;
}
.verification-app { --bg-primary: #fff; --bg-secondary: #f6f8fb; --bg-tertiary: #f2f4f7; --text-primary: #1d2129; --text-secondary: #4e5969; --text-tertiary: #86909c; --border-light: #dfe4ec; --primary-color: #356de0; height: 100dvh; display: flex; flex-direction: column; }
.verification-app.dark, body:has(.verification-app.dark) { --bg-primary: #20252e; --bg-secondary: #171c24; --bg-tertiary: #2a303b; --text-primary: #e5e9f0; --text-secondary: #b2bbc9; --text-tertiary: #8d99aa; --border-light: #343e4b; --primary-color: #79a2f5; }
.verification-app main { position: relative; flex: 1; min-height: 0; overflow: hidden; }
.fixture-notice { display: flex; flex: none; justify-content: space-between; gap: 12px; padding: 6px 12px; background: var(--bg-primary); color: var(--text-tertiary); font-size: 11px; border-top: 1px solid var(--border-light); }
.fixture-notice button { color: var(--primary-color); background: none; border: 0; cursor: pointer; }
.fixture-notice > span { min-width: 0; flex: 1; }
.fixture-actions { display: flex; flex: none; gap: 12px; align-items: center; }
.fixture-settings { padding: 24px; }
</style>
