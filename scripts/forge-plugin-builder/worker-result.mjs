import { createHash } from 'node:crypto'
import { ensure } from './errors.mjs'

// 容器产物的实际核验由 collectArtifacts 完成，这里只回写摘要，绝不回写私有路径/源码/日志。
export function reportResult(result, config, phase) {
  ensure(result?.deployed === false && ['built', 'failed'].includes(result.status)
    && /^job-[a-zA-Z0-9_-]{1,64}$/.test(result.jobId), 'WORKER_RESULT_INVALID')
  const report = { success: result.status === 'built', jobId: result.jobId,
    packageSha256: config.packageSha256, sourceCommit: config.commit, image: config.image,
    phase: result.status === 'built' ? 'artifact_verification' : (result.failurePhase || phase),
    sourceSha256: result.source?.sha256 || null }
  if (report.success) {
    ensure(Array.isArray(result.artifacts) && result.artifacts.length > 0, 'WORKER_ARTIFACTS_MISSING')
    report.artifactCount = result.artifacts.length
    report.artifactBytes = result.artifacts.reduce((total, file) => total + file.bytes, 0)
    const manifest = result.artifacts.map(file => `${file.path}:${file.bytes}:${file.sha256}\n`).join('')
    report.artifactManifestSha256 = createHash('sha256').update(manifest).digest('hex')
    report.failureCode = null
  }
  else {
    report.failureCode = result.failureCode || 'PREFLIGHT_FAILED'
    report.artifactCount = null
    report.artifactBytes = null
    report.artifactManifestSha256 = null
  }
  return report
}
