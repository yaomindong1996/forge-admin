<template>
  <NModal
    :show="show"
    preset="card"
    title="社区与支持"
    class="community-support-dialog"
    :style="{ width: 'min(740px, calc(100vw - 32px))', maxHeight: 'calc(100dvh - 32px)' }"
    :content-style="{ minHeight: '0', overflow: 'auto' }"
    :segmented="{ content: true }"
    @update:show="$emit('update:show', $event)"
  >
    <!-- 登录配置的群二维码优先，未配置时保留现有维护者联系方式 -->
    <p class="support-intro">
      遇到问题可扫码交流，或联系系统管理员协助排查。
    </p>
    <div class="support-grid">
      <section v-for="entry in entries" :key="entry.key" class="support-entry">
        <h3>{{ entry.title }}</h3>
        <NImage
          :src="entry.image"
          :preview-src="entry.image"
          :alt="entry.title"
          object-fit="contain"
          class="support-image"
        />
        <p>{{ entry.description }}</p>
      </section>
    </div>
    <template #footer>
      <div class="support-footer">
        <span>点击二维码可放大查看</span>
        <NButton @click="$emit('update:show', false)">
          关闭
        </NButton>
      </div>
    </template>
  </NModal>
</template>

<script setup>
import { NButton, NImage, NModal } from 'naive-ui'
import { computed } from 'vue'
import maintainerQr from '@/assets/images/forge-wechat-group1.png'
import groupQr from '@/assets/images/forge-wechat-group.png'
import supportQr from '@/assets/images/forge-wechat-support.png'

const props = defineProps({
  show: Boolean,
  groupImage: { type: String, default: '' },
  groupName: { type: String, default: '用户交流群' },
})
defineEmits(['update:show'])

const entries = computed(() => [
  {
    key: 'group',
    title: props.groupImage ? props.groupName : '联系维护者',
    image: props.groupImage || groupQr,
    description: props.groupImage ? '扫码加入交流群，交流使用与配置问题' : '扫码联系维护者，获取使用帮助',
  },
  { key: 'maintainer', title: '技术交流', image: maintainerQr, description: '低代码、流程与插件扩展问题' },
  { key: 'support', title: '支持维护', image: supportQr, description: '支持项目持续维护与完善' },
])
</script>

<style scoped>
.support-intro {
  margin: 0 0 20px;
  color: var(--text-secondary, #64748b);
  font-size: 13px;
}
.support-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
.support-entry {
  min-width: 0;
  text-align: center;
}
.support-entry h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
}
.support-image {
  width: 100%;
  background: #fff;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
}
.support-image :deep(img) {
  width: 100%;
  height: 180px;
  object-fit: contain;
}
.support-entry p {
  margin: 12px 0 0;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.6;
}
.support-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.support-footer span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}
@media (max-width: 560px) {
  .support-grid {
    grid-template-columns: 1fr;
    gap: 24px;
    max-height: 65dvh;
    overflow: auto;
  }
  .support-image {
    width: 190px;
  }
}
</style>
