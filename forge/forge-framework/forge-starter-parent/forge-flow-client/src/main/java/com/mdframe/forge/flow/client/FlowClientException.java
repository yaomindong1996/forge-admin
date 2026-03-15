package com.mdframe.forge.flow.client;

/**
 * 流程客户端异常
 * 
 * @author forge
 */
public class FlowClientException extends RuntimeException {

    public FlowClientException(String message) {
        super(message);
    }

    public FlowClientException(String message, Throwable cause) {
        super(message, cause);
    }
}