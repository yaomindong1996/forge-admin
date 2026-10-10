<template>
  <view class="contacts-page contacts-page--stack">
    <AiFeedbackHost />
    <scroll-view class="contacts-scroll contacts-scroll--stack" scroll-y :show-scrollbar="false">
      <view v-if="loading" class="contacts-section">
        <AiListSkeleton :rows="4" />
      </view>

      <AiEmpty
        v-else-if="!member"
        title="成员不存在"
        description="该成员可能已停用或不在当前组织。"
        icon="user-x"
      />

      <template v-else>
        <!-- 名片 -->
        <view class="member-card">
          <ContactAvatar :src="member.avatar || ''" :name="member.realName" size="lg" />
          <view class="member-card__copy">
            <text class="member-card__name">{{ member.realName || '未命名成员' }}</text>
            <text v-if="subtitle" class="member-card__desc">{{ subtitle }}</text>
          </view>
        </view>

        <!-- 快捷操作：没有可拨号码时置灰 -->
        <view class="member-actions">
          <button class="member-action" :class="{ 'is-disabled': !dialable }" @click="dial">
            <AiIcon name="phone" :color="dialable ? '#0066ff' : '#c1c3c6'" size="md" />
            <text>打电话</text>
          </button>
          <button class="member-action" :class="{ 'is-disabled': !member.email }" @click="copy(member.email, '邮箱')">
            <AiIcon name="mail" :color="member.email ? '#0066ff' : '#c1c3c6'" size="md" />
            <text>复制邮箱</text>
          </button>
        </view>

        <!-- 资料 -->
        <view class="member-info">
          <view class="member-info__row" @click="copy(member.phone, '手机号')">
            <text class="member-info__label">手机号</text>
            <text class="member-info__value">{{ member.phone || '未填写' }}</text>
            <AiIcon v-if="member.phone" name="copy" color="#a2a3a5" size="sm" />
          </view>
          <view class="member-info__row" @click="copy(member.email, '邮箱')">
            <text class="member-info__label">邮箱</text>
            <text class="member-info__value">{{ member.email || '未填写' }}</text>
            <AiIcon v-if="member.email" name="copy" color="#a2a3a5" size="sm" />
          </view>
          <view class="member-info__row">
            <text class="member-info__label">部门</text>
            <text class="member-info__value">{{ joinNames(member.orgNames) }}</text>
          </view>
          <view class="member-info__row">
            <text class="member-info__label">职位</text>
            <text class="member-info__value">{{ joinNames(member.postNames) }}</text>
          </view>
        </view>
      </template>
    </scroll-view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AiEmpty from '@/components/AiEmpty.vue'
import AiFeedbackHost from '@/components/feedback/AiFeedbackHost.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiListSkeleton from '@/components/AiListSkeleton.vue'
import ContactAvatar from '@/components/contacts/ContactAvatar.vue'
import api from '@/api'
import { canDial, contactSubtitle } from '@/utils/contacts'
import { toast } from '@/utils/notify'

const member = ref(null)
const loading = ref(true)
const subtitle = computed(() => contactSubtitle(member.value || {}))
const dialable = computed(() => canDial(member.value?.phone))

async function loadMember(userId) {
  loading.value = true
  try {
    const res = await api.getContactMember(userId)
    member.value = res?.data || null
  }
  catch (error) {
    console.error('加载成员信息失败:', error)
    member.value = null
  }
  finally {
    loading.value = false
  }
}

function joinNames(names) {
  return Array.isArray(names) && names.length ? names.join('、') : '未分配'
}

function dial() {
  if (!dialable.value) {
    toast('该成员未填写手机号')
    return
  }
  uni.makePhoneCall({ phoneNumber: String(member.value.phone).replace(/[\s-]/g, '') })
}

function copy(value, label) {
  if (!value) return
  uni.setClipboardData({
    data: String(value),
    showToast: false,
    success: () => toast(`${label}已复制`, { type: 'success' }),
    fail: () => toast('复制失败，请长按手动复制', { type: 'error' }),
  })
}

onLoad((query = {}) => {
  if (!query.userId) {
    loading.value = false
    return
  }
  loadMember(query.userId)
})
</script>

<style lang="scss" scoped src="../styles/contacts.scss"></style>
