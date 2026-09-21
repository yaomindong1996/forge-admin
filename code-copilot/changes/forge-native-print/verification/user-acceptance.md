# Forge 原生打印用户验收

> 当前状态：`implemented-pending-e2e`。以下步骤由真实环境执行并回填结果；代码阶段不自动启动服务、不执行迁移、不操作打印机。

## 1. 环境与迁移

- [ ] 备份目标库，确认流程 V1.0.168–V1.0.170 已成功且 checksum 未被 repair；启动 Admin 正常执行打印 V1.0.171–V1.0.174，并确认 `forge_schema_history` 无重复版本。
- [ ] 确认 Admin、Flow 使用同一业务库与 Redis，登录用户具备 `print:execute`；模板管理员另具备打印模板权限。
- [ ] 从应用中心卡片“更多 → 打印模板”进入，确认地址为 `/app-center/application/<applicationCode>/runtime?view=settings&settingsSection=printing`，并能看到当前应用的表单来源和模板列表。
- [ ] 发布一次包含打印绑定的应用版本，确认旧应用版本仍使用其固定模板版本/hash。

## 2. 模板与普通业务

- [ ] 在应用工作台创建模板，完成拖拽、属性修改、保存草稿、发布、默认绑定；刷新后内容一致。
- [ ] 从真实门户列表和详情分别打印同一条已保存记录，确认只出现服务端保存值，未保存表单值不会混入。
- [ ] 用 0/10/100/500 行记录检查页数、表头重复、合计、页码、最后一页和 50 页上限；检查亮暗主题与窄屏入口。
- [ ] 在设计器“更多 → 打印校准与验收”选择实际纸张和方向，按 100%/无边距/关闭浏览器页眉页脚打印校准页；用实体直尺确认横纵标尺均为 100mm、校准框距四边均为 10mm，再勾选并保存本机验收记录。
- [ ] 打开正式单据的系统打印对话框并保存 PDF，按校准后的纸张设置检查尺寸；再选一台实际打印机核对边距与分页。对话框状态和本机勾选均不得当作服务端物理打印成功。

## 3. 流程与签名

- [ ] 在待办、已办、我发起各选择同一流程实例，核对 record/task/instance/run 身份和可用模板范围，不串实例或重提轮次。
- [ ] 在模板明细中加入 `flow.history`，列出节点、办理人、意见、动作、时间及 `signature`；签名应显示图片，页面和 PDF 均不得显示 fileId。
- [ ] 验证会签每个 taskId 独立；节点 `INHERIT` 沿用应用模板，`RESTRICT` 只显示子集，空子集无可用模板。
- [ ] 待办存在未保存修改时，确认提示打印已保存数据，取消后不打开打印预览。

## 4. 失败与审计

- [ ] 临时撤销某签名文件权限或制造 403，确认预览报资源失败且打印按钮不可用；恢复权限后重试成功。
- [ ] 分别模拟图片损坏、非图片响应、加载超过 10 秒，并在加载中切换模板/关闭页面；确认不输出空白签名单据，网络请求取消且 Blob URL/iframe 被清理。
- [ ] 检查 `sys_print_execution`：只含身份、模板版本、场景、PREPARED/DIALOG_OPENED/FAILED、页数或受限错误码，不含业务正文、签名、URL、Token 或异常堆栈。
- [ ] 关闭系统打印对话框而不出纸，记录仍可为 DIALOG_OPENED；这是“对话框调用已发起”，不应解释为物理打印成功。

```sql
SELECT id, tenant_id, application_id, application_version_id,
       template_id, template_version_id, source_key, object_code,
       record_id, scene, task_id, process_instance_id, process_run_id,
       actor, generated_at, result, page_count, error_code
FROM sys_print_execution
ORDER BY id DESC
LIMIT 20;
```

## 5. 回滚

1. 停用相关打印绑定或模板，撤下应用/流程入口权限，阻止新增打印会话。
2. 如应用发布版本引入错误绑定，回滚到上一个已发布应用版本；固定模板引用随应用版本恢复。
3. 前端/后端回滚到引入打印前版本。保留 `sys_print_template*` 与 `sys_print_execution`，避免丢失用户模板和审计记录。
4. 数据库迁移默认不做破坏性回退；确需删除表或资源时先导出模板与执行元数据并单独评审 SQL。

## 6. 验收记录

- 环境/版本：
- 浏览器/打印机：
- 执行人/日期：
- 通过项：
- 失败项与证据：
- 最终结论：
