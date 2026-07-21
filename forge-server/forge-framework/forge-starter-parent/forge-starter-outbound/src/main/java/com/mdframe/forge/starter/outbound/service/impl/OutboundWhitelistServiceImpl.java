package com.mdframe.forge.starter.outbound.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistQuery;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistSaveRequest;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.service.OutboundWhitelistService;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@RequiredArgsConstructor
public class OutboundWhitelistServiceImpl implements OutboundWhitelistService {

    public static final long PLATFORM_TENANT_ID = 1L;

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private final SysOutboundWhitelistMapper mapper;
    private final OutboundHostNormalizer hostNormalizer;

    @Override
    public Page<SysOutboundWhitelist> page(OutboundWhitelistQuery query) {
        OutboundWhitelistQuery safeQuery = query == null ? new OutboundWhitelistQuery() : query;
        normalizeQuery(safeQuery);
        Page<SysOutboundWhitelist> page = new Page<>(safeQuery.getPageNum(), safeQuery.getPageSize());
        return mapper.selectWhitelistPage(page, PLATFORM_TENANT_ID, safeQuery);
    }

    @Override
    public SysOutboundWhitelist getById(Long id) {
        if (id == null) {
            throw new BusinessException("出站白名单ID不能为空");
        }
        SysOutboundWhitelist entity = mapper.selectByIdForTenant(PLATFORM_TENANT_ID, id);
        if (entity == null) {
            throw new BusinessException("出站白名单不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysOutboundWhitelist create(OutboundWhitelistSaveRequest request) {
        SysOutboundWhitelist entity = toEntity(request, false);
        assertNoOverlap(entity, null);
        if (mapper.insert(entity) != 1) {
            throw new BusinessException("新增出站白名单失败");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysOutboundWhitelist update(OutboundWhitelistSaveRequest request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException("出站白名单ID不能为空");
        }
        getById(request.getId());
        SysOutboundWhitelist entity = toEntity(request, true);
        assertNoOverlap(entity, entity.getId());
        if (mapper.updateRule(entity) != 1) {
            throw new BusinessException("修改出站白名单失败");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        if (mapper.logicDelete(PLATFORM_TENANT_ID, id) != 1) {
            throw new BusinessException("删除出站白名单失败");
        }
    }

    private SysOutboundWhitelist toEntity(OutboundWhitelistSaveRequest request, boolean updating) {
        if (request == null) {
            throw new BusinessException("出站白名单参数不能为空");
        }
        String scene = normalizeScene(request.getScene());
        String protocol = normalizeProtocol(request.getProtocol());
        String host = hostNormalizer.normalize(request.getHost());
        validatePorts(request.getPortStart(), request.getPortEnd());
        int allowPrivate = normalizeFlag(request.getAllowPrivate(), "私网例外标志不合法");
        int status = normalizeFlag(request.getStatus(), "状态不合法");
        if (OutboundScenes.JOB_WEBHOOK.equals(scene) && allowPrivate == 1) {
            throw new BusinessException("JOB_WEBHOOK场景禁止私网例外");
        }
        if (request.getRemark() != null && request.getRemark().length() > 500) {
            throw new BusinessException("备注长度不能超过500个字符");
        }

        SysOutboundWhitelist entity = new SysOutboundWhitelist();
        entity.setId(updating ? request.getId() : null);
        entity.setTenantId(PLATFORM_TENANT_ID);
        entity.setScene(scene);
        entity.setProtocol(protocol);
        entity.setHost(host);
        entity.setPortStart(request.getPortStart());
        entity.setPortEnd(request.getPortEnd());
        entity.setAllowPrivate(allowPrivate);
        entity.setStatus(status);
        entity.setRemark(request.getRemark() == null ? null : request.getRemark().trim());
        entity.setDelFlag(0);
        return entity;
    }

    private void assertNoOverlap(SysOutboundWhitelist entity, Long excludeId) {
        Long count = mapper.countOverlappingRules(
                PLATFORM_TENANT_ID,
                entity.getScene(),
                entity.getProtocol(),
                entity.getHost(),
                entity.getPortStart(),
                entity.getPortEnd(),
                excludeId);
        if (count != null && count > 0) {
            throw new BusinessException("相同场景、协议和主机已存在重叠端口规则");
        }
    }

    private String normalizeScene(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("出站场景不能为空");
        }
        String scene = value.trim().toUpperCase(Locale.ROOT);
        if (!OutboundScenes.SUPPORTED.contains(scene)) {
            throw new BusinessException("不支持的出站场景");
        }
        return scene;
    }

    private String normalizeProtocol(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("协议不能为空");
        }
        String protocol = value.trim().toLowerCase(Locale.ROOT);
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            throw new BusinessException("仅支持HTTP或HTTPS协议");
        }
        return protocol;
    }

    private void validatePorts(Integer portStart, Integer portEnd) {
        if (portStart == null || portEnd == null || portStart < MIN_PORT || portEnd > MAX_PORT
                || portStart > portEnd) {
            throw new BusinessException("端口范围不合法");
        }
    }

    private int normalizeFlag(Integer value, String message) {
        if (value == null || value < 0 || value > 1) {
            throw new BusinessException(message);
        }
        return value;
    }

    private void normalizeQuery(OutboundWhitelistQuery query) {
        if (query.getScene() != null && !query.getScene().isBlank()) {
            query.setScene(normalizeScene(query.getScene()));
        } else {
            query.setScene(null);
        }
        if (query.getProtocol() != null && !query.getProtocol().isBlank()) {
            query.setProtocol(normalizeProtocol(query.getProtocol()));
        } else {
            query.setProtocol(null);
        }
        if (query.getHost() != null && !query.getHost().isBlank()) {
            query.setHost(hostNormalizer.normalize(query.getHost()));
        } else {
            query.setHost(null);
        }
        if (query.getStatus() != null) {
            query.setStatus(normalizeFlag(query.getStatus(), "状态不合法"));
        }
    }
}
