package com.mdframe.forge.plugin.capability.flowaction.source;

import lombok.Data;

@Data
public class FlowActionSourceRow {

    private Long objectId;
    private String suiteCode;
    private String objectCode;
    private String objectName;
    private String configKey;
    private Integer publishedObjectVersion;
    private Long bindingId;
    private String bindingKey;
    private String bindingConfig;
}
