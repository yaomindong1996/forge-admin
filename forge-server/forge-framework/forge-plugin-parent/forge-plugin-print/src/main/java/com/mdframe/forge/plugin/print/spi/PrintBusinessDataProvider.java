package com.mdframe.forge.plugin.print.spi;

import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;

import java.util.Map;
import java.util.Set;

/**
 * 代码业务接入独立打印中心的受管 SPI。code 由服务端注册，不能由页面指定 Bean。
 */
public interface PrintBusinessDataProvider {

    String code();

    void authorizeDesignSource(PrintActor actor, PrintBusinessSource source,
                               PrintDesignAction action);

    PrintFieldCatalogVO catalog(PrintActor actor, PrintBusinessSource source);

    void validateDesignResources(PrintActor actor, PrintBusinessSource source,
                                 Set<String> fileIds);

    void authorizeRecord(PrintActor actor, PrintBusinessSource source,
                         PrintRecordRequest record, Map<String, Object> params);

    PrintData load(PrintActor actor, PrintBusinessSource source, PrintRecordRequest record,
                   PrintBindingSelection selection, Map<String, Object> params);

    void validateRuntimeResources(PrintActor actor, PrintBusinessSource source,
                                  PrintRecordRequest record, Set<String> fileIds);
}
