import { parseVersion } from '../forge-shared/version.mjs'
import { idPattern } from '../forge-plugin/descriptor.mjs'
import { validateRelative } from '../forge-plugin/paths.mjs'
import { sha256, validateFileNames } from '../forge-plugin-builder/files.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'
import { digestPattern, exactKeys } from './config.mjs'

export const metadataLimit = 8 * 1024 * 1024
export const serialize = value => `${JSON.stringify(value, null, 2)}\n`
export const artifactDigest = files => sha256(files.map(file => `${file.path}:${file.bytes}:${file.sha256}\n`).join(''))
const matches = (pattern, value) => typeof value === 'string' && pattern.test(value)

export function buildManifest(result, config) {
  ensure(result?.protocolVersion === 1 && result.status === 'built' && result.deployed === false,
    'SUCCESSFUL_BUILD_REQUIRED')
  ensure(result.preflight && result.source?.scope === 'admin-build', 'BUILD_EVIDENCE_INVALID')
  const targets = Object.keys(result.preflight.targets || {}).sort()
  ensure(targets.length > 0 && targets.every(key => ['server', 'ui'].includes(key)), 'BUILD_TARGET_INVALID')
  for (const key of targets) validateRelative(result.preflight.targets[key])
  const manifest = {
    protocolVersion: 1, repositoryId: config.repositoryId, jobId: result.jobId,
    resultSha256: config.resultSha256,
    plugin: { id: result.preflight.pluginId, version: result.preflight.version,
      coreVersion: result.preflight.coreVersion, edition: result.preflight.edition,
      operation: result.preflight.operation },
    source: { commit: result.source.commit, sha256: result.source.sha256, scope: result.source.scope },
    packageSha256: result.packageSha256, image: result.image, targets,
    artifactManifestSha256: artifactDigest(validateArtifacts(result.artifacts)), artifacts: result.artifacts,
  }
  return normalizeManifest(manifest)
}

export function normalizeManifest(value) {
  exactKeys(value, ['protocolVersion', 'repositoryId', 'jobId', 'resultSha256', 'plugin', 'source',
    'packageSha256', 'image', 'targets', 'artifactManifestSha256', 'artifacts'])
  ensure(value.protocolVersion === 1 && matches(/^[a-z][a-z0-9-]{0,63}$/, value.repositoryId)
    && matches(/^job-[a-zA-Z0-9_-]{1,64}$/, value.jobId), 'RELEASE_METADATA_INVALID')
  exactKeys(value.plugin, ['id', 'version', 'coreVersion', 'edition', 'operation'])
  const plugin = normalizePlugin(value.plugin)
  exactKeys(value.source, ['commit', 'sha256', 'scope'])
  ensure(matches(/^[a-f0-9]{40}$/, value.source.commit) && matches(digestPattern, value.source.sha256)
    && value.source.scope === 'admin-build', 'RELEASE_SOURCE_INVALID')
  ensure(matches(digestPattern, value.resultSha256) && matches(digestPattern, value.packageSha256)
    && matches(/^[a-z0-9][a-z0-9./_-]*@sha256:[a-f0-9]{64}$/, value.image), 'RELEASE_DIGEST_INVALID')
  const targets = validateTargets(value.targets)
  const artifacts = validateArtifacts(value.artifacts)
  ensure(value.artifactManifestSha256 === artifactDigest(artifacts), 'ARTIFACT_MANIFEST_MISMATCH')
  return { protocolVersion: 1, repositoryId: value.repositoryId, jobId: value.jobId,
    resultSha256: value.resultSha256, plugin,
    source: { commit: value.source.commit, sha256: value.source.sha256, scope: 'admin-build' },
    packageSha256: value.packageSha256, image: value.image, targets,
    artifactManifestSha256: value.artifactManifestSha256, artifacts }
}

function normalizePlugin(value) {
  ensure(typeof value.id === 'string' && idPattern.test(value.id)
    && value.edition === 'community' && ['install', 'replace'].includes(value.operation), 'RELEASE_PLUGIN_INVALID')
  for (const version of [value.version, value.coreVersion]) {
    ensure(typeof version === 'string' && version.length <= 128, 'RELEASE_VERSION_INVALID')
    try { parseVersion(version) }
    catch { ensure(false, 'RELEASE_VERSION_INVALID') }
  }
  return { id: value.id, version: value.version, coreVersion: value.coreVersion,
    edition: 'community', operation: value.operation }
}

function validateTargets(value) {
  ensure(Array.isArray(value) && value.length > 0 && value.length <= 2
    && value.every(key => ['server', 'ui'].includes(key)) && new Set(value).size === value.length,
  'RELEASE_TARGET_INVALID')
  ensure(JSON.stringify(value) === JSON.stringify([...value].sort()), 'RELEASE_TARGET_INVALID')
  return [...value]
}

function validateArtifacts(value) {
  ensure(Array.isArray(value) && value.length > 0 && value.length <= 4096, 'RELEASE_ARTIFACTS_INVALID')
  let total = 0
  const files = value.map(file => {
    exactKeys(file, ['path', 'bytes', 'sha256'])
    ensure(Number.isSafeInteger(file.bytes) && file.bytes > 0 && file.bytes <= 512 * 1024 * 1024
      && matches(digestPattern, file.sha256), 'RELEASE_ARTIFACTS_INVALID')
    total += file.bytes
    return { path: file.path, bytes: file.bytes, sha256: file.sha256 }
  })
  ensure(total <= 1024 * 1024 * 1024, 'ARTIFACT_TOTAL_LIMIT')
  validateFileNames(files.map(file => file.path), 4096)
  ensure(files.every((file, index) => index === 0 || files[index - 1].path < file.path), 'ARTIFACT_ORDER_INVALID')
  return files
}

export const targetProfile = manifest => Object.fromEntries(manifest.targets.map(key => [key, true]))
export const releaseId = manifest => `rel-${sha256(serialize(manifest))}`
