import path from 'node:path'
import { safeTarget } from '../forge-plugin/paths.mjs'
import { sha256 } from '../forge-plugin-builder/files.mjs'
import { collectArtifacts } from '../forge-plugin-builder/artifacts.mjs'
import { ensure } from '../forge-plugin-builder/errors.mjs'
import { readJson } from './config.mjs'
import { buildManifest, metadataLimit, targetProfile } from './manifest.mjs'

export async function inspectRelease(config, signal) {
  ensure(!signal?.aborted, 'INTERRUPTED')
  const resultFile = await safeTarget(config.jobRoot, 'result.json')
  const { value, data } = await readJson(resultFile, metadataLimit)
  ensure(sha256(data) === config.resultSha256, 'BUILD_RESULT_MISMATCH')
  const manifest = buildManifest(value, config)
  ensure(path.basename(config.jobRoot) === manifest.jobId, 'BUILD_JOB_MISMATCH')
  const root = await safeTarget(config.jobRoot, 'artifacts')
  const actual = await collectArtifacts(root, targetProfile(manifest), signal)
  ensure(JSON.stringify(actual) === JSON.stringify(manifest.artifacts), 'BUILD_ARTIFACT_MISMATCH')
  await assertResultUnchanged(config)
  return manifest
}

export async function assertResultUnchanged(config) {
  const { data } = await readJson(await safeTarget(config.jobRoot, 'result.json'), metadataLimit)
  ensure(sha256(data) === config.resultSha256, 'BUILD_RESULT_MISMATCH')
}
