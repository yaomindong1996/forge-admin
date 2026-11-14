import { describe, expect, it } from 'vitest'
import { buildReduceSignOptions, getSignRelationUserName } from '../signRelations'

describe('sign relations', () => {
  it('shows the target user name instead of the raw user id', () => {
    expect(getSignRelationUserName({ targetUserId: '70', targetUserName: 'test33' })).toBe('test33')
    expect(getSignRelationUserName({ targetUserId: '70' })).toBe('70')
    expect(getSignRelationUserName(null)).toBe('-')
  })

  it('only offers active relations for reduce sign', () => {
    const options = buildReduceSignOptions([
      { targetUserId: 70, targetUserName: 'test33', status: 1 },
      { targetUserId: '71', targetUserName: '已撤回', status: 0 },
      { targetUserName: '缺少用户', status: 1 },
    ])
    expect(options).toEqual([{ label: 'test33', value: '70' }])
    expect(buildReduceSignOptions(undefined)).toEqual([])
  })
})
