package com.mdframe.forge.plugin.system.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientCredentialSurfaceContractTest {

    private static final Path PROJECT_ROOT = Path.of("../../../..");

    @Test
    void operationLogShouldHardExcludeCredentialEndpoints() throws IOException {
        String aspect = Files.readString(Path.of(
                "../../forge-starter-parent/forge-starter-log/src/main/java/"
                        + "com/mdframe/forge/starter/log/aspect/OperationLogAspect.java"));
        int hardExclusion = aspect.indexOf("SENSITIVE_CREDENTIAL_PATH_SUFFIXES.stream()");
        int configurableExclusion = aspect.indexOf("logProperties.getExcludePaths()", hardExclusion);

        assertThat(aspect).contains(
                "\"/auth/login\"",
                "\"/auth/register\"",
                "\"/auth/changePassword\"",
                "\"/auth/resetPassword\"",
                "\"/auth/online/kickout\"",
                "\"/auth/online/batchKickout\"",
                "requestUrl.replaceAll(\";[^/]*\", \"\").replaceAll(\"/+$\", \"\")");
        assertThat(hardExclusion).isGreaterThanOrEqualTo(0).isLessThan(configurableExclusion);

        for (Path initSql : List.of(
                Path.of("../../../db/全量初始化SQL.sql"),
                Path.of("../../../forge-admin-server/sql/初始化脚本.sql"))) {
            assertThat(Files.readString(initSql)).as(initSql.toString()).contains("\"/auth/login\"");
        }
    }

    @Test
    void browserEnvironmentShouldNotAdvertiseClientSecrets() throws IOException {
        for (String file : List.of(
                ".env", ".env.development", ".env.production", ".env.example", ".env.test")) {
            Path envFile = PROJECT_ROOT.resolve("forge-admin-ui").resolve(file);
            assertThat(Files.readString(envFile)).as(envFile.toString())
                    .doesNotContain("VITE_APP_SECRET", "VITE_CLIENT_SECRET", "客户端AppSecret");
        }
    }

    @Test
    void mapperAndManagementResponseShouldKeepStoredSecretPrivate() throws IOException {
        String mapper = Files.readString(Path.of("src/main/resources/mapper/SysClientMapper.xml"));
        String clientService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/service/impl/ClientServiceImpl.java"));
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/controller/SysClientController.java"));
        String vo = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/vo/SysClientVO.java"));

        assertThat(mapper).contains(
                "id=\"ClientResultMap\"",
                "property=\"ipWhitelist\"",
                "typeHandler=\"com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler\"",
                "id=\"selectClientPage\"",
                "id=\"selectEnabledClients\"",
                "id=\"selectByClientCode\"",
                "id=\"selectByAppId\"",
                "id=\"updateClientIfUnchanged\"",
                "id=\"updateAppSecretIfUnchanged\"");
        String atomicUpdate = xmlStatement(mapper, "updateClientIfUnchanged", "update");
        assertThat(atomicUpdate).contains(
                "client_auth_method = #{client.clientAuthMethod}",
                "<if test=\"secretChanged\">app_secret = #{appSecret},</if>",
                "expectedAuthMethod == null",
                "client_auth_method = #{expectedAuthMethod}",
                "expectedSecret == null",
                "app_secret = #{expectedSecret}");
        assertThat(clientService)
                .contains("clientMapper.updateClientIfUnchanged(")
                .doesNotContain("clientMapper.updateById(");
        assertThat(controller)
                .contains("vo.setAppSecretMasked(hasAppSecret ? \"****\" : \"\")")
                .doesNotContain("RespInfo.success(client.getAppSecret())");
        assertThat(vo).doesNotContain("private String appSecret;");
    }

    @Test
    void bearerTokensShouldNotEnterLogsOrBroadcastPayloads() throws IOException {
        String listener = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/listener/SystemSaTokenListener.java"));
        String onlineService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/service/impl/SysOnlineUserServiceImpl.java"));
        String authService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/plugin/system/service/impl/SystemAuthServiceImpl.java"));

        assertThat(listener).doesNotContain("tokenValue: {}", "tokenValue={}");
        assertThat(onlineService)
                .doesNotContain("tokenValue={}", "token={}", "data.put(\"tokenValue\"")
                .doesNotMatch("(?s).*log\\.(?:trace|debug|info|warn|error)\\([^;]{0,300},\\s*token(?:Value)?\\b.*");
        assertThat(authService).doesNotContain("token={}, client={}");
    }

    private String xmlStatement(String mapper, String statementId, String elementName) {
        int start = mapper.indexOf("id=\"" + statementId + "\"");
        int end = mapper.indexOf("</" + elementName + ">", start);
        assertThat(start).as(statementId).isGreaterThanOrEqualTo(0);
        assertThat(end).as(statementId).isGreaterThan(start);
        return mapper.substring(start, end);
    }
}
