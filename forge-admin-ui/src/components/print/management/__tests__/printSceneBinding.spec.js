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
    const save = vi.fn()
    const remove = vi.fn()
    await syncPrintTemplateScenes({
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
  })

  it('reactivates a disabled binding instead of inserting again', async () => {
    const save = vi.fn()
    const remove = vi.fn()
    await syncPrintTemplateScenes({
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
  })

  it('skips the network when the selected scenes already match', async () => {
    const save = vi.fn()
    const remove = vi.fn()
    await syncPrintTemplateScenes({
      source: { applicationId: '1' },
      templateId: 9,
      scenes: ['LIST'],
      bindings: [{ id: 1, templateId: 9, scene: 'LIST', status: 1, bindingRevision: 1 }],
      save,
      remove,
    })
    expect(save).not.toHaveBeenCalled()
    expect(remove).not.toHaveBeenCalled()
  })
})
