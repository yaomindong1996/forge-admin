# 低代码 Java 服务增强开发指南

Forge 的 Java 服务增强用于把已经开发、评审和部署的 Java 业务能力，绑定到低代码应用的标准触发点。平台不会在线编译 Java，也不接受 Bean 名、Class 名或反射路径。

## 1. 开发流程

1. 在后端业务模块实现 `LowcodeExtensionHandler`。
2. 使用 `@Component` 注册为 Spring Bean。
3. 声明稳定的处理器编码、允许钩子、输入输出结构、超时、风险和权限。
4. 完成代码评审、测试并随服务发布。
5. 重启服务后，在“应用工作台 → 动作与增强 → Java 服务增强”中选择处理器。
6. 保存增强草稿，执行受限测试，通过后再启用。

## 2. 处理器模板

```java
package com.example.forge.extension;

import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionContext;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionResult;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionInputField;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Component
public class PurchaseAmountCheckHandler implements LowcodeExtensionHandler {

    @Override
    public String handlerCode() {
        return "purchase_amount_check";
    }

    @Override
    public String handlerName() {
        return "采购金额校验";
    }

    @Override
    public Set<String> allowedHooks() {
        return Set.of("BEFORE_CREATE", "BEFORE_UPDATE", "BEFORE_SUBMIT");
    }

    @Override
    public Map<String, ExtensionInputField> inputSchema() {
        return Map.of(
                "amount", ExtensionInputField.required("NUMBER"),
                "currency", ExtensionInputField.optional("STRING")
        );
    }

    @Override
    public Map<String, ExtensionInputField> outputSchema() {
        return Map.of("normalizedAmount", ExtensionInputField.required("NUMBER"));
    }

    @Override
    public int timeoutMs() {
        return 800;
    }

    @Override
    public String riskLevel() {
        return "MEDIUM";
    }

    @Override
    public String requiredPermission() {
        return "purchase:order:edit";
    }

    @Override
    public ExtensionExecutionResult execute(ExtensionExecutionContext context) {
        Number inputAmount = (Number) context.getInput().get("amount");
        BigDecimal amount = new BigDecimal(inputAmount.toString());
        if (amount.signum() < 0) {
            return ExtensionExecutionResult.failure("AMOUNT_NEGATIVE", "采购金额不能小于零");
        }
        return ExtensionExecutionResult.success(Map.of("normalizedAmount", amount));
    }
}
```

## 3. 契约规则

- `handlerCode` 必须以小写字母开头，只包含小写字母、数字和下划线，长度 2～64。
- `allowedHooks` 必须使用 Forge 标准钩子；前端触发矩阵会自动禁用处理器未声明的钩子。
- 输入只允许处理器 `inputSchema` 声明的字段，未知字段会被拒绝。
- 支持的输入类型为 `STRING`、`LONG/INTEGER`、`NUMBER/DECIMAL`、`BOOLEAN`、`OBJECT/MAP`、`ARRAY/LIST`。
- 超时范围最终限制为 10～5000ms，输出不得超过 64KB。
- `requiredPermission` 非空时，当前用户必须拥有对应权限。
- 返回 `success=false` 会记录失败审计；失败策略为“阻断”时中止当前业务动作。
- 日志、返回信息和异常中不得包含密码、Token、身份证号、手机号等敏感数据。

## 4. 安全边界

- 不在低代码页面编写或编译 Java 源码。
- 不通过 Bean 名、Class 名或反射调用任意服务。
- 不在增强配置中保存 URL、Token、Cookie、AK/SK 或数据库密码。
- 数据库查询继续遵循 Forge Mapper XML、租户隔离和数据权限规则。
- 需要 SQL 能力时，应建设参数化只读查询或受控数据动作，不直接执行用户输入的任意 SQL。

## 5. 运行与审计

处理器由固定线程池执行。平台会验证租户、操作者、应用、对象、入口、版本、钩子和输入结构，并记录处理器编码、扩展版本、执行耗时、成功状态及脱敏错误摘要。
