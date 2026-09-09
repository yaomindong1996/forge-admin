<template>
  <div class="scope-rule-editor">
    <p class="scope-rule-intro">
      配置此页面支持哪些限制方式，具体能看哪些数据由角色授权决定。至少配置本人或组织范围。
    </p>

    <section class="scope-rule">
      <div class="scope-rule__head">
        <NSwitch :value="userEnabled" size="small" aria-label="支持按本人限制" @update:value="toggleUser" />
        <div class="scope-rule__copy">
          <strong>按本人限制</strong>
          <span>角色选择「仅本人」时，按创建人、负责人等字段匹配当前用户</span>
        </div>
      </div>
      <details v-if="userEnabled" class="scope-rule__body">
        <summary>字段设置 <code>{{ simpleColumn(formData.userIdColumn) || '自定义 SQL' }}</code></summary>
        <label class="scope-field">
          <span>对应字段</span>
          <NInput
            v-if="userSql"
            :value="formData.userIdColumn"
            type="textarea"
            :rows="3"
            placeholder="自定义 SQL，以 <sql> 开头"
            @update:value="value => setField('userIdColumn', value)"
          />
          <NSelect
            v-else
            :value="simpleColumn(formData.userIdColumn)"
            filterable
            tag
            :options="userColumnOptions"
            placeholder="如 create_by"
            @update:value="value => setField('userIdColumn', value)"
          />
        </label>
        <button type="button" class="scope-mode-button" @click="toggleSql('user')">
          {{ userSql ? '改用字段名' : '使用自定义 SQL' }}
        </button>
        <div v-if="userSql" class="scope-tokens">
          <button v-for="token in commonTokens" :key="token" type="button" class="scope-token" @click="appendToken('userIdColumn', token)">
            {{ token }}
          </button>
        </div>
      </details>
    </section>

    <section class="scope-rule">
      <div class="scope-rule__head">
        <NSwitch :value="orgEnabled" size="small" aria-label="支持按组织限制" @update:value="toggleOrg" />
        <div class="scope-rule__copy">
          <strong>按组织限制</strong>
          <span>角色选「本组织 / 本组织及下级」时按这个字段过滤</span>
        </div>
      </div>
      <details v-if="orgEnabled" class="scope-rule__body">
        <summary>字段设置 <code>{{ simpleColumn(formData.orgIdColumn) || '自定义 SQL' }}</code></summary>
        <label class="scope-field">
          <span>对应字段</span>
          <NInput
            v-if="orgSql"
            :value="formData.orgIdColumn"
            type="textarea"
            :rows="3"
            placeholder="自定义 SQL，以 <sql> 开头"
            @update:value="value => setField('orgIdColumn', value)"
          />
          <NSelect
            v-else
            :value="simpleColumn(formData.orgIdColumn)"
            filterable
            tag
            :options="orgColumnOptions"
            placeholder="如 org_id"
            @update:value="value => setField('orgIdColumn', value)"
          />
        </label>
        <button type="button" class="scope-mode-button" @click="toggleSql('org')">
          {{ orgSql ? '改用字段名' : '使用自定义 SQL' }}
        </button>
        <div v-if="orgSql" class="scope-tokens">
          <button v-for="token in commonTokens" :key="token" type="button" class="scope-token" @click="appendToken('orgIdColumn', token)">
            {{ token }}
          </button>
        </div>
      </details>
    </section>

    <section class="scope-rule">
      <div class="scope-rule__head">
        <NSwitch :value="regionEnabled" size="small" aria-label="支持按行政区划限制" @update:value="toggleRegion" />
        <div class="scope-rule__copy">
          <strong>按行政区划过滤</strong>
          <span>角色选「本区划」时使用</span>
        </div>
      </div>
      <details v-if="regionEnabled" class="scope-rule__body">
        <summary>字段设置 <code>{{ simpleColumn(formData.regionCodeColumn) || '自定义 SQL' }}</code></summary>
        <label class="scope-field">
          <span>对应字段</span>
          <NInput
            v-if="regionSql"
            :value="formData.regionCodeColumn"
            type="textarea"
            :rows="3"
            placeholder="自定义 SQL，以 <sql> 开头"
            @update:value="value => setField('regionCodeColumn', value)"
          />
          <NSelect
            v-else
            :value="simpleColumn(formData.regionCodeColumn)"
            filterable
            tag
            :options="regionColumnOptions"
            placeholder="如 region_code"
            @update:value="value => setField('regionCodeColumn', value)"
          />
        </label>
        <button type="button" class="scope-mode-button" @click="toggleSql('region')">
          {{ regionSql ? '改用字段名' : '使用自定义 SQL' }}
        </button>
        <div v-if="regionSql" class="scope-tokens">
          <button v-for="token in regionTokens" :key="token" type="button" class="scope-token" @click="appendToken('regionCodeColumn', token)">
            {{ token }}
          </button>
        </div>
      </details>
    </section>

    <section class="scope-rule">
      <div class="scope-rule__head">
        <NSwitch :value="tenantEnabled" size="small" aria-label="支持按租户限制" @update:value="toggleTenant" />
        <div class="scope-rule__copy">
          <strong>按租户隔离</strong>
          <span>多租户列表建议保持开启，字段一般是 tenant_id</span>
        </div>
      </div>
      <details v-if="tenantEnabled" class="scope-rule__body">
        <summary>字段设置 <code>{{ simpleColumn(formData.tenantIdColumn) || 'tenant_id' }}</code></summary>
        <label class="scope-field">
          <span>对应字段</span>
          <NSelect
            :value="simpleColumn(formData.tenantIdColumn) || 'tenant_id'"
            filterable
            tag
            :options="tenantColumnOptions"
            placeholder="tenant_id"
            @update:value="value => setField('tenantIdColumn', value)"
          />
        </label>
      </details>
    </section>

    <section class="scope-rule">
      <div class="scope-rule__head">
        <NSwitch
          :value="flowEnabled"
          size="small"
          aria-label="经手单据可见"
          @update:value="toggleFlow"
        />
        <div class="scope-rule__copy">
          <strong>审批过的单据也能看见</strong>
          <span>自己发起、审批或被抄送的单据会出现在列表中，改删仍走上面的范围</span>
        </div>
      </div>
      <div v-if="flowEnabled" class="scope-rule__body">
        <label class="scope-field">
          <span>流程业务类型</span>
          <NInput
            :value="formData.flowBusinessType"
            placeholder="与流程绑定一致，如 leave；低代码可留空"
            @update:value="value => setField('flowBusinessType', value)"
          />
        </label>
        <label class="scope-field">
          <span>单据主键列</span>
          <NInput
            :value="formData.recordIdColumn || 'id'"
            placeholder="默认 id"
            @update:value="value => setField('recordIdColumn', value)"
          />
        </label>
      </div>
    </section>
  </div>
</template>

<script setup>
import { NInput, NSelect, NSwitch } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({
  formData: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update'])

function setField(field, value) {
  emit('update', field, value)
}

const userColumnOptions = [
  { label: '创建人 create_by', value: 'create_by' },
  { label: '用户 user_id', value: 'user_id' },
  { label: '申请人 apply_user_id', value: 'apply_user_id' },
  { label: '负责人 owner_id', value: 'owner_id' },
]
const orgColumnOptions = [
  { label: '组织 org_id', value: 'org_id' },
  { label: '部门 dept_id', value: 'dept_id' },
  { label: '申请部门 apply_dept_id', value: 'apply_dept_id' },
  { label: '创建部门 create_dept', value: 'create_dept' },
]
const tenantColumnOptions = [
  { label: '租户 tenant_id', value: 'tenant_id' },
]
const regionColumnOptions = [
  { label: '区划 region_code', value: 'region_code' },
  { label: '区域 area_code', value: 'area_code' },
]
const commonTokens = ['#{userId}', '#{tenantId}', '#{orgIds}', '#{customOrgIds}']
const regionTokens = ['#{regionCode}', '#{regionLevel}', '#{regionAncestors}']

const userEnabled = computed(() => hasValue(props.formData.userIdColumn))
const orgEnabled = computed(() => hasValue(props.formData.orgIdColumn))
const regionEnabled = computed(() => hasValue(props.formData.regionCodeColumn))
const tenantEnabled = computed(() => hasValue(props.formData.tenantIdColumn))
const flowEnabled = computed(() => Number(props.formData.flowRelatedVisible) === 1)
const userSql = computed(() => isSql(props.formData.userIdColumn))
const orgSql = computed(() => isSql(props.formData.orgIdColumn))
const regionSql = computed(() => isSql(props.formData.regionCodeColumn))

function hasValue(value) {
  return String(value || '').trim() !== ''
}

function isSql(value) {
  return String(value || '').trim().toLowerCase().startsWith('<sql>')
}

function simpleColumn(value) {
  return isSql(value) ? '' : String(value || '').trim()
}

function toggleUser(enabled) {
  setField('userIdColumn', enabled ? (simpleColumn(props.formData.userIdColumn) || 'create_by') : '')
}

function toggleOrg(enabled) {
  setField('orgIdColumn', enabled ? (simpleColumn(props.formData.orgIdColumn) || 'org_id') : '')
}

function toggleRegion(enabled) {
  setField('regionCodeColumn', enabled ? (simpleColumn(props.formData.regionCodeColumn) || 'region_code') : '')
}

function toggleTenant(enabled) {
  setField('tenantIdColumn', enabled ? (simpleColumn(props.formData.tenantIdColumn) || 'tenant_id') : '')
}

function toggleFlow(enabled) {
  setField('flowRelatedVisible', enabled ? 1 : 0)
  if (enabled && !props.formData.recordIdColumn)
    setField('recordIdColumn', 'id')
}

function toggleSql(type) {
  if (type === 'user') {
    setField('userIdColumn', userSql.value ? 'create_by' : '<sql>t.create_by = #{userId}')
    return
  }
  if (type === 'org') {
    setField('orgIdColumn', orgSql.value ? 'org_id' : '<sql>t.org_id IN (#{orgIds})')
    return
  }
  setField('regionCodeColumn', regionSql.value ? 'region_code' : '<sql>t.region_code = #{regionCode}')
}

function appendToken(field, token) {
  const current = String(props.formData[field] || '').trim()
  setField(field, current ? `${current} ${token}` : token)
}
</script>

<style scoped>
.scope-rule-editor {
  display: grid;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.scope-rule-intro {
  margin: 0 0 4px;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.scope-rule {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  background: var(--bg-primary);
}

.scope-rule__head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.scope-rule__copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.scope-rule__copy strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

.scope-rule__copy span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.scope-rule__body {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
}

.scope-rule__body summary {
  color: var(--text-tertiary);
  font-size: 12px;
  cursor: pointer;
}

.scope-rule__body summary code {
  margin-left: 6px;
  font-size: 11px;
}

.scope-rule__body[open] summary {
  margin-bottom: 10px;
}

.scope-mode-button {
  display: block;
  margin-top: 6px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--primary-color);
  font-size: 12px;
  cursor: pointer;
}

.scope-mode-button:hover {
  text-decoration: underline;
}

.scope-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.scope-field > span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 18px;
}

.scope-tokens {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.scope-token {
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-secondary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  cursor: pointer;
}

.scope-token:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}
</style>
