package com.mdframe.forge.plugin.system.vo;

import com.mdframe.forge.starter.plugin.license.RuntimeLicenseReport;

/** 全实例授权安全读视图，不包含文件内容或位置。 */
public record RuntimeLicenseStatusVO(String mode, String edition, RuntimeLicenseReport report) {
}
