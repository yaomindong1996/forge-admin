# 低代码统一字段查询事件测试规格

## 1. 协议与映射

| ID | 场景 | 预期 |
|---|---|---|
| FIELD-01 | 合法规则标准化 | 只保留白名单字段并补齐安全默认值 |
| FIELD-02 | 未知触发器/来源/参数来源/结果模式 | 规则失败关闭，不发请求 |
| FIELD-03 | FORM_FIELD/CONTEXT_PATH/ROUTE_QUERY 参数 | 只按声明路径构建参数，不携带其它表单值 |
| FIELD-04 | ROOT 结果映射 | 按点路径映射到目标字段 |
| FIELD-05 | FIRST_ROW 数组/records/list/rows | 只选择首行并映射 |
| FIELD-06 | 空结果 | 进入 not_found，按 CLEAR/KEEP 处理 |
| FIELD-07 | 危险键或原型污染路径 | 配置拒绝或路径不可访问 |

## 2. 调度与并发

| ID | 场景 | 预期 |
|---|---|---|
| RUNTIME-01 | CHANGE 连续输入 | 只在最后一次防抖到期后执行 |
| RUNTIME-02 | 同规则发起新请求 | 旧请求收到 abort，新请求可回填 |
| RUNTIME-03 | 旧请求晚于新请求返回 | 旧响应被序列号丢弃 |
| RUNTIME-04 | 来源字段为空且 skipWhenEmpty | 不发请求，按配置清理目标字段 |
| RUNTIME-05 | clearTargetsOnTrigger | 请求前清理显式目标，不影响其它字段 |
| RUNTIME-06 | 组件卸载 | 计时器和未完成请求全部清理 |
| RUNTIME-07 | 请求失败 | 状态为 error，仅展示受控文案 |

## 3. 组件与设计器

| ID | 场景 | 预期 |
|---|---|---|
| UI-01 | 字段 blur | 分发对应 BLUR 规则 |
| UI-02 | Enter/扫码完成 | 分发 SCAN_COMPLETE，不提交整个表单 |
| UI-03 | MANUAL 规则 | 字段旁显示查询入口和 loading 状态 |
| UI-04 | 未找到/失败 | 使用配置文案反馈，不展示原始异常 |
| UI-05 | 设计器目录 | 只能选择 EXTERNAL_API/DATASET 受管来源 |
| UI-06 | 元数据映射 | 参数和结果目标通过结构化选择保存 |

## 4. 后端发布与协议快照

| ID | 场景 | 预期 |
|---|---|---|
| PUBLISH-01 | 合法 fieldEvents | 发布检查通过，快照完整保留受控协议 |
| PUBLISH-02 | 事件 ID/参数名/目标字段重复 | 返回明确问题路径 |
| PUBLISH-03 | 来源/目标字段不存在 | 发布失败 |
| PUBLISH-04 | debounce 越界/文案超长 | 发布失败 |
| PUBLISH-05 | URL/Header/认证/SQL/script/handler | 任意层级命中即拒绝 |
| PUBLISH-06 | 无 fieldEvents 的旧表单 | 保持兼容并正常发布 |

## 5. 验证层级

1. Vitest 纯函数和 AiForm/AiFormItem 组件定向测试。
2. generator 发布检查、RuntimeConfig 和 ProtocolSnapshot 定向 Maven 测试，使用 JDK 17 与 `-Penable-tests`。
3. generator 相关 Reactor 模块聚合编译。
4. Node `v20.19.0` 下目标 ESLint 与前端生产构建。
5. `git diff --check` 和字段事件协议危险键/泄密静态扫描。

## 6. 部署环境补验

- 使用真实已发布外部 API 和数据集验证目录、元数据、查询、ACL 与多租户隔离；
- 浏览器手工验证新增/编辑表单的 onLoad、blur、扫码枪 Enter 和手动查询体验；
- 弱网环境验证超时、取消和重试反馈；
- 本阶段不修改数据库，不需要新增 Flyway 实跑。

## 7. 2026-08-10 增量验证结果

- `src/components/ai-form/__tests__`：8 个测试文件、35 个测试全部通过，其中字段事件纯函数/并发 8 个、AiFormItem 组件 3 个。
- generator 定向测试：发布校验 4 个、协议快照 5 个、RuntimeConfig 字段事件方法 1 个，共 10 个测试通过。
- generator 相关聚合编译：external、data、generator 在内的 32 个 Reactor 模块全部成功。
- 目标 ESLint：0 errors；仅保留 `AiForm.vue` 既有的 required prop 同时有 default warning。
- 前端生产构建：成功，8887 modules transformed；保留仓库既有组件重名、混合导入和 CSS 注释 warning。
- `git diff --check`：通过；新协议没有 URL、Header、认证、凭据、SQL 或脚本配置入口。
- `LowcodeRuntimeConfigBuilderTest` 整类执行时，一个既有主子表用例因测试数据的 id 主键未声明自增而失败；新增字段事件方法已单独执行通过，未修改该无关测试基线。
