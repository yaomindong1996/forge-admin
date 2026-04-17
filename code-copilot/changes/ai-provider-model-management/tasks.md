# 任务拆分 — AI供应商与模型管理
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层
> 每个任务 = 可独立提交的原子变更（3-5 个文件）

## 前置条件
- [x] forge-plugin-ai 模块已存在
- [x] AiProvider 实体和 CRUD 接口已存在
- [x] 前端 AiCrudPage 组件可用

## Task 1: 新建 ai_model 表 DDL 及数据迁移脚本 ✅
- **状态**: 已完成
- **实际文件**:
  - `forge-plugin-ai/src/main/resources/sql/ai_model.sql` — 新增，建表DDL + 菜单数据(sys_resource) + 数据迁移脚本
- **备注**: 菜单表实际为 sys_resource（非 sys_menu），已在 Task 1 中一并完成菜单和权限数据插入

## Task 2: 新增 AiModel 实体、Mapper、Service ✅
- **状态**: 已完成
- **实际文件**:
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/domain/AiModel.java` — 新增
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/mapper/AiModelMapper.java` — 新增
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/service/AiModelService.java` — 新增，含双写同步

## Task 3: 新增 AiModelController 接口层 ✅
- **状态**: 已完成
- **实际文件**:
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/model/controller/AiModelController.java` — 新增

## Task 4: 修改 AiProviderService 和 AiProviderController 兼容逻辑 ✅
- **状态**: 已完成
- **实际文件**:
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/service/AiProviderService.java` — 修改，新增 deleteProvider() 含关联模型校验
  - `forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/provider/controller/AiProviderController.java` — 修改，page/getById 聚合填充 models/defaultModel

## Task 5: 前端 API 层及供应商管理页面 ✅
- **状态**: 已完成
- **实际文件**:
  - `forge-admin-ui/src/api/ai.js` — 新增
  - `forge-admin-ui/src/views/ai/provider.vue` — 新增

## Task 6: 前端模型管理页面 ✅
- **状态**: 已完成
- **实际文件**:
  - `forge-admin-ui/src/views/ai/model.vue` — 新增

## Task 7: 插入菜单数据 ✅
- **状态**: 已完成（合并到 Task 1）
- **备注**: 菜单数据已包含在 ai_model.sql 中，使用 sys_resource 表（非 sys_menu），包含目录、菜单、按钮权限和角色分配
