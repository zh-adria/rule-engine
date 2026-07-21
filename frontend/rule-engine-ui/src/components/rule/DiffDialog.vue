<template>
  <el-dialog
    v-model="appStore.diffDialogVisible"
    title="版本差异对比"
    width="82%"
    top="5vh"
    align-center
  >
    <div class="diff-dialog__header">
      <div>
        <span class="diff-dialog__label">旧版本</span>
        <span class="version-pill version-pill--danger">v{{ appStore.diffOldVersion }}</span>
        <span class="diff-dialog__arrow">→</span>
        <span class="diff-dialog__label">新版本</span>
        <span class="version-pill version-pill--success">v{{ appStore.diffNewVersion }}</span>
      </div>
      <div class="diff-dialog__stat">
        <span class="diff-stat diff-stat--add">+{{ appStore.diffStat.added }} 行</span>
        <span class="diff-stat diff-stat--remove">-{{ appStore.diffStat.removed }} 行</span>
        <span class="diff-stat diff-stat--equal">{{ appStore.diffStat.unchanged }} 行未变</span>
      </div>
    </div>

    <div class="diff-dialog__body">
      <div
        v-for="(hunk, idx) in appStore.diffHunks"
        :key="idx"
        :class="['diff-hunk', `diff-hunk--${hunk.type}`]"
      >
        <div
          v-for="(line, lineIdx) in hunk.lines"
          :key="lineIdx"
          :class="['diff-line', `diff-line--${hunk.type}`]"
        >
          <span class="diff-line__gutter">{{ hunk.type === 'remove' ? '-' : hunk.type === 'add' ? '+' : ' ' }}</span>
          <span class="diff-line__code">{{ line }}</span>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { useAppStore } from '../../stores/app'

const appStore = useAppStore()
</script>

<style scoped>
.diff-dialog__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--sp-3);
  padding-bottom: var(--sp-3);
  border-bottom: 1px solid var(--color-neutral-200);
  flex-wrap: wrap;
  gap: var(--sp-2);
}
.diff-dialog__label {
  font-size: var(--fs-sm);
  color: var(--color-neutral-500);
  margin-right: var(--sp-1);
}
.diff-dialog__arrow {
  margin: 0 var(--sp-3);
  color: var(--color-neutral-400);
}
.version-pill {
  font-family: var(--font-mono);
  font-size: var(--fs-xs);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-weight: var(--fw-medium);
}
.version-pill--danger { background: var(--color-danger-bg); color: var(--color-danger-fg); }
.version-pill--success { background: var(--color-success-bg); color: var(--color-success-fg); }

.diff-dialog__stat {
  display: inline-flex;
  gap: var(--sp-3);
  font-size: var(--fs-sm);
}
.diff-stat--add     { color: var(--color-success-fg); font-weight: var(--fw-semibold); }
.diff-stat--remove  { color: var(--color-danger-fg);  font-weight: var(--fw-semibold); }
.diff-stat--equal   { color: var(--color-neutral-500); }

.diff-dialog__body {
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-md);
  overflow: auto;
  max-height: 60vh;
}

.diff-hunk--add     { background: var(--color-success-bg); }
.diff-hunk--remove  { background: var(--color-danger-bg); }
.diff-hunk--equal   { background: var(--color-surface); }

.diff-line {
  display: flex;
  min-width: min-content;
}
.diff-line__gutter {
  min-width: 28px;
  padding: 0 8px;
  user-select: none;
  text-align: center;
  color: var(--color-neutral-500);
  font-family: var(--font-mono);
  font-size: var(--fs-sm);
  line-height: 1.6;
  background: rgba(0, 0, 0, 0.02);
  border-right: 1px solid var(--color-neutral-200);
  flex-shrink: 0;
}
.diff-line--add    .diff-line__gutter { color: var(--color-success-fg); background: rgba(22, 163, 74, 0.08); }
.diff-line--remove .diff-line__gutter { color: var(--color-danger-fg);  background: rgba(220, 38, 38, 0.08); }

.diff-line__code {
  flex: 1;
  white-space: pre;
  padding: 0 8px;
  font-family: var(--font-mono);
  font-size: var(--fs-sm);
  line-height: 1.6;
  color: var(--color-neutral-800);
}
</style>
