#!/usr/bin/env node
import { pathToFileURL } from 'node:url'
import { readDeliveryConfig } from './config.mjs'
import { executeDelivery } from './executor.mjs'
import { recoverLock } from './session.mjs'

export async function runDeliveryCli(args, output = console) {
  if (args.length !== 4 || !['run', 'recover-lock'].includes(args[0]) || args[3] !== '--reviewed') {
    output.error('用法：node scripts/forge-plugin-release/delivery/index.mjs <run|recover-lock> <配置> <任务ID> --reviewed')
    return 1
  }
  const controller = new AbortController()
  const abort = () => controller.abort()
  process.once('SIGINT', abort)
  process.once('SIGTERM', abort)
  try {
    const execute = args[0] === 'run' ? executeDelivery : recoverLock
    const result = await execute(await readDeliveryConfig(args[1]), args[2],
      { reviewed: true, signal: controller.signal })
    output.log(JSON.stringify(result, null, 2))
    return 0
  }
  catch (failure) {
    const code = /^[A-Z][A-Z0-9_]{0,95}$/.test(failure.code || '') ? failure.code : 'DELIVERY_FAILED'
    output.error('交付未完成：' + code + '；请核查服务端任务和本地目标锁，不要重复切换。')
    return 1
  }
  finally {
    process.removeListener('SIGINT', abort)
    process.removeListener('SIGTERM', abort)
  }
}
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href)
  process.exitCode = await runDeliveryCli(process.argv.slice(2))
