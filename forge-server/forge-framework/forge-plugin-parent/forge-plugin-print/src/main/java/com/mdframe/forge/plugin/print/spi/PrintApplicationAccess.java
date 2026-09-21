package com.mdframe.forge.plugin.print.spi;

import com.mdframe.forge.plugin.print.enums.PrintDesignAction;

/**
 * 应用侧实现，禁止依赖打印服务或应用发布编排，避免 Bean 环。
 */
public interface PrintApplicationAccess {

    void authorize(PrintActor actor, Long applicationId, PrintDesignAction action);

    /**
     * 必须在当前事务中锁定 tenant/application 对应的持久化应用行。
     */
    void lockApplication(PrintActor actor, Long applicationId);

    /**
     * 必须检查全部仍有效的应用发布快照引用，失败抛出异常。
     */
    void assertTemplateUnreferenced(PrintActor actor, Long applicationId, Long templateId);
}
