package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务对象单据模式保存参数。
 */
@Data
public class BusinessDocumentConfigDTO {

    private Boolean documentEnabled;

    private String documentName;

    private String documentNoRule;

    private String noRuleTemplate;

    private String statusField;

    private String starterField;

    private String ownerField;

    private String defaultFlowKey;

    private Map<String, String> statusMapping = new LinkedHashMap<>();

    private List<StatusMappingRowDTO> statusMappingRows = new ArrayList<>();

    private Map<String, Object> statusActionPolicy = new LinkedHashMap<>();

    private Map<String, Object> options = new LinkedHashMap<>();

    @Data
    public static class StatusMappingRowDTO {

        private String standardStatus;

        private String statusValue;

        private String displayName;

        private String tagType;

        private Boolean allowEdit;

        private Boolean allowDelete;

        private Boolean allowStartFlow;
    }
}
