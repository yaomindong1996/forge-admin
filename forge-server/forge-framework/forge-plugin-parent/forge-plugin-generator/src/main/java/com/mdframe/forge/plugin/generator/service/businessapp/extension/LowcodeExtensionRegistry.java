package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 服务端扩展显式注册表，不提供 Bean 名或类名解析入口。
 */
@Component
public class LowcodeExtensionRegistry {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");

    private final Map<String, LowcodeExtensionHandler> handlers;

    public LowcodeExtensionRegistry(List<LowcodeExtensionHandler> handlerList) {
        Map<String, LowcodeExtensionHandler> registry = new LinkedHashMap<>();
        if (handlerList != null) {
            for (LowcodeExtensionHandler handler : handlerList) {
                String code = StringUtils.trimToEmpty(handler.handlerCode());
                if (!CODE_PATTERN.matcher(code).matches()) {
                    throw new IllegalStateException("低代码扩展 handlerCode 格式不正确: " + code);
                }
                if (registry.putIfAbsent(code, handler) != null) {
                    throw new IllegalStateException("重复注册低代码扩展处理器: " + code);
                }
            }
        }
        this.handlers = Collections.unmodifiableMap(registry);
    }

    public Optional<LowcodeExtensionHandler> find(String handlerCode) {
        String code = StringUtils.trimToEmpty(handlerCode);
        if (!CODE_PATTERN.matcher(code).matches()) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(code));
    }

    public LowcodeExtensionHandler require(String handlerCode) {
        return find(handlerCode)
                .orElseThrow(() -> new BusinessException("未注册或不允许的服务端扩展处理器"));
    }

    public Map<String, LowcodeExtensionHandler> registeredHandlers() {
        return handlers;
    }
}
