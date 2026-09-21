package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.starter.core.exception.BusinessException;

/**
 * 返回稳定错误码，不携带业务正文或数据库异常原文。
 */
public final class PrintFailure {

    private PrintFailure() {
    }

    public record Detail(String errorCode, String path) {
    }

    public static BusinessException of(int status, String code, String message) {
        return new BusinessException(status, message, new Detail(code, ""));
    }

    public static BusinessException field(String path, String message) {
        return new BusinessException(400, message, new Detail("PRINT_FIELD_NOT_ALLOWED", path));
    }

    public static BusinessException conflict() {
        return of(409, "PRINT_REVISION_CONFLICT", "模板或绑定已发生变化，请重新载入后重试；当前修改尚未保存");
    }

    public static BusinessException denied() {
        return of(403, "PRINT_ACCESS_DENIED", "无权访问此打印来源或记录");
    }

    public static BusinessException missing() {
        return of(404, "PRINT_TEMPLATE_UNAVAILABLE", "打印模板或版本不存在、已停用或不可用");
    }
}
