# ForgeAdmin 贡献指南

欢迎参与 ForgeAdmin 开源项目共建🎉  
仓库地址：https://gitee.com/ForgeLab/forge-admin

## 零、新人从哪里开始

1. 浏览开放中的 [good first issue](https://gitee.com/ForgeLab/forge-admin/issues?q=is%3Aopen+label%3A%22good+first+issue%22)
2. 在目标 Issue 评论「认领」，按本文 Fork → 改代码 → 提 PR
3. 维护者发布任务时可参考 [docs/community/good-first-issues.md](docs/community/good-first-issues.md)

提 Issue 请选用模板：**Bug 反馈** / **功能建议**。空白 Issue 已关闭（见 `.gitee/ISSUE_TEMPLATE`）。

## 一、协作模式（重要）

本项目统一采用 **Fork + Pull Request** 工作流：

1. 请勿直接向本仓库申请成员权限；
2. 所有代码修改必须通过 PR 提交，**main 分支受保护，禁止直接 Push**；
3. 所有 PR 最终由项目创始人审核、决定是否合并，拥有最终取舍权。

> ⚠️ 重要约定
> 1. 开源主干（main）内代码遵循仓库 LICENSE 协议开源；
> 2. ForgeAdmin 品牌、商标、官方商业授权、私有化定制服务相关收益归属项目创始主体；
> 3. 社区代码贡献属于自愿开源共建，**不默认享有商业收益分成**；若需要付费合作开发，由双方单独协商劳务协议。
> 4. 部分高级功能规划为商业增值模块，相关需求 PR 可能不予合入开源主干，请理解。

## 二、开发分支规范

- `main`：稳定发行主干，保持可用状态，仅通过 PR 合并；
- `dev`（规划分支）：新功能开发分支；

> 发起 PR 请优先目标指向 `dev`（若暂无 `dev` 可指向 `main`），版本发布后由维护者合并至 main。

### 开发者标准流程

1. Fork `ForgeLab/forge-admin` 到你自己的 Gitee 账号
2. Clone 你 Fork 后的仓库到本地
3. 添加上游仓库（仅首次执行）

```bash
git remote add upstream https://gitee.com/ForgeLab/forge-admin.git
```

4. 同步上游并新建功能分支

```bash
git fetch upstream
git checkout -b feat/your-topic upstream/main
```

5. 本地修改、自测后提交（commit message 请用中文，可保留类名/路径）
6. Push 到你的 Fork，在 Gitee 上向本仓库创建 Pull Request
7. PR 请填写模板，并关联 Issue（如 `Fixes #123`）

## 三、提交前最小自测

按改动范围选择（不必全跑）：

| 改动范围 | 建议命令 |
|----------|----------|
| 仅文档 / Markdown | 目视预览即可 |
| `forge-admin-ui` | `cd forge-admin-ui && pnpm lint:fix`（有单测则再 `pnpm test` 相关文件） |
| `forge-h5-ui` | 按该目录 README / package.json 脚本做最小检查 |
| `forge-server` | `cd forge-server && mvn -pl <模块> -am compile -DskipTests` |

**请勿提交：** `.env`、`application-dev.yml` 真实密码、密钥、本地数据库 dump、`.DS_Store`。

## 四、行为约定

- 一个 PR 尽量只做一件事，便于 Review
- 大功能先开 Issue 讨论再写代码
- 尊重 Review 意见；长时间无响应的 PR 可能被关闭并说明原因

感谢你的贡献！
