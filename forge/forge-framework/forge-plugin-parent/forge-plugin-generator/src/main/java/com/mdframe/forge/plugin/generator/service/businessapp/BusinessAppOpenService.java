package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppOpenInfoVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 业务应用入口打开方式解析服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessAppOpenService {

    private final BusinessAppMapper businessAppMapper;

    public BusinessAppOpenInfoVO openInfo(Long id) {
        AiBusinessApp app = businessAppMapper.selectEntityById(resolveTenantId(), id);
        if (app == null) {
            throw new BusinessException("应用入口不存在");
        }
        return buildOpenInfo(app);
    }

    public BusinessAppOpenInfoVO buildRuntimeOpenInfo(AiBusinessApp app) {
        return buildOpenInfo(app);
    }

    private BusinessAppOpenInfoVO buildOpenInfo(AiBusinessApp app) {
        BusinessAppOpenInfoVO vo = new BusinessAppOpenInfoVO();
        vo.setAppId(app.getId());
        vo.setAppCode(app.getAppCode());
        vo.setAppName(app.getAppName());
        vo.setAppType(app.getAppType());
        vo.setEntryMode(app.getEntryMode());
        vo.setConfigKey(app.getConfigKey());
        vo.setPermissionGranted(hasPermission("ai:businessApp:open"));
        vo.setOpenType(resolveOpenType(app.getEntryMode()));
        vo.setTargetUrl(resolveTargetUrl(app));

        boolean enabled = Integer.valueOf(1).equals(app.getStatus());
        boolean hasTarget = StringUtils.isNotBlank(vo.getTargetUrl());
        vo.setCanOpen(enabled && hasTarget && Boolean.TRUE.equals(vo.getPermissionGranted()));
        if (!enabled) {
            vo.setMessage("应用入口已停用");
        } else if (!hasTarget) {
            vo.setMessage("应用入口尚未配置打开地址");
        } else if (!Boolean.TRUE.equals(vo.getPermissionGranted())) {
            vo.setMessage("缺少应用入口打开权限");
        } else {
            vo.setMessage("可打开应用入口");
        }
        return vo;
    }

    private String resolveTargetUrl(AiBusinessApp app) {
        String entryMode = StringUtils.defaultIfBlank(app.getEntryMode(), "ROUTE").toUpperCase();
        if ("RUNTIME".equals(entryMode)) {
            if (StringUtils.isNotBlank(app.getConfigKey())) {
                return "/ai/crud-page/" + app.getConfigKey();
            }
            return StringUtils.trimToNull(app.getEntryUrl());
        }
        return StringUtils.trimToNull(app.getEntryUrl());
    }

    private String resolveOpenType(String entryMode) {
        String mode = StringUtils.defaultIfBlank(entryMode, "ROUTE").toUpperCase();
        return switch (mode) {
            case "RUNTIME" -> "ROUTE";
            case "IFRAME" -> "IFRAME";
            case "EXTERNAL" -> "EXTERNAL";
            case "H5" -> "H5";
            case "API" -> "API";
            default -> "ROUTE";
        };
    }

    private boolean hasPermission(String permission) {
        try {
            return SessionHelper.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
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
