<script setup>
import { useRouter } from 'vue-router'
import HomeActionIcon from './HomeActionIcon.vue'
import HomeSection from './HomeSection.vue'

const router = useRouter()
const entries = [
  { title: '用户管理', desc: '账户信息', icon: 'user', path: '/system/user' },
  { title: '角色管理', desc: '权限分配', icon: 'role', path: '/system/role' },
  { title: '组织管理', desc: '部门成员', icon: 'organization', path: '/system/org' },
  { title: '菜单管理', desc: '导航资源', icon: 'menu', path: '/system/menu' },
  { title: '岗位管理', desc: '岗位职责', icon: 'post', path: '/system/post' },
  { title: '文件中心', desc: '附件管理', icon: 'files', path: '/system/file-list' },
]
</script>

<template>
  <HomeSection title="快捷入口" description="常用系统管理工作区" icon="apps">
    <!-- 原有六个系统入口，不新增虚假菜单 -->
    <nav class="quick-entry-list" aria-label="快捷入口">
      <button
        v-for="item in entries" :key="item.path" type="button" class="quick-entry"
        @click="router.push(item.path)"
      >
        <span class="quick-entry-icon"><HomeActionIcon :name="item.icon" :size="24" /></span>
        <span class="quick-entry-copy"><strong>{{ item.title }}</strong><span>{{ item.desc }}</span></span>
      </button>
    </nav>
  </HomeSection>
</template>

<style scoped>
.quick-entry-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 12px;
}
.quick-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 64px;
  padding: 10px;
  border: 1px solid var(--home-border, #e5e7eb);
  border-radius: var(--home-radius-sm, 6px);
  background: var(--home-panel, #fff);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background 160ms ease,
    border-color 160ms ease;
}
.quick-entry-icon {
  display: inline-flex;
  color: var(--home-brand, #0e42d2);
}
.quick-entry-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}
.quick-entry-copy strong {
  font-size: 12px;
  line-height: 18px;
  font-weight: 500;
}
.quick-entry-copy > span {
  color: var(--home-muted, #86909c);
  font-size: 11px;
  line-height: 16px;
}
.quick-entry-copy strong,
.quick-entry-copy > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.quick-entry:hover {
  background: var(--home-brand-soft, #f2f6ff);
  border-color: var(--home-brand-border, #bfd0ff);
}
.quick-entry:focus-visible {
  outline: 2px solid var(--home-brand, #0e42d2);
  outline-offset: 1px;
}
</style>
