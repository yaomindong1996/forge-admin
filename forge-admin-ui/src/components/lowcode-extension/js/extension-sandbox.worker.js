import { validateClientScript } from './extension-context-api.js'

const sendToHost = globalThis.postMessage.bind(globalThis)
const compileScript = globalThis.Function
let activeNonce = null
let currentStage = 'BOOTSTRAP'
const blockedGlobals = [
  'fetch',
  'XMLHttpRequest',
  'WebSocket',
  'WebTransport',
  'EventSource',
  'BroadcastChannel',
  'MessageChannel',
  'MessagePort',
  'importScripts',
  'indexedDB',
  'caches',
  'navigator',
  'location',
  'setTimeout',
  'setInterval',
  'queueMicrotask',
  'close',
  'Worker',
  'SharedWorker',
  'Function',
  'eval',
  'Promise',
  'WebAssembly',
  'SharedArrayBuffer',
  'Atomics',
]

globalThis.addEventListener('message', (event) => {
  const request = event.data || {}
  if (request.type !== 'EXECUTE_EXTENSION')
    return
  const nonce = request.nonce
  activeNonce = nonce
  try {
    currentStage = 'VALIDATE'
    validateClientScript(request.script)
    currentStage = 'HARDEN'
    hardenWorkerGlobal()
    currentStage = 'EXECUTE'
    const result = executeScript(request.script, request.context || {})
    currentStage = 'RESPONSE'
    sendToHost({ nonce, ok: true, result, stage: currentStage })
  }
  catch (error) {
    sendToHost({
      nonce,
      ok: false,
      stage: currentStage,
      error: normalizeError(error, currentStage),
    })
  }
  finally {
    activeNonce = null
    currentStage = 'IDLE'
  }
})

globalThis.addEventListener('error', (event) => {
  sendFatal(event.error || event.message)
})

globalThis.addEventListener('unhandledrejection', (event) => {
  event.preventDefault?.()
  sendFatal(event.reason)
})

sendToHost({
  type: 'EXTENSION_WORKER_READY',
  protocol: 'forge-extension-worker-v1',
})

function executeScript(source, context) {
  const fields = { ...(context.fields || {}) }
  const allowedFields = new Set(context.allowedFields || [])
  const allowedActions = new Set(context.allowedActions || [])
  const effects = []

  const api = Object.freeze({
    readField(field) {
      assertAllowedCode(field, allowedFields, '字段')
      return clone(fields[field])
    },
    setField(field, value) {
      assertAllowedCode(field, allowedFields, '字段')
      const safeValue = clone(value)
      fields[field] = safeValue
      effects.push({ type: 'SET_FIELD', field, value: safeValue })
    },
    showMessage(message, level = 'info') {
      const safeLevel = ['info', 'success', 'warning', 'error'].includes(level) ? level : 'info'
      effects.push({
        type: 'SHOW_MESSAGE',
        message: String(message || '').slice(0, 500),
        level: safeLevel,
      })
    },
    triggerAction(actionCode, payload = {}) {
      assertAllowedCode(actionCode, allowedActions, '动作')
      effects.push({
        type: 'TRIGGER_ACTION',
        actionCode,
        payload: clone(payload),
      })
    },
  })

  const runner = Reflect.construct(compileScript, ['api', `
    "use strict";
    const { readField, setField, showMessage, triggerAction } = api;
    return (function runExtension() {
      ${source}
    }).call(undefined);
  `])
  const output = runner(api)
  if (output && typeof output.then === 'function')
    throw new Error('客户端扩展只允许同步执行')

  return {
    output: clone(output),
    effects,
  }
}

function hardenWorkerGlobal() {
  removeConstructor(Function.prototype)
  removeConstructor(Object.prototype)
  removeConstructor(Array.prototype)
  removeConstructor(Object.getPrototypeOf(async () => {}))
  removeConstructor(Object.getPrototypeOf(function* isolatedGeneratorPrototype() {}))
  for (const name of blockedGlobals) {
    try {
      Object.defineProperty(globalThis, name, {
        configurable: false,
        enumerable: false,
        writable: false,
        value: undefined,
      })
    }
    catch {
      // 不可重定义的能力仍会被静态策略拒绝，Worker 也会在超时后被宿主终止。
    }
  }
  // postMessage 保留给 Worker 协议通信；存储脚本由前后端静态策略禁止引用该标识符。
  Object.freeze(Object.prototype)
  Object.freeze(Array.prototype)
}

function removeConstructor(prototype) {
  try {
    Object.defineProperty(prototype, 'constructor', {
      configurable: false,
      enumerable: false,
      writable: false,
      value: undefined,
    })
  }
  catch {
    // Worker 仍由宿主超时终止；静态策略同时拒绝 constructor 和转义标识符。
  }
}

function assertAllowedCode(code, allowlist, label) {
  const value = String(code || '')
  if (!allowlist.has(value))
    throw new Error(`未授权${label}: ${value}`)
}

function clone(value) {
  if (value === undefined)
    return null
  return structuredClone(value)
}

function sendFatal(error) {
  try {
    sendToHost({
      type: 'EXTENSION_WORKER_FATAL',
      nonce: activeNonce,
      stage: currentStage,
      error: normalizeError(error, currentStage),
    })
  }
  catch {
    // 宿主仍会收到浏览器 ErrorEvent，并展示文件和行列位置。
  }
}

function normalizeError(error, stage) {
  const message = error instanceof Error ? error.message : String(error || '')
  const safeMessage = message
    .replace(/(token|secret|password|cookie|authorization)\S*/gi, '[REDACTED]')
    .slice(0, 420)
  return stageLabel(stage) + '：' + (safeMessage || '未知错误')
}

function stageLabel(stage) {
  return {
    BOOTSTRAP: 'Worker 模块初始化阶段',
    VALIDATE: '脚本安全校验阶段',
    HARDEN: '沙箱环境初始化阶段',
    EXECUTE: '脚本运行阶段',
    RESPONSE: '结果返回阶段',
  }[stage] || 'Worker 未知阶段'
}
