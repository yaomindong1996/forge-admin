package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.constant.BusinessAppMode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeCodegenRequest;
import com.mdframe.forge.plugin.generator.service.AiCrudCodegenService;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeCodePreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

/**
 * 业务访问入口维度的功能代码预览与下载服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessAppCodegenService {

    private static final String SOURCE_DRAFT = "DRAFT";

    private final BusinessAppService appService;
    private final AiCrudConfigService crudConfigService;
    private final AiCrudCodegenService codegenService;
    private final BusinessCodegenConfigAssembler configAssembler;

    public Map<String, Object> getOptions(Long appId) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        JSONObject options = readOptions(app.getOptions());
        return buildCodegenOptions(app, options, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOptions(Long appId, LowcodeCodegenRequest request) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        JSONObject options = readOptions(app.getOptions());
        options.put("codegen", buildCodegenOptions(app, options, request));
        app.setOptions(writeOptions(options));
        appService.updateById(app);
    }

    public LowcodeCodePreviewVO previewCode(Long appId, LowcodeCodegenRequest request) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        AiCrudConfig config = prepareBusinessCodegenConfig(app, request);
        Map<String, String> files = codegenService.generateFiles(config);
        configAssembler.assertNoGenericRuntimeApi(files);

        LowcodeCodePreviewVO vo = new LowcodeCodePreviewVO();
        vo.setAppId(app.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setSourceType(resolveSourceType(request));
        vo.setVersionId(request == null ? null : request.getVersionId());
        vo.setFiles(files);
        vo.setFileCount(files.size());
        return vo;
    }

    public byte[] downloadCode(Long appId, LowcodeCodegenRequest request) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        AiCrudConfig config = prepareBusinessCodegenConfig(app, request);
        Map<String, String> files = codegenService.generateFiles(config);
        configAssembler.assertNoGenericRuntimeApi(files);
        return codegenService.toZip(files);
    }

    public String resolveDownloadFilename(Long appId) {
        AiBusinessApp app = requireCodeDownloadApp(appId);
        String name = StringUtils.firstNonBlank(app.getAppCode(), app.getConfigKey(), String.valueOf(app.getId()));
        return configAssembler.toPathSegment(name).replace("-", "_") + "-code.zip";
    }

    private AiBusinessApp requireCodeDownloadApp(Long appId) {
        AiBusinessApp app = appService.requireEntity(appId);
        String entryMode = StringUtils.defaultIfBlank(app.getEntryMode(), "").toUpperCase(Locale.ROOT);
        if (!"RUNTIME".equals(entryMode)) {
            throw new BusinessException("只有业务页面访问入口支持功能代码下载");
        }
        JSONObject options = readOptions(app.getOptions());
        if (!BusinessAppMode.isCodeDownload(options.get("appMode"))) {
            throw new BusinessException("当前访问入口不是下载代码模式");
        }
        if (StringUtils.isBlank(app.getSuiteCode()) || StringUtils.isBlank(app.getObjectCode())) {
            throw new BusinessException("下载代码模式需要关联业务域和业务单元");
        }
        if (StringUtils.isBlank(app.getConfigKey())) {
            throw new BusinessException("访问入口尚未配置业务页面，不能生成代码");
        }
        return app;
    }

    private AiCrudConfig prepareBusinessCodegenConfig(AiBusinessApp app, LowcodeCodegenRequest request) {
        AiCrudConfig config = crudConfigService.getByConfigKey(app.getConfigKey());
        if (config == null) {
            throw new BusinessException("访问入口尚未发布，不能生成代码");
        }
        JSONObject appOptions = readOptions(app.getOptions());
        JSONObject appCodegen = buildCodegenOptions(app, appOptions, request);
        String apiBase = configAssembler.normalizeBusinessApiBase(StringUtils.firstNonBlank(
                appCodegen.getString("businessApiBase"),
                defaultBusinessApiBase(app)
        ));
        return configAssembler.prepare(config, request, appCodegen, apiBase);
    }

    private JSONObject buildCodegenOptions(AiBusinessApp app, JSONObject appOptions, LowcodeCodegenRequest request) {
        JSONObject existing = readCodegen(appOptions);
        JSONObject codegen = new JSONObject();
        putIfNotBlank(codegen, "sourceType", StringUtils.firstNonBlank(
                request == null ? null : request.getSourceType(),
                existing.getString("sourceType"),
                SOURCE_DRAFT));
        if (request != null && request.getVersionId() != null) {
            codegen.put("versionId", request.getVersionId());
        } else if (existing.get("versionId") != null) {
            codegen.put("versionId", existing.get("versionId"));
        }
        String apiBase = StringUtils.firstNonBlank(
                request == null ? null : request.getBusinessApiBase(),
                existing.getString("businessApiBase"),
                defaultBusinessApiBase(app));
        codegen.put("businessApiBase", configAssembler.normalizeBusinessApiBase(apiBase));

        putIfNotBlank(codegen, "groupId", StringUtils.firstNonBlank(
                request == null ? null : request.getGroupId(),
                existing.getString("groupId")));
        putIfNotBlank(codegen, "domainPackage", StringUtils.firstNonBlank(
                request == null ? null : request.getDomainPackage(),
                existing.getString("domainPackage"),
                existing.getString("packageName")));
        putIfNotBlank(codegen, "moduleName", StringUtils.firstNonBlank(
                request == null ? null : request.getModuleName(),
                existing.getString("moduleName"),
                resolveModuleName(codegen.getString("businessApiBase"))));
        putIfNotBlank(codegen, "author", StringUtils.firstNonBlank(
                request == null ? null : request.getAuthor(),
                existing.getString("author")));
        String requestedEntityPrefix = request == null ? null : request.getEntityPrefix();
        codegen.put("entityPrefix", LowcodeCodegenOptionUtils.normalizeEntityPrefix(
                requestedEntityPrefix != null ? requestedEntityPrefix : existing.getString("entityPrefix")));
        codegen.put("stripTablePrefixes", LowcodeCodegenOptionUtils.resolveStripTablePrefixes(
                request == null ? null : request.getStripTablePrefixes(),
                existing.get("stripTablePrefixes"), LowcodeCodegenOptionUtils.DEFAULT_STRIP_TABLE_PREFIXES));
        codegen.put("backendBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getBackendBasePath(), existing.getString("backendBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_BACKEND_BASE_PATH, "后端 Java 输出路径"));
        codegen.put("mapperXmlBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getMapperXmlBasePath(), existing.getString("mapperXmlBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_MAPPER_XML_BASE_PATH, "Mapper XML 输出路径"));
        codegen.put("frontendBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                request == null ? null : request.getFrontendBasePath(),
                existing.getString("frontendBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_BASE_PATH, "前端页面输出路径"));
        codegen.put("frontendApiBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getFrontendApiBasePath(), existing.getString("frontendApiBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_API_BASE_PATH, "前端 API 输出路径"));
        codegen.put("includeBackend", resolveBoolean(request == null ? null : request.getIncludeBackend(),
                existing.get("includeBackend"), true));
        codegen.put("includeFrontend", resolveBoolean(request == null ? null : request.getIncludeFrontend(),
                existing.get("includeFrontend"), true));
        codegen.put("includeSql", resolveBoolean(request == null ? null : request.getIncludeSql(),
                existing.get("includeSql"), true));
        codegen.put("includeMenuSql", resolveBoolean(request == null ? null : request.getIncludeMenuSql(),
                existing.get("includeMenuSql"), true));
        codegen.put("includeDictSql", resolveBoolean(request == null ? null : request.getIncludeDictSql(),
                existing.get("includeDictSql"), true));
        codegen.put("includeExcelSql", resolveBoolean(request == null ? null : request.getIncludeExcelSql(),
                existing.get("includeExcelSql"), true));
        return codegen;
    }

    private String defaultBusinessApiBase(AiBusinessApp app) {
        return "/" + configAssembler.toPathSegment(app.getSuiteCode())
                + "/" + configAssembler.toPathSegment(app.getObjectCode());
    }

    private String resolveModuleName(String apiBase) {
        String[] parts = StringUtils.defaultString(apiBase).split("/");
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                return part.replace("-", "_");
            }
        }
        return "app";
    }

    private JSONObject readOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(options);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONObject readCodegen(JSONObject options) {
        JSONObject codegen = options == null ? null : options.getJSONObject("codegen");
        return codegen == null ? new JSONObject() : codegen;
    }

    private String writeOptions(JSONObject options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        return options.toJSONString();
    }

    private void putIfNotBlank(JSONObject target, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.put(key, value);
        }
    }

    private boolean resolveBoolean(Boolean requestValue, Object existingValue, boolean fallback) {
        if (requestValue != null) {
            return requestValue;
        }
        if (existingValue == null) {
            return fallback;
        }
        if (existingValue instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(existingValue));
    }

    private String resolveSourceType(LowcodeCodegenRequest request) {
        return StringUtils.defaultIfBlank(request == null ? null : request.getSourceType(), SOURCE_DRAFT)
                .toUpperCase(Locale.ROOT);
    }
}
