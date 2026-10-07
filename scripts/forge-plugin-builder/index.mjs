#!/usr/bin/env node
import { realpathSync } from 'node:fs'
import { pathToFileURL } from 'node:url'
import { readConfig } from './config.mjs'
import { executeBuild } from './builder.mjs'
import { ensure, errorCode } from './errors.mjs'

const help = `Forge 离线插件构建执行器（不部署、不连接 Web 队列）

用法：node scripts/forge-plugin-builder/index.mjs check /absolute/builder.json [--force]
      node scripts/forge-plugin-builder/index.mjs run /absolute/builder.json --reviewed [--force]

先审查源码/插件包/镜像，再执行 run；只使用本地 rootless Docker 和固定离线命令。
check 不调用 Docker、不修改原工程，在独占工作目录保存预检及结果；run 只构建该副本。
结果：workspaceRoot/job-*/result.json；产物：对应 artifacts 目录。构建成功不等于部署成功。
配置与隔离要求见 scripts/forge-plugin-builder/README.md；缺少容器时不会在宿主降级执行。
`

export async function runBuilderCli(args, output = console) {
  if (!args.length || (args.length === 1 && ['--help', '-h'].includes(args[0]))) {
    output.log(help)
    return 0
  }
  const controller = new AbortController()
  const abort = () => controller.abort()
  process.on('SIGINT', abort)
  process.on('SIGTERM', abort)
  try {
    const [command, file, ...flags] = args
    ensure(['check', 'run'].includes(command) && file && !file.startsWith('-')
      && new Set(flags).size === flags.length
      && flags.every(flag => ['--force', '--reviewed'].includes(flag)), 'ARGUMENTS_INVALID')
    ensure(command !== 'check' || !flags.includes('--reviewed'), 'ARGUMENTS_INVALID')
    const result = await executeBuild(await readConfig(file), { run: command === 'run',
      reviewed: flags.includes('--reviewed'), force: flags.includes('--force'), signal: controller.signal })
    output.log(JSON.stringify(result, null, 2))
    return result.status === 'failed' ? 1 : 0
  }
  catch (error) {
    output.error(JSON.stringify({ status: 'failed', failureCode: errorCode(error), deployed: false }))
    return 1
  }
  finally {
    process.removeListener('SIGINT', abort)
    process.removeListener('SIGTERM', abort)
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(realpathSync(process.argv[1])).href) {
  process.exitCode = await runBuilderCli(process.argv.slice(2).filter(argument => argument !== '--'))
}
