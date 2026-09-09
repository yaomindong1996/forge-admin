# 执行日志 — 编码规则易混淆字符独立选择

## 2026-07-18 基线与范围

| 项目 | 结果 | 说明 |
|------|------|------|
| 仓库规则与记忆 | passed | 已读取根 AGENTS、code-copilot AGENTS、三份 memory、coding-style、automated-testing-standard |
| 相关 Skill | applied | using-superpowers、writing-plans、forge-coding-standards、frontend-design |
| 分支 | `main` | 非禁止直接修改的 `master` |
| 工作区 | dirty | 目标前端四个文件已有未提交改动；本轮保留并局部追加 |
| 历史基线 | reused | 2026-07-17 Generator 31/31、前端 14/14 + build、Admin 42/42 |

### Research 结论

- 页面当前只有 `excludeAmbiguous` 总开关。
- 数据库和 DTO 只能持久化 0/1。
- 引擎在 boolean=true 时固定移除 I/O/Z，容量和 legacy 宽度也依赖该 boolean。
- 采用新 `excludedCharacters` 字段 + 旧开关兜底，避免改变历史值 `1` 的语义。

### 本轮服务

- 未启动 Admin、Flow、MySQL 或 Redis。
- 为 Playwright 验证临时启动 Vite `127.0.0.1:3000`，验证后发送中断并确认端口不可连接。
- `127.0.0.1:8580` 是本轮前已存在的后端，保持运行，未做任何停止操作。

## 2026-07-18 Red / Green 与最终验证

| 阶段 | 命令/范围 | 结果 | 证据 |
|------|-----------|------|------|
| Red 前端 | code-rule-utils Vitest | failed as expected，2/18 | 缺少 `excludedCharacters` 默认值和 `normalizeExcludedCharacters` |
| Red 后端 | Engine/Migration/Mapper tests | failed as expected | 缺少 String alphabet 重载、DTO setter 和 V1.0.39；同时暴露本地仓库旧 starter-id 构件，先 reactor install 后复跑 |
| Green 前端 | code-rule-utils Vitest | passed，18/18 | 单选、组合、旧开关全选归一化均覆盖 |
| Green 后端 | 八个编码规则相关测试类 | passed，36/36 | Controller 3、Migration 6、Engine 10、Legacy 2、Mapper 5、Dynamic CRUD 3、Object Binding 6、Cache 1 |
| 定向 Lint | 五个编码规则 JS/Vue 文件 | passed | 0 errors/warnings；首轮 3 个 import 排序问题已修复 |
| 浏览器 | Playwright Chromium | passed | I/O/Z 三项；点击 I/Z 后 true/false/true；console 0 errors；截图 `/tmp/code-rule-ambiguous-options.png` |
| 前端构建 | Node 20.19.0 `pnpm build` | passed | 8691 modules，约 2m57s；仅既有组件重名、动态导入和 CSS 注释警告 |
| Admin 聚合 | Java 17 Maven compile | passed，42/42 | Generator 与 Admin 装配成功 |
| XML/Flyway | xmllint + placeholder | passed | Mapper XML 合法；V1.0.39 无 Flyway `${...}` 占位符 |
| 差异格式 | git diff check | passed | tracked 差异无空白错误；新增文件补做 no-index check |

### 实现结论

- 页面从单个总开关改为 I、O、Z 三个独立复选项，并保留当前 Forge 克制配置工作台风格。
- `excludedCharacters` 进入 DTO、Entity、Mapper、定义缓存和 V1.0.39；历史 `excludeAmbiguous=1` 回填并兜底为 `I,O,Z`。
- Codec/Engine 的编码、容量、号段步长和 legacy 宽度缓存统一使用规范化字符集合。
- 进制切换不再清空选择；DECIMAL/HEX 保留配置但不改变其字符表。

### 跳过项

- 未执行真实 Flyway migrate、更新后的保存接口 HTTP 和数据库回查；避免修改用户运行态数据库，由用户环境补验。
