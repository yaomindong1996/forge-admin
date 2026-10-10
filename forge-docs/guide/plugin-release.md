# 插件候选制品和部署

本地封存、候选登记与远程交付分为独立操作。插件中心“发布与部署”使用真实控制面接口，
独立执行器接腾讯 COS 和 rootless Docker Compose；完整配置与恢复步骤见仓库
`scripts/forge-plugin-release/delivery/README.md`。
仅支持已核验的 community Admin 制品，不改变付费源码构建和许可证边界。

P3.3b1 提供独立 `forge:plugin-release`，接在[离线构建](./plugin-builder.md)之后，
将通过复验的 Admin JAR/UI 保存为本地只读候选快照。它不会回写任务、选择部署主机、远程上传、
迁移数据库或启动服务，也不验证商业许可证。

```bash
node scripts/forge-plugin-release/index.mjs check /absolute/release.json
node scripts/forge-plugin-release/index.mjs publish /absolute/release.json --reviewed
node scripts/forge-plugin-release/index.mjs verify /absolute/vault.json rel-<64位清单摘要>
```

完整配置、权限、协议与故障处置见生成工程携带的 `scripts/forge-plugin-release/README.md`。
需要专用非root用户、canonical私有工作区/制品库，发布须显式声明已核查。
只处理 built、未部署、community、admin-build 结果；逐项重新计算实际文件SHA，拒绝缺失/额外文件、
链接、摘要变化与异常JSON，不执行任何包内代码。

结果固定绑定原报告、源码提交/摘要、包/镜像、插件/核心版本和产物摘要。
原始result.json的文件SHA并非插件中心机器结果报告的SHA，后续桥接必须显式区分。
快照为 `rel-<manifest字节SHA>`，同ID重试先复验旧快照，损坏不覆盖；文件0400/目录0500。
这些权限是工具约定，不是防管理员改写的签名存储；本地回执时间并无认证签名。

输出部署准备清单，其中实时任务审批、目标/权限、备份恢复、迁移、部署/健康核验保持 pending。
`liveTaskApprovalVerified=false`、`deployed=false`；本地 `--reviewed` 不能替代
[插件中心](./plugin-center.md)已审查任务的实时认证，不能把 sealed/verified 当作已安装或已发布。

失败暂存和崩溃锁保留供运维核查，不自动清除或抢占。macOS提交时仅顶层目录短暂可写，
子目录/文件始终只读；最终收敛并复验后才报告成功。未完成/损坏快照须人工调查并隔离。
离线入口不包含远程动作；远程发布和部署须使用独立交付执行器、权限和备份确认。

## 当前审批只读核验

P3.3b2a提供`verify-approval /absolute/approval.json rel-<清单SHA>`，
复用原构建机器的专用HTTPS认证，仅处理运维指定的任务，不自动运行队列。
配置明确期望当前revision、approve_build审查ID、原worker和**服务端报告SHA**。
成功报告、插件/核心版本、包/源码/镜像/产物摘要必须一致；已关闭、旧审批、其它租户/机器拒绝。
Web单条XML查询当前审批快照，不读取本地制品、不写表、不审批/发布/部署。

CLI先后两次复验实际文件，单次网络请求3秒超时、不重定向、响应有界，Bearer仅由环境读取。
输出本次核验时间，liveTaskApprovalVerified=true只在该时点有效；
本地只读回执仍false，不生成长期授权或部署令牌。服务端没有独立读取制品字节，
deployed/deploymentAuthorized/serverArtifactBytesVerified仍false。
此只读核验不是部署授权；实际部署须创建独立交付任务并完成目标、备份、迁移与运行核验。

## 候选制品人工登记

P3.3b2b1新增`prepare-registration /absolute/registration.json rel-<清单SHA>`，
离线复验实际文件并输出小型登记JSON；不读取worker凭证、不联网或登记。
配置七个字段：protocolVersion、repositoryId、vaultRoot、taskId、revision、reviewId、serverResultSha256。

平台管理员在插件任务详情导入/预览JSON、确认本地已复验且尚未部署并填写核查说明；
需要独立登记及任务详情权限。服务端在事务锁内重新验证当前审批/完整报告，只追加审计记录。
记录不是制品上传、远程发布或部署许可；关闭后保留历史并显示当前审批不匹配。
本地标签、清单/原文件SHA为人工导入；平台没有读取制品字节，不能独立核验其真实性。
