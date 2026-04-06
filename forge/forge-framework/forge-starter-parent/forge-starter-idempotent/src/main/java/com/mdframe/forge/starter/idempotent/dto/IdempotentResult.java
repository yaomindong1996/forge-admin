package com.mdframe.forge.starter.idempotent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotentResult {
    
    private String requestId;
    
    private Object result;
    
    private Throwable exception;
    
    private long executeTime;
    
    private String status;
    
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PROCESSING = "PROCESSING";
}