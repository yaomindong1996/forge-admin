# Message Read Batch Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace message read-status N+1 updates with one tenant-scoped SQL update per batch/all-read request.

**Architecture:** Keep both HTTP contracts unchanged. Route `MessageServiceImpl` directly to two explicit `SysMessageReceiverMapper` update statements in Mapper XML, with tenant, user, unread state, and optional message ID boundaries enforced in SQL.

**Tech Stack:** Java 17, Spring transactions, MyBatis/MyBatis-Plus, Mapper XML, JUnit 5, Mockito.

---

### Task 1: Pin the Single-Update Contract

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/test/java/com/mdframe/forge/plugin/message/service/impl/MessageServiceImplTest.java`

- [x] **Step 1: Add failing service tests**

Add Mockito tests that construct `MessageServiceImpl` with a mocked `SysMessageReceiverMapper` and verify these exact calls:

```java
verify(receiverMapper).markAllMessagesRead(eq(1L), eq(42L), any(LocalDateTime.class));
verify(receiverMapper).markMessagesReadBatch(eq(1L), eq(42L), eq(List.of(11L, 12L)), any(LocalDateTime.class));
```

For `List.of()` verify `verifyNoInteractions(receiverMapper)`.

- [x] **Step 2: Run the tests and capture Red**

Run:

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-message \
  -Dtest=MessageServiceImplTest test
```

Expected: test compilation fails because the new Mapper batch methods do not exist yet, proving the old API cannot satisfy the single-update contract.

### Task 2: Implement Mapper-Level Batch Updates

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/mapper/SysMessageReceiverMapper.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageReceiverMapper.xml`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/java/com/mdframe/forge/plugin/message/service/impl/MessageServiceImpl.java`

- [x] **Step 1: Add Mapper methods**

```java
int markMessagesReadBatch(@Param("tenantId") Long tenantId,
                          @Param("userId") Long userId,
                          @Param("messageIds") List<Long> messageIds,
                          @Param("readTime") LocalDateTime readTime);

int markAllMessagesRead(@Param("tenantId") Long tenantId,
                        @Param("userId") Long userId,
                        @Param("readTime") LocalDateTime readTime);
```

- [x] **Step 2: Add explicit SQL updates**

```xml
<update id="markAllMessagesRead">
    UPDATE sys_message_receiver
    SET read_flag = 1,
        read_time = #{readTime}
    WHERE tenant_id = #{tenantId}
      AND user_id = #{userId}
      AND read_flag = 0
</update>
```

The batch variant uses the same conditions plus a guarded `<foreach>` `message_id IN (...)` list.

- [x] **Step 3: Replace entity loading and per-row updates**

```java
receiverMapper.markMessagesReadBatch(resolveTenantId(), userId, messageIds, LocalDateTime.now());
receiverMapper.markAllMessagesRead(resolveTenantId(), userId, LocalDateTime.now());
```

Keep the empty-list guard before the batch Mapper call.

- [x] **Step 4: Run Green tests**

Run the Task 1 Maven command again.

Expected: all `MessageServiceImplTest` cases pass and Mockito verifies one Mapper update per non-empty operation.

### Task 3: Verify and Deliver

**Files:**
- Modify: `code-copilot/changes/message-read-batch-update/spec.md`
- Modify: `code-copilot/changes/message-read-batch-update/tasks.md`
- Modify: `code-copilot/changes/message-read-batch-update/test-spec.md`
- Modify: `code-copilot/changes/message-read-batch-update/execution-log.md`
- Modify: `code-copilot/memory/pitfalls.md`

- [x] **Step 1: Run static and aggregate verification**

```bash
xmllint --noout forge-server/forge-framework/forge-plugin-parent/forge-plugin-message/src/main/resources/mapper/SysMessageReceiverMapper.xml
```

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-message -am compile -DskipTests
```

Expected: XML parses and the Maven reactor ends with `BUILD SUCCESS`.

- [x] **Step 2: Update change evidence**

Mark completed requirements and record exact test counts, build result, warnings, skipped runtime checks, and service cleanup state.

- [x] **Step 3: Stage only this change and commit**

Explicitly stage the four message plugin files, five change documents, and the memory entry. Exclude `.DS_Store`, run `git diff --cached --check`, then commit:

```bash
git commit -m '[message-read-batch-update] 优化消息已读批量更新'
```

Do not push.
