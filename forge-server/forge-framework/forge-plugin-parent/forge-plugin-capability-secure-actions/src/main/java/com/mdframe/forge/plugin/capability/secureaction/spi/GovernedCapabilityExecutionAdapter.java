package com.mdframe.forge.plugin.capability.secureaction.spi;

import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;

import java.util.Map;

/**
 * 固定 capability.invoke 元工具的受控能力执行扩展点。
 *
 * <p>适配器只能处理已经通过可信身份、实时授权、Schema 和 elicitation 的能力，
 * 但仍必须在副作用前重新校验自己的业务状态和来源快照。</p>
 */
public interface GovernedCapabilityExecutionAdapter {

    boolean supports(SecureActionDescriptor descriptor);

    void validate(SecureActionDescriptor descriptor, Map<String, Object> input);

    Map<String, Object> execute(
            SecureActionDescriptor descriptor,
            Map<String, Object> input,
            String requestId);
}
