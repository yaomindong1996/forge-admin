# 增量验证计划

- 复用版本可见性、Boot 4 T0–T6 和最近下载既有验证记录，不重新规划无关全量 E2E。
- 核对远端 v1.2.0 基线、目标标签未被使用、统计周内提交可达 main。
- 根 revision、独立 BOM、三端 package.json 一致；只同步宿主发行版本，不改原生 manifest/插件独立版本。
- Node 版本工具测试：增加独立 BOM 漂移、同步与检查回归；精确 1.2.1 随包说明不能回退到 Unreleased。
- PC/H5 版本展示相关测试；后端版本服务/控制器测试需显式启用 -Penable-tests。
- 本地验证构建与资源处理，核对实际 version.json/build-info；脏工作区产物不当成正式制品。
- SQL 版本号查重及 git diff --check；不执行迁移，不访问真实模型、数据库、Redis 或存储。
- 图片检查中文、版本号、裁切与已发布/上线等事实性文案，必须与社群稿和源码一致。
- 原有全量前端 25 项基线失败单独披露；未复跑不写本轮通过。

## 实际结果

- Node 版本与 BOM 防漂移：8 项；PC 版本/关于系统 7 项；H5 版本 2 项，均通过。
- 后端显式 -Penable-tests：ForgeVersionTest 38 项、SystemVersionService/Controller 6 项、
  BusinessExceptionDefaultCodeTest 3 项，均通过；以上定向测试合计 64 项，不重复累计首次/复跑。
- 后端四服务 51 模块聚合 package 通过；PC、H5、Report 构建通过。
- 四个 JAR build-info 与框架核心资源版本为 1.2.1；三端 version.json 为 1.2.1，说明只取精确版本段。
- 版本工具、开源边界与空白检查通过；Node 语法检查通过。
- 前端 ESLint 配置不适用于根 Node 脚本（node:test 被误要求改为 Vitest），复跑暴露 33 项诊断，
  不计为 ESLint 通过，也不为了消除诊断改测试运行器/降低规则；本轮按 Node 测试与语法验证。
- 海报 1086×1448 PNG，检查了 V1.2.1、中文、更新周期与发布准备提示；没有虚构上线/性能/二维码。
- 未做全量前端重跑、真实服务启动、数据库/缓存/模型/存储、插件客户工程和部署 E2E；
  原有全量失败与正式发布前门禁仍保留。
