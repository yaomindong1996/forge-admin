# 移动端组件能力矩阵

> 状态：`implemented`。全部设计器组件键已登记；核心字段、远程选择器、复杂数据卡片和常用页面挂件均有移动实现，不安全组件明确阻断，不会伪装成普通输入框。

## 当前交付边界

- 已完成真实交互：文本、数字、金额、单复选、开关、滑块、评分、日期/时间/范围、文件、图片、签名、扫码、动态数组。
- 已完成移动适配：表单布局、卡片、页签、折叠、页面画布单列化、列表卡片、分页、流程字段/明细权限。
- 人员、组织、区域、级联、关联记录已接统一移动选择面板；支持静态树、系统用户/组织/区划接口、业务记录接口、受管查询源和 `/system`、`/ai`、`/api` 白名单内的声明式接口。
- 选择器支持搜索、分页、多选、Long ID 字符串化、联动参数、显示名称冗余字段和单选记录字段映射；外部 URL 或 DELETE 等非选项接口会被阻断。
- 表格、树表和主从明细统一转为移动卡片；树数据保留层级缩进，远程选择使用底部全屏面板。
- 日历、水印已提供移动专用渲染；二维码及 CODE128/CODE39/EAN13/UPC 条形码通过跨端 Canvas 生成真实机器码。
- 脚本、动态 Vue 永久阻断；富文本/Markdown 当前只做去标签文本展示；iframe 当前只显示域名白名单提示。
- 小程序请求需要部署侧配置 `VITE_MP_API_BASE_URL`，并在平台后台登记 request/uploadFile 合法域名。

## 字段组件

| 组件键 | 移动策略 | 目标等级 |
|---|---|---|
| input / textarea / text | 输入、长文本、只读文本分别渲染 | interactive / readonly |
| number / money | 数字键盘、范围和精度校验 | interactive |
| slider / rate / color | 移动滑块、评分、色板弹层 | interactive |
| select / dictSelect / radio / radioButton / checkbox | Picker、胶囊单选、复选列表 | interactive |
| transfer | 双列表改为“可选/已选”底部弹层 | adapted |
| cascader / treeSelect | 移动树形面板，保留层级、联动和多选语义 | adapted |
| customSelect | 受管查询源或安全白名单声明式接口 | adapted |
| date / datetime / month / year | 日期时间 Picker | interactive |
| daterange / datetimerange / timerange | 起止双选择器 | interactive |
| switch | 移动开关 | interactive |
| barcodeScanner | `uni.scanCode`，H5 按浏览器能力降级 | adapted |
| userSelect / orgTreeSelect / regionTreeSelect | 搜索列表、组织树、区域树统一移动面板 | adapted |
| objectReference / recordSelector | 分页远程记录选择、回显及字段映射 | adapted |
| fileUpload / imageUpload | 统一文件 ID 语义、鉴权预览 | interactive |
| array / subTable | 明细卡片、行权限和嵌套校验 | adapted |

## 布局、业务和动作组件

| 组件组 | 组件键 | 移动策略 |
|---|---|---|
| 布局 | grid, col, card, tabs, tabPane, collapse, collapseItem, box, divider, spacer, space, groupTitle, formSectionTitle, tableCell | 单列优先，必要时横向滚动 |
| 表格/CRUD | table, AiTable, data-table, AiCrudPage | 卡片列表、筛选抽屉、分页 |
| 表单 | AiForm, search-form, step-form | 统一字段注册器和移动校验 |
| 明细 | subTable, sub-table-tabs | 明细卡片和底部弹层 |
| 导航/数据 | toolbar, tree-panel, detail-info | 底部工具栏、全屏树、描述列表 |
| 动作 | query-set, custom-query, import-button, export-button, add-button, reset-button, action-button, button-group, link | 统一动作分发和权限校验 |
| 展示 | info-panel, steps, timeline, empty-state, stats-strip | 移动原生展示 |
| 签名 | signature-pad | Canvas 签名，输出文件 ID/临时文件按既有接口上传 |

## 页面挂件和媒体

| 组件键 | 移动策略 | 目标等级 |
|---|---|---|
| back-button / page-title / text-title / paragraph / text-tip / tag-list / statistic | 原生展示 | interactive / readonly |
| rich-text / html-tag / custom-html | 安全富文本，移除脚本和事件属性 | readonly |
| markdown / code / log | 安全文本渲染，不执行代码 | readonly |
| watermark | 页面容器水印 | adapted |
| calendar / countdown / number-animation | 移动展示组件 | adapted |
| descriptions / announcement / list | 描述卡片、公告、列表 | adapted |
| breadcrumb / menu / pagination | 移动导航与简化分页 | adapted |
| split | 上下布局或页签替代 | adapted |
| audio-player / video-player / avatar | uni-app 媒体组件 | adapted |
| barcode / qrcode | 跨端 Canvas 真实生成，保持设计端值、尺寸、颜色和格式协议 | adapted |
| iframe | H5 白名单 iframe；小程序 web-view/外链提示 | readonly |
| vue-component | 禁止动态编译和执行 | blocked |
