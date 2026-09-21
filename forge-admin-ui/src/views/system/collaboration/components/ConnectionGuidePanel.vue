<template>
  <n-modal
    v-model:show="visible"
    title="配置指南"
    preset="card"
    style="width: min(720px, calc(100vw - 32px))"
  >
    <div class="guide-content">
      <div class="guide-steps">
        <div class="guide-step">
          <div class="guide-step__index">
            1
          </div>
          <div>
            <div class="guide-step__title">
              新建连接
            </div>
            <div class="guide-step__desc">
              选择平台、填写企业标识等基础信息，连接标识可留空自动生成。每个字段旁的 ? 图标都有详细说明。
            </div>
          </div>
        </div>
        <div class="guide-step">
          <div class="guide-step__index">
            2
          </div>
          <div>
            <div class="guide-step__title">
              配置平台应用
            </div>
            <div class="guide-step__desc">
              打开连接的「配置」，在平台应用中新增应用，填入应用 ID/Key 与 Secret。一个连接可配置多个应用，分别承担登录、消息等不同职责。
            </div>
          </div>
        </div>
        <div class="guide-step">
          <div class="guide-step__index">
            3
          </div>
          <div>
            <div class="guide-step__title">
              启用能力并测试
            </div>
            <div class="guide-step__desc">
              将扫码登录、通讯录同步、消息推送等能力启用到具体应用，然后用「测试」验证连通性，同步和投递结果在对应页签中查看。
            </div>
          </div>
        </div>
      </div>
      <n-divider style="margin: 16px 0 12px" />
      <div class="guide-section-title">
        各平台凭据获取路径
      </div>
      <n-table :bordered="true" size="small" class="guide-table">
        <thead>
          <tr>
            <th style="width: 90px">
              平台
            </th>
            <th>企业标识</th>
            <th>平台应用凭据（ID/Secret）</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>企业微信</td>
            <td>管理后台 → 我的企业 → 企业信息 → 企业 ID</td>
            <td>管理后台 → 应用管理 → 自建应用 → AgentId / Secret</td>
          </tr>
          <tr>
            <td>钉钉</td>
            <td>开放平台首页右上角 CorpId</td>
            <td>开放平台 → 应用开发 → 企业内部应用 → AppKey / AppSecret</td>
          </tr>
          <tr>
            <td>飞书</td>
            <td>管理后台 → 设置 → 企业信息</td>
            <td>开放平台 → 开发者后台 → 应用 → App ID / App Secret</td>
          </tr>
          <tr>
            <td>其它（Gitee/GitHub 等）</td>
            <td>无需填写（仅 OAuth 登录）</td>
            <td>平台开发者设置 → OAuth 应用 → Client ID / Client Secret</td>
          </tr>
        </tbody>
      </n-table>
    </div>
  </n-modal>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  show: Boolean,
})
const emit = defineEmits(['update:show'])
const visible = computed({
  get: () => props.show,
  set: value => emit('update:show', value),
})
</script>

<style scoped>
.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.guide-step {
  display: flex;
  gap: 12px;
}

.guide-step__index {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #eff6ff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

.guide-step__title {
  font-size: 14px;
  font-weight: 500;
  line-height: 24px;
  color: var(--text-color-1, #1f2937);
}

.guide-step__desc {
  margin-top: 2px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-color-3, #6b7280);
}

.guide-section-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-1, #1f2937);
}

.guide-table {
  font-size: 13px;
}
</style>
