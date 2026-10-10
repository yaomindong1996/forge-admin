import { spawn } from 'node:child_process'
import { BuildError, ensure } from './errors.mjs'

// 不继承认证、数据库、JAVA_TOOL_OPTIONS、DOCKER_CONTEXT 等运维环境。
export const cleanEnvironment = Object.freeze({ PATH: '/usr/local/bin:/usr/bin:/bin', LANG: 'C.UTF-8' })
export const outputLimit = 1024 * 1024

export function runProcess(executable, args, options = {}) {
  ensure(!options.signal?.aborted, 'INTERRUPTED')
  return new Promise((resolve, reject) => {
    const child = spawn(executable, args, { cwd: options.cwd, env: options.env || cleanEnvironment,
      shell: false, detached: true, stdio: ['ignore', 'pipe', 'pipe'] })
    const state = { bytes: 0, chunks: [], failure: null }
    const kill = (code) => {
      state.failure ||= new BuildError(code)
      // 子进程组是本次 spawn 创建的，不查杀其它用户/任务的进程。
      try {
        process.kill(-child.pid, 'SIGKILL')
      }
      catch (error) {
        if (error.code !== 'ESRCH') {
          child.kill('SIGKILL')
        }
      }
    }
    const abort = () => kill('INTERRUPTED')
    const timer = setTimeout(() => kill('COMMAND_TIMEOUT'), options.timeoutMs || 3000)
    options.signal?.addEventListener('abort', abort, { once: true })
    const capture = (data, stdout) => {
      state.bytes += data.length
      if (state.bytes > (options.outputLimit || outputLimit)) {
        kill('OUTPUT_LIMIT')
        return
      }
      if (stdout) {
        state.chunks.push(data)
      }
    }
    child.stdout.on('data', data => capture(data, true))
    child.stderr.on('data', data => capture(data, false))
    child.once('error', () => { state.failure ||= new BuildError('EXECUTABLE_UNAVAILABLE') })
    child.once('close', (code) => {
      clearTimeout(timer)
      options.signal?.removeEventListener('abort', abort)
      if (state.failure || code !== 0) {
        reject(state.failure || new BuildError('COMMAND_FAILED'))
        return
      }
      resolve(Buffer.concat(state.chunks))
    })
  })
}
