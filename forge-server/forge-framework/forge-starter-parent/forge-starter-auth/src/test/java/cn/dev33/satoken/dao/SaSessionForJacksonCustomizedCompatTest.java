package cn.dev33.satoken.dao;

import cn.dev33.satoken.json.SaJsonTemplateForJackson3;
import cn.dev33.satoken.session.SaSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Boot4 / Sa-Token 1.45 Jackson3 仍能读出 ≤1.38 redis-jackson 写入的 Session JSON。
 */
class SaSessionForJacksonCustomizedCompatTest {

    @Test
    void shouldDeserializeLegacyRedisSessionTypeId() {
        // 字段形状取自本机 Redis 存量 Authorization:login:token-session:*（1.38 redis-jackson 写出）
        String legacyJson = """
                {"@class":"cn.dev33.satoken.dao.SaSessionForJacksonCustomized",\
                "id":"Authorization:login:token-session:legacy-token",\
                "type":"Token-Session","loginType":"login","loginId":null,\
                "token":"legacy-token","createTime":1710000000000,\
                "dataMap":{"@class":"java.util.concurrent.ConcurrentHashMap"},\
                "tokenSignList":["java.util.Vector",[]]}
                """;

        SaSession session = new SaJsonTemplateForJackson3().jsonToObject(legacyJson, SaSession.class);

        assertThat(session).isInstanceOf(SaSessionForJacksonCustomized.class);
        assertThat(session.getId()).isEqualTo("Authorization:login:token-session:legacy-token");
        assertThat(session.getToken()).isEqualTo("legacy-token");
    }
}
