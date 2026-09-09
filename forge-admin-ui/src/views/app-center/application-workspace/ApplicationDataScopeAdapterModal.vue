<template>
  <n-modal
    :show="show"
    :mask-closable="!saving"
    :close-on-esc="!saving"
    @update:show="updateVisible"
  >
    <n-card
      class="adapter-modal-card"
      :bordered="false"
      :closable="!saving"
      role="dialog"
      aria-modal="true"
      :header-style="{ padding: '22px 24px 18px' }"
      :content-style="{ padding: '0 24px 22px', maxHeight: 'calc(88vh - 150px)', overflowY: 'auto' }"
      :footer-style="{ padding: '16px 24px', borderTop: '1px solid var(--border-color)' }"
      @close="updateVisible(false)"
    >
      <template #header>
        <div class="modal-heading">
          <span class="heading-icon" aria-hidden="true">
            <i class="i-material-symbols:shield-lock" />
          </span>
          <div class="heading-copy">
            <div class="heading-title-row">
              <h2>数据范围适配</h2>
              <span class="object-chip">
                {{ object?.objectName || object?.objectCode || '业务对象' }}
              </span>
            </div>
            <p>选择对象接入系统数据权限的方式，并完成字段映射。</p>
          </div>
        </div>
      </template>

      <div class="adapter-content">
        <n-alert
          v-if="sharedApplicationCount > 1"
          type="warning"
          :bordered="false"
        >
          该业务对象被 {{ sharedApplicationCount }} 个应用共用。这里修改的是对象级字段适配，会同时影响这些应用；角色的数据范围覆盖仍只在各应用内生效。
        </n-alert>

        <section class="adapter-section">
          <div class="section-heading">
            <span class="section-index">1</span>
            <div>
              <h3>选择数据策略</h3>
              <p>租户隔离始终生效，可按需继续接入角色数据范围。</p>
            </div>
          </div>

          <div class="strategy-grid" role="radiogroup" aria-label="对象数据策略">
            <button
              type="button"
              class="strategy-card"
              :class="{ 'is-selected': draft.dataScope === 'TENANT' }"
              :aria-pressed="draft.dataScope === 'TENANT'"
              @click="selectStrategy('TENANT')"
            >
              <span class="strategy-icon is-tenant" aria-hidden="true">
                <i class="i-material-symbols:domain-rounded" />
              </span>
              <span class="strategy-copy">
                <strong>仅租户隔离</strong>
                <small>数据按租户隔离，不再按角色细分可见范围</small>
              </span>
              <span class="selection-mark" aria-hidden="true">
                <i class="i-material-symbols:check-rounded" />
              </span>
            </button>

            <button
              type="button"
              class="strategy-card"
              :class="{ 'is-selected': draft.dataScope === 'FOLLOW_SYSTEM' }"
              :aria-pressed="draft.dataScope === 'FOLLOW_SYSTEM'"
              @click="selectStrategy('FOLLOW_SYSTEM')"
            >
              <span class="strategy-icon is-system" aria-hidden="true">
                <i class="i-material-symbols:account-tree-outline" />
              </span>
              <span class="strategy-copy">
                <strong>跟随系统数据权限</strong>
                <small>角色可配置本人、本部门及下级等数据范围</small>
              </span>
              <span class="selection-mark" aria-hidden="true">
                <i class="i-material-symbols:check-rounded" />
              </span>
            </button>
          </div>
        </section>

        <section v-if="isFollowSystem" class="adapter-section field-section">
          <div class="section-heading">
            <span class="section-index">2</span>
            <div>
              <h3>字段映射</h3>
              <p>指定系统用户、组织和区划在当前对象中的对应字段。</p>
            </div>
          </div>

          <n-form label-placement="top" size="small" :show-feedback="false">
            <n-grid :cols="2" :x-gap="14" :y-gap="12" responsive="screen">
              <n-form-item-gi label="本人字段" required>
                <n-select
                  v-model:value="draft.userField"
                  :options="fieldOptions"
                  filterable
                  clearable
                  placeholder="选择记录所属用户字段"
                />
              </n-form-item-gi>
              <n-form-item-gi label="组织字段" required>
                <n-select
                  v-model:value="draft.orgField"
                  :options="fieldOptions"
                  filterable
                  clearable
                  placeholder="选择记录所属组织字段"
                />
              </n-form-item-gi>
              <n-form-item-gi label="区划字段（可选）" :span="2">
                <n-select
                  v-model:value="draft.regionField"
                  :options="fieldOptions"
                  filterable
                  clearable
                  placeholder="需要按行政区划控制时选择"
                />
              </n-form-item-gi>
            </n-grid>
          </n-form>

          <label class="related-visible-row">
            <n-switch v-model:value="draft.flowRelatedVisible" size="small" aria-label="流程经手人可查看单据" />
            <span class="related-visible-copy">
              <strong>流程经手人可查看单据</strong>
              <small>角色按个人或部门看数时，额外可见自己发起、审批过、被抄送的单据。只能查看，不能修改或删除。待办仍在流程工作台处理。</small>
            </span>
          </label>
        </section>

        <n-alert v-if="validationMessage" type="error" :bordered="false">
          {{ validationMessage }}
        </n-alert>
      </div>

      <template #footer>
        <div class="modal-actions">
          <n-button secondary :disabled="saving" @click="updateVisible(false)">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="submit">
            保存并应用
          </n-button>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  object: {
    type: Object,
    default: null,
  },
  saving: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:show', 'save'])

const draft = ref(createDraft())
const validationMessage = ref('')

const isFollowSystem = computed(() => draft.value.dataScope === 'FOLLOW_SYSTEM')
const sharedApplicationCount = computed(() => Number(props.object?.sharedApplicationCount || 1))
const fieldOptions = computed(() => (props.object?.dataScopeAdapter?.fields || []).map(field => ({
  label: `${field.label || field.field}${field.columnName ? `（${field.field} / ${field.columnName}）` : `（${field.field}）`}`,
  value: field.field,
})))

watch(
  () => [props.show, props.object],
  ([show]) => {
    if (show) {
      draft.value = createDraft()
      validationMessage.value = ''
    }
  },
  { deep: true },
)

function createDraft() {
  const adapter = props.object?.dataScopeAdapter || {}
  return {
    dataScope: adapter.dataScope || props.object?.dataScopeMode || 'TENANT',
    userField: adapter.userField || null,
    orgField: adapter.orgField || null,
    regionField: adapter.regionField || null,
    flowRelatedVisible: adapter.flowRelatedVisible !== false,
  }
}

function updateVisible(value) {
  if (props.saving)
    return
  emit('update:show', value)
}

function selectStrategy(value) {
  if (props.saving)
    return
  draft.value.dataScope = value
  validationMessage.value = ''
}

function submit() {
  validationMessage.value = ''
  if (isFollowSystem.value && !draft.value.userField) {
    validationMessage.value = '跟随系统数据权限时必须选择本人字段。'
    return
  }
  if (isFollowSystem.value && !draft.value.orgField) {
    validationMessage.value = '跟随系统数据权限时必须选择组织字段。'
    return
  }
  emit('save', {
    dataScope: draft.value.dataScope,
    userField: draft.value.userField || null,
    orgField: draft.value.orgField || null,
    regionField: draft.value.regionField || null,
    flowRelatedVisible: isFollowSystem.value ? draft.value.flowRelatedVisible !== false : null,
  })
}
</script>

<style scoped>
.adapter-modal-card {
  width: min(720px, calc(100vw - 32px));
  overflow: hidden;
  border-radius: 14px;
  box-shadow: 0 24px 64px rgb(15 23 42 / 20%);
}

.modal-heading {
  display: flex;
  align-items: center;
  gap: 14px;
}

.heading-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  place-items: center;
  border-radius: 11px;
  background: linear-gradient(145deg, #2563eb, #4f7ff7);
  box-shadow: 0 8px 18px rgb(37 99 235 / 22%);
  color: #fff;
  font-size: 22px;
}

.heading-copy {
  min-width: 0;
}

.heading-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.heading-title-row h2 {
  margin: 0;
  color: var(--text-color-1);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.heading-copy p {
  margin: 4px 0 0;
  color: var(--text-color-3);
  font-size: 12px;
}

.object-chip {
  max-width: 220px;
  overflow: hidden;
  border: 1px solid rgb(37 99 235 / 14%);
  border-radius: 999px;
  padding: 3px 9px;
  background: rgb(37 99 235 / 7%);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.adapter-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.adapter-section {
  min-width: 0;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
}

.section-index {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  place-items: center;
  border-radius: 7px;
  background: rgb(37 99 235 / 9%);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.section-heading h3 {
  margin: 0;
  color: var(--text-color-1);
  font-size: 15px;
  font-weight: 700;
}

.section-heading p {
  margin: 3px 0 0;
  color: var(--text-color-3);
  font-size: 12px;
  line-height: 1.5;
}

.strategy-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.strategy-card {
  position: relative;
  display: grid;
  min-width: 0;
  min-height: 96px;
  grid-template-columns: 38px minmax(0, 1fr) 18px;
  align-items: flex-start;
  gap: 11px;
  border: 1px solid var(--border-color);
  border-radius: 11px;
  padding: 15px;
  background: var(--card-color);
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: border-color 160ms ease, box-shadow 160ms ease, transform 160ms ease, background 160ms ease;
}

.strategy-card:hover {
  border-color: rgb(37 99 235 / 45%);
  box-shadow: 0 8px 22px rgb(15 23 42 / 7%);
  transform: translateY(-1px);
}

.strategy-card.is-selected {
  border-color: #3977f6;
  background: linear-gradient(145deg, rgb(37 99 235 / 7%), rgb(79 127 247 / 2%));
  box-shadow: 0 0 0 1px rgb(37 99 235 / 12%), 0 10px 24px rgb(37 99 235 / 10%);
}

.strategy-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 10px;
  font-size: 20px;
}

.strategy-icon.is-tenant {
  background: rgb(71 85 105 / 9%);
  color: #475569;
}

.strategy-icon.is-system {
  background: rgb(37 99 235 / 10%);
  color: #2563eb;
}

.strategy-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
}

.strategy-copy strong {
  color: var(--text-color-1);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.strategy-copy small {
  color: var(--text-color-3);
  font-size: 12px;
  line-height: 1.55;
}

.selection-mark {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: 50%;
  color: transparent;
  font-size: 14px;
  transition: all 160ms ease;
}

.strategy-card.is-selected .selection-mark {
  border-color: #3977f6;
  background: #3977f6;
  color: #fff;
}

.field-section {
  border: 1px solid rgb(148 163 184 / 18%);
  border-radius: 11px;
  padding: 15px 16px 4px;
  background: rgb(148 163 184 / 5%);
}

.related-visible-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 14px 0 12px;
  cursor: pointer;
}

.related-visible-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.related-visible-copy strong {
  color: var(--text-color-1);
  font-size: 13px;
  font-weight: 600;
}

.related-visible-copy small {
  color: var(--text-color-3);
  font-size: 12px;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
}

@media (max-width: 640px) {
  .strategy-grid {
    grid-template-columns: 1fr;
  }

  .heading-title-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 5px;
  }
}
</style>
