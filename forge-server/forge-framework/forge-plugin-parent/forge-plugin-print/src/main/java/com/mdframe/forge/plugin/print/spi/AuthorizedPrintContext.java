package com.mdframe.forge.plugin.print.spi;

import java.util.List;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;

/**
 * 仅由可信 Provider 和打印编排层创建。应用来源的 versions 来自已发布快照；
 * 独立来源的 versions 来自启用绑定中固定的已发布版本。
 */
public record AuthorizedPrintContext(
        PrintActor actor,
        PrintRecordRequest record,
        Long applicationVersionId,
        Long sourceRevision,
        List<VersionRef> versions,
        PrintFieldCatalogVO catalog) {

    public AuthorizedPrintContext {
        versions = versions == null ? List.of() : List.copyOf(versions);
    }

    public AuthorizedPrintContext(PrintActor actor, PrintRecordRequest record,
                                  Long applicationVersionId, List<VersionRef> versions,
                                  PrintFieldCatalogVO catalog) {
        this(actor, record, applicationVersionId, null, versions, catalog);
    }

    public record VersionRef(Long templateId, Long templateVersionId, boolean isDefault, int sortOrder) {
    }
}
