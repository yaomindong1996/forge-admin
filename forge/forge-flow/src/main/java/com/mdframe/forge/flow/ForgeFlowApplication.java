package com.mdframe.forge.flow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Forge Flow 流程服务启动类
 *
 * @author forge
 */
@SpringBootApplication(scanBasePackages = {"com.mdframe.forge"})
@MapperScan("com.mdframe.forge.**.mapper")
public class ForgeFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ForgeFlowApplication.class, args);
    }
}
