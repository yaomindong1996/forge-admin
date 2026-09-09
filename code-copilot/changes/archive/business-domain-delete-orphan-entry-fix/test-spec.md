# 测试 Spec — 业务域删除孤立应用入口修复
> status: completed
> created: 2026-08-05

## 0. 测试原则

- 使用 Red/Green 固定用户反馈场景，先证明旧实现会无条件阻断入口。
- 不连接真实数据库；以 Service 单测、Mapper XML 静态检查和构建形成最小闭环。
- 前端删除入口是薄交互层，执行目标 ESLint 与生产构建。

## 1. P0 核心场景

| 场景 | 输入/Mock | 预期 |
|------|-----------|------|
| 清理孤立入口 | 入口数 2、有效应用引用 0、确认=true | 停用入口菜单，逻辑删除入口，再删除业务域 |
| 未确认入口 | 入口数 2、确认=false | 提示包含“2 个访问入口”和“确认”，不执行清理 |
| 入口仍绑定有效应用 | 入口数 1、有效应用引用 1 | 失败关闭，不删除入口、对象或业务域 |
| 入口与对象组合清理 | 入口数 1、对象数 2、确认=true | 入口 → 对象关系 → 对象 → 业务域 |
| 空业务域 | 入口/对象/应用均为 0 | 无需清理确认，直接删除业务域 |

## 2. P1 数据访问契约

- 有效入口引用查询 JOIN `ai_business_application`，同时过滤入口和应用 `del_flag=0`。
- 入口逻辑删除限定 `tenant_id + suite_code + del_flag=0`，写 `status=0, del_flag=id`。
- 不新增物理删除入口 SQL，不修改历史 Flyway。

## 3. 计划命令

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator \
  -Dtest=BusinessSuiteServiceTest test
```

```bash
xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/resources/mapper/BusinessSuiteMapper.xml
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

```bash
source ~/.nvm/nvm.sh
nvm use v20.19.0
pnpm exec eslint src/views/app-center/index.vue 'src/views/app-center/suite.[suiteCode].vue'
pnpm build
```

```bash
git diff --check
```

## 4. 跳过项

- 不启动 Admin、MySQL 或 Redis，不执行真实数据删除。
- 部署环境需验证：删除最后一个业务应用后，业务域确认框展示孤立入口数量并可成功删除；刷新后业务域、入口均不可见。

## 5. 执行结果

| 级别 | 验证 | 结果 |
|------|------|------|
| Red | 旧实现运行新增入口场景 | 8 个用例中 2 failures + 1 error，均命中旧的入口硬阻断 |
| P0 | `BusinessSuiteServiceTest` | 9/9 passed，0 failures/errors/skipped |
| P1 | `BusinessSuiteMapper.xml` | `xmllint --noout` passed |
| 后端 | Generator reactor compile | 30/30 modules passed |
| 前端 | 目标 ESLint | 0 errors，3 条既有 `vue/attributes-order` warnings |
| 前端 | `pnpm build` | passed，8848 modules transformed |
| 通用 | `git diff --check` | passed |
