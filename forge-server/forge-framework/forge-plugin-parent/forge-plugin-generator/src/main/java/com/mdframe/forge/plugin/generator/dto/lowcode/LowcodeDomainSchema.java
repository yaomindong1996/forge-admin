package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低代码业务领域扩展协议。
 */
@Data
public class LowcodeDomainSchema {

    private AiContext aiContext = new AiContext();

    private Naming naming = new Naming();

    private Defaults defaults = new Defaults();

    private Codegen codegen = new Codegen();

    private List<LowcodeFieldSchema> fieldTemplates = new ArrayList<>();

    private List<DictRecommendation> dictRecommendations = new ArrayList<>();

    private List<SecurityPolicy> securityPolicies = new ArrayList<>();

    @Data
    public static class AiContext {

        private String description;

        private List<String> terms = new ArrayList<>();

        private List<String> commonObjects = new ArrayList<>();

        private String fieldNamingPreference;

        private List<String> constraints = new ArrayList<>();

        private List<String> generationNotes = new ArrayList<>();
    }

    @Data
    public static class Naming {

        private String tablePrefix;

        private String configKeyPrefix;

        private String objectCodeStyle = "lower_snake";
    }

    @Data
    public static class Defaults {

        private String appType = "SINGLE";

        private String layoutType = "simple-crud";

        private String tableMode = "CREATE";

        private Long menuParentId;
    }

    @Data
    public static class Codegen {

        /** Maven groupId，例如 com.mdframe.forge.business。 */
        private String groupId;

        /** 领域默认 Java 基础包名，例如 com.mdframe.forge.business；最终代码包会追加 moduleName。 */
        private String domainPackage;

        /** 领域默认模块名，例如 crm。 */
        private String moduleName;

        /** 前端默认输出根路径，例如 frontend/src/views/crm。 */
        private String frontendBasePath;

        /** 统一业务类名前缀，例如 Biz。 */
        private String entityPrefix;

        /** 默认剥离的物理表名前缀。 */
        private List<String> stripTablePrefixes = new ArrayList<>(List.of("sys_", "ai_", "t_", "tb_"));

        private String backendBasePath = "backend/src/main/java";

        private String mapperXmlBasePath = "backend/src/main/resources/mapper";

        private String frontendApiBasePath = "frontend/src/api";

        private Boolean includeBackend = true;

        private Boolean includeFrontend = true;

        private Boolean includeSql = true;

        private Boolean includeMenuSql = true;

        private Boolean includeDictSql = true;

        private Boolean includeExcelSql = true;
    }

    @Data
    public static class DictRecommendation {

        private String fieldPattern;

        private String dictType;
    }

    @Data
    public static class SecurityPolicy {

        private String fieldPattern;

        private String sensitiveType;

        private String encryptAlgorithm;
    }
}
