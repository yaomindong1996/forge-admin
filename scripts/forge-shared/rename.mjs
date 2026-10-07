import fs from 'node:fs/promises'
import path from 'node:path'
import { collectFiles, collectDirectories, exists } from './files.mjs'

export const backendParentArtifacts = {
  root: 'forge-server',
  framework: 'forge-framework',
  dependencies: 'forge-dependencies',
  starterParent: 'forge-starter-parent',
  pluginParent: 'forge-plugin-parent',
  flowParent: 'forge-flow',
  businessParent: 'forge-business',
}

const binaryExtensions = new Set([
  '.png',
  '.jpg',
  '.jpeg',
  '.gif',
  '.webp',
  '.ico',
  '.woff',
  '.woff2',
  '.ttf',
  '.eot',
  '.xdb',
  '.jar',
  '.zip',
  '.gz',
  '.pdf',
])

export function buildArtifactMap(catalog, options) {
  const modulePrefix = options.moduleArtifactPrefix
  const map = {
    [backendParentArtifacts.framework]: `${modulePrefix}-framework`,
    [backendParentArtifacts.dependencies]: `${modulePrefix}-dependencies`,
    [backendParentArtifacts.starterParent]: `${modulePrefix}-starter-parent`,
    [backendParentArtifacts.pluginParent]: `${modulePrefix}-plugin-parent`,
    [backendParentArtifacts.flowParent]: `${modulePrefix}-flow`,
    [backendParentArtifacts.businessParent]: `${modulePrefix}-business`,
    'forge-admin': `${modulePrefix}-admin`,
    'forge-report': `${modulePrefix}-report`,
    'forge-starter-property': `${modulePrefix}-starter-property`,
  }

  for (const moduleInfo of Object.values(catalog.modules)) {
    if (!moduleInfo.artifactId) {
      continue
    }
    const next = moduleInfo.artifactId
      .replace(/^forge-/, `${modulePrefix}-`)
      .replace(/-parent$/, '-parent')
    map[moduleInfo.artifactId] = next
  }
  return map
}

export function buildApplicationClassMap(options) {
  const prefix = options.javaName
  return {
    ForgeAdminApplication: `${prefix}AdminApplication`,
    ForgeReportApplication: `${prefix}ReportApplication`,
    ForgeAppServerApplication: `${prefix}AppServerApplication`,
    ForgeFlowApplication: `${prefix}FlowApplication`,
  }
}

// —— 顺序化替换表：不能全局重排，后续规则可能继续使用前面规则的输出。 ——
export function buildTextReplacements(artifactMap, applicationClassMap, options, selection) {
  const snakeName = toSnakeCase(options.projectName)
  const h5Replacements = selection?.frontendIds?.has('h5-ui')
    ? [
        ['forge-h5-ui', `${options.projectName}-h5-ui`],
        ['/forge-h5', `/${options.projectName}-h5`],
        ['forge_h5', `${snakeName}_h5`],
      ]
    : []
  const replacements = [
    // Docker/H5 具体名称先替换，避免被 forge-admin 或 /forge 等通用前缀截断。
    ['docker-forge-admin', dockerDirName(options)],
    ...h5Replacements,
    ...projectTextReplacements(artifactMap, options),
    ...adminEnvironmentReplacements(options),
  ]
  if (selection?.frontendIds?.has('report-ui')) {
    replacements.push(...reportTextReplacements(artifactMap, options))
  }
  replacements.push(...moduleTextReplacements(artifactMap, applicationClassMap, options))
  return replacements
}

function projectTextReplacements(artifactMap, options) {
  const snakeName = toSnakeCase(options.projectName)
  const serverDirName = `${options.artifactPrefix}-server`
  const adminServerArtifactId = artifactMap['forge-admin-server']
  return [
    ['com.mdframe.forge', options.basePackage],
    ['com/mdframe/forge', options.basePackage.replaceAll('.', '/')],
    ['Forge AI', options.javaName],
    // 完整启动类留给后面的类映射；品牌前缀不能抢先把它改成 <prefix>Application。
    [/ForgeAdmin(?!Application)/, options.javaName],
    ['Forge Admin', options.displayName],
    ['Forge 工作台', `${options.javaName} 工作台`],
    ['企业级中后台基础框架', options.displayName],
    ['企业级中后台管理系统', options.adminTitle || options.displayName],
    ['forge-project', options.projectName],
    ['cd forge-server &&', `cd ${serverDirName} &&`],
    ['cd forge-server ', `cd ${serverDirName} `],
    ['cd forge-server\n', `cd ${serverDirName}\n`],
    ['cd forge &&', `cd ${serverDirName} &&`],
    ['cd forge ', `cd ${serverDirName} `],
    ['cd forge\n', `cd ${serverDirName}\n`],
    ['forge-server/                    # 后端根目录', `${serverDirName}/              # 后端根目录`],
    ['forge/                          # 后端根目录', `${serverDirName}/              # 后端根目录`],
    ['forge-server/                     # 后端根工程', `${serverDirName}/               # 后端根工程`],
    ['forge/                           # 后端根工程', `${serverDirName}/               # 后端根工程`],
    ['forge-server/\n', `${serverDirName}/\n`],
    ['forge/\n', `${serverDirName}/\n`],
    ['CREATE DATABASE forge ', `CREATE DATABASE ${options.databaseName} `],
    ['mysql -u root -p forge ', `mysql -u root -p ${options.databaseName} `],
    ['forge-admin-ui', `${options.projectName}-admin-ui`],
    ['forge-server/forge-admin/', `${serverDirName}/${adminServerArtifactId}/`],
    ['forge/forge-admin/', `${serverDirName}/${adminServerArtifactId}/`],
    ['forge-admin/', `${adminServerArtifactId}/`],
    ['forge-server/db', `${serverDirName}/db`],
    ['forge/db', `${serverDirName}/db`],
    ['forge-server/scripts', `${serverDirName}/scripts`],
    ['forge/scripts', `${serverDirName}/scripts`],
    ['forge-server/var', `${serverDirName}/var`],
    ['forge/var', `${serverDirName}/var`],
    ['forge_admin_new', options.databaseName],
    ['forge_admin', options.databaseName],
    ['forge_schema_history', `${snakeName}_schema_history`],
    ['vue-naive-admin', `${options.projectName}-admin-ui`],
    ['com.forge', options.basePackage],
  ]
}

function adminEnvironmentReplacements(options) {
  return [
    // 前端环境变量替换 - 基础配置
    ['VITE_TITLE=企业级中后台基础框架', `VITE_TITLE=${options.adminTitle || options.displayName}`],
    ['VITE_TITLE=Forge Admin', `VITE_TITLE=${options.adminTitle || options.displayName}`],
    ['VITE_HTTP_PORT=3000', `VITE_HTTP_PORT=${options.adminPort || '5173'}`],
    ['VITE_HTTP_PORT=5173', `VITE_HTTP_PORT=${options.adminPort || '5173'}`],
    ['VITE_HTTP_PORT=5174', `VITE_HTTP_PORT=${options.adminPort || '5174'}`],
    // 路径替换
    ['VITE_PUBLIC_PATH=/forge\n', `VITE_PUBLIC_PATH=${options.adminPublicPath || '/'}\n`],
    ['VITE_BASE_URL=/forge\n', `VITE_BASE_URL=${options.adminBaseUrl || '/'}\n`],
    ['VITE_PUBLIC_PATH=/forge', `VITE_PUBLIC_PATH=${options.adminPublicPath || '/'}`],
    ['VITE_BASE_URL=/forge', `VITE_BASE_URL=${options.adminBaseUrl || '/'}`],
    // API 前缀替换
    ['VITE_REQUEST_PREFIX=/forge-api', `VITE_REQUEST_PREFIX=${options.adminApiPrefix || '/api'}`],
    ['VITE_REQUEST_PREFIX=/dev-api', `VITE_REQUEST_PREFIX=${options.adminApiPrefix || '/api'}`],
    ['VITE_REQUEST_PREFIX=/api', `VITE_REQUEST_PREFIX=${options.adminApiPrefix || '/api'}`],
    // 代理地址替换
    ['VITE_HTTP_PROXY_TARGET=http://localhost:8580',
      `VITE_HTTP_PROXY_TARGET=${options.adminProxyTarget || 'http://localhost:8580'}`],
    ['VITE_HTTP_PROXY_TARGET=http://127.0.0.1:8580/',
      `VITE_HTTP_PROXY_TARGET=${options.adminProxyTarget || 'http://localhost:8580'}/`],
    ['VITE_HTTP_PROXY_TARGET=http://127.0.0.1:8580',
      `VITE_HTTP_PROXY_TARGET=${options.adminProxyTarget || 'http://localhost:8580'}`],
  ]
}

function reportTextReplacements(artifactMap, options) {
  const snakeName = toSnakeCase(options.projectName)
  const serverDirName = `${options.artifactPrefix}-server`
  const reportServerArtifactId = artifactMap['forge-report-server']
  const reportPath = `/${options.projectName}-report`
  return [
    ['forge-report-ui', `${options.projectName}-report-ui`],
    ['forge-server/forge-report/', `${serverDirName}/${reportServerArtifactId}/`],
    ['forge/forge-report/', `${serverDirName}/${reportServerArtifactId}/`],
    ['forge-report/', `${reportServerArtifactId}/`],
    ['forge_report', `${snakeName}_report`],
    ['forge_pc_001', `${snakeName}_pc_001`],
    ['/forge-report', reportPath],
    ['VITE_SSO_TARGET_CLIENT=forge_report', `VITE_SSO_TARGET_CLIENT=${snakeName}_report`],
    ['VITE_SSO_TARGET_CLIENT=forge_website_report', `VITE_SSO_TARGET_CLIENT=${snakeName}_website_report`],
    ['"forge_report":', `"${snakeName}_report":`],
    ['"forge_website_report":', `"${snakeName}_website_report":`],
    ['VITE_REPORT_UI_PATH_PREFIX=/forge-report', `VITE_REPORT_UI_PATH_PREFIX=/${options.projectName}-report`],
    ['VITE_SSO_BRIDGE_ROUTE=/report/design', `VITE_SSO_BRIDGE_ROUTE=/${options.projectName}-report/design`],
    [`http://www.dlforgelab.com:8084${reportPath}`, reportPath],
    ['VITE_REPORT_UI_HOST_FALLBACK=www.dlforgelab.com:8084', 'VITE_REPORT_UI_HOST_FALLBACK='],
    ['http://81.70.22.48:8084/forge-report', `http://localhost:8084/${options.projectName}-report`],
    ['http://localhost:3021/forge-report', `http://localhost:8084/${options.projectName}-report`],
    ['localhost:3021', `localhost:8084`],
  ]
}

function moduleTextReplacements(artifactMap, applicationClassMap, options) {
  const serverDirName = `${options.artifactPrefix}-server`
  const replacements = []
  for (const [from, to] of Object.entries(applicationClassMap)) {
    replacements.push([from, to])
  }

  for (const [from, to] of Object.entries(artifactMap).sort((a, b) => b[0].length - a[0].length)) {
    replacements.push([`forge-server/${from}`, `${serverDirName}/${to}`])
    replacements.push([`forge/${from}`, `${serverDirName}/${to}`])
  }

  for (const [from, to] of Object.entries(artifactMap).sort((a, b) => b[0].length - a[0].length)) {
    replacements.push([from, to])
  }

  for (const targetName of Object.values(artifactMap).sort((a, b) => b.length - a.length)) {
    replacements.push([`forge-server/${targetName}`, `${serverDirName}/${targetName}`])
    replacements.push([`forge/${targetName}`, `${serverDirName}/${targetName}`])
  }

  return replacements
}

export async function rewritePomGroupIds(serverRoot, groupId) {
  const pomFiles = await collectFiles(serverRoot, filePath => path.basename(filePath) === 'pom.xml')
  for (const pomFile of pomFiles) {
    const content = await fs.readFile(pomFile, 'utf8')
    const nextContent = content.split('com.mdframe.forge').join(groupId)
    if (nextContent !== content) {
      await fs.writeFile(pomFile, nextContent)
    }
  }
}

export async function rewriteRootPomArtifact(serverRoot, rootArtifactId) {
  const pomFiles = await collectFiles(serverRoot, filePath => path.basename(filePath) === 'pom.xml')
  for (const pomFile of pomFiles) {
    const content = await fs.readFile(pomFile, 'utf8')
    const nextContent = content
      .split('<artifactId>forge-server</artifactId>').join(`<artifactId>${rootArtifactId}</artifactId>`)
      .split('<artifactId>forge</artifactId>').join(`<artifactId>${rootArtifactId}</artifactId>`)
      .split('<name>forge-server</name>').join(`<name>${rootArtifactId}</name>`)
      .split('<name>forge</name>').join(`<name>${rootArtifactId}</name>`)
      .split('<description>forge-server</description>').join(`<description>${rootArtifactId}</description>`)
      .split('<description>forge</description>').join(`<description>${rootArtifactId}</description>`)
    if (nextContent !== content) {
      await fs.writeFile(pomFile, nextContent)
    }
  }
}

export function applyTextReplacements(content, replacements) {
  let nextContent = content
  for (const [from, to] of replacements) {
    nextContent = nextContent.split(from).join(to)
  }
  return nextContent
}

export async function rewriteTextFiles(rootDir, replacements, hasReportUi) {
  const files = await collectFiles(rootDir, filePath => !isBinaryFile(filePath))
  for (const file of files) {
    let content
    try {
      content = await fs.readFile(file, 'utf8')
    }
    catch {
      continue
    }
    let nextContent = applyTextReplacements(content, replacements)

    // 如果没有报表模块，删除 SSO 相关的配置行
    if (!hasReportUi) {
      nextContent = removeSsoConfigLines(nextContent)
    }

    if (nextContent !== content) {
      await fs.writeFile(file, nextContent)
    }
  }
}

function removeSsoConfigLines(content) {
  const lines = content.split('\n')
  const filteredLines = lines.filter(line => {
    const trimmed = line.trim()
    // 跳过 SSO 相关的配置行（包括注释掉的）
    if (trimmed.startsWith('#') && (
      trimmed.includes('VITE_SSO_') ||
      trimmed.includes('VITE_REPORT_UI_')
    )) {
      return false
    }
    if (trimmed.startsWith('VITE_SSO_') ||
        trimmed.startsWith('VITE_REPORT_UI_')) {
      return false
    }
    return true
  })
  return filteredLines.join('\n')
}

function isBinaryFile(filePath) {
  return binaryExtensions.has(path.extname(filePath).toLowerCase())
}

export async function moveJavaPackageDirectories(rootDir, oldPackage, newPackage) {
  // 源目标相同不能走合并后删除源目录，否则一次原包名生成会把 Java 源码清空。
  if (oldPackage === newPackage) {
    return
  }
  const oldPackagePath = oldPackage.replaceAll('.', path.sep)
  const newPackagePath = newPackage.replaceAll('.', path.sep)
  const javaRoots = await collectDirectories(rootDir, dirPath => /src[/\\](main|test)[/\\]java$/.test(dirPath))

  for (const javaRoot of javaRoots) {
    const oldDir = path.join(javaRoot, oldPackagePath)
    if (!(await exists(oldDir))) {
      continue
    }
    const newDir = path.join(javaRoot, newPackagePath)
    await fs.mkdir(path.dirname(newDir), { recursive: true })
    if (await exists(newDir)) {
      await mergeDirectory(oldDir, newDir)
      await fs.rm(oldDir, { recursive: true, force: true })
    }
    else {
      await fs.rename(oldDir, newDir)
    }
    await removeEmptyParents(path.dirname(oldDir), javaRoot)
  }
}

async function mergeDirectory(source, target) {
  await fs.mkdir(target, { recursive: true })
  const entries = await fs.readdir(source, { withFileTypes: true })
  for (const entry of entries) {
    const sourcePath = path.join(source, entry.name)
    const targetPath = path.join(target, entry.name)
    if (entry.isDirectory()) {
      await mergeDirectory(sourcePath, targetPath)
    }
    else {
      await fs.rename(sourcePath, targetPath)
    }
  }
}

async function removeEmptyParents(startDir, stopDir) {
  let current = startDir
  while (current.startsWith(stopDir) && current !== stopDir) {
    try {
      await fs.rmdir(current)
    }
    catch {
      break
    }
    current = path.dirname(current)
  }
}

export async function renameFilesByBasename(rootDir, basenameMap, extension = '') {
  const files = await collectFiles(rootDir, filePath => Object.hasOwn(basenameMap, path.basename(filePath, extension)))
  for (const file of files) {
    const basename = path.basename(file, extension)
    const nextBasename = basenameMap[basename]
    if (!nextBasename || nextBasename === basename) {
      continue
    }
    const target = path.join(path.dirname(file), `${nextBasename}${extension}`)
    if (await exists(target)) {
      continue
    }
    await fs.rename(file, target)
  }
}

export async function renameArtifactDirectories(serverRoot, artifactMap) {
  const dirs = await collectDirectories(serverRoot, () => true)
  dirs.sort((a, b) => b.length - a.length)
  for (const dir of dirs) {
    const baseName = path.basename(dir)
    const nextBaseName = artifactMap[baseName]
    if (!nextBaseName || nextBaseName === baseName) {
      continue
    }
    const target = path.join(path.dirname(dir), nextBaseName)
    if (await exists(target)) {
      continue
    }
    await fs.rename(dir, target)
  }
}

export function dockerDirName(options) {
  return `docker-${options.projectName}`
}

export function toSnakeCase(value) {
  return String(value)
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/-/g, '_')
    .replace(/[^a-zA-Z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
    .toLowerCase()
}

/**
 * 同一改名规则用于工程/插件的隔离源码副本；校验和复制由调用者负责，不在宿主工作区直接运行。
 * context.options 沿用生成器规范化选项；插件可扩展模块/启动类映射，但不能改变替换阶段顺序。
 */
export async function renameSourceTree(directory, context) {
  const { options, catalog, selection } = context
  const artifactMap = context.artifactMap ?? buildArtifactMap(catalog, options)
  const applicationClassMap = context.applicationClassMap ?? buildApplicationClassMap(options)
  const replacements = buildTextReplacements(artifactMap, applicationClassMap, options, selection)
  // 先把 POM 替成 groupId，才替换 Java 包名；二者不同时不能让后一步覆盖 Maven 坐标。
  await rewritePomGroupIds(directory, options.groupId)
  await rewriteRootPomArtifact(directory, `${options.artifactPrefix}-server`)
  await rewriteTextFiles(directory, replacements, selection?.frontendIds?.has('report-ui'))
  await moveJavaPackageDirectories(directory, 'com.mdframe.forge', options.basePackage)
  await renameFilesByBasename(directory, applicationClassMap, '.java')
  await renameArtifactDirectories(directory, artifactMap)
  return { artifactMap, applicationClassMap, replacements }
}
