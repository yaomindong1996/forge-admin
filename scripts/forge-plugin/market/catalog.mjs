import { idPattern } from '../descriptor.mjs'
import { parseVersion, satisfiesVersion } from '../../forge-shared/version.mjs'
import { archiveLimit, check } from './config.mjs'
import { request } from './http.mjs'

export async function ownedPlugins(config, options) {
  return pages(config, '/api/plugins/mine', options)
}

export async function pluginVersions(config, id, options) {
  check(typeof id === 'string' && idPattern.test(id), 'MARKET_PLUGIN_INVALID')
  check(await request(config, `/api/plugins/${id}/access`, options) === true, 'MARKET_ACCESS_DENIED')
  return (await pages(config, `/api/plugins/${id}/versions`, options)).map(validateRelease)
}

export async function pinnedRelease(config, selection, options) {
  check(typeof selection.id === 'string' && idPattern.test(selection.id), 'MARKET_PLUGIN_INVALID')
  validateVersion(selection.version)
  const versions = await pluginVersions(config, selection.id, options)
  const selected = versions.filter(value => value.version === selection.version)
  check(selected.length === 1, 'MARKET_VERSION_NOT_FOUND')
  const release = selected[0]
  check(satisfiesVersion(selection.coreVersion, release.requiresCore), 'MARKET_CORE_INCOMPATIBLE')
  return release
}

export function validateRelease(value) {
  check(value && (typeof value.id === 'string' || Number.isSafeInteger(value.id)), 'MARKET_RELEASE_INVALID')
  const id = String(value.id)
  check(/^[1-9]\d{0,18}$/.test(id) && BigInt(id) <= 9223372036854775807n, 'MARKET_RELEASE_INVALID')
  validateVersion(value.version)
  check(['community', 'ee'].includes(value.edition) && value.status === 'PUBLISHED'
    && typeof value.requiresCore === 'string' && value.requiresCore.length <= 128
    && typeof value.sha256 === 'string' && /^[a-f0-9]{64}$/.test(value.sha256)
    && Number.isSafeInteger(value.archiveBytes) && value.archiveBytes > 0 && value.archiveBytes <= archiveLimit,
  'MARKET_RELEASE_INVALID')
  return { id, version: value.version, edition: value.edition, requiresCore: value.requiresCore,
    sha256: value.sha256, archiveBytes: value.archiveBytes }
}

export function validateVersion(value) {
  check(typeof value === 'string' && value.length <= 128, 'MARKET_VERSION_INVALID')
  try { parseVersion(value) }
  catch { check(false, 'MARKET_VERSION_INVALID') }
}

async function pages(config, route, options) {
  const records = []
  let total
  for (let page = 1; page <= 100; page++) {
    const value = await request(config, `${route}?pageNum=${page}&pageSize=50`, options)
    check(value && Array.isArray(value.records) && value.records.length <= 50
      && Number.isSafeInteger(value.total) && value.total >= 0 && value.total <= 5000, 'MARKET_PAGE_INVALID')
    if (total === undefined) total = value.total
    check(total === value.total && records.length + value.records.length <= total, 'MARKET_PAGE_CHANGED')
    records.push(...value.records)
    if (records.length >= value.total) return records
    check(value.records.length > 0, 'MARKET_PAGE_INVALID')
  }
  check(false, 'MARKET_PAGE_LIMIT')
}
