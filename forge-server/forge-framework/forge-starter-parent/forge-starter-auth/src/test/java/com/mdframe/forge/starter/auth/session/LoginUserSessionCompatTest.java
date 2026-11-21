package com.mdframe.forge.starter.auth.session;

import cn.dev33.satoken.json.SaJsonTemplateForJackson3;
import cn.dev33.satoken.session.SaSession;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginUser 随 Token-Session 以 JSON 存入 Redis，Admin / Flow / App 等服务共享。
 * 新增字段必须满足：能随会话还原；旧会话没有该字段时可读；旧版本服务遇到新字段时也可读。
 */
class LoginUserSessionCompatTest {

    private static final String LOGIN_USER_KEY = "loginUser";

    private final SaJsonTemplateForJackson3 json = new SaJsonTemplateForJackson3();

    @Test
    void passwordExpiredMarkerSurvivesSessionRoundTrip() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setUsername("alice");
        loginUser.setPasswordExpired(true);
        loginUser.setForcePasswordChange(true);

        LoginUser restored = readLoginUser(sessionJson(loginUser));

        assertThat(restored.getUserId()).isEqualTo(7L);
        assertThat(restored.getPasswordExpired()).isTrue();
        assertThat(restored.getForcePasswordChange()).isTrue();
    }

    @Test
    void sessionWrittenBeforePasswordExpiredFieldExistedIsStillReadable() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        loginUser.setForcePasswordChange(false);
        String legacyJson = sessionJson(loginUser).replace("\"passwordExpired\":null,", "");
        assertThat(legacyJson).doesNotContain("passwordExpired");

        LoginUser restored = readLoginUser(legacyJson);

        assertThat(restored.getUserId()).isEqualTo(7L);
        assertThat(restored.getPasswordExpired()).isNull();
        assertThat(restored.getForcePasswordChange()).isFalse();
    }

    @Test
    void sessionWithFieldUnknownToThisVersionIsStillReadable() {
        // 模拟新旧版本混跑：新版本写入了当前类没有的字段，旧版本服务读取同一个会话
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(7L);
        String newerJson = sessionJson(loginUser)
                .replace("\"userId\":", "\"fieldAddedByNewerVersion\":true,\"userId\":");
        assertThat(newerJson).contains("fieldAddedByNewerVersion");

        LoginUser restored = readLoginUser(newerJson);

        assertThat(restored.getUserId()).isEqualTo(7L);
    }

    private String sessionJson(LoginUser loginUser) {
        SaSession session = new SaSession("Authorization:login:token-session:compat-token");
        session.set(LOGIN_USER_KEY, loginUser);
        return json.objectToJson(session);
    }

    private LoginUser readLoginUser(String sessionJson) {
        SaSession session = json.jsonToObject(sessionJson, SaSession.class);
        return (LoginUser) session.get(LOGIN_USER_KEY);
    }
}
