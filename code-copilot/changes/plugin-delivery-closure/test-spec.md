# 增量验证

复用源码安装器 / 生成器 / plugin-center 已有测试，不重做历史阶段计划。

## D1 P0

- PKCE / state、Host、方法、路径、重复参数、超时、端口占用、中断关闭。
- HTTPS / 本地 HTTP 边界、禁止重定向、JSON / ZIP 字节限制、固定错误码不泄露会话。
- 版本与权益、Long ID 精度、兼容区间、元数据与包内描述匹配、SHA256 检查。
- check 不写业务源码，add 未确认不登录 / 不写入；真正 ZIP 进入现有安装器。
- 缓存链接 / 硬链接 / 权限 / 内容被改拒绝；原工程定制保护不变。

## D1 P1 / 维护基线

- 安装器与生成器 Node 测试；原模板和一次性 full 生成工程 DB stub 测试。
- 生成工程 market 模块逐字节不重命名；Node --check、git diff --check、check:edition。

## D2

按确认后的 SDK / Compose 配置追加安全负例与实际本地制品验证。
本地协议夹具不代表网站登录 / COS / Docker 已真实集成通过，未运行项明确保留待验收。

### D2.2 Compose 部署包

- check 不写入；prepare 要人工确认，仅使用已封存的实际制品，单端 / 双端都可用。
- 固定镜像 digest、loopback 端口、只读根和挂载、禁提权、禁止缺失配置自动创建。
- 目录重叠、公共权限、配置软 / 硬链接、额外命令、镜像 latest、重复端口必须拒绝。
- verify 检查制品摘要、Compose 配置、准备回执与权限；篡改任一文件不能伪造部署成功。
- full 生成工程复制 Compose 模块原始字节，DB stub 基线通过；不启动 Docker。

```bash
node --test scripts/forge-plugin-release/compose/compose.test.mjs
node --test scripts/forge-plugin/market/generated.test.mjs
```

## D1 执行命令

```bash
node --test scripts/forge-plugin/market/market.test.mjs scripts/forge-plugin/market/sso.test.mjs
node --test scripts/forge-plugin/market/generated.test.mjs scripts/forge-plugin/*.test.mjs scripts/forge-create/*.test.mjs
node --test forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
node scripts/guards/check-edition.mjs
```

使用本机 Node 20.19.0。market HTTP / loopback 用例需要允许绑定本地测试端口，未访问真实网站。
generated 用例生成一次性 full 工程，在内部执行相同 DB stub 测试，随后清理测试自行分配的目录。
