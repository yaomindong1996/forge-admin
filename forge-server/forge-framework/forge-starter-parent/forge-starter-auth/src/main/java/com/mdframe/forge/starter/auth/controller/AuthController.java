package com.mdframe.forge.starter.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.starter.auth.domain.*;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.auth.service.IAuthService;
import com.mdframe.forge.starter.auth.service.IMenuService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
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
    @SaIgnore
    @PostMapping("/login")
    @IgnoreTenant
    public RespInfo<LoginResult> login(@RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return RespInfo.success(result);
    }

    /**
     * 申请一次性 SSO 票据
     */
    @ApiDecrypt
    @IgnoreTenant
    @PostMapping("/sso/ticket")
    public RespInfo<SsoTicketResult> createSsoTicket(@RequestBody SsoTicketRequest request) {
        SsoTicketResult result = authService.createSsoTicket(request);
        return RespInfo.success(result);
    }

    /**
     * 使用一次性票据换发目标客户端 Token
     */
    @SaIgnore
    @IgnoreTenant
    @ApiDecrypt
    @PostMapping("/sso/exchange")
    public RespInfo<LoginResult> exchangeSsoTicket(@RequestBody SsoExchangeRequest request) {
        LoginResult result = authService.exchangeSsoTicket(request);
        return RespInfo.success(result);
    }

    /**
     * 登出
     */
    @SaIgnore
    @IgnoreTenant
    @PostMapping("/logout")
    public RespInfo<Void> logout() {
        authService.logout();
        return RespInfo.success();
    }

    /**
     * 获取当前登录用户信息（每次按已认证用户ID从DB重建，与登录时 buildLoginUser 逻辑一致）
     */
    @GetMapping("/userInfo")
    public RespInfo<LoginUser> getUserInfo() {
        LoginUser loginUser = SessionHelper.getLoginUser();

        if (loginUser != null) {
            LoginUser fresh = authService.loadUserByUserId(
                    loginUser.getUserId(), loginUser.getTenantId(), loginUser.getActiveOrgId());
            if (fresh != null) {
                fresh.setLoginTime(loginUser.getLoginTime());
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
    @SaIgnore
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
     * 发送找回密码验证码。通道由全局短信/邮件配置决定。
     */
    @SaIgnore
    @PostMapping("/resetPassword/code")
    @IgnoreTenant
    public RespInfo<Void> sendResetPasswordCode(@RequestBody SendResetPasswordCodeRequest request) {
        authService.sendResetPasswordCode(request);
        return RespInfo.success("验证码已发送", null);
    }

    /**
     * 通过短信或邮箱验证码重置密码。
     */
    @SaIgnore
    @PostMapping("/resetPassword")
    @IgnoreTenant
    public RespInfo<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        boolean success = authService.resetPassword(request);
        return success ? RespInfo.success() : RespInfo.error("重置密码失败");
    }

    /**
     * 获取验证码（根据配置返回对应类型的验证码）
     */
    @SaIgnore
    @GetMapping("/captcha")
    @IgnoreTenant
    public RespInfo<CaptchaResult> getCaptcha() {
        CaptchaResult result = authService.getCaptcha();
        return RespInfo.success(result);
    }

    /**
     * 获取滑块验证码
     */
    @SaIgnore
    @GetMapping("/captcha/slider")
    @IgnoreTenant
    public RespInfo<SliderCaptchaResult> getSliderCaptcha() {
        SliderCaptchaResult result = authService.getSliderCaptcha();
        return RespInfo.success(result);
    }

    /**
     * 发送短信验证码
     */
    @SaIgnore
    @PostMapping("/captcha/sms")
    @IgnoreTenant
    public RespInfo<SmsCaptchaResult> sendSmsCaptcha(@RequestParam String phone) {
        SmsCaptchaResult result = authService.sendSmsCaptcha(phone);
        return RespInfo.success(result);
    }

    /**
     * 获取登录配置（含群二维码开关与图片引用，登录页匿名读取）
     */
    @SaIgnore
    @GetMapping("/loginConfig")
    @IgnoreTenant
    public RespInfo<LoginConfigResult> getLoginConfig(@RequestParam(required = false) String userClient,
                                                      @RequestParam(required = false) Long tenantId) {
        LoginConfigResult result = authService.getLoginConfig(userClient, tenantId);
        return RespInfo.success(result);
    }

    /**
     * 查询登录页可选租户。
     */
    @SaIgnore
    @GetMapping("/tenant/options")
    @IgnoreTenant
    public RespInfo<List<LoginTenantOption>> loginTenantOptions() {
        return RespInfo.success(authService.listLoginTenantOptions());
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
