import { describe, expect, it, vi } from 'vitest'
import { DEFAULT_PRINT_SCENES, scenesOfTemplate, syncPrintTemplateScenes } from '../printSceneBinding'

describe('print scene binding', () => {
  it('reads enabled scenes for a template', () => {
    expect(scenesOfTemplate([
      { templateId: 9, scene: 'LIST', status: 1 },
      { templateId: 9, scene: 'DETAIL', status: 0 },
      { templateId: '9', scene: 'FLOW_TODO', status: 1 },
      { templateId: 8, scene: 'LIST', status: 1 },
    ], 9)).toEqual(['LIST', 'FLOW_TODO'])
  })

  it('saves missing scenes and removes extras', async () => {
    const save = vi.fn(async payload => ({
      data: { id: 9, templateId: 9, scene: payload.scene, status: 1, bindingRevision: 1 },
    }))
    const remove = vi.fn()
    const next = await syncPrintTemplateScenes({
      source: { applicationId: '1', pageId: 'page_buy' },
      templateId: 9,
      scenes: ['LIST', 'DETAIL'],
      bindings: [
        { id: 1, templateId: 9, scene: 'LIST', status: 1, bindingRevision: 2, sortOrder: 0 },
        { id: 2, templateId: 9, scene: 'FLOW_TODO', status: 1, bindingRevision: 3, sortOrder: 1 },
      ],
      save,
      remove,
    })
    expect(save).toHaveBeenCalledWith(expect.objectContaining({
      source: { applicationId: '1', pageId: 'page_buy' },
      templateId: 9,
      scene: 'DETAIL',
      isDefault: true,
      status: 1,
    }))
    expect(remove).toHaveBeenCalledWith(2, 3)
    expect(scenesOfTemplate(next, 9)).toEqual(['LIST', 'DETAIL'])
  })

  it('reactivates a disabled binding instead of inserting again', async () => {
    const save = vi.fn(async payload => ({
      data: {
        id: payload.id ?? 8,
        templateId: 9,
        scene: payload.scene,
        status: 1,
        bindingRevision: (payload.expectedRevision || 0) + 1,
        sortOrder: payload.sortOrder ?? 0,
      },
    }))
    const remove = vi.fn()
    const next = await syncPrintTemplateScenes({
      source: { applicationId: '1' },
      templateId: 9,
      scenes: DEFAULT_PRINT_SCENES,
      bindings: [{ id: 4, templateId: 9, scene: 'LIST', status: 0, bindingRevision: 6, sortOrder: 2 }],
      save,
      remove,
    })
    expect(save).toHaveBeenCalledWith(expect.objectContaining({
      id: 4,
      expectedRevision: 6,
      scene: 'LIST',
      sortOrder: 2,
    }))
    expect(save).toHaveBeenCalledWith(expect.objectContaining({ scene: 'DETAIL', id: undefined }))
    expect(remove).not.toHaveBeenCalled()
    expect(scenesOfTemplate(next, 9)).toEqual(['LIST', 'DETAIL'])
  })

  it('skips the network when the selected scenes already match', async () => {
    const save = vi.fn()
    const remove = vi.fn()
    const bindings = [{ id: 1, templateId: 9, scene: 'LIST', status: 1, bindingRevision: 1 }]
    const next = await syncPrintTemplateScenes({
      source: { applicationId: '1' },
      templateId: 9,
      scenes: ['LIST'],
      bindings,
      save,
      remove,
    })
    expect(save).not.toHaveBeenCalled()
    expect(remove).not.toHaveBeenCalled()
    expect(next).toEqual(bindings)
    expect(next).not.toBe(bindings)
  })

  it('updates fixed versions even when the selected scenes are unchanged', async () => {
    const save = vi.fn(async payload => ({
      data: {
        ...payload,
        id: payload.id,
        bindingRevision: payload.expectedRevision + 1,
      },
    }))
    const remove = vi.fn()
    const next = await syncPrintTemplateScenes({
      source: { businessSourceId: '5', sourceCode: 'purchase_order' },
      templateId: 9,
      templateVersionId: '202',
      scenes: ['DETAIL'],
      bindings: [{
        id: 11,
        templateId: 9,
        templateVersionId: '201',
        scene: 'DETAIL',
        status: 1,
        bindingRevision: 3,
      }],
      save,
      remove,
    })
    expect(save).toHaveBeenCalledWith(expect.objectContaining({
      id: 11,
      expectedRevision: 3,
      templateVersionId: '202',
      scene: 'DETAIL',
    }))
    expect(remove).not.toHaveBeenCalled()
    expect(next[0].templateVersionId).toBe('202')
  })
})
