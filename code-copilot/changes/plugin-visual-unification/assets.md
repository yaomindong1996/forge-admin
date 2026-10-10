# 插画与图标资产

- 生成方式：内置 image_gen（2026-10-10）。
- 采用文件：`forge-admin-ui/src/assets/illustrations/plugins/plugin-modules.png`，1536×1024 RGBA。
- 采用第一版较低饱和配色。第二版去背景尝试未改善效果，未进入项目。
- 用于插件中心 120×80 和发现页 180×120 的小幅点缀，不作为整页背景。
- 原生图标：`src/components/common/forgeSymbols.js` + `ForgeSymbol.vue`，统一网格与描边，颜色继承主题。
- 公共旧图片仍可被菜单配置/图标选择器引用，保留；一级导航已取消旧图标展示。

## 采用提示词

Create one small professional UI spot illustration for Forge Admin's plugin center, not a full page mockup. Transparent background. Subject: a precision-engineered modular platform made of three interlocking square software modules, one module slightly raised to reveal neat connector pins, subtle small line symbols engraved on top (a branching workflow, a small database, and a simple code bracket); one coherent compact composition. Style: crisp restrained technical editorial illustration, shallow isometric perspective, clean flat planar facets, elegant thin slate-blue outlines, porcelain off-white surfaces and a few muted cornflower blue faces (#5B83D8), no saturated multicolor palette. Broad simple recognizable shapes that read well at 144 by 96 pixels, landscape 3:2 proportions, centered with tight but safe padding. Fine detail should not overwhelm silhouette. Minimal depth, no heavy shadow, no glow, no gradient backdrop, no floating confetti, no text, no letters, no numbers, no logo, no watermark. Genuine transparent alpha outside the objects, suitable for both light and dark enterprise UI. This is a small quiet decoration, not a large marketing hero.

## 未采用的编辑提示

保留原对象，只移除外围阴影/光晕并维持透明背景。此候选配色变得过艳，未使用。
