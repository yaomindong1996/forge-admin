#!/usr/bin/env node
import { realpathSync } from 'node:fs'
import { pathToFileURL } from 'node:url'
import { ensure, errorCode } from '../../forge-plugin-builder/errors.mjs'
import { readComposeConfig } from './config.mjs'
import { inspectCompose, prepareCompose, verifyCompose } from './prepare.mjs'

export async function runComposeCli(args, output = console) {
  const controller = new AbortController()
  const interrupt = () => controller.abort()
  process.once('SIGINT', interrupt)
  process.once('SIGTERM', interrupt)
  try {
    const [command, file, id, flag] = args
    ensure(['check', 'prepare', 'verify'].includes(command) && file && id
      && (command !== 'prepare' ? args.length === 3 : args.length === 4 && flag === '--reviewed'),
    'COMPOSE_ARGUMENTS_INVALID')
    const config = await readComposeConfig(file)
    const result = await execute(command, config, id, controller.signal)
    output.log(JSON.stringify(result, null, 2))
    return 0
  }
  catch (error) { output.error(errorCode(error)); return 1 }
  finally {
    process.removeListener('SIGINT', interrupt)
    process.removeListener('SIGTERM', interrupt)
  }
}

async function execute(command, config, id, signal) {
  if (command === 'check') return (await inspectCompose(config, id, signal)).summary
  if (command === 'verify') return verifyCompose(config, id, signal)
  return prepareCompose(config, id, { reviewed: true, signal })
}

if (process.argv[1] && import.meta.url === pathToFileURL(realpathSync(process.argv[1])).href) {
  process.exitCode = await runComposeCli(process.argv.slice(2))
}
