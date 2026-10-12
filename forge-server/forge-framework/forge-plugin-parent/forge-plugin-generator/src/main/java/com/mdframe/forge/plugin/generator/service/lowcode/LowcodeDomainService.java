package com.mdframe.forge.plugin.generator.service.lowcode;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeDomain;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeDomainSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeDomainMapper;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDomainTreeVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDomainVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeDomainWorkspaceVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 低代码业务领域服务。
 */
@Service
@RequiredArgsConstructor
public class LowcodeDomainService extends ServiceImpl<AiLowcodeDomainMapper, AiLowcodeDomain> {

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    private static final long ROOT_PARENT_ID = 0L;
    /** 业务领域在发布作用域缓存（{@link LowcodePublishScopeCache}）中的键前缀，写操作后按前缀失效。 */
    private static final String DOMAIN_CACHE_PREFIX = "domain|";
    private static final Pattern DOMAIN_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,47}$");
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*$");
    private static final String DEFAULT_APP_TYPE = "SINGLE";
    private static final String DEFAULT_LAYOUT_TYPE = "simple-crud";
    private static final Set<String> AUDIT_FIELDS = Set.of(
            "id", "tenantId", "createBy", "createTime", "createDept", "updateBy", "updateTime", "delFlag"
    );
    private static final Set<String> AUDIT_COLUMNS = Set.of(
            "id", "tenant_id", "create_by", "create_time", "create_dept", "update_by", "update_time", "del_flag"
    );

    private final ObjectMapper objectMapper;

    public Page<LowcodeDomainVO> page(PageQuery pageQuery, String keyword, String status, Long parentId) {
        Long tenantId = resolveTenantId();
        Page<AiLowcodeDomain> domainPage = baseMapper.selectDomainPage(
                new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()),
                tenantId,
                StringUtils.trimToNull(keyword),
                StringUtils.trimToNull(status),
                parentId);
        Page<LowcodeDomainVO> result = new Page<>(domainPage.getCurrent(), domainPage.getSize(), domainPage.getTotal());
        result.setRecords(domainPage.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    public List<LowcodeDomainTreeVO> tree(String keyword, String status) {
        Long tenantId = resolveTenantId();
        List<AiLowcodeDomain> domains = baseMapper.selectDomainList(
                tenantId,
                StringUtils.trimToNull(keyword),
                StringUtils.trimToNull(status));
        List<LowcodeDomainTreeVO> nodes = domains.stream()
                .map(this::toTreeVO)
                .sorted(Comparator.comparing((LowcodeDomainTreeVO node) -> node.getSort() == null ? 0 : node.getSort())
                        .thenComparing(node -> node.getId() == null ? 0L : node.getId()))
                .toList();
        Map<Long, LowcodeDomainTreeVO> nodeMap = new LinkedHashMap<>();
        Map<Long, List<LowcodeDomainTreeVO>> childrenMap = new LinkedHashMap<>();
        for (LowcodeDomainTreeVO node : nodes) {
            nodeMap.put(node.getId(), node);
            Long parentId = normalizeParentId(node.getParentId());
            childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
        }
        List<LowcodeDomainTreeVO> roots = new ArrayList<>();
        for (LowcodeDomainTreeVO node : nodes) {
            Long parentId = normalizeParentId(node.getParentId());
            if (ROOT_PARENT_ID == parentId || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                LowcodeDomainTreeVO parent = nodeMap.get(parentId);
                parent.setChildren(childrenMap.getOrDefault(parent.getId(), new ArrayList<>()));
            }
        }
        for (LowcodeDomainTreeVO node : nodes) {
            node.setChildren(childrenMap.getOrDefault(node.getId(), new ArrayList<>()));
        }
        return roots;
    }

    public LowcodeDomainVO getDetail(Long id) {
        return toVO(requireDomain(id));
    }

    public LowcodeDomainVO getDefaults(Long id) {
        return getDetail(id);
    }

    public LowcodeDomainWorkspaceVO workspace(Long id) {
        AiLowcodeDomain domain = requireDomain(id);
        Long tenantId = resolveTenantId();
        LowcodeDomainWorkspaceVO workspace = baseMapper.selectWorkspaceSummary(tenantId, id);
        if (workspace == null) {
            workspace = new LowcodeDomainWorkspaceVO();
        }
        workspace.setDomain(toVO(domain));
        workspace.setObjects(baseMapper.selectObjectOverviews(tenantId, id));
        workspace.setRecentVersions(baseMapper.selectRecentVersions(tenantId, id));
        return workspace;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(LowcodeDomainDTO dto) {
        if (dto == null) {
            throw new BusinessException("业务领域不能为空");
        }
        Long tenantId = resolveTenantId();
        AiLowcodeDomain domain = new AiLowcodeDomain();
        copyDtoToEntity(dto, domain, tenantId, true);
        save(domain);
        LowcodePublishScopeCache.invalidatePrefix(DOMAIN_CACHE_PREFIX);
        return domain.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(LowcodeDomainDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("业务领域ID不能为空");
        }
        AiLowcodeDomain domain = requireDomain(dto.getId());
        copyDtoToEntity(dto, domain, resolveTenantId(), false);
        updateById(domain);
        LowcodePublishScopeCache.invalidatePrefix(DOMAIN_CACHE_PREFIX);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        AiLowcodeDomain domain = requireDomain(id);
        validateStatus(status);
        domain.setStatus(status);
        updateById(domain);
        LowcodePublishScopeCache.invalidatePrefix(DOMAIN_CACHE_PREFIX);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiLowcodeDomain domain = requireDomain(id);
        Long tenantId = resolveTenantId();
        if (baseMapper.countChildren(tenantId, id) > 0) {
            throw new BusinessException("存在下级业务领域，不能删除");
        }
        if (baseMapper.countAppsByDomainId(tenantId, id) > 0) {
            throw new BusinessException("该业务领域已存在低代码应用，请先迁移应用或停用领域");
        }
        removeById(domain.getId());
        LowcodePublishScopeCache.invalidatePrefix(DOMAIN_CACHE_PREFIX);
    }

    public AiLowcodeDomain requireEnabledDomain(Long id) {
        AiLowcodeDomain domain = requireDomain(id);
        if (!STATUS_ENABLED.equals(domain.getStatus())) {
            throw new BusinessException("业务领域已停用，不能创建或迁入应用");
        }
        return domain;
    }

    public AiLowcodeDomain requireDomain(Long id) {
        if (id == null) {
            throw new BusinessException("业务领域ID不能为空");
        }
        Long tenantId = resolveTenantId();
        AiLowcodeDomain domain = LowcodePublishScopeCache.get(
                DOMAIN_CACHE_PREFIX + "id|" + tenantId + "|" + id,
                () -> baseMapper.selectDomainById(tenantId, id));
        if (domain == null) {
            throw new BusinessException("业务领域不存在");
        }
        return domain;
    }

    public AiLowcodeDomain getByCode(String domainCode) {
        if (StringUtils.isBlank(domainCode)) {
            return null;
        }
        Long tenantId = resolveTenantId();
        return LowcodePublishScopeCache.get(
                DOMAIN_CACHE_PREFIX + "code|" + tenantId + "|" + domainCode,
                () -> baseMapper.selectByCode(tenantId, domainCode));
    }

    private void copyDtoToEntity(LowcodeDomainDTO dto, AiLowcodeDomain domain, Long tenantId, boolean create) {
        Long parentId = normalizeParentId(dto.getParentId());
        String domainCode = StringUtils.trimToNull(dto.getDomainCode());
        String domainName = StringUtils.trimToNull(dto.getDomainName());
        if (StringUtils.isBlank(domainCode) || !DOMAIN_CODE_PATTERN.matcher(domainCode).matches()) {
            throw new BusinessException("领域编码格式不正确（小写字母开头，仅含小写字母+数字+下划线，2-48字符）");
        }
        if (StringUtils.isBlank(domainName)) {
            throw new BusinessException("领域名称不能为空");
        }
        if (!create && domain.getId().equals(parentId)) {
            throw new BusinessException("父级领域不能选择自身");
        }
        if (parentId > ROOT_PARENT_ID && baseMapper.selectDomainById(tenantId, parentId) == null) {
            throw new BusinessException("父级业务领域不存在");
        }
        if (!create && parentId > ROOT_PARENT_ID && isDescendantDomain(tenantId, parentId, domain.getId())) {
            throw new BusinessException("父级领域不能选择自身或下级领域");
        }
        Long excludeId = create ? null : domain.getId();
        if (baseMapper.countByCode(tenantId, domainCode, excludeId) > 0) {
            throw new BusinessException("领域编码已存在: " + domainCode);
        }
        if (baseMapper.countByNameInParent(tenantId, parentId, domainName, excludeId) > 0) {
            throw new BusinessException("同一父级下领域名称已存在: " + domainName);
        }

        String status = StringUtils.defaultIfBlank(dto.getStatus(), STATUS_ENABLED);
        validateStatus(status);
        String tablePrefix = normalizePrefix(StringUtils.defaultIfBlank(dto.getTablePrefix(), "biz_" + domainCode + "_"), "默认表名前缀");
        String configKeyPrefix = normalizePrefix(StringUtils.defaultIfBlank(dto.getConfigKeyPrefix(), domainCode + "_"), "默认配置键前缀");

        LowcodeDomainSchema domainSchema = normalizeSchema(dto.getDomainSchema(), tablePrefix, configKeyPrefix, dto);
        domain.setTenantId(tenantId);
        domain.setParentId(parentId);
        domain.setDomainCode(domainCode);
        domain.setDomainName(domainName);
        domain.setDomainDesc(StringUtils.trimToNull(dto.getDomainDesc()));
        domain.setIcon(StringUtils.trimToNull(dto.getIcon()));
        domain.setSort(dto.getSort() == null ? 0 : dto.getSort());
        domain.setStatus(status);
        domain.setMenuParentId(dto.getMenuParentId());
        domain.setTablePrefix(tablePrefix);
        domain.setConfigKeyPrefix(configKeyPrefix);
        domain.setDefaultAppType(DEFAULT_APP_TYPE);
        domain.setDefaultLayoutType(DEFAULT_LAYOUT_TYPE);
        domain.setDefaultTableMode(StringUtils.defaultIfBlank(dto.getDefaultTableMode(), "CREATE"));
        domain.setDomainSchema(writeSchema(domainSchema));
    }

    private LowcodeDomainSchema normalizeSchema(LowcodeDomainSchema schema, String tablePrefix,
                                                String configKeyPrefix, LowcodeDomainDTO dto) {
        LowcodeDomainSchema result = schema == null ? new LowcodeDomainSchema() : schema;
        if (result.getAiContext() == null) {
            result.setAiContext(new LowcodeDomainSchema.AiContext());
        }
        if (result.getNaming() == null) {
            result.setNaming(new LowcodeDomainSchema.Naming());
        }
        if (result.getDefaults() == null) {
            result.setDefaults(new LowcodeDomainSchema.Defaults());
        }
        if (result.getCodegen() == null) {
            result.setCodegen(new LowcodeDomainSchema.Codegen());
        }
        if (result.getFieldTemplates() == null) {
            result.setFieldTemplates(new ArrayList<>());
        }
        if (result.getDictRecommendations() == null) {
            result.setDictRecommendations(new ArrayList<>());
        }
        if (result.getSecurityPolicies() == null) {
            result.setSecurityPolicies(new ArrayList<>());
        }
        validateFieldTemplates(result);
        result.getNaming().setTablePrefix(tablePrefix);
        result.getNaming().setConfigKeyPrefix(configKeyPrefix);
        if (StringUtils.isBlank(result.getNaming().getObjectCodeStyle())) {
            result.getNaming().setObjectCodeStyle("lower_snake");
        }
        result.getDefaults().setAppType(null);
        result.getDefaults().setLayoutType(null);
        result.getDefaults().setTableMode(StringUtils.defaultIfBlank(dto.getDefaultTableMode(), "CREATE"));
        result.getDefaults().setMenuParentId(dto.getMenuParentId());
        return result;
    }

    private void validateFieldTemplates(LowcodeDomainSchema schema) {
        Set<String> fields = new HashSet<>();
        for (LowcodeFieldSchema field : schema.getFieldTemplates()) {
            if (field == null) {
                continue;
            }
            String fieldName = StringUtils.trimToEmpty(field.getField());
            String columnName = StringUtils.trimToEmpty(field.getColumnName());
            if (AUDIT_FIELDS.contains(fieldName) || AUDIT_COLUMNS.contains(columnName)) {
                throw new BusinessException("领域通用字段不能覆盖审计字段: " + fieldName);
            }
            if (StringUtils.isNotBlank(fieldName) && !fields.add(fieldName)) {
                throw new BusinessException("领域通用字段重复: " + fieldName);
            }
        }
    }

    private String normalizePrefix(String prefix, String label) {
        String value = StringUtils.trimToEmpty(prefix);
        if (StringUtils.isBlank(value) || !PREFIX_PATTERN.matcher(value).matches()) {
            throw new BusinessException(label + "格式不正确（小写字母开头，仅含小写字母+数字+下划线）");
        }
        return value.endsWith("_") ? value : value + "_";
    }

    private void validateStatus(String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException("领域状态不正确");
        }
    }

    private LowcodeDomainVO toVO(AiLowcodeDomain domain) {
        LowcodeDomainVO vo = new LowcodeDomainVO();
        vo.setId(domain.getId());
        vo.setParentId(domain.getParentId());
        vo.setDomainCode(domain.getDomainCode());
        vo.setDomainName(domain.getDomainName());
        vo.setDomainDesc(domain.getDomainDesc());
        vo.setIcon(domain.getIcon());
        vo.setSort(domain.getSort());
        vo.setStatus(domain.getStatus());
        vo.setMenuParentId(domain.getMenuParentId());
        vo.setTablePrefix(domain.getTablePrefix());
        vo.setConfigKeyPrefix(domain.getConfigKeyPrefix());
        vo.setDefaultAppType(domain.getDefaultAppType());
        vo.setDefaultLayoutType(domain.getDefaultLayoutType());
        vo.setDefaultTableMode(domain.getDefaultTableMode());
        vo.setDomainSchema(readSchema(domain.getDomainSchema()));
        vo.setCreateTime(domain.getCreateTime());
        vo.setUpdateTime(domain.getUpdateTime());
        Long tenantId = domain.getTenantId() == null ? resolveTenantId() : domain.getTenantId();
        LowcodeDomainWorkspaceVO summary = baseMapper.selectWorkspaceSummary(tenantId, domain.getId());
        if (summary != null) {
            vo.setAppCount(summary.getAppCount());
            vo.setPublishedCount(summary.getPublishedCount());
        }
        return vo;
    }

    private LowcodeDomainTreeVO toTreeVO(AiLowcodeDomain domain) {
        LowcodeDomainTreeVO vo = new LowcodeDomainTreeVO();
        vo.setId(domain.getId());
        vo.setParentId(domain.getParentId());
        vo.setDomainCode(domain.getDomainCode());
        vo.setDomainName(domain.getDomainName());
        vo.setDomainDesc(domain.getDomainDesc());
        vo.setIcon(domain.getIcon());
        vo.setSort(domain.getSort());
        vo.setStatus(domain.getStatus());
        return vo;
    }

    private LowcodeDomainSchema readSchema(String json) {
        if (StringUtils.isBlank(json)) {
            return new LowcodeDomainSchema();
        }
        try {
            return objectMapper.readValue(json, LowcodeDomainSchema.class);
        } catch (Exception e) {
            throw new BusinessException("领域协议格式不正确");
        }
    }

    private String writeSchema(LowcodeDomainSchema schema) {
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new BusinessException("领域协议序列化失败");
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    public List<Long> collectDescendantIds(Long id) {
        AiLowcodeDomain domain = requireDomain(id);
        Long tenantId = resolveTenantId();
        List<AiLowcodeDomain> domains = baseMapper.selectDomainList(tenantId, null, null);
        Map<Long, List<Long>> childrenMap = new LinkedHashMap<>();
        for (AiLowcodeDomain node : domains) {
            Long parentId = normalizeParentId(node.getParentId());
            childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node.getId());
        }
        List<Long> collected = new ArrayList<>();
        ArrayDeque<Long> stack = new ArrayDeque<>();
        stack.push(domain.getId());
        Set<Long> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            Long currentId = stack.pop();
            if (currentId == null || !visited.add(currentId)) {
                continue;
            }
            collected.add(currentId);
            List<Long> children = childrenMap.get(currentId);
            if (children == null || children.isEmpty()) {
                continue;
            }
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
        return collected;
    }

    private boolean isDescendantDomain(Long tenantId, Long candidateParentId, Long domainId) {
        Long currentId = candidateParentId;
        Set<Long> visited = new HashSet<>();
        while (currentId != null && currentId > ROOT_PARENT_ID) {
            if (!visited.add(currentId)) {
                return true;
            }
            if (currentId.equals(domainId)) {
                return true;
            }
            AiLowcodeDomain currentDomain = baseMapper.selectDomainById(tenantId, currentId);
            if (currentDomain == null) {
                return false;
            }
            currentId = normalizeParentId(currentDomain.getParentId());
        }
        return false;
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
