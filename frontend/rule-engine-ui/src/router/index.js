import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'LayoutDashboard' }
      },
      {
        path: 'rules',
        name: 'RuleList',
        component: () => import('../views/rule/RuleList.vue'),
        meta: { title: '规则配置', icon: 'Settings', permission: 'RULE_READ' }
      },
      {
        path: 'rules/:ruleCode',
        name: 'RuleDetail',
        component: () => import('../views/rule/RuleDetail.vue'),
        meta: { title: '规则详情', icon: 'Settings', permission: 'RULE_READ' }
      },
      {
        path: 'rule-sets',
        name: 'RuleSetList',
        component: () => import('../views/rule-set/RuleSetList.vue'),
        meta: { title: '规则编排', icon: 'Layers', permission: 'RULE_SET_READ' }
      },
      {
        path: 'rule-sets/:setCode',
        name: 'RuleSetDetail',
        component: () => import('../views/rule-set/RuleSetDetail.vue'),
        meta: { title: '规则集详情', icon: 'Layers', permission: 'RULE_SET_READ' }
      },
      {
        path: 'audit',
        name: 'AuditLog',
        component: () => import('../views/AuditLog.vue'),
        meta: { title: '审计留痕', icon: 'ShieldCheck', permission: 'AUDIT_READ' }
      },
      {
        path: 'approvals',
        name: 'ApprovalList',
        component: () => import('../views/approval/ApprovalList.vue'),
        meta: { title: '审批管理', icon: 'CheckCircle', permission: 'APPROVAL_READ' }
      },
      {
        path: 'webhooks',
        name: 'WebhookList',
        component: () => import('../views/webhook/WebhookList.vue'),
        meta: { title: 'Webhook', icon: 'Webhook', permission: 'WEBHOOK_READ' }
      },
      {
        path: 'templates',
        name: 'TemplateList',
        component: () => import('../views/template/TemplateList.vue'),
        meta: { title: '规则模板', icon: 'Copy', permission: 'RULE_READ' }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('../views/UserManage.vue'),
        meta: { title: '用户管理', icon: 'Users', permission: 'USER_MANAGE' }
      },
      {
        path: 'custom-fields',
        name: 'CustomFieldList',
        component: () => import('../views/custom-field/CustomFieldList.vue'),
        meta: { title: '自定义字段', icon: 'Settings', permission: 'USER_MANAGE' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard - check authentication
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.public) {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  // Check permission if required
  if (to.meta.permission) {
    const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')
    if (!permissions.includes(to.meta.permission) && !permissions.includes('ADMIN')) {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
