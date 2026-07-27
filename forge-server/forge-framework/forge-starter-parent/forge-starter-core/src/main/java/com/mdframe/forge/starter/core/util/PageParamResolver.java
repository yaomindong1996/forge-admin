package com.mdframe.forge.starter.core.util;

/**
 * Resolves the standard pageNum parameter while preserving the legacy page alias.
 */
public final class PageParamResolver {

    private static final int DEFAULT_PAGE_NUM = 1;

    private PageParamResolver() {
    }

    public static int resolve(Integer page, Integer pageNum) {
        if (page != null) {
            return page;
        }
        return pageNum != null ? pageNum : DEFAULT_PAGE_NUM;
    }
}
