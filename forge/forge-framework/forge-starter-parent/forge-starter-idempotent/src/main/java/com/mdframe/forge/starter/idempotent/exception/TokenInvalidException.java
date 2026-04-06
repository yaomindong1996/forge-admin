package com.mdframe.forge.starter.idempotent.exception;

import com.mdframe.forge.framework.core.exception.BusinessException;

public class TokenInvalidException extends BusinessException {
    
    private String token;
    
    public TokenInvalidException(String message) {
        super(message);
    }
    
    public TokenInvalidException(String message, String token) {
        super(message);
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }
}