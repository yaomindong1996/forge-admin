const BAR_WIDTH = 24
const TRANSFORM_HALF_LIFE = 2800

export function formatProgressBar(percent) {
  const pct = Math.min(100, Math.max(0, Math.round(percent)))
  const filled = Math.round((BAR_WIDTH * pct) / 100)
  return `${'█'.repeat(filled)}${'░'.repeat(BAR_WIDTH - filled)} ${String(pct).padStart(3)}%`
}

export function estimateTransformPercent(transformed) {
  return Math.min(90, Math.max(1, 90 * (1 - Math.exp(-transformed / TRANSFORM_HALF_LIFE))))
}

export function pluginBuildProgress(options = {}) {
  const out = options.stdout || process.stdout
  const isTty = () => options.isTTY ?? Boolean(out.isTTY)
  const state = {
    transformed: 0,
    rendered: 0,
    lastShown: -1,
    lastAt: 0,
    stage: 'transform',
  }

  function percent() {
    if (state.stage === 'transform')
      return estimateTransformPercent(state.transformed)
    if (state.stage === 'render')
      return Math.min(98, 90 + Math.min(8, state.rendered / 40))
    return 100
  }

  function label() {
    if (state.stage === 'transform')
      return `transforming ${state.transformed} modules`
    if (state.stage === 'render')
      return `rendering ${state.rendered} chunks`
    return 'build complete'
  }

  function show(force = false) {
    const now = Date.now()
    const rounded = Math.round(percent())
    if (!force && rounded === state.lastShown && now - state.lastAt < 400)
      return
    state.lastShown = rounded
    state.lastAt = now
    const line = `  ${formatProgressBar(rounded)}  ${label()}`
    if (isTty() && !force && state.stage !== 'done') {
      out.write(`\r${line}`)
      return
    }
    if (isTty() && force)
      out.write('\r')
    out.write(`${line}\n`)
  }

  return {
    name: 'forge-build-progress',
    apply: 'build',
    buildStart() {
      state.transformed = 0
      state.rendered = 0
      state.lastShown = -1
      state.lastAt = 0
      state.stage = 'transform'
      out.write('\n')
    },
    transform() {
      state.transformed += 1
      show()
    },
    buildEnd() {
      show(true)
    },
    renderStart() {
      state.stage = 'render'
      show(true)
    },
    renderChunk() {
      state.rendered += 1
      show()
    },
    closeBundle() {
      state.stage = 'done'
      show(true)
    },
  }
}
