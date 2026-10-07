import fs from 'node:fs/promises'
import path from 'node:path'
import { requireCondition, safeTarget, statOptional } from './paths.mjs'
import { verifyChecks } from './ownership.mjs'

export async function transact(context, plan, prepare, hooks = {}) {
  const meta = await safeTarget(context.root, '.forge-plugin')
  await fs.mkdir(meta, { recursive: true })
  const lock = await safeTarget(context.root, '.forge-plugin/lock')
  let handle
  try {
    handle = await fs.open(lock, 'wx')
  }
  catch (error) {
    throw new Error(`工程安装锁未能获取，请检查是否有其他安装或遗留锁：${lock}`, { cause: error })
  }
  let work
  let failure
  const state = { operations: [], parents: [], applied: [], warnings: [] }
  let lockIdentity
  try {
    lockIdentity = await handle.stat()
    await handle.writeFile(JSON.stringify({ pid: process.pid, time: new Date().toISOString() }))
    await protectInternalFiles(context.root)
    await verifyOriginals(context)
    await verifyChecks(context.root, plan.checks)
    work = await fs.mkdtemp(path.join(meta, 'work-'))
    const prepared = await prepare(work)
    state.operations = prepared.operations
    await stageHostFiles(work, state.operations, prepared.files)
    for (const operation of state.operations.filter(item => item.host)) {
      operation.expected = context.files[operation.relative]
    }
    const paths = state.operations.map((operation, index) => ({ relative: operation.relative, backup: `old-${index}` }))
    const recovery = JSON.stringify({ id: plan.id, files: context.files, paths }, null, 2)
    await fs.writeFile(path.join(work, 'restore.json'), recovery)
    for (const [index, operation] of state.operations.entries()) {
      await hooks.beforeWrite?.({ index, relative: operation.relative })
      await verifyChecks(context.root, plan.checks.filter(check => check.relative === operation.relative))
      await applyOperation(context.root, work, operation, state)
      await hooks.afterWrite?.({ index, relative: operation.relative })
    }
    // 首装也保留旧 POM/config；提交后清理失败不能触发一个缺少原件的半回滚。
    const backup = await retainBackup(context.root, work, plan.id)
    return { ...prepared.result, backup, warnings: state.warnings }
  }
  catch (error) {
    const failures = await rollback(context.root, state)
    const suffix = failures.length ? `；回滚异常：${failures.join('；')}` : '；已恢复原源码/POM/配置'
    failure = new Error(`${error.message}${suffix}${work ? `；恢复暂存：${work}` : ''}`, { cause: error })
    throw failure
  }
  finally {
    const warning = await releaseLock(handle, lock, lockIdentity)
    if (warning) {
      state.warnings.push(warning)
      if (failure) {
        failure.message += `；${warning}`
      }
    }
  }
}

async function releaseLock(handle, lock, identity) {
  try {
    await handle.close()
    const current = await statOptional(lock)
    requireCondition(current && identity && current.ino === identity.ino && current.dev === identity.dev,
      '安装锁在执行过程中被替换/移除；未删除他人锁，请人工检查')
    await fs.unlink(lock)
    return null
  }
  catch (error) {
    return `安装锁释放异常：${error.message}`
  }
}

async function protectInternalFiles(root) {
  // 恢复副本可能含客户定制代码；只忽略工具自身目录，不提前实现 T10 的插件版次门禁。
  const file = await safeTarget(root, '.forge-plugin/.gitignore')
  if (!await statOptional(file)) {
    await fs.writeFile(file, '*\n', { flag: 'wx' })
  }
  else {
    requireCondition(await fs.readFile(file, 'utf8') === '*\n', '安装恢复目录的忽略规则已变更，请人工检查')
  }
}

async function verifyOriginals(context) {
  for (const [relative, expected] of Object.entries(context.files)) {
    const target = await safeTarget(context.root, relative)
    const actual = await statOptional(target) ? await fs.readFile(target, 'utf8') : null
    requireCondition(actual === expected, `宿主文件在预检后发生变化：${relative}`)
  }
}

async function stageHostFiles(work, operations, files) {
  for (const [relative, text] of Object.entries(files)) {
    const stage = path.join(work, `host-${operations.length}`)
    await fs.writeFile(stage, text, { flag: 'wx' })
    operations.push({ relative, stage, host: true })
  }
}

async function ensureParents(root, relative, state) {
  const parts = relative.split('/').slice(0, -1)
  for (let index = 1; index <= parts.length; index++) {
    const parent = await safeTarget(root, parts.slice(0, index).join('/'))
    if (!await statOptional(parent)) {
      await fs.mkdir(parent)
      state.parents.push(parent)
    }
  }
}

async function applyOperation(root, work, operation, state) {
  const target = await safeTarget(root, operation.relative, !!operation.allowLink)
  const existing = await statOptional(target)
  requireCondition(!existing || operation.replace || operation.host, `目标目录在预检后被占用：${operation.relative}`)
  if (operation.host) {
    const actual = existing ? await fs.readFile(target, 'utf8') : null
    requireCondition(actual === operation.expected, `宿主文件在写入前发生变化：${operation.relative}`)
  }
  await ensureParents(root, operation.relative, state)
  const action = { ...operation, target, backup: path.join(work, `old-${state.applied.length}`) }
  state.applied.push(action)
  if (existing) {
    await fs.rename(target, action.backup)
    action.moved = true
  }
  if (operation.link) {
    await fs.symlink(operation.link, target, 'dir')
    action.installed = true
  }
  else if (operation.stage) {
    await fs.rename(operation.stage, target)
    action.installed = true
  }
}

async function rollback(root, state) {
  const failures = []
  for (const action of [...state.applied].reverse()) {
    try {
      await safeTarget(root, action.relative, !!action.link || !!action.allowLink)
      if (action.installed) {
        await fs.rename(action.target, `${action.backup}-new`)
      }
      if (action.moved) {
        await fs.rename(action.backup, action.target)
      }
    }
    catch (error) {
      failures.push(`${action.relative}: ${error.message}`)
    }
  }
  for (const parent of [...state.parents].reverse()) {
    try {
      await fs.rmdir(parent)
    }
    catch (error) {
      // 只清理本次创建的空目录；并发新增的用户文件不删除。
      if (!['ENOTEMPTY', 'EEXIST'].includes(error.code)) {
        failures.push(`${parent}: ${error.message}`)
      }
    }
  }
  return failures
}

async function retainBackup(root, work, id) {
  const directory = await safeTarget(root, `.forge-plugin/backups/${id}`)
  await fs.mkdir(directory, { recursive: true })
  const destination = path.join(directory, path.basename(work))
  await fs.rename(work, destination)
  return destination
}
