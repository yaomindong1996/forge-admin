# 单测 Spec — 编码规则易混淆字符独立选择
> status: done
> created: 2026-07-18

## 0. 测试原则

- 复用归档变更“编码规则配置优化”的 31/31 Generator、14/14 前端和 8691 modules 构建基线。
- 先补本轮字符集合契约测试并确认 Red，再实现 Green。
- 不启动 Admin/MySQL/Redis，不修改本地数据库；真实 Flyway/HTTP 由可用环境补充。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| 后端 | JUnit 5；Maven `enable-tests` profile |
| 前端 | Vitest；Node v20.19.0 |
| 静态 | ESLint、xmllint、Flyway placeholder、git diff check |

## 2. 覆盖范围

### P0 — 核心业务逻辑

| 类/模块 | 场景 | 输入 | 预期结果 |
|---------|------|------|----------|
| `CodeRuleRadixCodec` | 单独排除 I | `I` | 字母表无 I，仍含 O/Z |
| `CodeRuleRadixCodec` | 排除 O/Z | `O,Z` | 字母表无 O/Z，仍含 I |
| `CodeRuleRadixCodec` | 小写进制 | `I` | 移除 i，不移除 o/z |
| `CodeRuleRadixCodec` | 旧开关 | 新字段空、旧值 1 | 等价 I/O/Z 全选 |
| `CodeRuleEngine` | 容量与编码 | 具体字符组合 | 步长、最大值和编码使用同一字母表 |

### P1 — 数据访问层

- Mapper XML 读写 `excluded_characters`。
- V1.0.39 先防重补列，再回填历史 `exclude_ambiguous=1`。

### P2 — 前端

- 字符集合规范化支持单选、组合、重复/非法字符过滤和旧开关全选。
- 进制切换不清空已选字符。
- 页面存在 I、O、Z 三个独立复选项，不再使用总开关。

### 不测试

- 不执行真实数据库迁移和登录态浏览器验收；本轮不启动服务且遵循既定联调分工。

## 3. 执行计划

- [x] Step 1：新增前后端测试并确认 Red。
- [x] Step 2：实现协议、迁移和引擎后确认 Green。
- [x] Step 3：完成前端选择交互，执行 Vitest 与定向 ESLint。
- [x] Step 4：执行 Generator 回归、前端 build、Admin 聚合编译和静态检查。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-17 | Generator 编码规则 | 八个编码规则相关测试类 | passed，31/31 | 归档变更最终基线 |
| 2026-07-17 | 前端 | Vitest + ESLint + build | passed，14/14；0 errors；8691 modules | 归档变更最终基线 |
| 2026-07-17 | Admin reactor | compile -DskipTests | passed，42/42 | Java 17 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-18 | 字符集合纯函数 | Vitest | `pnpm exec vitest run src/views/app-center/__tests__/code-rule-utils.spec.js` | passed，18/18 | Red 阶段 2 项按预期失败 |
| 2026-07-18 | 编码规则后端 | JUnit | 八个编码规则相关测试类 | passed，36/36 | 0 failure/error/skip；含旧总开关与新全选字段的服务层等价性 |
| 2026-07-18 | 相关前端文件 | ESLint | 五个编码规则 JS/Vue 文件 | passed，0 errors | 首轮发现 3 个导入排序错误后修正 |
| 2026-07-18 | 生产前端 | Vite build | `pnpm build` | passed，8691 modules | 仅仓库既有非阻断警告 |
| 2026-07-18 | Admin 聚合 | Maven compile | `mvn -pl forge-admin-server -am compile -DskipTests` | passed，42/42 | Java 17 |
| 2026-07-18 | 页面交互 | Playwright Chromium | 登录、打开新增工作台、展开 SEQ、点击 I/Z | passed | labels=I/O/Z；checked=true/false/true；console 0 errors |
| 2026-07-18 | XML/Flyway/差异 | 静态检查 | xmllint、placeholder、diff check | passed | 新文件另做 no-index check |

## 6. 执行证据

- `execution-log.md`：同目录追加实际命令与关键输出。
- 关键数据库检查：Flyway 静态契约；真实迁移由用户环境补验。
- 服务启动与停止：临时 Vite `:3000` 已停止并确认端口释放；`:8580` 为用户原有后端，未停止。
