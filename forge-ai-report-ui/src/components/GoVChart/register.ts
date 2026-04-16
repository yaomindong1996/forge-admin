import { VChart } from '@visactor/vchart/esm/core';
import { registerBarChart, registerAreaChart, registerLineChart, registerPieChart, registerFunnelChart, registerWordCloudChart, registerScatterChart } from '@visactor/vchart/esm/chart';
import { registerTooltip, registerCartesianCrossHair, registerDiscreteLegend, registerLabel } from '@visactor/vchart/esm/component';
import { registerDomTooltipHandler } from '@visactor/vchart/esm/plugin/components';
import { registerAnimate } from '@visactor/vchart';

export const registerChartsAndComponents = () => {
  VChart.useRegisters([
    // 图表
    registerBarChart,
    registerAreaChart,
    registerLineChart,
    registerPieChart,
    registerScatterChart,
    registerFunnelChart,
    registerWordCloudChart,

    // 组件
    registerTooltip,
    registerDomTooltipHandler,
    registerCartesianCrossHair,
    registerDiscreteLegend,
    registerLabel,

    // 动画
    registerAnimate
  ]);
}

