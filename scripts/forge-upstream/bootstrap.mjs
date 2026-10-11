import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { git, importTree, writeJson } from './git.mjs'
import { generateSnapshot, copyGenerator, generatorDigest } from './source.mjs'
import { mappingVersion } from './policy.mjs'
import { statePath, generatorPath, gitStateDirectory, normalizerDigest } from './state.mjs'

const toolRoot = path.dirname(fileURLToPath(import.meta.url))

export async function bootstrap({ source, ref, target, options, generator }) {
  // mkdir 不接受已有目录（即使为空），避免初始化清理误伤已有工程。
  await fs.mkdir(target)
  const generated = await generateSnapshot({ source, ref, options, generator })
  try {
    for (const entry of await fs.readdir(generated.snapshot)) {
      await fs.cp(path.join(generated.snapshot, entry), path.join(target, entry),
        { recursive: true, errorOnExist: true, force: false })
    }
    git(target, ['init', '-b', 'main'])
    const directory = gitStateDirectory(target)
    await fs.mkdir(directory, { recursive: true })
    const baseCommit = await importTree({ root: target, snapshot: generated.snapshot,
      message: `引入社区底座 ${generated.version}（${generated.commit.slice(0, 12)}）`,
      indexFile: path.join(directory, 'initial.index') })
    git(target, ['update-ref', 'refs/heads/main', baseCommit])
    git(target, ['reset', '--mixed', baseCommit])
    await copyGenerator(generator, path.join(target, generatorPath))
    const state = { schemaVersion: 1, mappingVersion, productAnchor: generated.version,
      upstream: { repository: upstreamUrl(source), commit: generated.commit, version: generated.version },
      baseCommit, generatorSha256: await generatorDigest(generator),
      normalizerSha256: await normalizerDigest(), options }
    await writeJson(path.join(target, statePath), state)
    await installTools(target)
    await initializeProjectContext(target, options)
    git(target, ['add', '--all'])
    git(target, ['commit', '-m', '初始化独立工程、开发边界与可追踪社区基线'])
    return { target, ...state }
  }
  finally { await fs.rm(generated.temp, { recursive: true, force: true }) }
}

function upstreamUrl(source) {
  try {
    const url = git(source, ['remote', 'get-url', 'origin']).trim()
    // 不把带凭据的 URL、本机路径或临时目录写入可提交基线。
    if (/^https:\/\/[^@\s]+$/.test(url) && !url.includes('?')) return url
    if (/^git@[\w.-]+:[\w./-]+$/.test(url)) return url
  }
  catch { /* 本地源码仓库可不配置远端，仍用准确 commit 追踪。 */ }
  return null
}

async function installTools(root) {
  const target = path.join(root, 'scripts/forge-upstream')
  await fs.mkdir(target, { recursive: true })
  for (const entry of await fs.readdir(toolRoot, { withFileTypes: true })) {
    if (entry.isFile() && !entry.name.endsWith('.test.mjs')) {
      await fs.copyFile(path.join(toolRoot, entry.name), path.join(target, entry.name))
    }
  }
  const manifest = JSON.parse(await fs.readFile(path.join(root, 'package.json'), 'utf8'))
  Object.assign(manifest.scripts, {
    'forge:upstream': 'node scripts/forge-upstream/index.mjs',
    'version:check': 'node scripts/forge-shared/version-cli.mjs --check',
    'version:sync': 'node scripts/forge-shared/version-cli.mjs --sync',
  })
  await writeJson(path.join(root, 'package.json'), manifest)
}

async function initializeProjectContext(root, options) {
  const readme = path.join(root, 'README.md')
  await fs.appendFile(readme, '\n## 社区同步与独立开发\n\n'
    + '- 本工程拥有独立 Git 历史；没有配置 push 远端，不能推回社区仓库。\n'
    + '- 同步说明见 `scripts/forge-upstream/README.md`；身份与基线见 `.forge/upstream/baseline.json`。\n'
    + '- 产品发行版本取根 POM revision；社区兼容版本取基线 upstream.version，二者独立。\n'
    + '- `forge.config.json` 的 forgeVersion 记录社区插件兼容底座，不随产品升版手改。\n'
    + '- 历史迁移不可修改；专属插件迁移使用插件安装器独立记录，不加入社区迁移序列。\n'
    + '- 本地连接信息只写忽略的 application-dev.yml / .env.development.local。\n')
  const agents = path.join(root, 'AGENTS.md')
  let content = await fs.readFile(agents, 'utf8')
  content = content.replace(/### 5\.18 开源模板仓库禁止企业版代码[\s\S]*?(?=### 5\.19)/,
    '### 5.18 独立产品开发边界\n\n'
    + '- 本工程为独立私有产品；专属功能与收费插件只在此工程或单独私有源码仓库开发。\n'
    + '- 禁止将产品主分支合并或推送到社区仓库；通用 BUG 修复提取不含专属代码的独立提交。\n'
    + '- 社区升级必须使用 scripts/forge-upstream 工具，先预览、后在升级分支三方合并。\n'
    + '- 不得修改冻结映射和历史迁移来消除冲突；也不得重新运行生成器覆盖当前工程。\n\n')
  await fs.writeFile(agents, content)
  await fs.writeFile(path.join(root, 'CHANGELOG.md'), '# 更新日志\n\n## [Unreleased]\n\n'
    + '- 建立独立产品开发工程及可追踪社区基线；当前为开发初始化，不代表正式发布。\n')
  for (const directory of [`${options['artifact-prefix']}-server/plugins`,
    `${options['project-name']}-admin-ui/src/views/plugins`]) {
    await fs.mkdir(path.join(root, directory), { recursive: true })
    await fs.writeFile(path.join(root, directory, '.gitkeep'), '')
  }
}
