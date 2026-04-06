package com.mdframe.forge.starter.idempotent.constant;

public interface IdempotentConstant {
    String DEFAULT_PREFIX = "idempotent:";
    int DEFAULT_EXPIRE = 600;
    String DEFAULT_MESSAGE = "请勿重复提交";
}
