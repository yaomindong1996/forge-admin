package com.mdframe.forge.plugin.print.spi;

/**
 * 只能由服务端登录上下文取得，不出现在请求 DTO 中。
 */
public record PrintActor(Long tenantId, Long userId, Long departmentId) {

    public PrintActor {
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            throw new IllegalArgumentException("打印身份缺失");
        }
    }
}
