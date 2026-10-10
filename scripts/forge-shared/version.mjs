import fs from 'node:fs/promises'

const versionPattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([\w.-]+))?(?:\+([\w.-]+))?$/

export function parseVersion(value) {
  const match = typeof value === 'string' ? value.match(versionPattern) : null
  if (!match) {
    throw new Error(`非法语义版本：${value}`)
  }
  const identifiers = [match[4], match[5]].map(part => part?.split('.') || [])
  if (identifiers.flat().some(part => !/^[0-9A-Za-z-]+$/.test(part))
    || identifiers[0].some(part => /^0\d+$/.test(part))) {
    throw new Error(`非法语义版本：${value}`)
  }
  return { core: match.slice(1, 4).map(BigInt), prerelease: identifiers[0] }
}

export function compareVersions(left, right) {
  const a = parseVersion(left)
  const b = parseVersion(right)
  for (let index = 0; index < 3; index++) {
    if (a.core[index] !== b.core[index]) {
      return a.core[index] > b.core[index] ? 1 : -1
    }
  }
  return comparePrerelease(a.prerelease, b.prerelease)
}

function comparePrerelease(left, right) {
  if (!left.length || !right.length) {
    return Number(!left.length) - Number(!right.length)
  }
  for (let index = 0; index < Math.min(left.length, right.length); index++) {
    const a = left[index]
    const b = right[index]
    if (a === b) {
      continue
    }
    const numericA = /^\d+$/.test(a)
    const numericB = /^\d+$/.test(b)
    if (numericA && numericB) {
      return BigInt(a) > BigInt(b) ? 1 : -1
    }
    return numericA !== numericB ? (numericA ? -1 : 1) : (a > b ? 1 : -1)
  }
  return Math.sign(left.length - right.length)
}

export function satisfiesVersion(current, expression) {
  parseVersion(current)
  if (typeof expression !== 'string' || !expression.trim()) {
    throw new Error('核心版本兼容范围不能为空')
  }
  // 全部比较式先解析，不能因前半段不匹配而跳过后面的非法范围。
  const comparisons = expression.trim().split(/\s+/).map(token => {
    const match = token.match(/^(>=|<=|>|<|=)?(.+)$/)
    parseVersion(match?.[2])
    return { operator: match[1] || '=', version: match[2] }
  })
  return comparisons.every(({ operator, version }) => {
    const result = compareVersions(current, version)
    return { '>=': result >= 0, '>': result > 0, '<=': result <= 0, '<': result < 0, '=': result === 0 }[operator]
  })
}

export async function readPomVersion(pomFile) {
  const pom = (await fs.readFile(pomFile, 'utf8')).replace(/<!--[\s\S]*?-->/g, '')
  const properties = pom.match(/<properties>([\s\S]*?)<\/properties>/)?.[1] || ''
  const revisions = [...properties.matchAll(/<revision>\s*([^<]*?)\s*<\/revision>/g)]
  if (revisions.length !== 1) {
    throw new Error('模板根 POM 必须包含唯一、合法的明文 revision 版本')
  }
  try {
    parseVersion(revisions[0][1])
  }
  catch {
    throw new Error('模板根 POM 必须包含唯一、合法的明文 revision 版本')
  }
  return revisions[0][1]
}
