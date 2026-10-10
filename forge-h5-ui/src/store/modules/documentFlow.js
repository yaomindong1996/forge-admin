import { defineStore } from 'pinia'
import {
  DOCUMENT_FLOW_ACTION,
  buildDocumentFlowButtons,
  resolveDocumentFlowTone,
  resolveDocumentStatusText,
} from '../../utils/document-flow-actions.js'

export const CREATE_SUBMIT_KEY = 'CREATE_SUBMIT'

export const useDocumentFlowStore = defineStore('documentFlow', {
  state: () => ({
    runtime: null,
    loadingKey: '',
    // 仅从「发起审批」进入新建页时展示提交审批；其它入口新建后在详情页按运行态提交。
    createSubmit: false,
    // 键与 FLOW_PERMISSIONS 一致，由 useLowcodeDocumentFlow 按当前用户权限写入。
    permissions: { start: false, withdraw: false },
  }),
  getters: {
    visible: state => Boolean(state.runtime),
    actionState: state => buildDocumentFlowButtons(state.runtime, key => state.permissions[key] === true),
    buttons() { return this.actionState.buttons },
    hint() { return this.actionState.hint },
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
        if (!this.createSubmit || !this.permissions.start) return []
        return [{
          key: CREATE_SUBMIT_KEY,
          actionType: DOCUMENT_FLOW_ACTION.START,
          label: '提交审批',
          variant: 'primary',
          disabled: false,
          disabledReason: '',
        }]
      }
    },
  },
  actions: {
    setRuntime(runtime) {
      this.runtime = runtime && runtime.documentEnabled !== false ? runtime : null
    },
    reset() {
      this.runtime = null
      this.loadingKey = ''
    },
  },
})
