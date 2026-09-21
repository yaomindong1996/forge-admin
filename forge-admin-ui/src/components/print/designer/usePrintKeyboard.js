export function usePrintKeyboard(store) {
  return (event) => {
    if (event.target.closest('input, textarea, select, [contenteditable="true"]')) {
      return
    }
    const key = event.key.toLowerCase()
    const command = event.ctrlKey || event.metaKey
    const step = event.shiftKey ? 5 : 1
    const actions = {
      arrowleft: () => store.moveSelection(-step, 0),
      arrowright: () => store.moveSelection(step, 0),
      arrowup: () => store.moveSelection(0, -step),
      arrowdown: () => store.moveSelection(0, step),
      delete: () => store.removeSelection(),
      backspace: () => store.removeSelection(),
      escape: () => store.cancelGesture(),
    }
    if (command) {
      actions.z = () => event.shiftKey ? store.redo() : store.undo()
      actions.y = () => store.redo()
      actions.c = () => store.copySelection()
      actions.x = () => store.cutSelection()
      actions.v = () => store.pasteSelection()
      actions.a = () => {
        store.selectAll()
      }
      actions['='] = () => store.nudgeZoom(1)
      actions['+'] = () => store.nudgeZoom(1)
      actions['-'] = () => store.nudgeZoom(-1)
      actions._ = () => store.nudgeZoom(-1)
      actions['0'] = () => store.setZoom(1)
    }
    if (actions[key]) {
      event.preventDefault()
      actions[key]()
    }
  }
}
