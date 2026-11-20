package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageItemDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageQuery;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.entity.SysTenantPackage;
import com.mdframe.forge.plugin.system.entity.SysTenantPackageGrant;
import com.mdframe.forge.plugin.system.entity.SysTenantPackageItem;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantPackageGrantMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantPackageItemMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantPackageMapper;
import com.mdframe.forge.plugin.system.service.ISysTenantPackageService;
import com.mdframe.forge.plugin.system.service.TenantBootstrapPackages;
import com.mdframe.forge.plugin.system.service.TenantPackageResolution;
import com.mdframe.forge.plugin.system.vo.SysTenantPackageVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysTenantPackageServiceImpl implements ISysTenantPackageService {

    public static final String ITEM_BOOTSTRAP = "BOOTSTRAP";
    public static final String ITEM_RESOURCE = "RESOURCE";
    public static final String ITEM_QUOTA = "QUOTA";
    public static final String QUOTA_USER_LIMIT = "userLimit";
    public static final String RESOURCE_IDS = "resourceIds";

    private final SysTenantPackageMapper packageMapper;
    private final SysTenantPackageItemMapper itemMapper;
    private final SysTenantPackageGrantMapper grantMapper;
    private final SysTenantMapper tenantMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<SysTenantPackage> selectPage(SysTenantPackageQuery query) {
        assertSuperAdmin();
        return TenantContextHolder.executeIgnore(() -> {
            LambdaQueryWrapper<SysTenantPackage> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StringUtils.isNotBlank(query.getPackageName()), SysTenantPackage::getPackageName, query.getPackageName())
                    .like(StringUtils.isNotBlank(query.getPackageCode()), SysTenantPackage::getPackageCode, query.getPackageCode())
                    .eq(query.getPackageStatus() != null, SysTenantPackage::getPackageStatus, query.getPackageStatus())
                    .orderByAsc(SysTenantPackage::getSort)
                    .orderByDesc(SysTenantPackage::getCreateTime);
            return packageMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        });
    }

    @Override
    public SysTenantPackageVO detail(Long id) {
        assertSuperAdmin();
        return TenantContextHolder.executeIgnore(() -> {
            SysTenantPackage pkg = requirePackage(id);
            SysTenantPackageVO vo = new SysTenantPackageVO();
            BeanUtil.copyProperties(pkg, vo);
            vo.setItems(listItems(id));
            return vo;
        });
    }

    @Override
    public List<SysTenantPackage> options() {
        return TenantContextHolder.executeIgnore(() -> packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getPackageStatus, 1)
                .orderByAsc(SysTenantPackage::getSort)
                .orderByAsc(SysTenantPackage::getId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertPackage(SysTenantPackageDTO dto) {
        assertSuperAdmin();
        return Boolean.TRUE.equals(TenantContextHolder.executeIgnore(() -> {
            assertCodeUnique(dto.getPackageCode(), null);
            SysTenantPackage pkg = new SysTenantPackage();
            BeanUtil.copyProperties(dto, pkg, "items");
            if (pkg.getPackageStatus() == null) {
                pkg.setPackageStatus(1);
            }
            if (pkg.getIsDefault() == null) {
                pkg.setIsDefault(0);
            }
            if (pkg.getSort() == null) {
                pkg.setSort(100);
            }
            pkg.setDelFlag(0L);
            if (Objects.equals(pkg.getIsDefault(), 1)) {
                clearDefaultFlags();
            }
            if (packageMapper.insert(pkg) <= 0) {
                return false;
            }
            replaceItems(pkg.getId(), dto.getItems());
            return true;
        }));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePackage(SysTenantPackageDTO dto) {
        assertSuperAdmin();
        if (dto.getId() == null) {
            throw new BusinessException("套餐ID不能为空");
        }
        return Boolean.TRUE.equals(TenantContextHolder.executeIgnore(() -> {
            requirePackage(dto.getId());
            assertCodeUnique(dto.getPackageCode(), dto.getId());
            SysTenantPackage pkg = new SysTenantPackage();
            BeanUtil.copyProperties(dto, pkg, "items");
            if (Objects.equals(pkg.getIsDefault(), 1)) {
                clearDefaultFlags();
            }
            // 主表字段未变时部分库可能返回 0，组成项仍需落库
            packageMapper.updateById(pkg);
            replaceItems(dto.getId(), dto.getItems());
            return true;
        }));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        assertSuperAdmin();
        return Boolean.TRUE.equals(TenantContextHolder.executeIgnore(() -> {
            SysTenantPackage pkg = requirePackage(id);
            if (Objects.equals(pkg.getIsDefault(), 1)) {
                throw new BusinessException("默认套餐不能删除");
            }
            softDeleteItems(id);
            return packageMapper.deleteById(id) > 0;
        }));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(Long[] ids) {
        assertSuperAdmin();
        if (ids == null || ids.length == 0) {
            return false;
        }
        for (Long id : ids) {
            deleteById(id);
        }
        return true;
    }

    @Override
    public TenantPackageResolution resolve(Long packageId) {
        return TenantContextHolder.executeIgnore(() -> {
            SysTenantPackage pkg = packageId == null ? findDefaultPackage() : requireEnabledPackage(packageId);
            if (pkg == null) {
                return fallbackResolution();
            }
            return toResolution(pkg, listItems(pkg.getId()));
        });
    }

    @Override
    public TenantPackageResolution resolveDefault() {
        return resolve(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordGrant(Long tenantId, TenantPackageResolution resolution, List<String> appliedBootstrap) {
        if (tenantId == null || resolution == null || resolution.getPackageId() == null) {
            return;
        }
        TenantContextHolder.executeIgnore(() -> {
            try {
                Map<String, Object> snapshot = new LinkedHashMap<>();
                snapshot.put("packageId", resolution.getPackageId());
                snapshot.put("packageCode", resolution.getPackageCode());
                snapshot.put("packageName", resolution.getPackageName());
                snapshot.put("bootstrapPackages", appliedBootstrap != null ? appliedBootstrap : resolution.getBootstrapPackages());
                snapshot.put("resourceIds", resolution.getResourceIds());
                snapshot.put("userLimit", resolution.getUserLimit());
                SysTenantPackageGrant grant = new SysTenantPackageGrant();
                grant.setTenantId(tenantId);
                grant.setPackageId(resolution.getPackageId());
                grant.setPackageCode(resolution.getPackageCode());
                grant.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
                grant.setGrantStatus("ACTIVE");
                grant.setGrantedAt(LocalDateTime.now());
                grant.setDelFlag(0L);
                grantMapper.insert(grant);
            } catch (Exception ex) {
                throw new BusinessException("写入套餐开通快照失败: " + ex.getMessage());
            }
            return null;
        });
    }

    @Override
    public Set<Long> resolveMenuResourceWhitelist(Long tenantId) {
        // 默认租户不做套餐菜单裁剪
        if (tenantId == null || Objects.equals(tenantId, 1L)) {
            return Set.of();
        }
        return TenantContextHolder.executeIgnore(() -> {
            Set<Long> fromGrant = readResourceIdsFromLatestGrant(tenantId);
            if (!fromGrant.isEmpty()) {
                return fromGrant;
            }
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || tenant.getPackageId() == null) {
                return Set.of();
            }
            SysTenantPackage pkg = packageMapper.selectById(tenant.getPackageId());
            if (pkg == null) {
                return Set.of();
            }
            TenantPackageResolution resolution = toResolution(pkg, listItems(pkg.getId()));
            return resolution.getResourceIds() == null ? Set.of() : Set.copyOf(resolution.getResourceIds());
        });
    }

    private Set<Long> readResourceIdsFromLatestGrant(Long tenantId) {
        IPage<SysTenantPackageGrant> page = grantMapper.selectPage(
                new Page<>(1, 1),
                new LambdaQueryWrapper<SysTenantPackageGrant>()
                        .eq(SysTenantPackageGrant::getTenantId, tenantId)
                        .eq(SysTenantPackageGrant::getGrantStatus, "ACTIVE")
                        .orderByDesc(SysTenantPackageGrant::getGrantedAt)
                        .orderByDesc(SysTenantPackageGrant::getId));
        List<SysTenantPackageGrant> grants = page.getRecords();
        if (grants == null || grants.isEmpty() || StringUtils.isBlank(grants.get(0).getSnapshotJson())) {
            return Set.of();
        }
        try {
            Map<String, Object> snapshot = objectMapper.readValue(
                    grants.get(0).getSnapshotJson(), new TypeReference<Map<String, Object>>() {
                    });
            Object raw = snapshot.get("resourceIds");
            if (raw == null) {
                return Set.of();
            }
            if (raw instanceof String text) {
                return new LinkedHashSet<>(parseLongList(text));
            }
            String asJson = objectMapper.writeValueAsString(raw);
            return new LinkedHashSet<>(parseLongList(asJson));
        } catch (Exception ex) {
            log.warn("解析租户 {} 开通快照 resourceIds 失败", tenantId);
            return Set.of();
        }
    }

    private TenantPackageResolution toResolution(SysTenantPackage pkg, List<SysTenantPackageItem> items) {
        List<String> bootstrap = new ArrayList<>();
        Set<Long> resourceIds = new LinkedHashSet<>();
        Integer userLimit = null;
        for (SysTenantPackageItem item : items) {
            if (ITEM_BOOTSTRAP.equalsIgnoreCase(item.getItemType())) {
                if (isTruthy(item.getItemValue()) && TenantBootstrapPackages.RECOMMENDED.contains(item.getItemCode())) {
                    bootstrap.add(item.getItemCode());
                }
            } else if (ITEM_QUOTA.equalsIgnoreCase(item.getItemType())
                    && QUOTA_USER_LIMIT.equals(item.getItemCode())
                    && StringUtils.isNotBlank(item.getItemValue())) {
                try {
                    userLimit = Integer.parseInt(item.getItemValue().trim());
                } catch (NumberFormatException ignored) {
                    log.warn("套餐 {} 配额 userLimit 非法: {}", pkg.getPackageCode(), item.getItemValue());
                }
            } else if (ITEM_RESOURCE.equalsIgnoreCase(item.getItemType())
                    && RESOURCE_IDS.equals(item.getItemCode())
                    && StringUtils.isNotBlank(item.getItemValue())) {
                resourceIds.addAll(parseLongList(item.getItemValue()));
            }
        }
        return TenantPackageResolution.builder()
                .packageId(pkg.getId())
                .packageCode(pkg.getPackageCode())
                .packageName(pkg.getPackageName())
                .bootstrapPackages(bootstrap)
                .resourceIds(resourceIds)
                .userLimit(userLimit)
                .build();
    }

    private TenantPackageResolution fallbackResolution() {
        return TenantPackageResolution.builder()
                .packageId(null)
                .packageCode(null)
                .packageName(null)
                .bootstrapPackages(new ArrayList<>(TenantBootstrapPackages.RECOMMENDED))
                .resourceIds(Set.of())
                .userLimit(null)
                .build();
    }

    private List<Long> parseLongList(String raw) {
        try {
            String trimmed = raw.trim();
            if (trimmed.startsWith("[")) {
                return objectMapper.readValue(trimmed, new TypeReference<List<Long>>() {
                });
            }
            List<Long> out = new ArrayList<>();
            for (String part : trimmed.split(",")) {
                if (StringUtils.isNotBlank(part)) {
                    out.add(Long.parseLong(part.trim()));
                }
            }
            return out;
        } catch (Exception ex) {
            log.warn("解析 RESOURCE.resourceIds 失败: {}", raw);
            return List.of();
        }
    }

    private static boolean isTruthy(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String v = value.trim().toLowerCase();
        return !(v.equals("0") || v.equals("false") || v.equals("no") || v.equals("off"));
    }

    private void replaceItems(Long packageId, List<SysTenantPackageItemDTO> items) {
        softDeleteItems(packageId);
        if (items == null) {
            return;
        }
        int order = 0;
        for (SysTenantPackageItemDTO dto : items) {
            if (dto == null || StringUtils.isBlank(dto.getItemType()) || StringUtils.isBlank(dto.getItemCode())) {
                continue;
            }
            SysTenantPackageItem item = new SysTenantPackageItem();
            item.setPackageId(packageId);
            item.setItemType(dto.getItemType().trim());
            item.setItemCode(dto.getItemCode().trim());
            item.setItemValue(dto.getItemValue());
            item.setSort(dto.getSort() != null ? dto.getSort() : order++);
            item.setDelFlag(0L);
            itemMapper.insert(item);
        }
    }

    private void softDeleteItems(Long packageId) {
        List<SysTenantPackageItem> existing = listItems(packageId);
        for (SysTenantPackageItem item : existing) {
            itemMapper.deleteById(item.getId());
        }
    }

    private List<SysTenantPackageItem> listItems(Long packageId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SysTenantPackageItem>()
                .eq(SysTenantPackageItem::getPackageId, packageId)
                .orderByAsc(SysTenantPackageItem::getSort)
                .orderByAsc(SysTenantPackageItem::getId));
    }

    private void clearDefaultFlags() {
        List<SysTenantPackage> defaults = packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getIsDefault, 1));
        for (SysTenantPackage pkg : defaults) {
            SysTenantPackage patch = new SysTenantPackage();
            patch.setId(pkg.getId());
            patch.setIsDefault(0);
            packageMapper.updateById(patch);
        }
    }

    private void assertCodeUnique(String code, Long excludeId) {
        Long count = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getPackageCode, code)
                .ne(excludeId != null, SysTenantPackage::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("套餐编码已存在");
        }
    }

    private SysTenantPackage requirePackage(Long id) {
        SysTenantPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException("套餐不存在");
        }
        return pkg;
    }

    private SysTenantPackage requireEnabledPackage(Long id) {
        SysTenantPackage pkg = requirePackage(id);
        if (!Objects.equals(pkg.getPackageStatus(), 1)) {
            throw new BusinessException("套餐已禁用");
        }
        return pkg;
    }

    private SysTenantPackage findDefaultPackage() {
        List<SysTenantPackage> defaults = packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getIsDefault, 1)
                .eq(SysTenantPackage::getPackageStatus, 1)
                .orderByAsc(SysTenantPackage::getSort)
                .orderByAsc(SysTenantPackage::getId));
        if (!defaults.isEmpty()) {
            return defaults.get(0);
        }
        List<SysTenantPackage> enabled = packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getPackageStatus, 1)
                .orderByAsc(SysTenantPackage::getSort)
                .orderByAsc(SysTenantPackage::getId));
        return enabled.isEmpty() ? null : enabled.get(0);
    }

    private void assertSuperAdmin() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new BusinessException("只有超级管理员可以管理租户套餐");
        }
    }
}
