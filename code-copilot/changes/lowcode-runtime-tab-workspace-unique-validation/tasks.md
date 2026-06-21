# 低代码运行态多页签录入与唯一校验任务

- [x] 梳理现有 `AiCrudPage` 打开方式、低代码运行配置和动态 CRUD 保存链路。
- [x] 扩展 Spec、测试方案和执行日志。
- [x] 在 `AiCrudPageProps.js` 增加 `formOpenMode` 和 `tabWorkspace` 配置。
- [x] 在 `AiCrudPage.vue` 增加平铺面板和多页签工作区，复用现有表单渲染、保存和详情加载逻辑。
- [x] 在运行态 `crud-page.vue` 下发 `formOpenMode/tabWorkspace`，兼容旧 `modalType`。
- [x] 在表单设计器和列表设计器的打开方式配置中加入平铺、多页签。
- [x] 在 `LowcodeRuntimeConfigBuilder` 发布配置中输出新的打开方式配置。
- [x] 扩展 `LowcodeModelSchema` 增加唯一约束协议。
- [x] 扩展 `DynamicCrudRepository` 支持联合唯一存在性查询并排除当前 id。
- [x] 在 `DynamicCrudService.insert/updateById` 中执行唯一性校验。
- [x] 运行前后端增量验证并更新执行日志。
