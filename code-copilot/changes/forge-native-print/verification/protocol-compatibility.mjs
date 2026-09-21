import { createRequire } from 'node:module'
import { readFile, mkdtemp, rm } from 'node:fs/promises'
import path from 'node:path'
import os from 'node:os'
import { fileURLToPath, pathToFileURL } from 'node:url'
import assert from 'node:assert/strict'

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..')
const ui = path.join(repo, 'forge-admin-ui')
const require = createRequire(path.join(ui, 'package.json'))
const { build } = await import(pathToFileURL(require.resolve('vite')).href)
const fixtures = path.join(repo, 'forge-server/forge-framework/forge-plugin-parent/forge-plugin-print/src/test/resources/print')
const base = JSON.parse(await readFile(path.join(fixtures, 'valid-document.json'), 'utf8'))
const cases = JSON.parse(await readFile(path.join(fixtures, 'compatibility-cases.json'), 'utf8'))
const outDir = await mkdtemp(path.join(os.tmpdir(), 'forge-print-protocol-'))
try {
  await build({
    configFile: false,
    root: ui,
    logLevel: 'error',
    build: { outDir, emptyOutDir: true, minify: false, lib: {
      entry: path.join(ui, 'src/components/print/protocol/validate.js'),
      formats: ['es'], fileName: () => 'protocol.mjs',
    } },
  })
  const { validatePrintDocument } = await import(pathToFileURL(path.join(outDir, 'protocol.mjs')).href)
  for (const item of cases) {
    const document = structuredClone(base)
    for (const patch of item.patches) {
      const keys = patch.pointer.slice(1).split('/')
      const parent = keys.slice(0, -1).reduce((value, key) => value[key], document)
      // 定义自有键，避免 __proto__ 测例变成测试进程的原型赋值。
      Object.defineProperty(parent, keys.at(-1), { value: patch.value, enumerable: true, writable: true, configurable: true })
    }
    const issues = validatePrintDocument(document)
    assert.equal(issues.length === 0, item.frontendValid, `${item.name}: ${JSON.stringify(issues)}`)
  }
  console.log(JSON.stringify({ frontendCases: cases.length, passed: cases.length,
    explicitlyStricterBackend: cases.filter(item => item.frontendValid !== item.backendValid).map(item => item.name) }))
} finally {
  await rm(outDir, { recursive: true, force: true })
}
