import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const flowDir = resolve(__dirname, '../..')
const read = file => readFileSync(resolve(flowDir, file), 'utf8')

// design.vue 是 Options 薄壳，模板只能用 useFlowDesign 返回的成员；composable 内部导入的工具函数不会自动暴露。
function templateCalls(sfc) {
  const template = sfc.slice(sfc.indexOf('<template>'), sfc.lastIndexOf('</template>'))
  return new Set([...template.matchAll(/(?<![\w.$])([a-z]\w*)\(/g)].map(match => match[1]))
}

function returnedMembers(source) {
  const block = source.slice(source.lastIndexOf('  return {'))
  return new Set([...block.matchAll(/\b([a-z_$][\w$]*)\b/gi)].map(match => match[1]))
}

describe('flow design template bindings', () => {
  it('exposes every composable-imported helper the template calls', () => {
    const calls = templateCalls(read('design.vue'))
    const part1 = read('composables/useFlowDesign.part1.js')
    const imported = new Set([...part1.matchAll(/^import \{([^}]+)\} from/gm)]
      .flatMap(match => match[1].split(',').map(name => name.trim().split(/\s+as\s+/).pop()))
      .filter(Boolean))
    const exposed = returnedMembers(part1)
    const missing = [...calls].filter(name => imported.has(name) && !exposed.has(name))
    expect(missing).toEqual([])
    expect(exposed.has('sanitizeHtml')).toBe(true)
  })
})
