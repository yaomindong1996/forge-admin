package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigDTO;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCrudConfigService extends ServiceImpl<AiCrudConfigMapper, AiCrudConfig> {

    private final ObjectMapper objectMapper;
    private final MenuRegisterAdapter menuRegisterAdapter;
    @Lazy
    private final AiPageTemplateService pageTemplateService;

    public AiCrudConfig getByConfigKey(String configKey) {
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
        return buildRenderVO(config);
    }

    private AiCrudConfigRenderVO buildRenderVO(AiCrudConfig config) {
        AiCrudConfigRenderVO vo = new AiCrudConfigRenderVO();
        vo.setConfigKey(config.getConfigKey());
        vo.setTableName(config.getTableName());
        vo.setTableComment(config.getTableComment());
        vo.setRowKey("id");
        vo.setModalType("drawer");
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
            if (StringUtils.isNotBlank(config.getSearchSchema())) {
                vo.setSearchSchema(objectMapper.readValue(config.getSearchSchema(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getColumnsSchema())) {
                vo.setColumnsSchema(objectMapper.readValue(config.getColumnsSchema(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getEditSchema())) {
                vo.setEditSchema(objectMapper.readValue(config.getEditSchema(), Object.class));
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
                vo.setOptions(objectMapper.readValue(config.getOptions(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getDictConfig())) {
                vo.setDictConfig(objectMapper.readValue(config.getDictConfig(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getDesensitizeConfig())) {
                vo.setDesensitizeConfig(objectMapper.readValue(config.getDesensitizeConfig(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getEncryptConfig())) {
                vo.setEncryptConfig(objectMapper.readValue(config.getEncryptConfig(), Object.class));
            }
            if (StringUtils.isNotBlank(config.getTransConfig())) {
                vo.setTransConfig(objectMapper.readValue(config.getTransConfig(), Object.class));
            }
        } catch (Exception e) {
            log.error("[AiCrudConfigService] JSON解析失败, configKey={}", config.getConfigKey(), e);
            throw new BusinessException("配置JSON格式错误");
        }
        return vo;
    }

    private void copyDtoToEntity(AiCrudConfigDTO dto, AiCrudConfig config) {
        if (dto.getTableComment() != null) {
            config.setTableComment(dto.getTableComment());
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
        if (dto.getStatus() != null) {
            config.setStatus(dto.getStatus());
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
}
