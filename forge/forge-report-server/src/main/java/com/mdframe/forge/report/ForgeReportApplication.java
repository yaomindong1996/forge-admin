package com.mdframe.forge.report;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Forge Report 数据可视化大屏后端服务
 */
@SpringBootApplication(scanBasePackages = {"com.mdframe.forge"})
@MapperScan("com.mdframe.forge.report.**.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ForgeReportApplication {
    
    public static void main(String[] args) {
        // 设置默认 profile 为 dev（如果未指定）
        if (System.getProperty("spring.profiles.active") == null 
                && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
            System.setProperty("spring.profiles.active", "dev");
        }
        SpringApplication.run(ForgeReportApplication.class, args);
    }
    
}
