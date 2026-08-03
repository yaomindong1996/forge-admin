package com.mdframe.forge.plugin.capability.flowaction.source;

import lombok.Data;

@Data
public class FlowActionSourceRow {

    private Long objectId;
    private String suiteCode;
    private String objectCode;
    private String objectName;
    private String configKey;
    private Long runtimeConfigId;
    private Long runtimeDatasourceId;
    private String runtimeDatasourceCode;
    private String runtimeDatasourceSnapshot;
    private Integer publishedObjectVersion;
    private String modelSnapshot;
    private Long bindingId;
    private String bindingKey;
    private String bindingConfig;
    private String statusField;
    private String starterField;
    private String ownerField;
    private String statusMapping;
}
