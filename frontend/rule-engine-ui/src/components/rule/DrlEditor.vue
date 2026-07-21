<template>
  <div class="drl-editor">
    <header class="drl-editor__header">
      <span class="drl-editor__title">
        <FileCode :size="16" style="margin-right: 6px; vertical-align: -2px" />
        DRL 编辑器
      </span>
      <div class="drl-editor__toolbar">
        <el-button size="small" @click="$emit('parse')">
          <ArrowDown :size="13" style="margin-right: 4px" />
          解析为可视化
        </el-button>
        <el-button size="small" type="success" @click="$emit('generate')">
          <ArrowUp :size="13" style="margin-right: 4px" />
          从可视化生成
        </el-button>
      </div>
    </header>

    <div ref="editorContainer" class="drl-editor__cm" />

    <div class="drl-editor__footer">
      <span class="hint">提示:DRL 与可视化规则双向转换,后端 /convert 不通时会回退本地正则解析</span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, onDeactivated, onActivated } from 'vue'
import { ArrowUp, ArrowDown, FileCode } from 'lucide-vue-next'
import { EditorView, keymap } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { java } from '@codemirror/lang-java'
import { oneDark } from '@codemirror/theme-one-dark'
import { indentWithTab } from '@codemirror/commands'

const props = defineProps({
  modelValue: { type: String, default: '' },
  rows: { type: Number, default: 16 }
})

const emit = defineEmits(['update:modelValue', 'parse', 'generate'])

const editorContainer = ref(null)
let editorView = null

const customTheme = EditorView.theme({
  '&': {
    fontSize: '13px',
    fontFamily: "'JetBrains Mono', 'Fira Code', 'SF Mono', 'Cascadia Code', monospace",
    height: '100%'
  },
  '.cm-content': {
    caretColor: '#c7d2fe',
    minHeight: '200px'
  },
  '&.cm-focused .cm-cursor': {
    borderLeftColor: '#818cf8'
  },
  '.cm-selectionBackground, .cm-selectionBackground ::selection': {
    backgroundColor: 'rgba(99, 102, 241, 0.25) !important'
  },
  '.cm-gutters': {
    backgroundColor: 'rgba(0, 0, 0, 0.2)',
    borderRight: '1px solid rgba(255, 255, 255, 0.06)',
    color: 'rgba(148, 163, 184, 0.5)'
  },
  '.cm-activeLineGutter': {
    backgroundColor: 'rgba(99, 102, 241, 0.1)'
  },
  '.cm-activeLine': {
    backgroundColor: 'rgba(255, 255, 255, 0.03)'
  }
}, { dark: true })

const startEditor = () => {
  if (editorView || !editorContainer.value) return

  const state = EditorState.create({
    doc: props.modelValue,
    extensions: [
      java(),
      oneDark,
      customTheme,
      keymap.of([indentWithTab]),
      EditorView.lineWrapping,
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          emit('update:modelValue', update.state.doc.toString())
        }
      }),
      EditorState.tabSize.of(2)
    ]
  })

  editorView = new EditorView({
    state,
    parent: editorContainer.value
  })
}

watch(() => props.modelValue, (newVal) => {
  if (editorView && newVal !== editorView.state.doc.toString()) {
    editorView.dispatch({
      changes: {
        from: 0,
        to: editorView.state.doc.length,
        insert: newVal
      }
    })
  }
})

onMounted(() => {
  startEditor()
})

onBeforeUnmount(() => {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
})

onDeactivated(() => {
  // Pause CodeMirror when cached by keep-alive to prevent memory leak
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
})

onActivated(() => {
  // Re-create editor when returning from keep-alive cache
  startEditor()
})
</script>

<style scoped>
.drl-editor {
  margin-top: var(--sp-4);
}

.drl-editor__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--sp-2);
  margin-bottom: var(--sp-2);
  flex-wrap: wrap;
}
.drl-editor__title {
  font-size: var(--fs-sm);
  font-weight: var(--fw-semibold);
  color: var(--color-neutral-800);
  display: inline-flex;
  align-items: center;
}
.drl-editor__toolbar {
  display: inline-flex;
  gap: var(--sp-2);
}

.drl-editor__cm {
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #0b1020;
  min-height: 280px;
  height: calc(var(--rows, 16) * 22px);
}

.drl-editor__cm :deep(.cm-editor) {
  height: 100%;
}

.drl-editor__cm :deep(.cm-scroller) {
  overflow: auto;
}

.drl-editor__footer {
  margin-top: var(--sp-2);
}
.hint {
  font-size: var(--fs-xs);
  color: var(--color-neutral-500);
}
</style>
