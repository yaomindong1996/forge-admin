#!/usr/bin/env node

import { realpathSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { checkEdition } from './edition.mjs'

const toolRoot = fileURLToPath(new URL('../..', import.meta.url))
const help = '用法：pnpm check:edition\n'
  + '检查开源模板工作区可提交文件和 Git 索引，不修改任何文件。'

export async function runEditionCli(args, output = console, root = toolRoot) {
  if (args.length === 1 && ['--help', '-h'].includes(args[0])) {
    output.log(help)
    return 0
  }
  if (args.length) {
    output.error(help)
    return 1
  }
  try {
    const result = await checkEdition(root)
    if (!result.violations.length) {
      output.log(`开源边界检查通过（索引 ${result.indexed}，工作区 ${result.working} 个文件）。`)
      return 0
    }
    output.error('开源边界检查失败：')
    for (const item of result.violations) {
      output.error(`[${item.source}] ${JSON.stringify(item.relative)}：${item.rule}`)
    }
    return 1
  }
  catch (error) {
    output.error(`开源边界无法完成检查：${error.message}`)
    return 1
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(realpathSync(process.argv[1])).href) {
  process.exitCode = await runEditionCli(process.argv.slice(2).filter(argument => argument !== '--'))
}
