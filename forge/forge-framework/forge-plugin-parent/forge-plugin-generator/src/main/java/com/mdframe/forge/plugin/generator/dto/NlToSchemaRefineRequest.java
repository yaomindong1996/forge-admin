package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class NlToSchemaRefineRequest {

    private String currentSchema;

    private String message;

    private String sessionId;
}
