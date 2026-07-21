<template>
  <div class="custom-fields">
    <!-- ====== Masthead ====== -->
    <header class="custom-fields__mast">
      <p class="custom-fields__kicker">CONFIG · 自定义字段</p>
      <h1 class="custom-fields__hed">自定义字段管理</h1>
      <p class="custom-fields__deck">为不同业务线配置规则条件中使用的自定义字段。</p>
      <div class="custom-fields__actions">
        <el-select v-model="filterBiz" placeholder="业务线筛选" clearable style="width: 180px" @change="loadFields">
          <el-option label="全部" value="" />
          <el-option label="健康险" value="HEALTH" />
          <el-option label="车险" value="AUTO" />
          <el-option label="寿险" value="LIFE" />
          <el-option label="财产险" value="PROPERTY" />
        </el-select>
        <button class="custom-fields__add" @click="openEditDialog()">
          <Plus :size="16" />
          <span>新建字段</span>
        </button>
      </div>
    </header>

    <!-- ====== Table ---------- -->
    <section class="custom-fields__table" v-if="fields.length">
      <div class="custom-fields__thead">
        <span class="custom-fields__col custom-fields__col--code">编码</span>
        <span class="custom-fields__col custom-fields__col--label">名称</span>
        <span class="custom-fields__col custom-fields__col--type">类型</span>
        <span class="custom-fields__col custom-fields__col--biz">业务线</span>
        <span class="custom-fields__col custom-fields__col--order">排序</span>
        <span class="custom-fields__col custom-fields__col--op">操作</span>
      </div>
      <div
        v-for="(f, idx) in fields"
        :key="f.id"
        class="custom-fields__row"
        :class="`custom-fields__row--delay-${Math.min(idx, 5)}`"
      >
        <span class="custom-fields__col custom-fields__col--code re-text-code">{{ f.fieldCode }}</span>
        <span class="custom-fields__col custom-fields__col--label">{{ f.fieldLabel }}</span>
        <span class="custom-fields__col custom-fields__col--type">
          <span class="type-pill" :class="`type-pill--${f.fieldType}`">{{ typeLabel(f.fieldType) }}</span>
        </span>
        <span class="custom-fields__col custom-fields__col--biz">{{ f.businessLine || '全部' }}</span>
        <span class="custom-fields__col custom-fields__col--order re-text-num">{{ f.sortOrder }}</span>
        <span class="custom-fields__col custom-fields__col--op">
          <button class="cf-act" @click="openEditDialog(f)" title="编辑">
            <Pencil :size="14" />
          </button>
          <button class="cf-act cf-act--danger" @click="deleteField(f)" title="删除">
            <Trash2 :size="14" />
          </button>
        </span>
      </div>
    </section>

    <!-- ====== Empty State ====== -->
    <div v-else-if="!loading" class="custom-fields__empty">
      <Layers :size="36" />
      <h2 class="custom-fields__empty-title">暂无自定义字段</h2>
      <p class="custom-fields__empty-desc">点击"新建字段"开始配置</p>
      <button class="re-btn re-btn--primary" @click="openEditDialog()">
        <Plus :size="14" />
        <span>新建字段</span>
      </button>
    </div>

    <el-skeleton v-if="loading" :rows="5" animated class="custom-fields__skeleton" />

    <!-- ====== Edit Dialog ====== -->
    <el-dialog v-model="editVisible" :title="editingField.id ? '编辑字段' : '新建字段'" width="520px" align-center>
      <el-form :model="editingField" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字段编码" required>
              <el-input v-model="editingField.fieldCode" :disabled="!!editingField.id" placeholder="如: customerAge" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段名称" required>
              <el-input v-model="editingField.fieldLabel" placeholder="如: 客户年龄" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字段类型" required>
              <el-select v-model="editingField.fieldType" style="width: 100%">
                <el-option label="文本" value="string" />
                <el-option label="数值" value="number" />
                <el-option label="布尔" value="boolean" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务线">
              <el-input v-model="editingField.businessLine" placeholder="如: HEALTH（留空=全部）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排序">
          <el-input-number v-model="editingField.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="danger" v-if="editingField.id" @click="deleteCurrent">删除</el-button>
        <el-button type="primary" @click="saveField">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Pencil, Trash2, Layers } from 'lucide-vue-next'
import { listCustomFields, createCustomField, deleteCustomField } from '../../api/rules'
import { ElMessage, ElMessageBox } from 'element-plus'

const fields = ref([])
const loading = ref(false)
const filterBiz = ref('')
const editVisible = ref(false)
const editingField = ref({ fieldCode: '', fieldLabel: '', fieldType: 'string', businessLine: '', sortOrder: 0 })

function typeLabel(t) {
  return { string: '文本', number: '数值', boolean: '布尔' }[t] || t
}

async function loadFields() {
  loading.value = true
  try {
    fields.value = await listCustomFields(filterBiz.value || undefined)
  } catch (e) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function openEditDialog(row) {
  if (row) {
    editingField.value = { ...row }
  } else {
    editingField.value = { fieldCode: '', fieldLabel: '', fieldType: 'string', businessLine: '', sortOrder: 0 }
  }
  editVisible.value = true
}

async function saveField() {
  if (!editingField.value.fieldCode || !editingField.value.fieldLabel) {
    ElMessage.warning('请填写字段编码和名称')
    return
  }
  try {
    await createCustomField(editingField.value)
    ElMessage.success('已保存')
    editVisible.value = false
    await loadFields()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

async function deleteField(row) {
  try {
    await ElMessageBox.confirm(`删除字段 "${row.fieldLabel}"?`, '确认', { type: 'warning' })
    await deleteCustomField(row.id)
    ElMessage.success('已删除')
    await loadFields()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败: ' + e.message)
  }
}

async function deleteCurrent() {
  if (!editingField.value.id) return
  await deleteField(editingField.value)
  editVisible.value = false
}

onMounted(loadFields)
</script>

<style scoped>
/* ===========================================================
   CUSTOM FIELD LIST — taste-skill anti-slop redesign
   · Editorial masthead with filter actions
   · Custom table with type-colored pills
   · Row hover with left-border accent
   · Staggered entrance animation
   =========================================================== */

.custom-fields {
  padding-bottom: var(--sp-8);
}

/* ---------- Masthead ---------- */
.custom-fields__mast {
  position: relative;
  margin-bottom: var(--sp-5);
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--color-neutral-200);
}
.custom-fields__kicker {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--color-neutral-400);
  margin: 0 0 var(--sp-2);
}
.custom-fields__hed {
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 700;
  line-height: var(--lh-tight);
  letter-spacing: var(--tracking-tight);
  color: var(--color-neutral-900);
  margin: 0 0 var(--sp-2);
}
.custom-fields__deck {
  margin: 0;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: var(--fs-md);
  color: var(--color-neutral-500);
  line-height: var(--lh-body);
}
.custom-fields__actions {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}
.custom-fields__add {
  display: inline-flex;
  align-items: center;
  gap: var(--sp-2);
  padding: 0 var(--sp-4);
  height: 38px;
  background: var(--color-primary-600);
  color: #fff;
  border: none;
  border-radius: var(--radius-full);
  font-family: var(--font-sans);
  font-size: var(--fs-sm);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(79,70,229,0.2);
  transition: all var(--dur-2) var(--ease);
}
.custom-fields__add:hover {
  background: var(--color-primary-700);
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(79,70,229,0.3);
}
.custom-fields__add:active {
  transform: translateY(0) scale(0.98);
}

/* ---------- Custom Table ---------- */
.custom-fields__table {
  background: var(--color-surface);
  border: 1px solid var(--color-neutral-200);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.custom-fields__thead {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 80px 100px 60px 100px;
  gap: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  background: var(--color-neutral-50);
  border-bottom: 1px solid var(--color-neutral-200);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--color-neutral-500);
  text-transform: uppercase;
}
.custom-fields__row {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 80px 100px 60px 100px;
  gap: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--color-neutral-100);
  font-size: var(--fs-sm);
  color: var(--color-neutral-700);
  align-items: center;
  position: relative;
  transition: background var(--dur-2) var(--ease);
  animation: fadeInUp 400ms var(--ease) both;
}
.custom-fields__row:last-child { border-bottom: none; }
.custom-fields__row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: transparent;
  transition: background var(--dur-2) var(--ease);
}
.custom-fields__row:hover {
  background: var(--color-neutral-50);
}
.custom-fields__row:hover::before {
  background: var(--color-primary-500);
}

/* Stagger delays */
.custom-fields__row--delay-0 { animation-delay: 0ms; }
.custom-fields__row--delay-1 { animation-delay: 40ms; }
.custom-fields__row--delay-2 { animation-delay: 80ms; }
.custom-fields__row--delay-3 { animation-delay: 120ms; }
.custom-fields__row--delay-4 { animation-delay: 160ms; }
.custom-fields__row--delay-5 { animation-delay: 200ms; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.custom-fields__col {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- Type Pills ---------- */
.type-pill {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  letter-spacing: 0.03em;
  white-space: nowrap;
}
.type-pill--string { background: #ede9fe; color: #6d28d9; }
.type-pill--number { background: #dbeafe; color: #1e40af; }
.type-pill--boolean { background: #fef3c7; color: #92400e; }

.cf-act {
  all: unset;
  cursor: pointer;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-neutral-500);
  transition: all var(--dur-2) var(--ease);
}
.cf-act:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}
.cf-act--danger:hover {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}

/* ---------- Empty State ---------- */
.custom-fields__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--sp-3);
  text-align: center;
  padding: var(--sp-12) var(--sp-4);
  border: 2px dashed var(--color-neutral-200);
  border-radius: var(--radius-lg);
  animation: fadeInUp 400ms var(--ease) both;
}
.custom-fields__empty-icon {
  color: var(--color-neutral-300);
  margin-bottom: var(--sp-2);
}
.custom-fields__empty-title {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--fs-xl);
  font-weight: 700;
  color: var(--color-neutral-700);
}
.custom-fields__empty-desc {
  margin: 0;
  color: var(--color-neutral-500);
  font-size: var(--fs-sm);
}
.custom-fields__skeleton { padding: var(--sp-5); }

/* ---------- Responsive ---------- */
@media (max-width: 1100px) {
  .custom-fields__thead { display: none; }
  .custom-fields__row {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto auto;
    height: auto;
    padding: var(--sp-4);
    gap: var(--sp-2);
    margin-bottom: var(--sp-2);
    border: 1px solid var(--color-neutral-200);
    border-radius: var(--radius-lg);
  }
  .custom-fields__row::before { display: none; }
  .custom-fields__row:hover {
    border-color: var(--color-primary-400);
    box-shadow: var(--shadow-sm);
  }
}
@media (max-width: 640px) {
  .custom-fields__mast { position: static; }
  .custom-fields__actions {
    position: static;
    margin-top: var(--sp-4);
    width: 100%;
  }
  .custom-fields__add { flex: 1; justify-content: center; }
}
</style>
