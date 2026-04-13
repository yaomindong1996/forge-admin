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

  // axis
  const { name: xAxisName, ...restXAxisProps } = chartProps.xAxis
  const { name: yAxisName, ...restYAxisProps } = chartProps.yAxis
  spec.axes = [{
    orient: 'bottom',
    ...restXAxisProps,
    label: {
      formatMethod: (value: string) => Number(value).toFixed(2)
    }
  }, {
    orient: 'left',
    ...restYAxisProps
  }]

  delete spec.xAxis
  delete spec.yAxis
  // console.log('spec-scatter-transform', spec)
  return spec
}