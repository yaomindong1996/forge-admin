package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysFileStorageConfig;
import com.mdframe.forge.plugin.system.mapper.SysFileStorageConfigMapper;
import com.mdframe.forge.plugin.system.service.ISysFileStorageConfigService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 文件存储配置Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileStorageConfigServiceImpl extends ServiceImpl<SysFileStorageConfigMapper, SysFileStorageConfig>
        implements ISysFileStorageConfigService {
    
    private final StorageConfigProvider configProvider;
    private final FileManager fileManager;

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "jsp", "jspx", "php", "asp", "aspx", "html", "htm",
            "js", "mjs", "ts", "vue", "sh", "bash", "bat", "cmd",
            "ps1", "exe", "dll", "so", "dylib", "jar", "war", "ear",
            "sql", "md", "svg"
    );
    
    @Override
    public Page<SysFileStorageConfig> page(PageQuery query, SysFileStorageConfig condition) {
        LambdaQueryWrapper<SysFileStorageConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(condition.getConfigName())) {
            wrapper.like(SysFileStorageConfig::getConfigName, condition.getConfigName());
        }
        
        if (StrUtil.isNotBlank(condition.getStorageType())) {
            wrapper.eq(SysFileStorageConfig::getStorageType, condition.getStorageType());
        }
        
        if (condition.getEnabled() != null) {
            wrapper.eq(SysFileStorageConfig::getEnabled, condition.getEnabled());
        }
        wrapper.orderByDesc(SysFileStorageConfig::getIsDefault)
                .orderByAsc(SysFileStorageConfig::getOrderNum);
        Page<SysFileStorageConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.baseMapper.selectPage(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        SysFileStorageConfig config = this.getById(id);
        if (config == null) {
            throw new RuntimeException("文件存储配置不存在");
        }
        normalizeRequiredAllowedTypes(config);

        // 取消所有默认配置
        this.lambdaUpdate()
                .set(SysFileStorageConfig::getIsDefault, false)
                .update();
        
        // 设置新的默认配置
        this.lambdaUpdate()
                .eq(SysFileStorageConfig::getId, id)
                .set(SysFileStorageConfig::getAllowedTypes, config.getAllowedTypes())
                .set(SysFileStorageConfig::getIsDefault, true)
                .update();
        
        // 刷新配置缓存
        configProvider.refreshConfig();
        fileManager.refreshConfiguredStorages();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(Long id, Boolean enabled) {
        SysFileStorageConfig config = null;
        if (Boolean.TRUE.equals(enabled)) {
            config = this.getById(id);
            if (config == null) {
                throw new RuntimeException("文件存储配置不存在");
            }
            normalizeRequiredAllowedTypes(config);
        }

        var update = this.lambdaUpdate()
                .eq(SysFileStorageConfig::getId, id)
                .set(SysFileStorageConfig::getEnabled, enabled);
        if (config != null) {
            update.set(SysFileStorageConfig::getAllowedTypes, config.getAllowedTypes());
        }
        update.update();
        
        // 刷新配置缓存
        configProvider.refreshConfig();
        fileManager.refreshConfiguredStorages();
    }
    
    @Override
    public boolean testConnection(Long id) {
        SysFileStorageConfig config = this.getById(id);
        if (config == null) {
            return false;
        }
        
        try {
            FileStorage storage = fileManager.getStorage(config.getStorageType());
            if (storage == null) {
                log.warn("未找到存储实现: {}", config.getStorageType());
                return false;
            }
            storage.init(convertToStorageConfig(config));
            return storage.testConnection();
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return false;
        } finally {
            fileManager.refreshConfiguredStorages();
        }
    }

    @Override
    public boolean createBucket(Long id) {
        SysFileStorageConfig config = this.getById(id);
        if (config == null) {
            return false;
        }

        FileStorage storage = fileManager.getStorage(config.getStorageType());
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + config.getStorageType());
        }
        try {
            storage.init(convertToStorageConfig(config));
            return storage.createBucket(config.getBucketName());
        } finally {
            fileManager.refreshConfiguredStorages();
        }
    }

    @Override
    public SysFileStorageConfig getDefaultConfig() {
        return this.lambdaQuery()
                .eq(SysFileStorageConfig::getIsDefault, true)
                .eq(SysFileStorageConfig::getEnabled, true)
                .last("limit 1")
                .one();
    }

    @Override
    public List<SysFileStorageConfig> listEnabledOptions() {
        return this.lambdaQuery()
                .select(SysFileStorageConfig::getId,
                        SysFileStorageConfig::getConfigName,
                        SysFileStorageConfig::getStorageType,
                        SysFileStorageConfig::getIsDefault,
                        SysFileStorageConfig::getEnabled)
                .eq(SysFileStorageConfig::getEnabled, true)
                .orderByDesc(SysFileStorageConfig::getIsDefault)
                .orderByAsc(SysFileStorageConfig::getOrderNum)
                .list();
    }

    @Override
    public boolean save(SysFileStorageConfig entity) {
        normalizeRequiredAllowedTypes(entity);
        boolean success = super.save(entity);
        configProvider.refreshConfig();
        fileManager.refreshConfiguredStorages();
        return success;
    }

    @Override
    public boolean updateById(SysFileStorageConfig entity) {
        preserveMaskedCredentials(entity);
        normalizeRequiredAllowedTypes(entity);
        boolean success = super.updateById(entity);
        configProvider.refreshConfig();
        fileManager.refreshConfiguredStorages();
        return success;
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        boolean success = super.removeById(id);
        configProvider.refreshConfig();
        fileManager.refreshConfiguredStorages();
        return success;
    }

    private StorageConfig convertToStorageConfig(SysFileStorageConfig entity) {
        StorageConfig config = new StorageConfig();
        config.setId(entity.getId());
        config.setConfigName(entity.getConfigName());
        config.setStorageType(entity.getStorageType());
        config.setIsDefault(entity.getIsDefault());
        config.setEnabled(entity.getEnabled());
        config.setEndpoint(entity.getEndpoint());
        config.setAccessKey(entity.getAccessKey());
        config.setSecretKey(entity.getSecretKey());
        config.setBucketName(entity.getBucketName());
        config.setRegion(entity.getRegion());
        config.setBasePath(entity.getBasePath());
        config.setDomain(entity.getDomain());
        config.setUseHttps(entity.getUseHttps());
        config.setMaxFileSize(entity.getMaxFileSize());
        config.setAllowedTypes(entity.getAllowedTypes());
        config.setOrderNum(entity.getOrderNum());
        config.setExtraConfig(entity.getExtraConfig());
        return config;
    }

    private void preserveMaskedCredentials(SysFileStorageConfig entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        boolean accessKeyMasked = SensitiveDataUtil.isMaskedValue(entity.getAccessKey());
        boolean secretKeyMasked = SensitiveDataUtil.isMaskedValue(entity.getSecretKey());
        if (!accessKeyMasked && !secretKeyMasked) {
            return;
        }
        SysFileStorageConfig existing = this.getById(entity.getId());
        if (existing == null) {
            return;
        }
        if (accessKeyMasked) {
            entity.setAccessKey(existing.getAccessKey());
        }
        if (secretKeyMasked) {
            entity.setSecretKey(existing.getSecretKey());
        }
    }

    private void normalizeRequiredAllowedTypes(SysFileStorageConfig entity) {
        if (entity == null) {
            throw new RuntimeException("文件存储配置不能为空");
        }
        if (StrUtil.isBlank(entity.getAllowedTypes())) {
            throw new RuntimeException("支持的文件类型不能为空");
        }
        Set<String> normalizedTypes = new LinkedHashSet<>();
        for (String type : entity.getAllowedTypes().split(",")) {
            String normalizedType = normalizeExtension(type);
            if (StrUtil.isBlank(normalizedType)) {
                continue;
            }
            if (DANGEROUS_EXTENSIONS.contains(normalizedType)) {
                throw new RuntimeException("不允许配置高风险文件类型: " + normalizedType);
            }
            normalizedTypes.add(normalizedType);
        }
        if (normalizedTypes.isEmpty()) {
            throw new RuntimeException("支持的文件类型不能为空");
        }
        entity.setAllowedTypes(String.join(",", normalizedTypes));
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }
}
