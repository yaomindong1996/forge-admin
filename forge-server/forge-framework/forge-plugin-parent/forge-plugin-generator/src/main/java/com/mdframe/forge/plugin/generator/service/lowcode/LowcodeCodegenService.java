package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeCodegenRequest;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudCodegenService;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeCodePreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 低代码应用维度代码预览与下载服务。
 */
@Service
@RequiredArgsConstructor
public class LowcodeCodegenService {

    private static final String SOURCE_DRAFT = "DRAFT";
    private static final String SOURCE_PUBLISHED = "PUBLISHED";
    private static final String SOURCE_VERSION = "VERSION";

    private final ObjectMapper objectMapper;
    private final LowcodeAppService appService;
    private final LowcodeDomainService domainService;
    private final LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    private final AiCrudCodegenService codegenService;
    private final AiCrudConfigService configService;
    private final AiCrudConfigVersionMapper versionMapper;

    public LowcodeCodePreviewVO previewCode(Long appId, LowcodeCodegenRequest request) {
        AiCrudConfig config = resolveCodegenConfig(appId, request);
        Map<String, String> files = codegenService.generateFiles(config);
        LowcodeCodePreviewVO vo = new LowcodeCodePreviewVO();
        vo.setAppId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setSourceType(resolveSourceType(request));
        vo.setVersionId(request == null ? null : request.getVersionId());
        vo.setFiles(files);
        vo.setFileCount(files.size());
        return vo;
    }

    public byte[] downloadCode(Long appId, LowcodeCodegenRequest request) {
        AiCrudConfig config = resolveCodegenConfig(appId, request);
        return codegenService.generateZip(config);
    }

    public byte[] downloadByConfigKey(String configKey) {
        AiCrudConfig config = configService.getByConfigKey(configKey);
        if (config == null) {
            throw new BusinessException("配置不存在: " + configKey);
        }
        if ("LOWCODE".equals(config.getBuildMode())) {
            return codegenService.generateZip(prepareRuntimeConfig(copyConfig(config), null));
        }
        return codegenService.generateZip(configKey);
    }

    /**
     * 将运行配置补齐为可用于代码生成的配置副本。
     */
    public AiCrudConfig prepareConfigForCodegen(AiCrudConfig config, LowcodeCodegenRequest request) {
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        String sourceType = resolveSourceType(request);
        AiCrudConfig source = switch (sourceType) {
            case SOURCE_VERSION -> fromVersion(config, request == null ? null : request.getVersionId());
            case SOURCE_PUBLISHED -> {
                if (!"PUBLISHED".equals(config.getPublishStatus())) {
                    throw new BusinessException("应用尚未发布，不能按发布版本生成代码");
                }
                yield copyConfig(configService.resolvePublishedRuntimeConfig(config));
            }
            case SOURCE_DRAFT -> copyConfig(config);
            default -> throw new BusinessException("代码生成来源不正确: " + sourceType);
        };
        return prepareRuntimeConfig(source, request);
    }

    public Map<String, Object> getOptions(Long appId) {
        AiCrudConfig config = appService.requireConfig(appId);
        return buildCodegenOptions(config, null);
    }

    public void saveOptions(Long appId, LowcodeCodegenRequest request) {
        AiCrudConfig config = appService.requireConfig(appId);
        Map<String, Object> options = readOptions(config.getOptions());
        options.put("codegen", buildCodegenOptions(config, request));
        config.setOptions(writeJson(options, "代码生成配置"));
        configService.updateById(config);
    }

    private AiCrudConfig resolveCodegenConfig(Long appId, LowcodeCodegenRequest request) {
        AiCrudConfig base = appService.requireConfig(appId);
        String sourceType = resolveSourceType(request);
        AiCrudConfig config = switch (sourceType) {
            case SOURCE_VERSION -> fromVersion(base, request == null ? null : request.getVersionId());
            case SOURCE_PUBLISHED -> {
                if (!"PUBLISHED".equals(base.getPublishStatus())) {
                    throw new BusinessException("应用尚未发布，不能按发布版本生成代码");
                }
                yield copyConfig(configService.resolvePublishedRuntimeConfig(base));
            }
            case SOURCE_DRAFT -> copyConfig(base);
            default -> throw new BusinessException("代码生成来源不正确: " + sourceType);
        };
        return prepareRuntimeConfig(config, request);
    }

    private String resolveSourceType(LowcodeCodegenRequest request) {
        return StringUtils.defaultIfBlank(request == null ? null : request.getSourceType(), SOURCE_DRAFT)
                .toUpperCase();
    }

    private AiCrudConfig prepareRuntimeConfig(AiCrudConfig config, LowcodeCodegenRequest request) {
        if (StringUtils.isBlank(config.getModelSchema()) || StringUtils.isBlank(config.getPageSchema())) {
            return applyCodegenOptions(config, request);
        }
        try {
            LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
            LowcodePageSchema pageSchema = objectMapper.readValue(config.getPageSchema(), LowcodePageSchema.class);
            LowcodeRuntimeConfig runtimeConfig = runtimeConfigBuilder.buildRuntimeConfig(
                    config.getConfigKey(), modelSchema, pageSchema);
            config.setSearchSchema(runtimeConfig.getSearchSchema());
            config.setColumnsSchema(runtimeConfig.getColumnsSchema());
            config.setEditSchema(runtimeConfig.getEditSchema());
            config.setApiConfig(runtimeConfig.getApiConfig());
            config.setTableName(StringUtils.defaultIfBlank(config.getTableName(), runtimeConfig.getTableName()));
            config.setTableComment(StringUtils.defaultIfBlank(config.getTableComment(), runtimeConfig.getTableComment()));
            config.setLayoutType(StringUtils.defaultIfBlank(runtimeConfig.getLayoutType(), config.getLayoutType()));
            config.setDictConfig(StringUtils.defaultIfBlank(config.getDictConfig(), runtimeConfig.getDictConfig()));
            config.setDesensitizeConfig(StringUtils.defaultIfBlank(config.getDesensitizeConfig(), runtimeConfig.getDesensitizeConfig()));
            config.setEncryptConfig(StringUtils.defaultIfBlank(config.getEncryptConfig(), runtimeConfig.getEncryptConfig()));
            config.setTransConfig(StringUtils.defaultIfBlank(config.getTransConfig(), runtimeConfig.getTransConfig()));
            config.setOptions(mergeOptions(runtimeConfig.getOptions(), config.getOptions()));
            return applyCodegenOptions(config, request);
        } catch (Exception e) {
            throw new BusinessException("低代码运行时配置生成失败: " + e.getMessage());
        }
    }

    private AiCrudConfig applyCodegenOptions(AiCrudConfig config, LowcodeCodegenRequest request) {
        Map<String, Object> options = readOptions(config.getOptions());
        Map<String, Object> codegen = buildCodegenOptions(config, request);
        options.put("codegen", codegen);
        if (codegen.get("domainPackage") != null) {
            options.put("packageName", codegen.get("domainPackage"));
        }
        if (codegen.get("moduleName") != null) {
            options.put("moduleName", codegen.get("moduleName"));
        }
        if (codegen.get("author") != null) {
            options.put("author", codegen.get("author"));
        }
        mirrorCodegenOption(options, codegen, "entityPrefix");
        mirrorCodegenOption(options, codegen, "stripTablePrefixes");
        mirrorCodegenOption(options, codegen, "backendBasePath");
        mirrorCodegenOption(options, codegen, "mapperXmlBasePath");
        mirrorCodegenOption(options, codegen, "frontendBasePath");
        mirrorCodegenOption(options, codegen, "frontendApiBasePath");
        mirrorCodegenOption(options, codegen, "includeBackend");
        mirrorCodegenOption(options, codegen, "includeFrontend");
        mirrorCodegenOption(options, codegen, "includeSql");
        mirrorCodegenOption(options, codegen, "includeMenuSql");
        mirrorCodegenOption(options, codegen, "includeDictSql");
        mirrorCodegenOption(options, codegen, "includeExcelSql");
        config.setOptions(writeJson(options, "代码生成配置"));
        return config;
    }

    private Map<String, Object> buildCodegenOptions(AiCrudConfig config, LowcodeCodegenRequest request) {
        LowcodeDomainSchema.Codegen defaults = resolveDomainCodegen(config, request);
        Map<String, Object> existing = readCodegenOptions(config);
        Map<String, Object> codegen = new LinkedHashMap<>();
        String groupId = StringUtils.firstNonBlank(
                request == null ? null : request.getGroupId(),
                text(existing.get("groupId")),
                defaults == null ? null : defaults.getGroupId());
        String domainPackage = StringUtils.firstNonBlank(
                request == null ? null : request.getDomainPackage(),
                text(existing.get("domainPackage")),
                text(existing.get("packageName")),
                defaults == null ? null : defaults.getDomainPackage());
        String moduleName = StringUtils.firstNonBlank(
                request == null ? null : request.getModuleName(),
                text(existing.get("moduleName")),
                defaults == null ? null : defaults.getModuleName(),
                config.getDomainCode());
        domainPackage = normalizeBasePackageName(domainPackage, moduleName);
        groupId = StringUtils.defaultIfBlank(groupId, domainPackage);
        groupId = normalizeBasePackageName(groupId, moduleName);
        String frontendBasePath = StringUtils.firstNonBlank(
                request == null ? null : request.getFrontendBasePath(),
                text(existing.get("frontendBasePath")),
                defaults == null ? null : defaults.getFrontendBasePath());
        String businessApiBase = StringUtils.firstNonBlank(
                request == null ? null : request.getBusinessApiBase(),
                text(existing.get("businessApiBase")));
        String author = StringUtils.firstNonBlank(
                request == null ? null : request.getAuthor(),
                text(existing.get("author")));
        String requestedEntityPrefix = request == null ? null : request.getEntityPrefix();
        String entityPrefix = LowcodeCodegenOptionUtils.normalizeEntityPrefix(
                requestedEntityPrefix != null
                        ? requestedEntityPrefix
                        : firstNonNull(text(existing.get("entityPrefix")),
                        defaults == null ? null : defaults.getEntityPrefix(), ""));
        List<String> stripTablePrefixes = LowcodeCodegenOptionUtils.resolveStripTablePrefixes(
                request == null ? null : request.getStripTablePrefixes(),
                existing.get("stripTablePrefixes"),
                defaults == null ? LowcodeCodegenOptionUtils.DEFAULT_STRIP_TABLE_PREFIXES
                        : defaults.getStripTablePrefixes());
        String backendBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getBackendBasePath(),
                        text(existing.get("backendBasePath")),
                        defaults == null ? null : defaults.getBackendBasePath()),
                LowcodeCodegenOptionUtils.DEFAULT_BACKEND_BASE_PATH, "后端 Java 输出路径");
        String mapperXmlBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getMapperXmlBasePath(),
                        text(existing.get("mapperXmlBasePath")),
                        defaults == null ? null : defaults.getMapperXmlBasePath()),
                LowcodeCodegenOptionUtils.DEFAULT_MAPPER_XML_BASE_PATH, "Mapper XML 输出路径");
        frontendBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(frontendBasePath,
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_BASE_PATH, "前端页面输出路径");
        String frontendApiBasePath = LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getFrontendApiBasePath(),
                        text(existing.get("frontendApiBasePath")),
                        defaults == null ? null : defaults.getFrontendApiBasePath()),
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_API_BASE_PATH, "前端 API 输出路径");
        putIfNotBlank(codegen, "groupId", groupId);
        putIfNotBlank(codegen, "domainPackage", domainPackage);
        putIfNotBlank(codegen, "moduleName", moduleName);
        putIfNotBlank(codegen, "frontendBasePath", frontendBasePath);
        codegen.put("entityPrefix", entityPrefix);
        codegen.put("stripTablePrefixes", stripTablePrefixes);
        codegen.put("backendBasePath", backendBasePath);
        codegen.put("mapperXmlBasePath", mapperXmlBasePath);
        codegen.put("frontendApiBasePath", frontendApiBasePath);
        putIfNotBlank(codegen, "businessApiBase", businessApiBase);
        putIfNotBlank(codegen, "author", author);
        codegen.put("includeBackend", resolveBoolean(request == null ? null : request.getIncludeBackend(),
                existing.get("includeBackend"), defaultBoolean(
                        defaults == null ? null : defaults.getIncludeBackend(), true)));
        codegen.put("includeFrontend", resolveBoolean(request == null ? null : request.getIncludeFrontend(),
                existing.get("includeFrontend"), defaultBoolean(
                        defaults == null ? null : defaults.getIncludeFrontend(), true)));
        codegen.put("includeSql", resolveBoolean(request == null ? null : request.getIncludeSql(),
                existing.get("includeSql"), defaultBoolean(
                        defaults == null ? null : defaults.getIncludeSql(), true)));
        codegen.put("includeMenuSql", resolveBoolean(request == null ? null : request.getIncludeMenuSql(),
                existing.get("includeMenuSql"), defaultBoolean(
                        defaults == null ? null : defaults.getIncludeMenuSql(), true)));
        codegen.put("includeDictSql", resolveBoolean(request == null ? null : request.getIncludeDictSql(),
                existing.get("includeDictSql"), defaultBoolean(
                        defaults == null ? null : defaults.getIncludeDictSql(), true)));
        codegen.put("includeExcelSql", resolveBoolean(request == null ? null : request.getIncludeExcelSql(),
                existing.get("includeExcelSql"), defaultBoolean(
                        defaults == null ? null : defaults.getIncludeExcelSql(), true)));
        return codegen;
    }

    private String normalizeBasePackageName(String packageName, String moduleName) {
        if (StringUtils.isBlank(packageName)) {
            return packageName;
        }
        String normalized = packageName.replaceAll("\\.+$", "");
        if (StringUtils.isBlank(moduleName)) {
            return normalized;
        }
        String suffix = "." + moduleName.trim();
        if (normalized.endsWith(suffix)) {
            return normalized.substring(0, normalized.length() - suffix.length());
        }
        return normalized;
    }

    private LowcodeDomainSchema.Codegen resolveDomainCodegen(AiCrudConfig config, LowcodeCodegenRequest request) {
        Long domainId = request != null && request.getDomainId() != null ? request.getDomainId() : config.getDomainId();
        AiLowcodeDomain domain = null;
        if (domainId != null) {
            domain = domainService.requireDomain(domainId);
        } else if (StringUtils.isNotBlank(config.getDomainCode())) {
            domain = domainService.getByCode(config.getDomainCode());
        }
        if (domain == null || StringUtils.isBlank(domain.getDomainSchema())) {
            return null;
        }
        try {
            LowcodeDomainSchema schema = objectMapper.readValue(domain.getDomainSchema(), LowcodeDomainSchema.class);
            return schema.getCodegen();
        } catch (Exception e) {
            return null;
        }
    }

    private AiCrudConfig fromVersion(AiCrudConfig base, Long versionId) {
        if (versionId == null) {
            throw new BusinessException("按历史版本生成代码时 versionId 不能为空");
        }
        AiCrudConfigVersion version = versionMapper.selectVersionById(resolveTenantId(base), base.getId(), versionId);
        if (version == null) {
            throw new BusinessException("代码生成版本不存在");
        }
        AiCrudConfig config = copyConfig(base);
        config.setDomainId(version.getDomainId());
        config.setDomainCode(version.getDomainCode());
        config.setObjectCode(version.getObjectCode());
        config.setObjectName(version.getObjectName());
        config.setModelSchema(version.getModelSchema());
        config.setPageSchema(version.getPageSchema());
        config.setSearchSchema(version.getSearchSchema());
        config.setColumnsSchema(version.getColumnsSchema());
        config.setEditSchema(version.getEditSchema());
        config.setApiConfig(version.getApiConfig());
        config.setOptions(version.getOptions());
        applyVersionSnapshot(config, version.getPublishSnapshot());
        return config;
    }

    private void applyVersionSnapshot(AiCrudConfig config, String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return;
        }
        Map<String, Object> snapshot = readOptions(snapshotJson);
        config.setTableName(StringUtils.defaultIfBlank(text(snapshot.get("tableName")), config.getTableName()));
        config.setTableComment(StringUtils.defaultIfBlank(text(snapshot.get("tableComment")), config.getTableComment()));
        config.setAppName(StringUtils.defaultIfBlank(text(snapshot.get("appName")), config.getAppName()));
        config.setLayoutType(StringUtils.defaultIfBlank(text(snapshot.get("layoutType")), config.getLayoutType()));
        config.setDictConfig(StringUtils.defaultIfBlank(text(snapshot.get("dictConfig")), config.getDictConfig()));
        config.setDesensitizeConfig(StringUtils.defaultIfBlank(text(snapshot.get("desensitizeConfig")), config.getDesensitizeConfig()));
        config.setEncryptConfig(StringUtils.defaultIfBlank(text(snapshot.get("encryptConfig")), config.getEncryptConfig()));
        config.setTransConfig(StringUtils.defaultIfBlank(text(snapshot.get("transConfig")), config.getTransConfig()));
    }

    private AiCrudConfig copyConfig(AiCrudConfig source) {
        AiCrudConfig target = new AiCrudConfig();
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setConfigKey(source.getConfigKey());
        target.setTableName(source.getTableName());
        target.setTableComment(source.getTableComment());
        target.setAppName(source.getAppName());
        target.setSearchSchema(source.getSearchSchema());
        target.setColumnsSchema(source.getColumnsSchema());
        target.setEditSchema(source.getEditSchema());
        target.setApiConfig(source.getApiConfig());
        target.setOptions(source.getOptions());
        target.setMode(source.getMode());
        target.setBuildMode(source.getBuildMode());
        target.setStatus(source.getStatus());
        target.setPublishStatus(source.getPublishStatus());
        target.setMenuName(source.getMenuName());
        target.setMenuParentId(source.getMenuParentId());
        target.setMenuSort(source.getMenuSort());
        target.setMenuResourceId(source.getMenuResourceId());
        target.setDictConfig(source.getDictConfig());
        target.setDesensitizeConfig(source.getDesensitizeConfig());
        target.setEncryptConfig(source.getEncryptConfig());
        target.setTransConfig(source.getTransConfig());
        target.setLayoutType(source.getLayoutType());
        target.setModelSchema(source.getModelSchema());
        target.setPageSchema(source.getPageSchema());
        target.setDraftVersion(source.getDraftVersion());
        target.setPublishedVersion(source.getPublishedVersion());
        target.setPublishTime(source.getPublishTime());
        target.setPublishBy(source.getPublishBy());
        target.setDomainId(source.getDomainId());
        target.setDomainCode(source.getDomainCode());
        target.setObjectCode(source.getObjectCode());
        target.setObjectName(source.getObjectName());
        return target;
    }

    private String mergeOptions(String runtimeOptionsJson, String configOptionsJson) {
        Map<String, Object> configured = readOptions(configOptionsJson);
        Object codegen = configured.get("codegen");
        Map<String, Object> merged = new LinkedHashMap<>(configured);
        merged.putAll(readOptions(runtimeOptionsJson));
        if (codegen != null) {
            merged.put("codegen", codegen);
        }
        return writeJson(merged, "代码生成配置");
    }

    private Map<String, Object> readOptions(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new BusinessException("低代码配置 JSON 不是合法对象", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readCodegenOptions(AiCrudConfig config) {
        Map<String, Object> options = readOptions(config == null ? null : config.getOptions());
        Object codegen = options.get("codegen");
        if (codegen instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    private String writeJson(Object value, String fieldName) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(fieldName + "序列化失败");
        }
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

    private void mirrorCodegenOption(Map<String, Object> options,
                                     Map<String, Object> codegen,
                                     String key) {
        if (codegen.containsKey(key)) {
            options.put(key, codegen.get(key));
        }
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean resolveBoolean(Boolean requested, Object existing, boolean defaultValue) {
        if (requested != null) {
            return requested;
        }
        if (existing instanceof Boolean bool) {
            return bool;
        }
        if (existing instanceof String text && StringUtils.isNotBlank(text)) {
            return Boolean.parseBoolean(text);
        }
        return defaultValue;
    }

    private boolean defaultBoolean(Boolean configured, boolean defaultValue) {
        return configured == null ? defaultValue : configured;
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long resolveTenantId(AiCrudConfig config) {
        if (config.getTenantId() != null) {
            return config.getTenantId();
        }
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
