export function artifactTask() {
  const result = {
    success: true,
    jobId: 'job-test',
    packageSha256: 'a'.repeat(64),
    sourceCommit: 'b'.repeat(40),
    image: `builder@sha256:${'c'.repeat(64)}`,
    phase: 'artifact_verification',
    failureCode: null,
    sourceSha256: 'd'.repeat(64),
    artifactCount: 1,
    artifactBytes: 100,
    artifactManifestSha256: 'f'.repeat(64),
  }
  return {
    id: '00000000-0000-4000-8000-00000000000a',
    status: 'release_ready',
    revision: 3,
    pluginId: 'demo',
    version: '1.0.0',
    operation: 'install',
    preview: { coreVersion: '1.2.0' },
    execution: { result, resultSha256: 'e'.repeat(64) },
    reviews: [{ id: '00000000-0000-4000-8000-00000000000b', decision: 'approve_build' }],
    artifacts: [],
  }
}

export function artifactMetadata(task = artifactTask()) {
  return {
    protocolVersion: 1,
    taskId: task.id,
    reviewId: task.reviews[0].id,
    revision: 3,
    serverResultSha256: task.execution.resultSha256,
    releaseId: `rel-${'1'.repeat(64)}`,
    manifestSha256: '1'.repeat(64),
    repositoryId: 'local-test',
    resultSha256: '2'.repeat(64),
    pluginId: 'demo',
    pluginVersion: '1.0.0',
    coreVersion: '1.2.0',
    operation: 'install',
    result: task.execution.result,
  }
}
