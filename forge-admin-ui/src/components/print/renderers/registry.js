import PrintBarcode from './PrintBarcode.vue'
import PrintHtml from './PrintHtml.vue'
import PrintImage from './PrintImage.vue'
import PrintQrcode from './PrintQrcode.vue'
import PrintShape from './PrintShape.vue'
import PrintStaticTable from './PrintStaticTable.vue'
import PrintTable from './PrintTable.vue'
import PrintText from './PrintText.vue'

export const printRenderers = Object.freeze({
  TEXT: PrintText,
  IMAGE: PrintImage,
  HTML: PrintHtml,
  LINE: PrintShape,
  RECTANGLE: PrintShape,
  ELLIPSE: PrintShape,
  PAGE_NUMBER: PrintText,
  BARCODE: PrintBarcode,
  QRCODE: PrintQrcode,
  TABLE: PrintTable,
  STATIC_TABLE: PrintStaticTable,
})
