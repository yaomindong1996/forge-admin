package com.mdframe.forge.plugin.job.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzDriverDelegateResolverTest {

    @Test
    void resolvesSqlServer() {
        assertThat(QuartzDriverDelegateResolver.resolve("Microsoft SQL Server",
                "jdbc:sqlserver://localhost:1433;databaseName=forge"))
                .isEqualTo(QuartzDriverDelegateResolver.SQLSERVER);
    }

    @Test
    void resolvesPostgres() {
        assertThat(QuartzDriverDelegateResolver.resolve("PostgreSQL", "jdbc:postgresql://localhost/forge"))
                .isEqualTo(QuartzDriverDelegateResolver.POSTGRES);
    }

    @Test
    void defaultsToStdJdbc() {
        assertThat(QuartzDriverDelegateResolver.resolve("MySQL", "jdbc:mysql://localhost/forge"))
                .isEqualTo(QuartzDriverDelegateResolver.MYSQL);
        assertThat(QuartzDriverDelegateResolver.resolve(null, null))
                .isEqualTo(QuartzDriverDelegateResolver.MYSQL);
    }
}
