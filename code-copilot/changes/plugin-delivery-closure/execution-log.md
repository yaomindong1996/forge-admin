# 执行记录

## 2026-10-09：范围核对

- 分支 codex/plugin-foundation；website 保持 codex/plugin-source-delivery，不混分支。
- 已检查现有客户 SSO、源码下载、安装器、生成器、封存制品和审批核验实现。
- 用户确认：COS 制品库、Docker Compose 部署。本轮不实际操作远端环境。
- 现有未提交的制品登记文件不属于本轮变更，保留原样。
- 当前仅完成读代码 / Spec，尚未执行本轮自动化测试。

## 2026-10-09：D1 客户源码获取

- CLI 新增 market owned / versions / check / add，复用 website 客户 SSO 和真实源码接口协议。
- 配置不放会话；PKCE verifier / state 与客户会话仅存在内存，退出擦除并关闭回调监听。
- 下载验真后调用已有安装预检和事务；--reviewed / --force 不绕过客户定制保护。
- `node --test scripts/forge-plugin/market/market.test.mjs scripts/forge-plugin/market/sso.test.mjs`：23/23 通过。
- `node --test scripts/forge-plugin/market/generated.test.mjs scripts/forge-plugin/*.test.mjs scripts/forge-create/*.test.mjs`：246/246 通过。
- 原模板 DB stub 31/31 通过；full 生成工程相同测试通过。单独复跑 generated 用例 1/1 通过。
- 全部新增生产模块 `node --check` 通过，`git diff --check` 通过。
- 工作区开源边界检查通过（索引 9741 / 工作区 9787）；提交前另检查暂存原始内容。
- 初次本地网络测试被沙箱拒绝监听，授权仅 loopback 测试后通过，不连接远端。
- 初次子测试继承 NODE_TEST_CONTEXT 导致 TAP 输出为空；清除子进程测试 IPC 上下文后严格断言通过。
- 接口状态与 website 实现核对为大写 PUBLISHED，Long ID 按精确字符串处理，无数字截断。
- 本轮没有 Java / Vue 改动，不执行无关服务聚合构建；网站真实 SSO / MySQL / COS / Docker 验收未运行。
- 测试服务全部由测试关闭，临时生成工程已由测试回收；用户服务和工作区存量文件未清理。
