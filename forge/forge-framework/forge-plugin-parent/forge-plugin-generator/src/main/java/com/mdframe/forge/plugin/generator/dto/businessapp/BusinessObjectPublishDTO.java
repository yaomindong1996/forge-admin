package com.mdframe.forge.plugin.generator.dto.businessapp;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务对象发布参数。
 */
@Data
public class BusinessObjectPublishDTO {

    /** 发布模式：PUBLISH/CHECK_ONLY/REPUBLISH。 */
    private String publishMode;

    private Boolean syncTable;

    private Boolean force;

    private String remark;

    private LowcodeModelSchema modelSchema;

    private LowcodePageSchema pageSchema;

    private Map<String, Object> publishOptions = new LinkedHashMap<>();
}
