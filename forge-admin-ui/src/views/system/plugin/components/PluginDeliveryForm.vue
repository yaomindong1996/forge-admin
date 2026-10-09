<template>
  <section class="delivery-form">
    <!-- 目标与候选选择，不接受路径/凭证/命令 -->
    <NAlert v-if="!store.targets.length" type="info" :bordered="false">
      尚未配置可用交付目标。请部署方启用独立交付执行器、COS仓库及目标配置。
    </NAlert>
    <NForm label-placement="top" :disabled="locked">
      <div class="fields">
        <NFormItem label="部署目标">
          <NSelect v-model:value="store.draft.targetId" :options="targets" placeholder="选择已授权目标" />
        </NFormItem>
        <NFormItem label="已登记候选">
          <NSelect v-model:value="store.draft.releaseId" :options="candidates" placeholder="选择锁定版本" />
        </NFormItem>
        <NFormItem label="交付动作">
          <NSelect
            v-model:value="store.draft.action" :options="dict.sys_plugin_delivery_action || []"
            placeholder="选择交付动作"
          />
        </NFormItem>
      </div>
      <p v-if="store.target" class="hint">
        当前核验版本：{{ store.target.currentReleaseId || '无' }} · 上一版：{{ store.target.previousReleaseId || '无' }}
      </p>
      <NAlert v-if="store.target?.unverifiedReleaseId" type="warning" :bordered="false">
        目标仍有未核验部署，只能恢复最后确认版本：{{ store.target.currentReleaseId || '无确认版本，请人工核查' }}。
        原执行器停止后，先使用 recover-lock 归档遗留锁。
      </NAlert>
      <!-- 不把备份声明当作自动备份，更不执行数据库回滚 -->
      <template v-if="store.draft.action && store.draft.action !== 'publish'">
        <NFormItem label="数据库备份凭据编号（不填写密钥）">
          <NInput v-model:value="store.draft.backupReference" :maxlength="128" placeholder="例如 backup-20261009-01" />
        </NFormItem>
        <NCheckbox v-model:checked="store.draft.migrationsReviewed">
          已完成数据库备份并审查迁移影响；本任务不会自动备份或回滚数据库
        </NCheckbox>
        <NCheckbox v-if="store.draft.action === 'restore'" v-model:checked="store.draft.backwardCompatible">
          已确认上一版与当前数据库向后兼容
        </NCheckbox>
      </template>
      <NFormItem label="人工确认说明">
        <NInput
          v-model:value="store.draft.note" type="textarea" :maxlength="1000"
          :autosize="{ minRows: 2, maxRows: 4 }" placeholder="至少10字，说明审查和操作依据，不要填写凭证"
        />
      </NFormItem>
    </NForm>
    <NAlert v-if="store.pending" type="warning" :bordered="false">
      上次提交结果未确认，请先刷新记录；重试使用冻结的原请求。
    </NAlert>
    <NButton
      class="delivery-submit" type="primary" :loading="store.busy" :disabled="!store.pending && !store.complete"
      @click="store.submit"
    >
      {{ store.pending ? '重试原请求' : '确认创建交付任务' }}
    </NButton>
    <p class="hint">
      创建任务不会在当前服务执行代码。目标执行器必须使用独立凭证领取；成功表示执行器核验回执，非平台直连验证。
    </p>
  </section>
</template>

<script setup>
import { NAlert, NButton, NCheckbox, NForm, NFormItem, NInput, NSelect } from 'naive-ui'
import { computed } from 'vue'
import { useDict } from '@/composables/useDict'
import { usePluginDeliveryStore } from '@/stores/plugin/deliveryStore'

const store = usePluginDeliveryStore()
const { dict } = useDict('sys_plugin_delivery_action')
const locked = computed(() => store.busy || Boolean(store.pending))
const targets = computed(() => store.targets.map(row => ({ label: row.name || row.id, value: row.id })))
const candidates = computed(() => store.available.map(row => ({
  label: `${row.pluginId} · ${row.pluginVersion} · ${row.releaseId.slice(0, 16)}`,
  value: row.releaseId,
})))
</script>

<style scoped>
.delivery-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.fields {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr;
  gap: 12px;
}
.hint {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.delivery-submit {
  align-self: flex-start;
  max-width: 100%;
}
:deep(.n-checkbox) {
  display: flex;
  margin-bottom: 12px;
}
@media (max-width: 720px) {
  .fields {
    grid-template-columns: 1fr;
  }
}
</style>
