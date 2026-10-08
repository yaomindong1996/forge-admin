# 执行记录

## 2026-10-08

- 原 plugin-foundation 工作区存在未提交改动，本轮仅新增 license 包/测试/Spec，并修改自动配置列表。
- website SSO/COS 独立提交 030111b 完成；许可证实施采用已确认客户＋项目绑定。

## 客户端验证完成

- 新增 LicensePayload/Envelope/Codec、Loader、FeatureGate、Properties 与默认关闭自动配置。
  私钥不进入客户工程，启动验签原始 payload 字节，每次 Gate 查询检查使用期限。
  多节点绑定一致，维护到期不停止永久使用；文件更新需重启，离线撤销无法即时同步。
- 严格 JSON 增加关闭标量类型强制转换和浮点转整数；网站协议同步，去包名逐字核对一致。
- 实际命令（forge-server 下）：Java17 位于 `/private/tmp/lawhub-october-jdk/Contents/Home`，
  Maven `/private/tmp/apache-maven-3.9.11/bin/mvn`，Mockito ByteBuddy agent 1.17.8；
  `mvn -s /private/tmp/forge-website-central-settings.xml -B -ntp
  -pl forge-framework/forge-starter-parent/forge-starter-plugin -am test -Penable-tests`。
  最终 205 项、0 失败/错误/跳过，5 模块 SUCCESS；没有沿用默认 skipTests 假报测试成功。
- 测试均使用 @TempDir 和临时真实 Ed25519 KeyPair；未生成或存放生产密钥，没有共享库变更。
- 新增 11 Java 文件行宽检查通过；git diff --check 通过。自动配置列表仅新增本扩展一行。
- 网站商业插件 125 项，管理端 21 项和门户 29 项及构建通过，详细记录在对应工程的
  `code-copilot/changes/plugin-commercial-platform/execution-log.md`。
- 新增 deployment-guide.md 描述客户配置、可信公钥、Gate 接入、永久/维护边界和部署验收。
  CRUD 技能用于网站实体/DTO/查询边界，部署文档使用 write-page 技能；不改变开源工程品牌。
- 仅本地中文提交本轮 license 文件与 Spec，不暂存已有 plugin-center、脚本、UI 或 .DS_Store 修改。
  未 push、未合并 main、未部署、未签发真实客户授权；部署后联调由用户完成。
