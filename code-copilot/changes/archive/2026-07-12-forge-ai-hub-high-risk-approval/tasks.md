# 实施任务 — Forge AI 中枢高风险动作人工审批

> status: done

## Task 1：模块与 HIGH 发布边界

- [x] 新增 high-risk-approval 模块并接入 plugin parent、BOM、Admin；
- [x] 抽取 BUSINESS_ACTION 定义工厂，MEDIUM 行为保持不变；
- [x] 新增 HIGH 专用发布入口和 policy 持久化；
- [x] Descriptor/Catalog 输出真实 riskLevel，HIGH 不进入同步业务动作路径。

## Task 2：信封加密

- [x] 定义 CapabilityPayloadCrypto 与不可变 envelope；
- [x] 实现 AES-256-GCM + AES Key Wrap、AAD、摘要复核；
- [x] 外部 Secret 版本化 KEK 配置及启动失败关闭；
- [x] 单测篡改 tag、错误 keyId、轮换旧 key 解密和无密钥失败。

## Task 3：审批预占与状态机

- [x] 实体、Mapper/XML、事务服务和唯一幂等预占；
- [x] 规范请求摘要、业务状态摘要和安全结果快照；
- [x] RESERVED/PENDING/EXECUTING/终态恢复与 20 路并发测试；
- [x] 所有查询显式 tenant/del_flag，更新绑定完整可信身份。

## Task 4：专用 Flowable 审批

- [x] 默认 BPMN、模型保留规则、专用 formKey；
- [x] USER A 委托身份启动审批，固定 businessKey；
- [x] 只读 BusinessCodeFormProvider 展示安全审批摘要；
- [x] completed/rejected/canceled 回调按 approvalId 加锁。

## Task 5：回调重新授权与一次执行

- [x] 重新加载 client/credentialVersion/grant/capability/version/policy/USER A/服务账号；
- [x] 业务状态摘要漂移、过期、撤销和版本漂移失败关闭；
- [x] 解密后重跑 Schema/字段/动作白名单；
- [x] 原 idempotencyKey 执行并与审批终态同事务，重复回调零副作用。

## Task 6：approval.get 固定工具

- [x] 注册统一可见的固定查询工具；
- [x] 只允许原 client + actor/serviceUser/tenant/org 查询；
- [x] 输出安全状态摘要，不返回 payload、taskId、密钥字段；
- [x] Streamable HTTP tools/list/tool-call 回归。

## Task 7：Flyway、验证与文档

- [x] 新表、逻辑删除唯一键、权限资源和外部 Secret 示例配置；
- [x] 静态检查、Mapper XML、禁止依赖和旧 transport 扫描；
- [x] High Risk、Secure Actions、Flow、Identity、Control Plane、MCP、Admin 聚合回归；
- [x] 未执行真实 Flyway/Flowable E2E/模型时明确记录；
- [x] 更新 spec/test-spec/execution-log，完成二次 Review。
