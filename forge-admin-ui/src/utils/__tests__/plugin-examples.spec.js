import { describe, expect, it } from 'vitest'
import { getPluginExamplesExtension } from '../plugin-examples'

describe('插件自有使用示例扩展', () => {
  it('社区默认不捆绑具体插件；非法 ID 与未安装插件都不可加载', () => {
    // 模板仓库不随包交付具体插件示例，扩展槽只在安装后由 views/plugins/*/examples 出现
    expect(getPluginExamplesExtension('file-preview')).toBeNull()
    expect(getPluginExamplesExtension('plugin-system')).toBeNull()
  })

  it.each(['../file-preview', 'file-preview/examples', '', undefined, 'https://example.test', '<script>'])(
    '非法模块引用不可加载：%s',
    (id) => {
      expect(getPluginExamplesExtension(id)).toBeNull()
    },
  )
})
