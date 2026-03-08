package com.mdframe.forge.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"com.mdframe.forge"})
@MapperScan("com.mdframe.forge.**.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ForgeAdminApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ForgeAdminApplication.class, args);
    }
    
}
