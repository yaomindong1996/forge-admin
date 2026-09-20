import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  MOBILE_COMPONENT_CAPABILITY,
  listRegisteredMobileComponentTypes,
  normalizeMobileComponentType,
  resolveMobileComponent,
} from '../mobile-component-registry.js'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const adminSpecDir = path.resolve(testDir, '../../../../../forge-admin-ui/src/components/lowcode-builder/designer-core/spec')
const pageSchemaFile = path.resolve(testDir, '../../../../../forge-admin-ui/src/components/lowcode-builder/page/page-schema.js')
const legacyDesignerFile = path.resolve(testDir, '../../../../../forge-admin-ui/src/components/form-designer/FormDesigner.vue')
const specFiles = [
  'field-components.js',
  'layout-components.js',
  'page-components.js',
  'media-components.js',
  'business-components.js',
  'widget-components.js',
  'zone-action-components.js',
]

function adminComponentTypes() {
  return specFiles.flatMap((file) => {
    const source = fs.readFileSync(path.join(adminSpecDir, file), 'utf8')
    return Array.from(source.matchAll(/^\s{2}type: '([^']+)'/gm), match => match[1])
  })
}

test('mobile registry covers every component type exposed by the admin designer', () => {
  const registered = new Set(listRegisteredMobileComponentTypes())
  const missing = Array.from(new Set(adminComponentTypes()))
    .map(normalizeMobileComponentType)
    .filter(type => !registered.has(type))

  assert.deepEqual(missing, [])
})

test('mobile registry covers page-builder canvas block types', () => {
  const source = fs.readFileSync(pageSchemaFile, 'utf8')
  const blockTypes = Array.from(source.matchAll(/blockType:\s*'([^']+)'/g), match => match[1])
  const missing = Array.from(new Set(blockTypes)).filter(type => resolveMobileComponent(type).kind === 'unknown')
  assert.deepEqual(missing, [])
})

test('mobile registry covers component types from the legacy form designer', () => {
  const source = fs.readFileSync(legacyDesignerFile, 'utf8')
  const componentListSource = source.slice(
    source.indexOf('const basicComponents'),
    source.indexOf('// 日期类型选项'),
  )
  const componentTypes = Array.from(componentListSource.matchAll(/\{ type: '([^']+)'/g), match => match[1])
  const missing = Array.from(new Set(componentTypes)).filter(type => resolveMobileComponent(type).kind === 'unknown')
  assert.deepEqual(missing, [])
})

test('unknown components are blocked instead of becoming editable text inputs', () => {
  assert.deepEqual(resolveMobileComponent('remote-script-widget'), {
    type: 'remote-script-widget',
    sourceType: 'remote-script-widget',
    kind: 'unknown',
    capability: MOBILE_COMPONENT_CAPABILITY.BLOCKED,
    renderer: 'unsupported',
    reason: '移动端尚未登记该组件，已阻止编辑和提交',
  })
})

test('protocol component names normalize across case and separator styles', () => {
  assert.equal(resolveMobileComponent('fileupload').type, 'fileUpload')
  assert.equal(resolveMobileComponent('REGION_TREE_SELECT').type, 'regionTreeSelect')
  assert.equal(resolveMobileComponent('input-number').type, 'number')
  assert.equal(resolveMobileComponent('inputNumber').type, 'number')
  assert.equal(resolveMobileComponent('datePicker').type, 'date')
  assert.equal(resolveMobileComponent('timePicker').type, 'time')
})

test('platform-specific components expose an explicit mini-program fallback', () => {
  assert.equal(resolveMobileComponent('iframe', { platform: 'h5' }).capability, MOBILE_COMPONENT_CAPABILITY.READONLY)
  assert.equal(resolveMobileComponent('iframe', { platform: 'mp-weixin' }).capability, MOBILE_COMPONENT_CAPABILITY.BLOCKED)
  assert.equal(resolveMobileComponent('pillSelect').type, 'radioButton')
})
