<template>
  <section class="settings-section-card">
    <header>
      <h2>应用权限</h2>
      <p>应用可见范围控制谁能打开应用；页面入口和对象数据范围按角色配置。</p>
    </header>
    <n-alert type="info" :bordered="false" class="settings-info-alert">
      应用管理员自动拥有所有页面权限。保存后需重新发布，正式门户和工作台才使用新的可见范围。
    </n-alert>
    <n-form label-placement="top">
      <n-form-item label="可见范围">
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
      <n-form-item v-if="permission.visibility === 'roles'" label="可见角色">
        <n-select
          :value="permission.roleIds"
          :options="roleOptions"
          :loading="roleLoading"
          multiple
          filterable
          placeholder="选择可见角色"
          @update:value="patch({ roleIds: normalizeIds($event) })"
        />
      </n-form-item>
      <n-form-item v-if="permission.visibility === 'departments'" label="可见部门">
        <n-tree-select
          :value="permission.departmentIds"
          :options="orgOptions"
          :loading="orgLoading"
          multiple
          filterable
          check-strategy="all"
          key-field="key"
          label-field="label"
          placeholder="选择可见部门"
          @update:value="patch({ departmentIds: normalizeIds($event) })"
        />
      </n-form-item>
      <n-form-item v-if="permission.visibility === 'users'" label="可见用户">
        <UserSelectPicker
          :model-value="permission.userIds"
          :label-value="permission.userLabels"
          multiple
          placeholder="选择可见用户"
          @update:model-value="patch({ userIds: normalizeIds($event) })"
          @update:label-value="patch({ userLabels: $event })"
        />
      </n-form-item>
    </n-form>

    <div class="settings-page-permission">
      <h3>页面与数据权限</h3>
      <ApplicationPermissionsPanel
        v-if="application?.applicationCode"
        :application="application"
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
.settings-page-permission {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.settings-page-permission h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.settings-info-alert {
  margin-bottom: 16px;
}
</style>
