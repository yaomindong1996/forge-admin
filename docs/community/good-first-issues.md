# 新人可认领任务（Good First Issue）

本文给维护者用：把下面任务复制到 Gitee Issue，并打上标签 `good first issue`、`help wanted`。  
贡献者入口：[贡献指南](../../CONTRIBUTING.md) · [开放中的 good first issue](https://gitee.com/ForgeLab/forge-admin/issues?q=is%3Aopen+label%3A%22good+first+issue%22)

## 仓库标签（请先在 Gitee 建好）

**入口不在「仓库设置/管理」里。** 按官方说明：

1. 打开仓库 → 顶部点 **Issues**
2. 页面右上角点 **⋯（三个点）** → **标签管理**  
   或者：新建 / 打开任意 Issue → 右侧 **标签** → 下拉栏底部的 **标签管理**
3. 点 **新建标签**（需仓库管理员账号）

建议创建：

| 标签 | 颜色建议 | 用途 |
|------|----------|------|
| `good first issue` | `#7057ff` | 新人 1～2 小时可完成 |
| `help wanted` | `#008672` | 欢迎外部认领 |
| `bug` | `#d73a4a` | 缺陷 |
| `enhancement` | `#a2eeef` | 增强 |
| `documentation` | `#0075ca` | 文档 |

若 ⋯ 里没有「标签管理」：确认当前登录账号是仓库 **管理员/拥有者**（访客/只读看不到）。企业版若关掉了 Issue，先在仓库功能里开启 Issues。

---

## 任务 1：README 补充「第一次贡献」入口

**标题：** `[good first issue] README 增加新人贡献入口与 good first issue 链接`

**标签：** `good first issue` `help wanted` `documentation`

```markdown
## 背景
README「贡献指南」段落较简略，新人不知道从哪下手。

## 要做什么（验收标准）
- [ ] 在 README「🤝 贡献指南」中补充：Fork + PR 流程指向 CONTRIBUTING.md
- [ ] 增加 good first issue 列表链接（Gitee issues 过滤 label）
- [ ] 增加 Issue 模板说明：Bug / 功能建议请用仓库模板

## 相关文件
- `README.md`
- `CONTRIBUTING.md`（只读引用，不必大改）

## 怎么开始
1. Fork 后改 README，本地预览 Markdown 即可
2. 提交 PR，关联本 Issue

## 预估耗时
约 0.5 小时
```

---

## 任务 2：补齐 `.env.example` 注释

**标题：** `[good first issue] 为 forge-admin-ui/.env.example 补充中文注释`

**标签：** `good first issue` `help wanted` `documentation`

```markdown
## 背景
前端环境变量模板字段含义对首次部署的开发者不直观。

## 要做什么（验收标准）
- [ ] 为 `forge-admin-ui/.env.example` 每个关键变量补充一行中文注释（端口、代理、加密开关等）
- [ ] 不写入真实密码 / Token / 内网地址
- [ ] 如有 `forge-h5-ui/.env.example`，同样补注释（若文件不存在可只做 admin-ui）

## 相关文件
- `forge-admin-ui/.env.example`
- 可选：`forge-h5-ui/.env.example`

## 怎么开始
1. 对照现有 `.env.development` 字段含义写注释，勿提交 `.env*.local`
2. PR 中说明改了哪些键

## 预估耗时
约 1 小时
```

---

## 任务 3：扩充 CONTRIBUTING「按模块自测」说明

**标题：** `[good first issue] 扩充 CONTRIBUTING 按模块自测与常见踩坑`

**标签：** `good first issue` `help wanted` `documentation`

```markdown
## 背景
CONTRIBUTING 已有「提交前最小自测」表，仍偏简略，希望按常见改动路径补细。

## 要做什么（验收标准）
- [ ] 在「提交前最小自测」下补充：改 AiCrudPage / 流程页 / 字典相关时的注意点（各 1～3 条，引用 AGENTS.md 或 DESIGN.md，勿整篇复制）
- [ ] 补充：后端禁止随意改 Flyway 已执行脚本、业务状态用枚举等 2～3 条「提 PR 易踩坑」
- [ ] 保持中文、简洁，总增量建议不超过 40 行

## 相关文件
- `CONTRIBUTING.md`
- 参考：`AGENTS.md`、`forge-admin-ui/DESIGN.md`

## 预估耗时
约 1～2 小时
```

---

## 任务 4：文档站或 README 修正一处过时端口/路径

**标题：** `[good first issue] 核对并修正文档中的默认端口与演示路径`

**标签：** `good first issue` `help wanted` `documentation`

```markdown
## 背景
文档里可能仍有过时端口（如前端 3000/后端 8580）或演示 URL 不一致。

## 要做什么（验收标准）
- [ ] 核对 README「快速开始」与 `forge-admin-ui/.env.example`、后端 `application-dev.example.yml` 默认端口是否一致
- [ ] 修正发现的过时端口/路径（至少修一处真实不一致；若已一致，在 PR 说明核对结果并补一句「以 example 为准」）
- [ ] 不改业务代码

## 相关文件
- `README.md`
- `forge-admin-ui/.env.example`
- `forge-server/forge-admin-server/src/main/resources/application-dev.example.yml`

## 预估耗时
约 1 小时
```

---

## 任务 5：为协作连接配置页补一句操作说明（文案）

**标题：** `[good first issue] 协作「连接配置」页补充简短操作提示文案`

**标签：** `good first issue` `help wanted` `enhancement`

```markdown
## 背景
系统协作连接「配置」面板步骤对首次使用者不够直观。

## 要做什么（验收标准）
- [ ] 在 `ConnectionSetupPanel`（或同页标题区）增加 1～2 句中文提示：先维护应用凭据，再启用能力
- [ ] 不改接口协议；样式遵循 `forge-admin-ui/DESIGN.md`（控制台风格，勿营销化大卡片）
- [ ] 本地打开协作连接配置页目视确认

## 相关文件
- `forge-admin-ui/src/views/system/collaboration/components/ConnectionSetupPanel.vue`

## 怎么开始
1. `cd forge-admin-ui && pnpm dev`
2. 进入系统协作连接 → 配置
3. PR 附截图

## 预估耗时
约 1～2 小时
```

---

## 维护者操作步骤

1. 按上文在 **Issues → ⋯ → 标签管理** 建好标签  
2. 新建 Issue → 选「新人任务」模板，或直接粘贴上列 Markdown  
3. 标题带 `[good first issue]`，右侧标签里勾选  
4. 有人评论认领后，指定负责人或评论确认「由 @xxx 认领」  
5. PR 合并后关闭 Issue，评论感谢并邀请继续认领下一题  

**临时办法（找不到标签管理时）：** 仍可先建 Issue，标题保留 `[good first issue]`；标签建好后再回去补打。过滤链接在有标签后才会生效。 
