package com.mdframe.forge.plugin.system.vo;

/** 只读版本身份：不包含环境配置、主机路径或租户授权信息。 */
public record SystemVersionVO(String version, String coreVersion, String edition, Build build) {
    public record Build(String service, String time, String commit) {
    }
}
