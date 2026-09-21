package com.mdframe.forge.plugin.print.spi;

import java.util.Set;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;

/**
 * 所有授权方法都必须由业务模块明确实现，没有默认放行。
 */
public interface PrintDataProvider {

    PrintSourceType sourceType();

    boolean supports(PrintSourceRequest source);

    void authorizeDesignSource(PrintActor actor, PrintSourceRequest source, PrintDesignAction action);

    PrintFieldCatalogVO catalog(AuthorizedPrintSource source);

    void validateDesignResources(AuthorizedPrintSource source, Set<String> fileIds);

    /**
     * 必须核验记录及场景；流程场景必须在返回 context 中解析出非空 processRunId。
     */
    AuthorizedPrintContext authorize(PrintActor actor, PrintRecordRequest request);

    PrintData load(AuthorizedPrintContext context, PrintBindingSelection selection);

    void validateRuntimeResources(AuthorizedPrintContext context, Set<String> fileIds);
}
