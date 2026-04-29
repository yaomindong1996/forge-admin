package com.mdframe.forge.app.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"com.mdframe.forge"})
@MapperScan("com.mdframe.forge.**.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ForgeAppServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ForgeAppServerApplication.class, args);
    }
    
}
