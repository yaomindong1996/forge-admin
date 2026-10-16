package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.CustomQueryScheme;
import com.mdframe.forge.plugin.generator.dto.CustomQueryConditionDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQueryExecuteDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQuerySchemeDTO;
import com.mdframe.forge.plugin.generator.dto.CustomQuerySchemeVO;
import com.mdframe.forge.plugin.generator.mapper.CustomQuerySchemeMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义查询方案服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomQueryService extends ServiceImpl<CustomQuerySchemeMapper, CustomQueryScheme> {

    private static final int MAX_SCHEME_NAME_LENGTH = 128;

    private final CustomQuerySchemeMapper customQuerySchemeMapper;
    private final DynamicCrudService dynamicCrudService;
    private final ObjectMapper objectMapper;

    public Page<Map<String, Object>> execute(String configKey, CustomQueryExecuteDTO request) {
        return dynamicCrudService.selectCustomPage(configKey, request == null ? new CustomQueryExecuteDTO() : request);
    }

    public List<CustomQuerySchemeVO> listSchemes(String configKey) {
        validateConfigKey(configKey);
        return customQuerySchemeMapper.selectUserSchemes(currentTenantId(), currentUserId(), configKey)
                .stream()
                .map(this::toVO)
                .toList();
    }

    public CustomQuerySchemeVO getScheme(String configKey, Long id) {
        CustomQueryScheme scheme = getOwnScheme(configKey, id);
        return toVO(scheme);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createScheme(String configKey, CustomQuerySchemeDTO dto) {
        validateConfigKey(configKey);
        validateScheme(dto, false);
        if (isDefault(dto)) {
            customQuerySchemeMapper.clearDefault(currentTenantId(), currentUserId(), configKey);
        }
        CustomQueryScheme scheme = new CustomQueryScheme();
        fillScheme(configKey, dto, scheme);
        save(scheme);
        return scheme.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateScheme(String configKey, CustomQuerySchemeDTO dto) {
        validateConfigKey(configKey);
        validateScheme(dto, true);
        CustomQueryScheme existing = getOwnScheme(configKey, dto.getId());
        if (isDefault(dto)) {
            customQuerySchemeMapper.clearDefault(currentTenantId(), currentUserId(), configKey);
        }
        fillScheme(configKey, dto, existing);
        existing.setCreateBy(null);
        existing.setCreateTime(null);
        existing.setCreateDept(null);
        updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteScheme(String configKey, Long id) {
        validateConfigKey(configKey);
        int deleted = customQuerySchemeMapper.deleteUserScheme(currentTenantId(), currentUserId(), configKey, normalizeId(id));
        if (deleted <= 0) {
            throw new BusinessException("查询方案不存在");
        }
    }

    private CustomQueryScheme getOwnScheme(String configKey, Long id) {
        validateConfigKey(configKey);
        CustomQueryScheme scheme = customQuerySchemeMapper.selectUserScheme(
                currentTenantId(), currentUserId(), configKey, normalizeId(id));
        if (scheme == null) {
            throw new BusinessException("查询方案不存在");
        }
        return scheme;
    }

    private void fillScheme(String configKey, CustomQuerySchemeDTO dto, CustomQueryScheme scheme) {
        scheme.setTenantId(currentTenantId());
        scheme.setConfigKey(configKey);
        scheme.setSchemeName(trim(dto.getSchemeName(), MAX_SCHEME_NAME_LENGTH));
        scheme.setConditionsJson(writeJson(defaultList(dto.getConditions())));
        scheme.setColumnsJson(writeJson(defaultList(dto.getFields())));
        scheme.setSortJson(writeJson(buildSortConfig(dto)));
        scheme.setDisplayJson(writeJson(buildDisplayConfig(dto)));
        scheme.setIsDefault(isDefault(dto) ? 1 : 0);
        scheme.setRemark(trimToNull(dto.getRemark(), 500));
    }

    private Map<String, Object> buildSortConfig(CustomQuerySchemeDTO dto) {
        Map<String, Object> sort = new LinkedHashMap<>();
        if (StringUtils.hasText(dto.getOrderByColumn())) {
            sort.put("orderByColumn", dto.getOrderByColumn().trim());
        }
        if (StringUtils.hasText(dto.getIsAsc())) {
            sort.put("isAsc", dto.getIsAsc().trim());
        }
        return sort;
    }

    private Map<String, Object> buildDisplayConfig(CustomQuerySchemeDTO dto) {
        Map<String, Object> display = new LinkedHashMap<>();
        display.put("renderMode", "card".equals(dto.getRenderMode()) ? "card" : "table");
        return display;
    }

    private CustomQuerySchemeVO toVO(CustomQueryScheme scheme) {
        CustomQuerySchemeVO vo = new CustomQuerySchemeVO();
        vo.setId(scheme.getId());
        vo.setConfigKey(scheme.getConfigKey());
        vo.setSchemeName(scheme.getSchemeName());
        vo.setConditions(readConditions(scheme.getConditionsJson()));
        vo.setFields(readFields(scheme.getColumnsJson()));
        fillSortVO(vo, scheme.getSortJson());
        fillDisplayVO(vo, scheme.getDisplayJson());
        vo.setIsDefault(scheme.getIsDefault());
        vo.setRemark(scheme.getRemark());
        vo.setCreateTime(scheme.getCreateTime());
        vo.setUpdateTime(scheme.getUpdateTime());
        return vo;
    }

    private void fillSortVO(CustomQuerySchemeVO vo, String sortJson) {
        try {
            if (!StringUtils.hasText(sortJson)) {
                return;
            }
            JsonNode sort = objectMapper.readTree(sortJson);
            vo.setOrderByColumn(readText(sort, "orderByColumn"));
            vo.setIsAsc(readText(sort, "isAsc"));
        } catch (Exception e) {
            log.warn("[CustomQueryService] 解析排序配置失败, schemeId={}", vo.getId(), e);
        }
    }

    private void fillDisplayVO(CustomQuerySchemeVO vo, String displayJson) {
        try {
            if (!StringUtils.hasText(displayJson)) {
                vo.setRenderMode("table");
                return;
            }
            JsonNode display = objectMapper.readTree(displayJson);
            String renderMode = readText(display, "renderMode");
            vo.setRenderMode("card".equals(renderMode) ? "card" : "table");
        } catch (Exception e) {
            vo.setRenderMode("table");
            log.warn("[CustomQueryService] 解析展示配置失败, schemeId={}", vo.getId(), e);
        }
    }

    private String readText(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<CustomQueryConditionDTO> readConditions(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CustomQueryConditionDTO>>() {});
        } catch (Exception e) {
            log.warn("[CustomQueryService] 解析查询条件失败", e);
            return Collections.emptyList();
        }
    }

    private List<String> readFields(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[CustomQueryService] 解析展示字段失败", e);
            return Collections.emptyList();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("查询方案JSON序列化失败");
        }
    }

    private <T> List<T> defaultList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private void validateScheme(CustomQuerySchemeDTO dto, boolean update) {
        if (dto == null) {
            throw new BusinessException("查询方案不能为空");
        }
        if (update) {
            normalizeId(dto.getId());
        }
        if (!StringUtils.hasText(dto.getSchemeName())) {
            throw new BusinessException("查询方案名称不能为空");
        }
    }

    private void validateConfigKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new BusinessException("configKey不能为空");
        }
    }

    private boolean isDefault(CustomQuerySchemeDTO dto) {
        return dto != null && Integer.valueOf(1).equals(dto.getIsDefault());
    }

    private Long normalizeId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("查询方案ID不能为空");
        }
        return id;
    }

    private Long currentTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Long currentUserId() {
        Long userId = SessionHelper.getUserId();
        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }
        return userId;
    }

    private String trim(String value, int maxLength) {
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private String trimToNull(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return trim(value, maxLength);
    }
}
