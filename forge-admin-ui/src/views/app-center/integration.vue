<template>
  <div class="center-shell">
    <header>
      <n-button text @click="router.push('/app-center')">返回应用中心</n-button>
      <h1>集成中心</h1>
      <p>统一登记开放接口、Webhook 和第三方平台入口，具体推送通道后续分阶段接入。</p>
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
import { CloudUploadOutline, GitNetworkOutline, LinkOutline, PaperPlaneOutline } from '@vicons/ionicons5'

const router = useRouter()

const entries = [
  { name: '开放接口', desc: '为业务对象和应用入口登记标准 API 能力。', status: '入口管理', icon: LinkOutline },
  { name: 'Webhook', desc: '登记事件推送、签名策略和目标系统。', status: '协议定义', icon: CloudUploadOutline },
  { name: '企微 / 飞书 / 钉钉', desc: '面向审批提醒、待办和业务通知的第三方平台入口。', status: '扩展预留', icon: PaperPlaneOutline },
  { name: '集成应用', desc: '将外部平台作为应用入口统一纳入权限和菜单管理。', status: '入口管理', icon: GitNetworkOutline },
]
</script>

<style scoped>
@import './shared-center.css';
</style>
