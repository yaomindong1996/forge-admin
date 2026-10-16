import { describe, expect, it } from 'vitest'
import { syncGridLayoutWithModel } from '../page-schema-layout'

describe('sanitizeBlockEvents via syncGridLayoutWithModel', () => {
  it('preserves confirmText and runtime event fields on save normalize', () => {
    const layout = {
      cols: 12,
      items: [{
        id: 'action-button_test',
        blockType: 'action-button',
        gridX: 0,
        gridY: 0,
        gridW: 2,
        gridH: 1,
        props: {
          text: '测试',
          events: [{
            id: 'evt_1',
            trigger: 'click',
            action: 'message',
            confirmText: '确认执行？',
            requestUrl: 'post@/demo',
            permissionCode: 'demo:run',
            displayCondition: '1==1',
            successBehavior: 'refreshList',
            handler: 'refreshList',
            description: 'desc',
            params: [{ name: '', value: 'OK', sourceType: 'static', sourceField: '' }],
          }],
        },
      }],
    }

    const next = syncGridLayoutWithModel(layout, { fields: [] }, { layoutType: 'simple-crud' })
    const event = next.items.find(item => item.id === 'action-button_test')?.props?.events?.[0]

    expect(event?.confirmText).toBe('确认执行？')
    expect(event?.requestUrl).toBe('post@/demo')
    expect(event?.permissionCode).toBe('demo:run')
    expect(event?.displayCondition).toBe('1==1')
    expect(event?.successBehavior).toBe('refreshList')
    expect(event?.handler).toBe('refreshList')
    expect(event?.params?.[0]?.value).toBe('OK')
  })

  it('coerces array-like events object and keeps message fields', () => {
    const layout = {
      cols: 12,
      items: [{
        id: 'link_test',
        blockType: 'link',
        gridX: 0,
        gridY: 0,
        gridW: 2,
        gridH: 1,
        props: {
          text: 'E2E_link',
          events: {
            0: {
              trigger: 'click',
              action: 'message',
              message: 'EVT_LIST_link_OK',
              messageType: 'success',
            },
            1: {},
          },
        },
      }],
    }

    const next = syncGridLayoutWithModel(layout, { fields: [] }, { layoutType: 'simple-crud' })
    const events = next.items.find(item => item.id === 'link_test')?.props?.events
    expect(Array.isArray(events)).toBe(true)
    expect(events).toHaveLength(1)
    expect(events[0]?.message).toBe('EVT_LIST_link_OK')
    expect(events[0]?.messageType).toBe('success')
    expect(events[0]?.action).toBe('message')
  })
})
