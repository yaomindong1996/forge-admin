<template>
  <div class="scoped-css-preview">
    <div class="preview-header">
      <span>隔离预览</span>
      <code>{{ scopeSelector }}</code>
    </div>
    <iframe
      title="作用域 CSS 隔离预览"
      sandbox=""
      :srcdoc="previewDocument"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  css: {
    type: String,
    default: '',
  },
  scopeSelector: {
    type: String,
    default: '',
  },
  applicationCode: {
    type: String,
    default: '',
  },
  pageCode: {
    type: String,
    default: '',
  },
})

const previewDocument = computed(() => {
  const css = String(props.css || '').replace(/<\/style/gi, '<\\/style')
  return `<!doctype html>
    <html><head><meta charset="utf-8"><style>
      body { margin: 0; padding: 16px; color: #303133; font: 13px/1.5 sans-serif; background: #f7f8fa; }
      .preview-card { padding: 14px; border: 1px solid #dfe3e8; border-radius: 6px; background: #fff; }
      .preview-card strong { display: block; margin-bottom: 6px; }
      ${css}
    </style></head><body>
      <div data-forge-app="${escapeAttribute(props.applicationCode)}" data-forge-page="${escapeAttribute(props.pageCode)}">
        <div class="preview-card customer-card panel"><strong>扩展样式预览</strong><span class="amount-cell">示例内容</span></div>
      </div>
    </body></html>`
})

function escapeAttribute(value) {
  return String(value || '').replace(/[&"<>]/g, character => ({
    '&': '&amp;',
    '"': '&quot;',
    '<': '&lt;',
    '>': '&gt;',
  })[character])
}
</script>

<style scoped>
.scoped-css-preview {
  overflow: hidden;
  border: 1px solid var(--n-border-color, #dfe3e8);
  border-radius: 6px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  padding: 0 10px;
  border-bottom: 1px solid var(--n-border-color, #e5e7eb);
  color: var(--n-text-color-2, #4b5563);
  background: var(--n-action-color, #f6f8fa);
  font-size: 12px;
}

.preview-header code {
  overflow: hidden;
  color: var(--n-text-color-3, #6e7781);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scoped-css-preview iframe {
  display: block;
  width: 100%;
  height: 180px;
  border: 0;
  background: #fff;
}
</style>
