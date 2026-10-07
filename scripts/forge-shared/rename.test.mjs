import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'
import {
  buildArtifactMap, buildApplicationClassMap, buildTextReplacements, applyTextReplacements,
  rewritePomGroupIds, rewriteRootPomArtifact, rewriteTextFiles, moveJavaPackageDirectories,
  renameFilesByBasename, renameArtifactDirectories, renameSourceTree, toSnakeCase,
} from './rename.mjs'
import { collectFiles, collectDirectories, exists } from './files.mjs'

const catalog = JSON.parse(await fs.readFile(new URL('../forge-create/module-catalog.json', import.meta.url), 'utf8'))
const options = {
  projectName: 'acme-app', artifactPrefix: 'foundation', moduleArtifactPrefix: 'core',
  javaName: 'Acme', basePackage: 'com.acme.runtime', groupId: 'org.acme.maven',
  databaseName: 'acme_db', displayName: '示例系统', adminTitle: '示例系统',
  adminPort: '3210', adminPublicPath: '/console', adminBaseUrl: '/console',
  adminApiPrefix: '/gateway', adminProxyTarget: 'http://localhost:8581',
}
const selection = { frontendIds: new Set(['admin-ui', 'report-ui', 'h5-ui']) }
const artifacts = buildArtifactMap(catalog, options)
const classes = buildApplicationClassMap(options)
const replacements = buildTextReplacements(artifacts, classes, options, selection)
const replace = value => applyTextReplacements(value, replacements)

async function fixture(t, files = {}) {
  const directory = await fs.mkdtemp(path.join(tmpdir(), 'forge-rename-'))
  // 只回收本用例通过 mkdtemp 创建的目录，不触碰用户工程或其它测试的临时目录。
  t.after(() => fs.rm(directory, { recursive: true, force: true }))
  for (const [name, content] of Object.entries(files)) {
    const file = path.join(directory, name)
    await fs.mkdir(path.dirname(file), { recursive: true })
    await fs.writeFile(file, content)
  }
  return directory
}

const read = (directory, file) => fs.readFile(path.join(directory, file), 'utf8')

test('模块映射覆盖目录清单及旧别名，子模块前缀独立于根工程前缀', () => {
  for (const module of Object.values(catalog.modules)) {
    if (module.artifactId) {
      assert.equal(artifacts[module.artifactId], module.artifactId.replace(/^forge-/, 'core-'))
    }
  }
  assert.equal(artifacts['forge-admin'], 'core-admin')
  assert.equal(artifacts['forge-report'], 'core-report')
  assert.equal(artifacts['forge-starter-property'], 'core-starter-property')
  assert.equal(artifacts['forge-starter-parent'], 'core-starter-parent')
  assert.equal(artifacts['forge-server'], undefined)
})

test('启动类映射与文本替换分开维护，不能修改调用者选项', () => {
  assert.deepEqual(classes, {
    ForgeAdminApplication: 'AcmeAdminApplication', ForgeReportApplication: 'AcmeReportApplication',
    ForgeAppServerApplication: 'AcmeAppServerApplication', ForgeFlowApplication: 'AcmeFlowApplication',
  })
  assert.equal(options.javaName, 'Acme')
})

test('文本替换按给定顺序执行，按字面量处理正则字符及替换值中的美元符号', () => {
  assert.equal(applyTextReplacements('a.b|aXb', [['a.b', '$1'], ['$1', '${value}']]), '${value}|aXb')
  assert.equal(applyTextReplacements('aa', [['a', 'b'], ['b', 'c']]), 'cc')
  assert.equal(applyTextReplacements('原样', []), '原样')
})

test('Java 包路径、数据库名和主迁移历史使用一致的规范化名称', () => {
  assert.equal(replace('com.mdframe.forge|com/mdframe/forge|forge_admin_new|forge_admin|forge_schema_history'),
    'com.acme.runtime|com/acme/runtime|acme_db|acme_db|acme_app_schema_history')
  assert.equal(replace('com.forge|Forge Admin|Forge 工作台'), 'com.acme.runtime|示例系统|Acme 工作台')
})

test('Docker 与 H5 具体前缀先于通用模块和公开路径替换', () => {
  assert.equal(replace('docker-forge-admin|forge-h5-ui|/forge-h5-api|forge_h5'),
    'docker-acme-app|acme-app-h5-ui|/acme-app-h5-api|acme_app_h5')
  assert.equal(replace('VITE_PUBLIC_PATH=/forge-h5\nVITE_PUBLIC_PATH=/forge\n'),
    'VITE_PUBLIC_PATH=/acme-app-h5\nVITE_PUBLIC_PATH=/console\n')
})

test('不选择 H5 时不替换 H5 专属路径和客户端标识', () => {
  const rules = buildTextReplacements(artifacts, classes, options, { frontendIds: new Set(['admin-ui']) })
  assert.equal(applyTextReplacements('/forge-h5-api|forge_h5', rules), '/forge-h5-api|forge_h5')
})

test('旧目录别名与替换中间态均落到同一个根工程，较长模块名不会被截断', () => {
  assert.equal(replace('forge-server/forge-admin-server|forge/forge-admin-server|forge-server/core-admin-server'),
    'foundation-server/core-admin-server|foundation-server/core-admin-server|foundation-server/core-admin-server')
  assert.equal(replace('forge-server/forge-admin/|forge/forge-admin/|forge-admin/'),
    'foundation-server/core-admin-server/|foundation-server/core-admin-server/|core-admin-server/')
  assert.equal(replace('forge-starter-plugin|forge-starter-parent|forge-server/db|forge/scripts|forge/var'),
    'core-starter-plugin|core-starter-parent|foundation-server/db|foundation-server/scripts|foundation-server/var')
})

test('自定义前端端口、路径、API 前缀和代理沿用原规则', () => {
  const before = ['VITE_HTTP_PORT=3000', 'VITE_HTTP_PORT=5174', 'VITE_PUBLIC_PATH=/forge',
    'VITE_BASE_URL=/forge', 'VITE_REQUEST_PREFIX=/forge-api', 'VITE_REQUEST_PREFIX=/dev-api',
    'VITE_HTTP_PROXY_TARGET=http://127.0.0.1:8580/'].join('\n')
  const after = ['VITE_HTTP_PORT=3210', 'VITE_HTTP_PORT=3210', 'VITE_PUBLIC_PATH=/console',
    'VITE_BASE_URL=/console', 'VITE_REQUEST_PREFIX=/gateway', 'VITE_REQUEST_PREFIX=/gateway',
    'VITE_HTTP_PROXY_TARGET=http://localhost:8581/'].join('\n')
  assert.equal(replace(before), after)
})

test('仅启用报表时替换报表路径、SSO 客户端和 JSON 客户端键', () => {
  const value = '/forge-report|VITE_SSO_TARGET_CLIENT=forge_website_report|"forge_website_report":'
  assert.equal(replace(value),
    '/acme-app-report|VITE_SSO_TARGET_CLIENT=acme_app_website_report|"acme_app_website_report":')
  const rules = buildTextReplacements(artifacts, classes, options, { frontendIds: new Set(['admin-ui']) })
  // 未选报表时仍存在旧模块别名 forge-report；只有报表前端/SSO 专属配置保持不变。
  assert.equal(applyTextReplacements(value, rules),
    '/core-report|VITE_SSO_TARGET_CLIENT=forge_website_report|"forge_website_report":')
})

test('文本替换不会修改输入映射或客户端选择集合', () => {
  const mapBefore = JSON.stringify(artifacts)
  const classBefore = JSON.stringify(classes)
  buildTextReplacements(artifacts, classes, options, selection)
  assert.equal(JSON.stringify(artifacts), mapBefore)
  assert.equal(JSON.stringify(classes), classBefore)
  assert.deepEqual([...selection.frontendIds], ['admin-ui', 'report-ui', 'h5-ui'])
})

test('POM groupId 独立替换，普通源码不会提前被 Maven 坐标改名', async (t) => {
  const directory = await fixture(t, {
    'a/pom.xml': '<groupId>com.mdframe.forge</groupId>', 'source.java': 'package com.mdframe.forge;',
  })
  await rewritePomGroupIds(directory, options.groupId)
  assert.equal(await read(directory, 'a/pom.xml'), '<groupId>org.acme.maven</groupId>')
  assert.equal(await read(directory, 'source.java'), 'package com.mdframe.forge;')
})

test('主工程及旧别名的 POM artifact/name/description 一起改名', async (t) => {
  const directory = await fixture(t, {
    'pom.xml': '<artifactId>forge-server</artifactId><name>forge</name><description>forge</description>',
    'notes.txt': 'forge-server',
  })
  await rewriteRootPomArtifact(directory, 'foundation-server')
  assert.equal(await read(directory, 'pom.xml'),
    '<artifactId>foundation-server</artifactId><name>foundation-server</name>'
      + '<description>foundation-server</description>')
  assert.equal(await read(directory, 'notes.txt'), 'forge-server')
})

test('无报表模块时删除活跃和注释的 SSO 配置行，保留其它正文', async (t) => {
  const directory = await fixture(t, { '.env': ['# VITE_SSO_ENABLED=true', 'VITE_SSO_TARGET_CLIENT=forge_report',
    '# VITE_REPORT_UI_PATH_PREFIX=/forge-report', 'VITE_REPORT_UI_HOST=local',
    'VITE_TITLE=自定义', 'const x = "VITE_SSO_ENABLED"'].join('\n') })
  await rewriteTextFiles(directory, replacements, false)
  assert.equal(await read(directory, '.env'), 'VITE_TITLE=自定义\nconst x = "VITE_SSO_ENABLED"')
})

test('有报表时保留配置，点文件与 SQL 改名，已知二进制原始字节不变', async (t) => {
  const bytes = Buffer.from([0, 255, ...Buffer.from('com.mdframe.forge|forge_admin'), 128])
  const directory = await fixture(t, {
    '.env': 'VITE_SSO_TARGET_CLIENT=forge_report\n', 'migration.sql': 'forge_schema_history',
    'logo.PNG': bytes, 'archive.pdf': bytes,
  })
  await rewriteTextFiles(directory, replacements, true)
  assert.equal(await read(directory, '.env'), 'VITE_SSO_TARGET_CLIENT=acme_app_report\n')
  assert.equal(await read(directory, 'migration.sql'), 'acme_app_schema_history')
  assert.deepEqual(await fs.readFile(path.join(directory, 'logo.PNG')), bytes)
  assert.deepEqual(await fs.readFile(path.join(directory, 'archive.pdf')), bytes)
})

test('main/test Java 包同时移动，并只删除旧包的空父目录', async (t) => {
  const directory = await fixture(t, {
    'src/main/java/com/mdframe/forge/plugin/Hello.java': 'main',
    'src/test/java/com/mdframe/forge/plugin/HelloTest.java': 'test',
    'src/main/java/com/mdframe/other/Keep.java': 'keep',
  })
  await moveJavaPackageDirectories(directory, 'com.mdframe.forge', options.basePackage)
  assert.equal(await read(directory, 'src/main/java/com/acme/runtime/plugin/Hello.java'), 'main')
  assert.equal(await read(directory, 'src/test/java/com/acme/runtime/plugin/HelloTest.java'), 'test')
  assert.equal(await exists(path.join(directory, 'src/test/java/com/mdframe')), false)
  assert.equal(await read(directory, 'src/main/java/com/mdframe/other/Keep.java'), 'keep')
})

test('已存在目标包时按既有规则合并，目标其它文件仍保留', async (t) => {
  const directory = await fixture(t, {
    'src/main/java/com/mdframe/forge/sub/New.java': 'new',
    'src/main/java/com/acme/runtime/Keep.java': 'keep',
  })
  await moveJavaPackageDirectories(directory, 'com.mdframe.forge', options.basePackage)
  assert.equal(await read(directory, 'src/main/java/com/acme/runtime/sub/New.java'), 'new')
  assert.equal(await read(directory, 'src/main/java/com/acme/runtime/Keep.java'), 'keep')
  assert.equal(await exists(path.join(directory, 'src/main/java/com/mdframe/forge')), false)
})

test('同包名移动必须 no-op，不能合并后删除自己的 Java 源码', async (t) => {
  const file = 'src/main/java/com/mdframe/forge/Keep.java'
  const directory = await fixture(t, { [file]: 'keep' })
  await moveJavaPackageDirectories(directory, 'com.mdframe.forge', 'com.mdframe.forge')
  assert.equal(await read(directory, file), 'keep')
})

test('没有旧 Java 包的目录保持原样', async (t) => {
  const directory = await fixture(t, { 'plain.txt': 'keep' })
  await moveJavaPackageDirectories(directory, 'com.mdframe.forge', options.basePackage)
  assert.deepEqual((await collectFiles(directory, () => true)).map(file => path.basename(file)), ['plain.txt'])
})

test('类文件按 basename 改名，已有目标或不匹配扩展名时不覆盖', async (t) => {
  const directory = await fixture(t, {
    'a/ForgeFlowApplication.java': 'flow', 'b/ForgeFlowApplication.java': 'source',
    'b/AcmeFlowApplication.java': 'target', 'ForgeFlowApplication.txt': 'text',
  })
  await renameFilesByBasename(directory, classes, '.java')
  assert.equal(await read(directory, 'a/AcmeFlowApplication.java'), 'flow')
  assert.equal(await read(directory, 'b/ForgeFlowApplication.java'), 'source')
  assert.equal(await read(directory, 'b/AcmeFlowApplication.java'), 'target')
  assert.equal(await read(directory, 'ForgeFlowApplication.txt'), 'text')
})

test('模块目录从最深层改名，已有目标目录时按原规则跳过', async (t) => {
  const directory = await fixture(t, {
    'forge-framework/forge-starter-parent/keep.txt': 'deep',
    'forge-plugin-system/old.txt': 'old', 'core-plugin-system/keep.txt': 'keep',
  })
  await renameArtifactDirectories(directory, artifacts)
  assert.equal(await read(directory, 'core-framework/core-starter-parent/keep.txt'), 'deep')
  assert.equal(await read(directory, 'forge-plugin-system/old.txt'), 'old')
  assert.equal(await read(directory, 'core-plugin-system/keep.txt'), 'keep')
})

test('共享入口编排 groupId、文本、包目录、类文件与模块目录，保留不同 Maven/Java 命名空间', async (t) => {
  const directory = await fixture(t, {
    'forge-flow-server/pom.xml': '<groupId>com.mdframe.forge</groupId><artifactId>forge-server</artifactId>',
    'forge-flow-server/src/main/java/com/mdframe/forge/ForgeFlowApplication.java':
      'package com.mdframe.forge; public class ForgeFlowApplication {}',
    'forge-flow-server/src/main/resources/META-INF/spring/imports': 'com.mdframe.forge.plugin.Config',
  })
  const result = await renameSourceTree(directory, { options, catalog, selection })
  assert.equal(await read(directory, 'core-flow-server/pom.xml'),
    '<groupId>org.acme.maven</groupId><artifactId>foundation-server</artifactId>')
  assert.equal(await read(directory, 'core-flow-server/src/main/java/com/acme/runtime/AcmeFlowApplication.java'),
    'package com.acme.runtime; public class AcmeFlowApplication {}')
  assert.equal(await read(directory, 'core-flow-server/src/main/resources/META-INF/spring/imports'),
    'com.acme.runtime.plugin.Config')
  assert.deepEqual(result.replacements, replacements)
})

test('插件可扩展模块和类映射，共享入口仍使用同一张顺序化替换表', async (t) => {
  const directory = await fixture(t, {
    'forge-plugin-hello/src/main/java/com/mdframe/forge/PluginBootstrap.java':
      'package com.mdframe.forge; public class PluginBootstrap {}',
    'forge-plugin.json': '{"server":{"module":"forge-plugin-hello"}}',
  })
  await renameSourceTree(directory, { options, catalog, selection,
    artifactMap: { ...artifacts, 'forge-plugin-hello': 'core-plugin-hello' },
    applicationClassMap: { ...classes, PluginBootstrap: 'AcmePluginBootstrap' },
  })
  assert.equal(await read(directory, 'core-plugin-hello/src/main/java/com/acme/runtime/AcmePluginBootstrap.java'),
    'package com.acme.runtime; public class AcmePluginBootstrap {}')
  assert.equal(await read(directory, 'forge-plugin.json'), '{"server":{"module":"core-plugin-hello"}}')
})

test('替换表构建失败时不开始文件改写', async (t) => {
  const directory = await fixture(t, { 'pom.xml': '<groupId>com.mdframe.forge</groupId>' })
  await assert.rejects(renameSourceTree(directory, { options, catalog: {}, selection }), TypeError)
  assert.equal(await read(directory, 'pom.xml'), '<groupId>com.mdframe.forge</groupId>')
})

test('共享遍历支持空/不存在的目录，只收集符合条件的文件与目录', async (t) => {
  const directory = await fixture(t, { 'a/one.java': 'one', '.env': 'env', 'b/two.txt': 'two' })
  const files = await collectFiles(directory, file => file.endsWith('.java'))
  assert.deepEqual(files.map(file => path.relative(directory, file)), ['a/one.java'])
  const directories = await collectDirectories(directory, dir => path.basename(dir) === 'a')
  assert.deepEqual(directories, [path.join(directory, 'a')])
  assert.deepEqual(await collectFiles(path.join(directory, 'missing'), () => true), [])
  assert.deepEqual(await collectDirectories(path.join(directory, 'missing'), () => true), [])
})

test('蛇形名称保持既有连字符、大小写与符号转换规则', () => {
  assert.equal(toSnakeCase('AcmeApp'), 'acme_app')
  assert.equal(toSnakeCase('--Hello__API--'), 'hello_api')
  assert.equal(toSnakeCase('a.b/c'), 'a_b_c')
})
