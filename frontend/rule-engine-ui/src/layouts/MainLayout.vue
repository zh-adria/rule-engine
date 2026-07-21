<template>
  <div class="re-app-shell">
    <aside
      class="re-sidebar"
      :class="{ 'is-collapsed': appStore.sidebarCollapsed }"
    >
      <div class="re-sidebar__brand">
        <span v-if="!appStore.sidebarCollapsed">规则引擎平台</span>
        <span v-else>规则</span>
      </div>

      <div class="re-sidebar__menu">
        <el-menu
          :default-active="currentRoute"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          @select="navigateTo"
        >
          <el-menu-item
            v-for="item in navItems"
            :key="item.name"
            :index="item.name"
          >
            <component :is="item.icon" class="re-sidebar__icon" />
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu>
      </div>

      <div class="re-sidebar__footer">
        <div class="re-sidebar__user" v-if="!appStore.sidebarCollapsed">
          <span class="re-sidebar__user-name re-truncate">{{ authStore.displayName }}</span>
          <span class="re-sidebar__user-role re-truncate">{{ roleLabel }}</span>
        </div>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <span class="re-header__avatar">{{ initials }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <LogOut :size="14" style="margin-right: 6px" />退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <div class="re-main">
      <header class="re-header">
        <div class="re-header__left">
          <button class="re-header__icon-btn" @click="appStore.toggleSidebar()">
            <PanelLeft :size="16" />
          </button>
          <button class="re-header__icon-btn" aria-label="搜索" @click="openCommandPalette">
            <Search :size="16" />
          </button>
          <div class="re-header__brand">规则引擎工作台</div>
        </div>
        <div class="re-header__right">
          <el-tooltip content="通知" placement="bottom">
            <button class="re-header__icon-btn re-header__badge" aria-label="通知">
              <Bell :size="16" />
            </button>
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="re-header__avatar">{{ initials }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <LogOut :size="14" style="margin-right: 6px" />退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="re-workspace">
        <div class="re-content">
          <router-view />
        </div>
      </main>
    </div>

    <!-- Diff Dialog -->
    <DiffDialog />
    <CommandPalette ref="cmdPaletteRef" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { markRaw } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useAppStore } from '../stores/app'
import {
  LayoutDashboard, Settings, Layers, ShieldCheck,
  Bell, Search, LogOut, PanelLeft
} from 'lucide-vue-next'
import DiffDialog from '../components/rule/DiffDialog.vue'
import CommandPalette from '../components/common/CommandPalette.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()
const cmdPaletteRef = ref(null)

function openCommandPalette() {
  cmdPaletteRef.value?.open()
}

const currentRoute = computed(() => route.name)

const initials = computed(() => {
  const n = authStore.displayName || authStore.username || '?'
  return n.slice(0, 2).toUpperCase()
})

/* 顶级路由(排除 RuleDetail/RuleSetDetail 子路由) */
const navItems = computed(() => {
  const all = [
    { name: 'Dashboard', icon: markRaw(LayoutDashboard), title: '仪表盘' },
    {
      name: 'RuleList',
      icon: markRaw(Settings),
      title: '规则配置',
      perm: 'RULE_READ',
    },
    {
      name: 'RuleSetList',
      icon: markRaw(Layers),
      title: '规则编排',
      perm: 'RULE_SET_READ',
    },
    {
      name: 'AuditLog',
      icon: markRaw(ShieldCheck),
      title: '审计留痕',
      perm: 'AUDIT_READ',
    },
    {
      name: 'UserManage',
      icon: markRaw(Layers),
      title: '用户管理',
      adminOnly: true,
    },
    {
      name: 'CustomFieldList',
      icon: markRaw(Settings),
      title: '自定义字段',
      adminOnly: true,
    },
  ]
  return all.filter((i) => {
    if (i.perm && !authStore.hasPermission(i.perm)) return false
    if (i.adminOnly && !authStore.isAdmin) return false
    return true
  })
})

const roleLabel = computed(() => {
  const roles = authStore.roles
  if (roles.includes('ADMIN')) return '管理员'
  if (roles.includes('APPROVER')) return '审批者'
  if (roles.includes('RULE_AUTHOR')) return '规则编写'
  return '查看者'
})

function navigateTo(routeName) {
  router.push({ name: routeName })
}

function handleUserCommand(cmd) {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>
