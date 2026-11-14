# 增量验证

## 调用与测试工作台计划（2026-09-22）
- P0：首次 show=true 加载；切换能力/系统、清空和关闭时旧响应不得覆盖，错误区域可重试。多个系统不自动代选。
- P0：真实写调用先确认，未准备好不执行；重复点击/确认回调防重复；更换上下文或关闭后令牌迟到不得继续调用；报文/导出不含认证密钥。
- P1：字段输入保持字符串长 ID、数字/布尔类型及原 JSON；未知/数组结构可编辑 JSON，不丢额外字段；错误输入不执行。
- P1：失败先显示阶段/下一步、结果优先响应而非 Token；静态通过不宣称实际成功，网络错误不自动重试。
- 原前端矩阵增量执行、任务文件 lint、Vite build、git diff --check；真实组件 + 模拟 API 浏览器验证首屏、测试流程及明暗/390px。无真实网关、凭据、审批/数据库调用，后端未变不重复构建。

### 本轮结果
- 2026-09-22 18:59:08：16 文件、141/141 测试通过，2.56s。新增调用指南 7 项、参数字段 7 项、在线测试 12 项，覆盖 OAuth/HMAC、确认取消/重复/过期、关闭和切换时迟到令牌隔离、类型与字符串长 ID、凭据脱敏。
- 14 个任务 JS/Vue 文件 ESLint 无 errors/warnings；最终 Vite 生产构建通过，27.13s；git diff --check 通过。相关 SFC 最大 356 行。
- 浏览器真实组件 + 全模拟接口：多系统明确选择、授权阻断优先显示、身份缺失校验、字段/JSON 切换、取消确认恢复、确认后立即关闭提示框并显示进度、结果优先业务返回、文档切换保留输入。
- 桌面浅色与 390×844 深色通过；页面宽度/滚动宽度均 390，内容内部滚动，footerBottom=814.09 小于 viewportHeight=844；浏览器 error=[]。
- 开发预览日志曾记录一次 ResizeObserver loop 通知未交付（18:50:44），后续最终滚动布局交互未再出现；浏览器 console error=[] 不等于整个开发过程没有任何事件警告。
- 本轮未执行真实网关或业务副作用。临时标签 9 已关闭、viewport 已恢复、127.0.0.1:5200 服务会话 31910 已停止。

复用已有 capability-open-platform-productization 与 unified-capability-open-platform 的权限/版本/凭据边界。

- P0：跨租户、身份覆盖、未授权接口、不可用来源和过期版本必须拒绝；客户端密钥仍只展示一次。
- P0：注册取消不得发布；来源切换清理旧字段；异步请求不能覆盖较新选择；升级锁定来源。
- P1：应用/页面选择解析正确对象；流程与表单选择生成正确请求；目录和接入系统主操作能完成闭环。
- P1：工作台明暗主题、窄屏、加载/空态/失败、可访问名称和滚动边界。
- 命令：定向 Vitest、定向 ESLint、Vite build、git diff --check；相关 Maven -Penable-tests。
- 无真实后端时使用独立浏览器验证夹具，明确模拟数据；不将其视为真实调用验收。

## 2026-09-21 收尾增量结果

- 前端 22 个测试：发布页面/表单块来源解析、字符串长 ID、失效绑定拒绝、三类来源切换、REST 无流程模型依赖、升级恢复、旧请求隔离、提交/表单校验期间防双击、折叠技术字段校验。
- 后端 37 个测试（9 类）：REST 显式纳管、DTO 与业务权限、来源注册/发布/适配器兼容、表单必填/白名单、数据源与文档字段策略漂移、模拟回执幂等、流程撤回实例绑定、两组 Spring 自动装配开关。
- SQL 静态检查：181/182 版本唯一、NOT EXISTS/IF NOT EXISTS、审计字段、租户/客户端/能力/键摘要唯一约束、无 Flyway 模板占位符、无破坏性 SQL；Mapper XML 和验证 POM 解析通过。
- 浏览器：真实 Vue 注册器与接入工作台、模拟 API；浅色/深色、390×844 窄屏，应用→表单→确认与 REST 来源流程；长内容可滚动，底部操作按钮可见，授权空态可见，无 console error。
- 全部相关 SFC 不超过 800 行（最大 client.vue 777 行）。
- 仍未覆盖：真实网关登录/调用、Admin/Flow 联调、MySQL 事务并发、Flyway 实跑、人工权限与状态流转验收。仅打包，不启动业务服务。

## 可复跑命令

使用 JDK 17、Maven、项目锁文件对应前端依赖。每组命令从标明目录开始，不要在同一终端连续粘贴多组 `cd`。

前端目录 `forge-admin-ui`：

```bash
./node_modules/.bin/vitest run src/views/ai/capability/__tests__
./node_modules/.bin/eslint src/views/ai/capability/catalog.vue src/views/ai/capability/client.vue src/views/ai/capability/useCapabilityClients.js src/views/ai/capability/registrationSources.js src/views/ai/capability/components/CapabilityCallGuideModal.vue src/views/ai/capability/components/CapabilityClientWorkbenchModal.vue src/views/ai/capability/components/CapabilityOnlineTestPanel.vue src/views/ai/capability/components/CapabilityRegisterModal.vue src/views/ai/capability/components/CapabilityApplicationSource.vue src/views/ai/capability/components/CapabilityPageHeader.vue src/views/ai/capability/components/CapabilitySystemSource.vue src/views/ai/capability/components/useCapability*.js src/views/ai/capability/__tests__ src/stores/capability
NODE_OPTIONS=--max-old-space-size=8192 ./node_modules/.bin/vite build
```

后端目录 `forge-server`：

```bash
mvn -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-capability-parent/forge-plugin-capability-actions -am install -Dmaven.test.skip=true
mvn -B -ntp -N -f forge-framework/forge-dependencies/pom.xml install -Dmaven.test.skip=true
mvn -B -ntp -f ../code-copilot/changes/open-platform-experience/verification/pom.xml -Penable-tests test
mvn -B -ntp -pl forge-admin-server -am package -Dmaven.test.skip=true
```

仓库根目录：

```bash
git diff --check
xmllint --noout --nonet forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability-parent/forge-plugin-capability-actions/src/main/resources/mapper/capability/LowcodeFormReceiptMapper.xml code-copilot/changes/open-platform-experience/verification/pom.xml
rg -n '\$\{[^}]+\}' forge-server/db/mysql/migration/V1.0.184__add_open_platform_withdraw_operation.sql forge-server/db/mysql/migration/V1.0.182__add_open_platform_form_receipt_and_permissions.sql
```

最后的占位符扫描预期无输出、退出码 1。`verification/pom.xml` 仅是本轮定向测试入口，不加入生产 reactor；标准测试编译的存量阻断见执行记录。
