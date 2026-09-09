/**
 * @fileoverview 媒体组件 spec（6 个 · scope: L）
 * @description 音视频、头像、条形码、二维码、内嵌页面。
 */

const audioPlayer = {
  type: 'audio-player',
  aliases: [],
  scope: 'L',
  category: 'media',
  group: '媒体',
  label: '音频播放器',
  desc: '音频播放控件',
  layout: { defaultSpan: 12 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 媒体
    src: { group: '媒体', priority: 'common', type: 'string', title: '音频地址' },
    // 常用 · 外观
    autoplay: { group: '外观', priority: 'common', type: 'boolean', title: '自动播放', default: false },
    loop: { group: '外观', priority: 'common', type: 'boolean', title: '循环', default: false },
    showControls: { group: '外观', priority: 'common', type: 'boolean', title: '显示控件', default: true },
    // 高级
    preload: { group: '高级', priority: 'advanced', type: 'select', title: '预加载', options: [
      { label: '自动', value: 'auto' },
      { label: '元数据', value: 'metadata' },
      { label: '无', value: 'none' },
    ], default: 'metadata' },
  } },
  dataSources: ['context', 'remote'],
  print: { hidden: true, breakAvoid: false },
}

const videoPlayer = {
  type: 'video-player',
  aliases: [],
  scope: 'L',
  category: 'media',
  group: '媒体',
  label: '视频播放器',
  desc: '视频播放控件',
  layout: { defaultSpan: 16 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 媒体
    src: { group: '媒体', priority: 'common', type: 'string', title: '视频地址' },
    poster: { group: '媒体', priority: 'common', type: 'string', title: '封面图' },
    // 常用 · 外观
    width: { group: '外观', priority: 'common', type: 'string', title: '宽度', default: '100%' },
    height: { group: '外观', priority: 'common', type: 'string', title: '高度' },
    autoplay: { group: '外观', priority: 'common', type: 'boolean', title: '自动播放', default: false },
    loop: { group: '外观', priority: 'common', type: 'boolean', title: '循环', default: false },
    muted: { group: '外观', priority: 'common', type: 'boolean', title: '静音', default: false },
    // 高级
    preload: { group: '高级', priority: 'advanced', type: 'select', title: '预加载', options: [
      { label: '自动', value: 'auto' },
      { label: '元数据', value: 'metadata' },
      { label: '无', value: 'none' },
    ], default: 'metadata' },
    objectFit: { group: '高级', priority: 'advanced', type: 'select', title: '填充方式', options: [
      { label: '填充', value: 'fill' },
      { label: '包含', value: 'contain' },
      { label: '覆盖', value: 'cover' },
      { label: '无', value: 'none' },
    ], default: 'contain' },
  } },
  dataSources: ['context', 'remote'],
  print: { hidden: true, breakAvoid: false },
}

const avatar = {
  type: 'avatar',
  aliases: [],
  scope: 'L',
  category: 'media',
  group: '媒体',
  label: '头像框',
  desc: '头像和用户信息',
  layout: { defaultSpan: 8 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 媒体
    src: { group: '媒体', priority: 'common', type: 'string', title: '头像地址' },
    text: { group: '媒体', priority: 'common', type: 'string', title: '文字头像' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'select', title: '尺寸', options: [
      { label: '小', value: 'small' },
      { label: '中', value: 'medium' },
      { label: '大', value: 'large' },
    ], default: 'medium' },
    shape: { group: '外观', priority: 'common', type: 'select', title: '形状', options: [
      { label: '圆形', value: 'circle' },
      { label: '方形', value: 'square' },
    ], default: 'circle' },
    // 高级
    showName: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示名称', default: false },
    round: { group: '高级', priority: 'advanced', type: 'boolean', title: '圆角', default: false },
    bordered: { group: '高级', priority: 'advanced', type: 'boolean', title: '显示边框', default: false },
    color: { group: '高级', priority: 'advanced', type: 'color', title: '背景色' },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: false },
}

const barcode = {
  type: 'barcode',
  aliases: [],
  // 表单画布挂件支持（isPageWidgetComponentKey），与列表页共用同一 spec
  scope: 'F+L',
  category: 'media',
  group: '媒体',
  label: '条形码',
  desc: '条形码生成（jsbarcode）',
  layout: { defaultSpan: 8, defaultW: 4, defaultH: 3 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 媒体
    value: { group: '媒体', priority: 'common', type: 'string', title: '条码值', default: 'FORGE-2026-0001' },
    format: { group: '媒体', priority: 'common', type: 'select', title: '格式', options: [
      { label: 'CODE128', value: 'CODE128' },
      { label: 'CODE39', value: 'CODE39' },
      { label: 'EAN13', value: 'EAN13' },
      { label: 'UPC', value: 'UPC' },
    ], default: 'CODE128' },
    // 常用 · 外观
    showText: { group: '外观', priority: 'common', type: 'boolean', title: '显示文本', default: true },
    barWidth: { group: '外观', priority: 'common', type: 'number', title: '条宽', default: 2 },
    barHeight: { group: '外观', priority: 'common', type: 'number', title: '条高', default: 72 },
    // 高级
    lineColor: { group: '高级', priority: 'advanced', type: 'color', title: '线条色', default: '#0f172a' },
    background: { group: '高级', priority: 'advanced', type: 'color', title: '背景色', default: 'transparent' },
    margin: { group: '高级', priority: 'advanced', type: 'number', title: '边距', default: 10 },
    fontSize: { group: '高级', priority: 'advanced', type: 'number', title: '文字大小', default: 20 },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: false },
}

const qrcode = {
  type: 'qrcode',
  aliases: [],
  // 表单画布挂件支持（isPageWidgetComponentKey），与列表页共用同一 spec
  scope: 'F+L',
  category: 'media',
  group: '媒体',
  label: '二维码',
  desc: '二维码生成（qrcode-vue3）',
  layout: { defaultSpan: 8, defaultW: 4, defaultH: 4 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 媒体
    value: { group: '媒体', priority: 'common', type: 'string', title: '二维码内容', default: 'https://forge.local' },
    // 常用 · 外观
    size: { group: '外观', priority: 'common', type: 'number', title: '尺寸', default: 132 },
    showText: { group: '外观', priority: 'common', type: 'boolean', title: '显示文本', default: true },
    // 高级
    foreground: { group: '高级', priority: 'advanced', type: 'color', title: '前景色', default: '#0f172a' },
    background: { group: '高级', priority: 'advanced', type: 'color', title: '背景色', default: 'transparent' },
    errorCorrectionLevel: { group: '高级', priority: 'advanced', type: 'select', title: '容错等级', options: [
      { label: 'L', value: 'L' },
      { label: 'M', value: 'M' },
      { label: 'Q', value: 'Q' },
      { label: 'H', value: 'H' },
    ], default: 'Q' },
    margin: { group: '高级', priority: 'advanced', type: 'number', title: '边距', default: 0 },
  } },
  dataSources: ['context'],
  print: { hidden: false, breakAvoid: false },
}

const iframe = {
  type: 'iframe',
  aliases: [],
  scope: 'L',
  category: 'media',
  group: '高级',
  label: '内嵌页面',
  desc: 'iframe 外部页面',
  layout: { defaultSpan: 24 },
  container: false,
  propsSchema: { properties: {
    // 常用 · 媒体
    src: { group: '媒体', priority: 'common', type: 'string', title: '页面地址' },
    // 常用 · 外观
    width: { group: '外观', priority: 'common', type: 'string', title: '宽度', default: '100%' },
    height: { group: '外观', priority: 'common', type: 'string', title: '高度', default: '480px' },
    borderless: { group: '外观', priority: 'common', type: 'boolean', title: '无边框', default: true },
    // 高级
    sandbox: { group: '高级', priority: 'advanced', type: 'string', title: '沙箱策略', desc: '如 allow-scripts allow-same-origin' },
    loading: { group: '高级', priority: 'advanced', type: 'select', title: '加载方式', options: [
      { label: '懒加载', value: 'lazy' },
      { label: '立即', value: 'eager' },
    ], default: 'lazy' },
    name: { group: '高级', priority: 'advanced', type: 'string', title: 'name 属性' },
    referrerpolicy: { group: '高级', priority: 'advanced', type: 'select', title: '引用策略', options: [
      { label: '无', value: 'no-referrer' },
      { label: '同源', value: 'origin' },
      { label: '严格来源', value: 'strict-origin-when-cross-origin' },
    ] },
  } },
  dataSources: ['remote'],
  print: { hidden: true, breakAvoid: false },
}

export const mediaComponentSpecs = [
  audioPlayer,
  videoPlayer,
  avatar,
  barcode,
  qrcode,
  iframe,
]
