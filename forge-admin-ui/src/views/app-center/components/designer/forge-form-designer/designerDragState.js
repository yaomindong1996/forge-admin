import { ref } from 'vue'

export const designerDropKey = ref('')
export const designerDragSourceId = ref('')
export const designerDropError = ref('')
let dropErrorTimer = null

export function setDesignerDropKey(key = '') {
  designerDropKey.value = key
}

export function clearDesignerDropKey() {
  designerDropKey.value = ''
}

export function setDesignerDropError(message = '') {
  designerDropError.value = message
  if (dropErrorTimer)
    clearTimeout(dropErrorTimer)
  if (message) {
    dropErrorTimer = setTimeout(() => {
      designerDropError.value = ''
      dropErrorTimer = null
    }, 1800)
  }
}

export function clearDesignerDropError() {
  if (dropErrorTimer) {
    clearTimeout(dropErrorTimer)
    dropErrorTimer = null
  }
  designerDropError.value = ''
}

export function setDesignerDragSource(sourceId = '') {
  designerDragSourceId.value = sourceId
}

export function clearDesignerDragSource() {
  designerDragSourceId.value = ''
}
