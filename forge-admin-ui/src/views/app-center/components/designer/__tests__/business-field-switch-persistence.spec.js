import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

describe('business field switch persistence contract', () => {
  it('saves the current field before leaving the data structure panel', () => {
    const designerSource = readSource('src/views/app-center/object-designer.[objectCode].vue')

    expect(designerSource).toContain(':confirm-dirty-switch="!fieldDraftDirty"')
    expect(designerSource).toContain('@dirty-change="handleFieldDirtyChange"')
    expect(designerSource).toContain('if (!await saveFieldDraftBeforePanelSwitch())')
    expect(designerSource).toContain('await fieldManagerRef.value?.saveSelectedField?.()')
    expect(designerSource).toContain('if (saved !== true)')
  })

  it('delegates a field-draft navigation directly to the parent save coordinator', () => {
    const shellSource = readSource('src/views/app-center/components/designer/BusinessObjectDesignerShell.vue')

    expect(shellSource).toContain('confirmDirtySwitch')
    expect(shellSource).toContain('if (!props.dirty || !props.confirmDirtySwitch)')
  })

  it('keeps field actions in the always-visible property header', () => {
    const propertySource = readSource('src/views/app-center/components/designer/BusinessFieldPropertyPanel.vue')

    expect(propertySource).toContain('class="property-head-buttons"')
    expect(propertySource).toContain('<SaveOutline />')
    expect(propertySource).not.toContain('class="property-footer"')
  })
})
