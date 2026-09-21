<script setup>
import { computed } from 'vue'
import { printRenderers } from '../renderers/registry'

defineOptions({ name: 'PrintPage' })

const props = defineProps({
  page: { type: Object, required: true },
  geometry: { type: Object, required: true },
  watermark: { type: Object, default: null },
  overlay: { type: Object, default: null },
  nested: { type: Boolean, default: false },
})

function position(node) {
  return {
    position: 'absolute',
    left: `${node.xMm || 0}mm`,
    top: `${node.yMm || 0}mm`,
    width: node.widthMm ? `${node.widthMm}mm` : undefined,
    height: node.heightMm ? `${node.heightMm}mm` : undefined,
    opacity: node.style?.opacity ?? 1,
    transform: node.rotationDeg || node.flipX || node.flipY ? `rotate(${node.rotationDeg || 0}deg) scaleX(${node.flipX ? -1 : 1}) scaleY(${node.flipY ? -1 : 1})` : undefined,
    transformOrigin: 'center center',
  }
}

const watermarkTiles = computed(() => {
  const mark = props.watermark
  if (!mark?.text)
    return []
  const gapX = mark.gapXMm || 64
  const gapY = mark.gapYMm || 42
  const tiles = []
  for (let y = 10; y < props.geometry.heightMm; y += gapY) {
    for (let x = 8; x < props.geometry.widthMm; x += gapX)
      tiles.push({ x, y })
  }
  return tiles
})
</script>

<template>
  <article
    :data-print-page="nested ? undefined : page.number"
    :style="{ position: 'relative', flex: 'none', width: `${geometry.widthMm}mm`, height: `${geometry.heightMm}mm`, boxSizing: 'border-box', background: '#ffffff', color: '#000000', margin: 0, padding: 0, overflow: 'hidden', printColorAdjust: 'exact', webkitPrintColorAdjust: 'exact' }"
  >
    <img
      v-if="overlay?.src"
      :src="overlay.src"
      alt=""
      :style="{
        position: 'absolute',
        inset: 0,
        width: '100%',
        height: '100%',
        objectFit: 'fill',
        opacity: overlay.opacity ?? 1,
        transform: overlay.rotationDeg ? `rotate(${overlay.rotationDeg}deg)` : undefined,
        pointerEvents: 'none',
        zIndex: 0,
      }"
    >
    <div
      v-if="watermarkTiles.length"
      :style="{ position: 'absolute', inset: 0, zIndex: 1, overflow: 'hidden', pointerEvents: 'none' }"
    >
      <span
        v-for="(tile, index) in watermarkTiles"
        :key="index"
        :style="{
          position: 'absolute',
          left: `${tile.x}mm`,
          top: `${tile.y}mm`,
          color: watermark.color || '#94a3b8',
          opacity: watermark.opacity ?? 0.08,
          fontSize: `${watermark.fontSizePt || 14}pt`,
          fontWeight: 700,
          whiteSpace: 'nowrap',
          transform: `rotate(${watermark.rotateDeg ?? -24}deg)`,
          transformOrigin: 'center center',
        }"
      >{{ watermark.text }}</span>
    </div>
    <div
      v-for="(band, index) in [page.header, page.footer].filter(item => item?.elements?.length)"
      :key="index"
      :style="{ ...position(band), zIndex: 2 }"
    >
      <div v-for="element in band.elements" :key="element.id" :data-print-element="element.id" :style="position(element)">
        <component :is="printRenderers[element.type]" :node="element" />
      </div>
    </div>
    <div v-for="(fragment, index) in page.fragments" :key="`${fragment.id}-${index}`" :data-print-fragment="fragment.id" :style="{ ...position(fragment), zIndex: 2 }">
      <template v-if="fragment.kind === 'FIXED'">
        <div v-for="element in fragment.elements" :key="element.id" :data-print-element="element.id" :style="position(element)">
          <component :is="printRenderers[element.type]" :node="element" />
        </div>
      </template>
      <PrintPage
        v-else-if="fragment.kind === 'TILE'"
        nested
        :page="fragment.page"
        :geometry="fragment.geometry"
      />
      <component :is="printRenderers[fragment.type]" v-else :node="fragment" />
    </div>
  </article>
</template>
