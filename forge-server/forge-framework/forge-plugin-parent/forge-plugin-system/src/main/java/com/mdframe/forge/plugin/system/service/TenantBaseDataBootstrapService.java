package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.plugin.system.listener.DictChangeEvent;
import com.mdframe.forge.plugin.system.mapper.SysConfigMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictTypeMapper;
import com.mdframe.forge.plugin.system.mapper.TenantBaseDataBootstrapMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 新租户基础数据引导：从默认租户拷贝字典类型/字典项/系统配置。
 * <p>行政区划等平台表本身忽略租户行，无需拷贝。幂等：目标租户已有对应数据则跳过该类别。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantBaseDataBootstrapService {

    public static final Long DEFAULT_TENANT_ID = 1L;

    private final TenantBaseDataBootstrapMapper bootstrapMapper;
    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final SysConfigMapper configMapper;
    private final ApplicationEventPublisher eventPublisher;

    public record BootstrapResult(int dictTypesCopied, int dictDataCopied, int configsCopied,
                                  boolean dictSkipped, boolean configSkipped) {
        public static BootstrapResult skippedAll() {
            return new BootstrapResult(0, 0, 0, true, true);
        }
    }

    /**
     * 确保目标租户具备默认可运行的字典与配置；已有数据则不覆盖。
     */
    @Transactional(rollbackFor = Exception.class)
    public BootstrapResult ensureFromDefaultTenant(Long targetTenantId) {
        if (targetTenantId == null) {
            throw new BusinessException("租户ID不能为空");
        }
        if (Objects.equals(targetTenantId, DEFAULT_TENANT_ID)) {
            return BootstrapResult.skippedAll();
        }
        return TenantContextHolder.executeIgnore(() -> doBootstrap(targetTenantId));
    }

    private BootstrapResult doBootstrap(Long targetTenantId) {
        long sourceTypes = bootstrapMapper.countActiveDictTypes(DEFAULT_TENANT_ID);
        if (sourceTypes <= 0) {
            log.warn("默认租户无可用字典，跳过租户 {} 的字典拷贝", targetTenantId);
        }
        boolean dictSkipped = bootstrapMapper.countActiveDictTypes(targetTenantId) > 0;
        boolean configSkipped = bootstrapMapper.countActiveConfigs(targetTenantId) > 0;

        int typesCopied = 0;
        int dataCopied = 0;
        int configsCopied = 0;
        Set<String> changedDictTypes = new HashSet<>();

        if (!dictSkipped && sourceTypes > 0) {
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
                SysDictData copy = copyDictData(source, targetTenantId, null);
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
            List<SysConfig> sourceConfigs = bootstrapMapper.selectActiveConfigs(DEFAULT_TENANT_ID);
            for (SysConfig source : sourceConfigs) {
                SysConfig copy = copyConfig(source, targetTenantId);
                if (configMapper.insert(copy) > 0) {
                    configsCopied++;
                }
            }
        }

        if (!changedDictTypes.isEmpty()) {
            eventPublisher.publishEvent(new DictChangeEvent(this, changedDictTypes));
        }

        log.info("租户 {} 基础数据引导完成: dictTypes={}, dictData={}, configs={}, dictSkipped={}, configSkipped={}",
                targetTenantId, typesCopied, dataCopied, configsCopied, dictSkipped, configSkipped);
        return new BootstrapResult(typesCopied, dataCopied, configsCopied, dictSkipped, configSkipped);
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

    private static SysDictData copyDictData(SysDictData source, Long targetTenantId, Long parentDictCode) {
        SysDictData copy = new SysDictData();
        copy.setTenantId(targetTenantId);
        copy.setDictSort(source.getDictSort());
        copy.setDictLabel(source.getDictLabel());
        copy.setDictValue(source.getDictValue());
        copy.setDictType(source.getDictType());
        copy.setCssClass(source.getCssClass());
        copy.setListClass(source.getListClass());
        copy.setIsDefault(source.getIsDefault());
        copy.setParentDictCode(parentDictCode);
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
