<script setup>
import { NButton, NFormItem, NInputNumber, NSelect, NSwitch } from 'naive-ui'
import { computed } from 'vue'
import { usePrintDesignerStore } from '@/stores/print/printDesignerStore'

const store = usePrintDesignerStore()
const element = computed(() => store.activeElement)
const imageFits = [
  { label: '完整包含', value: 'contain' },
  { label: '覆盖裁切', value: 'cover' },
  { label: '拉伸填满', value: 'fill' },
  { label: '仅缩小', value: 'scale-down' },
]
const barcodeFormats = ['CODE128', 'CODE39', 'EAN13', 'EAN8', 'ITF14'].map(value => ({ label: value, value }))
const pageFormats = [{ label: '当前页', value: 'CURRENT' }, { label: '当前页 / 总页数', value: 'CURRENT_TOTAL' }]

function patch(patch) {
  store.patchSelected(patch)
}

function style(key, value) {
  patch({ style: { ...element.value.style, [key]: value } })
}
</script>

<template>
  <section v-if="element" class="designer-group">
    <h3>元素行为</h3>
    <NFormItem label="锁定位置与操作" size="small">
      <NSwitch :value="!!element.locked" @update:value="store.toggleSelectionLock($event)" />
    </NFormItem>
    <div class="panel-grid">
      <NFormItem label="旋转角度" size="small">
        <NInputNumber :value="element.rotationDeg || 0" :min="-180" :max="180" :step="15" :disabled="element.locked" @update:value="$event !== null && patch({ rotationDeg: $event })" />
      </NFormItem>
      <NFormItem label="镜像" size="small">
        <div class="panel-row compact-actions">
          <NButton size="tiny" :type="element.flipX ? 'primary' : 'default'" :disabled="element.locked" @click="store.flipSelection('x')">
            水平
          </NButton>
          <NButton size="tiny" :type="element.flipY ? 'primary' : 'default'" :disabled="element.locked" @click="store.flipSelection('y')">
            垂直
          </NButton>
        </div>
      </NFormItem>
    </div>
    <NFormItem v-if="element.type === 'IMAGE'" label="图片适配" size="small">
      <NSelect :value="element.style?.objectFit || 'contain'" :options="imageFits" @update:value="style('objectFit', $event)" />
    </NFormItem>
    <NFormItem v-if="element.type === 'BARCODE'" label="条码制式" size="small">
      <NSelect :value="element.barcodeFormat || 'CODE128'" :options="barcodeFormats" @update:value="patch({ barcodeFormat: $event })" />
    </NFormItem>
    <NFormItem v-if="['BARCODE', 'QRCODE'].includes(element.type)" :label="element.type === 'BARCODE' ? '显示下方编码' : '显示下方内容'" size="small">
      <NSwitch :value="element.type === 'BARCODE' ? element.showCodeText !== false : !!element.showCodeText" @update:value="patch({ showCodeText: $event })" />
    </NFormItem>
    <NFormItem v-if="element.type === 'PAGE_NUMBER'" label="页码格式" size="small">
      <NSelect :value="element.pageNumberFormat || 'CURRENT_TOTAL'" :options="pageFormats" @update:value="patch({ pageNumberFormat: $event })" />
    </NFormItem>
    <p class="muted">
      右键元素可复制、旋转、镜像、调整层级或锁定。
    </p>
  </section>
</template>

<style scoped>
.compact-actions {
  min-height: 34px;
  align-items: center;
}
</style>
