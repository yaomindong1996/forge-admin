<template>
  <div class="center-shell">
    <header>
      <n-button text @click="router.push('/app-center')">返回应用中心</n-button>
      <h1>引擎中心</h1>
      <p>流程、审批、报表、权限、消息、触发器和导入导出能力统一挂接到业务对象或应用入口。</p>
    </header>
    <div class="capability-grid">
      <section v-for="item in engines" :key="item.name" class="capability-card">
        <n-icon :component="item.icon" />
        <strong>{{ item.name }}</strong>
        <p>{{ item.desc }}</p>
        <span>{{ item.status }}</span>
      </section>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import {
  BarChartOutline,
  CheckmarkDoneOutline,
  FlashOutline,
  GitBranchOutline,
  KeyOutline,
  MailOutline,
  SwapVerticalOutline,
} from '@vicons/ionicons5'

const router = useRouter()

const engines = [
  { name: '流程引擎', desc: '业务流转、任务分派和流程监控入口。', status: '已接入底座', icon: GitBranchOutline },
  { name: '审批引擎', desc: '合同审批、折扣审批和回款审批挂接。', status: '配置入口', icon: CheckmarkDoneOutline },
  { name: '报表引擎', desc: '销售看板、客户分析和回款分析。', status: '配置入口', icon: BarChartOutline },
  { name: '权限引擎', desc: '区域可见、部门可见和负责人可见。', status: '已接入底座', icon: KeyOutline },
  { name: '消息引擎', desc: '到期提醒、审批通知和跟进提醒。', status: '已接入底座', icon: MailOutline },
  { name: '触发器', desc: '金额汇总、阶段变化提醒和逾期待办。', status: '协议定义', icon: FlashOutline },
  { name: '导入导出', desc: '对象级导入模板和导出字段配置。', status: '配置入口', icon: SwapVerticalOutline },
]
</script>

<style scoped>
@import './shared-center.css';
</style>
