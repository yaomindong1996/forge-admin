package com.mdframe.forge.plugin.data.dto;

import lombok.Data;

@Data
public class DataDatasetAclDTO {

    private Long id;

    private String subjectType;

    private Long subjectId;

    private String accessLevel;
}
