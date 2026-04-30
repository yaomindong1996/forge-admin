<template>
  <div class="nexus-role-page">
    <!-- Page Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">
          Identity & Roles
        </h1>
        <p class="page-subtitle">
          Manage granular access controls and identity policies across all environments.
        </p>
      </div>
      <div class="page-actions">
        <NButton size="small" secondary>
          <template #icon>
            <i class="i-material-symbols:upload" />
          </template>
          Import Policy
        </NButton>
        <NButton type="primary" size="small" class="btn-primary-glow">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          Create Role
        </NButton>
      </div>
    </div>

    <!-- Bento Stats Grid -->
    <div class="bento-grid">
      <div class="bento-card">
        <div class="bento-card-header">
          <div class="bento-card-icon" style="background: var(--primary-50);">
            <i class="i-material-symbols:shield" style="color: var(--primary-500);" />
          </div>
        </div>
        <div class="bento-card-body">
          <div class="bento-value">
            24
          </div>
          <div class="bento-label">
            <span>Active Roles</span>
            <span class="bento-sub">+3 this week</span>
          </div>
        </div>
      </div>

      <div class="bento-card">
        <div class="bento-card-header">
          <div class="bento-card-icon" style="background: #dcfce7;">
            <i class="i-material-symbols:group" style="color: #16a34a;" />
          </div>
        </div>
        <div class="bento-card-body">
          <div class="bento-value">
            1,492
          </div>
          <div class="bento-label">
            <span>Assignments</span>
            <span class="bento-sub">98% utilization</span>
          </div>
        </div>
      </div>

      <div class="bento-card">
        <div class="bento-card-header">
          <div class="bento-card-icon" style="background: var(--bg-secondary);">
            <i class="i-material-symbols:shield-lock" style="color: var(--text-tertiary);" />
          </div>
        </div>
        <div class="bento-card-body">
          <div class="bento-value">
            0
          </div>
          <div class="bento-label">
            <span>Security Flags</span>
            <span class="bento-sub">All policies passing</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Table Controls -->
    <div class="table-controls">
      <div class="filter-pills">
        <NButton
          v-for="pill in filterPills"
          :key="pill.label"
          :type="pill.active ? 'primary' : 'default'"
          size="small"
          :quill="!pill.active"
          :secondary="!pill.active"
          class="filter-pill-btn"
          :class="{ 'filter-pill-active': pill.active }"
          @click="activeFilter = pill.label"
        >
          {{ pill.label }}
          <NTag v-if="pill.badge" size="small" :type="pill.active ? 'primary' : 'default'" class="pill-badge">
            {{ pill.badge }}
          </NTag>
        </NButton>
      </div>

      <div class="control-actions">
        <NInput
          v-model:value="searchValue"
          placeholder="Filter roles..."
          size="small"
          class="search-input"
          round
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </NInput>
        <NButton size="small" quaternary circle>
          <template #icon>
            <i class="i-material-symbols:monitoring" />
          </template>
        </NButton>
      </div>
    </div>

    <!-- Grid Header -->
    <div class="grid-header">
      <div class="col col-name">
        Identity Policy
      </div>
      <div class="col col-access">
        Access Level
      </div>
      <div class="col col-users">
        Users Attached
      </div>
      <div class="col col-status">
        Status
      </div>
      <div class="col col-actions">
        Actions
      </div>
    </div>

    <!-- Role Rows -->
    <div class="role-rows">
      <div
        v-for="role in roleData"
        :key="role.id"
        class="role-row"
        :class="{ 'is-system': role.isSystem }"
      >
        <div class="col col-name">
          <div class="name-cell">
            <div class="name-icon">
              <i class="i-material-symbols:fingerprint" />
            </div>
            <div class="name-info">
              <div class="name-row">
                <span class="name-text">{{ role.name }}</span>
                <NTag v-if="role.isSystem" size="tiny" type="primary" class="system-tag">
                  System
                </NTag>
              </div>
              <div class="name-id">
                {{ role.id }}
              </div>
              <div class="name-desc">
                {{ role.description }}
              </div>
            </div>
          </div>
        </div>

        <div class="col col-access">
          <div class="access-level">
            {{ role.accessLevel }}
          </div>
          <div class="access-tags">
            <NTag v-for="(perm, i) in role.permissions" :key="i" size="small" class="perm-tag">
              {{ perm }}
            </NTag>
          </div>
        </div>

        <div class="col col-users">
          <div v-if="role.userCount > 0" class="user-avatars">
            <div class="avatar-stack">
              <img
                v-for="(avatar, i) in role.avatars.slice(0, 3)"
                :key="i"
                :src="avatar"
                alt="User"
                class="mini-avatar"
              >
            </div>
            <span v-if="role.userCount > 3" class="user-count">+{{ role.userCount - 3 }}</span>
          </div>
          <span v-else class="no-users">None</span>
        </div>

        <div class="col col-status">
          <div class="status-cell">
            <span class="status-dot" :class="role.statusColor" />
            <span class="status-text">{{ role.status }}</span>
          </div>
        </div>

        <div class="col col-actions">
          <div class="action-buttons">
            <NButton size="tiny" quaternary circle class="action-btn">
              <template #icon>
                <i class="i-material-symbols:edit" />
              </template>
            </NButton>
            <NButton size="tiny" quaternary circle>
              <template #icon>
                <i class="i-material-symbols:more-horiz" />
              </template>
            </NButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { NButton, NInput, NTag } from 'naive-ui'
import { ref } from 'vue'

const searchValue = ref('')
const activeFilter = ref('All Roles')

const filterPills = [
  { label: 'All Roles', active: true },
  { label: 'System', active: false },
  { label: 'Custom', active: false },
  { label: 'Needs Review', active: false, badge: '2' },
]

const roleData = [
  {
    name: 'Super Admin',
    id: 'role_adm_99x2',
    description: 'Full system control across all regions.',
    accessLevel: 'God Mode',
    permissions: ['All Modules', 'Billing', 'Keys'],
    userCount: 4,
    avatars: [
      'https://i.pravatar.cc/150?u=1',
      'https://i.pravatar.cc/150?u=2',
      'https://i.pravatar.cc/150?u=3',
    ],
    status: 'Active',
    statusColor: 'emerald',
    isSystem: true,
  },
  {
    name: 'Platform Engineer',
    id: 'role_eng_22p4',
    description: 'Infrastructure management and deployments.',
    accessLevel: 'Elevated',
    permissions: ['CI/CD', 'Databases', '+12'],
    userCount: 18,
    avatars: [
      'https://i.pravatar.cc/150?u=4',
      'https://i.pravatar.cc/150?u=5',
      'https://i.pravatar.cc/150?u=6',
    ],
    status: 'Active',
    statusColor: 'emerald',
    isSystem: false,
  },
  {
    name: 'Data Analyst',
    id: 'role_dat_88k1',
    description: 'Read-only access to metrics and reports.',
    accessLevel: 'Read Only',
    permissions: ['Metrics', 'Exports'],
    userCount: 42,
    avatars: [
      'https://i.pravatar.cc/150?u=7',
      'https://i.pravatar.cc/150?u=8',
      'https://i.pravatar.cc/150?u=9',
    ],
    status: 'Review',
    statusColor: 'amber',
    isSystem: false,
  },
  {
    name: 'Contractor',
    id: 'role_cnt_01x8',
    description: 'Time-bound temporary access.',
    accessLevel: 'Restricted',
    permissions: ['Jira', 'Wiki'],
    userCount: 0,
    avatars: [],
    status: 'Inactive',
    statusColor: 'slate',
    isSystem: false,
  },
]
</script>

<style scoped>
.nexus-role-page {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

/* Page Header */
.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: 1.1;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin: 4px 0 0;
}

.page-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn-primary-glow {
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.2) !important;
}

/* Bento Grid */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.bento-card {
  padding: 16px;
  background: var(--bg-primary);
  border: 1px solid var(--nexus-border);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
  transition: border-color var(--transition-base);
}

.bento-card:hover {
  border-color: var(--border-default);
}

.bento-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.bento-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-light);
  font-size: 18px;
}

.bento-card-body {
  margin-top: 16px;
}

.bento-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: 1;
  margin-bottom: 4px;
}

.bento-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.bento-label > span:first-child {
  font-weight: 600;
  color: var(--text-secondary);
}

.bento-sub {
  color: var(--text-tertiary);
  font-weight: 500;
}

.bento-sub::before {
  content: '•';
  margin-right: 2px;
}

/* Table Controls */
.table-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--nexus-border);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  margin-bottom: 12px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.filter-pills {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  padding: 0 4px;
}

.filter-pills :deep(.n-button) {
  height: 32px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 6px;
  border: 1px solid transparent;
  white-space: nowrap;
}

.filter-pill-active {
  background: var(--nexus-active-bg) !important;
  border-color: var(--primary-500) !important;
  color: var(--nexus-active-text) !important;
}

.pill-badge {
  margin-left: 4px;
  font-size: 10px;
  font-weight: 700;
  padding: 0 4px;
  height: 14px;
  line-height: 14px;
}

.filter-pill-active .pill-badge {
  background: rgba(22, 93, 255, 0.15) !important;
  color: var(--nexus-active-text) !important;
  border: none !important;
}

.control-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-right: 8px;
}

.search-input {
  width: 180px;
}

.search-input :deep(.n-input__input-el) {
  font-size: 12px;
  font-weight: 500;
}

/* Grid Header */
.grid-header {
  display: grid;
  grid-template-columns: 4fr 3fr 2fr 2fr 1fr;
  gap: 12px;
  padding: 6px 16px;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 8px;
}

/* Role Rows */
.role-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-row {
  display: grid;
  grid-template-columns: 4fr 3fr 2fr 2fr 1fr;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-primary);
  border: 1px solid var(--nexus-border);
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
  transition: all var(--transition-base);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.role-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 0;
  background: var(--primary-500);
  transition: width var(--transition-base);
}

.role-row.is-system::before {
  width: 3px;
}

.role-row:hover {
  background: var(--nexus-hover-bg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.col {
  min-width: 0;
}

/* Name Cell */
.name-cell {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.name-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
  color: var(--text-tertiary);
  transition: color var(--transition-base);
}

.role-row:hover .name-icon {
  color: var(--primary-500);
}

.name-info {
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.name-text {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  transition: color var(--transition-base);
}

.role-row:hover .name-text {
  color: var(--primary-600);
}

.system-tag {
  font-size: 9px !important;
  font-weight: 700 !important;
  padding: 0 4px !important;
  line-height: 14px !important;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.name-id {
  font-size: 11px;
  font-family: var(--font-family-mono);
  color: var(--text-tertiary);
  margin-bottom: 2px;
}

.name-desc {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Access */
.access-level {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.access-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.perm-tag {
  font-size: 10px;
  font-weight: 500;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
  border-radius: 4px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  color: var(--text-secondary);
}

/* Users */
.user-avatars {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar-stack {
  display: flex;
  margin-right: -6px;
}

.mini-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid var(--bg-primary);
  box-shadow: 0 0 0 1px var(--border-light);
}

.user-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.no-users {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 500;
}

/* Status */
.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.emerald {
  background: #22c55e;
}

.status-dot.amber {
  background: #f59e0b;
}

.status-dot.slate {
  background: #94a3b8;
}

.status-text {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
}

/* Actions */
.action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  padding-right: 4px;
}

.action-btn {
  opacity: 0;
  transition: opacity var(--transition-base);
}

.role-row:hover .action-btn {
  opacity: 1;
}

/* Responsive */
@media (max-width: 1024px) {
  .bento-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid-header,
  .role-row {
    grid-template-columns: 3fr 3fr 2fr 2fr 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .bento-grid {
    grid-template-columns: 1fr;
  }

  .grid-header {
    display: none;
  }

  .role-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .table-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .control-actions {
    justify-content: flex-end;
  }
}
</style>
