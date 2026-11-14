import { defineStore } from 'pinia'
import {
  DOCUMENT_FLOW_ACTION,
  buildDocumentFlowButtons,
  buildProcessStartButtons,
  resolveDocumentFlowTone,
  resolveDocumentStatusText,
} from '../../utils/document-flow-actions.js'

export const CREATE_SUBMIT_KEY = 'CREATE_SUBMIT'

const EMPTY_PROCESS_STATE = Object.freeze({ activeProcessCodes: [], startedProcessCodes: [] })

export const useDocumentFlowStore = defineStore('documentFlow', {
  state: () => ({
    runtime: null,
    loadingKey: '',
    // 仅从「发起审批」进入新建页时展示提交审批；其它入口新建后在详情页按运行态提交。
    createSubmit: false,
    // 从「发起审批」目录的业务流程条目进入新建页时，只展示该流程的发起按钮。
    createProcessCode: '',
    // 键与 FLOW_PERMISSIONS 一致，由 useLowcodeDocumentFlow 按当前用户权限写入。
    permissions: { start: false, withdraw: false, processStart: false },
    // 渲染配置投影的业务流程启动动作，已按当前记录过滤显示条件
    processActions: [],
    // 非单据对象也要保留，用于隐藏已运行或已发起的业务流程按钮
    processState: { ...EMPTY_PROCESS_STATE },
  }),
  getters: {
    visible: state => Boolean(state.runtime),
    actionState: state => buildDocumentFlowButtons(state.runtime, key => state.permissions[key] === true),
    processButtonState: state => buildProcessStartButtons(state.processActions, state.processState, {
      canStart: state.permissions.processStart === true,
    }),
    buttons() { return [...this.actionState.buttons, ...this.processButtonState.buttons] },
    hint() { return this.actionState.hint || this.processButtonState.hint },
    statusText: state => resolveDocumentStatusText(state.runtime),
    statusTone: state => resolveDocumentFlowTone(state.runtime?.flowStatus),
    message: state => (state.runtime?.message && state.runtime.message !== state.runtime.documentStatusLabel
      ? state.runtime.message
      : ''),
    disabledReason() {
      return this.buttons.find(button => button.disabled && button.disabledReason)?.disabledReason || ''
    },
    footerButtons() {
      return mode => {
        if (mode !== 'create') return this.buttons
        const processButtons = this.createProcessCode
          ? this.processButtonState.buttons.filter(button => button.processCode === this.createProcessCode)
          : []
        if (!this.createSubmit) return processButtons
        const createButtons = this.permissions.start
          ? [{
              key: CREATE_SUBMIT_KEY,
              actionType: DOCUMENT_FLOW_ACTION.START,
              label: '提交审批',
              variant: 'primary',
              disabled: false,
              disabledReason: '',
            }]
          : []
        return [...createButtons, ...processButtons]
      }
    },
  },
  actions: {
    setRuntime(runtime) {
      this.runtime = runtime && runtime.documentEnabled !== false ? runtime : null
      this.processState = {
        activeProcessCodes: Array.isArray(runtime?.activeProcessCodes) ? runtime.activeProcessCodes : [],
        startedProcessCodes: Array.isArray(runtime?.startedProcessCodes) ? runtime.startedProcessCodes : [],
      }
    },
    setProcessActions(actions) {
      this.processActions = Array.isArray(actions) ? actions : []
    },
    reset() {
      this.runtime = null
      this.loadingKey = ''
      this.processActions = []
      this.processState = { ...EMPTY_PROCESS_STATE }
    },
  },
})
