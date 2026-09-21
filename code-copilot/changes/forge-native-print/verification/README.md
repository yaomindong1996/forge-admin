# M1 浏览器验证

此目录只使用合成数据，不连接 Admin、Flow、数据库或真实附件。正式运行入口在后续 M3–M5 接入。

在项目根目录执行：

```bash
source ~/.nvm/nvm.sh && nvm use v24.21.0
node code-copilot/changes/forge-native-print/verification/serve.mjs
```

打开 `http://127.0.0.1:4318/`，依次点击 0/10/100/500 行。结果显示行数、页数、重复/溢出检查。`检查打印文档` 会创建和销毁独立 iframe，检查未缩放的输出 DOM。`打印` 调用浏览器打印；`DIALOG_OPENED` 仅表示客户端调用事件。

异常按钮验证缺少指定字体、损坏图片和超高明细。预期显示 `EXPECTED_FAILURE` 并禁用打印。切回正常数量可重新准备。

构建同一验证入口（包含所有 M1 渲染与运行模块）：

```bash
node code-copilot/changes/forge-native-print/verification/serve.mjs --build
```

输出至 `/private/tmp/forge-print-verification-dist`。使用完按 Ctrl+C 停止验证服务。不要将临时目录作为生产发布物。

2026-09-18 已用 Chromium 153 实际验证，记录见 [browser-results.json](browser-results.json)。已人工查看 10 行单据截图，中文、编码和表格边界显示正常；没有保存 PDF 或调用物理打印机。A4 100% 缩放、关闭浏览器额外页眉/页脚、罕见汉字覆盖和实机尺寸仍按 test-spec 的人工验收执行。

已知边界：首期只支持本地已安装字体及系统通用字体族；显式指定但未安装的字体会失败。`keepWithNext` 保持整个区块与下一区块首片一起，无法在一页容纳时明确失败。页眉/页脚 `repeat=false` 只在首/末页显示，仍为各页保留相同物理正文区域。

2026-09-18 目标纠正后，在 forge-admin 重新执行单测/构建及浏览器验证，实际结果见 [browser-results-forge-admin.json](browser-results-forge-admin.json)。原 browser-results.json 明确标注为 LawHub 来源记录，不混用两次结果。

## M2 设计器验证

使用相同 serve.mjs 启动后访问 `http://127.0.0.1:4318/?designer`。提供 100 行合成明细、主字段、图片解析器和主题/字段失效开关。默认草稿只存在该浏览器 origin 的 localStorage，键为 `forge:print:local-draft:v1`；不会请求真实 API。

检查：元素拖动/右下角缩放、Shift 多选和空白区框选、方向键、元素复制粘贴、字段拖入、区块上下排序、属性修改、多行表头合并拆分与合计、保存/恢复/导入导出、新建取消、失效字段定位、真实分页预览、明暗主题和窄窗口。底部“验证状态”显示实际 Pinia 状态的只读摘要。

本轮证据：[browser-results-m2.json](browser-results-m2.json)。主页面组件落在 `forge-admin-ui/src/views/print/designer.vue`；正式模板列表、权限菜单、数据源绑定与 API 保存仍待 M3/T28、M4 接入。


## M3b 持久化与运行验证

正式模板页现在通过服务端 API 保存，M2 本地草稿模式仅保留在独立组件验证入口。

在根目录使用 Node 24.21.0 执行：

```bash
node code-copilot/changes/forge-native-print/verification/serve.mjs --persistence
```

访问 `http://127.0.0.1:4318/persistence.html`。仅在该模式中把 API、字典和当前用户替换为合成实现；生产构建不导入这些 mock。HTTP 仅监听 localhost，内存数据随进程结束清除。m3b-wire.json 是 PrintTemplateControllerTest 通过真实 Service/Mapper/事务生成的合成 prepare 响应，时间戳作为固定样例保存。

依次检查：新建→设计→保存→发布→版本列表→场景默认绑定；“下次保存冲突”验证 409 保留编辑，“下次保存延迟”验证等待期间的编辑；“合成单据打印”使用固定版本 1，“仅运行权限”验证运行入口，“切换缺少适配器”验证无旧预览回退。浏览器操作只模拟页面调用，授权真实性以 Java 行为测试和后续真实 E2E 为准。

结果见 [m3b-results.json](m3b-results.json) 与 [browser-results-m3b.json](browser-results-m3b.json)。本轮已关闭页面并停止验证服务器，未打印 PDF 或操作物理打印机。

M4 接入时实现 PrintApplicationAccess（应用授权/事务行锁/发布引用检查）和 LOWCODE PrintDataProvider。Provider.authorize 必须从应用发布快照解析允许的不可变模板版本及字段目录，验证当前记录权限；流程场景还须解析同一业务 processRunId。缺少适配器返回 503 是当前阶段预期行为。不要以最新模板版本或设计态绑定替代发布清单。


## M4c 工作台增量

仓库根使用 Node 24：`node code-copilot/changes/forge-native-print/verification/serve.mjs --workspace`；合成验证页仍使用 `http://127.0.0.1:4318/app-center/application/purchase-demo?section=printing`，该地址只属于独立验证服务。正式前端入口为应用中心卡片“更多 → 打印模板”，对应 `/app-center/application/<applicationCode>/runtime?view=settings&settingsSection=printing`。`workspace.html` 只是 Vite 的合成入口文件。`--workspace --build` 可独立构建。选择页面中的表单、新建合成模板、返回工作台、切换明暗主题；390px 验证表格水平滚动和更多按钮。列表/详情合成入口共用正式预览页，真实 AiCrudPage 点击/主键转义另由 AiCrudPage-print.spec.js 覆盖。`m4c-actions.json` 来源为后端测试输出 target/print-runtime-actions.json。所有记录/权限/API 都是合成数据，不能替代真实门户 E2E。
