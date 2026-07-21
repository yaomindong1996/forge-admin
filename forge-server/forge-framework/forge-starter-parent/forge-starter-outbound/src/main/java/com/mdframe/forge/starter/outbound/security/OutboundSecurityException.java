package com.mdframe.forge.starter.outbound.security;

import com.mdframe.forge.starter.core.exception.BusinessException;

public class OutboundSecurityException extends BusinessException {

    public OutboundSecurityException(String message) {
        super(message);
    }
}
