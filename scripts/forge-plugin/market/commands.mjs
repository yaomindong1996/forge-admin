import path from 'node:path'
import { addPlugin, inspectPluginInstall } from '../installer.mjs'
import { loadProject } from '../project.mjs'
import { idPattern } from '../descriptor.mjs'
import { check, MarketError, readMarketConfig } from './config.mjs'
import { customerLogin } from './sso.mjs'
import { ownedPlugins, pinnedRelease, pluginVersions, validateVersion } from './catalog.mjs'
import { obtainSource } from './source.mjs'

export async function runMarket(args, output, environment) {
  const controller = new AbortController()
  const interrupt = () => controller.abort()
  process.once('SIGINT', interrupt)
  process.once('SIGTERM', interrupt)
  let session
  try {
    const command = parseCommand(args)
    const context = await loadProject(environment.root)
    const config = await readMarketConfig(path.resolve(environment.cwd || process.cwd(), command.config))
    session = await (environment.login || customerLogin)(config, { signal: controller.signal,
      notify: url => output.log(`请在浏览器打开并确认客户登录（5 分钟内有效）：\n${url}`) })
    const options = { session, signal: controller.signal }
    const result = await execute(context, config, command, options)
    output.log(JSON.stringify(result, null, 2))
    return 0
  }
  catch (error) {
    // 远端响应及安装器异常可能包含用户源码，CLI 只输出稳定安全码。
    output.error(error instanceof MarketError ? error.code : 'MARKET_OPERATION_FAILED')
    return 1
  }
  finally {
    if (session) { session.token = ''; session.deviceId = '' }
    process.removeListener('SIGINT', interrupt)
    process.removeListener('SIGTERM', interrupt)
  }
}

async function execute(context, config, command, options) {
  if (command.name === 'owned') {
    const records = await ownedPlugins(config, options)
    return { protocolVersion: 1, plugins: records.map(ownedView), installed: false }
  }
  if (command.name === 'versions') {
    return { pluginId: command.id, versions: await pluginVersions(config, command.id, options), installed: false }
  }
  const release = await pinnedRelease(config, { id: command.id, version: command.version,
    coreVersion: context.coreVersion }, options)
  const source = await obtainSource(context, config, { id: command.id, release }, options)
  const flags = { force: command.force }
  const preview = await inspectPluginInstall(context.root, source, flags)
  check(!options.signal.aborted, 'MARKET_INTERRUPTED')
  if (command.name === 'check') return { release, preview, installed: false, deployed: false }
  const result = await addPlugin(context.root, source, flags)
  return { release, preview, pluginId: result.record.id, version: result.record.version,
    installed: true, deployed: false, backup: result.backup || null,
    next: '重新构建并部署；企业插件还需要有效的客户＋项目运行时许可证。' }
}

function ownedView(value) {
  check(value && idPattern.test(value.pluginId) && typeof value.name === 'string' && value.name.length <= 128
    && typeof value.downloadable === 'boolean', 'MARKET_PAGE_INVALID')
  return { pluginId: value.pluginId, name: value.name, downloadable: value.downloadable }
}

function parseCommand(args) {
  const [name, config, id, version, ...flags] = args
  check(['owned', 'versions', 'check', 'add'].includes(name) && config && !config.startsWith('-'),
    'MARKET_ARGUMENTS_INVALID')
  if (name === 'owned') {
    check(args.length === 2, 'MARKET_ARGUMENTS_INVALID')
    return { name, config }
  }
  check(typeof id === 'string' && idPattern.test(id), 'MARKET_PLUGIN_INVALID')
  if (name === 'versions') {
    check(args.length === 3, 'MARKET_ARGUMENTS_INVALID')
    return { name, config, id }
  }
  validateVersion(version)
  check(new Set(flags).size === flags.length
    && flags.every(flag => ['--reviewed', '--force'].includes(flag)), 'MARKET_ARGUMENTS_INVALID')
  check(name !== 'add' || flags.includes('--reviewed'), 'MARKET_REVIEW_REQUIRED')
  check(name !== 'check' || !flags.includes('--reviewed'), 'MARKET_ARGUMENTS_INVALID')
  return { name, config, id, version, force: flags.includes('--force') }
}
