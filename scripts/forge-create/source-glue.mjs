import fs from 'node:fs/promises'
import path from 'node:path'

const applicationIntegrationDependencies = [
  'forge-plugin-generator',
  'forge-plugin-capability-platform',
  'forge-plugin-capability-actions',
]

// 仅用于新生成的 Admin 工程；可选模块缺失时不能保留引用它们的组合接入层。
export async function pruneOptionalAdminGlue(adminServerRoot, selectedArtifacts) {
  if (applicationIntegrationDependencies.every(artifact => selectedArtifacts.has(artifact))) return

  const relativePaths = [
    'src/main/java/com/mdframe/forge/admin/integration',
    'src/test/java/com/mdframe/forge/admin/integration',
    'src/main/resources/mapper/ApplicationIntegrationMapper.xml',
  ]
  for (const relativePath of relativePaths) {
    await fs.rm(path.join(adminServerRoot, relativePath), { recursive: true, force: true })
  }
}
