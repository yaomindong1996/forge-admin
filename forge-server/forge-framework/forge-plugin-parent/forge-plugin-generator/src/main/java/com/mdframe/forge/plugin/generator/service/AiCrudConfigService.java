package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessDocumentConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigDTO;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRuntimeConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessDocumentConfigService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeRuntimeConfigBuilder;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCrudConfigService extends ServiceImpl<AiCrudConfigMapper, AiCrudConfig> {

    private final ObjectMapper objectMapper;
    private final MenuRegisterAdapter menuRegisterAdapter;
    private final LowcodeRuntimeConfigBuilder lowcodeRuntimeConfigBuilder;
    private final BusinessDocumentConfigService documentConfigService;
    private final AiCrudConfigVersionMapper versionMapper;
    @Lazy
    private final AiPageTemplateService pageTemplateService;

    public AiCrudConfig getByConfigKey(String configKey) {
        Long tenantId = getCurrentTenantId();
        if (tenantId != null) {
            return baseMapper.selectByConfigKey(tenantId, configKey);
        }
        return getOne(new LambdaQueryWrapper<AiCrudConfig>()
                .eq(AiCrudConfig::getConfigKey, configKey));
    }

    public Page<AiCrudConfig> listPage(PageQuery pageQuery, String configKey, String tableName) {
        LambdaQueryWrapper<AiCrudConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(configKey), AiCrudConfig::getConfigKey, configKey)
                .like(StringUtils.isNotBlank(tableName), AiCrudConfig::getTableName, tableName)
                .orderByDesc(AiCrudConfig::getCreateTime);
        return page(pageQuery.toPage(), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createConfig(AiCrudConfigDTO dto) {
        if (StringUtils.isBlank(dto.getConfigKey())) {
            throw new BusinessException("configKey不能为空");
        }
        if (!dto.getConfigKey().matches("^[a-z][a-z0-9_]{1,63}$")) {
            throw new BusinessException("configKey格式不正确（小写字母开头，仅含小写字母+数字+下划线，2-64字符）");
        }
        AiCrudConfig existing = getByConfigKey(dto.getConfigKey());
        if (existing != null) {
            throw new BusinessException("configKey已存在: " + dto.getConfigKey());
        }
        if ("CONFIG".equals(dto.getMode())) {
            if (StringUtils.isBlank(dto.getTableName())) {
                throw new BusinessException("配置驱动模式必须指定tableName");
            }
            AiCrudConfig tableExisting = getOne(new LambdaQueryWrapper<AiCrudConfig>()
                    .eq(AiCrudConfig::getTableName, dto.getTableName())
                    .eq(AiCrudConfig::getMode, "CONFIG"));
            if (tableExisting != null) {
                throw new BusinessException("该表已有配置驱动的CRUD配置: " + dto.getTableName());
            }
        }
        validateJsonFields(dto);

        AiCrudConfig config = new AiCrudConfig();
        copyDtoToEntityOnCreate(dto, config);
        config.setStatus(StringUtils.isNotBlank(dto.getStatus()) ? dto.getStatus() : "0");
        config.setMode(StringUtils.isNotBlank(dto.getMode()) ? dto.getMode() : "CONFIG");
        config.setBuildMode(StringUtils.isNotBlank(dto.getBuildMode()) ? dto.getBuildMode() : "AI");
        config.setPublishStatus(StringUtils.isNotBlank(dto.getPublishStatus()) ? dto.getPublishStatus() : "PUBLISHED");
        config.setDraftVersion(dto.getDraftVersion() != null ? dto.getDraftVersion() : 0);
        config.setPublishedVersion(dto.getPublishedVersion() != null ? dto.getPublishedVersion() : 1);
        if (StringUtils.isBlank(config.getAppName())) {
            config.setAppName(StringUtils.defaultIfBlank(dto.getMenuName(), config.getTableComment()));
        }
        save(config);

        if ("CONFIG".equals(config.getMode())) {
            String menuName = StringUtils.isNotBlank(dto.getMenuName()) ? dto.getMenuName() : config.getTableComment();
            Long parentId = dto.getMenuParentId() != null ? dto.getMenuParentId() : 0L;
            Integer sort = dto.getMenuSort() != null ? dto.getMenuSort() : 0;
            try {
                Long menuResourceId = menuRegisterAdapter.registerMenu(menuName, parentId, config.getConfigKey(), sort);
                config.setMenuResourceId(menuResourceId);
                updateById(config);
            } catch (Exception e) {
                log.warn("[AiCrudConfigService] 菜单注册失败, configKey={}, 将继续保存配置", config.getConfigKey(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(AiCrudConfigDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("ID不能为空");
        }
        AiCrudConfig config = getById(dto.getId());
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        validateJsonFields(dto);
        copyDtoToEntity(dto, config);
        updateById(config);

        if (config.getMenuResourceId() != null) {
            try {
                String menuName = StringUtils.isNotBlank(dto.getMenuName()) ? dto.getMenuName() : config.getTableComment();
                Integer sort = dto.getMenuSort() != null ? dto.getMenuSort() : 0;
                menuRegisterAdapter.updateMenu(config.getMenuResourceId(), menuName, sort);
            } catch (Exception e) {
                log.warn("[AiCrudConfigService] 菜单更新失败, configKey={}", config.getConfigKey(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        AiCrudConfig config = getById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        if (config.getMenuResourceId() != null) {
            // 校验：若该菜单已被某角色赋权，则不允许删除
            if (menuRegisterAdapter.hasRolePermission(config.getMenuResourceId())) {
                throw new BusinessException("该配置关联的菜单已被角色赋权，不允许直接删除。请先在【系统管理-角色管理】中移除相关角色对该菜单的授权后再操作。");
            }
            try {
                menuRegisterAdapter.deleteMenu(config.getMenuResourceId());
            } catch (Exception e) {
                log.warn("[AiCrudConfigService] 菜单删除失败, menuResourceId={}", config.getMenuResourceId(), e);
            }
        }
        removeById(id);
    }

    public AiCrudConfigRenderVO getRenderConfig(String configKey) {
        AiCrudConfig config = getByConfigKey(configKey);
        if (config == null || "1".equals(config.getStatus())) {
            throw new BusinessException("CRUD配置不存在或已停用");
        }
        if (!"CONFIG".equals(config.getMode())) {
            throw new BusinessException("该配置不是配置驱动模式");
        }
        if ("LOWCODE".equals(config.getBuildMode()) && !"PUBLISHED".equals(config.getPublishStatus())) {
            throw new BusinessException("低代码应用尚未发布");
        }
        return buildRenderConfig(resolvePublishedRuntimeConfig(config));
    }

    private AiCrudConfig resolvePublishedRuntimeConfig(AiCrudConfig config) {
        if (config == null
                || !"LOWCODE".equals(config.getBuildMode())
                || !"PUBLISHED".equals(config.getPublishStatus())) {
            return config;
        }
        Integer versionNo = config.getPublishedVersion();
        if (versionNo == null || versionNo <= 0) {
            log.warn("[AiCrudConfigService] 已发布配置缺少 publishedVersion, configKey={}", config.getConfigKey());
            return config;
        }
        Long tenantId = config.getTenantId() == null ? getCurrentTenantId() : config.getTenantId();
        AiCrudConfigVersion version = versionMapper.selectVersionByNo(tenantId, config.getId(), versionNo);
        if (version == null) {
            log.warn("[AiCrudConfigService] 未找到发布版本快照, configKey={}, versionNo={}", config.getConfigKey(), versionNo);
            return config;
        }
        AiCrudConfig published = new AiCrudConfig();
        published.setId(config.getId());
        published.setTenantId(config.getTenantId());
        published.setConfigKey(config.getConfigKey());
        published.setTableName(config.getTableName());
        published.setTableComment(config.getTableComment());
        published.setAppName(config.getAppName());
        published.setMenuName(config.getMenuName());
        published.setMode(config.getMode());
        published.setBuildMode(config.getBuildMode());
        published.setStatus(config.getStatus());
        published.setPublishStatus(config.getPublishStatus());
        published.setMenuParentId(config.getMenuParentId());
        published.setMenuSort(config.getMenuSort());
        published.setMenuResourceId(config.getMenuResourceId());
        published.setLayoutType(config.getLayoutType());
        published.setDomainId(version.getDomainId() == null ? config.getDomainId() : version.getDomainId());
        published.setDomainCode(StringUtils.defaultIfBlank(version.getDomainCode(), config.getDomainCode()));
        published.setObjectCode(StringUtils.defaultIfBlank(version.getObjectCode(), config.getObjectCode()));
        published.setObjectName(StringUtils.defaultIfBlank(version.getObjectName(), config.getObjectName()));
        published.setDraftVersion(config.getDraftVersion());
        published.setPublishedVersion(config.getPublishedVersion());
        published.setPublishTime(config.getPublishTime());
        published.setPublishBy(config.getPublishBy());
        published.setModelSchema(StringUtils.defaultIfBlank(version.getModelSchema(), config.getModelSchema()));
        published.setPageSchema(StringUtils.defaultIfBlank(version.getPageSchema(), config.getPageSchema()));
        published.setSearchSchema(StringUtils.defaultIfBlank(version.getSearchSchema(), config.getSearchSchema()));
        published.setColumnsSchema(StringUtils.defaultIfBlank(version.getColumnsSchema(), config.getColumnsSchema()));
        published.setEditSchema(StringUtils.defaultIfBlank(version.getEditSchema(), config.getEditSchema()));
        published.setApiConfig(StringUtils.defaultIfBlank(version.getApiConfig(), config.getApiConfig()));
        published.setOptions(StringUtils.defaultIfBlank(version.getOptions(), config.getOptions()));
        applyPublishedSnapshotFields(published, config, version);
        return published;
    }

    private void applyPublishedSnapshotFields(AiCrudConfig published, AiCrudConfig draft, AiCrudConfigVersion version) {
        if (StringUtils.isBlank(version.getPublishSnapshot())) {
            published.setDictConfig(draft.getDictConfig());
            published.setDesensitizeConfig(draft.getDesensitizeConfig());
            published.setEncryptConfig(draft.getEncryptConfig());
            published.setTransConfig(draft.getTransConfig());
            return;
        }
        try {
            Map<String, Object> snapshot = objectMapper.readValue(
                    version.getPublishSnapshot(), new TypeReference<Map<String, Object>>() {});
            published.setTableName(StringUtils.defaultIfBlank(text(snapshot.get("tableName")), published.getTableName()));
            published.setTableComment(StringUtils.defaultIfBlank(text(snapshot.get("tableComment")), draft.getTableComment()));
            published.setAppName(StringUtils.defaultIfBlank(text(snapshot.get("appName")), draft.getAppName()));
            published.setMenuName(StringUtils.defaultIfBlank(text(snapshot.get("menuName")), draft.getMenuName()));
            published.setLayoutType(StringUtils.defaultIfBlank(text(snapshot.get("layoutType")), draft.getLayoutType()));
            published.setDictConfig(StringUtils.defaultIfBlank(text(snapshot.get("dictConfig")), draft.getDictConfig()));
            published.setDesensitizeConfig(StringUtils.defaultIfBlank(text(snapshot.get("desensitizeConfig")), draft.getDesensitizeConfig()));
            published.setEncryptConfig(StringUtils.defaultIfBlank(text(snapshot.get("encryptConfig")), draft.getEncryptConfig()));
            published.setTransConfig(StringUtils.defaultIfBlank(text(snapshot.get("transConfig")), draft.getTransConfig()));
        } catch (Exception e) {
            log.warn("[AiCrudConfigService] 读取发布快照失败, configKey={}, versionNo={}",
                    draft.getConfigKey(), version.getVersionNo(), e);
            published.setDictConfig(draft.getDictConfig());
            published.setDesensitizeConfig(draft.getDesensitizeConfig());
            published.setEncryptConfig(draft.getEncryptConfig());
            published.setTransConfig(draft.getTransConfig());
        }
    }

    public AiCrudConfigRenderVO buildRenderConfig(AiCrudConfig config) {
        AiCrudConfigRenderVO vo = new AiCrudConfigRenderVO();
        vo.setConfigKey(config.getConfigKey());
        vo.setTableName(config.getTableName());
        vo.setTableComment(config.getTableComment());
        vo.setAppName(config.getAppName());
        vo.setMenuName(config.getMenuName());
        vo.setObjectCode(config.getObjectCode());
        vo.setObjectName(config.getObjectName());
        vo.setPublishStatus(config.getPublishStatus());
        vo.setPublishedVersion(config.getPublishedVersion());
        vo.setRowKey("id");
        vo.setModalType("modal");
        vo.setModalWidth("800px");
        vo.setEditGridCols(1);
        vo.setSearchGridCols(4);
        // 载入模板默认配置
        String layoutType = StringUtils.isNotBlank(config.getLayoutType()) ? config.getLayoutType() : "simple-crud";
        vo.setLayoutType(layoutType);
        try {
            AiPageTemplate template = pageTemplateService.getByTemplateKey(layoutType);
            if (template != null && StringUtils.isNotBlank(template.getDefaultConfig())) {
                vo.setTemplateDefaultConfig(objectMapper.readValue(template.getDefaultConfig(), Object.class));
            }
        } catch (Exception e) {
            log.debug("[AiCrudConfigService] 加载模板默认配置失败, layoutType={}", layoutType);
        }
        try {
            if (StringUtils.isNotBlank(config.getModelSchema())) {
                vo.setModelSchema(objectMapper.readValue(config.getModelSchema(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getPageSchema())) {
                vo.setPageSchema(objectMapper.readValue(config.getPageSchema(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getModelSchema()) && StringUtils.isNotBlank(config.getPageSchema())) {
                if (hasStoredRuntimeConfig(config)) {
                    applyStoredRuntimeConfig(config, vo);
                } else {
                    applyLowcodeRuntimeConfig(config, vo);
                }
            } else {
                applyLegacyRuntimeConfig(config, vo);
            }
        } catch (Exception e) {
            log.error("[AiCrudConfigService] JSON解析失败, configKey={}", config.getConfigKey(), e);
            throw new BusinessException("配置JSON格式错误");
        }
        applyDocumentRuntimeHints(config, vo);
        return vo;
    }

    private boolean hasStoredRuntimeConfig(AiCrudConfig config) {
        return StringUtils.isNotBlank(config.getSearchSchema())
                && StringUtils.isNotBlank(config.getColumnsSchema())
                && StringUtils.isNotBlank(config.getEditSchema())
                && StringUtils.isNotBlank(config.getApiConfig());
    }

    private void applyLowcodeRuntimeConfig(AiCrudConfig config, AiCrudConfigRenderVO vo) throws Exception {
        LowcodeModelSchema modelSchema = objectMapper.readValue(config.getModelSchema(), LowcodeModelSchema.class);
        LowcodePageSchema pageSchema = objectMapper.readValue(config.getPageSchema(), LowcodePageSchema.class);
        LowcodeRuntimeConfig runtimeConfig = lowcodeRuntimeConfigBuilder.buildRuntimeConfig(
                config.getConfigKey(), modelSchema, pageSchema);

        vo.setLayoutType(runtimeConfig.getLayoutType());
        vo.setTableName(runtimeConfig.getTableName());
        vo.setTableComment(runtimeConfig.getTableComment());
        vo.setSearchSchema(readJson(runtimeConfig.getSearchSchema()));
        vo.setColumnsSchema(readJson(runtimeConfig.getColumnsSchema()));
        vo.setEditSchema(readJson(runtimeConfig.getEditSchema()));
        vo.setApiConfig(readJson(runtimeConfig.getApiConfig()));
        vo.setOptions(readJson(runtimeConfig.getOptions()));
        vo.setDictConfig(readJson(runtimeConfig.getDictConfig()));
        vo.setDesensitizeConfig(readJson(runtimeConfig.getDesensitizeConfig()));
        vo.setEncryptConfig(readJson(runtimeConfig.getEncryptConfig()));
        vo.setTransConfig(readJson(runtimeConfig.getTransConfig()));
    }

    private void applyStoredRuntimeConfig(AiCrudConfig config, AiCrudConfigRenderVO vo) throws Exception {
        vo.setSearchSchema(readJson(config.getSearchSchema()));
        vo.setColumnsSchema(readJson(config.getColumnsSchema()));
        vo.setEditSchema(readJson(config.getEditSchema()));
        vo.setApiConfig(readJson(config.getApiConfig()));
        if (StringUtils.isNotBlank(config.getOptions())) {
            vo.setOptions(readJson(config.getOptions()));
        }
        if (StringUtils.isNotBlank(config.getDictConfig())) {
            vo.setDictConfig(readJson(config.getDictConfig()));
        }
        if (StringUtils.isNotBlank(config.getDesensitizeConfig())) {
            vo.setDesensitizeConfig(readJson(config.getDesensitizeConfig()));
        }
        if (StringUtils.isNotBlank(config.getEncryptConfig())) {
            vo.setEncryptConfig(readJson(config.getEncryptConfig()));
        }
        if (StringUtils.isNotBlank(config.getTransConfig())) {
            vo.setTransConfig(readJson(config.getTransConfig()));
        }
    }

    private void applyLegacyRuntimeConfig(AiCrudConfig config, AiCrudConfigRenderVO vo) throws Exception {
        if (StringUtils.isNotBlank(config.getSearchSchema())) {
            vo.setSearchSchema(readJson(config.getSearchSchema()));
        }
        if (StringUtils.isNotBlank(config.getColumnsSchema())) {
            vo.setColumnsSchema(readJson(config.getColumnsSchema()));
        }
        if (StringUtils.isNotBlank(config.getEditSchema())) {
            vo.setEditSchema(readJson(config.getEditSchema()));
        }
        // 强制使用动态路由，避免 AI 生成错误的 URL
        java.util.Map<String, Object> apiConfig = new java.util.HashMap<>();
        String ck = config.getConfigKey();
        apiConfig.put("list", "get@/ai/crud/" + ck + "/page");
        apiConfig.put("detail", "get@/ai/crud/" + ck + "/:id");
        apiConfig.put("create", "post@/ai/crud/" + ck);
        apiConfig.put("update", "put@/ai/crud/" + ck);
        apiConfig.put("delete", "delete@/ai/crud/" + ck + "/:id");
        vo.setApiConfig(apiConfig);
        if (StringUtils.isNotBlank(config.getOptions())) {
            vo.setOptions(readJson(config.getOptions()));
        }
        if (StringUtils.isNotBlank(config.getDictConfig())) {
            vo.setDictConfig(readJson(config.getDictConfig()));
        }
        if (StringUtils.isNotBlank(config.getDesensitizeConfig())) {
            vo.setDesensitizeConfig(readJson(config.getDesensitizeConfig()));
        }
        if (StringUtils.isNotBlank(config.getEncryptConfig())) {
            vo.setEncryptConfig(readJson(config.getEncryptConfig()));
        }
        if (StringUtils.isNotBlank(config.getTransConfig())) {
            vo.setTransConfig(readJson(config.getTransConfig()));
        }
    }

    private Object readJson(String json) throws Exception {
        return objectMapper.readValue(json, Object.class);
    }

    private void applyDocumentRuntimeHints(AiCrudConfig config, AiCrudConfigRenderVO vo) {
        AiBusinessDocumentConfig documentConfig = documentConfigService.selectEnabledByConfigKey(
                config.getTenantId(), config.getConfigKey());
        if (documentConfig == null && StringUtils.isNotBlank(config.getObjectCode())) {
            documentConfig = documentConfigService.selectEnabledByObjectCode(config.getTenantId(), config.getObjectCode());
        }
        if (documentConfig == null) {
            return;
        }
        String documentNoField = documentConfigService.resolveDocumentNoField(documentConfig, config);
        if (StringUtils.isBlank(documentNoField)) {
            return;
        }
        vo.setOptions(mergeDocumentNoOption(vo.getOptions(), documentNoField));
        vo.setEditSchema(markDocumentNoFieldReadonly(vo.getEditSchema(), documentNoField));
    }

    private Object mergeDocumentNoOption(Object options, String documentNoField) {
        Map<String, Object> optionMap = toObjectMap(options);
        optionMap.put("documentNoField", documentNoField);
        return optionMap;
    }

    private Object markDocumentNoFieldReadonly(Object editSchema, String documentNoField) {
        if (editSchema == null || StringUtils.isBlank(documentNoField)) {
            return editSchema;
        }
        List<Map<String, Object>> fields;
        try {
            fields = objectMapper.convertValue(editSchema, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return editSchema;
        }
        Set<String> aliases = new java.util.LinkedHashSet<>();
        aliases.add(documentNoField);
        aliases.add(snakeToCamel(documentNoField));
        aliases.add(camelToSnake(documentNoField));
        List<Map<String, Object>> result = new ArrayList<>(fields.size());
        for (Map<String, Object> field : fields) {
            Map<String, Object> item = new LinkedHashMap<>(field);
            String fieldName = StringUtils.firstNonBlank(
                    text(item.get("field")),
                    text(item.get("prop")),
                    text(item.get("key")),
                    text(item.get("model"))
            );
            if (StringUtils.isNotBlank(fieldName)
                    && (aliases.contains(fieldName)
                    || aliases.contains(snakeToCamel(fieldName))
                    || aliases.contains(camelToSnake(fieldName)))) {
                item.put("disabled", true);
                item.put("readonly", true);
                item.put("required", false);
                item.put("rules", List.of());
                Map<String, Object> props = toObjectMap(item.get("props"));
                props.put("disabled", true);
                props.put("readonly", true);
                props.put("clearable", false);
                props.put("placeholder", "系统自动生成");
                item.put("props", props);
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, itemValue) -> {
                if (key != null) {
                    result.put(String.valueOf(key), itemValue);
                }
            });
            return result;
        }
        try {
            return objectMapper.convertValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String snakeToCamel(String value) {
        if (StringUtils.isBlank(value) || !value.contains("_")) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            result.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return result.toString();
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                result.append('_').append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void copyDtoToEntity(AiCrudConfigDTO dto, AiCrudConfig config) {
        if (dto.getTableComment() != null) {
            config.setTableComment(dto.getTableComment());
        }
        if (dto.getAppName() != null) {
            config.setAppName(dto.getAppName());
        }
        if (dto.getSearchSchema() != null) {
            config.setSearchSchema(dto.getSearchSchema());
        }
        if (dto.getColumnsSchema() != null) {
            config.setColumnsSchema(dto.getColumnsSchema());
        }
        if (dto.getEditSchema() != null) {
            config.setEditSchema(dto.getEditSchema());
        }
        if (dto.getApiConfig() != null) {
            config.setApiConfig(dto.getApiConfig());
        }
        if (dto.getOptions() != null) {
            config.setOptions(dto.getOptions());
        }
        if (dto.getMode() != null) {
            config.setMode(dto.getMode());
        }
        if (dto.getBuildMode() != null) {
            config.setBuildMode(dto.getBuildMode());
        }
        if (dto.getStatus() != null) {
            config.setStatus(dto.getStatus());
        }
        if (dto.getPublishStatus() != null) {
            config.setPublishStatus(dto.getPublishStatus());
        }
        if (dto.getMenuName() != null) {
            config.setMenuName(dto.getMenuName());
        }
        if (dto.getMenuParentId() != null) {
            config.setMenuParentId(dto.getMenuParentId());
        }
        if (dto.getMenuSort() != null) {
            config.setMenuSort(dto.getMenuSort());
        }
        if (dto.getDictConfig() != null) {
            config.setDictConfig(dto.getDictConfig());
        }
        if (dto.getDesensitizeConfig() != null) {
            config.setDesensitizeConfig(dto.getDesensitizeConfig());
        }
        if (dto.getEncryptConfig() != null) {
            config.setEncryptConfig(dto.getEncryptConfig());
        }
        if (dto.getTransConfig() != null) {
            config.setTransConfig(dto.getTransConfig());
        }
        if (dto.getLayoutType() != null) {
            config.setLayoutType(dto.getLayoutType());
        }
        if (dto.getModelSchema() != null) {
            config.setModelSchema(dto.getModelSchema());
        }
        if (dto.getPageSchema() != null) {
            config.setPageSchema(dto.getPageSchema());
        }
        if (dto.getDraftVersion() != null) {
            config.setDraftVersion(dto.getDraftVersion());
        }
        if (dto.getPublishedVersion() != null) {
            config.setPublishedVersion(dto.getPublishedVersion());
        }
    }

    private void copyDtoToEntityOnCreate(AiCrudConfigDTO dto, AiCrudConfig config) {
        if (dto.getConfigKey() != null) {
            config.setConfigKey(dto.getConfigKey());
        }
        if (dto.getTableName() != null) {
            config.setTableName(dto.getTableName());
        }
        copyDtoToEntity(dto, config);
    }

    private void validateJsonFields(AiCrudConfigDTO dto) {
        validateJson(dto.getSearchSchema(), "searchSchema");
        validateJson(dto.getColumnsSchema(), "columnsSchema");
        validateJson(dto.getEditSchema(), "editSchema");
        validateJson(dto.getApiConfig(), "apiConfig");
        validateJson(dto.getOptions(), "options");
        validateJson(dto.getDictConfig(), "dictConfig");
        validateJson(dto.getDesensitizeConfig(), "desensitizeConfig");
        validateJson(dto.getEncryptConfig(), "encryptConfig");
        validateJson(dto.getTransConfig(), "transConfig");
        validateJson(dto.getModelSchema(), "modelSchema");
        validateJson(dto.getPageSchema(), "pageSchema");
    }

    private void validateJson(String json, String fieldName) {
        if (StringUtils.isBlank(json)) {
            return;
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(fieldName + " JSON格式不正确");
        }
    }

    private Long getCurrentTenantId() {
        try {
            return SessionHelper.getTenantId();
        } catch (Exception e) {
            return null;
        }
    }
}
