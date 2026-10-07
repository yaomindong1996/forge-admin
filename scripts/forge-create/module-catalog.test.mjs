import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import test from 'node:test'

const catalog = JSON.parse(await fs.readFile(new URL('./module-catalog.json', import.meta.url), 'utf8'))
const byArtifact = new Map(Object.entries(catalog.modules).map(([id, module]) => [module.artifactId, id]))

function closure(roots) {
  const selected = new Set()
  const visit = (id) => {
    assert.ok(catalog.modules[id], `未知模块依赖：${id}`)
    if (selected.has(id)) {
      return
    }
    selected.add(id)
    for (const dependency of catalog.modules[id].dependencies || []) {
      visit(dependency)
    }
  }
  roots.forEach(visit)
  return selected
}

function internalDependencies(pom) {
  // 仅验证本轮三个模块的标准 dependency 块，剔除注释，不把 parent 或外部依赖混进编译闭包。
  const xml = pom.replace(/<!--[\s\S]*?-->/g, '')
  return [...xml.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)]
    .map(([, block]) => {
      const group = block.match(/<groupId>\s*([^<]+?)\s*<\/groupId>/)?.[1]
      const scope = block.match(/<scope>\s*([^<]+?)\s*<\/scope>/)?.[1]
      const artifact = block.match(/<artifactId>\s*([^<]+?)\s*<\/artifactId>/)?.[1]
      return group === 'com.mdframe.forge' && scope !== 'test' ? artifact : null
    })
    .filter(Boolean)
}

test('打印模块在目录清单和 full 根模块中明确登记', async () => {
  const print = catalog.modules['plugin-print']
  assert.equal(print.type, 'plugin')
  assert.equal(print.artifactId, 'forge-plugin-print')
  assert.equal(print.path, 'forge-server/forge-framework/forge-plugin-parent/forge-plugin-print')
  assert.ok(catalog.presets.full.roots.includes('plugin-print'))
  await fs.access(new URL(`../../${print.path}/pom.xml`, import.meta.url))
})

for (const id of ['plugin-print', 'plugin-generator', 'plugin-data']) {
  test(`${id} 的真实内部 POM 依赖必须登记，不能被脚手架裁掉`, async () => {
    const module = catalog.modules[id]
    const pom = await fs.readFile(new URL(`../../${module.path}/pom.xml`, import.meta.url), 'utf8')
    const dependencies = internalDependencies(pom)
    assert.ok(dependencies.length > 0, '必须读到真实 POM 依赖，不能空断言通过')
    for (const artifact of dependencies) {
      const dependencyId = byArtifact.get(artifact)
      assert.ok(dependencyId, `未登记的内部 artifact：${artifact}`)
      assert.ok(module.dependencies.includes(dependencyId), `${id} 遗漏直接编译依赖 ${dependencyId}`)
    }
  })
}

for (const [name, preset] of Object.entries(catalog.presets)) {
  test(`${name} 的完整闭包无未知模块，保留 generator/data 的打印依赖`, () => {
    const selected = closure(preset.roots)
    if (selected.has('plugin-generator')) {
      for (const dependency of ['plugin-print', 'plugin-data', 'plugin-external']) {
        assert.ok(selected.has(dependency), `${name} 缺少 ${dependency}`)
      }
    }
    if (selected.has('plugin-data')) {
      assert.ok(selected.has('plugin-print'), `${name} 的数据模块缺少打印依赖`)
    }
  })
}

test('minimal-admin 的编译闭包补齐后仍不引入 AI 模块，沿用生成器降级适配器', () => {
  const selected = closure(catalog.presets['minimal-admin'].roots)
  assert.ok(selected.has('plugin-generator'))
  assert.ok(selected.has('plugin-print'))
  assert.equal(selected.has('plugin-ai'), false)
})
