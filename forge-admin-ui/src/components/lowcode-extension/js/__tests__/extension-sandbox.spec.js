import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  sanitizeExtensionContext,
  validateClientScript,
  validateSandboxResult,
} from '../extension-context-api'
import ExtensionSandboxHost from '../ExtensionSandboxHost.vue'

describe('client extension sandbox policy', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it.each([
    ['window.location', 'window'],
    ['document.cookie', 'document'],
    ['localStorage.getItem("token")', 'localStorage'],
    ['fetch("https://example.com")', 'fetch'],
    ['new WebSocket("wss://example.com")', 'WebSocket'],
    ['eval("1 + 1")', 'eval'],
    ['new Function("return 1")', 'Function'],
    ['postMessage({ leaked: true })', 'postMessage'],
    ['({}).constructor.constructor("return self")()', 'constructor'],
    ['Object.prototype.polluted = true', 'prototype'],
    ['`$' + '{import("https://example.com/x.js")}' + '`', '模板字符串'],
    ['Object.constr\\u0075ctor("return 1")()', '转义标识符'],
  ])('rejects %s', (script, keyword) => {
    expect(() => validateClientScript(script)).toThrow(keyword)
  })

  it('allows only the documented synchronous context API', () => {
    const source = `
      const amount = readField('amount')
      if (amount > 1000) {
        setField('level', 'HIGH')
        showMessage('已按金额标记等级', 'info')
        triggerAction('recalculate', { amount })
      }
      return { amount }
    `

    expect(validateClientScript(source)).toEqual({ valid: true })
  })

  it('removes token, secrets and fields outside the allowlist from context', () => {
    const context = sanitizeExtensionContext({
      token: 'bearer-secret',
      cookies: 'session-secret',
      record: {
        id: 8,
        amount: 1200,
        idCard: 'sensitive',
      },
      allowedActions: ['recalculate'],
    }, ['amount'])

    expect(context).toEqual({
      recordId: 8,
      fields: { amount: 1200 },
      allowedFields: ['amount'],
      allowedActions: ['recalculate'],
    })
  })

  it('rejects oversized or protocol-invalid worker results', () => {
    expect(() => validateSandboxResult({ effects: [{ type: 'RAW_HTML' }] }, 1024)).toThrow('结果协议')
    expect(() => validateSandboxResult({ output: 'x'.repeat(2048), effects: [] }, 1024)).toThrow('输出')
  })

  it('keeps dynamic script execution out of the main-page host', () => {
    const hostPath = resolve('src/components/lowcode-extension/js/ExtensionSandboxHost.vue')
    const hostSource = readFileSync(hostPath, 'utf8')

    expect(hostSource).not.toMatch(/\beval\s*\(/)
    expect(hostSource).not.toMatch(/\bnew\s+Function\s*\(/)
    expect(hostSource).not.toMatch(/URL\.createObjectURL|blob:/)
  })

  it('terminates a non-responsive worker when execution exceeds the deadline', async () => {
    vi.useFakeTimers()
    const listeners = {}
    const worker = {
      addEventListener: vi.fn((type, listener) => {
        listeners[type] = listener
      }),
      postMessage: vi.fn(),
      terminate: vi.fn(),
    }
    vi.stubGlobal('Worker', vi.fn(() => worker))
    const wrapper = mount(ExtensionSandboxHost, { props: { timeoutMs: 50 } })

    const execution = wrapper.vm.execute('return { ok: true }', {}, [])
    const assertion = expect(execution).rejects.toThrow('超时')
    listeners.message({ data: { type: 'EXTENSION_WORKER_READY' } })
    await vi.advanceTimersByTimeAsync(60)

    await assertion
    expect(worker.terminate).toHaveBeenCalledOnce()
  })

  it('reports the browser worker error message and safe source position', async () => {
    const listeners = {}
    const worker = {
      addEventListener: vi.fn((type, listener) => {
        listeners[type] = listener
      }),
      postMessage: vi.fn(),
      terminate: vi.fn(),
    }
    vi.stubGlobal('Worker', vi.fn(() => worker))
    const wrapper = mount(ExtensionSandboxHost)

    const execution = wrapper.vm.execute('return { ok: true }', {}, [])
    const assertion = expect(execution).rejects.toThrow(
      'Failed to load module script（extension-sandbox.worker.js:12:3）',
    )
    listeners.error({
      message: 'Failed to load module script',
      filename: 'http://localhost:3000/src/extension-sandbox.worker.js?v=secret',
      lineno: 12,
      colno: 3,
      preventDefault: vi.fn(),
    })

    await assertion
    expect(worker.terminate).toHaveBeenCalledOnce()
  })
})
