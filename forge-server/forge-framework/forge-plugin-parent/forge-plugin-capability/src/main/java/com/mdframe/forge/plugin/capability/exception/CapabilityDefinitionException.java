package com.mdframe.forge.plugin.capability.exception;

import com.mdframe.forge.plugin.capability.model.CapabilityErrorCode;

public class CapabilityDefinitionException extends IllegalArgumentException {

    private final CapabilityErrorCode errorCode;

    public CapabilityDefinitionException(CapabilityErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CapabilityErrorCode getErrorCode() {
        return errorCode;
    }
}
