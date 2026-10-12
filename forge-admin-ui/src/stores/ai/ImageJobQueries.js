import { imageGenerateGetResult } from '@/api/ai/media'
import { imageJobResult, imageRecordId, isImageJobTerminal } from './image-job-utils'

const POLL_INTERVAL_MS = 2000
const QUERY_WINDOW_MS = 5 * 60 * 1000
const MAX_CACHED_JOBS = 64

// 独立查询生命周期：只接受已有权限的 getter，不持有 Token、用户资料或业务提示词。
export class ImageJobQueries {
  constructor(jobs, allowed) {
    this.jobs = jobs
    this.allowed = allowed
    this.runs = new Map()
  }

  stop(run) {
    clearTimeout(run.timer)
    run.controller?.abort()
    if (this.runs.get(run.id) === run) {
      this.runs.delete(run.id)
      const job = this.jobs.get(run.id)
      if (job) {
        job.loading = false
      }
    }
  }

  reset() {
    for (const run of this.runs.values()) {
      this.stop(run)
    }
    this.jobs.clear()
  }

  ensure(id) {
    if (this.jobs.has(id)) {
      return this.jobs.get(id)
    }
    if (this.jobs.size >= MAX_CACHED_JOBS) {
      const removable = [...this.jobs.keys()].find(key => !this.runs.has(key))
      if (!removable) {
        throw new Error('当前查询任务过多，请关闭部分任务后重试')
      }
      this.jobs.delete(removable)
    }
    this.jobs.set(id, { result: { id, status: 'pending' }, loading: false, paused: false, error: '' })
    return this.jobs.get(id)
  }

  current(run) {
    return this.allowed() && this.runs.get(run.id) === run && run.subscribers.size > 0
  }

  schedule(run, delay = POLL_INTERVAL_MS) {
    clearTimeout(run.timer)
    run.timer = setTimeout(() => this.poll(run), delay)
  }

  pause(run, message) {
    const job = this.jobs.get(run.id)
    job.paused = true
    job.error = message
    clearTimeout(run.timer)
    run.timer = null
  }

  async poll(run) {
    run.timer = null
    if (!this.current(run) || run.controller) {
      return
    }
    const job = this.jobs.get(run.id)
    run.controller = new AbortController()
    job.loading = true
    try {
      const response = await imageGenerateGetResult(run.id, {
        signal: run.controller.signal,
        timeout: 10000,
        needTip: false,
      })
      if (!this.current(run)) {
        return
      }
      if (response?.code !== 200) {
        throw new Error('图片任务查询失败')
      }
      job.result = imageJobResult(response.data, run.id)
      if (!isImageJobTerminal(job.result)) {
        this.continueQuery(run)
      }
    }
    catch (error) {
      if (this.current(run) && error?.name !== 'AbortError') {
        this.pause(run, '图片结果查询已暂停，任务可能仍在执行，请稍后重新查询')
      }
    }
    finally {
      if (this.current(run)) {
        run.controller = null
        job.loading = false
      }
    }
  }

  continueQuery(run) {
    if (Date.now() - run.startedAt >= QUERY_WINDOW_MS) {
      this.pause(run, '等待结果超过 5 分钟，已暂停查询；可稍后重新查询，不会再次生成图片')
      return
    }
    this.schedule(run)
  }

  subscribe(value) {
    const id = imageRecordId(value)
    if (!id || !this.allowed()) {
      return () => {}
    }
    const job = this.ensure(id)
    let run = this.runs.get(id)
    if (!run) {
      run = { id, subscribers: new Set(), controller: null, timer: null, startedAt: Date.now() }
      this.runs.set(id, run)
    }
    const subscriber = Symbol('image-job-view')
    run.subscribers.add(subscriber)
    if (!job.paused && !isImageJobTerminal(job.result) && !run.controller && !run.timer) {
      this.schedule(run, 0)
    }
    return () => {
      run.subscribers.delete(subscriber)
      if (!run.subscribers.size) {
        this.stop(run)
      }
    }
  }

  resume(value) {
    const run = this.runs.get(imageRecordId(value))
    if (!run || !this.current(run) || run.controller) {
      return
    }
    const job = this.jobs.get(run.id)
    if (isImageJobTerminal(job.result)) {
      return
    }
    job.paused = false
    job.error = ''
    run.startedAt = Date.now()
    this.schedule(run, 0)
  }
}
