import fs from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'
import { createNavigationNode, normalizeInAppBuilder } from '../in-app-builder/in-app-builder-schema'

function readSource(relativePath) {
  return fs.readFileSync(path.resolve(process.cwd(), relativePath), 'utf8')
}

describe('page design tabs', () => {
  it('switches among form, list, settings and publish in page design mode', () => {
    const source = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    expect(source).toContain('switchPageDesignTab(\'form\')')
    expect(source).toContain('switchPageDesignTab(\'list\')')
    expect(source).toContain('switchPageDesignTab(\'settings\')')
    expect(source).toContain('switchPageDesignTab(\'publish\')')
    expect(source).toContain('activePageDesignTab === \'list\'')
    expect(source).toContain('PageDesignSettingsPanel')
    expect(source).toContain('PageDesignPublishPanel')
    expect(source).toContain('v-else-if="!editing && runtimeViewMode === \'pages\'"')
  })

  it('keeps print templates on the current page settings', () => {
    const runtimeSource = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    const settingsSource = readSource('src/views/app-center/components/designer/PageDesignSettingsPanel.vue')
    expect(runtimeSource).toContain(':application="application"')
    expect(runtimeSource).toContain(':objects="objects"')
    expect(settingsSource).toContain('key: \'printing\'')
    expect(settingsSource).toContain(':page-id="node.id"')
    expect(settingsSource).toContain('ApplicationPrintPanel')
    expect(settingsSource).toContain('页面水印')
  })

  it('keeps page navigation visibility when normalizing builder schema', () => {
    const hidden = normalizeInAppBuilder({
      inAppBuilder: {
        nodes: [{ id: 'page_hidden', type: 'page', title: '隐藏页', navigationVisible: false }],
      },
    }, { applicationName: '测试应用' }, [])
    expect(hidden.nodes[0]).toMatchObject({ id: 'page_hidden', navigationVisible: false })

    const created = createNavigationNode(hidden, { type: 'page', title: '新页面' })
    expect(created.nodes.find(node => node.title === '新页面')).toMatchObject({ navigationVisible: true })
  })

  it('uses div roots for embedded designer modals so Naive UI can keep input focus', () => {
    const runtimeSource = readSource('src/views/app-center/application-runtime.[applicationCode].vue')
    const processSource = readSource('src/views/app-center/business-process.[processId].vue')

    expect(runtimeSource).toContain('<div class="embedded-process-designer-shell">')
    expect(runtimeSource).not.toContain('<section class="embedded-process-designer-shell">')
    expect(processSource).toContain('<div class="embedded-flow-designer-shell">')
    expect(processSource).not.toContain('<section class="embedded-flow-designer-shell">')
  })
})
