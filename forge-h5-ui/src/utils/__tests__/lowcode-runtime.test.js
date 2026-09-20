import assert from 'node:assert/strict'
import test from 'node:test'
import {
  normalizeDesignerComponents,
  normalizeDesignerField,
  normalizeRuntimeZoneCanvasNodes,
  normalizeInternalPageUrl,
  resolveMobilePageChrome,
} from '../lowcode-runtime.js'

test('legacy date picker keeps its configured semantic subtype', () => {
  const field = normalizeDesignerField({
    type: 'datePicker',
    field: 'validPeriod',
    props: { type: 'daterange' },
  })

  assert.equal(field.type, 'daterange')
})

test('resolves mobile page chrome without allowing external back URLs', () => {
  const config = {
    objectName: '采购单',
    options: {
      mobilePage: { showNavigation: false, showBack: false, safeAreaBottom: true, subtitle: '审批移动页' },
    },
  }
  assert.deepEqual(resolveMobilePageChrome(config, { showBack: '1', backUrl: 'https://evil.example' }), {
    title: '采购单',
    subtitle: '审批移动页',
    showNav: false,
    showBack: true,
    safeBottom: true,
    padded: true,
    backUrl: '',
  })
  assert.equal(normalizeInternalPageUrl('/pages/index/index?tab=apps'), '/pages/index/index?tab=apps')
})

test('unknown designer nodes remain visible for an explicit blocked fallback', () => {
  const nodes = normalizeDesignerComponents({}, {
    components: [{ id: 'unsafe-1', type: 'remote-script-widget', label: '远程脚本' }],
  })

  assert.equal(nodes.length, 1)
  assert.equal(nodes[0].nodeType, 'remote-script-widget')
})

test('unknown canvas blocks remain visible for an explicit blocked fallback', () => {
  const nodes = normalizeRuntimeZoneCanvasNodes({}, {
    zoneKey: 'detail',
    props: {
      canvas: {
        items: [{ id: 'unsafe-2', blockType: 'remote-script-widget', label: '远程脚本' }],
      },
    },
  })

  assert.equal(nodes.length, 1)
  assert.equal(nodes[0].nodeType, 'remote-script-widget')
})
