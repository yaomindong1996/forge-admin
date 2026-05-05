# Flowable SPEL 表达式动态审批人使用指南

## 概述

本系统支持使用 Spring Expression Language (SPEL) 表达式动态计算流程节点的审批人，实现灵活的审批路由逻辑。

## 功能特性

- ✅ 根据流程变量动态计算审批人
- ✅ 支持复杂的条件判断和三元表达式
- ✅ 提供常用的查询方法（部门负责人、角色用户、区域负责人等）
- ✅ 支持会签场景的多审批人合并
- ✅ 前端提供表达式模板和语法验证

## 前端使用

### 1. 在流程设计器中配置

1. 打开流程设计器，选择用户任务节点
2. 在右侧属性面板中，选择"审批人类型"为 **SPEL 表达式**
3. 点击"配置 SPEL 表达式"按钮
4. 在弹窗中可以：
   - 从模板列表选择常用表达式
   - 手动编写自定义表达式
   - 查看语法提示和示例
5. 表达式会实时验证语法错误
6. 保存后，表达式会写入 BPMN XML 的 `flowable:assignee` 属性

### 2. 表达式模板

系统提供以下预定义模板：

| 模板名称 | 表达式 | 说明 |
|---------|--------|------|
| 根据部门查找负责人 | `${flowSpelService.findDeptManager(execution.getVariable("deptId"))}` | 查找指定部门的负责人 |
| 根据角色查找用户 | `${flowSpelService.findUsersByRole(execution.getVariable("roleKey"))}` | 查找具有指定角色的所有用户（会签） |
| 根据行政区划查找负责人 | `${flowSpelService.findRegionManager(execution.getVariable("regionCode"))}` | 查找指定行政区划的区域负责人 |
| 发起人的直属上级 | `${flowSpelService.getInitiatorLeader(execution)}` | 流程发起人的直属上级 |
| 根据订单金额动态审批 | `${execution.getVariable("amount") > 10000 ? flowSpelService.findUsersByRole("finance_manager") : flowSpelService.findUsersByRole("finance_staff")}` | 金额大于1万由财务经理审批，否则由财务专员审批 |
| 部门+角色组合查询 | `${flowSpelService.findUsersByDeptAndRole(execution.getVariable("deptId"), "dept_manager")}` | 查找指定部门的部门经理 |
| 根据业务规则查找 | `${flowSpelService.findApproverByBusinessRule(execution, "order")}` | 根据复杂业务规则动态确定审批人 |

## 后端实现

### 1. FlowSpelService 服务

后端提供 `FlowSpelService` 服务类，注册为 Spring Bean，名称为 `flowSpelService`。

**位置：** `com.mdframe.forge.starter.flow.service.FlowSpelService`

### 2. 可用方法

#### 基础查询方法

```java
// 根据部门ID查找部门负责人
String findDeptManager(Object deptId)

// 根据角色标识查找所有具有该角色的用户（返回逗号分隔的用户ID列表）
String findUsersByRole(Object roleKey)

// 根据行政区划代码查找区域负责人
String findRegionManager(Object regionCode)

// 根据用户ID查找其直属上级
String findUserLeader(Object userId)

// 根据部门ID和角色标识查找用户
String findUsersByDeptAndRole(Object deptId, Object roleKey)
```

#### 流程上下文方法

```java
// 获取流程发起人
String getInitiator(DelegateExecution execution)

// 获取流程发起人的上级
String getInitiatorLeader(DelegateExecution execution)

// 根据业务规则动态查找审批人
String findApproverByBusinessRule(DelegateExecution execution, String businessType)
```

#### 工具方法

```java
// 条件选择审批人
String conditionalAssignee(boolean condition, String trueValue, String falseValue)

// 合并多个审批人列表（用于会签）
String mergeAssignees(String... assigneeLists)
```

### 3. 实现自定义查询逻辑

`FlowSpelService` 中的方法目前返回 `null`，需要根据实际业务实现查询逻辑。

**示例：实现部门负责人查询**

```java
@Autowired
private SysDeptService deptService;

public String findDeptManager(Object deptId) {
    log.info("SPEL: 查找部门负责人, deptId={}", deptId);
    
    if (deptId == null) {
        log.warn("SPEL: 部门ID为空，无法查找负责人");
        return null;
    }
    
    try {
        SysDept dept = deptService.getById(deptId.toString());
        if (dept != null && dept.getManagerId() != null) {
            log.info("SPEL: 找到部门负责人: managerId={}", dept.getManagerId());
            return dept.getManagerId().toString();
        }
        log.warn("SPEL: 未找到部门负责人: deptId={}", deptId);
        return null;
    } catch (Exception e) {
        log.error("SPEL: 查找部门负责人失败", e);
        return null;
    }
}
```

**示例：实现角色用户查询（会签场景）**

```java
@Autowired
private SysUserService userService;

public String findUsersByRole(Object roleKey) {
    log.info("SPEL: 查找角色用户, roleKey={}", roleKey);
    
    if (roleKey == null) {
        log.warn("SPEL: 角色标识为空，无法查找用户");
        return null;
    }
    
    try {
        List<SysUser> users = userService.findUsersByRole(roleKey.toString());
        if (users != null && !users.isEmpty()) {
            String result = users.stream()
                .map(u -> u.getId().toString())
                .collect(Collectors.joining(","));
            log.info("SPEL: 找到角色用户: count={}, userIds={}", users.size(), result);
            return result;
        }
        log.warn("SPEL: 未找到角色用户: roleKey={}", roleKey);
        return null;
    } catch (Exception e) {
        log.error("SPEL: 查找角色用户失败", e);
        return null;
    }
}
```

## 使用示例

### 示例 1：根据部门动态分配审批人

**场景：** 订单审批流程，需要由订单所属部门的负责人审批

**流程变量：**
- `deptId`: 订单所属部门ID

**SPEL 表达式：**
```
${flowSpelService.findDeptManager(execution.getVariable("deptId"))}
```

**启动流程时传入变量：**
```java
Map<String, Object> variables = new HashMap<>();
variables.put("deptId", "1001");
runtimeService.startProcessInstanceByKey("order_approval", variables);
```

### 示例 2：根据金额动态选择审批人

**场景：** 报销流程，金额小于5000由部门经理审批，大于5000由财务总监审批

**流程变量：**
- `amount`: 报销金额

**SPEL 表达式：**
```
${execution.getVariable("amount") > 5000 ? flowSpelService.findUsersByRole("finance_director") : flowSpelService.findUsersByRole("dept_manager")}
```

### 示例 3：多级审批链

**场景：** 请假流程，根据请假天数确定审批层级

**流程变量：**
- `days`: 请假天数

**SPEL 表达式：**
```
${execution.getVariable("days") <= 3 ? flowSpelService.getInitiatorLeader(execution) : flowSpelService.findUsersByRole("hr_manager")}
```

### 示例 4：会签场景

**场景：** 合同审批，需要法务部所有人员会签

**流程变量：**
- `deptId`: 法务部门ID

**SPEL 表达式：**
```
${flowSpelService.findUsersByRole("legal")}
```

**说明：** 返回逗号分隔的用户ID列表，Flowable 会自动创建多个任务实例

## 表达式语法

### 1. 访问流程变量

```
${execution.getVariable("variableName")}
```

### 2. 调用服务方法

```
${flowSpelService.methodName(param1, param2)}
```

### 3. 三元表达式

```
${condition ? trueValue : falseValue}
```

### 4. 逻辑运算

```
${var1 > 100 && var2 < 200}
${var1 == 'value' || var2 != null}
```

### 5. 字符串操作

```
${execution.getVariable("name").toUpperCase()}
${execution.getVariable("code").substring(0, 3)}
```

## 注意事项

1. **空值处理**：表达式应该处理变量为 null 的情况，避免 NullPointerException
2. **返回值格式**：
   - 单个审批人：返回用户ID字符串，如 `"1001"`
   - 多个审批人（会签）：返回逗号分隔的用户ID列表，如 `"1001,1002,1003"`
3. **性能考虑**：避免在表达式中执行耗时操作，建议将复杂逻辑封装到服务方法中
4. **日志记录**：`FlowSpelService` 中的方法会记录详细日志，便于调试
5. **错误处理**：如果表达式执行失败或返回 null，任务将无法分配审批人，流程会挂起

## 调试技巧

### 1. 查看日志

`FlowSpelService` 的所有方法都会记录日志，格式为：

```
SPEL: 查找部门负责人, deptId=1001
SPEL: 部门负责人查询结果: 2001
```

### 2. 测试表达式

可以在流程启动前，先测试表达式是否正确：

```java
@Autowired
private FlowSpelService flowSpelService;

@Test
public void testSpelExpression() {
    String managerId = flowSpelService.findDeptManager("1001");
    System.out.println("部门负责人: " + managerId);
}
```

### 3. 查看 BPMN XML

保存流程后，可以查看 BPMN XML 文件，确认表达式是否正确写入：

```xml
<userTask id="task1" name="部门审批" flowable:assignee="${flowSpelService.findDeptManager(execution.getVariable('deptId'))}">
  ...
</userTask>
```

## 常见问题

### Q1: 表达式执行后任务没有分配审批人？

**A:** 检查以下几点：
1. `FlowSpelService` 方法是否返回了有效的用户ID
2. 返回的用户ID是否存在于系统中
3. 查看日志，确认表达式是否正确执行
4. 检查流程变量是否正确传入

### Q2: 如何支持动态候选人（而不是指定审批人）？

**A:** 使用 `flowable:candidateUsers` 属性代替 `flowable:assignee`，表达式返回逗号分隔的用户ID列表。

### Q3: 表达式中可以访问 Spring Bean 吗？

**A:** 可以，只要 Bean 注册到 Spring 容器中，就可以通过 `@beanName.methodName()` 的方式调用。

### Q4: 如何实现更复杂的业务规则？

**A:** 建议将复杂逻辑封装到 `FlowSpelService` 的方法中，而不是在表达式中直接编写复杂逻辑。

## 扩展开发

如果需要添加新的查询方法，只需在 `FlowSpelService` 中添加 public 方法即可：

```java
@Service("flowSpelService")
public class FlowSpelService {
    
    /**
     * 自定义查询方法
     */
    public String customQuery(Object param) {
        // 实现查询逻辑
        return "userId";
    }
}
```

然后在前端的 SPEL 模板中添加对应的模板：

```javascript
{
  label: '自定义查询',
  value: '${flowSpelService.customQuery(execution.getVariable("param"))}',
  description: '自定义查询说明',
}
```

## 总结

SPEL 表达式功能提供了灵活的审批人动态计算能力，可以满足各种复杂的业务场景。通过合理使用表达式和服务方法，可以实现高度可配置的流程审批路由。
