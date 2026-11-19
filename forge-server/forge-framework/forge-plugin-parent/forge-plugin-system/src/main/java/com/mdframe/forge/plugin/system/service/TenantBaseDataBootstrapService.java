package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.listener.DictChangeEvent;
import com.mdframe.forge.plugin.system.mapper.SysConfigMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictTypeMapper;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.TenantBaseDataBootstrapMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 新租户基础数据引导：按管理员勾选的数据包从默认租户拷贝或生成骨架数据。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantBaseDataBootstrapService {

    public static final Long DEFAULT_TENANT_ID = 1L;
    public static final String TENANT_ADMIN_ROLE_KEY = "tenant_admin";

    private final TenantBaseDataBootstrapMapper bootstrapMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysConfigMapper configMapper;
    private final SysOrgMapper orgMapper;
    private final SysRoleMapper roleMapper;
    private final ApplicationEventPublisher eventPublisher;

    public record BootstrapResult(Map<String, Integer> packageCounts, Set<String> appliedPackages) {
        public static BootstrapResult empty() {
            return new BootstrapResult(Map.of(), Set.of());
        }

        /** 兼容旧字段：字典类型拷贝数。 */
        public int dictTypesCopied() {
            return packageCounts.getOrDefault("dictTypes", 0);
        }

        public int dictDataCopied() {
            return packageCounts.getOrDefault("dictData", 0);
        }

        public int configsCopied() {
            return packageCounts.getOrDefault("configs", 0);
        }

        public boolean dictSkipped() {
            return packageCounts.getOrDefault("dictSkipped", 0) > 0;
        }

        public boolean configSkipped() {
            return packageCounts.getOrDefault("configSkipped", 0) > 0;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public BootstrapResult ensureFromDefaultTenant(Long targetTenantId) {
        return ensureFromDefaultTenant(targetTenantId, null, null);
    }

    /**
     * @param packages   {@code null} 用推荐默认；空集表示不引导
     * @param tenantName 用于生成根组织名称
     */
    @Transactional(rollbackFor = Exception.class)
    public BootstrapResult ensureFromDefaultTenant(Long targetTenantId, List<String> packages, String tenantName) {
        if (targetTenantId == null) {
            throw new BusinessException("租户ID不能为空");
        }
        if (Objects.equals(targetTenantId, DEFAULT_TENANT_ID)) {
            return BootstrapResult.empty();
        }
        Set<String> selected = TenantBootstrapPackages.normalize(packages);
        if (selected.isEmpty()) {
            return BootstrapResult.empty();
        }
        String orgName = StringUtils.defaultIfBlank(tenantName, "默认组织");
        return TenantContextHolder.executeIgnore(() -> doBootstrap(targetTenantId, selected, orgName));
    }

    private BootstrapResult doBootstrap(Long targetTenantId, Set<String> selected, String orgName) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Set<String> applied = new HashSet<>();

        if (selected.contains(TenantBootstrapPackages.DICT_AND_CONFIG)) {
            applied.add(TenantBootstrapPackages.DICT_AND_CONFIG);
            copyDictAndConfig(targetTenantId, counts);
        }
        if (selected.contains(TenantBootstrapPackages.ROOT_ORG)) {
            applied.add(TenantBootstrapPackages.ROOT_ORG);
            counts.put("rootOrg", ensureRootOrg(targetTenantId, orgName));
        }
        if (selected.contains(TenantBootstrapPackages.TENANT_ADMIN_ROLE)) {
            applied.add(TenantBootstrapPackages.TENANT_ADMIN_ROLE);
            counts.put("tenantAdminRole", ensureTenantAdminRole(targetTenantId));
        }
        if (selected.contains(TenantBootstrapPackages.MESSAGE_TEMPLATES)) {
            applied.add(TenantBootstrapPackages.MESSAGE_TEMPLATES);
            int templates = bootstrapMapper.copyMessageTemplates(DEFAULT_TENANT_ID, targetTenantId);
            int bizTypes = bootstrapMapper.copyMessageBizTypes(DEFAULT_TENANT_ID, targetTenantId);
            counts.put("messageTemplates", templates);
            counts.put("messageBizTypes", bizTypes);
        }
        if (selected.contains(TenantBootstrapPackages.FLOW_ASSIST)) {
            applied.add(TenantBootstrapPackages.FLOW_ASSIST);
            int phrases = bootstrapMapper.copyFlowCommentPhrases(DEFAULT_TENANT_ID, targetTenantId);
            int spel = bootstrapMapper.copyFlowSpelTemplates(DEFAULT_TENANT_ID, targetTenantId);
            counts.put("flowCommentPhrases", phrases);
            counts.put("flowSpelTemplates", spel);
        }
        if (selected.contains(TenantBootstrapPackages.DATA_SCOPE_PRESETS)) {
            applied.add(TenantBootstrapPackages.DATA_SCOPE_PRESETS);
            counts.put("dataScopePresets", bootstrapMapper.copyDataScopeConfigs(DEFAULT_TENANT_ID, targetTenantId));
        }

        log.info("租户 {} 基础数据引导完成: packages={}, counts={}", targetTenantId, applied, counts);
        return new BootstrapResult(counts, applied);
    }

    private void copyDictAndConfig(Long targetTenantId, Map<String, Integer> counts) {
        boolean dictSkipped = bootstrapMapper.countActiveDictTypes(targetTenantId) > 0;
        boolean configSkipped = bootstrapMapper.countActiveConfigs(targetTenantId) > 0;
        counts.put("dictSkipped", dictSkipped ? 1 : 0);
        counts.put("configSkipped", configSkipped ? 1 : 0);

        int typesCopied = 0;
        int dataCopied = 0;
        int configsCopied = 0;
        Set<String> changedDictTypes = new HashSet<>();

        if (!dictSkipped && bootstrapMapper.countActiveDictTypes(DEFAULT_TENANT_ID) > 0) {
            List<SysDictType> sourceDictTypes = bootstrapMapper.selectActiveDictTypes(DEFAULT_TENANT_ID);
            for (SysDictType source : sourceDictTypes) {
                SysDictType copy = copyDictType(source, targetTenantId);
                if (dictTypeMapper.insert(copy) > 0) {
                    typesCopied++;
                    if (copy.getDictType() != null) {
                        changedDictTypes.add(copy.getDictType());
                    }
                }
            }
            List<SysDictData> sourceDictData = bootstrapMapper.selectActiveDictData(DEFAULT_TENANT_ID);
            Map<Long, Long> dictCodeMap = new HashMap<>();
            for (SysDictData source : sourceDictData) {
                Long oldCode = source.getDictCode();
                SysDictData copy = copyDictData(source, targetTenantId);
                if (dictDataMapper.insert(copy) > 0) {
                    dataCopied++;
                    dictCodeMap.put(oldCode, copy.getDictCode());
                    if (copy.getDictType() != null) {
                        changedDictTypes.add(copy.getDictType());
                    }
                }
            }
            for (SysDictData source : sourceDictData) {
                Long oldParent = source.getParentDictCode();
                if (oldParent == null) {
                    continue;
                }
                Long newCode = dictCodeMap.get(source.getDictCode());
                Long newParent = dictCodeMap.get(oldParent);
                if (newCode == null || newParent == null) {
                    continue;
                }
                SysDictData patch = new SysDictData();
                patch.setDictCode(newCode);
                patch.setParentDictCode(newParent);
                dictDataMapper.updateById(patch);
            }
        }

        if (!configSkipped) {
            for (SysConfig source : bootstrapMapper.selectActiveConfigs(DEFAULT_TENANT_ID)) {
                if (configMapper.insert(copyConfig(source, targetTenantId)) > 0) {
                    configsCopied++;
                }
            }
        }

        counts.put("dictTypes", typesCopied);
        counts.put("dictData", dataCopied);
        counts.put("configs", configsCopied);
        if (!changedDictTypes.isEmpty()) {
            eventPublisher.publishEvent(new DictChangeEvent(this, changedDictTypes));
        }
    }

    private int ensureRootOrg(Long targetTenantId, String orgName) {
        if (bootstrapMapper.countActiveOrgs(targetTenantId) > 0) {
            return 0;
        }
        SysOrg org = new SysOrg();
        org.setTenantId(targetTenantId);
        org.setOrgName(orgName);
        org.setParentId(0L);
        org.setAncestors("0");
        org.setSort(0);
        org.setOrgType("1");
        org.setOrgStatus(1);
        org.setDelFlag(0L);
        org.setRemark("租户创建时自动生成");
        return orgMapper.insert(org) > 0 ? 1 : 0;
    }

    private int ensureTenantAdminRole(Long targetTenantId) {
        if (bootstrapMapper.countRolesByKey(targetTenantId, TENANT_ADMIN_ROLE_KEY) > 0) {
            return 0;
        }
        Long sourceRoleId = bootstrapMapper.selectTemplateAdminRoleId(DEFAULT_TENANT_ID);
        if (sourceRoleId == null) {
            log.warn("默认租户无 admin 角色，跳过租户 {} 的管理员角色引导", targetTenantId);
            return 0;
        }
        SysRole role = new SysRole();
        role.setTenantId(targetTenantId);
        role.setRoleName("租户管理员");
        role.setRoleKey(TENANT_ADMIN_ROLE_KEY);
        role.setRoleType(1);
        role.setOrgScopeType(1);
        role.setDataScope(1);
        role.setSort(1);
        role.setRoleStatus(1);
        role.setIsSystem(1);
        role.setRemark("租户创建时自动生成（不含租户管理菜单）");
        role.setDelFlag(0L);
        if (roleMapper.insert(role) <= 0 || role.getId() == null) {
            return 0;
        }
        int resources = bootstrapMapper.copyRoleResourcesExcludingTenantMgmt(
                DEFAULT_TENANT_ID, sourceRoleId, targetTenantId, role.getId());
        return 1 + Math.max(resources, 0);
    }

    private static SysDictType copyDictType(SysDictType source, Long targetTenantId) {
        SysDictType copy = new SysDictType();
        copy.setTenantId(targetTenantId);
        copy.setDictName(source.getDictName());
        copy.setDictType(source.getDictType());
        copy.setDictStatus(source.getDictStatus());
        copy.setRemark(source.getRemark());
        copy.setDelFlag(0L);
        return copy;
    }

    private static SysDictData copyDictData(SysDictData source, Long targetTenantId) {
        SysDictData copy = new SysDictData();
        copy.setTenantId(targetTenantId);
        copy.setDictSort(source.getDictSort());
        copy.setDictLabel(source.getDictLabel());
        copy.setDictValue(source.getDictValue());
        copy.setDictType(source.getDictType());
        copy.setCssClass(source.getCssClass());
        copy.setListClass(source.getListClass());
        copy.setIsDefault(source.getIsDefault());
        copy.setParentDictCode(null);
        copy.setLinkedDictType(source.getLinkedDictType());
        copy.setLinkedDictValue(source.getLinkedDictValue());
        copy.setDictStatus(source.getDictStatus());
        copy.setRemark(source.getRemark());
        copy.setDelFlag(0L);
        return copy;
    }

    private static SysConfig copyConfig(SysConfig source, Long targetTenantId) {
        SysConfig copy = new SysConfig();
        copy.setTenantId(targetTenantId);
        copy.setConfigName(source.getConfigName());
        copy.setConfigKey(source.getConfigKey());
        copy.setConfigValue(source.getConfigValue());
        copy.setConfigType(source.getConfigType());
        copy.setConfigDesc(source.getConfigDesc());
        copy.setSort(source.getSort());
        copy.setDelFlag(0L);
        return copy;
    }
}
