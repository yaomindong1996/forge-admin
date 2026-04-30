package com.mdframe.forge.starter.auth.controller;

import com.mdframe.forge.starter.auth.domain.*;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.auth.service.IAuthService;
import com.mdframe.forge.starter.auth.service.IMenuService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @Autowired(required = false)
    private IMenuService menuService;

    /**
     * 统一登录入口
     * 根据authType参数自动选择认证方式：
     * - password: 用户名+密码
     * - code: 用户名+密码+验证码
     *
     * @param request 登录请求，包含authType字段
     * @return 登录结果
     */
    @PostMapping("/login")
    @IgnoreTenant
    public RespInfo<LoginResult> login(@RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return RespInfo.success(result);
    }

    /**
     * 登出
     */
    @IgnoreTenant
    @PostMapping("/logout")
    public RespInfo<Void> logout() {
        authService.logout();
        return RespInfo.success();
    }

    /**
     * 获取当前登录用户信息（每次完全从DB重建，与登录时 buildLoginUser 逻辑一致）
     */
    @GetMapping("/userInfo")
    public RespInfo<LoginUser> getUserInfo() {
        LoginUser loginUser = SessionHelper.getLoginUser();

        if (loginUser != null) {
            LoginUser fresh = authService.loadUserByUsername(
                    loginUser.getUsername(), loginUser.getTenantId());
            if (fresh != null) {
                fresh.setLoginTime(System.currentTimeMillis());
                fresh.setLoginIp(loginUser.getLoginIp());
                fresh.setUserClient(loginUser.getUserClient());
                SessionHelper.setLoginUser(fresh);
                loginUser = fresh;
            }
        }

        return RespInfo.success(loginUser);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public RespInfo<LoginUser> register(@RequestBody RegisterRequest request) {
        LoginUser loginUser = authService.register(request);
        return RespInfo.success(loginUser);
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public RespInfo<Void> changePassword(@RequestParam String oldPassword,
                                          @RequestParam String newPassword) {
        boolean success = authService.changePassword(oldPassword, newPassword);
        return success ? RespInfo.success() : RespInfo.error("修改密码失败");
    }

    /**
     * 重置密码
     */
    @PostMapping("/resetPassword")
    public RespInfo<Void> resetPassword(@RequestParam String username,
                                         @RequestParam String newPassword,
                                         @RequestParam String code,
                                         @RequestParam String codeKey) {
        boolean success = authService.resetPassword(username, newPassword, code, codeKey);
        return success ? RespInfo.success() : RespInfo.error("重置密码失败");
    }

    /**
     * 获取验证码（根据配置返回对应类型的验证码）
     */
    @GetMapping("/captcha")
    @IgnoreTenant
    public RespInfo<CaptchaResult> getCaptcha() {
        CaptchaResult result = authService.getCaptcha();
        return RespInfo.success(result);
    }

    /**
     * 获取滑块验证码
     */
    @GetMapping("/captcha/slider")
    @IgnoreTenant
    public RespInfo<SliderCaptchaResult> getSliderCaptcha() {
        SliderCaptchaResult result = authService.getSliderCaptcha();
        return RespInfo.success(result);
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/captcha/sms")
    @IgnoreTenant
    public RespInfo<SmsCaptchaResult> sendSmsCaptcha(@RequestParam String phone) {
        SmsCaptchaResult result = authService.sendSmsCaptcha(phone);
        return RespInfo.success(result);
    }

    /**
     * 获取登录配置
     */
    @GetMapping("/loginConfig")
    @IgnoreTenant
    public RespInfo<LoginConfigResult> getLoginConfig() {
        LoginConfigResult result = authService.getLoginConfig();
        return RespInfo.success(result);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refreshToken")
    public RespInfo<LoginResult> refreshToken() {
        LoginResult result = authService.refreshToken();
        return RespInfo.success(result);
    }

    /**
     * 查询当前用户的资源树（含菜单和按钮权限）
     */
    @GetMapping("/current/tree")
    public RespInfo<List<UserResourceTreeVO>> getCurrentUserResourceTree() {
        return RespInfo.success(menuService.selectCurrentUserResourceTree());
    }

    /**
     * 查询当前用户的菜单树（仅目录和菜单，不含按钮）
     */
    @GetMapping("/current/menu")
    public RespInfo<List<UserResourceTreeVO>> getCurrentUserMenuTree() {
        return RespInfo.success(menuService.selectCurrentUserMenuTree());
    }

    /**
     * 查询当前用户的权限标识列表
     */
    @GetMapping("/current/permissions")
    public RespInfo<List<String>> getCurrentUserPermissions() {
        return RespInfo.success(menuService.selectCurrentUserPermissions());
    }
}
