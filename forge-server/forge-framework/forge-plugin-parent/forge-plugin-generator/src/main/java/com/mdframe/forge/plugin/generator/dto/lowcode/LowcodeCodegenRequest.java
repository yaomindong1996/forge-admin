package com.mdframe.forge.plugin.generator.dto.lowcode;

import lombok.Data;

import java.util.List;

/**
 * 低代码应用代码预览/下载请求。
 */
@Data
public class LowcodeCodegenRequest {

    /** DRAFT/PUBLISHED/VERSION，默认 DRAFT。 */
    private String sourceType;

    private Long versionId;

    private Long domainId;

    /** Maven groupId，默认从业务领域 domainSchema.codegen.groupId 读取。 */
    private String groupId;

    /** Java 基础包名，默认从业务领域 domainSchema.codegen.domainPackage 读取；最终代码包会追加 moduleName。 */
    private String domainPackage;

    private String moduleName;

    private String author;

    /** 统一业务类名前缀，例如 Biz；为空时不追加。 */
    private String entityPrefix;

    /** 物理表名前缀剥离列表，按顺序只删除第一个匹配项；空列表表示不剥离。 */
    private List<String> stripTablePrefixes;

    private String backendBasePath;

    private String mapperXmlBasePath;

    private String frontendApiBasePath;

    private Boolean includeBackend;

    private Boolean includeFrontend;

    private Boolean includeSql;

    private Boolean includeMenuSql;

    private Boolean includeDictSql;

    private Boolean includeExcelSql;

    private String frontendBasePath;

    /** 下载代码模式业务接口前缀，例如 /crm/customer。 */
    private String businessApiBase;

    /** 应用级批量生成时选中的业务对象 ID；为空时默认生成应用全部对象。 */
    private List<Long> objectIds;
}
