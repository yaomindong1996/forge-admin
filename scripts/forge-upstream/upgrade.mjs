import fs from 'node:fs/promises'
import path from 'node:path'
import { git, resolveCommit, assertUpgradeBranch, importTree, writeJson, treeEntries } from './git.mjs'
import { immutableMigration } from './policy.mjs'
import { generateSnapshot } from './source.mjs'
import { readState, statePath, generatorPath, inspectCandidate, mergePreview, gitStateDirectory } from './state.mjs'

export async function planUpgrade({ root, source, ref }) {
  assertUpgradeBranch(root)
  const { state, stateHash } = await readState(root)
  git(root, ['merge-base', '--is-ancestor', state.baseCommit, 'HEAD'])
  const commit = resolveCommit(source, ref)
  // 禁止把另一个项目、回退提交或重写后的社区历史误认为连续升级。
  git(source, ['merge-base', '--is-ancestor', state.upstream.commit, commit])
  const generated = await generateSnapshot({ source, ref: commit, options: state.options,
    generator: path.join(root, generatorPath), productAnchor: state.productAnchor })
  try {
    const directory = gitStateDirectory(root)
    await fs.mkdir(directory, { recursive: true })
    const candidate = await importTree({ root, snapshot: generated.snapshot, parent: state.baseCommit,
      message: `同步社区基线 ${generated.version}（${commit.slice(0, 12)}）`,
      indexFile: path.join(directory, 'candidate.index') })
    const inspection = inspectCandidate(root, state, candidate)
    const preview = mergePreview(root, candidate)
    const plan = { schemaVersion: 1, head: resolveCommit(root, 'HEAD'), stateHash,
      candidate, oldBase: state.baseCommit, upstream: { ...state.upstream, commit, version: generated.version },
      ...inspection, ...preview }
    // Git 私有元数据不进入交付包；候选 ref 在 commit 前防止被 GC。
    git(root, ['update-ref', 'refs/forge-upstream/candidate', candidate])
    await writeJson(path.join(directory, 'plan.json'), plan)
    return plan
  }
  finally { await fs.rm(generated.temp, { recursive: true, force: true }) }
}

export async function applyUpgrade(root) {
  assertUpgradeBranch(root)
  const { state, stateHash } = await readState(root)
  const plan = JSON.parse(await fs.readFile(path.join(gitStateDirectory(root), 'plan.json'), 'utf8'))
  if (plan.schemaVersion !== 1 || plan.head !== resolveCommit(root, 'HEAD')
    || plan.stateHash !== stateHash || plan.oldBase !== state.baseCommit) {
    throw new Error('预览已过期，请重新执行 plan')
  }
  if (resolveCommit(root, `${plan.candidate}^`) !== state.baseCommit) throw new Error('候选基线父提交不匹配')
  const inspection = inspectCandidate(root, state, plan.candidate)
  if (inspection.blocked.length) throw new Error(inspection.blocked.join('\n'))
  if (!inspection.changes.length && plan.upstream.commit === state.upstream.commit) {
    return { unchanged: true, message: '已经是该社区基线，无需合并' }
  }
  let conflicts = false
  try { git(root, ['merge', '--no-commit', '--no-ff', plan.candidate]) }
  catch (error) {
    let merging
    try { merging = git(root, ['rev-parse', '--verify', 'MERGE_HEAD']).trim() }
    catch { throw error }
    if (merging !== plan.candidate) throw error
    conflicts = true
  }
  const next = { ...state, baseCommit: plan.candidate, upstream: plan.upstream }
  await writeJson(path.join(root, statePath), next)
  git(root, ['add', '--', statePath])
  return { conflicts, message: '已准备三方合并，检查/测试后手动 git commit；取消使用 git merge --abort' }
}

export async function verifyBaseline(root) {
  const { state } = await readState(root)
  for (const [name, entry] of treeEntries(root, state.baseCommit)) {
    if (!immutableMigration(name)) continue
    const file = path.join(root, name)
    const current = await fs.readFile(file)
    const oid = git(root, ['hash-object', '--stdin'], { input: current }).trim()
    if (oid !== entry.oid) throw new Error(`历史迁移被修改：${name}`)
  }
  const unmerged = git(root, ['diff', '--name-only', '--diff-filter=U']).trim()
  if (unmerged) throw new Error(`尚有未解决冲突：\n${unmerged}`)
  return state
}
