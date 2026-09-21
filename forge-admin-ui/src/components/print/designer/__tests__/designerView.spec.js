import { describe, expect, it } from 'vitest'
import { computeMiniMapRegion, scrollFromMiniMapPoint } from '../designerView'

describe('print designer minimap geometry', () => {
  const metrics = {
    scrollLeft: 100,
    scrollTop: 200,
    clientWidth: 400,
    clientHeight: 300,
    scrollWidth: 1000,
    scrollHeight: 1500,
  }

  it('maps viewport scroll into a minimap region', () => {
    expect(computeMiniMapRegion(metrics, 200, 150)).toEqual({
      left: 20,
      top: 20,
      width: 80,
      height: 30,
    })
  })

  it('converts a minimap point back into scroll offsets', () => {
    expect(scrollFromMiniMapPoint(20, 20, metrics, 200, 150)).toEqual({
      scrollLeft: 100,
      scrollTop: 200,
    })
  })

  it('clamps scroll conversion to the reachable range', () => {
    expect(scrollFromMiniMapPoint(999, 999, metrics, 200, 150)).toEqual({
      scrollLeft: 600,
      scrollTop: 1200,
    })
  })
})
