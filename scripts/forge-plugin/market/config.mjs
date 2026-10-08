import { readRegular } from '../../forge-plugin-builder/files.mjs'
import { parseStrictJson } from '../json.mjs'
import fs from 'node:fs/promises'

export const callbackUrl = 'http://127.0.0.1:37329/callback'
export const archiveLimit = 8 * 1024 * 1024
export const jsonLimit = 64 * 1024

export class MarketError extends Error {
  constructor(code) { super(code); this.code = code }
}

export function check(condition, code) {
  if (!condition) throw new MarketError(code)
}

export function parseJson(data) {
  try { return parseStrictJson(new TextDecoder('utf-8', { fatal: true }).decode(data), jsonLimit) }
  catch { throw new MarketError('MARKET_JSON_INVALID') }
}

export async function readMarketConfig(file) {
  const stat = await fs.lstat(file)
  check(typeof process.getuid === 'function' && stat.isFile() && !stat.isSymbolicLink()
    && stat.uid === process.getuid() && (stat.mode & 0o022) === 0, 'MARKET_CONFIG_UNSAFE')
  const value = parseJson(await readRegular(file, jsonLimit))
  const keys = ['protocolVersion', 'apiBaseUrl', 'authorizeUrl', 'clientId']
  check(value && Object.keys(value).length === keys.length
    && Object.keys(value).every(key => keys.includes(key)) && value.protocolVersion === 1,
  'MARKET_CONFIG_INVALID')
  check(value.clientId === 'forge-cli', 'MARKET_CLIENT_INVALID')
  return { ...value, apiBaseUrl: endpoint(value.apiBaseUrl), authorizeUrl: endpoint(value.authorizeUrl) }
}

export function endpoint(value) {
  check(typeof value === 'string' && value.length <= 2048 && !/[\s\\%]/.test(value), 'MARKET_URL_INVALID')
  let url
  try { url = new URL(value) }
  catch { throw new MarketError('MARKET_URL_INVALID') }
  const loopback = ['localhost', '127.0.0.1'].includes(url.hostname)
  check((url.protocol === 'https:' || (url.protocol === 'http:' && loopback))
    && !url.username && !url.password && !url.search && !url.hash
    && url.pathname.split('/').every(part => !['.', '..'].includes(part)), 'MARKET_URL_INVALID')
  check(url.href.replace(/\/$/, '') === value.replace(/\/$/, ''), 'MARKET_URL_INVALID')
  return url.href.replace(/\/$/, '')
}
