import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const testDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(testDir, '../../../..')

function read(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const mobileApi = read('forge-h5-ui/src/api/index.js')
const generatorControllerRoot = 'forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/controller'
const systemControllerRoot = 'forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller'

const backendContracts = {
  crudConfig: read(`${generatorControllerRoot}/AiCrudConfigController.java`),
  dynamicCrud: read(`${generatorControllerRoot}/DynamicCrudController.java`),
  businessFlow: read(`${generatorControllerRoot}/BusinessFlowController.java`),
  querySource: read(`${generatorControllerRoot}/LowcodeQuerySourceController.java`),
  recordSelector: read(`${generatorControllerRoot}/BusinessRecordSelectorController.java`),
  flowTask: read('forge-server/forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowTaskController.java'),
  user: read(`${systemControllerRoot}/SysUserController.java`),
  org: read(`${systemControllerRoot}/SysOrgController.java`),
  region: read(`${systemControllerRoot}/SysRegionController.java`),
  dict: read(`${systemControllerRoot}/SysDictDataController.java`),
}

function includesAll(source, fragments) {
  fragments.forEach(fragment => assert.ok(source.includes(fragment), `missing contract fragment: ${fragment}`))
}

test('mobile runtime reuses the published low-code and selector protocols', () => {
  includesAll(mobileApi, [
    '/ai/crud-config/render/',
    '/ai/crud/${encodeURIComponent(configKey)}/page',
    '/ai/crud/${encodeURIComponent(configKey)}/${encodeURIComponent(id)}',
    '/ai/lowcode/query-source/execute',
    '/ai/business/selector/query',
    '/system/user/page',
    '/system/org/tree',
    '/system/region/treeAll',
    '/system/dict/data/type/',
  ])
  includesAll(backendContracts.crudConfig, ['@RequestMapping("/ai/crud-config")', '@GetMapping("/render/{configKey}")'])
  includesAll(backendContracts.dynamicCrud, ['@RequestMapping("/ai/crud/{configKey}")', '@GetMapping("/page")', '@GetMapping("/{id}")', '@PostMapping', '@PutMapping', '@DeleteMapping("/{id}")'])
  includesAll(backendContracts.querySource, ['@RequestMapping("/ai/lowcode/query-source")', '@PostMapping("/execute")'])
  includesAll(backendContracts.recordSelector, ['@RequestMapping("/ai/business/selector")', '@PostMapping("/query")'])
  includesAll(backendContracts.user, ['@RequestMapping("/system/user")', '@GetMapping("/page")'])
  includesAll(backendContracts.org, ['@RequestMapping("/system/org")', '@GetMapping("/tree")'])
  includesAll(backendContracts.region, ['@RequestMapping("/system/region")', '@GetMapping("/treeAll")'])
  includesAll(backendContracts.dict, ['@RequestMapping("/system/dict/data")', '@GetMapping("/type/{dictType}")'])
})

test('approval forms and actions reuse the existing business-flow and Flowable contracts', () => {
  includesAll(mobileApi, [
    '/ai/business/flow/task-form-context',
    '/ai/business/flow/task-form-context/readonly',
    '/ai/business/flow/task-action',
    '/api/flow/task/approve',
    '/api/flow/task/reject',
    '/api/flow/task/return',
    '/api/flow/task/delegate',
    '/api/flow/task/history/',
  ])
  includesAll(backendContracts.businessFlow, [
    '@RequestMapping("/ai/business/flow")',
    '@GetMapping("/task-form-context")',
    '@GetMapping("/task-form-context/readonly")',
    '@PutMapping("/task-form-context")',
    '@PostMapping("/task-action")',
  ])
  includesAll(backendContracts.flowTask, [
    '@RequestMapping("/api/flow/task")',
    '@PostMapping("/approve")',
    '@PostMapping("/reject")',
    '@PostMapping("/return")',
    '@PostMapping("/delegate")',
    '@GetMapping("/history/{processInstanceId}")',
  ])
  assert.doesNotMatch(mobileApi, /\/(?:mobile|h5)\/(?:crud|lowcode|flow)/i)
})
