package com.mdframe.forge.plugin.generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 代码生成器配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "forge.generator")
public class GeneratorConfig {

    /**
     * 默认作者
     */
    private String author = "Forge Generator";

    /**
     * 默认包名
     */
    private String packageName = "com.mdframe.forge.plugin";

    /**
     * 默认模块名
     */
    private String moduleName = "system";

    /**
     * 默认模板引擎
     */
    private String templateEngine = "VELOCITY";

    /**
     * 是否覆盖已存在文件
     */
    private Boolean autoRemovePrefix = true;

    /**
     * 表前缀（自动移除）
     */
    private String tablePrefix = "sys_";

    /**
     * 生成基础路径
     */
    private String basePath = System.getProperty("user.dir");
}
