# Lowcode App Designer Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 分阶段修复低代码应用创建过渡、业务编码唯一性、左树右表/树形运行时和子表字段排序，使设计态与运行态协议一致。

**Architecture:** 阶段一从现有应用运行页和业务对象保存链路补齐加载状态与后端唯一校验，不引入新的业务对象概念。阶段二/三统一树配置归一化和树接口协议，复用已有 TreeCrudTemplate；阶段四把子表展示与筛选顺序作为 schema 数组，设计器、运行态和流程任务表单共享编译结果。

**Tech Stack:** Vue 3、Pinia、Naive UI、Vite/Vitest、Spring Boot 3、MyBatis-Plus、Mapper XML、Flowable low-code runtime。

---

## 阶段一：创建体验与编码校验

1. 确认 `objectCode` 的租户唯一语义和页面编码的应用唯一语义，复用现有对象 Mapper XML，不在 Service 里拼复杂查询。
2. 增加 typed 校验 DTO/VO（若现有接口可复用则扩展现有 DTO），Service 在 create/update 事务内执行最终冲突检查，异常返回稳定错误码。
3. 前端创建向导对业务编码做防抖异步校验，保存时仍依赖后端兜底；编辑当前记录传 `excludeId`。
4. 应用运行页将新建页面流程拆为“创建中、加载设计资源、设计器就绪、失败可重试”状态，使用现有 `ApplicationRuntimeSkeleton`，修正父容器高度和 `min-height:0`。
5. 增加前端 Vitest/源码结构测试和后端 Service 单测或编译验证，更新 execution-log。

## 阶段二：设计器模板与配置迁移

1. 在 page-schema/列表设计器建立 `normalizeTreeConfig`，兼容旧 `keyField/labelField/parentField/filterField/nodeValueField` 等字段，输出用户字段与高级字段分层结构。
2. 复核 `simple-crud` 与 `tree-crud` 模板生成、切换、保存、重新打开，模板切换不覆盖用户字段目录和表单绑定。
3. 树配置面板只展示业务文案，提供字段选择器和加载方式说明；技术字段放入高级折叠区。
4. 后端运行配置构建器和发布保存都调用同一归一化规则，保留旧草稿回读能力。

## 阶段三：树形运行时协议

1. 固化 `GET /tree` 协议：全量模式接收基础查询条件并返回扁平或嵌套节点；懒加载接收字符串 `parentValue` 并返回当前层节点及 `isLeaf`。
2. TreeCrudTemplate 仅用 `nodeValueField` 产生右表过滤值，用 `filterField` 产生右表查询字段；不得默认把父级字段当节点值。
3. 节点过滤使用标准 `publicParams`/`_searchTypes` 结构，清空节点删除树注入条件；树请求失败只显示树错误，不吞掉右表基础数据。
4. DynamicCrudController/Service 在树查询中复用租户、逻辑删除和数据权限条件，补接口级测试。

## 阶段四：子表排序

1. 定义 `childTable.displayFields` 与 `childTable.filterFields` 为有序字段编码数组；旧 `fieldRefs`/无序集合按字段目录稳定排序迁移。
2. 设计器使用现有拖拽依赖和 Pinia formDesigner store，字段行竖向展示，提供拖拽手柄及键盘上下移动。
3. 编译运行态 schema 时按数组顺序生成列、表单项和筛选项；流程任务表单编译复用相同 helper。
4. 增加顺序稳定性、未知字段过滤和跨渲染链路一致性测试。

## Verification

- 每阶段先执行 `git diff --check` 和目标模块编译/前端构建。
- 前端修改使用 Node 20.19.0 且所有 pnpm 命令带 `--ignore-workspace`。
- 不启动真实服务和数据库；接口/E2E 需要环境时明确记录跳过原因。
- 完成每阶段后回填 `tasks.md`、`test-spec.md` 与 `execution-log.md`。

