package com.mdframe.forge.starter.orm.config;

import cn.hutool.core.net.NetUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.mdframe.forge.starter.orm.handler.InjectionMetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

/**
 * mybatis-plus配置类(下方注释有插件介绍)
 *
 * @author Lion Li
 */
@Slf4j
@EnableTransactionManagement(proxyTargetClass = true)
@MapperScan("${mybatis-plus.mapperPackage}")
public class MybatisPlusConfig implements InitializingBean {

    /**
     * 自动注入其他模块提供的拦截器（可选）
     */
    @Autowired(required = false)
    private List<InnerInterceptor> innerInterceptors;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 1. 先添加其他模块注册的拦截器（按优先级顺序）
        if (innerInterceptors != null && !innerInterceptors.isEmpty()) {
            log.info("检测到 {} 个自定义拦截器，开始注册", innerInterceptors.size());
            innerInterceptors.forEach(inner -> {
                interceptor.addInnerInterceptor(inner);
                log.info("注册拦截器: {}", inner.getClass().getSimpleName());
            });
        }
        
        // 2. 添加分页插件
        interceptor.addInnerInterceptor(paginationInnerInterceptor());
        
        // 3. 添加乐观锁插件
        interceptor.addInnerInterceptor(optimisticLockerInnerInterceptor());
        
        log.info("MyBatis-Plus拦截器初始化完成，共 {} 个拦截器", interceptor.getInterceptors().size());
        return interceptor;
    }

    /**
     * 分页插件，自动识别数据库类型
     */
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        // 分页合理化
        //paginationInnerInterceptor.setOverflow(true);
        return paginationInnerInterceptor;
    }

    /**
     * 乐观锁插件
     */
    public OptimisticLockerInnerInterceptor optimisticLockerInnerInterceptor() {
        return new OptimisticLockerInnerInterceptor();
    }

    /**
     * 使用网卡信息绑定雪花生成器
     * 防止集群雪花ID重复
     */
    @Bean
    public IdentifierGenerator idGenerator() {
        return new DefaultIdentifierGenerator(NetUtil.getLocalhost());
    }
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new InjectionMetaObjectHandler();
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("12313");
    }
}
