import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import {
  afterBusinessObjectsLoaded,
  afterRelationsLoaded,
  erCanvasObjectCount,
  onObjectSwitched,
} from '../er-canvas-candidates'

/** 复现 bs001：企业档案仅关联联系人；套件第一项却是无关的客户档案 */
const SUITE_FIRST = 'customer'
const RELATION_TARGET = 'biz_company_contact'

/** 旧逻辑：空画布时用套件第一项填充（竞态根因） */
function legacySeedFirstSuiteObject(existing, firstOption) {
  if (!existing.length && firstOption)
    return [firstOption]
  return existing
}

describe('er canvas candidates (biz_company 2↔3 flicker)', () => {
  it('documents the old race: objects-first yields 3 canvas models', () => {
    let candidates = []
    candidates = legacySeedFirstSuiteObject(candidates, SUITE_FIRST)
    candidates = afterRelationsLoaded(candidates, [RELATION_TARGET])
    expect(candidates).toEqual([SUITE_FIRST, RELATION_TARGET])
    expect(erCanvasObjectCount(candidates)).toBe(3)
  })

  it('fixes objects-first race: stays at 2 models (main + contact)', () => {
    let candidates = []
    candidates = afterBusinessObjectsLoaded(candidates)
    candidates = afterRelationsLoaded(candidates, [RELATION_TARGET])
    expect(candidates).toEqual([RELATION_TARGET])
    expect(candidates).not.toContain(SUITE_FIRST)
    expect(erCanvasObjectCount(candidates)).toBe(2)
  })

  it('fixes relations-first race: same stable 2 models', () => {
    let candidates = []
    candidates = afterRelationsLoaded(candidates, [RELATION_TARGET])
    candidates = afterBusinessObjectsLoaded(candidates)
    expect(candidates).toEqual([RELATION_TARGET])
    expect(erCanvasObjectCount(candidates)).toBe(2)
  })

  it('both load orders converge to the same candidate set', () => {
    const objectsFirst = afterRelationsLoaded(
      afterBusinessObjectsLoaded([]),
      [RELATION_TARGET],
    )
    const relationsFirst = afterBusinessObjectsLoaded(
      afterRelationsLoaded([], [RELATION_TARGET]),
    )
    expect(objectsFirst).toEqual(relationsFirst)
    expect(erCanvasObjectCount(objectsFirst)).toBe(2)
  })

  it('clears candidates when switching object', () => {
    expect(onObjectSwitched()).toEqual([])
  })

  it('keeps explicit user picks when merging relation targets', () => {
    const candidates = afterRelationsLoaded(['customer'], [RELATION_TARGET])
    expect(candidates).toEqual(['customer', RELATION_TARGET])
    expect(erCanvasObjectCount(candidates)).toBe(3)
  })

  it('source no longer auto-seeds the first suite object onto the ER canvas', () => {
    const source = readFileSync(
      resolve('src/views/app-center/components/designer/composables/useBusinessRelationDesigner.part1.js'),
      'utf8',
    )
    expect(source).toContain('afterBusinessObjectsLoaded')
    expect(source).toContain('afterRelationsLoaded')
    expect(source).toContain('onObjectSwitched')
    expect(source).not.toContain('erCandidateObjectCodes.value = [targetObjectOptions.value[0].value]')
  })
})
