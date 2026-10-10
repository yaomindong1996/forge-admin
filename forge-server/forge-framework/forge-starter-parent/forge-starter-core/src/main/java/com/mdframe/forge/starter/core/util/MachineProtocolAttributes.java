package com.mdframe.forge.starter.core.util;

/** 仅专用机器认证过滤器可设置；HTTP请求头不能成为加解密协议豁免依据。 */
public final class MachineProtocolAttributes {
    public static final String VERIFIED_JSON = MachineProtocolAttributes.class.getName() + ".verifiedJson";
    private MachineProtocolAttributes() {}
}
