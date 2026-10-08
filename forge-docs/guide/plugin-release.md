# 本地候选制品封存

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
本阶段没有远程仓库适配、目标部署或自动恢复，须确认环境后继续建设。
