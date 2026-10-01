# 应用调试包导出/导入

## 背景

用户在客户环境设计的低代码应用，本地无法复现时难以定位问题。需要把应用设计态打成配置包，在本地导入后自动建应用、对象、表结构与页面，便于调试验证。

## 目标

### P0 / P1（已完成）

- 整应用或按页勾选导出；依赖闭包；导入建应用/对象/DDL/builder；进度可视化。

### P2（本阶段：完整应用包）

导出/导入必须形成**可打开、配置齐全**的应用设计态，避免缺流程/打印/扩展等导致本地无法对照复现。

1. **伴随资产完整导出**（`schemaVersion=2`）
   - 对象：字段/表单/列表/联动/关系 + **单据 documentConfig**
   - 应用：options（含 inAppBuilder）/ portalConfig / **aiAssistantConfig**
   - **processes**（草稿 businessProcessJson）
   - **bindings**（FLOW 等能力挂接）
   - **extensions**（含内容）
   - **entries**（访问入口）
   - **triggers**（按对象）
   - **printing**（模板 schema + 绑定；设计态软采集，不要求源环境已发布）
2. **导入还原**：单项失败记入 warnings，不阻断主对象/页面还原。
3. **可选一键发布**：`autoPublish=true`，失败只警告。

## 非目标

- 业务行数据、密钥、真实 JDBC 连接
- 覆盖同编码已有应用（仍强制改名新建）
- Flowable 运行时历史实例

## 协议（v2）

在 v1 基础上增加：

```json
{
  "schemaVersion": 2,
  "application": { "aiAssistantConfig": {} },
  "objects": [{ "documentConfig": {} }],
  "entries": [],
  "bindings": [],
  "processes": [{ "processCode": "", "subjectObjectCode": "", "businessProcessJson": {} }],
  "extensions": [{ "extensionCode": "", "content": "" }],
  "triggers": [],
  "printing": { "templates": [], "bindings": [], "warnings": [] }
}
```

导入兼容 `schemaVersion` 1 与 2。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `.../debug-bundle/pages` | 页面清单 |
| GET | `.../debug-bundle/export?pageIds=` | 导出完整设计态 |
| POST | `.../debug-bundle/import?autoPublish=` | 导入；可选发布 |

## 验收（P2）

1. 含流程+打印+扩展的应用导出包内对应数组非空（若源有配置）。
2. 导入后可打开页面、对象设计器、流程草稿、打印模板/绑定（允许 warnings）。
3. 勾选 autoPublish 时成功则 `published=true`；失败不影响设计态。
4. 单测 + 编译通过。
