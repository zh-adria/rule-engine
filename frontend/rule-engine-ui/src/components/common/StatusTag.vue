<template>
  <span class="re-tag" :class="mapped.cls">
    <component
      v-if="mapped.icon"
      :is="mapped.icon"
      :size="12"
      style="margin-right: 4px; opacity: 0.85"
      aria-hidden="true"
    />
    {{ mapped.label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import {
  CircleCheck, CircleX, AlertTriangle, Info,
  Timer, Archive, Box, Workflow, GitBranch, Beaker,
  RotateCcw, Send, Ban, CircleEllipsis
} from 'lucide-vue-next'
import { markRaw } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

/* 集中语义色映射 — 与 DESIGN.md "附录 A" 保持一致 */
const STATUS_MAP = {
  /* Decision output */
  ACCEPT:         { cls: 're-tag--success', icon: markRaw(CircleCheck),   label: '通过' },
  DECLINE:        { cls: 're-tag--danger',  icon: markRaw(CircleX),       label: '拒保' },
  MANUAL_REVIEW:  { cls: 're-tag--warning', icon: markRaw(AlertTriangle),  label: '人工复核' },
  RATE_UP:        { cls: 're-tag--info',    icon: markRaw(Info),           label: '加费' },
  PRICE_ADJUST:   { cls: 're-tag--info',    icon: markRaw(Info),           label: '定价调整' },
  BLACKLIST_HIT:  { cls: 're-tag--danger',  icon: markRaw(Ban),            label: '黑名单命中' },

  /* Rule / Version lifecycle */
  ACTIVE:         { cls: 're-tag--success', icon: markRaw(CircleCheck),   label: '活跃' },
  DRAFT:          { cls: 're-tag--neutral', icon: markRaw(CircleEllipsis), label: '草稿' },
  TESTING:        { cls: 're-tag--info',    icon: markRaw(Beaker),        label: '测试中' },
  PENDING_APPROVAL:{ cls: 're-tag--warning', icon: markRaw(Timer),          label: '待审批' },
  SUBMITTED:      { cls: 're-tag--warning', icon: markRaw(Send),           label: '已提交' },
  APPROVED:       { cls: 're-tag--success', icon: markRaw(CircleCheck),   label: '已审批' },
  PUBLISHED:      { cls: 're-tag--success', icon: markRaw(CircleCheck),   label: '已发布' },
  GRAY:           { cls: 're-tag--info',    icon: markRaw(GitBranch),     label: '灰度' },
  ROLLED_BACK:    { cls: 're-tag--neutral', icon: markRaw(RotateCcw),     label: '已回滚' },
  REJECTED:       { cls: 're-tag--danger',  icon: markRaw(CircleX),       label: '已驳回' },
  ARCHIVED:       { cls: 're-tag--neutral', icon: markRaw(Archive),        label: '已归档' },
  INACTIVE:       { cls: 're-tag--neutral', icon: markRaw(Box),            label: '未启用' },

  /* Approval */
  PENDING:        { cls: 're-tag--warning', icon: markRaw(Timer),          label: '待处理' },
  APPROVE:        { cls: 're-tag--success', icon: markRaw(CircleCheck),   label: '已通过' },
  REJECT:         { cls: 're-tag--danger',  icon: markRaw(CircleX),       label: '已驳回' },
}

const mapped = computed(
  () => STATUS_MAP[props.status] || {
    cls: 're-tag--neutral',
    icon: markRaw(Info),
    label: props.status
  }
)
</script>
