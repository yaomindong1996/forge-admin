import fs from 'node:fs/promises'
import path from 'node:path'

const templateBlockBegin = '# forge-template-only:begin'
const templateBlockEnd = '# forge-template-only:end'
const ignoredToolNames = new Set(['.git', '.DS_Store', 'node_modules', 'fixtures', '__tests__', 'tests'])

// 配置版本必须来自本次模板，不能猜测版本或把 Maven 未解析变量带给安装器。
export async function readForgeVersion(repoRoot) {
  const pom = (await fs.readFile(path.join(repoRoot, 'forge-server/pom.xml'), 'utf8'))
    .replace(/<!--[\s\S]*?-->/g, '')
  const properties = pom.match(/<properties>([\s\S]*?)<\/properties>/)?.[1] || ''
  const revisions = [...properties.matchAll(/<revision>\s*([^<]*?)\s*<\/revision>/g)]
  if (revisions.length !== 1 || !isLiteralVersion(revisions[0][1])) {
    throw new Error('模板根 POM 必须包含唯一、合法的明文 revision 版本')
  }
  return revisions[0][1]
}

function isLiteralVersion(version) {
  const match = version.match(/^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+([0-9A-Za-z.-]+))?$/)
  if (!match || match.slice(1, 4).some(value => /^0\d/.test(value))) {
    return false
  }
  const prerelease = match[4]?.split('.') || []
  const build = match[5]?.split('.') || []
  return prerelease.every(value => value && !/^0\d+$/.test(value)) && build.every(Boolean)
}

// 逐行保留原始字节（含 CRLF），只删除精确标记区块；损坏标记不能静默丢掉客户规则。
export function stripTemplateOnlyGitignore(content) {
  const result = []
  let inside = false
  let seen = false
  for (const line of content.match(/[^\n]*(?:\n|$)/g) || []) {
    const marker = line.trim()
    if (marker === templateBlockBegin) {
      if (inside || seen) {
        throw new Error('.gitignore 模板专用区块重复或嵌套')
      }
      inside = true
      seen = true
    }
    else if (marker === templateBlockEnd) {
      if (!inside) {
        throw new Error('.gitignore 模板专用区块结束标记缺少开始标记')
      }
      inside = false
    }
    else if (!inside) {
      result.push(line)
    }
  }
  if (inside) {
    throw new Error('.gitignore 模板专用区块未闭合')
  }
  return result.join('')
}

export async function copyGeneratedGitignore(repoRoot, outputRoot) {
  const source = await fs.readFile(path.join(repoRoot, '.gitignore'), 'utf8')
  await fs.writeFile(path.join(outputRoot, '.gitignore'), stripTemplateOnlyGitignore(source))
}

export async function writeGeneratedProjectConfig({ outputRoot, options, selection, forgeVersion }) {
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
    forgeVersion,
    plugins: [],
  }
  await fs.writeFile(path.join(outputRoot, 'forge.config.json'), `${JSON.stringify(config, null, 2)}\n`)
}

// 必须在业务源码改名后调用：这些工具保存原始 Forge 匹配表，供后续插件源码再改名。
export async function copyGeneratedPluginTools({ repoRoot, outputRoot, projectName }) {
  for (const directory of ['scripts/forge-plugin', 'scripts/forge-shared']) {
    await copyRuntimeToolDirectory(path.join(repoRoot, directory), path.join(outputRoot, directory))
  }
  const catalogDirectory = path.join(outputRoot, 'scripts/forge-create')
  await fs.mkdir(catalogDirectory, { recursive: true })
  await fs.copyFile(
    path.join(repoRoot, 'scripts/forge-create/module-catalog.json'),
    path.join(catalogDirectory, 'module-catalog.json'),
  )
  const manifest = {
    name: projectName,
    private: true,
    type: 'module',
    scripts: { 'forge:plugin': 'node scripts/forge-plugin/index.mjs' },
  }
  await fs.writeFile(path.join(outputRoot, 'package.json'), `${JSON.stringify(manifest, null, 2)}\n`)
}

async function copyRuntimeToolDirectory(source, target) {
  const entries = await fs.readdir(source, { withFileTypes: true })
  await fs.mkdir(target, { recursive: true })
  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    if (ignoredToolNames.has(entry.name) || entry.name.startsWith('.env')
      || entry.name.endsWith('.local') || /\.(test|spec)\.[cm]?[jt]s$/.test(entry.name)) {
      continue
    }
    const sourcePath = path.join(source, entry.name)
    const targetPath = path.join(target, entry.name)
    if (entry.isDirectory()) {
      await copyRuntimeToolDirectory(sourcePath, targetPath)
    }
    else if (entry.isFile()) {
      await fs.copyFile(sourcePath, targetPath)
    }
    else {
      throw new Error(`插件工具目录不接受软链接或特殊文件：${entry.name}`)
    }
  }
}
