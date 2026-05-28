<template>
  <div class="center-shell">
    <header>
      <n-button text @click="router.push('/app-center')">
        返回应用中心
      </n-button>
      <h1>引擎中心</h1>
      <p>流程、审批、报表、权限、消息、触发器和导入导出能力统一挂接到业务对象或应用入口。</p>
    </header>
    <div class="capability-grid">
      <section v-for="item in engines" :key="item.name" class="capability-card">
        <n-icon :component="item.icon" />
        <strong>{{ item.name }}</strong>
        <p>{{ item.desc }}</p>
        <div class="engine-meta">
          <span>{{ item.status }}</span>
          <span>{{ bindingCount(item.types) }} 个接入</span>
          <span v-if="latestBindingUpdate(item.types)">最近 {{ latestBindingUpdate(item.types) }}</span>
        </div>
        <n-button text type="primary" :disabled="!resolveEngineEntry(item)" @click="openEngine(item)">
          {{ item.action }}
        </n-button>
      </section>
    </div>
  </div>
</template>

<script setup>
import {
  BarChartOutline,
  CheckmarkDoneOutline,
  FlashOutline,
  GitBranchOutline,
  KeyOutline,
  MailOutline,
  SwapVerticalOutline,
} from '@vicons/ionicons5'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { businessBindingList } from '@/api/business-app'

const router = useRouter()
const bindings = ref([])

const engines = [
  { name: '流程引擎', desc: '业务流转、任务分派和流程监控入口。', status: '已接入底座', types: ['FLOW'], entry: '/flow/model', action: '流程配置', icon: GitBranchOutline },
  { name: '审批引擎', desc: '合同审批、折扣审批和回款审批挂接。', status: '配置入口', types: ['APPROVAL'], entry: '/flow/template', action: '发起/配置', icon: CheckmarkDoneOutline },
  { name: '报表引擎', desc: '销售看板、客户分析和回款分析。', status: '配置入口', types: ['REPORT'], entry: '/report/design', action: '报表配置', icon: BarChartOutline },
  { name: '权限引擎', desc: '区域可见、部门可见和负责人可见。', status: '已接入底座', types: ['PERMISSION'], entry: '/system/role', action: '权限配置', icon: KeyOutline },
  { name: '消息引擎', desc: '到期提醒、审批通知和跟进提醒。', status: '已接入底座', types: ['MESSAGE'], entry: '/message/template', action: '消息模板', icon: MailOutline },
  { name: '触发器', desc: '金额汇总、阶段变化提醒和逾期待办。', status: '协议定义', types: ['TRIGGER'], entry: '', action: '查看接入', icon: FlashOutline },
  { name: '导入导出', desc: '对象级导入模板和导出字段配置。', status: '配置入口', types: ['IMPORT', 'EXPORT'], entry: '/app-center', action: '对象入口', icon: SwapVerticalOutline },
]

onMounted(loadBindings)

async function loadBindings() {
  const res = await businessBindingList({})
  bindings.value = res.data || []
}

function bindingCount(types) {
  return bindings.value.filter(item => types.includes(item.bindingType) && item.status === 1).length
}

function latestBindingUpdate(types) {
  const latest = bindings.value
    .filter(item => types.includes(item.bindingType) && item.updateTime)
    .map(item => new Date(item.updateTime).getTime())
    .filter(time => Number.isFinite(time))
    .sort((a, b) => b - a)[0]
  if (!latest)
    return ''
  return new Date(latest).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function resolveEngineEntry(item) {
  if (item.entry)
    return item.entry
  const binding = bindings.value.find(binding => item.types.includes(binding.bindingType) && binding.canOpen)
  return binding?.entryUrl || ''
}

function openEngine(item) {
  const entry = resolveEngineEntry(item)
  if (!entry)
    return
  router.push(entry)
}
</script>

<style scoped>
@import './shared-center.css';

.engine-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.engine-meta span {
  border-radius: 4px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
  line-height: 22px;
  padding: 0 8px;
}
</style>
