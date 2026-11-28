package com.mdframe.forge.business.core.purchase.export;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.business.core.purchase.dto.SamplePurchaseOrderQuery;
import com.mdframe.forge.business.core.purchase.mapper.SamplePurchaseOrderMapper;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.auth.domain.UserResourceTreeVO;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Loopback-only test routes. The sample purchase module has no production export endpoint. */
@RestController
class PurchaseExportFixtureController {

    static final String BASE_PATH = "/business/sample-purchase-order";
    static final String EXPORT_PERMISSION = "business:purchaseOrderTest:export";
    static final String EXPORT_API_PERMISSION = "business:purchaseOrderTest:exportApi";
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int PAGE_SIZE = 1;

    private final JdbcTemplate jdbc;
    private final SamplePurchaseOrderMapper purchases;
    private final SysResourceMapper resources;
    private final SysUserOrgRoleMapper orgRoles;
    private final ISysResourceService resourceService;

    PurchaseExportFixtureController(JdbcTemplate jdbc, SqlSessionTemplate session, ISysResourceService resourceService) {
        this.jdbc = jdbc;
        this.resourceService = resourceService;
        purchases = session.getMapper(SamplePurchaseOrderMapper.class);
        resources = session.getMapper(SysResourceMapper.class);
        orgRoles = session.getMapper(SysUserOrgRoleMapper.class);
    }

    @PostMapping("/auth/login")
    Map<String, Object> login(@RequestParam("userId") long userId) {
        LoginUser user = jdbc.queryForObject("SELECT tenant_id, org_id FROM sys_user_org WHERE user_id = ?",
                (row, index) -> {
                    LoginUser member = new LoginUser();
                    member.setUserId(userId);
                    member.setUserType(SystemConstants.UserType.NORMAL_USER);
                    member.setTenantId(row.getLong("tenant_id"));
                    member.setActiveOrgId(row.getLong("org_id"));
                    return member;
                }, userId);
        user.setRoleIds(orgRoles.selectActiveRoleIdsByUserOrg(user.getTenantId(), userId, user.getActiveOrgId()));
        StpUtil.login(userId);
        SessionHelper.setLoginUser(user);
        List<Long> grantedIds = resourceService.selectCurrentUserResourceIds();
        user.setPermissions(resources.selectByIds(grantedIds).stream().map(SysResource::getPerms).collect(Collectors.toSet()));
        user.setApiPermissions(resources.selectApiPermissionPatternsByResourceIds(grantedIds));
        SessionHelper.setLoginUser(user);
        return Map.of("token", StpUtil.getTokenValue(), "apiPermissions", user.getApiPermissions());
    }

    @GetMapping("/auth/current/menu")
    RespInfo<List<UserResourceTreeVO>> menuTree() {
        return RespInfo.success(resourceService.selectCurrentUserMenuTree());
    }

    @GetMapping("/auth/current/permissions")
    RespInfo<List<String>> buttonPermissions() {
        return RespInfo.success(resourceService.selectCurrentUserPermissions());
    }

    @GetMapping(BASE_PATH + "/page")
    IPage<SamplePurchaseOrderVO> list(@RequestParam("pageNum") long pageNum, @Valid SamplePurchaseOrderQuery query) {
        return purchasePage(pageNum, query);
    }

    @SaCheckPermission(EXPORT_PERMISSION)
    @PostMapping(BASE_PATH + "/export")
    void export(@Valid @RequestBody SamplePurchaseOrderQuery query, HttpServletResponse response) throws IOException {
        IPage<SamplePurchaseOrderVO> page = purchasePage(1, query);
        List<SamplePurchaseOrderVO> rows = new ArrayList<>(page.getRecords());
        for (long current = 2; current <= page.getPages(); current++) {
            rows.addAll(purchasePage(current, query).getRecords());
        }
        response.setContentType(XLSX);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("purchase-orders.xlsx").build().toString());
        EasyExcel.write(response.getOutputStream()).head(List.of(List.of("采购单号")))
                .sheet("采购单").doWrite(rows.stream().map(row -> List.of(row.getOrderNo())).toList());
    }

    private IPage<SamplePurchaseOrderVO> purchasePage(long pageNum, SamplePurchaseOrderQuery query) {
        return purchases.selectPage(new Page<>(pageNum, PAGE_SIZE), SessionHelper.getTenantId(), query);
    }
}
