import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import test from 'node:test'

const server = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const read = name => readFileSync(path.join(server, name), 'utf8')
const example = read('scripts/deployment/forge-admin.env.example')
const pom = read('forge-admin-server/pom.xml')
const application = read('forge-admin-server/src/main/resources/application.yml')
const compose = readFileSync(path.join(server, '../docker-forge-admin/docker-compose.yml'), 'utf8')

test('Admin defaults to bundled migrations and fails when the location is missing', () => {
  const location = application.split(/\r?\n/).find(line => /^\s+locations:/.test(line))?.trim()
  const strict = application.split(/\r?\n/).find(line => /fail-on-missing-locations:/.test(line))?.trim()
  assert.equal(location, 'locations: ${FORGE_FLYWAY_LOCATIONS:classpath:db/migration}')
  assert.equal(strict, 'fail-on-missing-locations: true')
})

test('standalone deployment scans only migrations bundled in the JAR', () => {
  const entries = example.split(/\r?\n/).filter(line => line.trim() && !line.startsWith('#'))
  assert.deepEqual(entries, ['FORGE_FLYWAY_LOCATIONS=classpath:db/migration'])
  assert.doesNotMatch(example, /(?:PASSWORD|SECRET|TOKEN|USERNAME)=/)
})

test('Admin packaging includes unfiltered versioned SQL in the expected classpath', () => {
  const resources = [...pom.matchAll(/<resource>([\s\S]*?)<\/resource>/g)].map(match => match[1])
  const migration = resources.find(resource => resource.includes('<targetPath>db/migration</targetPath>'))
  assert.ok(migration, 'Missing packaged migration resource')
  assert.match(migration, /<directory>\$\{project\.basedir\}\/\.\.\/db\/migration<\/directory>/)
  assert.match(migration, /<filtering>false<\/filtering>/)
  assert.match(migration, /<include>\*\.sql<\/include>/)
})

test('deployment override is consumed without changing Admin history or disabling validation', () => {
  assert.match(application, /locations:\s*\$\{FORGE_FLYWAY_LOCATIONS:/)
  assert.match(application, /table:\s*forge_schema_history\b/)
  assert.doesNotMatch(application, /validate-on-migrate:\s*false/)
  assert.doesNotMatch(example, /(?:ENABLED=false|VALIDATE_ON_MIGRATE=false|SCHEMA_HISTORY|REPAIR)/)
})

test('Admin Docker deployment does not depend on a host migration directory', () => {
  const admin = compose.match(/^  forge-admin:\n([\s\S]*?)(?=^  forge-ui:)/m)?.[1]
  assert.ok(admin, 'Admin service configuration is missing')
  assert.equal(admin.includes('db/migration'), false, 'Admin must not mount host migration SQL')
  assert.match(admin, /application-datasource\.yml:\/app\/application-prod\.yml/)
})
