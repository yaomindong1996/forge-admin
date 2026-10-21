import { describe, expect, it } from 'vitest'
import { getPluginGuideExtension } from '../plugin-guide'

describe('已编译插件使用说明扩展', () => {
  it('只接受插件自有固定路径；模板默认无插件时返回空', () => {
    // 社区模板不捆绑具体插件 guide；安装后由 views/plugins/*/guide/index.vue 提供
    expect(getPluginGuideExtension('file-preview')).toBeNull()
    expect(getPluginGuideExtension('plugin-system')).toBeNull()
    for (const id of ['../file-preview', 'https://example.com', '', null, 'file-preview/guide'])
      expect(getPluginGuideExtension(id)).toBeNull()
  })
})
