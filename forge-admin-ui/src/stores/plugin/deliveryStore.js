import { defineStore } from 'pinia'
import { computed, reactive, ref } from 'vue'
import { createDeliveryActions } from './deliveryActions'

export const usePluginDeliveryStore = defineStore('plugin-delivery', () => {
  const targets = ref([])
  const candidates = ref([])
  const tasks = ref([])
  const loading = ref(false)
  const busy = ref(false)
  const error = ref('')
  const pending = ref(null)
  const draft = reactive({
    targetId: null,
    releaseId: null,
    action: null,
    backupReference: '',
    migrationsReviewed: false,
    backwardCompatible: false,
    note: '',
  })
  const target = computed(() => targets.value.find(row => row.id === draft.targetId))
  const candidate = computed(() => candidates.value.find(row => row.releaseId === draft.releaseId))
  const available = computed(() => candidates.value.filter(row => row.repositoryId === target.value?.repositoryId))
  const complete = computed(() => {
    if (!target.value || !candidate.value || !draft.action || target.value.activeTaskId
      || candidate.value.repositoryId !== target.value.repositoryId || draft.note.trim().length < 10) {
      return false
    }
    if (draft.action === 'publish')
      return true
    if (!draft.backupReference || !draft.migrationsReviewed)
      return false
    if (target.value.unverifiedReleaseId && draft.action !== 'restore')
      return false
    const restoreId = target.value.unverifiedReleaseId
      ? target.value.currentReleaseId
      : target.value.previousReleaseId
    return draft.action !== 'restore' || (draft.backwardCompatible && draft.releaseId === restoreId)
  })
  const actions = createDeliveryActions({
    targets,
    candidates,
    tasks,
    loading,
    busy,
    error,
    pending,
    draft,
    candidate,
    complete,
  })
  return {
    targets,
    candidates,
    tasks,
    loading,
    busy,
    error,
    pending,
    draft,
    target,
    available,
    complete,
    ...actions,
  }
})
