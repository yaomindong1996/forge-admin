import { Datum } from "@visactor/vchart/esm/typings"
import { cloneDeep } from "lodash"

export default (chartProps: any) => {
  const spec = cloneDeep(chartProps)

  // tooltip
  const keyFill = spec.tooltip.style.keyLabel.fill
  const valueFill = spec.tooltip.style.valueLabel.fill
  const titleFill = spec.tooltip.style.titleLabel.keyFill
  delete spec.tooltip.style.keyLabel.fill
  delete spec.tooltip.style.valueLabel.fill
  delete spec.tooltip.style.titleLabel.keyFill
  spec.tooltip.style.keyLabel.fontColor = keyFill
  spec.tooltip.style.valueLabel.fontColor = valueFill
  spec.tooltip.style.titleLabel.fontColor = titleFill


  // console.log('spec-word-cloud-transform', spec)
  return spec
}