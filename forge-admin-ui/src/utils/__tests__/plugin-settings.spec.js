import { describe, expect, it } from 'vitest'
import { getPluginSettingsExtension } from '../plugin-settings'

describe('已编译可信插件设置注册', () => {
  it('拒绝远端模块、路径穿越、HTML和不存在的插件', () => {
    for (const id of [undefined, '', '../file-preview', 'https://example.test', '<script>', 'not-installed', 'file-preview'])
      expect(getPluginSettingsExtension(id)).toBeNull()
  })

  it('宿主不写死预览ID；设置扩展仅在已编译插件目录存在时注册', () => {
    expect(getPluginSettingsExtension('system')).toBeNull()
  })
})
