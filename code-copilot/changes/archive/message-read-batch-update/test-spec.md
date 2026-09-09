# 测试 Spec — 消息已读批量更新优化
> status: completed
> created: 2026-08-05

## 1. P0 服务行为

| 场景 | 输入 | 预期 |
|------|------|------|
| 全部已读 | tenantId=1, userId=42 | 只调用一次 `markAllMessagesRead(1, 42, readTime)` |
| 批量已读 | messageIds=[11,12], tenantId=1, userId=42 | 只调用一次 `markMessagesReadBatch(1, 42, ids, readTime)` |
| 空批量 | messageIds=[] | 不调用 Mapper |

## 2. P1 SQL 契约

- 全部已读 SQL 包含 `tenant_id = #{tenantId}`、`user_id = #{userId}`、`read_flag = 0`。
- 批量已读 SQL 额外包含参数化 `message_id IN (...)`。
- 两条 SQL 均设置 `read_flag = 1` 与统一 `read_time`。
- Service 中两条链路不再出现 `selectList` 和 `receivers.forEach(receiverMapper::updateById)`。

## 3. 计划命令

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-message \
  -Dtest=MessageServiceImplTest test
```

```bash
cd forge-server
xmllint --noout forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageReceiverMapper.xml
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-message -am compile -DskipTests
```

```bash
git diff --check
```

## 4. 跳过项

- 不启动 Admin/MySQL/Redis，不修改真实消息已读状态。
- 本轮不修改前端；已静态确认两个实际入口均只调用一次后端批量接口。

## 5. 本轮增量验证结果

| 级别 | 验证项 | 结果 |
|------|--------|------|
| P0 | Red：新增测试编译 | 按预期失败，Mapper 尚不存在两个批量方法 |
| P0 | Green：`MessageServiceImplTest` | 5 tests，0 failures，0 errors，0 skipped |
| P1 | Mapper XML 语法 | `xmllint --noout` 通过 |
| P1 | Message 插件聚合编译 | 27/27 reactor modules `SUCCESS` |
| P1 | 差异检查 | `git diff --check` 通过 |

运行态接口与真实数据库更新未执行，原因是本轮按用户偏好只做代码、自动化测试和静态/聚合构建验证。
