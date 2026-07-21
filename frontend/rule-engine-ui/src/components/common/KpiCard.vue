<template>
  <article class="re-kpi-card" :class="accentCls">
    <div class="re-kpi-card__label">
      <span class="re-kpi-card__icon">
        <component :is="icon" :size="16" />
      </span>
      {{ label }}
    </div>
    <div class="re-kpi-card__value">{{ formattedValue }}</div>
    <div v-if="trend" class="re-kpi-card__trend" :class="trendClass">
      <component :is="trendIcon" :size="12" />
      <span>{{ Math.abs(trend.value).toFixed(1) }}%</span>
      <span style="margin-left: 4px">vs 昨日</span>
    </div>
    <div v-else class="re-kpi-card__trend re-kpi-card__trend is-flat">
      <Minus :size="12" />
      <span>较昨日持平</span>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowUpRight, ArrowDownRight, Minus } from 'lucide-vue-next'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: Function, required: true },
  icon: { type: [Object, Function], required: true },
  trend: { type: Object, default: null }, // { value: Number, direction: 'up'|'down' }
  accent: { type: String, default: 'primary' } // primary | warning | success
})

const formattedValue = computed(() => {
  const v = props.value()
  return v == null ? '—' : v.toLocaleString()
})

const trendClass = computed(() => ({
  'is-up': props.trend?.direction === 'up',
  'is-down': props.trend?.direction === 'down'
}))

const trendIcon = computed(() => {
  if (props.trend?.direction === 'up')   return ArrowUpRight
  if (props.trend?.direction === 'down') return ArrowDownRight
  return Minus
})

const accentCls = computed(() => ({
  're-kpi-card--warning': props.accent === 'warning',
  're-kpi-card--success': props.accent === 'success'
}))
</script>

<style scoped>
.re-kpi-card--warning .re-kpi-card__icon {
  background: var(--color-warning-bg);
  color: var(--color-warning-fg);
}
.re-kpi-card--success .re-kpi-card__icon {
  background: var(--color-success-bg);
  color: var(--color-success-fg);
}
</style>
