# 单测 Spec — 业务域删除孤立对象优化
> status: done
> created: 2026-08-04

## 0. 测试原则

- **Red/Green TDD**：先新增业务域删除回归用例并记录 Red，再实现 Green。
- **增量优先**：只覆盖本轮删除协议、服务编排、Mapper SQL 和前端调用变化。
- **环境隔离**：不启动真实服务、不修改本地数据库；使用服务单元测试、静态检查和构建验证。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit 5（Spring Boot Starter Test） |
| Mock 风格 | JDK 动态代理 + 反射注入 MyBatis-Plus `baseMapper`，沿用模块既有测试风格 |
| 已有相关测试 | `BusinessApplicationServiceTest`、`BusinessApplicationObjectServiceTest` |
| 前端验证 | ESLint/生产构建，删除入口为薄交互层不新增组件测试框架 |

## 2. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖）

#### 类名: BusinessSuiteService

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| `delete` | 有孤立对象且已确认 | `id=10, cleanup=true` | 子域/应用/入口/有效引用为 0，对象为 2 | 删除关系、逻辑删除对象、删除业务域 |
| `delete` | 有孤立对象但未确认 | `id=10, cleanup=false` | 对象为 2 | 抛出包含对象数量的确认提示，不清理 |
| `delete` | 仍有业务应用 | `cleanup=true` | 应用数为 1 | 优先提示删除/迁移应用，不清理对象 |
| `delete` | 仍有访问入口 | `cleanup=true` | 入口数为 1 | 提示删除/迁移入口，不清理对象 |
| `delete` | 对象仍被有效应用引用 | `cleanup=true` | 有效引用数为 1 | 失败关闭，不清理对象 |
| `delete` | 无对象 | `cleanup=false` | 所有依赖计数为 0 | 直接逻辑删除业务域 |

### P1 — 数据访问层

- 静态检查 `BusinessSuiteMapper.xml`：
  - 有效引用统计同时过滤对象、关联和应用的 `del_flag = 0`。
  - 对象逻辑删除写入 `del_flag = id`，限定 `tenant_id + suite_code + del_flag = 0`。
  - 对象关系物理删除限定 `tenant_id + suite_code`。

### P2 — 入口层/前端

- Controller 参数默认 `false`，并传递到 Service。
- API 使用加密参数发送 `cleanupOrphanObjects`。
- 应用中心与旧详情页在对象数大于 0 时显示清理边界并传 `true`。

### 不测试（明确列出原因）

- 不执行真实数据库删除：用户偏好明确由用户负责真实 Flyway、服务启动和端到端联调；本轮无表结构变更。
- 不验证动态业务数据表：设计明确不删除业务数据表，本轮没有相关 SQL。
- 不新增浏览器 E2E：交互仅调整确认文案和请求参数，使用前端构建与静态差异覆盖。

## 3. 执行计划

- [x] Step 1: 新增 P0 测试并运行定向测试，确认 Red。
- [x] Step 2: 实现后端协议与 Mapper，运行定向测试确认 Green。
- [x] Step 3: 更新前端并执行生产构建。
- [x] Step 4: 执行 generator 模块测试/编译、XML 解析和 `git diff --check`。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-08-04 | 前序字典唯一性变更 | generator 相关模块测试 | 已通过 | 前序提交 `85331311`，不替代本轮验证 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-08-04 | 后端删除链路 | Red | `mvn -Penable-tests -Dtest=BusinessSuiteServiceTest test` | 失败，6 处均为缺少 `delete(Long, boolean)` | 符合预期 Red |
| 2026-08-04 | 后端删除链路 | Green | 同上 | 6/6 通过 | 无跳过 |
| 2026-08-04 | generator 回归 | 模块全量测试 | `mvn -Penable-tests test` | 569 个中 563 通过，2 失败、4 错误 | 失败位于未修改的公式、Binding、扩展版本和运行配置测试 |
| 2026-08-04 | 前端 API/交互 | 生产构建 | `pnpm build`（Node 20.19.0） | 通过，8847 modules，3m43s | 仅既有 Vite/CSS/动态导入警告 |
| 2026-08-04 | 前端目标文件 | ESLint | `pnpm exec eslint ...` | 0 errors，3 warnings | `index.vue` 原有 attributes-order 警告 |
| 2026-08-04 | Mapper XML | XML 解析 | `xmllint --noout BusinessSuiteMapper.xml` | 通过 | 无输出 |
| 2026-08-04 | 全部差异 | 差异检查 | `git diff --check` | 通过 | 保留既有 `.DS_Store` 变更 |

## 6. 执行证据

- `execution-log.md`：已记录 Red/Green、回归测试、前端构建和静态检查。
- 关键接口：`DELETE /ai/business/suite/{id}?cleanupOrphanObjects=true`。
- 关键数据库检查：本轮仅静态核对 SQL 逻辑删除与租户条件。
- 服务启动与停止：本轮不启动服务。
