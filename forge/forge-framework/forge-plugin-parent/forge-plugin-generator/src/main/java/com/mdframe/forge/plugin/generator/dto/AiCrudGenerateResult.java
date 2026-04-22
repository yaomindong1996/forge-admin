package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class AiCrudGenerateResult {

    private boolean success;
    private String configKey;
    private String tableName;
    private String tableComment;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String apiConfig;
    private String dictConfig;
    private String desensitizeConfig;
    private String encryptConfig;
    private String transConfig;
    private String errorMessage;
    private boolean fallback;

    public static AiCrudGenerateResult ok(String configKey, String tableName, String tableComment,
                                           String searchSchema, String columnsSchema,
                                           String editSchema, String apiConfig,
                                           String dictConfig, String desensitizeConfig,
                                           String encryptConfig, String transConfig) {
        AiCrudGenerateResult r = new AiCrudGenerateResult();
        r.setSuccess(true);
        r.setConfigKey(configKey);
        r.setTableName(tableName);
        r.setTableComment(tableComment);
        r.setSearchSchema(searchSchema);
        r.setColumnsSchema(columnsSchema);
        r.setEditSchema(editSchema);
        r.setApiConfig(apiConfig);
        r.setDictConfig(dictConfig);
        r.setDesensitizeConfig(desensitizeConfig);
        r.setEncryptConfig(encryptConfig);
        r.setTransConfig(transConfig);
        return r;
    }

    public static AiCrudGenerateResult fail(String message) {
        AiCrudGenerateResult r = new AiCrudGenerateResult();
        r.setSuccess(false);
        r.setErrorMessage(message);
        return r;
    }
}
