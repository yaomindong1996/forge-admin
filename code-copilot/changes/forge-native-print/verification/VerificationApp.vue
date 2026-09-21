<script setup>
import { nextTick, ref, shallowRef } from 'vue'
import { createBrowserPrintSession } from '../../../../forge-admin-ui/src/components/print/runtime/browserPrint'
import PrintPreview from '../../../../forge-admin-ui/src/components/print/runtime/PrintPreview.vue'
import { fixture, resolveSyntheticImage } from './fixtures'

const data = shallowRef(fixture(100))
const report = ref('准备中')
const output = ref(null)
const expectedFailure = ref('')
const printReport = ref('')
let latestLayout
const mode = new URLSearchParams(location.search).get('mode')
if (mode === '500') data.value = fixture(500)
if (mode === '0') data.value = fixture(0)
if (mode === '10') data.value = fixture(10)

async function ready(result) {
  latestLayout = result
  await nextTick()
  await Promise.all([...document.querySelectorAll('.print-preview img')].map(image => image.decode()))
  const rows = result.pages.flatMap(page => page.fragments.flatMap(fragment => fragment.rows || []).filter(row => row.kind === 'data'))
  const errors = []
  if (rows.length !== data.value.context.children.items.length) errors.push('明细数量不一致')
  if (new Set(rows.map(row => row.key)).size !== rows.length) errors.push('明细重复')
  const expected = data.value.context.children.items.map(row => row.seq)
  if (JSON.stringify(rows.map(row => row.cells[0].text)) !== JSON.stringify(expected)) errors.push('明细顺序错误')
  for (const page of result.pages) {
    let previousBottom = result.geometry.bodyTopMm
    for (const fragment of page.fragments) {
      if (fragment.yMm < previousBottom - 0.001) errors.push('正文重叠')
      if (fragment.yMm + fragment.heightMm > result.geometry.bodyTopMm + result.geometry.contentHeightMm + 0.001) errors.push('正文超出页脚')
      previousBottom = fragment.yMm + fragment.heightMm
    }
  }
  const cells = [...document.querySelectorAll('[role=cell], [role=columnheader]')]
  for (const cell of cells) {
    if (cell.scrollHeight > cell.clientHeight + 1 || cell.scrollWidth > cell.clientWidth + 1) errors.push('单元格溢出')
  }
  const images = [...document.querySelectorAll('.print-preview img')]
  if (images.some(image => !image.complete || !image.naturalWidth)) errors.push('图片未就绪')
  const resultText = { status: errors.length ? 'FAIL' : 'PASS', rows: rows.length, pages: result.pages.length, images: images.length, cells: cells.length, errors, userAgent: navigator.userAgent }
  output.value = resultText
  report.value = JSON.stringify(resultText)
}

function switchFixture(count) {
  report.value = '准备中'
  expectedFailure.value = ''
  output.value = null
  data.value = fixture(count)
}

function failureCase(kind) {
  const test = fixture(10)
  if (kind === 'font') {
    test.template.header.elements[0].style.fontFamily = 'Forge Missing Font 987654'
    expectedFailure.value = 'FONT_UNAVAILABLE'
  }
  else if (kind === 'row') {
    test.context.children.items[0].name = '超高明细\n'.repeat(100)
    expectedFailure.value = 'ELEMENT_TOO_TALL'
  }
  else {
    test.template.body[1].elements[2].binding.value = 'data:image/png;base64,bm90YW5pbWFnZQ=='
    expectedFailure.value = 'RESOURCE_FAILED'
  }
  output.value = null
  report.value = '准备中'
  data.value = test
}

function failed(error) {
  const result = { status: expectedFailure.value === error.code ? 'EXPECTED_FAILURE' : 'FAIL', code: error.code, path: error.path, message: error.message }
  output.value = result
  report.value = JSON.stringify(result)
}

async function checkPrintDocument() {
  const session = await createBrowserPrintSession(latestLayout)
  const frame = document.querySelector('iframe[data-forge-print]')
  const pages = frame.contentDocument.querySelectorAll('[data-print-page]')
  const dataRows = frame.contentDocument.querySelectorAll('[data-row-kind=data]')
  const cells = [...frame.contentDocument.querySelectorAll('[role=cell], [role=columnheader]')]
  const overflow = cells.filter(cell => cell.scrollHeight > cell.clientHeight + 1 || cell.scrollWidth > cell.clientWidth + 1).length
  const snapshot = { pages: pages.length, rows: dataRows.length, overflow, images: frame.contentDocument.images.length }
  session.dispose()
  snapshot.framesAfterCleanup = document.querySelectorAll('iframe[data-forge-print]').length
  printReport.value = JSON.stringify(snapshot)
}
</script>

<template>
  <div class="verification">
    <nav>
      <strong>M1 浏览器验证（全部为合成数据）</strong>
      <button v-for="count in [0, 10, 100, 500]" :key="count" @click="switchFixture(count)">{{ count }} 行</button>
      <button @click="failureCase('font')">缺少字体</button>
      <button @click="failureCase('image')">损坏图片</button>
      <button @click="failureCase('row')">超高明细</button>
      <button :disabled="output?.status !== 'PASS'" @click="checkPrintDocument">检查打印文档</button>
    </nav>
    <pre id="verification-report" :data-status="output?.status">{{ report }}</pre>
    <pre id="print-document-report">{{ printReport }}</pre>
    <PrintPreview v-bind="data" :resolve-file="resolveSyntheticImage" @ready="ready" @error="failed" @execution="event => printReport = JSON.stringify(event)" />
  </div>
</template>

<style>
body { margin: 0; font-family: Arial, sans-serif; }
.verification { height: 100vh; display: flex; flex-direction: column; }
nav { display: flex; gap: 10px; align-items: center; padding: 8px; }
pre { margin: 0; padding: 8px; background: #eef3f8; white-space: pre-wrap; font-size: 12px; }
.print-preview { flex: 1; }
</style>
