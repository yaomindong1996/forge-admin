import { realpathSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { BuildError } from '../forge-plugin-builder/errors.mjs'
import { readReleaseConfig } from './config.mjs'
import { checkRelease, publishRelease } from './vault.mjs'
import { verifyRelease } from './verify.mjs'
import { readApprovalConfig } from './approval-config.mjs'
import { verifyApproval } from './approval.mjs'

const help = `forge:plugin-release（本地候选制品，不部署）
  check <absolute-release.json>
  publish <absolute-release.json> --reviewed
  verify <absolute-vault.json> <rel-sha256>
  verify-approval <absolute-approval.json> <rel-sha256>
release.json: protocolVersion=1, repositoryId, vaultRoot, jobRoot, resultSha256
vault.json: protocolVersion=1, repositoryId, vaultRoot
目录须预创建、canonical、当前非 root 用户私有；--reviewed 不代表服务端审批。
verify-approval 只核对指定任务的当前审批，不登记/部署；凭证仅从 worker 环境读取。`

export async function runReleaseCli(args, output = console) {
  if (args.length === 1 && ['help', '--help', '-h'].includes(args[0])) {
    output.log(help)
    return 0
  }
  const controller = new AbortController()
  const stop = () => controller.abort()
  process.once('SIGINT', stop)
  process.once('SIGTERM', stop)
  try {
    const result = await dispatch(args, controller.signal)
    output.log(JSON.stringify(result, null, 2))
    return 0
  }
  catch (error) {
    // 禁止泄露文件异常中的绝对路径或原始报告/配置内容。
    output.error(JSON.stringify({ code: error instanceof BuildError ? error.code : 'RELEASE_FAILED', deployed: false }))
    return 1
  }
  finally {
    process.removeListener('SIGINT', stop)
    process.removeListener('SIGTERM', stop)
  }
}

async function dispatch(args, signal) {
  const [command, file, extra] = args
  const valid = typeof file === 'string' && file.startsWith('/') && (
    (command === 'check' && args.length === 2)
    || (command === 'publish' && args.length === 3 && extra === '--reviewed')
    || (['verify', 'verify-approval'].includes(command) && args.length === 3))
  if (!valid) throw new BuildError('RELEASE_COMMAND_INVALID')
  if (command === 'verify-approval') return verifyApproval(await readApprovalConfig(file), extra, { signal })
  const config = await readReleaseConfig(file, command === 'verify' ? 'verify' : 'publish')
  if (command === 'check') return checkRelease(config, signal)
  if (command === 'verify') return verifyRelease(config, extra, signal)
  return publishRelease(config, { reviewed: true, signal })
}

if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  process.exitCode = await runReleaseCli(process.argv.slice(2))
}
