import { computed, ref, watch } from 'vue'

export function useModelFormSession({ providerModal, modelModal }) {
  let epoch = 0
  const providerAdvanced = ref(false)
  const modelAdvanced = ref(false)
  const providerBaseline = ref('')
  const modelBaseline = ref('')

  watch(() => providerModal.show, (show) => {
    providerBaseline.value = show ? JSON.stringify(providerModal.form) : ''
  }, { flush: 'post' })
  watch(() => modelModal.show, (show) => {
    modelBaseline.value = show ? JSON.stringify(modelModal.form) : ''
  }, { flush: 'post' })

  const providerDirty = computed(() =>
    providerModal.show && !!providerBaseline.value && providerBaseline.value !== JSON.stringify(providerModal.form),
  )
  const modelDirty = computed(() =>
    modelModal.show && !!modelBaseline.value && modelBaseline.value !== JSON.stringify(modelModal.form),
  )

  function closeProvider() {
    providerModal.show = false
    providerModal.form.apiKey = ''
    providerBaseline.value = ''
    providerAdvanced.value = false
  }

  function closeModel() {
    modelModal.show = false
    modelBaseline.value = ''
    modelAdvanced.value = false
  }

  async function requestClose(modal, dirty, close) {
    if (modal.saving)
      return false
    if (!dirty.value) {
      close()
      return true
    }
    const expected = epoch
    return new Promise((resolve) => {
      if (!window.$dialog?.warning) {
        close()
        resolve(true)
        return
      }
      window.$dialog.warning({
        title: '配置尚未保存',
        content: '关闭会放弃本次修改，已保存的配置保持不变。',
        positiveText: '放弃修改',
        negativeText: '继续编辑',
        onPositiveClick: () => {
          const allowed = epoch === expected && !modal.saving
          if (allowed)
            close()
          resolve(allowed)
        },
        onNegativeClick: () => resolve(false),
        onClose: () => resolve(false),
        onMaskClick: () => resolve(false),
      })
    })
  }

  const requestCloseProvider = () => requestClose(providerModal, providerDirty, closeProvider)
  const requestCloseModel = () => requestClose(modelModal, modelDirty, closeModel)

  async function confirmNavigation() {
    if (providerModal.saving || modelModal.saving)
      return false
    return await requestCloseProvider() && await requestCloseModel()
  }

  function dispose() {
    epoch++
    closeProvider()
    closeModel()
  }

  return {
    providerAdvanced,
    modelAdvanced,
    requestCloseProvider,
    requestCloseModel,
    closeProvider,
    closeModel,
    confirmNavigation,
    dispose,
  }
}
