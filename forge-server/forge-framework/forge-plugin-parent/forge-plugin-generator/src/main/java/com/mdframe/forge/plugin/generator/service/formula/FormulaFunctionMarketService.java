package com.mdframe.forge.plugin.generator.service.formula;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunction;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunctionInstall;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunctionVersion;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionInstallRequest;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionMarketQueryDTO;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionMarketResponse;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionRegisterRequest;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionResponse;
import com.mdframe.forge.plugin.generator.mapper.FormulaFunctionMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.orm.support.DuplicateKeyActions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公式函数市场服务。
 */
@Service
public class FormulaFunctionMarketService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String STATUS_INSTALLED = "INSTALLED";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String SOURCE_TENANT = "TENANT";
    private static final String IMPLEMENTATION_JAVA_BEAN = "JAVA_BEAN";

    private final FormulaFunctionMapper formulaFunctionMapper;
    private final FormulaFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public FormulaFunctionMarketService(FormulaFunctionMapper formulaFunctionMapper,
                                        FormulaFunctionRegistry functionRegistry) {
        this.formulaFunctionMapper = formulaFunctionMapper;
        this.functionRegistry = functionRegistry;
    }

    public FormulaFunctionMarketService(FormulaFunctionRegistry functionRegistry) {
        this.formulaFunctionMapper = null;
        this.functionRegistry = functionRegistry;
    }

    public Page<FormulaFunctionMarketResponse> page(FormulaFunctionMarketQueryDTO query) {
        FormulaFunctionMarketQueryDTO effectiveQuery = query == null ? new FormulaFunctionMarketQueryDTO() : query;
        Page<FormulaFunctionMarketResponse> page = new Page<>(
            effectiveQuery.getPageNum(), effectiveQuery.getPageSize());
        if (formulaFunctionMapper == null) {
            List<FormulaFunctionMarketResponse> records = functionRegistry.listDefinitions().stream()
                .map(this::fromDefinition)
                .toList();
            page.setTotal(records.size());
            page.setRecords(records);
            return page;
        }
        return formulaFunctionMapper.selectFormulaFunctionMarketPage(page, resolveTenantId(), effectiveQuery);
    }

    public FormulaFunctionMarketResponse detail(String functionCode) {
        if (functionCode == null || functionCode.isBlank()) {
            return null;
        }
        if (formulaFunctionMapper == null) {
            return functionRegistry.find(functionCode).map(this::fromDefinition).orElse(null);
        }
        return formulaFunctionMapper.selectFormulaFunctionMarketDetail(resolveTenantId(), functionCode);
    }

    public FormulaFunctionMarketResponse install(FormulaFunctionInstallRequest request) {
        if (request == null || request.getFunctionCode() == null || request.getFunctionCode().isBlank()) {
            throw new IllegalArgumentException("函数编码不能为空");
        }
        return saveInstall(request.getFunctionCode(), request.getVersion(),
            request.getEnabled() == null || Boolean.TRUE.equals(request.getEnabled()));
    }

    public FormulaFunctionMarketResponse enable(String functionCode) {
        return saveInstall(functionCode, null, true);
    }

    public FormulaFunctionMarketResponse disable(String functionCode) {
        return saveInstall(functionCode, null, false);
    }

    public FormulaFunctionMarketResponse registerCustomFunction(FormulaFunctionRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("函数注册请求不能为空");
        }
        String functionCode = normalizeText(request.getFunctionCode());
        if (functionCode == null) {
            throw new IllegalArgumentException("函数编码不能为空");
        }
        if (normalizeText(request.getDisplayName()) == null) {
            throw new IllegalArgumentException("展示名称不能为空");
        }
        if (normalizeText(request.getCategory()) == null) {
            throw new IllegalArgumentException("函数分类不能为空");
        }
        if (normalizeText(request.getBeanName()) == null || normalizeText(request.getMethodName()) == null) {
            throw new IllegalArgumentException("Java Bean 名称和方法名称不能为空");
        }
        validateArgumentSchema(request.getArgumentSchema());

        if (formulaFunctionMapper == null) {
            FormulaFunctionDefinition definition = toDefinition(request, functionCode,
                Boolean.FALSE.equals(request.getEnabled()) ? STATUS_DISABLED : STATUS_ENABLED);
            functionRegistry.registerDefinition(definition);
            functionRegistry.registerAviatorFunctions();
            return fromDefinition(definition);
        }

        Long tenantId = resolveTenantId();
        AiFormulaFunction existing = formulaFunctionMapper.selectFormulaFunctionByCode(tenantId, functionCode);
        if (existing != null && Boolean.TRUE.equals(existing.getBuiltin())) {
            throw new IllegalArgumentException("内置函数不允许覆盖: " + functionCode);
        }

        String version = resolveRequestVersion(request.getVersion());
        LocalDateTime now = LocalDateTime.now();
        Long userId = resolveUserId();
        Long deptId = resolveMainOrgId();

        AiFormulaFunction function = new AiFormulaFunction();
        function.setId(existing == null ? IdWorker.getId() : existing.getId());
        function.setTenantId(tenantId);
        function.setFunctionCode(functionCode);
        function.setDisplayName(normalizeText(request.getDisplayName()));
        function.setCategory(normalizeText(request.getCategory()));
        function.setDescription(normalizeText(request.getDescription()));
        function.setSourceType(SOURCE_TENANT);
        function.setArgumentSchema(normalizeArgumentSchema(request.getArgumentSchema()));
        function.setReturnType(normalizeReturnType(request.getReturnType()));
        function.setExample(normalizeText(request.getExample()));
        function.setStatus(Boolean.FALSE.equals(request.getEnabled()) ? STATUS_DISABLED : STATUS_ENABLED);
        function.setCurrentVersion(version);
        function.setLatestVersion(version);
        function.setBuiltin(false);
        function.setSortOrder(existing == null || existing.getSortOrder() == null ? 500 : existing.getSortOrder());
        function.setRemark("Java Bean 自定义公式函数");
        fillAuditFields(function, userId, deptId, now);
        DuplicateKeyActions.upsert(
                () -> formulaFunctionMapper.updateFormulaFunctionByUk(function),
                () -> formulaFunctionMapper.insertFormulaFunction(function));

        AiFormulaFunctionVersion functionVersion = new AiFormulaFunctionVersion();
        functionVersion.setId(IdWorker.getId());
        functionVersion.setTenantId(tenantId);
        functionVersion.setFunctionCode(functionCode);
        functionVersion.setVersion(version);
        functionVersion.setImplementationType(IMPLEMENTATION_JAVA_BEAN);
        functionVersion.setBeanName(normalizeText(request.getBeanName()));
        functionVersion.setMethodName(normalizeText(request.getMethodName()));
        functionVersion.setArgumentSchema(function.getArgumentSchema());
        functionVersion.setReturnType(function.getReturnType());
        functionVersion.setExample(function.getExample());
        functionVersion.setReleaseNote(normalizeText(request.getReleaseNote()));
        functionVersion.setStatus(function.getStatus());
        functionVersion.setRemark("Java Bean 自定义公式函数版本");
        fillAuditFields(functionVersion, userId, deptId, now);
        DuplicateKeyActions.upsert(
                () -> formulaFunctionMapper.updateFunctionVersionByUk(functionVersion),
                () -> formulaFunctionMapper.insertFunctionVersion(functionVersion));

        FormulaFunctionInstallRequest installRequest = new FormulaFunctionInstallRequest();
        installRequest.setFunctionCode(functionCode);
        installRequest.setVersion(version);
        installRequest.setEnabled(request.getEnabled() == null || Boolean.TRUE.equals(request.getEnabled()));
        FormulaFunctionMarketResponse response = install(installRequest);
        registerRuntimeDefinition(response);
        return response;
    }

    public List<FormulaFunctionResponse> availableFunctions() {
        if (formulaFunctionMapper == null) {
            return functionRegistry.listEnabledResponses();
        }
        syncInstalledDefinitions();
        return formulaFunctionMapper.selectEnabledInstalledFunctions(resolveTenantId()).stream()
            .map(this::toFunctionResponse)
            .toList();
    }

    public List<String> validateFunctionReferences(String expression, Collection<String> explicitRefs) {
        syncInstalledDefinitions();
        List<String> errors = new ArrayList<>(functionRegistry.validateFunctionReferences(expression, explicitRefs));
        if (formulaFunctionMapper == null) {
            return errors;
        }

        Set<String> functionCodes = new LinkedHashSet<>(functionRegistry.extractFunctionCodes(expression));
        if (explicitRefs != null) {
            functionCodes.addAll(explicitRefs);
        }
        for (String functionCode : functionCodes) {
            if (functionCode == null
                    || functionCode.isBlank()
                    || !functionRegistry.isManagedFunctionName(functionCode)) {
                continue;
            }
            FormulaFunctionMarketResponse detail = detail(functionCode);
            if (detail == null) {
                errors.add("Formula function is not installed: " + functionCode);
                continue;
            }
            if (!STATUS_ENABLED.equalsIgnoreCase(detail.getStatus())) {
                errors.add("Formula function is disabled: " + functionCode);
                continue;
            }
            if (!STATUS_INSTALLED.equalsIgnoreCase(detail.getInstallStatus())
                    || !Boolean.TRUE.equals(detail.getEnabled())) {
                errors.add("Formula function is not enabled: " + functionCode);
            }
        }
        return errors;
    }

    private FormulaFunctionMarketResponse saveInstall(String functionCode, String version, boolean enabled) {
        FormulaFunctionMarketResponse detail = detail(functionCode);
        if (detail == null) {
            throw new IllegalArgumentException("函数不存在: " + functionCode);
        }

        if (formulaFunctionMapper != null) {
            AiFormulaFunctionInstall install = new AiFormulaFunctionInstall();
            install.setId(IdWorker.getId());
            install.setTenantId(resolveTenantId());
            install.setFunctionCode(functionCode);
            install.setInstalledVersion(resolveVersion(version, detail));
            install.setInstallStatus(STATUS_INSTALLED);
            install.setEnabled(enabled);
            install.setSourceType(detail.getSourceType());
            install.setInstalledBy(resolveUserId());
            install.setInstalledTime(LocalDateTime.now());
            fillAuditFields(install);
            DuplicateKeyActions.upsert(
                    () -> formulaFunctionMapper.updateFunctionInstallByUk(install),
                    () -> formulaFunctionMapper.insertFunctionInstall(install));
            FormulaFunctionMarketResponse response = detail(functionCode);
            registerRuntimeDefinition(response);
            return response;
        }

        detail.setInstallStatus(STATUS_INSTALLED);
        detail.setInstalledVersion(resolveVersion(version, detail));
        detail.setEnabled(enabled);
        registerRuntimeDefinition(detail);
        return detail;
    }

    public void syncInstalledDefinitions() {
        if (formulaFunctionMapper == null) {
            return;
        }
        List<FormulaFunctionMarketResponse> records =
            formulaFunctionMapper.selectInstalledFunctionDefinitions(resolveTenantId());
        for (FormulaFunctionMarketResponse record : records) {
            registerRuntimeDefinition(record);
        }
        functionRegistry.registerAviatorFunctions();
    }

    private String resolveVersion(String version, FormulaFunctionMarketResponse detail) {
        if (version != null && !version.isBlank()) {
            return version;
        }
        if (detail.getCurrentVersion() != null && !detail.getCurrentVersion().isBlank()) {
            return detail.getCurrentVersion();
        }
        if (detail.getLatestVersion() != null && !detail.getLatestVersion().isBlank()) {
            return detail.getLatestVersion();
        }
        return "1.0.0";
    }

    private FormulaFunctionResponse toFunctionResponse(FormulaFunctionMarketResponse response) {
        return FormulaFunctionResponse.builder()
            .name(response.getFunctionCode())
            .displayName(response.getDisplayName())
            .category(response.getCategory())
            .description(response.getDescription())
            .argumentSchema(response.getArgumentSchema())
            .returnType(response.getReturnType())
            .sourceType(response.getSourceType())
            .example(response.getExample())
            .build();
    }

    private FormulaFunctionMarketResponse fromDefinition(FormulaFunctionDefinition definition) {
        FormulaFunctionMarketResponse response = new FormulaFunctionMarketResponse();
        response.setTenantId(DEFAULT_TENANT_ID);
        response.setFunctionCode(definition.getFunctionCode());
        response.setDisplayName(definition.getDisplayName());
        response.setCategory(definition.getCategory());
        response.setDescription(definition.getDescription());
        response.setSourceType(definition.getSourceType());
        response.setReturnType(definition.getReturnType());
        response.setExample(definition.getExample());
        response.setImplementationType(definition.getImplementationType());
        response.setBeanName(definition.getBeanName());
        response.setMethodName(definition.getMethodName());
        response.setStatus(definition.getStatus());
        response.setCurrentVersion("1.0.0");
        response.setLatestVersion("1.0.0");
        response.setBuiltin("BUILTIN".equalsIgnoreCase(definition.getSourceType()));
        response.setInstallStatus(STATUS_INSTALLED);
        response.setInstalledVersion("1.0.0");
        response.setEnabled(definition.isEnabled());
        return response;
    }

    private FormulaFunctionDefinition toDefinition(FormulaFunctionRegisterRequest request,
                                                   String functionCode,
                                                   String status) {
        FormulaFunctionDefinition.Builder builder = FormulaFunctionDefinition.builder()
            .functionCode(functionCode)
            .displayName(request.getDisplayName())
            .category(request.getCategory())
            .description(request.getDescription())
            .sourceType(SOURCE_TENANT)
            .returnType(normalizeReturnType(request.getReturnType()))
            .example(request.getExample())
            .status(status)
            .implementationType(IMPLEMENTATION_JAVA_BEAN)
            .beanName(request.getBeanName())
            .methodName(request.getMethodName())
            .timeoutMs(1000L);
        appendArguments(builder, request.getArgumentSchema());
        return builder.build();
    }

    private void registerRuntimeDefinition(FormulaFunctionMarketResponse response) {
        if (response == null || response.getFunctionCode() == null || response.getFunctionCode().isBlank()) {
            return;
        }
        if (!IMPLEMENTATION_JAVA_BEAN.equalsIgnoreCase(response.getImplementationType())
                || response.getBeanName() == null || response.getBeanName().isBlank()
                || response.getMethodName() == null || response.getMethodName().isBlank()) {
            return;
        }
        String status = STATUS_ENABLED.equalsIgnoreCase(response.getStatus())
                && Boolean.TRUE.equals(response.getEnabled())
            ? STATUS_ENABLED : STATUS_DISABLED;
        FormulaFunctionDefinition.Builder builder = FormulaFunctionDefinition.builder()
            .functionCode(response.getFunctionCode())
            .displayName(response.getDisplayName())
            .category(response.getCategory())
            .description(response.getDescription())
            .sourceType(response.getSourceType())
            .returnType(response.getReturnType())
            .example(response.getExample())
            .status(status)
            .implementationType(response.getImplementationType())
            .beanName(response.getBeanName())
            .methodName(response.getMethodName())
            .timeoutMs(1000L);
        appendArguments(builder, response.getArgumentSchema());
        functionRegistry.registerDefinition(builder.build());
        functionRegistry.registerAviatorFunctions();
    }

    private void appendArguments(FormulaFunctionDefinition.Builder builder, String argumentSchema) {
        for (Map<String, Object> argument : parseArgumentSchema(argumentSchema)) {
            String name = normalizeText(argument.get("name"));
            if (name == null) {
                continue;
            }
            String type = normalizeText(argument.get("type"));
            boolean required = !Boolean.FALSE.equals(argument.get("required"));
            builder.argument(name, type == null ? "ANY" : type, required);
        }
    }

    private List<Map<String, Object>> parseArgumentSchema(String argumentSchema) {
        String normalized = normalizeArgumentSchema(argumentSchema);
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(normalized, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("参数 Schema 必须是 JSON 数组");
        }
    }

    private void validateArgumentSchema(String argumentSchema) {
        parseArgumentSchema(argumentSchema);
    }

    private String normalizeArgumentSchema(String argumentSchema) {
        String value = normalizeText(argumentSchema);
        return value == null ? "[]" : value;
    }

    private String normalizeReturnType(String returnType) {
        String value = normalizeText(returnType);
        return value == null ? "ANY" : value.toUpperCase();
    }

    private String resolveRequestVersion(String version) {
        String value = normalizeText(version);
        return value == null ? "1.0.0" : value;
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private void fillAuditFields(AiFormulaFunctionInstall install) {
        Long userId = resolveUserId();
        Long deptId = resolveMainOrgId();
        LocalDateTime now = LocalDateTime.now();
        fillAuditFields(install, userId, deptId, now);
    }

    private void fillAuditFields(com.mdframe.forge.starter.tenant.core.TenantEntity entity,
                                 Long userId,
                                 Long deptId,
                                 LocalDateTime now) {
        entity.setCreateBy(userId);
        entity.setCreateTime(now);
        entity.setCreateDept(deptId);
        entity.setUpdateBy(userId);
        entity.setUpdateTime(now);
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
        } catch (Exception e) {
            return DEFAULT_TENANT_ID;
        }
    }

    private Long resolveUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveMainOrgId() {
        try {
            return SessionHelper.getMainOrgId();
        } catch (Exception e) {
            return null;
        }
    }
}
