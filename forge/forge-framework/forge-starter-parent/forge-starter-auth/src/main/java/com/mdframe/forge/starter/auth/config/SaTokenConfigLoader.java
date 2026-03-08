package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.SaManager;
import com.mdframe.forge.starter.config.config.SecurityConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.property.event.ConfigRefreshEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenConfigLoader implements ApplicationRunner, ApplicationListener<ConfigRefreshEvent> {

    private final ConfigManagerService configManagerService;

    @Override
    public void run(ApplicationArguments args) {
        applyConfig();
    }
    
    
//    @Async
//    @EventListener
//    public void onApiConfigRefresh(ConfigRefreshEvent event) {
//        log.info("sa-Token配置发生变化");
//        applyConfig();
//    }
    
   
    public void applyConfig() {
        try {
            cn.dev33.satoken.config.SaTokenConfig config = SaManager.getConfig();
            
            SecurityConfig securityConfig = configManagerService.getSecurityConfig();
            if (securityConfig == null) {
                return;
            }
            SecurityConfig.SaTokenConfig securityConfigSaToken = securityConfig.getSaToken();
            
            if (securityConfigSaToken == null) {
                return;
            }
            
            config.setTokenName(securityConfigSaToken.getTokenName());
            config.setTokenPrefix(securityConfigSaToken.getTokenPrefix());
            config.setTimeout(securityConfigSaToken.getTimeout());
            config.setActiveTimeout(securityConfigSaToken.getActivityTimeout());
            config.setIsConcurrent(securityConfigSaToken.getIsConcurrent());
            config.setIsShare(securityConfigSaToken.getIsShare());
            config.setIsReadBody(securityConfigSaToken.getIsReadBody());
            config.setIsReadCookie(securityConfigSaToken.getIsReadCookie());
            config.setIsReadHeader(securityConfigSaToken.getIsReadHeader());

            log.info("Sa-Token配置已从数据库加载并应用: tokenName={}, timeout={}s, activeTimeout={}s, " +
                            "isConcurrent={}, isShare={}, tokenStyle={}",
                    config.getTokenName(), config.getTimeout(), config.getActiveTimeout(),
                    config.getIsConcurrent(), config.getIsShare(), config.getTokenStyle());
        } catch (Exception e) {
            log.warn("从数据库加载Sa-Token配置失败，使用yml默认配置: {}", e.getMessage());
        }
    }
    
    @Override
    public void onApplicationEvent(ConfigRefreshEvent event) {
        log.info("sa-Token配置发生变化");
        applyConfig();
    }
}
