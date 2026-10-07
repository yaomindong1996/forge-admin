#!/usr/bin/env node

import path from 'node:path'
import { realpathSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { addPlugin, listPlugins, removePlugin } from './installer.mjs'

const help = `Forge 插件工具

用法：pnpm forge:plugin add <目录|包.zip> [--force] [--dev]
      pnpm forge:plugin list
      pnpm forge:plugin remove <插件ID>

--force 整包替换已安装源码；请先提交定制改动，升级/卸载会保留恢复备份。
--dev   仅模板工程可用：宿主接入 POM + 外部源码软链接，不改动外部插件文件。
安装后需重新构建、部署；卸载不删除数据库，迁移历史及业务数据须单独评估处理。
`
const toolRoot = fileURLToPath(new URL('../..', import.meta.url))

export async function runPluginCli(args, output = console, environment = {}) {
  if (args.length === 0 || args.includes('--help') || args.includes('-h')) {
    output.log(help)
    return 0
  }
  try {
    const command = parseArguments(args)
    const root = environment.root || toolRoot
    let result
    if (command.name === 'list') {
      const records = await listPlugins(root)
      output.log(records.length ? records.map(record =>
        `${record.id}\t${record.version}\t${record.mode}\t${record.source}`).join('\n') : '未安装插件。')
      return 0
    }
    if (command.name === 'add') {
      result = await addPlugin(root, path.resolve(environment.cwd || process.cwd(), command.argument),
        command.flags, environment.hooks)
    }
    else {
      result = await removePlugin(root, command.argument, environment.hooks)
    }
    printResult(output, result, command.name)
    return 0
  }
  catch (error) {
    output.error(error.message)
    return 1
  }
}

function printResult(output, result, command) {
  output.log(`插件 ${result.record.id} ${command === 'add' ? '安装/升级' : '卸载'}成功。`)
  if (result.backup) {
    output.log(`恢复备份：${result.backup}`)
  }
  output.log('请重新构建并部署；数据库表、数据和迁移历史未删除。')
  if (command === 'remove') {
    output.log('请单独停用关联菜单；确认业务数据后再人工清理表，保留迁移历史以避免重复迁移。')
  }
  for (const warning of result.warnings || []) {
    output.log(`注意：${warning}`)
  }
}

function parseArguments(args) {
  const [name, argument, ...options] = args
  if (!['add', 'list', 'remove'].includes(name)) {
    throw new Error('未知插件命令，请使用 --help。')
  }
  if (name === 'list' && args.length === 1) {
    return { name }
  }
  if (!argument || argument.startsWith('-') || name === 'list'
    || (name === 'remove' && options.length) || new Set(options).size !== options.length
    || options.some(option => !['--force', '--dev'].includes(option))) {
    throw new Error('命令参数非法，请使用 --help。')
  }
  return { name, argument, flags: { force: options.includes('--force'), dev: options.includes('--dev') } }
}

if (process.argv[1] && import.meta.url === pathToFileURL(realpathSync(process.argv[1])).href) {
  process.exitCode = await runPluginCli(process.argv.slice(2).filter(argument => argument !== '--'))
}
