package com.mdframe.forge.plugin.print.spi;

import java.util.List;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;

/**
 * 仅由可信 Provider 返回；versions 来自应用已发布快照，不得查询最新草稿兜底。
 */
public record AuthorizedPrintContext(PrintActor actor, PrintRecordRequest record, Long applicationVersionId, List<VersionRef> versions, PrintFieldCatalogVO catalog) {

    public AuthorizedPrintContext {
        versions = versions == null ? List.of() : List.copyOf(versions);
    }

    public record VersionRef(Long templateId, Long templateVersionId, boolean isDefault, int sortOrder) {
    }
}
