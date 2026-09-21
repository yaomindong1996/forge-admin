package com.mdframe.forge.plugin.print.spi;

import java.util.Map;

/**
 * 仅业务字段允许动态键值；框架再次投影，禁止把 Provider 原始对象直接返回。
 */
public record PrintData(Map<String, Object> main, Map<String, Object> children, Map<String, Object> flow) {
}
