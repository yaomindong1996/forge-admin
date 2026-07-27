package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.dto.UserOrgBindDTO;
import com.mdframe.forge.plugin.system.dto.UserOrgRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.UserPostBindDTO;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import com.mdframe.forge.starter.excel.model.GenericRowData;
import com.mdframe.forge.starter.excel.model.ImportErrorRecord;
import com.mdframe.forge.starter.excel.model.ImportResult;
import com.mdframe.forge.starter.excel.service.ExcelImportService;
import com.mdframe.forge.starter.log.context.OperationAuditContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 用户管理Controller
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@Slf4j
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysUserController {

    private final ISysUserService userService;

    @Autowired(required = false)
    private ExcelImportService excelImportService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysUser>> page(SysUserQuery query) {
        IPage<SysUser> page = userService.selectUserPage(query);
        sanitizeUserPage(page);
        return RespInfo.success(page);
    }

    /**
     * 根据ID查询用户详情
     */
    @OperationLog(module = "用户管理", type = OperationType.QUERY, desc = "查询用户详情")
    @PostMapping("/getById")
    public RespInfo<SysUser> getById(@RequestParam Long id) {
        SysUser user = userService.selectUserById(id);
        clearAuthFields(user);
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser != null && loginUser.isAdmin()) {
            List<SysUserTenantVO> userTenants = userService.selectUserTenants(id);
            List<SysUserTenantVO> enabledUserTenants = userTenants.stream()
                    .filter(item -> item.getStatus() == null || item.getStatus() != 0)
                    .toList();
            user.setTenantIds(enabledUserTenants.stream()
                    .map(SysUserTenantVO::getTenantId)
                    .toList());
            user.setTenantName(enabledUserTenants.stream()
                    .map(SysUserTenantVO::getTenantName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(",")));
        } else if (loginUser != null) {
            Long tenantId = loginUser.getTenantId();
            user.setTenantId(tenantId);
            user.setTenantIds(tenantId == null ? List.of() : List.of(tenantId));
        }
        // 填充用户岗位ID列表
        user.setPostIds(userService.selectUserPostIds(id));
        // 填充用户角色ID列表，支持新增/编辑表单直接维护角色
        user.setRoleIds(userService.selectUserRoleIds(id));
        return RespInfo.success(user);
    }

    /**
     * 新增用户
     */
    @OperationLog(module = "用户管理", type = OperationType.ADD, desc = "新增用户")
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysUserDTO dto) {
        boolean result = userService.insertUser(dto);
        if (result) {
            Map<String, Object> afterData = buildUserDtoAuditData(dto);
            OperationAuditContext.setAfterData(afterData);
            OperationAuditContext.setDiffData(buildDiffData(null, afterData));
        }
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改用户
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "修改用户")
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysUserDTO dto) {
        Map<String, Object> beforeData = buildUserAuditData(dto.getId());
        OperationAuditContext.setBeforeData(beforeData);
        boolean result = userService.updateUser(dto);
        if (result) {
            recordUserAfterSnapshot(beforeData, dto.getId());
        }
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除用户
     */
    @OperationLog(module = "用户管理", type = OperationType.DELETE, desc = "删除用户")
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        Map<String, Object> beforeData = buildUserAuditData(id);
        OperationAuditContext.setBeforeData(beforeData);
        boolean result = userService.deleteUserById(id);
        if (result) {
            recordUserDeletedSnapshot(beforeData, id);
        }
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "解除用户锁定")
    @PostMapping("/doUntieDisable")
    public RespInfo<Void> doUntieDisable(@RequestParam Long id) {
        Map<String, Object> beforeData = buildUserAuditData(id);
        OperationAuditContext.setBeforeData(beforeData);
        userService.doUntieDisable(id);
        recordUserAfterSnapshot(beforeData, id);
        return RespInfo.success();
    }

    /**
     * 批量删除用户
     */
    @OperationLog(module = "用户管理", type = OperationType.DELETE, desc = "批量删除用户")
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        List<Map<String, Object>> beforeData = Arrays.stream(ids == null ? new Long[0] : ids)
                .map(this::buildUserAuditData)
                .toList();
        OperationAuditContext.setBeforeData(beforeData);
        boolean result = userService.deleteUserByIds(ids);
        if (result) {
            Map<String, Object> afterData = new LinkedHashMap<>();
            afterData.put("ids", ids == null ? List.of() : Arrays.asList(ids));
            afterData.put("deleted", true);
            OperationAuditContext.setAfterData(afterData);
            OperationAuditContext.setDiffData(afterData);
        }
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }

    /**
     * 给用户绑定角色
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "绑定用户角色")
    @PostMapping("/{userId}/roles")
    public RespInfo<Void> bindRoles(@PathVariable Long userId,
                                    @RequestBody Long[] roleIds,
                                    @RequestParam(required = false) Long tenantId) {
        boolean result = userService.bindUserRoles(userId, roleIds, tenantId);
        return result ? RespInfo.success() : RespInfo.error("绑定角色失败");
    }

    /**
     * 批量给用户追加角色。
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "批量授权用户角色")
    @PostMapping("/batch/roles")
    public RespInfo<Void> batchBindRoles(@RequestBody BatchUserRoleBindDTO dto) {
        boolean result = userService.batchBindUserRoles(dto);
        return result ? RespInfo.success() : RespInfo.error("批量授权失败");
    }

    /**
     * 解除用户角色
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "解除用户角色")
    @PostMapping("/{userId}/roles/unbind")
    public RespInfo<Void> unbindRoles(@PathVariable Long userId, @RequestBody Long[] roleIds) {
        boolean result = userService.unbindUserRoles(userId, roleIds);
        return result ? RespInfo.success() : RespInfo.error("解除角色失败");
    }

    /**
     * 给用户绑定组织
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "绑定用户组织")
    @PostMapping("/{userId}/org")
    public RespInfo<Void> bindOrg(@PathVariable Long userId, @RequestParam Long orgId, @RequestParam(required = false, defaultValue = "0") Integer isMain) {
        boolean result = userService.bindUserOrg(userId, orgId, isMain);
        return result ? RespInfo.success() : RespInfo.error("绑定组织失败");
    }

    /**
     * 解除用户组织
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "解除用户组织")
    @PostMapping("/{userId}/org/unbind")
    public RespInfo<Void> unbindOrg(@PathVariable Long userId, @RequestParam Long orgId) {
        boolean result = userService.unbindUserOrg(userId, orgId);
        return result ? RespInfo.success() : RespInfo.error("解除组织失败");
    }

    /**
     * 查询用户的角色ID列表
     */
    @GetMapping("/{userId}/roles")
    public RespInfo<List<Long>> getUserRoleIds(@PathVariable Long userId,
                                               @RequestParam(required = false) Long tenantId) {
        List<Long> roleIds = userService.selectUserRoleIds(userId, tenantId);
        return RespInfo.success(roleIds);
    }

    /**
     * 查询用户的组织ID列表
     */
    @GetMapping("/{userId}/orgs")
    public RespInfo<List<Long>> getUserOrgIds(@PathVariable Long userId,
                                              @RequestParam(required = false) Long tenantId) {
        List<Long> orgIds = userService.selectUserOrgIds(userId, tenantId);
        return RespInfo.success(orgIds);
    }

    /**
     * 查询用户组织绑定详情。
     */
    @GetMapping("/{userId}/org-bindings")
    public RespInfo<List<UserOrgBindingVO>> getUserOrgBindings(@PathVariable Long userId,
                                                               @RequestParam(required = false) Long tenantId) {
        return RespInfo.success(userService.selectUserOrgBindings(userId, tenantId));
    }

    /**
     * 查询用户的租户绑定列表
     */
    @GetMapping("/{userId}/tenants")
    public RespInfo<List<SysUserTenantVO>> getUserTenants(@PathVariable Long userId) {
        return RespInfo.success(userService.selectUserTenants(userId));
    }

    /**
     * 批量绑定用户租户
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "绑定用户租户")
    @PostMapping("/{userId}/tenants")
    public RespInfo<Void> bindTenants(@PathVariable Long userId, @RequestBody UserTenantBindDTO dto) {
        boolean result = userService.bindUserTenants(userId, dto);
        return result ? RespInfo.success() : RespInfo.error("绑定租户失败");
    }

    /**
     * 批量将用户加入租户。
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "批量加入租户")
    @PostMapping("/batch/tenants")
    public RespInfo<Void> batchBindTenant(@RequestBody BatchUserTenantBindDTO dto) {
        boolean result = userService.batchBindUserTenant(dto);
        return result ? RespInfo.success() : RespInfo.error("批量加入租户失败");
    }

    /**
     * 批量绑定用户组织
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "批量绑定用户组织")
    @PostMapping("/{userId}/orgs")
    public RespInfo<Void> bindOrgs(@PathVariable Long userId,
                                   @RequestBody UserOrgBindDTO dto,
                                   @RequestParam(required = false) Long tenantId) {
        boolean result = userService.bindUserOrgs(userId, dto.getOrgIds(), dto.getMainOrgId(), tenantId);
        return result ? RespInfo.success() : RespInfo.error("绑定组织失败");
    }

    /**
     * 查询用户在指定组织下的角色ID列表。
     */
    @GetMapping("/{userId}/org-roles")
    public RespInfo<List<Long>> getUserOrgRoleIds(@PathVariable Long userId,
                                                  @RequestParam Long orgId,
                                                  @RequestParam(required = false) Long tenantId) {
        return RespInfo.success(userService.selectUserOrgRoleIds(userId, orgId, tenantId));
    }

    /**
     * 保存用户在指定组织下的角色。
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "保存用户组织角色")
    @PostMapping("/{userId}/org-roles")
    public RespInfo<Void> bindUserOrgRoles(@PathVariable Long userId,
                                           @RequestBody UserOrgRoleBindDTO dto) {
        boolean result = userService.bindUserOrgRoles(userId, dto.getOrgId(), dto.getRoleIds(), dto.getTenantId());
        return result ? RespInfo.success() : RespInfo.error("绑定组织角色失败");
    }

    /**
     * 查询用户的岗位ID列表
     */
    @GetMapping("/{userId}/posts")
    public RespInfo<List<Long>> getUserPostIds(@PathVariable Long userId,
                                               @RequestParam(required = false) Long tenantId) {
        List<Long> postIds = userService.selectUserPostIds(userId, tenantId);
        return RespInfo.success(postIds);
    }

    /**
     * 批量绑定用户岗位
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "绑定用户岗位")
    @PostMapping("/{userId}/posts")
    public RespInfo<Void> bindPosts(@PathVariable Long userId,
                                    @RequestBody UserPostBindDTO dto,
                                    @RequestParam(required = false) Long tenantId) {
        boolean result = userService.bindUserPosts(userId, dto.getPostIds(), dto.getMainPostId(), tenantId);
        return result ? RespInfo.success() : RespInfo.error("绑定岗位失败");
    }

    /**
     * 重置用户密码
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "重置用户密码", saveRequestParams = false)
    @PostMapping("/resetPwd")
    public RespInfo<Void> resetPwd(@RequestParam Long id, @RequestParam String password) {
        Map<String, Object> beforeData = buildUserAuditData(id);
        OperationAuditContext.setBeforeData(beforeData);
        boolean result = userService.resetPassword(id, password);
        if (result) {
            Map<String, Object> afterData = buildUserAuditData(id);
            afterData.put("passwordChanged", true);
            OperationAuditContext.setAfterData(afterData);
            OperationAuditContext.setDiffData(buildDiffData(beforeData, afterData));
        }
        return result ? RespInfo.success() : RespInfo.error("重置密码失败");
    }

    /**
     * 更新用户状态
     */
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, desc = "更新用户状态")
    @PostMapping("/updateStatus")
    public RespInfo<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        Map<String, Object> beforeData = buildUserAuditData(id);
        OperationAuditContext.setBeforeData(beforeData);
        boolean result = userService.updateUserStatus(id, status);
        if (result) {
            recordUserAfterSnapshot(beforeData, id);
        }
        return result ? RespInfo.success() : RespInfo.error("操作失败");
    }

    /**
     * 更新用户资料
     */
    @OperationLog(module = "个人资料", type = OperationType.UPDATE, desc = "更新用户资料")
    @PostMapping("/updateProfile")
    public RespInfo<Void> updateProfile(@RequestBody SysUserDTO dto) {
        Long currentUserId = SessionHelper.getUserId();
        Map<String, Object> beforeData = buildUserAuditData(currentUserId);
        OperationAuditContext.setBeforeData(beforeData);
        boolean result = userService.updateUserProfile(dto);
        if (result) {
            recordUserAfterSnapshot(beforeData, currentUserId);
        }
        return result ? RespInfo.success() : RespInfo.error("更新资料失败");
    }

    /**
     * 获取当前登录用户的基本资料（直接查数据库，非缓存）
     */
    @GetMapping("/profile")
    public RespInfo<SysUser> profile() {
        Long userId = SessionHelper.getUserId();
        SysUser user = userService.getById(userId);
        clearAuthFields(user);
        return RespInfo.success(user);
    }

    private void recordUserAfterSnapshot(Map<String, Object> beforeData, Long userId) {
        Map<String, Object> afterData = buildUserAuditData(userId);
        OperationAuditContext.setAfterData(afterData);
        OperationAuditContext.setDiffData(buildDiffData(beforeData, afterData));
    }

    private void recordUserDeletedSnapshot(Map<String, Object> beforeData, Long userId) {
        Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("id", userId);
        afterData.put("deleted", true);
        OperationAuditContext.setAfterData(afterData);
        OperationAuditContext.setDiffData(buildDiffData(beforeData, afterData));
    }

    private Map<String, Object> buildUserAuditData(Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", userId);
        if (userId == null) {
            return data;
        }
        SysUser user = userService.selectUserById(userId);
        if (user == null) {
            data.put("exists", false);
            return data;
        }
        data.put("exists", true);
        data.put("tenantId", user.getTenantId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("userType", user.getUserType());
        data.put("userClient", user.getUserClient());
        data.put("email", SensitiveDataUtil.maskEmail(user.getEmail()));
        data.put("phone", SensitiveDataUtil.maskPhone(user.getPhone()));
        data.put("idCard", SensitiveDataUtil.maskIdCard(user.getIdCard()));
        data.put("gender", user.getGender());
        data.put("userStatus", user.getUserStatus());
        data.put("avatar", user.getAvatar());
        data.put("regionCode", user.getRegionCode());
        data.put("forcePasswordChange", user.getForcePasswordChange());
        data.put("remark", user.getRemark());
        data.put("createDept", user.getCreateDept());
        data.put("roleIds", safeList(() -> userService.selectUserRoleIds(userId)));
        data.put("postIds", safeList(() -> userService.selectUserPostIds(userId)));
        data.put("orgIds", safeList(() -> userService.selectUserOrgIds(userId)));
        data.put("tenantIds", safeList(() -> userService.selectUserTenants(userId).stream()
                .map(SysUserTenantVO::getTenantId)
                .toList()));
        return data;
    }

    private Map<String, Object> buildUserDtoAuditData(SysUserDTO dto) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (dto == null) {
            return data;
        }
        data.put("id", dto.getId());
        data.put("tenantId", dto.getTenantId());
        data.put("tenantIds", dto.getTenantIds());
        data.put("username", dto.getUsername());
        data.put("realName", dto.getRealName());
        data.put("userType", dto.getUserType());
        data.put("userClient", dto.getUserClient());
        data.put("email", SensitiveDataUtil.maskEmail(dto.getEmail()));
        data.put("phone", SensitiveDataUtil.maskPhone(dto.getPhone()));
        data.put("idCard", SensitiveDataUtil.maskIdCard(dto.getIdCard()));
        data.put("gender", dto.getGender());
        data.put("userStatus", dto.getUserStatus());
        data.put("avatar", dto.getAvatar());
        data.put("regionCode", dto.getRegionCode());
        data.put("remark", dto.getRemark());
        data.put("createDept", dto.getCreateDept());
        data.put("postIds", dto.getPostIds());
        data.put("roleIds", dto.getRoleIds());
        data.put("orgIds", dto.getOrgIds());
        data.put("mainOrgId", dto.getMainOrgId());
        data.put("passwordConfigured", dto.getPassword() != null && !dto.getPassword().isBlank());
        return data;
    }

    private Map<String, Object> buildDiffData(Map<String, Object> beforeData, Map<String, Object> afterData) {
        Map<String, Object> diffData = new LinkedHashMap<>();
        Set<String> keys = new LinkedHashSet<>();
        if (beforeData != null) {
            keys.addAll(beforeData.keySet());
        }
        if (afterData != null) {
            keys.addAll(afterData.keySet());
        }
        for (String key : keys) {
            Object beforeValue = beforeData == null ? null : beforeData.get(key);
            Object afterValue = afterData == null ? null : afterData.get(key);
            if (!Objects.equals(beforeValue, afterValue)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("before", beforeValue);
                item.put("after", afterValue);
                diffData.put(key, item);
            }
        }
        return diffData;
    }

    private List<Long> safeList(Supplier<List<Long>> supplier) {
        try {
            List<Long> result = supplier.get();
            return result == null ? List.of() : result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void sanitizeUserPage(IPage<SysUser> page) {
        if (page == null || page.getRecords() == null) {
            return;
        }
        page.getRecords().forEach(user -> {
            clearAuthFields(user);
            user.setPhone(SensitiveDataUtil.maskPhone(user.getPhone()));
            user.setIdCard(SensitiveDataUtil.maskIdCard(user.getIdCard()));
            user.setEmail(SensitiveDataUtil.maskEmail(user.getEmail()));
        });
    }

    private void clearAuthFields(SysUser user) {
        if (user == null) {
            return;
        }
        user.setPassword(null);
        user.setSalt(null);
    }

    /**
     * 批量导入用户
     * 复用通用 Excel 导入框架解析文件，再调用 userService.insertUser 落库。
     */
    @OperationLog(module = "用户管理", type = OperationType.IMPORT, desc = "批量导入用户")
    @PostMapping("/import")
    public RespInfo<ImportResult<?>> importUsers(@RequestParam("file") MultipartFile file) {
        if (excelImportService == null) {
            return RespInfo.error("导入服务未启用");
        }
        ImportResult<Object> result = new ImportResult<>();
        try {
            ImportResult<GenericRowData> parsed = excelImportService.importData(file, "sys_user_import", GenericRowData.class);
            // 解析阶段产生的错误直接合并
            parsed.getErrors().forEach(result::addError);
            result.setTotalRows(parsed.getTotalRows());

            LoginUser loginUser = SessionHelper.getLoginUser();
            Long tenantId = loginUser != null ? loginUser.getTenantId() : null;

            for (GenericRowData row : parsed.getSuccessData()) {
                try {
                    SysUserDTO dto = buildUserDtoFromRow(row, tenantId, loginUser);
                    userService.insertUser(dto);
                    result.getSuccessData().add(row);
                } catch (Exception e) {
                    log.error("导入用户第{}行写入失败", row.getRowNum(), e);
                    ImportErrorRecord error = new ImportErrorRecord();
                    error.setRowNum(row.getRowNum());
                    error.setErrorType("写入失败");
                    error.setErrorMessage("用户数据写入失败");
                    error.setSuggestion("请检查用户名是否重复或字段格式是否正确");
                    result.addError(error);
                }
            }
            result.setSuccessRows(result.getSuccessData().size());
            result.setFailedRows(result.getErrors().size());
            result.buildSummary();
            result.setSuccess(result.getErrors().isEmpty());
        } catch (Exception e) {
            log.error("用户导入失败", e);
            result.setSuccess(false);
            result.setSummary(ImportResult.PUBLIC_FAILURE_MESSAGE);
        }
        return RespInfo.success(result);
    }

    /**
     * 将 Excel 解析出的行数据构造为用户新增 DTO。
     * 字段名与 sys_excel_column_config 的 field_name 保持一致。
     */
    private SysUserDTO buildUserDtoFromRow(GenericRowData row, Long tenantId, LoginUser loginUser) {
        SysUserDTO dto = new SysUserDTO();
        dto.setUsername(getStringField(row, "username"));
        dto.setRealName(getStringField(row, "realName"));
        dto.setPhone(getStringField(row, "phone"));
        dto.setPassword(getStringField(row, "password"));
        dto.setEmail(getStringField(row, "email"));
        dto.setRemark(getStringField(row, "remark"));
        Integer gender = getIntegerField(row, "gender");
        if (gender != null) {
            dto.setGender(gender);
        }
        Integer userType = getIntegerField(row, "userType");
        if (userType == null) {
            userType = loginUser != null && loginUser.isAdmin() ? 2 : 2;
        }
        dto.setUserType(userType);
        if (tenantId != null) {
            dto.setTenantId(tenantId);
            dto.setTenantIds(List.of(tenantId));
        }
        // 校验必填字段
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (dto.getRealName() == null || dto.getRealName().isBlank()) {
            throw new BusinessException("真实姓名不能为空");
        }
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        return dto;
    }

    private String getStringField(GenericRowData row, String key) {
        Object value = row.getField(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer getIntegerField(GenericRowData row, String key) {
        Object value = row.getField(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
