#!/usr/bin/env node
/**
 * Fail if a new MySQL migration under db/mysql/migration lacks a same-version
 * sibling under db/postgresql/migration (after baseline 1.0.234).
 *
 * Usage: node forge-server/scripts/db/check-pg-migration-pairs.mjs
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const forgeDir = path.resolve(__dirname, '../..')
const mysqlDir = path.join(forgeDir, 'db/mysql/migration')
const pgDir = path.join(forgeDir, 'db/postgresql/migration')
const BASELINE = [1, 0, 234]

function parseVersion(fileName) {
  const match = /^V(\d+)\.(\d+)\.(\d+)__/.exec(fileName)
  if (!match) return null
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

function cmp(a, b) {
  for (let i = 0; i < 3; i += 1) {
    if (a[i] !== b[i]) return a[i] - b[i]
  }
  return 0
}

function listSql(dir) {
  if (!fs.existsSync(dir)) return []
  return fs.readdirSync(dir).filter((name) => name.endsWith('.sql'))
}

const mysqlFiles = listSql(mysqlDir)
const pgFiles = new Set(listSql(pgDir))
const missing = []

for (const file of mysqlFiles) {
  const version = parseVersion(file)
  if (!version || cmp(version, BASELINE) <= 0) continue
  if (!pgFiles.has(file)) missing.push(file)
}

if (missing.length) {
  console.error('Missing PostgreSQL twin migrations for:')
  for (const file of missing) console.error(`  - ${file}`)
  console.error(`Place twins under ${path.relative(process.cwd(), pgDir)}`)
  process.exit(1)
}

console.log('OK: all MySQL migrations after 1.0.234 have PostgreSQL twins (or none yet).')
