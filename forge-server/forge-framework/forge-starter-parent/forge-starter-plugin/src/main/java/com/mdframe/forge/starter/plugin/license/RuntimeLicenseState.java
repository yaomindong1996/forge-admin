package com.mdframe.forge.starter.plugin.license;

/** 诊断与 Gate 共用状态，不能出现页面有效而业务不放行的两套规则。 */
public enum RuntimeLicenseState {
    VALID("valid"),
    NOT_YET_VALID("not_yet_valid"),
    EXPIRED("expired"),
    BINDING_MISMATCH("binding_mismatch"),
    INVALID("invalid");

    private final String code;

    RuntimeLicenseState(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean matches(String value) {
        return code.equals(value);
    }

    static RuntimeLicenseState evaluate(LicensePayload.Binding binding, LicensePayload payload, long now) {
        if (payload == null) {
            return INVALID;
        }
        if (binding == null || !binding.equals(payload.binding())) {
            return BINDING_MISMATCH;
        }
        if (payload.terms().issuedAt() > now || payload.terms().notBefore() > now) {
            return NOT_YET_VALID;
        }
        return payload.terms().allowsUse(now) ? VALID : EXPIRED;
    }
}
