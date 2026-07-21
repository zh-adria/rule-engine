<template>
  <header class="re-page-header">
    <div>
      <h1 class="re-page-header__title">
        <component
          v-if="icon"
          :is="icon"
          class="re-page-header__icon"
          :size="20"
          aria-hidden="true"
        />
        {{ title }}
      </h1>
      <p v-if="subtitle" class="re-page-header__subtitle">{{ subtitle }}</p>
    </div>
    <div class="re-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: [Object, Function], default: null }
})
</script>

<style scoped>
.re-page-header__icon {
  color: var(--color-primary-600);
  vertical-align: -3px;
  margin-right: var(--sp-2);
}
</style>
