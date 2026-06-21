# 执行日志

## 2026-06-21

- 创建变更目录：`code-copilot/changes/lowcode-runtime-tab-workspace-unique-validation/`
- 初始任务：运行态平铺/多页签录入、通用唯一性校验。

### 增量验证

变更范围：
- 前端：`AiCrudPage` 新增 `flat/tabWorkspace` 运行态录入方式，低代码运行页和设计器透传 `formOpenMode/tabWorkspace`，字段属性增加唯一校验开关。
- 后端：低代码模型协议增加唯一约束，动态 CRUD 新增/编辑保存前执行租户内唯一校验。

执行命令与结果：
- `git diff --check`
  - 结果：通过，无空白错误输出。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/views/ai/crud-page.vue src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/StructuredListPageDesigner.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`
  - 结果：通过，仅输出 Node 版本切换信息 `Now using node v20.19.0`。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 执行目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`。编译期间存在既有 deprecation / unchecked 提示，不阻断。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 53.21s`。
  - 警告：存在既有 CSS `//` 注释 minify 警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 提示，不阻断本次构建。

跳过项：
- 未启动本地前后端服务，未执行浏览器交互和真实数据库接口验证；原因是本轮先完成代码级闭环，服务级验证需要依赖本地后端、MySQL 和已发布低代码客户对象配置。

服务清理：
- 本轮未启动长期运行服务，无需清理 PID。

### 平铺录入与唯一校验入口修正

问题：
- `flat` 初版仍把表单放在列表下方，复杂表单录入空间不足。
- 唯一校验入口只在字段资产/字段属性面板和新增字段弹窗中可见，表单设计器选中字段时不直观。

修正：
- `flat` 改为当前页内单表单视图：新增/编辑后隐藏搜索区和列表，表单独占主内容区，右上角提供“返回列表”。
- 表单设计器右侧属性面板的字段“状态”页新增“唯一校验”开关。
- 表单组件 schema 归一化保留 `advancedProps`，自动字段资产创建和字段同步时会把 `advancedProps.unique` 写入模型字段，后端动态 CRUD 保存时据此校验重复。

执行命令与结果：
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/form-first/autoFieldRegistry.js src/views/app-center/components/designer/BusinessFormDesigner.vue`
  - 结果：通过，仅输出 Node 版本切换信息。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 1m 15s`。
  - 警告：仍存在既有 CSS `//` 注释 minify 警告，以及 `src/store/index.js` 动静态导入 chunk 提示，不阻断。

服务清理：
- 本轮未启动长期运行服务，无需清理 PID。

### 多页签工作区改为列表同级页签

问题：
- 将表单面板放在列表下方后，复杂表单可视空间不足，用户需要在页面和表单内频繁滚动，录入效率差。

修正：
- `tabWorkspace` 改为“列表 + 表单草稿”的同级页签工作区。
- 激活“列表”页签时显示搜索区和表格；激活表单页签时隐藏搜索区和表格，整块主内容区展示表单。
- `flat` 模式仍保留列表下方单面板，避免改变单面板语义。
- 表单页签关闭逻辑同步调整：关闭当前表单后切到相邻表单，关闭最后一个表单后回到“列表”页签。

执行命令与结果：
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue`
  - 结果：通过，仅输出 Node 版本切换信息。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 1m 3s`。
  - 警告：仍存在既有 CSS `//` 注释 minify 警告，以及 `src/store/index.js` 动静态导入 chunk 提示，不阻断。

服务清理：
- 本轮未启动长期运行服务，无需清理 PID。

### 多页签样式修正

问题：
- 初版多页签工作区跟表格同级直接追加，表格 `flex: 1` 和主区域滚动策略会让表单区域撑开页面，视觉上像覆盖当前页面。
- 页签使用空的 `n-tab-pane` 承载 tab 标题，内容实际在外部面板，样式不符合“同页工作台”预期。

修正：
- `AiCrudPage` 主区域在工作区打开时增加 `has-inline-workspace` 状态，表格和表单工作台改为明确的上下分区。
- 多页签改为轻量自绘页签条，支持激活态、脏标记和单页签关闭。
- 表单面板增加 `inline-form-panel-body` 内部滚动，页签条、头部和底部按钮固定在工作台内，避免撑满整页。

执行命令与结果：
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiCrudPage.vue`
  - 结果：通过，仅输出 Node 版本切换信息。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`built in 1m 4s`。
  - 警告：仍存在既有 CSS `//` 注释 minify 警告，以及 `src/store/index.js` 动静态导入 chunk 提示，不阻断。

服务清理：
- 本轮未启动长期运行服务，无需清理 PID。

### 唯一校验保存分支补漏

问题：
- 用户反馈“唯一性校验没生效，设置了唯一后数据仍能重复新增”。
- 排查发现普通单表新增/编辑已执行 `validateUniqueConstraints`，但主子表和关联运行态会在 `insertMasterDetailData` / `insertJoinedData` / `updateMasterDetailData` / `updateJoinedData` 分支内提前返回，绕过后续单表唯一校验。
- 旧运行配置如果 `modelSchema` 缺失但 `editSchema.advancedProps.unique` 已存在，fallback 追加约束时会拿到不可变空列表，存在边界风险。

修正：
- 主子表和关联运行态新增/编辑在写库前统一执行主表唯一校验。
- 唯一校验内部统一读取主表 payload，兼容 `{ main, children }` 请求结构。
- 唯一约束解析结果改为始终可追加的 `ArrayList`，确保旧配置可从 `editSchema` fallback 生成唯一约束。

执行命令与结果：
- `git diff --check`
  - 结果：通过，无空白错误输出。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 执行目录：`forge-server`
  - 结果：通过，`BUILD SUCCESS`，总耗时 `12.434 s`。
  - 警告：存在既有 deprecation / unchecked 提示，不阻断。

跳过项：
- 未启动本地后端和 MySQL 做真实客户新增接口验证；原因是本轮先完成后端分支补漏和编译闭环，真实数据验证依赖已发布低代码客户对象配置。

服务清理：
- 本轮未启动长期运行服务，无需清理 PID。
