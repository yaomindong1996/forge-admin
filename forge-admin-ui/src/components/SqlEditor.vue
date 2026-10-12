<template>
  <div
    class="sql-editor"
    :class="{
      'sql-editor--dark': theme === 'dark',
      'sql-editor--fullscreen': isFullscreen,
    }"
  >
    <div class="sql-editor__toolbar">
      <div class="sql-editor__title">
        <i class="i-material-symbols:database-outline" />
        <span>SQL 编辑器</span>
      </div>
      <n-space :size="8">
        <n-button size="small" secondary :disabled="readonly" @click="handleFormat">
          <template #icon>
            <i class="i-material-symbols:format-align-left" />
          </template>
          格式化
        </n-button>
        <n-button size="small" secondary :disabled="readonly" @click="handleClear">
          <template #icon>
            <i class="i-material-symbols:delete-outline" />
          </template>
          清空
        </n-button>
        <n-button
          v-if="showFullscreen"
          size="small"
          secondary
          circle
          :title="isFullscreen ? '退出全屏' : '全屏编辑'"
          @click="toggleFullscreen"
        >
          <template #icon>
            <i :class="isFullscreen ? 'i-material-symbols:fullscreen-exit' : 'i-material-symbols:fullscreen'" />
          </template>
        </n-button>
      </n-space>
    </div>
    <div class="sql-editor__body">
      <div ref="editorRef" class="sql-editor__container" />
      <div v-if="!innerValue" class="sql-editor__placeholder">
        {{ placeholder }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { sql } from '@codemirror/lang-sql'
import { basicSetup, EditorView } from 'codemirror'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  value: {
    type: String,
    default: '',
  },
  placeholder: {
    type: String,
    default: 'SELECT * FROM table_name WHERE ...',
  },
  readonly: {
    type: Boolean,
    default: false,
  },
  theme: {
    type: String,
    default: 'light',
  },
  showFullscreen: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:value', 'format', 'clear'])

const editorRef = ref(null)
const innerValue = ref(props.value || '')
const isFullscreen = ref(false)

let editorView = null

const editorTheme = EditorView.theme({
  '&': {
    height: '100%',
    fontSize: '13px',
  },
  '.cm-scroller': {
    fontFamily: 'JetBrains Mono, SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace',
    lineHeight: '1.65',
  },
  '.cm-content': {
    minHeight: '220px',
    padding: '12px 0',
  },
  '.cm-gutters': {
    backgroundColor: '#f8fafc',
    color: 'var(--text-tertiary)',
    borderRight: '1px solid #e2e8f0',
  },
  '.cm-activeLine': {
    backgroundColor: '#eff6ff',
  },
  '.cm-activeLineGutter': {
    backgroundColor: '#dbeafe',
    color: '#2563eb',
  },
  '.cm-selectionBackground, &.cm-focused .cm-selectionBackground': {
    backgroundColor: '#bfdbfe',
  },
  '&.cm-focused': {
    outline: 'none',
  },
}, { dark: false })

onMounted(async () => {
  await nextTick()
  initEditor()
})

onBeforeUnmount(() => {
  destroyEditor()
})

watch(() => props.value, (value) => {
  const nextValue = value || ''
  if (nextValue === innerValue.value)
    return

  innerValue.value = nextValue
  setEditorValue(nextValue)
})

watch(() => props.readonly, async () => {
  await nextTick()
  initEditor()
})

function initEditor() {
  destroyEditor()

  if (!editorRef.value)
    return

  editorView = new EditorView({
    doc: innerValue.value,
    extensions: [
      basicSetup,
      sql(),
      editorTheme,
      EditorView.lineWrapping,
      EditorView.editable.of(!props.readonly),
      EditorView.updateListener.of((update) => {
        if (!update.docChanged)
          return

        const value = update.state.doc.toString()
        innerValue.value = value
        emit('update:value', value)
      }),
    ],
    parent: editorRef.value,
  })
}

function destroyEditor() {
  if (editorView) {
    editorView.destroy()
    editorView = null
  }
}

function setEditorValue(value) {
  if (!editorView)
    return

  const currentValue = editorView.state.doc.toString()
  if (currentValue === value)
    return

  editorView.dispatch({
    changes: {
      from: 0,
      to: currentValue.length,
      insert: value,
    },
  })
}

function handleFormat() {
  const formatted = formatSql(innerValue.value)
  innerValue.value = formatted
  setEditorValue(formatted)
  emit('update:value', formatted)
  emit('format', formatted)
  editorView?.focus()
}

function handleClear() {
  innerValue.value = ''
  setEditorValue('')
  emit('update:value', '')
  emit('clear')
  editorView?.focus()
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  nextTick(() => {
    editorView?.requestMeasure()
    editorView?.focus()
  })
}

function formatSql(value) {
  if (!value)
    return ''

  const protectedSql = protectSqlFragments(value.trim())
  let text = protectedSql.text

  const compoundKeywords = [
    'left join',
    'right join',
    'inner join',
    'full join',
    'cross join',
    'group by',
    'order by',
  ]
  const keywords = [
    'select',
    'from',
    'where',
    'having',
    'limit',
    'offset',
    'union',
    'join',
    'on',
    'and',
    'or',
    'insert',
    'into',
    'values',
    'update',
    'set',
    'delete',
  ]

  text = text
    .replace(/\s+/g, ' ')
    .replace(/\s*,\s*/g, ', ')
    .trim()

  compoundKeywords.forEach((keyword) => {
    const pattern = new RegExp(`\\b${keyword.replace(/\s+/g, '\\s+')}\\b`, 'gi')
    text = text.replace(pattern, keyword.toUpperCase())
  })

  keywords.forEach((keyword) => {
    const pattern = new RegExp(`\\b${keyword}\\b`, 'gi')
    text = text.replace(pattern, keyword.toUpperCase())
  })

  text = text
    .replace(/\s+(FROM|WHERE|HAVING|GROUP BY|ORDER BY|LIMIT|OFFSET|UNION)\b/g, '\n$1')
    .replace(/\s+(LEFT JOIN|RIGHT JOIN|INNER JOIN|FULL JOIN|CROSS JOIN|JOIN)\b/g, '\n$1')
    .replace(/\s+(ON)\b/g, '\n  $1')
    .replace(/\s+(AND|OR)\b/g, '\n  $1')
    .replace(/,\s*/g, ',\n  ')

  return protectedSql.restore(text)
    .split('\n')
    .map(line => line.trimEnd())
    .join('\n')
}

function protectSqlFragments(value) {
  const fragments = []
  let text = ''
  let index = 0

  while (index < value.length) {
    const char = value[index]
    const next = value[index + 1]

    if (char === '\'' || char === '"' || char === '`') {
      const { fragment, nextIndex } = readQuoted(value, index, char)
      text += createPlaceholder(fragments, fragment)
      index = nextIndex
      continue
    }

    if (char === '-' && next === '-') {
      const nextLineIndex = value.indexOf('\n', index)
      const endIndex = nextLineIndex === -1 ? value.length : nextLineIndex + 1
      text += createPlaceholder(fragments, value.slice(index, endIndex))
      index = endIndex
      continue
    }

    if (char === '/' && next === '*') {
      const closeIndex = value.indexOf('*/', index + 2)
      const endIndex = closeIndex === -1 ? value.length : closeIndex + 2
      text += createPlaceholder(fragments, value.slice(index, endIndex))
      index = endIndex
      continue
    }

    text += char
    index += 1
  }

  return {
    text,
    restore: formatted => fragments.reduce((result, fragment, fragmentIndex) => {
      return result.replaceAll(`__SQL_FRAGMENT_${fragmentIndex}__`, fragment)
    }, formatted),
  }
}

function readQuoted(value, startIndex, quote) {
  let index = startIndex + 1

  while (index < value.length) {
    if (value[index] === '\\') {
      index += 2
      continue
    }

    if (value[index] === quote) {
      if (value[index + 1] === quote) {
        index += 2
        continue
      }

      index += 1
      break
    }

    index += 1
  }

  return {
    fragment: value.slice(startIndex, index),
    nextIndex: index,
  }
}

function createPlaceholder(fragments, fragment) {
  const placeholder = `__SQL_FRAGMENT_${fragments.length}__`
  fragments.push(fragment)
  return placeholder
}
</script>

<style scoped>
.sql-editor {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: var(--bg-primary);
}

.sql-editor--fullscreen {
  position: fixed;
  inset: 24px;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  box-shadow: 0 24px 60px rgb(0 0 0 / 28%);
}

.sql-editor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 8px 10px 8px 14px;
  border-bottom: 1px solid var(--border-light);
  background: linear-gradient(180deg, var(--bg-secondary) 0%, var(--bg-primary) 100%);
}

.sql-editor__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.sql-editor__title i {
  color: #2563eb;
  font-size: 18px;
}

.sql-editor__body {
  position: relative;
  min-height: 248px;
  background: var(--bg-primary);
}

.sql-editor--fullscreen .sql-editor__body {
  flex: 1;
  min-height: 0;
}

.sql-editor__container {
  min-height: 248px;
}

.sql-editor__placeholder {
  position: absolute;
  top: 14px;
  left: 54px;
  z-index: 1;
  max-width: calc(100% - 72px);
  color: var(--text-tertiary);
  font-family:
    JetBrains Mono,
    SFMono-Regular,
    Consolas,
    Liberation Mono,
    Menlo,
    monospace;
  font-size: 13px;
  line-height: 1.65;
  pointer-events: none;
}

.sql-editor :deep(.cm-editor) {
  min-height: 248px;
}

.sql-editor :deep(.cm-scroller) {
  min-height: 248px;
}

.sql-editor--fullscreen :deep(.cm-editor),
.sql-editor--fullscreen :deep(.cm-scroller),
.sql-editor--fullscreen .sql-editor__container {
  height: 100%;
  min-height: 0;
}

.sql-editor :deep(.cm-line) {
  padding: 0 12px;
}

.sql-editor--dark {
  border-color: #1e1e1e;
  background: #1e1e1e;
}

.sql-editor--dark .sql-editor__toolbar {
  border-bottom-color: #2f3338;
  background: #1e1e1e;
}

.sql-editor--dark .sql-editor__title,
.sql-editor--dark .sql-editor__title i {
  color: #d4d4d4;
}

.sql-editor--dark .sql-editor__body,
.sql-editor--dark .sql-editor__container,
.sql-editor--dark :deep(.cm-editor),
.sql-editor--dark :deep(.cm-scroller) {
  background: #1e1e1e !important;
}

.sql-editor--dark :deep(.cm-editor) {
  color: #d4d4d4 !important;
}

.sql-editor--dark :deep(.cm-gutters) {
  color: var(--text-tertiary) !important;
  background: #1e1e1e !important;
  border-right-color: #2f3338 !important;
}

.sql-editor--dark :deep(.cm-activeLine),
.sql-editor--dark :deep(.cm-activeLineGutter) {
  background: #252526 !important;
}

.sql-editor--dark .sql-editor__placeholder {
  color: var(--text-secondary);
}
</style>
