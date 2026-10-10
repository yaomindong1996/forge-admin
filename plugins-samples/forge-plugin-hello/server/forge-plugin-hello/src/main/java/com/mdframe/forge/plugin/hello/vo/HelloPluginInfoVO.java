package com.mdframe.forge.plugin.hello.vo;

/** 插件发布版本与宿主核心版本分开呈现，不能用 Maven 构件版本代替插件版本。 */
public record HelloPluginInfoVO(String id, String version, String coreVersion) {
}
