package com.mdframe.forge.starter.plugin.license;

/** 序号对应启动时配置顺序；无效文件不保留任何未经验证的内容。 */
record LoadedRuntimeLicense(int position, LicensePayload payload) {
    RuntimeLicenseReport.Entry view(LicensePayload.Binding binding, long now) {
        RuntimeLicenseState state = RuntimeLicenseState.evaluate(binding, payload, now);
        if (state == RuntimeLicenseState.INVALID || state == RuntimeLicenseState.BINDING_MISMATCH) {
            return new RuntimeLicenseReport.Entry(position, state.getCode(), null, null, null);
        }
        return new RuntimeLicenseReport.Entry(position, state.getCode(), payload.licenseId(),
                payload.scope(), payload.terms());
    }
}
