<script setup>
import {
  AddCircleOutline,
  AppsOutline,
  BusinessOutline,
  CheckmarkDoneOutline,
  ChevronForwardOutline,
  DocumentTextOutline,
  FolderOpenOutline,
  GitBranchOutline,
  IdCardOutline,
  LayersOutline,
  MailOpenOutline,
  MenuOutline,
  PeopleOutline,
  PersonOutline,
  PulseOutline,
  SendOutline,
  ShieldCheckmarkOutline,
  TimeOutline,
} from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'

defineProps({ name: { type: String, required: true }, size: { type: Number, default: 20 } })

// 静态功能图标直接渲染 SVG，不依赖生产构建扫描动态 CSS 类名。
const icons = {
  create: AddCircleOutline,
  apps: AppsOutline,
  organization: BusinessOutline,
  completed: CheckmarkDoneOutline,
  arrow: ChevronForwardOutline,
  task: DocumentTextOutline,
  files: FolderOpenOutline,
  workflow: GitBranchOutline,
  post: IdCardOutline,
  design: LayersOutline,
  cc: MailOpenOutline,
  menu: MenuOutline,
  role: PeopleOutline,
  user: PersonOutline,
  monitor: PulseOutline,
  started: SendOutline,
  authorization: ShieldCheckmarkOutline,
  time: TimeOutline,
}
</script>

<template>
  <NIcon class="home-action-icon" :size="size" aria-hidden="true">
    <component :is="icons[name] || AppsOutline" />
  </NIcon>
</template>

<style scoped>
.home-action-icon {
  display: inline-flex;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
