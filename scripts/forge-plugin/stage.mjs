import fs from 'node:fs/promises'
import path from 'node:path'
import { renameComponent, decodeUtf8 } from './content.mjs'
import { componentFiles, fingerprint, readInstalledDirectory } from './bundle.mjs'
import { normalizeParentPath, validateSourcePom } from './pom.mjs'
import { finalModule, targetsFor } from './project.mjs'
import { sourceComponent, devWrapperDigest } from './ownership.mjs'
import { requireCondition, validateRelative } from './paths.mjs'
import { identityContext } from './identity.mjs'

export function validateComponents(context, bundle, descriptor) {
  const components = {}
  if (descriptor.server) {
    components.server = componentFiles(bundle.files, `server/${descriptor.server.module}`)
    const pom = components.server.get('pom.xml')
    requireCondition(pom, '插件后端缺少 pom.xml')
    validateSourcePom(decodeUtf8(pom), descriptor.server.module)
    requireCondition(!['forge', 'forge-server'].includes(descriptor.server.module)
      && !Object.values(context.artifactMap).includes(finalModule(context, descriptor)),
      '插件 module 与核心模块重名')
  }
  if (descriptor.ui) {
    components.ui = componentFiles(bundle.files, descriptor.ui.dir)
    const serverPath = `server/${descriptor.server?.module}`
    requireCondition(!descriptor.server || !(descriptor.ui.dir.startsWith(`${serverPath}/`)
      || serverPath.startsWith(`${descriptor.ui.dir}/`) || serverPath === descriptor.ui.dir), '前后端组件目录不能重叠')
  }
  return components
}

export async function stageComponents(work, context, installation) {
  const { descriptor, components, source, dev } = installation
  const record = { ...descriptor, mode: dev ? 'dev' : 'copy', source, checksums: {} }
  const operations = []
  for (const [kind, relative] of Object.entries(targetsFor(context, descriptor))) {
    if (dev) {
      if (kind === 'server') {
        const directory = await stageDevServer(work, record, components.server)
        record.checksums.server = await devWrapperDigest(directory, record)
        operations.push({ relative, stage: directory })
      }
      else {
        operations.push({ relative, link: sourceComponent(record, kind) })
      }
      continue
    }
    const directory = path.join(work, `new-${kind}`)
    await writeFiles(directory, components[kind])
    if (!context.template) {
      const artifactMap = { ...context.artifactMap }
      if (descriptor.server) {
        artifactMap[descriptor.server.module] = finalModule(context, descriptor)
      }
      await renameComponent(directory, components[kind], identityContext({ ...context, artifactMap }, descriptor))
    }
    if (kind === 'server') {
      const pomFile = path.join(directory, 'pom.xml')
      await fs.writeFile(pomFile, normalizeParentPath(await fs.readFile(pomFile, 'utf8')))
      // 元数据不属于改名协议；避免品牌/路径替换误改插件 ID 和功能编码。
      await fs.writeFile(path.join(directory, 'src/main/resources/META-INF/forge-plugin.json'),
        components.server.get('src/main/resources/META-INF/forge-plugin.json'))
    }
    await fs.writeFile(path.join(directory, '.forge-plugin-owned.json'),
      JSON.stringify({ id: descriptor.id, version: descriptor.version }))
    record.checksums[kind] = fingerprint(await readInstalledDirectory(directory))
    operations.push({ relative, stage: directory })
  }
  return { record, operations }
}

async function stageDevServer(work, record, files) {
  const directory = path.join(work, 'new-server')
  const topLevel = new Set([...files.keys()].map(name => name.split('/')[0]))
  const links = [...topLevel].filter(name => name !== 'pom.xml').sort()
  await fs.mkdir(directory)
  // Maven 按真实文件位置解析 parent；只在宿主生成接入 POM，业务源码仍引用外部文件。
  await fs.writeFile(path.join(directory, 'pom.xml'), normalizeParentPath(decodeUtf8(files.get('pom.xml'))))
  await fs.writeFile(path.join(directory, '.forge-plugin-owned.json'), JSON.stringify({ id: record.id,
    version: record.version, mode: 'dev', links }))
  for (const name of links) {
    await fs.symlink(path.join(sourceComponent(record, 'server'), name), path.join(directory, name))
  }
  return directory
}

async function writeFiles(root, files) {
  await fs.mkdir(root)
  for (const [relative, data] of files) {
    validateRelative(relative)
    const target = path.join(root, relative)
    await fs.mkdir(path.dirname(target), { recursive: true })
    await fs.writeFile(target, data, { flag: 'wx' })
  }
}
