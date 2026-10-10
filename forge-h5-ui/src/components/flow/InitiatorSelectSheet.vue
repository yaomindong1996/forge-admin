<template>
  <AiPopupSheet
    :model-value="store.visible"
    :title="store.activeNode ? `选择${store.activeNode.nodeName}` : '选择审批人'"
    :description="store.activeNode ? (store.activeNode.multiple ? '可选择多人' : '只能选择一人') : '以下节点需要由你指定审批人'"
    :close-on-mask="false"
    @update:model-value="!$event && store.cancel()"
  >
    <!-- 成员选择：从通讯录搜索成员 -->
    <view v-if="store.activeNode" class="initiator-picker">
      <AiSearchBar v-model="keyword" placeholder="搜索姓名、账号或手机号" />
      <view class="initiator-picker__list">
        <view
          v-for="member in members.members.value"
          :key="member.userId"
          class="initiator-picker__row"
          @click="store.toggleMember(member)"
        >
          <ContactAvatar :src="member.avatar || ''" :name="member.realName" />
          <view class="initiator-picker__main">
            <text class="initiator-picker__name">{{ member.realName || '未命名成员' }}</text>
            <text v-if="contactSubtitle(member)" class="initiator-picker__desc">{{ contactSubtitle(member) }}</text>
          </view>
          <AiIcon
            v-if="store.isSelected(store.activeNodeKey, member.userId)"
            name="check"
            size="sm"
            color="var(--forge-color-primary)"
          />
        </view>
        <view class="initiator-picker__footer">
          <text v-if="members.loading.value">加载中…</text>
          <text v-else-if="members.failed.value" class="is-link" @click="members.reload()">加载失败，点击重试</text>
          <text v-else-if="!members.members.value.length">没有找到成员</text>
          <text v-else-if="!members.finished.value" class="is-link" @click="members.loadMore()">加载更多</text>
        </view>
      </view>
    </view>

    <!-- 节点概览：每个自选节点的已选成员 -->
    <view v-else class="initiator-nodes">
      <view v-for="node in store.nodes" :key="node.nodeKey" class="initiator-node">
        <view class="initiator-node__head">
          <text class="initiator-node__name">{{ node.nodeName }}</text>
          <text class="initiator-node__mode">{{ node.multiple ? '可多选' : '单选' }}</text>
        </view>
        <view class="initiator-node__members">
          <view
            v-for="member in store.selections[node.nodeKey]"
            :key="member.userId"
            class="initiator-node__chip"
            @click="store.removeMember(node.nodeKey, member.userId)"
          >
            <text>{{ member.realName || member.userId }}</text>
            <AiIcon name="x" size="xs" color="var(--forge-text-tertiary)" />
          </view>
          <view class="initiator-node__add" @click="openPicker(node.nodeKey)">
            <AiIcon name="plus" size="xs" color="var(--forge-color-primary)" />
            <text>{{ !node.multiple && store.selections[node.nodeKey]?.length ? '更换' : '添加' }}</text>
          </view>
        </view>
      </view>
    </view>

    <template #footer>
      <view class="initiator-sheet__actions">
        <AiButton v-if="store.activeNode" variant="secondary" block @click="store.back()">完成</AiButton>
        <template v-else>
          <AiButton variant="secondary" @click="store.cancel()">取消</AiButton>
          <AiButton :disabled="!store.isComplete" @click="confirm">确定</AiButton>
        </template>
      </view>
    </template>
  </AiPopupSheet>
</template>

<script setup>
import { ref, watch } from 'vue'
import AiButton from '@/components/AiButton.vue'
import AiIcon from '@/components/AiIcon.vue'
import AiPopupSheet from '@/components/AiPopupSheet.vue'
import AiSearchBar from '@/components/AiSearchBar.vue'
import ContactAvatar from '@/components/contacts/ContactAvatar.vue'
import { useContactMembers } from '@/composables/useContactMembers'
import { useInitiatorSelectStore } from '@/store'
import { contactSubtitle } from '@/utils/contacts'
import { toast } from '@/utils/notify'

const store = useInitiatorSelectStore()
const keyword = ref('')
const members = useContactMembers(() => {
  const value = keyword.value.trim()
  return value ? { keyword: value } : {}
})

let searchTimer = null
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => members.reload(), 300)
})

function openPicker(nodeKey) {
  store.pick(nodeKey)
  keyword.value = ''
  members.reload()
}

function confirm() {
  try {
    store.confirm()
  }
  catch (error) {
    toast(error?.message || '请选择审批人')
  }
}
</script>

<style lang="scss" scoped>
.initiator-nodes { display: flex; flex-direction: column; gap: 12px; }
.initiator-node { padding: 12px 0; border-bottom: 1px solid var(--forge-border-light); }
.initiator-node:last-child { border-bottom: none; }
.initiator-node__head { display: flex; align-items: baseline; gap: 8px; }
.initiator-node__name { color: var(--forge-text-primary); font-size: 15px; font-weight: 600; }
.initiator-node__mode { color: var(--forge-text-tertiary); font-size: 12px; }
.initiator-node__members { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.initiator-node__chip,
.initiator-node__add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  border-radius: 15px;
  font-size: 13px;
}
.initiator-node__chip { background: var(--forge-surface-muted); color: var(--forge-text-primary); }
.initiator-node__add { border: 1px dashed var(--forge-color-primary); color: var(--forge-color-primary); }

.initiator-picker { display: flex; flex-direction: column; gap: 8px; }
.initiator-picker__row { display: flex; align-items: center; gap: 12px; min-height: 60px; }
.initiator-picker__row:active { background: var(--forge-surface-muted); }
.initiator-picker__main { display: flex; flex: 1; flex-direction: column; min-width: 0; gap: 2px; }
.initiator-picker__name { color: var(--forge-text-primary); font-size: 15px; }
.initiator-picker__desc {
  overflow: hidden;
  color: var(--forge-text-tertiary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.initiator-picker__footer { padding: 12px 0; color: var(--forge-text-tertiary); font-size: 12px; text-align: center; }
.initiator-picker__footer .is-link { color: var(--forge-color-primary); }

.initiator-sheet__actions { display: flex; gap: 12px; }
.initiator-sheet__actions > * { flex: 1; }
</style>
