# 独立插件构建执行器

P3.1 提供离线 `forge:plugin-build check/run`：固定源码提交和包摘要、真实宿主预检、
独立 rootless 容器构建及产物核验。生产 Web 服务不执行上传代码，不启动数据库迁移或部署。

```sh
node scripts/forge-plugin-builder/index.mjs check /absolute/builder.json
node scripts/forge-plugin-builder/index.mjs run /absolute/builder.json --reviewed
```

完整配置、镜像固定接口、隔离限制及失败处理在工程内
`scripts/forge-plugin-builder/README.md`，该文件随生成工程工具交付。
必须使用专用非 root 构建用户、私有工作目录、本地 rootless daemon、审查过的固定镜像与离线缓存。
不提供自动下载镜像/依赖或宿主构建降级；源码预检也不是恶意代码/Secret 内容审计。

结果存于独占 `job-*/result.json`，绑定源码 commit、源码/包/镜像摘要及实际产物摘要。
check 不调用 Docker、不安装；run 仅安装到副本并构建，原工程/数据库不变。
即使已经提交的客户定制也阻断自动整包替换，须人工维护差异。
产物只生成 Admin JAR 和/或 UI 文件，构建通过不表示部署完成、健康或商业授权有效。

P3.2 已加入[机器认证任务桥接](./plugin-worker.md)，显式处理[插件中心](./plugin-center.md)选定任务，
不是自动消费队列。人工确认部署与运行核验属于 P3.3。开发机无 Docker，真实容器/离线缓存验收待执行，
Docker 桩和文件单测不能替代该验收。

P3.3b1 提供独立[本地候选制品封存](./plugin-release.md)：从成功结果重新核验实际JAR/UI，
封存为只读、内容寻址快照；不复制源码/配置，不远程发布或部署。
原始result.json摘要与服务端机器报告摘要不同，不能混用；
P3.3b2a已支持原构建机器只读核对当前审批，制品登记/远程发布及部署仍待后续阶段。
