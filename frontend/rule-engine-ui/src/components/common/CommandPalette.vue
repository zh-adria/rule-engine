<template>
  <Teleport to="body">
    <Transition name="cmd-fade">
      <div v-if="visible" class="cmd-overlay" @click.self="close">
        <div class="cmd-palette">
          <div class="cmd-palette__input-wrap">
            <Search :size="18" class="cmd-palette__icon" />
            <input
              ref="inputRef"
              v-model="query"
              class="cmd-palette__input"
              placeholder="输入命令或搜索..."
              @input="onInput"
              @keydown="onKeydown"
            />
            <kbd class="cmd-palette__esc" @click="close">ESC</kbd>
          </div>
          <div class="cmd-palette__list" :style="{ maxHeight: listHeight + 'px' }">
            <div v-if="filtered.length === 0" class="cmd-palette__empty">
              无匹配结果
            </div>
            <div
              v-for="(item, idx) in filtered"
              :key="item.key"
              :ref="(el) => { if (idx === selectedIndex) selectedRef = el }"
              class="cmd-palette__item"
              :class="{ 'is-selected': idx === selectedIndex }"
              @click="select(item)"
              @mouseenter="selectedIndex = idx"
            >
              <span class="cmd-palette__item-icon"><component :is="item.icon" :size="16" /></span>
              <div class="cmd-palette__item-body">
                <span class="cmd-palette__item-label">{{ item.label }}</span>
                <span v-if="item.hint" class="cmd-palette__item-hint">{{ item.hint }}</span>
              </div>
              <kbd v-if="item.shortcut" class="cmd-palette__shortcut">{{ item.shortcut }}</kbd>
            </div>
          </div>
          <footer class="cmd-palette__footer">
            <span><kbd>↑↓</kbd> 导航</span>
            <span><kbd>↵</kbd> 选择</span>
            <span><kbd>ESC</kbd> 关闭</span>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { Search, LayoutDashboard, Settings, Layers, ShieldCheck, Users, CheckCircle, Webhook, Plus, Rocket, Play } from 'lucide-vue-next'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const visible = ref(false)
const query = ref('')
const selectedIndex = ref(0)
const selectedRef = ref(null)
const inputRef = ref(null)
const listHeight = 320

const ALL_ITEMS = [
  { key: 'dashboard', label: '仪表盘', icon: markRaw(LayoutDashboard), route: '/dashboard', shortcut: 'G D', hint: '工作台概览' },
  { key: 'rules', label: '规则配置', icon: markRaw(Settings), route: '/rules', shortcut: 'G R', hint: '规则列表', perm: 'RULE_READ' },
  { key: 'rule-sets', label: '规则编排', icon: markRaw(Layers), route: '/rule-sets', shortcut: 'G S', hint: '规则集编排', perm: 'RULE_SET_READ' },
  { key: 'approvals', label: '审批管理', icon: markRaw(CheckCircle), route: '/approvals', shortcut: 'G A', hint: '待审批列表', perm: 'APPROVAL_READ' },
  { key: 'audit', label: '审计留痕', icon: markRaw(ShieldCheck), route: '/audit', shortcut: 'G L', hint: '审计日志', perm: 'AUDIT_READ' },
  { key: 'templates', label: '规则模板', icon: markRaw(Webhook), route: '/templates', shortcut: 'G T', hint: '模板库', perm: 'RULE_READ' },
  { key: 'custom-fields', label: '自定义字段', icon: markRaw(Settings), route: '/custom-fields', shortcut: 'G C', hint: '字段配置', perm: 'RULE_READ' },
  { key: 'webhooks', label: 'Webhook', icon: markRaw(Webhook), route: '/webhooks', shortcut: 'G W', hint: 'Webhook 管理', perm: 'WEBHOOK_READ' },
  { key: 'users', label: '用户管理', icon: markRaw(Users), route: '/users', shortcut: 'G U', hint: '用户与权限', adminOnly: true },
  { key: 'create-rule', label: '新建规则', icon: markRaw(Plus), action: () => { router.push('/rules'); setTimeout(() => { /* trigger create dialog */ }, 300) }, shortcut: 'N R' },
  { key: 'execute-rule', label: '执行规则', icon: markRaw(Play), action: () => router.push('/rules'), shortcut: 'N E', hint: '选择规则后执行' },
]

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return ALL_ITEMS.filter(item => isVisible(item))
  return ALL_ITEMS.filter(item => {
    if (!isVisible(item)) return false
    return item.label.toLowerCase().includes(q) || (item.hint && item.hint.toLowerCase().includes(q))
  })
})

function isVisible(item) {
  if (item.perm && !authStore.hasPermission(item.perm)) return false
  if (item.adminOnly && !authStore.isAdmin) return false
  return true
}

watch(visible, (v) => {
  if (v) {
    query.value = ''
    selectedIndex.value = 0
    nextTick(() => inputRef.value?.focus())
  }
})

watch(selectedIndex, (idx) => {
  nextTick(() => {
    if (selectedRef.value) {
      selectedRef.value.scrollIntoView({ block: 'nearest' })
    }
  })
})

function open() { visible.value = true }
function close() { visible.value = false }

function onInput() {
  selectedIndex.value = 0
}

function onKeydown(e) {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedIndex.value = (selectedIndex.value + 1) % filtered.value.length
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedIndex.value = (selectedIndex.value - 1 + filtered.value.length) % filtered.value.length
  } else if (e.key === 'Enter') {
    e.preventDefault()
    if (filtered.value[selectedIndex.value]) {
      select(filtered.value[selectedIndex.value])
    }
  } else if (e.key === 'Escape') {
    close()
  }
}

function select(item) {
  close()
  if (item.route) {
    router.push(item.route)
  } else if (item.action) {
    item.action()
  }
}

function onGlobalKeydown(e) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault()
    if (visible.value) {
      close()
    } else {
      open()
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', onGlobalKeydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onGlobalKeydown)
})

// Expose open method for header button
defineExpose({ open })
</script>

<style scoped>
.cmd-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: flex-start; justify-content: center;
  padding-top: 15vh;
  backdrop-filter: blur(2px);
}
.cmd-palette {
  width: 560px; max-width: 92vw;
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  overflow: hidden;
}
.cmd-palette__input-wrap {
  display: flex; align-items: center; gap: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--color-neutral-200);
}
.cmd-palette__icon { color: var(--color-neutral-400); flex-shrink: 0; }
.cmd-palette__input {
  flex: 1; border: none; outline: none;
  font-size: var(--fs-lg); font-family: var(--font-sans);
  color: var(--color-neutral-900); background: transparent;
}
.cmd-palette__input::placeholder { color: var(--color-neutral-400); }
.cmd-palette__esc {
  font-size: 11px; padding: 2px 8px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-neutral-300); color: var(--color-neutral-500);
  background: var(--color-neutral-100); cursor: pointer;
  font-family: var(--font-mono);
}
.cmd-palette__list {
  overflow-y: auto; padding: var(--sp-2) 0;
  max-height: 320px;
}
.cmd-palette__empty {
  text-align: center; padding: var(--sp-8);
  color: var(--color-neutral-400); font-size: var(--fs-sm);
}
.cmd-palette__item {
  display: flex; align-items: center; gap: var(--sp-3);
  padding: var(--sp-2) var(--sp-4); cursor: pointer;
  transition: background 0.1s;
}
.cmd-palette__item:hover,
.cmd-palette__item.is-selected {
  background: var(--color-primary-50);
}
.cmd-palette__item-icon {
  color: var(--color-neutral-400); flex-shrink: 0;
  width: 32px; height: 32px;
  display: inline-flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md);
  background: var(--color-neutral-100);
}
.cmd-palette__item.is-selected .cmd-palette__item-icon {
  background: var(--color-primary-100); color: var(--color-primary-700);
}
.cmd-palette__item-body { flex: 1; min-width: 0; }
.cmd-palette__item-label {
  display: block; font-size: var(--fs-sm); font-weight: 500;
  color: var(--color-neutral-900);
}
.cmd-palette__item-hint {
  display: block; font-size: var(--fs-xs); color: var(--color-neutral-500);
}
.cmd-palette__shortcut {
  font-size: 11px; padding: 2px 6px; border-radius: var(--radius-sm);
  border: 1px solid var(--color-neutral-300); color: var(--color-neutral-500);
  background: var(--color-neutral-100); font-family: var(--font-mono);
}
.cmd-palette__footer {
  display: flex; gap: var(--sp-4); padding: var(--sp-2) var(--sp-4);
  border-top: 1px solid var(--color-neutral-100);
  font-size: var(--fs-xs); color: var(--color-neutral-500);
}
.cmd-palette__footer kbd {
  font-size: 10px; padding: 1px 5px; border-radius: 3px;
  border: 1px solid var(--color-neutral-300); background: var(--color-neutral-100);
  font-family: var(--font-mono); margin-right: 2px;
}

/* Transition */
.cmd-fade-enter-active,
.cmd-fade-leave-active { transition: opacity 0.15s ease; }
.cmd-fade-enter-from,
.cmd-fade-leave-to { opacity: 0; }
</style>
