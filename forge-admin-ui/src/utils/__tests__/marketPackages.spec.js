import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'
import { assertPrintDocument } from '@/components/print/protocol/validate'
import { normalizeFormDesignerSchema } from '@/views/app-center/components/designer/form-first/formDesignerSchema'
import { unwrapContentPackage } from '../contentPackage'

const dir = join(dirname(fileURLToPath(import.meta.url)), 'fixtures/market')

function readJson(name) {
  return readFileSync(join(dir, name), 'utf8')
}

describe('official market sample packages', () => {
  it('imports leave form package into form designer schema', () => {
    const pack = unwrapContentPackage(readJson('leave-request.form.json'))
    expect(pack.kind).toBe('form')
    const schema = normalizeFormDesignerSchema(pack.payload)
    expect(schema.formKey).toBe('market_leave_form')
    expect(schema.components.length).toBeGreaterThanOrEqual(10)
    expect(schema.components.every(item => item.componentKey && item.id)).toBe(true)
  })

  it('imports leave print package as forge-print document', () => {
    const pack = unwrapContentPackage(readJson('leave-slip.print.json'))
    expect(pack.kind).toBe('print')
    expect(() => assertPrintDocument(pack.payload)).not.toThrow()
    const table = pack.payload.body.flatMap(section => section.elements || []).find(item => item.type === 'STATIC_TABLE')
    expect(table?.table?.cells?.length).toBeGreaterThanOrEqual(20)
    expect(table.table.cells.some(cell => cell.binding?.path === 'main.applicantName')).toBe(true)
    expect(table.table.cells.some(cell => cell.colSpan > 1)).toBe(true)
  })

  it('imports approval print-component elements', () => {
    const pack = unwrapContentPackage(readJson('approval-status.print-component.json'))
    expect(pack.kind).toBe('print-component')
    expect(pack.payload.elements?.length).toBeGreaterThanOrEqual(20)
    expect(pack.payload.elements.every(item => item.type && item.id)).toBe(true)
    expect(pack.payload.elements.some(item => item.type === 'RECTANGLE')).toBe(true)
    expect(pack.name).toMatch(/审批/)
  })

  it('keeps forge-application-bundle shape for leave app', () => {
    const bundle = JSON.parse(readJson('leave-request.forge-app.json'))
    expect(bundle.format).toBe('forge-application-bundle')
    expect(bundle.schemaVersion).toBe(2)
    expect(bundle.application.suiteCode).toBeTruthy()
    expect(bundle.objects[0].objectCode).toBe('leave_request')
    expect(bundle.objects[0].formDesignerSchema?.components?.length).toBeGreaterThan(0)
    const builder = bundle.application.options.inAppBuilder
    expect(builder.nodes?.map(n => n.id)).toEqual([
      'page_leave_home',
      'page_leave_list',
      'page_leave_apply',
      'page_leave_approve',
    ])
    expect(builder.homePageId).toBe('page_leave_home')
    expect(builder.pages.page_leave_list.layout.gridLayout.items[0]).toEqual(expect.objectContaining({
      blockType: 'AiCrudPage',
      props: expect.objectContaining({ formOnly: false, showSearch: true }),
    }))
    expect(builder.pages.page_leave_apply.layout.gridLayout.items[0]).toEqual(expect.objectContaining({
      blockType: 'AiCrudPage',
      props: expect.objectContaining({ formOnly: true }),
    }))
    expect(builder.pages.page_leave_approve.layout.gridLayout.layoutType).toBe('list-form')
    expect(builder.formAssets?.[0]?.id).toBe('form_leave_request')
  })

  it('keeps rich multi-page shape for employee onboarding app', () => {
    const bundle = JSON.parse(readJson('employee-onboarding.forge-app.json'))
    expect(bundle.format).toBe('forge-application-bundle')
    expect(bundle.objects[0].objectCode).toBe('employee_onboard')
    expect(bundle.objects[0].formDesignerSchema?.components?.length).toBeGreaterThanOrEqual(20)
    const builder = bundle.application.options.inAppBuilder
    expect(builder.nodes?.map(n => n.id)).toEqual([
      'page_onboard_home',
      'page_onboard_list',
      'page_onboard_apply',
      'page_onboard_docs',
      'page_onboard_unlock',
    ])
    expect(builder.pages.page_onboard_list.layout.gridLayout.items[0].props.formOnly).toBe(false)
    expect(builder.pages.page_onboard_apply.layout.gridLayout.items[0].props.formOnly).toBe(true)
    expect(builder.pages.page_onboard_docs.layout.gridLayout.layoutType).toBe('list-form')
    expect(builder.formAssets?.[0]?.id).toBe('form_employee_onboard')
  })
})
