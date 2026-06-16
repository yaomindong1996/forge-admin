# 给低代码平台造一个公式引擎：从字段计算到对象图计算

> 我们如何在 Vue3 + Spring Boot 的低代码平台上，用 Aviator + DAG 依赖分析 + 函数市场，构建一套可配置、可调试、可扩展的公式体系。

## 一、低代码平台的"计算缺口"

做过 CRM、ERP、合同管理的同学一定见过这类需求：

- 订单总额 = 单价 × 数量
- 折扣金额 = 当金额 > 10000 时打 9 折，否则不打折
- 客户累计回款 = SUM(该客户所有回款单金额)
- 合同到期天数 = 到期日 - 今天

这些逻辑在传统开发里，写在 Service 层的 Java 代码里，每个业务对象一套。需求变更时改代码、改测试、改上线——哪怕只是把"10000"改成"8000"。

低代码平台的核心承诺是"配置即业务"。但如果你问一个低代码平台用户："你的金额自动计算怎么配？"，大概率会得到一个尴尬的回答：**"这个得写代码。"**

这就是我们面对的**计算缺口**：低代码平台能搭页面、能配 CRUD、能拖表单，但一涉及字段间的计算关系，就回到了写代码的老路。

我们的目标是：**让业务人员在字段设计器里点几下，就能配置"总额 = 单价 × 数量"，发布后自动生效，不需要写一行 Java。**

这篇文章记录了我们在 Forge 低代码平台上，从 V1 字段级公式到 V2 对象图公式的完整技术方案。如果你也在给自己的平台造公式引擎，希望这篇能给你一些参考。

---

## 二、V1：字段级公式体系

### 2.1 三种公式类型

我们定义了三种基础公式类型，覆盖 90% 的业务计算场景：

| 类型 | 用途 | 示例 |
|------|------|------|
| `CALC` | 同对象字段间算术计算 | `unit_price * quantity` |
| `AGGREGATE` | 从表字段聚合到主表 | `SUM(detail.amount)` |
| `CONDITIONAL` | 条件判断赋值 | `IF(amount > 10000, 0.1, 0)` |

每种类型对应一个 `FormulaType` 枚举：

```java
public enum FormulaType {
    CALC,           // 算术计算
    AGGREGATE,      // 聚合计算
    CONDITIONAL     // 条件计算
}
```

### 2.2 两种计算模式

公式不是只有"怎么算"，还有"什么时候算"：

| 模式 | 触发时机 | 存储 | 适用场景 |
|------|----------|------|----------|
| `VIRTUAL` | 查询时实时计算 | 不存库 | 计算频繁变化、不需要持久化 |
| `STORED` | 保存时计算并持久化 | 存库 | 需要排序/筛选/报表的字段 |

```java
public enum FormulaMode {
    VIRTUAL,  // 虚拟字段，读时算
    STORED    // 存储字段，写时算
}
```

**设计决策**：金额类字段默认用 `STORED`（需要排序和报表），天数类字段用 `VIRTUAL`（实时变化）。这个决策后来被写进了项目规范：`CON-002: 金额类公式计算结果使用 long 类型（单位：分）`。

### 2.3 公式配置协议

每个字段的公式配置存储为一个 JSON 对象：

```json
{
  "type": "CALC",
  "mode": "STORED",
  "expression": "unit_price * quantity",
  "dependsOn": ["unit_price", "quantity"],
  "maxDepth": 3
}
```

| 字段 | 说明 |
|------|------|
| `type` | 公式类型：CALC / AGGREGATE / CONDITIONAL |
| `mode` | 计算模式：VIRTUAL / STORED |
| `expression` | 表达式字符串（Aviator 语法） |
| `dependsOn` | 依赖的字段编码列表 |
| `maxDepth` | 最大嵌套深度（防止循环引用） |

聚合公式额外带一个 `aggregateConfig`：

```json
{
  "type": "AGGREGATE",
  "mode": "STORED",
  "expression": "",
  "aggregateConfig": {
    "function": "SUM",
    "detailObjectCode": "order_detail",
    "detailField": "amount",
    "masterField": "id",
    "detailForeignKey": "order_id"
  }
}
```

---

## 三、表达式引擎选型：为什么是 Aviator

### 3.1 方案对比

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| Aviator | 轻量、安全沙箱、性能好 | 函数扩展需注册 | ✅ 选它 |
| MVEL | 功能丰富 | 太重、有安全风险 | ❌ |
| Spring SpEL | Spring 原生 | 与 Spring 强绑定、表达式语法不够直观 | ❌ |
| 自研 | 完全可控 | 工作量大、容易出 bug | ❌ |

### 3.2 选型理由

1. **安全沙箱**：Aviator 默认不允许访问 Java 类的任意方法，只暴露白名单内的函数。这对低代码平台至关重要——你不想让用户通过表达式调用 `Runtime.exec("rm -rf /")`。

2. **性能**：Aviator 编译表达式为字节码，重复执行时不需要重新解析。在我们的压测中，单次表达式执行 < 0.1ms。

3. **轻量**：jar 包 500KB 左右，对 Spring Boot 应用的启动时间影响可忽略。

### 3.3 集成方式

我们封装了一个 `AviatorAdapter`，统一入参出参：

```java
public class AviatorAdapter {

    /**
     * 执行表达式
     * @param expression Aviator 表达式，如 "unit_price * quantity"
     * @param context 变量上下文，如 {"unit_price": 100, "quantity": 3}
     * @return 计算结果
     */
    public Object execute(String expression, Map<String, Object> context) {
        Expression compiled = AviatorEvaluator.compile(expression, true);
        return compiled.execute(new AviatorMapContext(context));
    }
}
```

`ExpressionExecutor` 负责组装上下文、调用 Aviator、处理异常：

```java
public class ExpressionExecutor {

    public ExecutionResult execute(FormulaConfig config, Map<String, Object> recordData) {
        try {
            // 1. 组装上下文：只取 dependsOn 中声明的字段
            Map<String, Object> context = buildContext(config.getDependsOn(), recordData);

            // 2. 调用 Aviator
            Object result = aviatorAdapter.execute(config.getExpression(), context);

            // 3. 金额类结果转 long（分）
            if (result instanceof Number) {
                result = ((Number) result).longValue();
            }

            return ExecutionResult.success(result, context);
        } catch (Exception e) {
            return ExecutionResult.error(e.getMessage(), context);
        }
    }
}
```

---

## 四、DAG 依赖分析与循环检测

### 4.1 问题：字段间的循环引用

假设有三个字段：

- `discount` = IF(amount > 10000, 0.1, 0)  → 依赖 `amount`
- `amount` = price * quantity  → 依赖 `price`, `quantity`
- `final_price` = amount * (1 - discount)  → 依赖 `amount`, `discount`

这是正常的依赖链。但如果有人配成：

- `a` = b + 1  → 依赖 `b`
- `b` = c + 1  → 依赖 `c`
- `c` = a + 1  → 依赖 `a`

死循环。在运行时无限递归，直到 StackOverflowError。

### 4.2 方案：拓扑排序 + 深度限制

我们的 `FormulaDependencyAnalyzer` 做两件事：

**1. 构建依赖图**

遍历对象所有字段的公式配置，提取 `dependsOn`，构建有向图：

```java
public class FormulaDependencyAnalyzer {

    public DependencyAnalysisResult analyze(List<GenTableColumn> columns) {
        Map<String, Set<String>> graph = new HashMap<>();

        for (GenTableColumn col : columns) {
            FormulaConfig config = parseFormulaConfig(col.getFormulaConfig());
            if (config != null && config.getDependsOn() != null) {
                graph.put(col.getJavaField(), new HashSet<>(config.getDependsOn()));
            }
        }

        return detectCycles(graph);
    }
}
```

**2. 拓扑排序检测循环**

用 Kahn 算法做拓扑排序。如果排序后的节点数 < 总节点数，说明存在环：

```java
private DependencyAnalysisResult detectCycles(Map<String, Set<String>> graph) {
    // 计算入度
    Map<String, Integer> inDegree = new HashMap<>();
    graph.keySet().forEach(node -> inDegree.put(node, 0));
    graph.values().forEach(deps ->
        deps.forEach(dep -> inDegree.merge(dep, 1, Integer::sum))
    );

    // BFS 拓扑排序
    Queue<String> queue = new LinkedList<>();
    inDegree.forEach((node, degree) -> {
        if (degree == 0) queue.add(node);
    });

    List<String> sorted = new ArrayList<>();
    while (!queue.isEmpty()) {
        String node = queue.poll();
        sorted.add(node);
        if (graph.containsKey(node)) {
            graph.get(node).forEach(dep -> {
                inDegree.merge(dep, -1, Integer::sum);
                if (inDegree.get(dep) == 0) queue.add(dep);
            });
        }
    }

    // 如果排序后节点数 < 总节点数，存在环
    if (sorted.size() < graph.size()) {
        Set<String> cycleNodes = new HashSet<>(graph.keySet());
        cycleNodes.removeAll(sorted);
        return DependencyAnalysisResult.cycle(cycleNodes, findCyclePath(graph, cycleNodes));
    }

    return DependencyAnalysisResult.success(sorted);
}
```

### 4.3 发布拦截

在 `BusinessObjectPublishService` 中，发布前必须过 DAG 校验：

```java
DependencyAnalysisResult result = formulaDependencyAnalyzer.analyze(columns);
if (result.hasCycle()) {
    throw new BusinessException(
        "公式存在循环依赖: " + String.join(" → ", result.getCyclePath())
    );
}
```

**设计决策**：`maxDepth = 3`。超过 3 层嵌套的公式，即使没有循环，也会给出警告。因为深层嵌套意味着业务逻辑过于复杂，应该拆解。

---

## 五、V2 可观测性底座

V1 上线后，用户反馈最多的问题是：**"公式算出来的值不对，但我不知道哪一步算错了。"**

这很正常。当一个对象有 20 个字段、其中 8 个有公式、4 个是嵌套公式时，出了问题你看到的只是一个最终值。要排查，只能在脑子里"人肉执行"整个依赖链。

V2 的第一优先级不是加新公式类型，而是**让公式可解释、可追踪、可排错**。

### 5.1 执行日志

每次公式执行，可选记录一条日志：

```java
public class AiFormulaExecutionLog {
    private String objectCode;      // 业务对象编码
    private String recordId;        // 记录 ID
    private String fieldCode;       // 字段编码
    private String formulaType;     // 公式类型
    private String mode;            // 计算模式
    private String expression;      // 表达式
    private String inputSnapshot;   // 输入变量快照 JSON
    private String outputValue;     // 输出值
    private Boolean success;        // 是否成功
    private String errorMessage;    // 错误信息
    private Long elapsedMs;         // 耗时毫秒
    private String traceId;         // 链路追踪 ID
}
```

**关键设计**：`inputSnapshot` 记录了公式执行时的所有变量值。出了问题，你可以看到"当时传进去了什么"，而不是只能猜。

### 5.2 公式调试器

调试器是执行日志的"交互版"。用户输入一组 `sampleValues`，调试器逐步执行并展示每一步：

```java
public class FormulaDebugger {

    public FormulaDebugResponse debug(FormulaDebugRequest request) {
        List<FormulaExecutionStep> steps = new ArrayList<>();
        Map<String, Object> context = new HashMap<>(request.getSampleValues());

        // 按拓扑序执行公式
        for (String fieldCode : request.getExecutionPlan()) {
            FormulaConfig config = getFormulaConfig(fieldCode);

            long start = System.currentTimeMillis();
            Object result = expressionExecutor.execute(config, context).getValue();
            long elapsed = System.currentTimeMillis() - start;

            steps.add(FormulaExecutionStep.builder()
                .fieldCode(fieldCode)
                .formulaType(config.getType())
                .expression(config.getExpression())
                .inputVariables(Map.copyOf(context))
                .outputValue(result)
                .elapsedMs(elapsed)
                .build());

            // 将结果加入上下文，供后续公式使用
            context.put(fieldCode, result);
        }

        return FormulaDebugResponse.builder()
            .executionPlan(request.getExecutionPlan())
            .steps(steps)
            .finalContext(context)
            .build();
    }
}
```

前端拿到 `steps` 后，展示为一个逐步执行的表格：

```
┌─────────────┬─────────┬──────────────────────┬─────────────┬─────────┐
│ 字段         │ 类型     │ 表达式                │ 输入         │ 输出     │
├─────────────┼─────────┼──────────────────────┼─────────────┼─────────┤
│ unit_price  │ -       │ (用户输入)             │ 100         │ 100     │
│ quantity    │ -       │ (用户输入)             │ 3           │ 3       │
│ amount      │ CALC    │ unit_price * quantity  │ price=100,qty=3 │ 300 │
│ discount    │ COND    │ IF(amount>1000,0.1,0)  │ amount=300  │ 0       │
│ final_price │ CALC    │ amount*(1-discount)    │ amt=300,disc=0 │ 300 │
└─────────────┴─────────┴──────────────────────┴─────────────┴─────────┘
```

### 5.3 依赖可视化

依赖图返回 `nodes` 和 `edges`，前端用 ECharts 或 D3 渲染：

```json
{
  "nodes": [
    { "id": "unit_price", "type": "FIELD", "label": "单价" },
    { "id": "quantity", "type": "FIELD", "label": "数量" },
    { "id": "amount", "type": "FORMULA", "label": "金额(CALC)" }
  ],
  "edges": [
    { "from": "unit_price", "to": "amount", "type": "DEPENDS_ON" },
    { "from": "quantity", "to": "amount", "type": "DEPENDS_ON" }
  ]
}
```

检测到循环时，`edges` 中会包含一条特殊边，`cyclePath` 字段列出完整循环路径：

```json
{
  "hasCycle": true,
  "cyclePath": ["a", "b", "c", "a"]
}
```

前端根据 `cyclePath` 高亮循环路径的节点和边，让用户一眼看到问题出在哪。

---

## 六、V2 LOOKUP 与跨对象公式

### 6.1 从"单对象计算"到"对象图计算"

V1 的公式只能在同一个业务对象内部计算。但真实业务场景往往是跨对象的：

- 订单的"客户等级" = 读取关联客户的 `level` 字段
- 合同的"负责人姓名" = 读取关联用户的 `realName` 字段
- 回款单的"客户累计回款" = 聚合该客户所有回款单的金额

这些场景需要公式能**跨越对象边界**，读取关联对象的数据。

### 6.2 LOOKUP 公式

LOOKUP 是最简单的跨对象公式：根据当前对象的字段值，去关联对象"查一下"拿回来。

配置示例：

```json
{
  "type": "LOOKUP",
  "mode": "VIRTUAL",
  "dependsOn": ["customer_id"],
  "lookup": {
    "relationCode": "order_customer",
    "targetObjectCode": "customer",
    "sourceField": "customer_id",
    "targetField": "id",
    "returnField": "level",
    "notFoundValue": null
  }
}
```

执行逻辑：

1. 取当前记录的 `customer_id` 值
2. 通过 `relationCode` 找到对象关系配置
3. 去 `customer` 表查 `WHERE id = #{customer_id}`，取 `level` 字段
4. 返回结果，未找到时返回 `notFoundValue`

**关键约束**：LOOKUP 必须基于已配置的对象关系，不能自由拼接表名。这是安全红线——防止用户通过公式注入任意 SQL。

### 6.3 跨对象路径

比 LOOKUP 更简洁的写法是路径表达式：

```json
{
  "type": "CALC",
  "mode": "VIRTUAL",
  "expression": "customer.level",
  "crossObject": {
    "paths": [
      { "field": "customer", "targetObject": "customer", "relation": "order_customer" }
    ]
  }
}
```

`customer.level` 会被解析为：先通过 `order_customer` 关系找到客户对象，再取 `level` 字段。

**一跳限制**：首期只支持一跳关系（`customer.level`），不支持多跳（`customer.manager.realName`）。多跳意味着每多一跳就多一次数据库查询，性能和复杂度都难以控制。

### 6.4 跨对象 DAG 校验

跨对象公式引入了新的循环风险：对象 A 的公式引用对象 B，对象 B 的公式又引用对象 A。

发布时，`FormulaDependencyAnalyzer` 会扩展到对象图级别：

```
对象A.field_x → (LOOKUP) → 对象B.field_y → (LOOKUP) → 对象A.field_z → 循环！
```

检测到跨对象循环时，发布阻断，返回完整路径。

---

## 七、V2 条件规则设计器

### 7.1 痛点：手写表达式门槛高

条件公式 `IF(amount > 10000, 0.1, 0)` 对开发者来说很直观，但对业务配置人员来说：

- 什么是 `IF`？
- `>` 还是 `>=`？
- 括号怎么嵌套？
- 多条件 AND/OR 怎么写？

### 7.2 方案：可视化规则 → 表达式

条件规则设计器让配置人员通过下拉选择构建条件树：

```
┌─────────────────────────────────────────────────────┐
│ 满足以下 [全部] 条件：                                  │
│ ┌───────────────────────────────────────────────────┐│
│ │ [订单金额] [大于] [10000]                          ││
│ └───────────────────────────────────────────────────┘│
│ ┌───────────────────────────────────────────────────┐│
│ │ 且 [客户等级] [等于] [VIP]                         ││
│ └───────────────────────────────────────────────────┘│
│                                                      │
│ 则：[0.1]  否则：[0]                                  │
└─────────────────────────────────────────────────────┘
```

设计器输出一个 JSON AST：

```json
{
  "operator": "AND",
  "rules": [
    { "field": "amount", "op": "GT", "value": 10000 },
    { "field": "customer_level", "op": "EQ", "value": "VIP" }
  ],
  "then": 0.1,
  "else": 0
}
```

后端将 AST 编译为 Aviator 表达式：

```
(amount > 10000 && customer_level == 'VIP') ? 0.1 : 0
```

**双向同步**：配置人员可以在"规则模式"和"表达式模式"之间切换。高级用户可以直接写表达式，切换回规则模式时自动解析。

---

## 八、V2 函数市场

### 8.1 从静态列表到函数生态

V1 的函数列表是硬编码的：`SUM`、`AVG`、`MAX`、`MIN`、`COUNT`。用户问："能不能加一个`MEDIAN`（中位数）？"答案是："等我们改代码。"

V2 引入函数市场，把函数变成可注册、可安装、可治理的"插件"。

### 8.2 函数元数据

每个函数有完整的元数据描述：

```java
public class AiFormulaFunction {
    private String functionCode;      // 唯一编码，如 "MEDIAN"
    private String displayName;       // 显示名称，如 "中位数"
    private String category;          // 分类，如 "统计"
    private String description;       // 描述
    private String argumentSchema;    // 参数 JSON Schema
    private String returnType;        // 返回类型
    private String example;           // 使用示例
    private String status;            // ENABLED / DISABLED
    private String source;            // BUILTIN / SYSTEM / TENANT / MARKET
}
```

### 8.3 函数生命周期

```
注册 → 安装 → 启用 → 禁用 → 卸载
         ↓
      版本管理（回滚到旧版本）
```

**安全约束**：

- 函数实现首期只支持 Java Bean 注册，不开放任意脚本执行
- 函数执行有超时限制（默认 100ms）
- 函数异常会被隔离，不影响其他公式执行
- 未启用的函数不能被新公式引用

### 8.4 与调试器/日志的集成

函数调用会自动接入执行日志和调试器。在调试器的 `steps` 中，函数调用会显示为一个独立步骤：

```
│ discount_rate │ FUNCTION │ MEDIAN(history_discounts) │ [0.1,0.2,0.15] │ 0.15 │
```

---

## 九、架构总结

### 9.1 整体架构

```
┌──────────────────────────────────────────────────────────────┐
│                        配置层                                  │
│  字段设计器  │  条件规则设计器  │  函数市场  │  公式预览         │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│                        校验层                                  │
│  语法校验  │  依赖分析(DAG)  │  循环检测  │  深度限制  │  发布拦截 │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│                        执行层                                  │
│  ExpressionExecutor  │  AviatorAdapter  │  AggregateEngine    │
│  LookupExecutor      │  CrossObjectResolver  │  FunctionExecutor│
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│                      可观测层                                  │
│  执行日志  │  公式调试器  │  依赖可视化  │  函数调用追踪       │
└──────────────────────────────────────────────────────────────┘
```

### 9.2 关键设计决策

| 决策 | 理由 |
|------|------|
| 不替换 Aviator | 够用、安全、性能好，换引擎成本高 |
| 不开放任意脚本 | 安全红线，函数只能 Java Bean 注册 |
| 一跳跨对象限制 | 多跳性能不可控，先验证一跳场景 |
| DAG 发布拦截 | 宁可阻断发布，不能运行时死循环 |
| 公式配置用 JSON | 与低代码体系一致，便于序列化/反序列化 |

### 9.3 与业界对比

| 能力 | Salesforce 公式 | 明道云公式 | Forge 公式 |
|------|----------------|-----------|-----------|
| 基础计算 | ✅ | ✅ | ✅ |
| 聚合公式 | ✅（Rollup） | ✅ | ✅ |
| 条件公式 | ✅ | ✅ | ✅ |
| 跨对象引用 | ✅（多跳） | ❌ | ✅（一跳） |
| 公式调试器 | ❌ | ❌ | ✅ |
| 执行日志 | ❌ | ❌ | ✅ |
| 函数市场 | ❌ | ❌ | ✅ |
| 依赖可视化 | ❌ | ❌ | ✅ |

### 9.4 后续展望

- **AI 公式生成**：用户描述"订单金额超过1万打9折"，AI 自动生成公式配置
- **规则推荐**：基于历史公式，推荐常用计算模式
- **多跳跨对象**：在性能可控的前提下，支持 2-3 跳路径

---

## 写在最后

造公式引擎这件事，最难的不是写代码，而是**想清楚边界**：

- 什么该配置化，什么该写代码？
- 什么时候算，存不存库？
- 跨对象能不能跨，跨几层？
- 函数能不能自定义，安不安全？

每一个决策都会影响后续半年的架构演进。如果你也在做类似的事情，希望这篇文章能帮你少走一些弯路。

代码已开源在 [Forge Admin](https://github.com/mdframe/forge-project)，欢迎 star 和交流。
