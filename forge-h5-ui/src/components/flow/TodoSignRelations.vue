<template>
  <view v-if="signRelations.length" class="sign-relations">
    <text class="sign-relations__title">加签人员</text>
    <view v-for="relation in signRelations" :key="relation.id || relation.targetUserId" class="sign-relation">
      <view class="sign-relation__avatar">{{ signUserName(relation).slice(0, 1) }}</view>
      <view class="sign-relation__copy">
        <text class="sign-relation__name">{{ signUserName(relation) }}</text>
        <text v-if="relation.reason" class="sign-relation__reason">{{ relation.reason }}</text>
      </view>
      <text class="sign-relation__status" :class="{ 'is-active': Number(relation.status) === SIGN_RELATION_ACTIVE }">
        {{ signStatusText(relation) }}
      </text>
    </view>
  </view>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useTodoDetailStore } from '@/store'
import { SIGN_RELATION_ACTIVE, signStatusText, signUserName } from '@/utils/flow-sign'

const { signRelations } = storeToRefs(useTodoDetailStore())
</script>

<style lang="scss" scoped>
.sign-relations { display: flex; flex-direction: column; gap: 10px; margin-top: 4px; padding-top: 14px; border-top: 1px solid var(--forge-border); }
.sign-relations__title { color: var(--forge-text-primary); font-size: 14px; font-weight: 600; }
.sign-relation { display: flex; min-width: 0; align-items: center; gap: 10px; }

.sign-relation__avatar {
  display: flex;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  color: #fff;
  background: var(--forge-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.sign-relation__copy { min-width: 0; flex: 1; }
.sign-relation__name,
.sign-relation__reason { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sign-relation__name { color: var(--forge-text-primary); font-size: 14px; }
.sign-relation__reason { margin-top: 1px; color: var(--forge-text-tertiary); font-size: 12px; }
.sign-relation__status { flex: 0 0 auto; padding: 2px 8px; border-radius: 6px; color: var(--forge-text-tertiary); background: var(--forge-surface-muted); font-size: 12px; }
.sign-relation__status.is-active { color: var(--forge-tone-green); background: var(--forge-tone-green-bg); }
</style>
