import { ensure } from './errors.mjs'

export function maintainLease(call, abort, signal, interval = 30000) {
  let phase = 'source_snapshot'
  let stopped = false
  let pending = Promise.resolve()
  let timer
  const renew = () => {
    pending = pending.then(async () => {
      ensure(!stopped && !signal.aborted, 'WORKER_LEASE_LOST')
      const value = await call('heartbeat', { phase }, signal)
      ensure(value.status === 'building' && value.leaseSeconds === 90, 'WORKER_LEASE_LOST')
    }).catch((error) => { abort(); throw error })
    return pending
  }
  const tick = () => {
    renew().then(() => { if (!stopped) timer = setTimeout(tick, interval) }).catch(() => {})
  }
  timer = setTimeout(tick, interval)
  return {
    get phase() { return phase },
    async advance(value) { phase = value; await renew() },
    async stop() {
      clearTimeout(timer)
      try { await pending }
      catch { /* 失联已经触发中止；保留本地结果，最终回写若被拒绝不能冒充已完成。 */ }
      stopped = true
      clearTimeout(timer)
    },
  }
}
