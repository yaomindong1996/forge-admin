package com.mdframe.forge.starter.id.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.mdframe.forge.starter.id")
@MapperScan("com.mdframe.forge.starter.id.mapper")
public class IdAutoConfiguration {
}
