package com.mdframe.forge.plugin.ai.agent.engine.event.persistence;

import com.mdframe.forge.plugin.ai.agent.engine.event.mapper.AgentEventMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentEventMapperContractTest {
    @Test
    void should_insertActualXmlBatchWithExplicitTenantAndFields() throws Exception {
        try (SqlSession session = session()) {
            var first = row(9007199254740993L, 7L, "first");
            var second = row(9007199254740994L, 7L, "second");
            second.setParentId(first.getId());
            assertThat(session.getMapper(AgentEventMapper.class).insertBatch(List.of(first, second))).isEqualTo(2);
            try (var statement = session.getConnection().createStatement();
                 var result = statement.executeQuery("SELECT * FROM ai_agent_event ORDER BY id")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("id")).isEqualTo(first.getId());
                assertThat(result.getLong("tenant_id")).isEqualTo(7L);
                assertThat(result.getString("event_data")).isEqualTo("first");
                assertThat(result.getTimestamp("create_time").toLocalDateTime()).isEqualTo(first.getCreateTime());
                assertThat(result.next()).isTrue();
                assertThat(result.getLong("parent_id")).isEqualTo(first.getId());
                assertThat(result.getString("event_data")).isEqualTo("second");
                assertThat(result.next()).isFalse();
            }
        }
    }

    @Test
    void should_notPartiallyInsertBatch_when_uniqueConstraintFails() throws Exception {
        try (SqlSession session = session()) {
            var mapper = session.getMapper(AgentEventMapper.class);
            mapper.insertBatch(List.of(row(11L, 7L, "existing")));
            assertThatThrownBy(() -> mapper.insertBatch(List.of(row(12L, 7L, "new"), row(11L, 7L, "duplicate"))))
                    .isInstanceOf(org.apache.ibatis.exceptions.PersistenceException.class);
            try (var statement = session.getConnection().createStatement();
                 var result = statement.executeQuery("SELECT COUNT(*) FROM ai_agent_event")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void should_bindEventContentAsData_notSql() throws Exception {
        try (SqlSession session = session()) {
            String body = "'); DELETE FROM ai_agent_event; --";
            session.getMapper(AgentEventMapper.class).insertBatch(List.of(row(11L, 7L, body), row(12L, 7L, "safe")));
            try (var statement = session.getConnection().createStatement();
                 var result = statement.executeQuery("SELECT event_data FROM ai_agent_event ORDER BY id")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo(body);
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("safe");
            }
        }
    }

    private static SqlSession session() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        var configuration = new Configuration(new Environment("fixture", new JdbcTransactionFactory(), source));
        String resource = "mapper/AgentEventMapper.xml";
        try (var stream = AgentEventMapperContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(stream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
        try (var statement = session.getConnection().createStatement()) {
            statement.execute("""
                    CREATE TABLE ai_agent_event (
                        id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, session_id VARCHAR(64) NOT NULL,
                        turn_index INT NOT NULL, event_type VARCHAR(50) NOT NULL, event_data CLOB,
                        parent_id BIGINT, create_time TIMESTAMP NOT NULL
                    )
                    """);
        }
        return session;
    }

    private static AgentEventEntity row(Long id, Long tenantId, String text) {
        var row = new AgentEventEntity();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setSessionId("synthetic-session");
        row.setTurnIndex(1);
        row.setEventType("TEXT_BLOCK_DELTA");
        row.setEventData(text);
        row.setCreateTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        return row;
    }
}
