<template>
  <div class="center-shell">
    <header>
      <n-button text @click="router.push('/app-center')">返回应用中心</n-button>
      <h1>移动端中心</h1>
      <p>登记 H5、移动待办、移动审批和移动业务入口，第一阶段聚焦入口管理和可见范围。</p>
    </header>
    <div class="capability-grid">
      <section v-for="item in entries" :key="item.name" class="capability-card">
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
import { ClipboardOutline, ListOutline, PhonePortraitOutline, WalkOutline } from '@vicons/ionicons5'

const router = useRouter()

const entries = [
  { name: 'H5 应用入口', desc: '登记移动端入口名称、图标、地址和可见范围。', status: '入口管理', icon: PhonePortraitOutline },
  { name: '移动待办', desc: '承接销售任务、跟进提醒和回款逾期待办。', status: '规划接入', icon: ListOutline },
  { name: '移动审批', desc: '承接合同审批、折扣审批和流程任务。', status: '规划接入', icon: ClipboardOutline },
  { name: '移动拜访', desc: '客户拜访、签到、跟进记录和现场资料上传。', status: '样板预留', icon: WalkOutline },
]
</script>

<style scoped>
@import './shared-center.css';
</style>
