import { exactKeys, privateDirectory, readJson } from '../config.mjs'
import { ensure } from '../../forge-plugin-builder/errors.mjs'

export async function readCosConfig(file) {
  const { value } = await readJson(file)
  exactKeys(value, ['protocolVersion', 'repositoryId', 'vaultRoot', 'bucket', 'region', 'prefix', 'sourcePrefix'])
  ensure(value.protocolVersion === 1 && typeof value.repositoryId === 'string'
    && /^[a-z][a-z0-9-]{0,63}$/.test(value.repositoryId), 'COS_CONFIG_INVALID')
  ensure(typeof value.bucket === 'string' && typeof value.region === 'string'
    && /^[a-z0-9-]+-[0-9]{5,20}$/.test(value.bucket) && /^[a-z]{2}-[a-z]+(?:-\d+)?$/.test(value.region),
    'COS_BUCKET_INVALID')
  const valid = item => typeof item === 'string' && /^[a-z][a-z0-9/-]{0,120}\/$/.test(item)
    && !item.includes('//')
  ensure(valid(value.prefix) && valid(value.sourcePrefix)
    && !value.prefix.startsWith(value.sourcePrefix) && !value.sourcePrefix.startsWith(value.prefix),
  'COS_PREFIX_OVERLAP')
  return { ...value, vaultRoot: await privateDirectory(value.vaultRoot) }
}
