package com.mdframe.forge.plugin.data.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DbDialectFactoryTest {

    private final DbDialectFactory factory = new DbDialectFactory(new MySqlDialect(), new PostgreSqlDialect());

    @Test
    void resolvesPostgresAliases() {
        assertThat(factory.getDialect("POSTGRESQL")).isInstanceOf(PostgreSqlDialect.class);
        assertThat(factory.getDialect("postgresql")).isInstanceOf(PostgreSqlDialect.class);
        assertThat(factory.getDialect("Postgres")).isInstanceOf(PostgreSqlDialect.class);
        assertThat(factory.getDialect("pg")).isInstanceOf(PostgreSqlDialect.class);
    }

    @Test
    void blankDefaultsToMysql() {
        assertThat(factory.getDialect(null)).isInstanceOf(MySqlDialect.class);
        assertThat(factory.getDialect("")).isInstanceOf(MySqlDialect.class);
    }

    @Test
    void unknownTypeThrows() {
        assertThatThrownBy(() -> factory.getDialect("oracle"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的数据库类型");
    }
}
