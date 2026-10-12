<script setup>
/**
 * FormPermissionConfig — 表单字段权限
 *
 * 由当前流程字段目录生成，不再手工输入字段名。
 * 主表 / 子表 / 数组明细分区展示；子表行操作只保留当前目录中仍存在的子表。
 * config.formFieldPermissions: [{ field, label, readable, writable, required }]
 */
import { computed } from 'vue'
import {
  childTableKeysAlias,
  normalizeFlowArrayPermission,
  normalizeFlowChildPermission,
  normalizeFlowFieldCatalog,
  normalizeFlowFieldPermission,
  normalizeFlowFormPermissions,
  serializeFlowFormPermissions,
} from '@/utils/flow-field-permissions'

const props = defineProps({
  config: { type: Object, required: true },
  formFieldCatalog: { type: Array, default: () => [] },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const savedPermissionBundle = computed(() => {
  const embedded = normalizeFlowFormPermissions(props.config?.formFieldPermissions)
  const children = Array.isArray(props.config?.formChildPermissions) && props.config.formChildPermissions.length
    ? props.config.formChildPermissions
    : embedded.children
  const arrays = Array.isArray(props.config?.formArrayPermissions) && props.config.formArrayPermissions.length
    ? props.config.formArrayPermissions
    : embedded.arrays
  return normalizeFlowFormPermissions({
    fields: embedded.fields,
    children,
    arrays,
  })
})

const savedPermissions = computed(() => {
  const map = new Map()
  for (const item of savedPermissionBundle.value.fields) {
    if (item.field)
      map.set(item.permissionKey, item)
  }
  return map
})

const formFields = computed(() => {
  return normalizeFlowFieldCatalog(props.formFieldCatalog || []).map(item => ({
    ...item,
    componentType: item?.componentType || item?.type || '',
    dataType: item?.dataType || '',
    sourceRequired: item?.required === true || item?.sourceRequired === true,
  }))
})

const catalogChildKeys = computed(() => {
  const keys = []
  formFields.value.filter(field => field.scope === 'child' && field.childKey).forEach((field) => {
    if (!keys.some(key => childTableKeysAlias(key, field.childKey)))
      keys.push(field.childKey)
  })
  return keys
})

const catalogArrayKeys = computed(() => {
  const keys = []
  formFields.value.filter(field => field.scope === 'array' && field.arrayKey).forEach((field) => {
    if (!keys.includes(field.arrayKey))
      keys.push(field.arrayKey)
  })
  return keys
})

const rows = computed(() => {
  const output = formFields.value.map((field) => {
    const saved = findSavedFieldPermission(field)
    return {
      ...field,
      configured: Boolean(saved),
      ...normalizeFlowFieldPermission(saved || {
        ...field,
        readable: true,
        writable: field.scope !== 'child',
      }),
      childKey: field.childKey,
      childLabel: field.childLabel,
      permissionKey: field.permissionKey,
    }
  })

  for (const saved of savedPermissions.value.values()) {
    if (output.some(row => samePermissionRow(row, saved)))
      continue
    // 已从表单目录移除的子表/数组字段不再占位，避免和当前主表字段混在一起
    if (saved.scope === 'child' && !catalogChildKeys.value.some(key => childTableKeysAlias(key, saved.childKey)))
      continue
    if (saved.scope === 'array' && !catalogArrayKeys.value.includes(saved.arrayKey))
      continue
    output.push({
      ...saved,
      configured: true,
      sourceRequired: false,
      componentType: '',
      dataType: '',
      stale: true,
    })
  }
  return output
})

const mainFieldRows = computed(() => rows.value.filter(row => !row.scope || row.scope === 'main'))

const childFieldGroups = computed(() => {
  const groups = []
  catalogChildKeys.value.forEach((childKey) => {
    const childRows = rows.value.filter(row => row.scope === 'child' && childTableKeysAlias(row.childKey, childKey))
    if (!childRows.length)
      return
    const label = childRows.find(row => row.childLabel && row.childLabel !== childKey)?.childLabel
      || childRows[0]?.relationName
      || childKey
    groups.push({
      childKey,
      label,
      rows: childRows,
    })
  })
  return groups
})

const arrayFieldGroups = computed(() => {
  const groups = []
  catalogArrayKeys.value.forEach((arrayKey) => {
    const arrayRows = rows.value.filter(row => row.scope === 'array' && row.arrayKey === arrayKey)
    if (!arrayRows.length)
      return
    groups.push({
      arrayKey,
      label: arrayRows.find(row => row.arrayLabel)?.arrayLabel || arrayKey,
      rows: arrayRows,
    })
  })
  return groups
})

const staleMainRows = computed(() => mainFieldRows.value.filter(row => row.stale))
const activeMainRows = computed(() => mainFieldRows.value.filter(row => !row.stale))

function findSavedFieldPermission(field) {
  const direct = savedPermissions.value.get(field.permissionKey)
  if (direct)
    return direct
  if (field.scope !== 'child')
    return null
  for (const saved of savedPermissions.value.values()) {
    if (samePermissionRow(field, saved))
      return saved
  }
  return null
}

function samePermissionRow(row, saved) {
  if (!row || !saved)
    return false
  if (row.permissionKey && row.permissionKey === saved.permissionKey)
    return true
  return row.scope === 'child'
    && saved.scope === 'child'
    && String(row.childField || row.field || '') === String(saved.childField || saved.field || '')
    && childTableKeysAlias(row.childKey, saved.childKey)
}

const childPermissionRows = computed(() => {
  // 只展示当前字段目录里仍存在的子表，已删除子表的历史行操作配置直接丢弃
  return catalogChildKeys.value.map((childKey) => {
    const sample = formFields.value.find(field => field.scope === 'child' && childTableKeysAlias(field.childKey, childKey))
    const saved = savedPermissionBundle.value.children.find(child => childTableKeysAlias(child.childKey, childKey))
    return {
      ...normalizeFlowChildPermission({
        childKey,
        label: sample?.childLabel || sample?.relationName || saved?.label || childKey,
        ...(saved || {}),
        childKey,
      }),
      fieldCount: formFields.value.filter(field => field.scope === 'child' && childTableKeysAlias(field.childKey, childKey)).length,
    }
  })
})

const arrayPermissionRows = computed(() => {
  return catalogArrayKeys.value.map((arrayKey) => {
    const sample = formFields.value.find(field => field.scope === 'array' && field.arrayKey === arrayKey)
    const saved = savedPermissionBundle.value.arrays.find(array => array.arrayKey === arrayKey)
    return {
      ...normalizeFlowArrayPermission({
        arrayKey,
        label: sample?.arrayLabel || saved?.label || arrayKey,
        ...(saved || {}),
        arrayKey,
      }),
      fieldCount: formFields.value.filter(field => field.scope === 'array' && field.arrayKey === arrayKey).length,
    }
  })
})

const configuredCount = computed(() => {
  const fieldCount = rows.value.filter(row => row.configured && (row.readable === false || row.writable === false || row.required === true)).length
  const childCount = childPermissionRows.value.filter(row => row.allowCreate || row.allowUpdate || row.allowDelete || row.readable === false).length
  const arrayCount = arrayPermissionRows.value.filter(row => row.allowCreate || row.allowDelete || row.readable === false || row.allowUpdate === false).length
  return fieldCount + childCount + arrayCount
})

const hasAnyFieldRows = computed(() => rows.value.length > 0)

function childFieldCaption(row) {
  if (row?.scope === 'child')
    return row.field || row.childField || ''
  if (row?.scope === 'array')
    return `${row.arrayKey}[].${row.field}`
  return row?.field || ''
}

function update(permissionKey, patch) {
  if (props.readonly)
    return
  const nextRows = rows.value.map((row) => {
    if (row.permissionKey !== permissionKey)
      return row
    const next = { ...row, ...patch }

    if (Object.prototype.hasOwnProperty.call(patch, 'readable')) {
      next.readable = patch.readable === true
      next.visible = next.readable
      if (!next.readable) {
        next.writable = false
        next.editable = false
        next.required = false
      }
    }

    if (Object.prototype.hasOwnProperty.call(patch, 'writable')) {
      next.writable = patch.writable === true
      next.editable = next.writable
      if (next.writable) {
        next.readable = true
        next.visible = true
      }
      else {
        next.required = false
      }
    }

    if (Object.prototype.hasOwnProperty.call(patch, 'required')) {
      next.required = patch.required === true
      if (next.required) {
        next.readable = true
        next.visible = true
        next.writable = true
        next.editable = true
      }
    }

    if (next.readable === false) {
      next.writable = false
      next.editable = false
      next.required = false
    }
    if (next.writable === true || next.required === true) {
      next.readable = true
      next.visible = true
    }
    if (next.required === true) {
      next.writable = true
      next.editable = true
    }
    next.visible = next.readable
    next.editable = next.writable
    return next
  })
  emitPermissions(nextRows, childPermissionRows.value, arrayPermissionRows.value)
}

function updateChild(childKey, patch) {
  if (props.readonly)
    return
  const nextChildren = childPermissionRows.value.map((child) => {
    if (child.childKey !== childKey)
      return child
    const next = { ...child, ...patch }
    if (next.readable === false) {
      next.allowCreate = false
      next.allowUpdate = false
      next.allowDelete = false
    }
    if (next.allowCreate || next.allowUpdate || next.allowDelete)
      next.readable = true
    return normalizeFlowChildPermission(next)
  })
  emitPermissions(rows.value, nextChildren, arrayPermissionRows.value)
}

function updateArray(arrayKey, patch) {
  if (props.readonly)
    return
  const nextArrays = arrayPermissionRows.value.map((array) => {
    if (array.arrayKey !== arrayKey)
      return array
    const next = { ...array, ...patch, configured: true }
    if (next.readable === false) {
      next.allowCreate = false
      next.allowUpdate = false
      next.allowDelete = false
    }
    if (next.allowCreate || next.allowUpdate || next.allowDelete)
      next.readable = true
    return normalizeFlowArrayPermission(next)
  })
  emitPermissions(rows.value, childPermissionRows.value, nextArrays)
}

function emitPermissions(fields, children, arrays) {
  const normalizedChildren = children.map(normalizeFlowChildPermission)
  const normalizedArrays = arrays.map(normalizeFlowArrayPermission)
  emit('update:config', {
    formFieldPermissions: serializeFlowFormPermissions(fields, normalizedChildren, normalizedArrays),
    formChildPermissions: normalizedChildren,
    formArrayPermissions: normalizedArrays,
  })
}
</script>

<template>
  <div class="form-permission-config">
    <div class="form-permission-head">
      <div>
        <div class="form-permission-title">
          表单字段权限
        </div>
        <div class="form-permission-desc">
          主表与子表分开配置该审批节点的可见、可编辑和必填；子表行操作仅针对当前表单仍存在的子表。
        </div>
      </div>
      <n-tag size="small" :type="configuredCount > 0 ? 'info' : 'default'" :bordered="false">
        {{ configuredCount > 0 ? `${configuredCount} 项已调整` : '默认全量可写' }}
      </n-tag>
    </div>

    <div v-if="!hasAnyFieldRows" class="form-permission-empty">
      <i class="i-material-symbols:dynamic-form" />
      <div>
        <div class="form-permission-empty-title">
          暂无可配置字段
        </div>
        <div class="form-permission-empty-desc">
          请先为流程选择动态表单，或从应用中心带业务对象进入流程设计器。
        </div>
      </div>
    </div>

    <template v-else>
      <div v-if="activeMainRows.length || staleMainRows.length" class="permission-section">
        <div class="permission-section-title">
          主表字段
        </div>
        <div class="form-permission-table">
          <div class="form-permission-row is-head">
            <span>字段</span>
            <span>可见</span>
            <span>可编辑</span>
            <span>必填</span>
          </div>
          <div
            v-for="row in activeMainRows"
            :key="row.permissionKey"
            class="form-permission-row"
          >
            <div class="form-field-cell">
              <div class="form-field-label">
                {{ row.label || row.field }}
                <n-tag v-if="row.sourceRequired" size="tiny" type="warning" :bordered="false">
                  表单必填
                </n-tag>
              </div>
              <div class="form-field-code">
                {{ childFieldCaption(row) }}
              </div>
            </div>
            <n-checkbox
              data-test="permission-readable"
              :checked="row.readable"
              :disabled="readonly"
              @update:checked="update(row.permissionKey, { readable: $event })"
            />
            <n-checkbox
              data-test="permission-writable"
              :checked="row.writable"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { writable: $event })"
            />
            <n-checkbox
              data-test="permission-required"
              :checked="row.required"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { required: $event })"
            />
          </div>
          <div
            v-for="row in staleMainRows"
            :key="row.permissionKey"
            class="form-permission-row stale"
          >
            <div class="form-field-cell">
              <div class="form-field-label">
                {{ row.label || row.field }}
                <n-tag size="tiny" type="default" :bordered="false">
                  历史字段
                </n-tag>
              </div>
              <div class="form-field-code">
                {{ childFieldCaption(row) }}
              </div>
            </div>
            <n-checkbox
              data-test="permission-readable"
              :checked="row.readable"
              :disabled="readonly"
              @update:checked="update(row.permissionKey, { readable: $event })"
            />
            <n-checkbox
              data-test="permission-writable"
              :checked="row.writable"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { writable: $event })"
            />
            <n-checkbox
              data-test="permission-required"
              :checked="row.required"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { required: $event })"
            />
          </div>
        </div>
      </div>

      <div
        v-for="group in childFieldGroups"
        :key="`child-fields-${group.childKey}`"
        class="permission-section"
      >
        <div class="permission-section-title">
          子表字段 · {{ group.label }}
        </div>
        <div class="form-permission-table">
          <div class="form-permission-row is-head">
            <span>字段</span>
            <span>可见</span>
            <span>可编辑</span>
            <span>必填</span>
          </div>
          <div
            v-for="row in group.rows"
            :key="row.permissionKey"
            class="form-permission-row"
            :class="{ stale: row.stale }"
          >
            <div class="form-field-cell">
              <div class="form-field-label">
                {{ row.label || row.field }}
                <n-tag v-if="row.sourceRequired" size="tiny" type="warning" :bordered="false">
                  表单必填
                </n-tag>
                <n-tag v-if="row.stale" size="tiny" type="default" :bordered="false">
                  历史字段
                </n-tag>
              </div>
              <div class="form-field-code">
                {{ childFieldCaption(row) }}
              </div>
            </div>
            <n-checkbox
              data-test="permission-readable"
              :checked="row.readable"
              :disabled="readonly"
              @update:checked="update(row.permissionKey, { readable: $event })"
            />
            <n-checkbox
              data-test="permission-writable"
              :checked="row.writable"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { writable: $event })"
            />
            <n-checkbox
              data-test="permission-required"
              :checked="row.required"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { required: $event })"
            />
          </div>
        </div>
      </div>

      <div
        v-for="group in arrayFieldGroups"
        :key="`array-fields-${group.arrayKey}`"
        class="permission-section"
      >
        <div class="permission-section-title">
          数组明细 · {{ group.label }}
        </div>
        <div class="form-permission-table">
          <div class="form-permission-row is-head">
            <span>字段</span>
            <span>可见</span>
            <span>可编辑</span>
            <span>必填</span>
          </div>
          <div
            v-for="row in group.rows"
            :key="row.permissionKey"
            class="form-permission-row"
            :class="{ stale: row.stale }"
          >
            <div class="form-field-cell">
              <div class="form-field-label">
                {{ row.label || row.field }}
                <n-tag v-if="row.sourceRequired" size="tiny" type="warning" :bordered="false">
                  表单必填
                </n-tag>
              </div>
              <div class="form-field-code">
                {{ childFieldCaption(row) }}
              </div>
            </div>
            <n-checkbox
              data-test="permission-readable"
              :checked="row.readable"
              :disabled="readonly"
              @update:checked="update(row.permissionKey, { readable: $event })"
            />
            <n-checkbox
              data-test="permission-writable"
              :checked="row.writable"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { writable: $event })"
            />
            <n-checkbox
              data-test="permission-required"
              :checked="row.required"
              :disabled="readonly || !row.readable"
              @update:checked="update(row.permissionKey, { required: $event })"
            />
          </div>
        </div>
      </div>

      <div v-if="childPermissionRows.length" class="row-permission">
        <div class="row-permission-title">
          子表行操作
        </div>
        <div v-for="child in childPermissionRows" :key="child.childKey" class="row-permission-card">
          <div class="row-permission-meta">
            <span class="row-permission-name">{{ child.label || child.childKey }}</span>
            <span class="row-permission-count">{{ child.fieldCount }} 个字段</span>
          </div>
          <div class="row-permission-actions">
            <label><n-checkbox :checked="child.readable" :disabled="readonly" @update:checked="updateChild(child.childKey, { readable: $event })" /><span>可见</span></label>
            <label><n-checkbox data-test="child-allow-create" :checked="child.allowCreate" :disabled="readonly || !child.readable" @update:checked="updateChild(child.childKey, { allowCreate: $event })" /><span>新增</span></label>
            <label><n-checkbox data-test="child-allow-update" :checked="child.allowUpdate" :disabled="readonly || !child.readable" @update:checked="updateChild(child.childKey, { allowUpdate: $event })" /><span>修改</span></label>
            <label><n-checkbox data-test="child-allow-delete" :checked="child.allowDelete" :disabled="readonly || !child.readable" @update:checked="updateChild(child.childKey, { allowDelete: $event })" /><span>删除</span></label>
          </div>
        </div>
      </div>

      <div v-if="arrayPermissionRows.length" class="row-permission">
        <div class="row-permission-title">
          数组明细行操作
        </div>
        <div v-for="array in arrayPermissionRows" :key="array.arrayKey" class="row-permission-card">
          <div class="row-permission-meta">
            <span class="row-permission-name">{{ array.label || array.arrayKey }}</span>
            <span class="row-permission-count">{{ array.fieldCount }} 个行字段</span>
          </div>
          <div class="row-permission-actions">
            <label><n-checkbox :checked="array.readable" :disabled="readonly" @update:checked="updateArray(array.arrayKey, { readable: $event })" /><span>可见</span></label>
            <label><n-checkbox data-test="array-allow-create" :checked="array.allowCreate" :disabled="readonly || !array.readable" @update:checked="updateArray(array.arrayKey, { allowCreate: $event })" /><span>新增</span></label>
            <label><n-checkbox data-test="array-allow-update" :checked="array.allowUpdate" :disabled="readonly || !array.readable" @update:checked="updateArray(array.arrayKey, { allowUpdate: $event })" /><span>修改</span></label>
            <label><n-checkbox data-test="array-allow-delete" :checked="array.allowDelete" :disabled="readonly || !array.readable" @update:checked="updateArray(array.arrayKey, { allowDelete: $event })" /><span>删除</span></label>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.form-permission-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-permission-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.form-permission-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.form-permission-desc {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.form-permission-empty {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px dashed var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  padding: 14px;
  color: var(--text-secondary);
}

.form-permission-empty i {
  margin-top: 2px;
  color: #2563eb;
  font-size: 20px;
}

.form-permission-empty-title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.form-permission-empty-desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
}

.permission-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.permission-section-title {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
}

.form-permission-table {
  border: 1px solid var(--border-light);
  border-radius: 8px;
  overflow: hidden;
}

.form-permission-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 64px 64px 64px;
  align-items: center;
  gap: 8px;
  min-height: 54px;
  padding: 8px 12px;
  background: var(--bg-primary);
  border-top: 1px solid #eef2f7;
}

.row-permission-title {
  margin-bottom: 6px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
}

.row-permission-card {
  border: 1px solid var(--border-light);
  border-radius: 4px;
  background: var(--bg-primary);
  padding: 8px 10px;
}

.row-permission-card + .row-permission-card {
  margin-top: 6px;
}

.row-permission-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.row-permission-name {
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-permission-count {
  flex: none;
  color: var(--text-tertiary);
  font-size: 11px;
}

.row-permission-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: 8px;
}

.row-permission-actions label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.form-permission-row:first-child {
  border-top: none;
}

.form-permission-row.is-head {
  min-height: 36px;
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.form-permission-row:not(.is-head):hover {
  background: var(--bg-secondary);
}

.form-permission-row.stale {
  background: #fffbeb;
}

.form-field-cell {
  min-width: 0;
}

.form-field-label {
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.form-field-code {
  margin-top: 3px;
  color: var(--text-tertiary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  word-break: break-all;
}
</style>
