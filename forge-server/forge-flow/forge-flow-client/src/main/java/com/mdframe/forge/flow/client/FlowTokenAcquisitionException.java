package com.mdframe.forge.flow.client;

/**
 * 流程调用身份换取失败。该异常必须失败关闭，不能降级为静态服务账号。
 */
public class FlowTokenAcquisitionException extends RuntimeException {

    public FlowTokenAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
