package com.mdframe.forge.plugin.capability.identity.authorization;

import com.mdframe.forge.plugin.capability.model.CapabilityDefinition;

/**
 * 将能力编码映射到 Forge 按钮权限。后续安全动作可以替换该 Bean，
 * 但默认映射保持稳定且不从客户端参数推导。
 */
public class ForgeCapabilityPermissionMapper {

    public String discoveryPermission(CapabilityDefinition definition) {
        return "ai:capability:discover:" + definition.capabilityCode();
    }

    public String invocationPermission(CapabilityDefinition definition) {
        return "ai:capability:invoke:" + definition.capabilityCode();
    }
}
