let activeHost = null
let hostSequence = 0
const pendingTasks = []

export function registerFeedbackHost(controller) {
  const token = ++hostSequence
  activeHost = { token, controller }
  while (pendingTasks.length) {
    const task = pendingTasks.shift()
    Promise.resolve()
      .then(() => task.callback(controller))
      .then(task.resolve, task.reject)
  }

  return () => {
    if (activeHost?.token === token) {
      activeHost = null
    }
  }
}

export function getFeedbackHost() {
  return activeHost?.controller || null
}

export function runWithFeedbackHost(callback) {
  const controller = getFeedbackHost()
  if (controller) {
    return Promise.resolve().then(() => callback(controller))
  }
  return new Promise((resolve, reject) => {
    pendingTasks.push({ callback, resolve, reject })
  })
}
