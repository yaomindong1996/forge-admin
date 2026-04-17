import { Datum } from '@visactor/vchart/esm/typings'
import { cloneDeep } from 'lodash'

export default (chartProps: any) => {
  const spec = cloneDeep(chartProps)
  delete spec.category

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

  if (spec.extensionMark) {
    // extensionMark
    spec.extensionMark = [
      {
        name: 'arc_inner_shadow',
        type: 'arc',
        dataId: 'id0',
        style: {
          interactive: false,
          startAngle: (datum: Datum) => {
            return datum['__VCHART_ARC_START_ANGLE']
          },
          endAngle: (datum: Datum) => {
            return datum['__VCHART_ARC_END_ANGLE']
          },
          innerRadius: (datum: Datum, context: any) => {
            return context.getLayoutRadius() * spec.innerRadius - 30
          },
          outerRadius: (datum: Datum, context: any) => {
            return context.getLayoutRadius() * spec.innerRadius
          },
          fillOpacity: 0.3,
          fill: (datum: Datum, context: any) => {
            return context.seriesColor(datum[spec.seriesField])
          },
          visible: true,
          x: (datum: Datum, context: any) => {
            return context.getCenter().x()
          },
          y: (datum: Datum, context: any) => {
            return context.getCenter().y()
          }
        }
      },
      {
        name: 'arc_inner',
        type: 'symbol',
        // dataId: 'id0',
        style: {
          interactive: false,
          size: (datum: Datum, context: any) => {
            return context.getLayoutRadius() * 2 * spec.innerRadius - 100
          },
          fillOpacity: 0,
          lineWidth: 1,
          strokeOpacity: 0.5,
          stroke: {
            gradient: 'conical',
            startAngle: 0,
            endAngle: Math.PI * 2,
            stops: [
              {
                offset: 0,
                color: '#FFF',
                opacity: 0
              },
              {
                offset: 1,
                color: '#FFF',
                opacity: 1
              }
            ]
          },
          visible: true,
          x: (datum: Datum, context: any) => {
            return context.getCenter().x()
          },
          y: (datum: Datum, context: any) => {
            return context.getCenter().y()
          }
        }
      },
      {
        name: 'arc_outer',
        type: 'symbol',
        // dataId: 'id0',
        style: {
          interactive: false,
          size: (datum: Datum, context: any) => {
            return context.getLayoutRadius() * 2 * spec.outerRadius + 50
          },
          fillOpacity: 0,
          lineWidth: 1,
          strokeOpacity: 0.5,
          stroke: {
            gradient: 'conical',
            startAngle: 0,
            endAngle: Math.PI * 2,
            stops: [
              {
                offset: 0,
                color: '#FFF',
                opacity: 0
              },
              {
                offset: 1,
                color: '#FFF',
                opacity: 1
              }
            ]
          },
          visible: true,
          x: (datum: Datum, context: any) => {
            return context.getCenter().x()
          },
          y: (datum: Datum, context: any) => {
            return context.getCenter().y()
          }
        }
      }
    ]
  }
  return spec
}
