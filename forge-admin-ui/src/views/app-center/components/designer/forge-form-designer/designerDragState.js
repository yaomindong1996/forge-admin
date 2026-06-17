import { ref } from 'vue'

export const designerDropKey = ref('')
export const designerDragSourceId = ref('')

export function setDesignerDropKey(key = '') {
  designerDropKey.value = key
}

export function clearDesignerDropKey() {
  designerDropKey.value = ''
}

export function setDesignerDragSource(sourceId = '') {
  designerDragSourceId.value = sourceId
}

export function clearDesignerDragSource() {
  designerDragSourceId.value = ''
}
