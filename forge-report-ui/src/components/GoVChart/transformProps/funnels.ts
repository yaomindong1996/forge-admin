import { Datum } from "@visactor/vchart/esm/typings"
import { cloneDeep } from "lodash"
const INNER_RADIUS = 0.75
const OUTER_RADIUS = 0.68

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

  // label
  spec.label = {
    visible: true,
  }

  // console.log('spec-funnel-transform', spec)
  return spec
}