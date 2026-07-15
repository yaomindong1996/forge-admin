package com.mdframe.forge.plugin.generator.service.businessapp;

/**
 * 业务对象设计上下文只读提供器。
 *
 * <p>表映射编排只依赖该小接口，避免与完整对象设计器形成循环依赖。</p>
 */
@FunctionalInterface
public interface BusinessObjectDesignContextProvider {

    BusinessObjectDesignerService.DesignerContext loadContext(Long objectId);
}
