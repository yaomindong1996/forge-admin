#!/usr/bin/env node

import path from 'node:path'
import { pathToFileURL } from 'node:url'

const help = `Forge 插件工具

用法：pnpm forge:plugin --help

当前阶段：工程配置与工具交付已就绪。
add、list、remove 及 --force/--dev 将在 T8 开放，当前不会修改工程或安装插件。
`
const plannedCommands = new Set(['add', 'list', 'remove'])

export function runPluginCli(args, output = console) {
  if (args.length === 0 || args.includes('--help') || args.includes('-h')) {
    output.log(help)
    return 0
  }
  const command = args[0]
  if (plannedCommands.has(command)) {
    output.error(`插件 ${command} 命令尚未开放，将在 T8 实现；未修改任何工程文件。`)
  }
  else {
    output.error('未知插件命令，请使用 --help 查看当前工具能力。')
  }
  return 1
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  process.exitCode = runPluginCli(process.argv.slice(2).filter(argument => argument !== '--'))
}
