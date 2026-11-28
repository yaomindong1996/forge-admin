# 采购列表导出验收

为现有采购列表增加按筛选条件导出。只有明确授予导出的采购角色可操作，按当前租户和当前采购组织范围输出。

1. 从 `purchase-order-test.vue` 定位实际菜单与 `GET /business/sample-purchase-order/page`。沿用该菜单的权限命名，新增 `business:purchaseOrderTest:export` 动作和 `business:purchaseOrderTest:exportApi` API 资源，对应 `POST /business/sample-purchase-order/export`。采购角色按实际租户与明确的角色标识定位。
2. 前端根据新会话中的按钮权限决定是否显示导出，并传入当前查询条件。后端校验动作与 API 资源，从认证会话取得租户和角色信息，使用采购查询 DTO 绑定筛选条件。复用 `SamplePurchaseOrderMapper.xml` 的 `selectPage` 并逐页导出。采购列表 Service 在分页后还会对账状态，导出时应单独确认是否需要状态投影，避免逐页对账更新排序字段。数据量上限依据实际业务协议处理。
3. 采购 `selectPage` 自带租户与软删条件。配置 DataScope 的 `table_alias=po`、`org_id_column=applicant_dept_id`，并用普通角色的 `ORG` 范围验证最终 SQL。当前组织用于范围判断；仍要核实创建路径及存量数据实际记录的申请组织。组合角色可能扩大有效范围。产品明确要求任何可导出账号都只能看当前组织时，在共享的命名查询绑定服务端会话 `activeOrgId` 作为额外条件，缺少当前组织时拒绝，并单测组合角色。
4. 准备两名拥有导出权限的普通账号，处于同租户不同组织；再准备仅有列表权限的账号和另一租户账号。留一条软删除和一条筛选不匹配的记录。另用只授动作或只授 API 的账号检查两层限制。断言列表分页总数和导出 Excel 行。

接入实际页面时，按采购 Controller 的加解密合同提交筛选参数，用项目已有的 Blob 下载处理确认响应类型。业务错误需要显示提示，成功附件应保持为可打开的工作簿。

| 验证 | 预期 |
| --- | --- |
| 两个组织的授权账号分别导出 | 各自只看到本组织有效采购单，筛选与列表一致 |
| 其他租户与当前组织编号相同 | 工作簿无其他租户数据 |
| 只读采购列表账号 | 能进列表，导出隐藏，直接请求返回无权限且没有附件 |
| 分开授予按钮或 API | 单独取得任一授权仍无法下载 |
| 未登录 | 请求返回未登录且没有附件 |

全量模板中可用 `rg --files -g 'PurchaseExportPermissionTest.java'` 找到可执行样例。样例依赖采购业务模块，使用临时 HTTP 服务和 H2。留意通用响应的业务码可能与 HTTP 状态不同，断言 `code` 与附件响应都符合约定。

```bash
cd forge-server
mvn -pl forge-business/forge-business-core -am test -Penable-tests \
  -Dtest=PurchaseExportPermissionTest -Dsurefire.failIfNoSpecifiedTests=false
```
