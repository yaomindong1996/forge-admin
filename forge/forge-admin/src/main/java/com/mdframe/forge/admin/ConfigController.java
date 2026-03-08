package com.mdframe.forge.admin;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.property.example.AppConfigExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {
    
    @Value("${sys.user.initPassword:''}") // 从数据库读取
    private String appName;
    
    @Value("${forge.log.print-operation-log:false}") // 从数据库读取
    private Boolean operation;

    @Autowired
    private AppConfigExample appConfigExample;
    
    @Autowired
    private LogProperties logProperties;
    
    @Autowired
    private Environment environment;
    
    @GetMapping("/config")
    @SaIgnore
    public Object getConfig() {
        
        return operation + appName + environment.getProperty("forge.log.print-operation-log") + ".." +
                logProperties.getPrintOperationLog() + "enableOperationLog:" + logProperties.getEnableOperationLog();
    }
}
