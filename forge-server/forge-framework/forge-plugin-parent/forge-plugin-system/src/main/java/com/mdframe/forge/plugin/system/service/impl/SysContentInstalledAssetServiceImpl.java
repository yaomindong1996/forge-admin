package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mdframe.forge.plugin.system.dto.ContentInstalledAssetInstallDTO;
import com.mdframe.forge.plugin.system.entity.SysContentInstalledAsset;
import com.mdframe.forge.plugin.system.enums.ContentAssetKind;
import com.mdframe.forge.plugin.system.mapper.SysContentInstalledAssetMapper;
import com.mdframe.forge.plugin.system.service.ISysContentInstalledAssetService;
import com.mdframe.forge.plugin.system.vo.ContentInstalledAssetVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SysContentInstalledAssetServiceImpl implements ISysContentInstalledAssetService {

    private static final Pattern KEY_SAFE = Pattern.compile("[^a-z0-9._-]+");
    private static final int MAX_PACKAGE_CHARS = 2_000_000;

    private final SysContentInstalledAssetMapper assets;
    private final ObjectMapper objectMapper;

    @Override
    public Page<ContentInstalledAssetVO> page(String kind, String keyword, int pageNum, int pageSize) {
        ContentAssetKind assetKind = ContentAssetKind.require(kind);
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        Long tenantId = requireTenantId();
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        long total = assets.countByKind(tenantId, assetKind.getCode(), kw);
        long offset = (long) (safePage - 1) * safeSize;
        List<SysContentInstalledAsset> rows = total == 0
                ? List.of()
                : assets.selectPageByKind(tenantId, assetKind.getCode(), kw, offset, safeSize);
        Page<ContentInstalledAssetVO> page = new Page<>(safePage, safeSize, total);
        page.setRecords(rows.stream().map(ContentInstalledAssetVO::summary).toList());
        return page;
    }

    @Override
    public ContentInstalledAssetVO detail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("资产标识无效");
        }
        SysContentInstalledAsset row = assets.selectActiveById(requireTenantId(), id);
        if (row == null) {
            throw new BusinessException("内容资产不存在或已卸载");
        }
        return ContentInstalledAssetVO.detail(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentInstalledAssetVO install(ContentInstalledAssetInstallDTO dto) {
        ContentAssetKind kind = ContentAssetKind.require(dto.getKind());
        NormalizedPackage pack = normalizePackage(kind, dto);
        Long tenantId = requireTenantId();
        SysContentInstalledAsset existing = assets.selectByKindAndKey(tenantId, kind.getCode(), pack.assetKey());
        if (existing == null) {
            SysContentInstalledAsset row = new SysContentInstalledAsset();
            row.setTenantId(tenantId);
            row.setKind(kind.getCode());
            row.setAssetKey(pack.assetKey());
            row.setAssetName(pack.assetName());
            row.setAssetVersion(pack.assetVersion());
            row.setPackageJson(pack.packageJson());
            row.setSourceFileName(trimToNull(dto.getSourceFileName()));
            row.setRemark(trimToNull(dto.getRemark()));
            row.setDelFlag(0L);
            assets.insert(row);
            return ContentInstalledAssetVO.detail(row);
        }
        existing.setAssetName(pack.assetName());
        existing.setAssetVersion(pack.assetVersion());
        existing.setPackageJson(pack.packageJson());
        if (StringUtils.hasText(dto.getSourceFileName())) {
            existing.setSourceFileName(dto.getSourceFileName().trim());
        }
        if (dto.getRemark() != null) {
            existing.setRemark(trimToNull(dto.getRemark()));
        }
        assets.updateById(existing);
        return ContentInstalledAssetVO.detail(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("资产标识无效");
        }
        SysContentInstalledAsset row = assets.selectActiveById(requireTenantId(), id);
        if (row == null) {
            throw new BusinessException("内容资产不存在或已卸载");
        }
        assets.deleteById(id);
    }

    private NormalizedPackage normalizePackage(ContentAssetKind kind, ContentInstalledAssetInstallDTO dto) {
        String raw = dto.getPackageJson() == null ? "" : dto.getPackageJson().trim();
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("模板包内容不能为空");
        }
        if (raw.length() > MAX_PACKAGE_CHARS) {
            throw new BusinessException("模板包过大，请拆分后安装");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new BusinessException("模板包不是有效 JSON");
        }
        if (root == null || !root.isObject()) {
            throw new BusinessException("模板包格式无效");
        }
        ObjectNode packageNode;
        JsonNode payloadNode;
        if (root.has("payload") && root.get("payload").isObject()) {
            packageNode = (ObjectNode) root;
            payloadNode = root.get("payload");
        } else {
            packageNode = objectMapper.createObjectNode();
            packageNode.put("kind", kind.getCode());
            packageNode.set("payload", root);
            payloadNode = root;
        }
        if (payloadNode == null || !payloadNode.isObject() || payloadNode.isEmpty()) {
            throw new BusinessException("模板包缺少可用 payload");
        }
        String declaredKind = text(packageNode, "kind");
        if (StringUtils.hasText(declaredKind) && !kind.matches(declaredKind)) {
            throw new BusinessException("模板包类型与当前安装类型不一致");
        }
        packageNode.put("kind", kind.getCode());

        String name = firstNonBlank(dto.getAssetName(), text(packageNode, "name"), "未命名模板");
        packageNode.put("name", name);

        String version = firstNonBlank(dto.getAssetVersion(), text(packageNode, "version"), null);
        if (StringUtils.hasText(version)) {
            packageNode.put("version", version);
        }

        String key = firstNonBlank(
                dto.getAssetKey(),
                text(packageNode, "key"),
                text(packageNode, "id"),
                deriveKey(kind, name, payloadNode)
        );
        key = sanitizeKey(key);
        if (!StringUtils.hasText(key)) {
            throw new BusinessException("无法生成资产键");
        }
        packageNode.put("key", key);

        String canonical;
        try {
            canonical = objectMapper.writeValueAsString(packageNode);
        } catch (Exception ex) {
            throw new BusinessException("无法序列化模板包");
        }
        return new NormalizedPackage(key, name.trim(), trimToNull(version), canonical);
    }

    private String deriveKey(ContentAssetKind kind, String name, JsonNode payload) {
        String slug = sanitizeKey(name);
        String fingerprint = sha256Short(payload.toString());
        String base = StringUtils.hasText(slug) ? slug : kind.getCode();
        return base + "-" + fingerprint;
    }

    private static String sanitizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = KEY_SAFE.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("-{2,}", "-");
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        return normalized.replaceAll("^-+|-+$", "");
    }

    private static String sha256Short(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            tenantId = SessionHelper.getTenantId();
        }
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("无法识别当前租户");
        }
        return tenantId;
    }

    private record NormalizedPackage(String assetKey, String assetName, String assetVersion, String packageJson) {
    }
}
