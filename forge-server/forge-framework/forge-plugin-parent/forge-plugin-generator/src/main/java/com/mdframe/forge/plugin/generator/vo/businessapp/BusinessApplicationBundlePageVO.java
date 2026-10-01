package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 应用调试包可导出页面项。
 */
@Data
public class BusinessApplicationBundlePageVO {

    private String pageId;

    private String title;

    private String pageType;

    private String objectCode;

    private String objectName;

    /** 是否业务数据页或含数据块依赖 */
    private boolean dataBound;
}
