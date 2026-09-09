# 低代码子表行动作与父子可信上下文测试规格

## 1. 发布与协议

| ID | 场景 | 预期 |
|---|---|---|
| CHILD-PUBLISH-01 | CHILD_ROW + COMMAND + 有效 relationKey | 发布检查通过并投影到对应子表 |
| CHILD-PUBLISH-02 | CHILD_ROW 使用 CALL_API/OPEN_PAGE | 发布阻断 |
| CHILD-PUBLISH-03 | relationKey 缺失、非法或不存在 | 发布阻断 |
| CHILD-PUBLISH-04 | 动作 relationKey 与运行子表不匹配 | 不投影且发布阻断 |
| CHILD-PUBLISH-05 | 普通 ROW/DETAIL/TOOLBAR | 保持原行为 |

## 2. 服务端可信上下文

| ID | 场景 | 预期 |
|---|---|---|
| CHILD-EXEC-01 | 有效父 ID、子 ID、relationKey | 从发布快照和父详情构建上下文后执行 |
| CHILD-EXEC-02 | 子 ID 不属于父记录 | 步骤副作用前拒绝 |
| CHILD-EXEC-03 | 父记录不可见或不存在 | 安全失败，不泄露数据 |
| CHILD-EXEC-04 | 请求 relationKey 与动作/快照不一致 | 步骤副作用前拒绝 |
| CHILD-EXEC-05 | 浏览器伪造 record/parentRecord | 不进入可信上下文 |
| CHILD-EXEC-06 | record.* / parentRecord.* / SYSTEM.* | 分别读取权威子记录、父记录和可信身份 |
| CHILD-EXEC-07 | 同幂等键更换父/子/relationKey | 请求摘要冲突，零重复副作用 |

## 3. 前端运行时

| ID | 场景 | 预期 |
|---|---|---|
| CHILD-UI-01 | 已发布子行动作 | 显示在对应子表操作列 |
| CHILD-UI-02 | 未保存的新子行 | 动作禁用，不发请求 |
| CHILD-UI-03 | 有 inputSchema | 复用通用动作输入弹窗和校验 |
| CHILD-UI-04 | 点击执行 | 只发送父 ID、子 ID、relationKey、输入、routeQuery、幂等键 |
| CHILD-UI-05 | 无权限动作 | 不显示或不可执行，后端仍独立校验 |

## 4. 验证层级

1. JUnit/Mockito：执行上下文、父子归属、发布检查和运行配置投影。
2. Vitest：最小请求载荷、子行持久化判断和设计器协议。
3. JDK 17 下 generator 定向测试与相关模块聚合编译。
4. Node v20.19.0 下 ESLint、前端生产构建、git diff 与敏感配置扫描。

## 5. 部署环境补验

- 在真实 MySQL、多租户和数据权限角色下验证父记录不可见、子记录越权和关系变更；
- 浏览器验证编辑态未保存草稿不会被请求提交或伪装成权威记录；
- 弱网、双击和重试验证幂等行为；
- 本轮不启动真实服务，不修改真实数据库、Redis、流程和外部系统。

## 6. 本轮增量验证（2026-08-11）

| 验证项 | 命令 | 结果 |
|---|---|---|
| 后端 CHILD_ROW/命令专项测试 | `JAVA_HOME=.../openjdk@17/... mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Penable-tests -Dtest=BusinessActionExecutionServiceTest,BusinessObjectPublishServiceCommandTest,LowcodeRuntimeConfigBuilderTest,BusinessActionCommandPolicyTest test` | 26 tests，0 failures，0 errors，BUILD SUCCESS |
| 前端协议与运行时 Vitest | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec vitest run src/components/ai-form/__tests__/business-action-runtime.spec.js src/views/app-center/components/designer/__tests__/business-action-designer-protocol.spec.js` | 2 files，10 tests，全部通过 |
| 本阶段目标 ESLint | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm exec eslint <本阶段前端目标文件>` | 通过；`BusinessRelationDesigner.vue` 未纳入目标集，其余既有 lint 不属于本阶段新增改动 |
| 前端生产构建 | `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build` | BUILD SUCCESS；仅保留既有动态导入、CSS 注释和 chunk size 警告 |
| 工作区静态检查 | `git diff --check` | 通过 |

未执行真实服务、MySQL、Redis、Flowable、企业微信或浏览器弱网 E2E；这些属于部署环境补验，不阻断本阶段代码验收。
