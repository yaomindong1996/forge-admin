import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { pruneOptionalAdminGlue } from './source-glue.mjs'

const dependencies = [
  'forge-plugin-generator',
  'forge-plugin-capability-platform',
  'forge-plugin-capability-actions',
]
const integrationFiles = [
  'src/main/java/com/mdframe/forge/admin/integration/controller/ApplicationIntegrationController.java',
  'src/test/java/com/mdframe/forge/admin/integration/ApplicationIntegrationServiceTest.java',
  'src/main/resources/mapper/ApplicationIntegrationMapper.xml',
]
const unrelatedFiles = [
  'src/main/java/com/mdframe/forge/admin/controller/OtherController.java',
  'src/test/java/com/mdframe/forge/admin/OtherTest.java',
  'src/main/resources/mapper/OtherMapper.xml',
]

async function withFixture(t, callback) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'forge-admin-glue-'))
  t.after(() => fs.rm(root, { recursive: true, force: true }))
  for (const file of [...integrationFiles, ...unrelatedFiles]) {
    const target = path.join(root, file)
    await fs.mkdir(path.dirname(target), { recursive: true })
    await fs.writeFile(target, file)
  }
  await callback(root)
}

async function assertUnrelatedFiles(root) {
  for (const file of unrelatedFiles) {
    assert.equal(await fs.readFile(path.join(root, file), 'utf8'), file)
  }
}

test('all optional integration dependencies retain main/test/mapper unchanged', async (t) => {
  await withFixture(t, async (root) => {
    await pruneOptionalAdminGlue(root, new Set(dependencies))
    for (const file of integrationFiles) {
      assert.equal(await fs.readFile(path.join(root, file), 'utf8'), file)
    }
    await assertUnrelatedFiles(root)
  })
})

for (const missingDependency of dependencies) {
  test(`missing ${missingDependency} prunes only optional integration files`, async (t) => {
    await withFixture(t, async (root) => {
      const selectedArtifacts = new Set(dependencies.filter(artifact => artifact !== missingDependency))
      await pruneOptionalAdminGlue(root, selectedArtifacts)
      for (const file of integrationFiles) {
        await assert.rejects(fs.stat(path.join(root, file)), { code: 'ENOENT' })
      }
      await assertUnrelatedFiles(root)
    })
  })
}

test('pruning optional integration is idempotent with all dependencies absent', async (t) => {
  await withFixture(t, async (root) => {
    await pruneOptionalAdminGlue(root, new Set())
    await pruneOptionalAdminGlue(root, new Set())
    for (const file of integrationFiles) {
      await assert.rejects(fs.stat(path.join(root, file)), { code: 'ENOENT' })
    }
    await assertUnrelatedFiles(root)
  })
})
