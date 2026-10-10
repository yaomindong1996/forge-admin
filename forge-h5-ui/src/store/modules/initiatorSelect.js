import { defineStore } from 'pinia'
import { collectInitiatorSelectSelections, normalizeInitiatorSelectNodes } from '../../utils/initiator-select.js'

// Promise 的 resolve 不可序列化，放在模块级而不是 state 里。
let pendingResolve = null

function settle(value) {
  const resolve = pendingResolve
  pendingResolve = null
  resolve?.(value)
}

export const useInitiatorSelectStore = defineStore('initiatorSelect', {
  state: () => ({
    visible: false,
    nodes: [],
    // nodeKey -> [{ userId, realName, avatar }]
    selections: {},
    activeNodeKey: '',
  }),
  getters: {
    activeNode: state => state.nodes.find(node => node.nodeKey === state.activeNodeKey) || null,
    isComplete: state => state.nodes.every(node => (state.selections[node.nodeKey] || []).length > 0),
  },
  actions: {
    /** 打开选人面板；确认时 resolve { nodeKey: [userId] }，取消时 resolve null。 */
    open(nodes) {
      settle(null)
      this.nodes = normalizeInitiatorSelectNodes(nodes)
      this.selections = Object.fromEntries(this.nodes.map(node => [node.nodeKey, []]))
      this.activeNodeKey = ''
      this.visible = true
      return new Promise((resolve) => { pendingResolve = resolve })
    },
    pick(nodeKey) {
      this.activeNodeKey = nodeKey
    },
    back() {
      this.activeNodeKey = ''
    },
    isSelected(nodeKey, userId) {
      return (this.selections[nodeKey] || []).some(member => String(member.userId) === String(userId))
    },
    toggleMember(member) {
      const node = this.activeNode
      if (!node || !member?.userId) return
      const current = this.selections[node.nodeKey] || []
      const picked = { userId: String(member.userId), realName: member.realName || '', avatar: member.avatar || '' }
      if (!node.multiple) {
        this.selections[node.nodeKey] = [picked]
        this.back()
        return
      }
      this.selections[node.nodeKey] = this.isSelected(node.nodeKey, picked.userId)
        ? current.filter(item => item.userId !== picked.userId)
        : [...current, picked]
    },
    removeMember(nodeKey, userId) {
      this.selections[nodeKey] = (this.selections[nodeKey] || []).filter(item => item.userId !== String(userId))
    },
    confirm() {
      const ids = Object.fromEntries(Object.entries(this.selections)
        .map(([nodeKey, members]) => [nodeKey, members.map(member => member.userId)]))
      const result = collectInitiatorSelectSelections(this.nodes, ids)
      this.visible = false
      settle(result)
    },
    cancel() {
      this.visible = false
      settle(null)
    },
  },
})
