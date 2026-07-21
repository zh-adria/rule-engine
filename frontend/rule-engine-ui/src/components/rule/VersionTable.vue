<template>
  <div class="version-table">
    <div class="version-table__toolbar">
      <el-button
        size="small"
        :icon="GitBranch"
        :disabled="selectedVersions.length !== 2"
        @click="compareSelected"
      >
        对比选中版本({{ selectedVersions.length }}/2)
      </el-button>
      <span class="version-table__hint">勾选两个版本查看 DRL 差异</span>
    </div>

    <el-table
      :data="versions"
      size="small"
      max-height="360"
      empty-text="暂无版本"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="40" />
      <el-table-column prop="version" label="版本" width="70">
        <template #default="{ row }">
          <span class="version-pill">v{{ row.version }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建人" width="110" />
      <el-table-column prop="approvedBy" label="审批人" width="110" />
      <el-table-column label="生效开始" width="160">
        <template #default="{ row }">
          <span class="re-text-code" style="font-size: var(--fs-xs)">
            {{ formatDateTime(row.effectiveFrom) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="生效结束" width="160">
        <template #default="{ row }">
          <span class="re-text-code" style="font-size: var(--fs-xs)">
            {{ formatDateTime(row.effectiveTo) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="checksum" label="校验码" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="re-text-code" style="font-size: var(--fs-xs)">
            {{ row.checksum || '—' }}
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { GitBranch } from 'lucide-vue-next'
import { getRuleVersion } from '../../api/rules'
import { computeDiff, diffStats } from '../../utils/diff'
import { useAppStore } from '../../stores/app'
import StatusTag from '../common/StatusTag.vue'

const props = defineProps({
  versions: { type: Array, required: true },
  ruleCode: { type: String, required: true }
})

const appStore = useAppStore()
const selectedVersions = ref([])

function onSelectionChange(selection) {
  selectedVersions.value = selection.map(s => s.version)
}

async function compareSelected() {
  if (selectedVersions.value.length !== 2 || !props.ruleCode) return
  const [v1, v2] = selectedVersions.value.sort((a, b) => a - b)
  try {
    const [oldVer, newVer] = await Promise.all([
      getRuleVersion(props.ruleCode, v1),
      getRuleVersion(props.ruleCode, v2)
    ])
    const hunks = computeDiff(oldVer.drlContent || '', newVer.drlContent || '')
    const stat = diffStats(hunks)
    appStore.showDiff(v1, v2, hunks, stat)
  } catch (e) {
    console.error('Failed to load rule versions for diff', e)
  }
}

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.version-table {
  margin-top: var(--sp-2);
}

.version-table__toolbar {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  margin-bottom: var(--sp-2);
}
.version-table__hint {
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
}

.version-pill {
  font-family: var(--font-mono);
  font-size: var(--fs-xs);
  color: var(--color-neutral-700);
  background: var(--color-neutral-100);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  font-weight: var(--fw-medium);
}
</style>
