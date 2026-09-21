package com.mdframe.forge.plugin.print.spi;

import java.util.Set;

/**
 * 本次模板所需的授权字段/集合/静态文件；Provider 只加载需要的数据。
 */
public record PrintBindingSelection(AuthorizedPrintContext.VersionRef version, Set<String> fields, Set<String> collections, Set<String> staticFileIds) {

    public PrintBindingSelection {
        fields = Set.copyOf(fields);
        collections = Set.copyOf(collections);
        staticFileIds = Set.copyOf(staticFileIds);
    }
}
