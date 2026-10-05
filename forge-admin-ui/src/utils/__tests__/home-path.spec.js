import { describe, expect, it } from 'vitest'
import { getHomePath, isHomeTabPath, normalizeTabPathname } from '../home-path'

describe('首页路径', () => {
  it('把 / 和带查询的 /home 都认成首页', () => {
    expect(normalizeTabPathname('/home?x=1')).toBe('/home')
    expect(isHomeTabPath('/')).toBe(true)
    expect(isHomeTabPath('/home')).toBe(true)
    expect(isHomeTabPath(getHomePath())).toBe(true)
    expect(isHomeTabPath('/system/user')).toBe(false)
  })
})
