#!/usr/bin/env node

import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import {
  backendParentArtifacts, buildArtifactMap, buildApplicationClassMap, buildTextReplacements,
  rewritePomGroupIds, rewriteRootPomArtifact, rewriteTextFiles, applyTextReplacements,
  moveJavaPackageDirectories, renameFilesByBasename, renameArtifactDirectories, dockerDirName, toSnakeCase,
} from '../forge-shared/rename.mjs'
import { collectFiles, exists } from '../forge-shared/files.mjs'
import { pruneOptionalAdminGlue } from './source-glue.mjs'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '../..')
const catalogPath = path.join(__dirname, 'module-catalog.json')

const rootModuleArtifacts = [
  'forge-admin-server',
  'forge-framework',
  'forge-report-server',
  'forge-app-server',
  'forge-flow',
  'forge-business',
]

const flowModuleArtifacts = [
  'forge-flow-client',
  'forge-flow-server',
]

const businessModuleArtifacts = [
  'forge-business-core',
]

const ignoredNames = new Set([
  '.git',
  '.idea',
  '.vscode',
  '.DS_Store',
  '.vite',
  'node_modules',
  'dist',
  'deploy',
  'target',
  'logs',
  '.pnpm-store',
  '__pycache__',
])

const ignoredFileNames = new Set([
  'application-dev.yml',
  '.env.local',
  '.flattened-pom.xml',
])

// 模板自身历史（变更记录含已归档、已并入全量 SQL 的旧迁移归档、社区导出产物）和本机 docker 密钥文件不带入新项目
const ignoredTemplatePathPrefixes = [
  'code-copilot/changes',
  'forge-server/db/backup',
  'forge-server/db/community-export',
  'docker-forge-admin/.env',
]

const projectContextNames = [
  'AGENTS.md',
  '.agents',
  'code-copilot',
]

main().catch((error) => {
  console.error(`\n[forge:create] ${error.message}`)
  process.exit(1)
})

async function main() {
  const catalog = await readJson(catalogPath)
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp(catalog)
    return
  }

  const options = normalizeOptions(args, catalog)
  const selection = resolveSelection(catalog, options.preset, options.includeModuleIds)
  const artifactMap = buildArtifactMap(catalog, options)
  const applicationClassMap = buildApplicationClassMap(options)
  const outputRoot = path.resolve(process.cwd(), options.target)
  const adminServerArtifactId = artifactMap['forge-admin-server']

  await assertWritableTarget(outputRoot, options.force)
  await fs.mkdir(outputRoot, { recursive: true })

  const serverDirName = `${options.artifactPrefix}-server`
  const serverRoot = path.join(outputRoot, serverDirName)
  const selectedArtifacts = collectSelectedArtifacts(catalog, selection)

  await copyBackend(serverRoot)
  await pruneBackend(serverRoot, catalog, selection)
  await pruneBackendSourceGlue(serverRoot, selectedArtifacts)
  await patchBackendPoms(serverRoot, catalog, selection, selectedArtifacts)
  await rewritePomGroupIds(serverRoot, options.groupId)
  await rewriteRootPomArtifact(serverRoot, `${options.artifactPrefix}-server`)

  if (selection.frontendIds.has('admin-ui')) {
    await copyProjectDir(path.join(repoRoot, 'forge-admin-ui'), path.join(outputRoot, `${options.projectName}-admin-ui`))
  }
  if (selection.frontendIds.has('report-ui')) {
    await copyProjectDir(path.join(repoRoot, 'forge-report-ui'), path.join(outputRoot, `${options.projectName}-report-ui`))
  }
  if (selection.frontendIds.has('h5-ui')) {
    await copyProjectDir(path.join(repoRoot, 'forge-h5-ui'), path.join(outputRoot, `${options.projectName}-h5-ui`))
  }
  if (selection.deployIds.has('docker')) {
    await copyProjectDir(path.join(repoRoot, 'docker-forge-admin'), path.join(outputRoot, dockerDirName(options)))
  }

  await copyOptionalRootFiles(outputRoot)
  await copyProjectContextFiles(outputRoot)
  await writeGeneratedConfig(outputRoot, options, selection, catalog)

  const replacements = buildTextReplacements(artifactMap, applicationClassMap, options, selection)
  const hasReportUi = selection?.frontendIds?.has('report-ui')
  await rewriteTextFiles(outputRoot, replacements, hasReportUi)
  if (selection.deployIds.has('docker')) {
    await rewriteDockerDeploy(path.join(outputRoot, dockerDirName(options)), options)
  }
  await moveJavaPackageDirectories(outputRoot, 'com.mdframe.forge', options.basePackage)
  await renameFilesByBasename(outputRoot, applicationClassMap, '.java')
  await renameArtifactDirectories(serverRoot, artifactMap)
  await writeSelectedSqlBundle(serverRoot, catalog, selection, replacements, options)
  if (options.excludeLogData) {
    await stripLogSeedData(path.join(serverRoot, 'db/全量初始化SQL.sql'), options.javaName)
  }
  if (selection.deployIds.has('docker')) {
    // docker 首次建库必须与 db/全量初始化SQL.sql 同源，避免两份 SQL 漂移
    await fs.copyFile(
      path.join(serverRoot, 'db/全量初始化SQL.sql'),
      path.join(outputRoot, dockerDirName(options), 'init-sql/01-init.sql'),
    )
  }

  printSummary(outputRoot, options, selection, catalog, adminServerArtifactId)
}

function parseArgs(argv) {
  const args = {
    _: [],
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--') {
      continue
    }
    if (arg === '--help' || arg === '-h') {
      args.help = true
      continue
    }
    if (!arg.startsWith('--')) {
      args._.push(arg)
      continue
    }
    const eqIndex = arg.indexOf('=')
    if (eqIndex > -1) {
      args[arg.slice(2, eqIndex)] = arg.slice(eqIndex + 1)
      continue
    }
    const key = arg.slice(2)
    const next = argv[index + 1]
    if (!next || next.startsWith('--')) {
      args[key] = true
      continue
    }
    args[key] = next
    index += 1
  }
  return args
}

function normalizeOptions(args, catalog) {
  const target = args._[0]
  if (!target) {
    throw new Error('缺少目标目录。示例：pnpm forge:create -- ../smart-factory --base-package com.company.smartfactory')
  }

  const projectName = normalizeProjectName(args['project-name'] || path.basename(path.resolve(target)))
  const artifactPrefix = normalizeProjectName(args['artifact-prefix'] || projectName)
  const stripModulePrefix = args['strip-module-prefix']
    ? normalizeProjectName(args['strip-module-prefix'])
    : ''
  const moduleArtifactPrefix = normalizeProjectName(
    args['module-artifact-prefix'] || deriveModuleArtifactPrefix(artifactPrefix, stripModulePrefix),
  )
  const javaName = String(args['java-name'] || toPascalCase(projectName)).trim()
  const displayName = String(args['display-name'] || projectName)
  const preset = String(args.preset || 'ai-report')
  const basePackage = String(args['base-package'] || '').trim()
  const groupId = String(args['group-id'] || basePackage).trim()
  const databaseName = String(args['database-name'] || toSnakeCase(projectName)).trim()
  const includeModuleIds = parseModuleList(args.include || args['include-modules'])
  const excludeLogData = args['exclude-log-data'] === true || args['exclude-log-data'] === 'true'
  
  // 前端配置参数
  const adminTitle = String(args['admin-title'] || displayName)
  const adminApiPrefix = String(args['admin-api-prefix'] || '/api')
  const adminPort = String(args['admin-port'] || '5173')
  const adminProxyTarget = String(args['admin-proxy-target'] || 'http://localhost:8580')
  const adminPublicPath = String(args['admin-public-path'] || '/')
  const adminBaseUrl = String(args['admin-base-url'] || '/')

  if (!catalog.presets[preset]) {
    throw new Error(`未知 preset：${preset}。可选值：${Object.keys(catalog.presets).join(', ')}`)
  }
  for (const moduleId of includeModuleIds) {
    if (!catalog.modules[moduleId]) {
      throw new Error(`未知 include 模块：${moduleId}。可选模块见 scripts/forge-create/module-catalog.json`)
    }
  }
  if (!isValidJavaPackage(basePackage)) {
    throw new Error('请通过 --base-package 指定合法 Java 包名，例如 com.company.smartfactory')
  }
  if (!isValidJavaPackage(groupId)) {
    throw new Error('请通过 --group-id 指定合法 Maven groupId，默认等于 --base-package')
  }
  if (!/^[A-Z][A-Za-z0-9]*$/.test(javaName)) {
    throw new Error('--java-name 必须是合法的大驼峰 Java 名称，例如 LawHub')
  }
  if (!/^[a-z][a-z0-9_]*$/.test(databaseName)) {
    throw new Error('数据库名只能使用小写字母、数字和下划线，并且必须以字母开头')
  }

  return {
    target,
    projectName,
    artifactPrefix,
    displayName,
    preset,
    basePackage,
    groupId,
    databaseName,
    includeModuleIds,
    excludeLogData,
    moduleArtifactPrefix,
    javaName,
    stripModulePrefix,
    force: args.force === true || args.force === 'true',
    // 前端配置
    adminTitle,
    adminApiPrefix,
    adminPort,
    adminProxyTarget,
    adminPublicPath,
    adminBaseUrl,
  }
}

function deriveModuleArtifactPrefix(artifactPrefix, stripModulePrefix) {
  if (!stripModulePrefix) {
    return artifactPrefix
  }
  if (artifactPrefix === stripModulePrefix) {
    throw new Error('--strip-module-prefix 不能等于完整 artifact 前缀，否则子模块前缀为空')
  }
  if (!artifactPrefix.startsWith(`${stripModulePrefix}-`)) {
    throw new Error(`--strip-module-prefix 必须是 artifact 前缀的开头：${artifactPrefix}`)
  }
  return artifactPrefix.slice(stripModulePrefix.length + 1)
}

function parseModuleList(value) {
  if (!value) {
    return []
  }
  return String(value)
    .split(/[,\s]+/)
    .map(item => item.trim())
    .filter(Boolean)
}

function normalizeProjectName(value) {
  const normalized = String(value || '')
    .trim()
    .replace(/_/g, '-')
    .toLowerCase()
  if (!/^[a-z][a-z0-9-]*$/.test(normalized)) {
    throw new Error(`项目英文名不合法：${value}。只允许小写字母、数字、中横线，并且必须以字母开头`)
  }
  return normalized
}

function isValidJavaPackage(value) {
  return /^[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)+$/.test(value)
}

function resolveSelection(catalog, presetName, includeModuleIds = []) {
  const preset = catalog.presets[presetName]
  const selectedModuleIds = new Set()
  const frontendIds = new Set()
  const deployIds = new Set()

  // 前端和部署目录不进 Maven 模块集合，但要带上它们依赖的后端服务（如 H5 依赖 app-server）
  const visit = (moduleId) => {
    const moduleInfo = catalog.modules[moduleId]
    if (!moduleInfo) {
      throw new Error(`模块清单缺少定义：${moduleId}`)
    }
    const bucket = moduleInfo.type === 'frontend'
      ? frontendIds
      : moduleInfo.type === 'deploy' ? deployIds : selectedModuleIds
    if (bucket.has(moduleId)) {
      return
    }
    bucket.add(moduleId)
    for (const dependency of moduleInfo.dependencies || []) {
      visit(dependency)
    }
  }

  for (const root of preset.roots) {
    visit(root)
  }
  for (const moduleId of includeModuleIds) {
    visit(moduleId)
  }

  return {
    presetName,
    selectedModuleIds,
    frontendIds,
    deployIds,
  }
}

function collectSelectedArtifacts(catalog, selection) {
  const artifacts = new Set(Object.values(backendParentArtifacts))
  for (const moduleId of selection.selectedModuleIds) {
    const artifactId = catalog.modules[moduleId]?.artifactId
    if (artifactId) {
      artifacts.add(artifactId)
    }
  }
  if (![...selection.selectedModuleIds].some(id => catalog.modules[id]?.type === 'flow')) {
    artifacts.delete(backendParentArtifacts.flowParent)
  }
  if (![...selection.selectedModuleIds].some(id => catalog.modules[id]?.type === 'business')) {
    artifacts.delete(backendParentArtifacts.businessParent)
  }
  return artifacts
}

/**
 * 将字符串转换为 PascalCase（大驼峰）
 * 支持：kebab-case、snake_case、space 分隔、混合大小写
 * 示例：my-app → MyApp, my_app → MyApp, my app → MyApp, MyApp → MyApp
 */
function toPascalCase(str) {
  if (!str) return '';

  return str
  // 1. 把所有分隔符（- _ 空格）替换成空格，统一处理
  .replace(/[-_\s]+/g, ' ')
  // 2. 每个单词首字母大写，其余小写
  .replace(/\b\w+/g, (word) => {
    return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
  })
  // 3. 去掉所有空格
  .replace(/ /g, '');
}

async function copyBackend(serverRoot) {
  await copyProjectDir(path.join(repoRoot, 'forge-server'), serverRoot)
}

async function copyProjectDir(source, target) {
  await fs.cp(source, target, {
    recursive: true,
    filter: sourcePath => shouldCopyPath(sourcePath),
  })
}

function shouldCopyPath(sourcePath) {
  const baseName = path.basename(sourcePath)
  if (ignoredNames.has(baseName) || ignoredFileNames.has(baseName)) {
    return false
  }
  return !isIgnoredTemplatePath(sourcePath)
}

function isIgnoredTemplatePath(sourcePath) {
  const relativePath = path.relative(repoRoot, sourcePath).split(path.sep).join('/')
  return ignoredTemplatePathPrefixes.some(prefix => relativePath === prefix || relativePath.startsWith(`${prefix}/`))
}

async function pruneBackend(serverRoot, catalog, selection) {
  const selectedArtifacts = collectSelectedArtifacts(catalog, selection)

  await pruneModuleDirectories(serverRoot, rootModuleArtifacts, selectedArtifacts)
  await pruneModuleDirectories(
    path.join(serverRoot, 'forge-framework/forge-plugin-parent'),
    artifactsByType(catalog, 'plugin'),
    selectedArtifacts,
  )
  await pruneModuleDirectories(
    path.join(serverRoot, 'forge-framework/forge-starter-parent'),
    artifactsByType(catalog, 'starter'),
    selectedArtifacts,
  )
  await pruneModuleDirectories(path.join(serverRoot, 'forge-flow'), flowModuleArtifacts, selectedArtifacts)
  await pruneModuleDirectories(path.join(serverRoot, 'forge-business'), businessModuleArtifacts, selectedArtifacts)
}

async function pruneBackendSourceGlue(serverRoot, selectedArtifacts) {
  const adminServerRoot = path.join(serverRoot, 'forge-admin-server')
  await pruneOptionalAdminGlue(adminServerRoot, selectedArtifacts)
  if (!selectedArtifacts.has('forge-plugin-generator')) {
    await fs.rm(path.join(adminServerRoot, 'src/main/java/com/mdframe/forge/admin/bridge'), {
      recursive: true,
      force: true,
    })
  }
  else if (!selectedArtifacts.has('forge-plugin-ai')) {
    await writeFallbackAiClientAdapter(adminServerRoot)
  }
  if (!selectedArtifacts.has('forge-plugin-ai')) {
    await fs.rm(path.join(adminServerRoot, 'src/main/java/com/mdframe/forge/admin/ai'), {
      recursive: true,
      force: true,
    })
  }
}

async function writeFallbackAiClientAdapter(adminServerRoot) {
  const adapterFile = path.join(adminServerRoot, 'src/main/java/com/mdframe/forge/admin/bridge/AiClientAdapterImpl.java')
  const content = `package com.mdframe.forge.admin.bridge;

import com.mdframe.forge.plugin.generator.service.AiClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Fallback AI adapter for generated projects that keep generator but exclude plugin-ai.
 */
@Component
public class AiClientAdapterImpl implements AiClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(AiClientAdapterImpl.class);
    private static final String FALLBACK_REASON = "plugin-ai is not included in this generated project";

    @Override
    public AiClientResult call(String agentCode, String message, Map<String, String> contextVars) {
        return fallback(agentCode);
    }

    @Override
    public AiClientResult call(String agentCode, String message, Map<String, String> contextVars,
                               Integer timeoutSeconds) {
        return fallback(agentCode);
    }

    @Override
    public Flux<String> stream(String userInput, String agentCode, String message, Map<String, String> contextVars) {
        return emptyStream(agentCode);
    }

    @Override
    public Flux<String> stream(String userInput, String sessionId, String agentCode, String message,
                               Map<String, String> contextVars) {
        return emptyStream(agentCode);
    }

    @Override
    public Flux<String> stream(String userInput, String sessionId, String agentCode, String message,
                               Map<String, String> contextVars, Long providerId, Long modelId,
                               Double temperature, Integer maxTokens) {
        return emptyStream(agentCode);
    }

    @Override
    public String loadContextSpec(String agentCode) {
        return "";
    }

    private AiClientResult fallback(String agentCode) {
        log.warn("[AiClientAdapter] Skip AI call because plugin-ai is not included: agentCode={}", agentCode);
        return AiClientResult.fallback(FALLBACK_REASON);
    }

    private Flux<String> emptyStream(String agentCode) {
        log.warn("[AiClientAdapter] Skip AI stream because plugin-ai is not included: agentCode={}", agentCode);
        return Flux.empty();
    }
}
`
  await fs.writeFile(adapterFile, content)
}

function artifactsByType(catalog, type) {
  return Object.values(catalog.modules)
    .filter(moduleInfo => moduleInfo.type === type)
    .map(moduleInfo => moduleInfo.artifactId)
}

async function pruneModuleDirectories(parentDir, artifactIds, selectedArtifacts) {
  if (!(await exists(parentDir))) {
    return
  }
  for (const artifactId of artifactIds) {
    if (!selectedArtifacts.has(artifactId)) {
      await fs.rm(path.join(parentDir, artifactId), { recursive: true, force: true })
    }
  }
}

async function patchBackendPoms(serverRoot, catalog, selection, selectedArtifacts) {
  const selectedRootModules = rootModuleArtifacts.filter((artifactId) => {
    if (artifactId === 'forge-framework') {
      return true
    }
    return selectedArtifacts.has(artifactId)
  })
  await replacePomModules(path.join(serverRoot, 'pom.xml'), selectedRootModules)
  await replacePomModules(path.join(serverRoot, 'forge-framework/pom.xml'), [
    'forge-dependencies',
    'forge-starter-parent',
    'forge-plugin-parent',
  ])
  await replacePomModules(
    path.join(serverRoot, 'forge-framework/forge-plugin-parent/pom.xml'),
    artifactsByType(catalog, 'plugin').filter(artifactId => selectedArtifacts.has(artifactId)),
  )
  await replacePomModules(
    path.join(serverRoot, 'forge-framework/forge-starter-parent/pom.xml'),
    artifactsByType(catalog, 'starter').filter(artifactId => selectedArtifacts.has(artifactId)),
  )
  if (selectedArtifacts.has('forge-flow')) {
    await replacePomModules(
      path.join(serverRoot, 'forge-flow/pom.xml'),
      flowModuleArtifacts.filter(artifactId => selectedArtifacts.has(artifactId)),
    )
  }
  if (selectedArtifacts.has('forge-business')) {
    await replacePomModules(
      path.join(serverRoot, 'forge-business/pom.xml'),
      businessModuleArtifacts.filter(artifactId => selectedArtifacts.has(artifactId)),
    )
  }

  const pomFiles = await collectFiles(serverRoot, filePath => path.basename(filePath) === 'pom.xml')
  for (const pomFile of pomFiles) {
    await prunePomDependencies(pomFile, selectedArtifacts)
  }

  await patchBusinessCoreDependency(serverRoot, selectedArtifacts)
}

async function replacePomModules(pomFile, modules) {
  if (!(await exists(pomFile))) {
    return
  }
  const content = await fs.readFile(pomFile, 'utf8')
  const moduleContent = [
    '    <modules>',
    ...modules.map(moduleName => `        <module>${moduleName}</module>`),
    '    </modules>',
  ].join('\n')
  const nextContent = content.includes('<modules>')
    ? content.replace(/[\t ]*<modules>[\s\S]*?<\/modules>/, moduleContent)
    : content
  await fs.writeFile(pomFile, nextContent)
}

async function prunePomDependencies(pomFile, selectedArtifacts) {
  const content = await fs.readFile(pomFile, 'utf8')
  const nextContent = content.replace(/[\t ]*<dependency>[\s\S]*?<groupId>com\.mdframe\.forge<\/groupId>[\s\S]*?<artifactId>(forge[^<]+)<\/artifactId>[\s\S]*?<\/dependency>\s*/g, (block, artifactId) => {
    return selectedArtifacts.has(artifactId) ? block : ''
  })
  if (nextContent !== content) {
    await fs.writeFile(pomFile, nextContent)
  }
}

async function patchBusinessCoreDependency(serverRoot, selectedArtifacts) {
  if (!selectedArtifacts.has('forge-business-core') || !selectedArtifacts.has('forge-admin-server')) {
    return
  }
  await ensurePomDependency(
    path.join(serverRoot, 'forge-admin-server/pom.xml'),
    'com.mdframe.forge',
    'forge-business-core',
    '${revision}',
  )
}

async function ensurePomDependency(pomFile, groupId, artifactId, version) {
  if (!(await exists(pomFile))) {
    return
  }
  const content = await fs.readFile(pomFile, 'utf8')
  if (content.includes(`<artifactId>${artifactId}</artifactId>`)) {
    return
  }
  const dependencyLines = [
    '',
    '        <dependency>',
    `            <groupId>${groupId}</groupId>`,
    `            <artifactId>${artifactId}</artifactId>`,
  ]
  if (version) {
    dependencyLines.push(`            <version>${version}</version>`)
  }
  dependencyLines.push('        </dependency>')
  const dependency = `${dependencyLines.join('\n')}\n`
  const nextContent = content.replace(/\s*<\/dependencies>/, `${dependency}    </dependencies>`)
  if (nextContent !== content) {
    await fs.writeFile(pomFile, nextContent)
  }
}

// 模板 docker 写死了 /forge/ 公开路径和 /forge-api/ 前缀，生成后必须与管理端 .env.production 的替换结果一致
async function rewriteDockerDeploy(dockerRoot, options) {
  const base = `/${String(options.adminPublicPath || '/').replace(/^\/+|\/+$/g, '')}`.replace(/^\/$/, '')
  const publicPath = `${base}/`
  const apiPrefix = `/${String(options.adminApiPrefix || '/api').replace(/^\/+|\/+$/g, '')}`
  const serverDirName = `${options.artifactPrefix}-server`
  const common = [
    ['forge-mysql', `${options.projectName}-mysql`],
    ['forge-redis', `${options.projectName}-redis`],
    ['forge-network', `${options.projectName}-network`],
    ['forge-ui', `${options.projectName}-ui`],
  ]
  const perFile = {
    'nginx.conf': [
      ['/forge/assets/', `${publicPath}assets/`],
      ['location /forge/ {', `location ${publicPath} {`],
      ['/forge/index.html', `${publicPath}index.html`],
      ['/forge-api/', `${apiPrefix}/`],
    ],
    'Dockerfile.ui': [['/usr/share/nginx/html/forge', `/usr/share/nginx/html${base}`]],
    'Dockerfile.admin': [['COPY ./forge-server .', `COPY ./${serverDirName} .`]],
    'Dockerfile.flow': [['COPY ./forge-server .', `COPY ./${serverDirName} .`]],
    'docker-compose.yml': [['http://localhost/forge', `http://localhost${publicPath}`]],
  }
  for (const [fileName, fileReplacements] of Object.entries(perFile)) {
    const file = path.join(dockerRoot, fileName)
    if (!(await exists(file))) {
      continue
    }
    const content = await fs.readFile(file, 'utf8')
    const nextContent = applyTextReplacements(content, [...fileReplacements, ...common])
    if (nextContent !== content) {
      await fs.writeFile(file, nextContent)
    }
  }
}

async function copyOptionalRootFiles(outputRoot) {
  for (const fileName of ['LICENSE', '.gitignore']) {
    const source = path.join(repoRoot, fileName)
    if (await exists(source)) {
      await fs.copyFile(source, path.join(outputRoot, fileName))
    }
  }
}

async function stripLogSeedData(sqlFile, systemName) {
  if (!(await exists(sqlFile))) {
    throw new Error(`启用了 --exclude-log-data，但未找到全量初始化 SQL：${sqlFile}`)
  }

  const content = await fs.readFile(sqlFile, 'utf8')
  const statements = splitSqlStatements(content)
  const removed = new Map()
  const kept = []

  for (const statement of statements) {
    const table = extractInsertTable(statement)
    if (table && isLogOrRuntimeTable(table)) {
      removed.set(table, (removed.get(table) || 0) + 1)
      continue
    }
    kept.push(statement)
  }

  const summary = [...removed.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([table, count]) => `--   ${table}: ${count} INSERT statement(s)`)
    .join('\n')
  const header = [
    `-- ${systemName} initialization policy: log/runtime/history table DDL is retained,`,
    '-- while historical INSERT data is intentionally excluded.',
    summary,
  ].filter(Boolean).join('\n')

  await fs.writeFile(sqlFile, `${header}\n\n${kept.join('')}`)
  console.log(`[forge:create] 已从全量 SQL 移除 ${[...removed.values()].reduce((sum, count) => sum + count, 0)} 条日志/运行历史 INSERT`)
}

function splitSqlStatements(content) {
  const statements = []
  let start = 0
  let state = 'normal'

  for (let index = 0; index < content.length; index += 1) {
    const char = content[index]
    const next = content[index + 1]

    if (state === 'single') {
      if (char === '\\') {
        index += 1
      }
      else if (char === "'" && next === "'") {
        index += 1
      }
      else if (char === "'") {
        state = 'normal'
      }
      continue
    }
    if (state === 'double') {
      if (char === '\\') {
        index += 1
      }
      else if (char === '"' && next === '"') {
        index += 1
      }
      else if (char === '"') {
        state = 'normal'
      }
      continue
    }
    if (state === 'backtick') {
      if (char === '`' && next === '`') {
        index += 1
      }
      else if (char === '`') {
        state = 'normal'
      }
      continue
    }
    if (state === 'line-comment') {
      if (char === '\n') {
        state = 'normal'
      }
      continue
    }
    if (state === 'block-comment') {
      if (char === '*' && next === '/') {
        state = 'normal'
        index += 1
      }
      continue
    }

    if (char === "'") {
      state = 'single'
    }
    else if (char === '"') {
      state = 'double'
    }
    else if (char === '`') {
      state = 'backtick'
    }
    else if (char === '-' && next === '-') {
      state = 'line-comment'
      index += 1
    }
    else if (char === '#') {
      state = 'line-comment'
    }
    else if (char === '/' && next === '*') {
      state = 'block-comment'
      index += 1
    }
    else if (char === ';') {
      statements.push(content.slice(start, index + 1))
      start = index + 1
    }
  }

  if (start < content.length) {
    statements.push(content.slice(start))
  }
  return statements
}

function extractInsertTable(statement) {
  const match = statement.match(/^\s*(?:(?:--[^\n]*(?:\n|$))|(?:#[^\n]*(?:\n|$))|(?:\/\*[\s\S]*?\*\/\s*))*INSERT\s+INTO\s+`?([A-Za-z0-9_]+)`?/i)
  return match?.[1]?.toLowerCase() || ''
}

function isLogOrRuntimeTable(table) {
  return /(^|_)(log|logs)($|_)/.test(table)
    || table.endsWith('_history')
    || /^qrtz_(fired_triggers|scheduler_state|locks)$/.test(table)
    || table === 'worker_node'
    || table === 'sys_auth_online_user'
    || table === 'ai_crud_export_task'
    || /^ai_chat_(record|session)$/.test(table)
    || table === 'ai_dashboard_generate_record'
}

async function copyProjectContextFiles(outputRoot) {
  for (const name of projectContextNames) {
    const source = path.join(repoRoot, name)
    if (await exists(source)) {
      await copyProjectDir(source, path.join(outputRoot, name))
    }
  }
  const changesDir = path.join(outputRoot, 'code-copilot/changes')
  if (await exists(path.join(outputRoot, 'code-copilot'))) {
    await fs.mkdir(changesDir, { recursive: true })
    await fs.writeFile(path.join(changesDir, '.gitkeep'), '')
  }
}

async function writeGeneratedConfig(outputRoot, options, selection, catalog) {
  const config = {
    projectName: options.projectName,
    javaName: options.javaName,
    displayName: options.displayName,
    basePackage: options.basePackage,
    groupId: options.groupId,
    artifactPrefix: options.artifactPrefix,
    moduleArtifactPrefix: options.moduleArtifactPrefix,
    stripModulePrefix: options.stripModulePrefix,
    databaseName: options.databaseName,
    preset: options.preset,
    includedModules: options.includeModuleIds,
    excludeLogData: options.excludeLogData,
    modules: [...selection.selectedModuleIds].sort(),
    frontends: [...selection.frontendIds].sort(),
    deploy: [...selection.deployIds].sort(),
  }
  await fs.writeFile(
    path.join(outputRoot, 'forge.config.json'),
    `${JSON.stringify(config, null, 2)}\n`,
  )

  const presetDescription = catalog.presets[options.preset]?.description || options.preset
  const modulePrefixLine = options.moduleArtifactPrefix === options.artifactPrefix
    ? ''
    : `- 子模块 artifact 前缀: ${options.moduleArtifactPrefix}
`
  const adminServerArtifactId = `${options.moduleArtifactPrefix}-admin-server`
  const readme = `# ${options.displayName}

该工程由 Forge 项目装配器生成。

## 生成参数

- preset: ${options.preset} - ${presetDescription}
- Java 包名: ${options.basePackage}
- Maven groupId: ${options.groupId}
- artifact 前缀: ${options.artifactPrefix}
${modulePrefixLine}
- 数据库名: ${options.databaseName}
${options.includeModuleIds.length ? `- 额外模块: ${options.includeModuleIds.join(', ')}
` : ''}

## 常用命令

\`\`\`bash
cd ${options.artifactPrefix}-server
mvn -pl ${adminServerArtifactId} -am compile -DskipTests
\`\`\`

\`\`\`bash
cd ${options.projectName}-admin-ui
pnpm install
pnpm dev
\`\`\`

${selection.frontendIds.has('report-ui')
  ? `\`\`\`bash
cd ${options.projectName}-report-ui
pnpm install
pnpm dev
\`\`\`
`
  : ''}
${selection.frontendIds.has('h5-ui')
  ? `\`\`\`bash
cd ${options.projectName}-h5-ui
pnpm install
pnpm dev
\`\`\`
`
  : ''}
${selection.deployIds.has('docker')
  ? `\`\`\`bash
cd ${dockerDirName(options)}
cp .env.example .env
docker compose up -d
\`\`\`

Docker 首次启动用 \`init-sql/01-init.sql\` 建库，其中带有模板测试数据。等 admin 启动完成（Flyway 增量执行完）后清理一次，然后重启 admin：

\`\`\`bash
MYSQL_PWD=*** bash ${options.artifactPrefix}-server/scripts/db/clean-db.sh --host 127.0.0.1 --database ${options.databaseName} --execute
docker compose restart ${options.moduleArtifactPrefix}-admin
\`\`\`
`
  : ''}
## 说明

生成器按 preset 裁剪 Maven 子模块、starter、plugin、前端工程和模块 SQL 资源。管理端前端内部页面暂未做菜单级裁剪，后续应结合 seed 菜单数据和路由清单继续细化。
`
  await fs.writeFile(path.join(outputRoot, 'README.md'), readme)
}

async function writeSelectedSqlBundle(serverRoot, catalog, selection, replacements, options) {
  const entries = await collectSelectedSqlEntries(catalog, selection)
  const dbRoot = path.join(serverRoot, 'db')
  const moduleSqlRoot = path.join(dbRoot, 'module')
  await fs.mkdir(moduleSqlRoot, { recursive: true })

  const manifest = []
  let index = 1
  for (const entry of entries) {
    const sourceFile = path.join(repoRoot, entry.path)
    if (!(await exists(sourceFile))) {
      continue
    }
    const fileName = `${String(index).padStart(3, '0')}__${sanitizeFileName(entry.moduleId)}__${path.basename(entry.path)}`
    const targetFile = path.join(moduleSqlRoot, fileName)
    const content = applyTextReplacements(await fs.readFile(sourceFile, 'utf8'), replacements)
    await fs.writeFile(targetFile, content)
    manifest.push({
      order: index,
      module: entry.moduleId,
      source: entry.path,
      file: `module/${fileName}`,
    })
    index += 1
  }

  await fs.writeFile(
    path.join(dbRoot, 'manifest.json'),
    `${JSON.stringify({ scripts: manifest }, null, 2)}\n`,
  )
  await fs.writeFile(path.join(dbRoot, 'README.md'), buildDatabaseReadme(options))
}

async function collectSelectedSqlEntries(catalog, selection) {
  const entries = []
  const seen = new Set()
  const add = (moduleId, relativePath) => {
    if (seen.has(relativePath)) {
      return
    }
    seen.add(relativePath)
    entries.push({ moduleId, path: relativePath })
  }

  const requiredSeedRoot = path.join(repoRoot, 'forge-server/db/seed/required')
  const requiredSeeds = await collectFiles(requiredSeedRoot, filePath => path.extname(filePath) === '.sql')
  for (const filePath of requiredSeeds.sort()) {
    add('required-seed', path.relative(repoRoot, filePath))
  }

  for (const moduleId of selection.selectedModuleIds) {
    const moduleInfo = catalog.modules[moduleId]
    if (!moduleInfo?.path || moduleInfo.type === 'frontend') {
      continue
    }
    if (moduleId === 'admin-server') {
      continue
    }
    const moduleRoot = path.join(repoRoot, moduleInfo.path)
    const sqlFiles = await collectFiles(moduleRoot, (filePath) => {
      const normalized = filePath.split(path.sep).join('/')
      const baseName = path.basename(filePath)
      return path.extname(filePath) === '.sql'
        && !normalized.includes('/target/')
        && !normalized.includes('/templates/')
        && !baseName.includes('example')
        && !baseName.startsWith('test_')
    })
    for (const filePath of sqlFiles.sort()) {
      add(moduleId, path.relative(repoRoot, filePath))
    }
  }

  return entries
}

function sanitizeFileName(value) {
  return String(value).replace(/[^a-zA-Z0-9_-]/g, '_')
}

function buildDatabaseReadme(options) {
  return `# 数据库脚本

\`manifest.json\` 和 \`module/\` 按本次选择的模块收集 SQL 资源，数据库名已替换为 \`${options.databaseName}\`。

初始化干净库（全量 SQL → required seed → Flyway 增量 → 清理测试数据，需要 mysql 客户端和 Maven）：

\`\`\`bash
MYSQL_PWD=your_password bash scripts/db/init-db.sh --database ${options.databaseName} --recreate --clean
\`\`\`

只保留默认租户、超级管理员 admin、菜单权限、字典、系统参数、定时任务配置和内置模板；日志、流程、低代码应用及其自动建表、测试用户/租户/组织/角色都会清空。

没有 Maven 时：先 \`init-db.sh --database ${options.databaseName} --recreate\`，启动一次 admin 让 Flyway 执行增量，再执行 \`clean-db.sh --database ${options.databaseName}\` 预览、加 \`--execute\` 清理。

全量 SQL 只能导入空库；已有业务库不要重跑全量初始化，也不要执行 \`clean-db.sh\`（物理删除）。如需额外执行本目录按模块收集的 SQL，追加 \`--with-module\`。
`
}

async function assertWritableTarget(outputRoot, force) {
  if (!(await exists(outputRoot))) {
    return
  }
  const entries = await fs.readdir(outputRoot)
  if (entries.length === 0) {
    return
  }
  if (!force) {
    throw new Error(`目标目录非空：${outputRoot}。如需覆盖请加 --force`)
  }
  await fs.rm(outputRoot, { recursive: true, force: true })
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

function printHelp(catalog) {
  console.log(`Forge 项目装配器

用法：
  pnpm forge:create -- <target-dir> --base-package com.company.project [options]

参数：
  --preset            ${Object.keys(catalog.presets).join(' | ')}，默认 ai-report
  --project-name      项目英文名，默认取目标目录名
  --java-name         Java 类名前缀，默认由 project-name 转大驼峰，例如 LawHub
  --display-name      系统中文名，默认等于项目英文名
  --base-package      新 Java 包名，必填，例如 com.company.smartfactory
  --group-id          Maven groupId，默认等于 base-package
  --artifact-prefix   Maven artifactId 前缀，默认等于 project-name
  --module-artifact-prefix 子模块 artifactId 前缀，默认等于 artifact-prefix
  --strip-module-prefix 从子模块 artifactId 前缀中剥离指定前缀，例如 nmg-lt
  --database-name     数据库名，默认由 project-name 转 snake_case
  --include           额外模块 ID，逗号分隔，例如 business-core、h5-ui（移动端）、docker（部署编排）
  --exclude-log-data  保留日志/运行历史表结构，但从全量 SQL 移除其初始化数据
  --force             目标目录非空时覆盖

示例：
  pnpm forge:create -- ../smart-factory \\
    --preset ai-report \\
    --display-name 智慧工厂管理平台 \\
    --base-package com.company.smartfactory
`)
}

function printSummary(outputRoot, options, selection, catalog, adminServerArtifactId) {
  const modules = [...selection.selectedModuleIds].sort()
  const frontends = [...selection.frontendIds].sort()
  const deploys = [...selection.deployIds].sort()
  console.log('\n[forge:create] 生成完成')
  console.log(`目标目录：${outputRoot}`)
  console.log(`preset：${options.preset} - ${catalog.presets[options.preset].description}`)
  if (options.moduleArtifactPrefix !== options.artifactPrefix) {
    console.log(`子模块 artifact 前缀：${options.moduleArtifactPrefix}`)
  }
  console.log(`后端模块：${modules.join(', ')}`)
  console.log(`前端工程：${frontends.length ? frontends.join(', ') : '无'}`)
  console.log(`部署目录：${deploys.length ? deploys.map(() => dockerDirName(options)).join(', ') : '无'}`)
  console.log('\n建议验证：')
  console.log(`  cd ${path.join(outputRoot, `${options.artifactPrefix}-server`)}`)
  console.log(`  mvn -pl ${adminServerArtifactId} -am compile -DskipTests`)
  if (selection.frontendIds.has('admin-ui')) {
    console.log(`  cd ${path.join(outputRoot, `${options.projectName}-admin-ui`)}`)
    console.log('  pnpm install && pnpm build')
  }
  console.log('\n初始化干净数据库：')
  console.log(`  cd ${path.join(outputRoot, `${options.artifactPrefix}-server`)}`)
  console.log(`  MYSQL_PWD=*** bash scripts/db/init-db.sh --database ${options.databaseName} --recreate --clean`)
}
