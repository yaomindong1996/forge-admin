import fs from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { constants, realpathSync } from 'node:fs'
import { addPlugin } from '../forge-plugin/installer.mjs'
import { loadProject } from '../forge-plugin/project.mjs'
import { runProcess } from './process.mjs'
import { ensure, errorCode } from './errors.mjs'
import { buildTimeoutMs } from './docker.mjs'

export function buildCommands(context) {
  const server = path.join('/work/project', context.server)
  const bom = context.artifactMap['forge-dependencies']
  const framework = context.artifactMap['forge-framework']
  const admin = context.artifactMap['forge-admin-server']
  const offline = ['-o', '-q', '-Dmaven.repo.local=/work/maven', '-DskipTests']
  return {
    backend: [
      { executable: '/opt/forge/bin/mvn', cwd: server,
        args: [...offline, '-f', `${framework}/${bom}/pom.xml`, 'install'] },
      { executable: '/opt/forge/bin/mvn', cwd: server, args: [...offline, '-pl', admin, '-am', 'package'] },
    ],
    frontend: [
      { executable: '/opt/forge/bin/pnpm', cwd: path.join('/work/project', context.ui),
        args: ['--ignore-workspace', 'install', '--offline', '--frozen-lockfile', '--ignore-scripts',
          '--store-dir=/work/pnpm-store'] },
      { executable: '/usr/local/bin/node', cwd: path.join('/work/project', context.ui),
        args: ['node_modules/vite/bin/vite.js', 'build', '--outDir=/work/ui-dist'] },
    ],
  }
}

// 只从受控挂载复制到 tmpfs；不从源包读取命令，不启动服务/迁移/部署。
export async function buildInsideContainer(force) {
  ensure(fileURLToPath(new URL('..', import.meta.url)) === '/control/', 'CONTAINER_ONLY')
  ensure(process.getuid() === 0, 'CONTAINER_USER_MISMATCH')
  await fs.mkdir('/work/home', { mode: 0o700 })
  await fs.cp('/source', '/work/project', { recursive: true, dereference: false })
  const context = await loadProject('/work/project')
  const result = await addPlugin('/work/project', '/package.zip', { force })
  const commands = buildCommands(context)
  if (result.record.server) {
    await fs.cp('/opt/forge/cache/maven', '/work/maven', { recursive: true })
    await executeCommands(commands.backend)
    await copyBackend(context)
  }
  if (result.record.ui) {
    await fs.cp('/opt/forge/cache/pnpm', '/work/pnpm-store', { recursive: true })
    await executeCommands(commands.frontend)
    await fs.cp('/work/ui-dist', '/output/frontend', { recursive: true, dereference: false })
  }
}

async function executeCommands(commands) {
  for (const command of commands) {
    await runProcess(command.executable, command.args, { cwd: command.cwd, timeoutMs: buildTimeoutMs,
      env: { PATH: '/opt/forge/bin:/usr/local/bin:/usr/bin:/bin', HOME: '/work/home', CI: 'true',
        LANG: 'C.UTF-8', JAVA_HOME: '/opt/forge/java', MAVEN_OPTS: '-Xmx1536m',
        NODE_OPTIONS: '--max-old-space-size=2048' } })
  }
}

async function copyBackend(context) {
  const target = path.join('/work/project', context.server, context.artifactMap['forge-admin-server'], 'target')
  const jars = (await fs.readdir(target)).filter(name => name.endsWith('.jar')
    && !name.endsWith('-sources.jar') && !name.endsWith('-javadoc.jar'))
  ensure(jars.length === 1, 'BACKEND_JAR_AMBIGUOUS')
  const source = path.join(target, jars[0])
  const stat = await fs.lstat(source)
  ensure(stat.isFile() && !stat.isSymbolicLink(), 'BACKEND_JAR_UNSAFE')
  await fs.mkdir('/output/backend', { mode: 0o700 })
  await fs.copyFile(source, '/output/backend/admin.jar', constants.COPYFILE_EXCL)
}

if (process.argv[1] && import.meta.url === pathToFileURL(realpathSync(process.argv[1])).href) {
  try {
    ensure(process.argv.slice(2).every(arg => arg === '--force') && process.argv.length <= 3, 'ARGUMENTS_INVALID')
    await buildInsideContainer(process.argv.includes('--force'))
  }
  catch (error) {
    // 原始编译日志可能带业务数据/本地配置；仅回传可分类错误码。
    process.stderr.write(`${errorCode(error)}\n`)
    process.exitCode = 1
  }
}
