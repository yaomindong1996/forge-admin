<template>
  <section class="settings-section-card app-permission-settings">
    <header>
      <h2>应用权限</h2>
      <p>分两步配置：先决定谁能打开应用，再为角色勾选打开后能看到的页面与功能。</p>
    </header>

    <!-- 步骤 1：门禁 -->
    <div class="permission-step" data-permission-step="access">
      <header class="permission-step-head">
        <span class="permission-step-index">1</span>
        <div>
          <h3>谁可以打开这个应用</h3>
          <p>控制能否进入应用门户。改完后需重新发布，正式门户才会生效。</p>
        </div>
      </header>

      <n-alert type="info" :bordered="false" class="settings-info-alert">
        应用管理员自动拥有全部页面权限，不受步骤 2 勾选限制。
      </n-alert>

      <n-form label-placement="top">
        <n-form-item label="打开范围">
          <n-radio-group :value="permission.visibility" @update:value="patch({ visibility: $event })">
            <n-radio-button value="all">
              组织全员
            </n-radio-button>
            <n-radio-button value="roles">
              指定角色
            </n-radio-button>
            <n-radio-button value="departments">
              指定部门
            </n-radio-button>
            <n-radio-button value="users">
              指定用户
            </n-radio-button>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="应用管理员">
          <UserSelectPicker
            :model-value="permission.administrators"
            :label-value="permission.administratorLabels"
            multiple
            placeholder="选择应用管理员"
            @update:model-value="patch({ administrators: normalizeIds($event) })"
            @update:label-value="patch({ administratorLabels: $event })"
          />
        </n-form-item>
        <n-form-item v-if="permission.visibility === 'roles'" label="可打开应用的角色">
          <n-select
            :value="permission.roleIds"
            :options="roleOptions"
            :loading="roleLoading"
            multiple
            filterable
            placeholder="选择可以打开本应用的角色"
            @update:value="patch({ roleIds: normalizeIds($event) })"
          />
        </n-form-item>
        <n-form-item v-if="permission.visibility === 'departments'" label="可打开应用的部门">
          <n-tree-select
            :value="permission.departmentIds"
            :options="orgOptions"
            :loading="orgLoading"
            multiple
            filterable
            check-strategy="all"
            key-field="key"
            label-field="label"
            placeholder="选择可打开应用的部门"
            @update:value="patch({ departmentIds: normalizeIds($event) })"
          />
        </n-form-item>
        <n-form-item v-if="permission.visibility === 'users'" label="可打开应用的用户">
          <UserSelectPicker
            :model-value="permission.userIds"
            :label-value="permission.userLabels"
            multiple
            placeholder="选择可打开应用的用户"
            @update:model-value="patch({ userIds: normalizeIds($event) })"
            @update:label-value="patch({ userLabels: $event })"
          />
        </n-form-item>
      </n-form>
    </div>

    <!-- 步骤 2：进门后页面 RBAC -->
    <div class="permission-step" data-permission-step="pages">
      <header class="permission-step-head">
        <span class="permission-step-index">2</span>
        <div>
          <h3>打开后能看哪些页面</h3>
          <p>按角色勾选页面与功能。只勾功能、不勾页面时，门户左侧菜单仍为空。</p>
        </div>
      </header>

      <ApplicationPermissionsPanel
        v-if="application?.applicationCode"
        :application="application"
        :preferred-role-ids="preferredPageGrantRoleIds"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import { request } from '@/utils/http'
import ApplicationPermissionsPanel from '../../application-workspace/ApplicationPermissionsPanel.vue'

const props = defineProps({
  modelValue: { type: Object, required: true },
  application: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])
const permission = computed(() => props.modelValue.permission || {})
const roleOptions = ref([])
const orgOptions = ref([])
const roleLoading = ref(false)
const orgLoading = ref(false)

/** 步骤 1 指定「可打开应用的角色」时，步骤 2 优先为这些角色配页面 */
const preferredPageGrantRoleIds = computed(() => {
  if (String(permission.value.visibility || 'all').toLowerCase() !== 'roles')
    return []
  return normalizeIds(permission.value.roleIds)
})

onMounted(() => {
  loadRoles()
  loadOrgs()
})

function patch(value) {
  emit('update:modelValue', {
    ...props.modelValue,
    permission: { ...permission.value, ...value },
  })
}

function normalizeIds(values) {
  return [...new Set((Array.isArray(values) ? values : []).map(value => String(value || '').trim()).filter(Boolean))]
}

async function loadRoles() {
  roleLoading.value = true
  try {
    const response = await request.get('/system/role/page', { params: { pageNum: 1, pageSize: 200 } })
    const records = response.data?.records || response.data?.rows || response.data || []
    roleOptions.value = (Array.isArray(records) ? records : []).map(item => ({
      label: `${item.roleName || item.roleKey || item.id}${item.roleKey ? ` · ${item.roleKey}` : ''}`,
      value: String(item.id || item.roleId),
    })).filter(item => item.value)
  }
  catch {
    roleOptions.value = []
  }
  finally {
    roleLoading.value = false
  }
}

async function loadOrgs() {
  orgLoading.value = true
  try {
    const response = await request.get('/system/org/tree')
    orgOptions.value = normalizeOrgTree(response.data || [])
  }
  catch {
    orgOptions.value = []
  }
  finally {
    orgLoading.value = false
  }
}

function normalizeOrgTree(nodes) {
  return (Array.isArray(nodes) ? nodes : []).map(node => ({
    key: String(node.id || node.orgId || ''),
    label: node.orgName || node.label || node.name || String(node.id || ''),
    children: normalizeOrgTree(node.children || []),
  })).filter(node => node.key)
}
</script>

<style scoped>
.permission-step {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid var(--border-light, #e2e8f0);
  border-radius: 10px;
  background: var(--card-color, #fff);
}

.permission-step + .permission-step {
  margin-top: 18px;
}

.permission-step-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.permission-step-index {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.12);
  color: var(--primary-color, #2563eb);
  font-size: 13px;
  font-weight: 700;
}

.permission-step-head h3 {
  margin: 0 0 4px;
  color: var(--text-color-1, #0f172a);
  font-size: 15px;
}

.permission-step-head p {
  margin: 0;
  color: var(--text-color-3, #64748b);
  font-size: 12px;
  line-height: 1.55;
}

.settings-info-alert {
  margin-bottom: 16px;
}
</style>
