package com.mdframe.forge.starter.apiconfig.registry;

import com.mdframe.forge.starter.apiconfig.domain.entity.SysApiConfig;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.Method;
import java.util.*;

/**
 * API配置扫描器
 * 扫描所有Controller中的RequestMapping，解析注解信息
 */
@Slf4j
@Component
public class ApiConfigScanner {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    public ApiConfigScanner(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    /**
     * 扫描所有Controller中的接口
     *
     * @return 接口配置列表
     */
    public List<ApiConfigMeta> scanAllApis() {
        log.info("开始扫描API接口...");
        List<ApiConfigMeta> apiList = new ArrayList<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            try {
                ApiConfigMeta meta = parseApiConfig(mappingInfo, handlerMethod);
                if (meta != null) {
                    apiList.add(meta);
                }
            } catch (Exception e) {
                log.warn("解析API配置失败: {}, error={}", handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName(), e.getMessage());
            }
        }

        log.info("API接口扫描完成，共扫描到{}个接口", apiList.size());
        return apiList;
    }

    /**
     * 解析单个接口配置
     */
    private ApiConfigMeta parseApiConfig(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        // 获取请求路径
        String urlPath = null;
        
        // 从PatternsRequestCondition获取路径
        if (mappingInfo.getPathPatternsCondition() != null) {
            Set<PathPattern> patterns = mappingInfo.getPathPatternsCondition().getPatterns();
            if (patterns != null && !patterns.isEmpty()) {
                urlPath = patterns.iterator().next().getPatternString();
            }
        }
        
        if (urlPath == null) {
            log.warn("无法获取请求路径: {}", handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName());
            return null;
        }

        // 获取请求方法
        Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
        String reqMethod = methods != null && !methods.isEmpty() ? methods.iterator().next().name() : "ALL";

        // 获取类级别的注解
        Class<?> beanType = handlerMethod.getBeanType();
        ApiPermissionIgnore classAuthIgnore = beanType.getAnnotation(ApiPermissionIgnore.class);
        ApiDecrypt classDecrypt = beanType.getAnnotation(ApiDecrypt.class);
        ApiEncrypt classEncrypt = beanType.getAnnotation(ApiEncrypt.class);
        IgnoreTenant classIgnoreTenant = beanType.getAnnotation(IgnoreTenant.class);

        // 获取方法级别的注解
        Method method = handlerMethod.getMethod();
        ApiPermissionIgnore methodAuthIgnore = method.getAnnotation(ApiPermissionIgnore.class);
        ApiDecrypt methodDecrypt = method.getAnnotation(ApiDecrypt.class);
        ApiEncrypt methodEncrypt = method.getAnnotation(ApiEncrypt.class);
        IgnoreTenant methodIgnoreTenant = method.getAnnotation(IgnoreTenant.class);
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        // 构建配置元数据
        ApiConfigMeta meta = new ApiConfigMeta();
        meta.setUrlPath(urlPath);
        meta.setReqMethod(reqMethod);
        meta.setBeanType(beanType.getName());
        meta.setMethodName(method.getName());

        // 解析模块编码（从包名提取）
        meta.setModuleCode(extractModuleCode(beanType.getPackage().getName(),operationLog));

        // 解析接口名称
        meta.setApiName(buildApiName(beanType.getSimpleName(), method.getName(), operationLog));

        // 解析接口编码
        meta.setApiCode(buildApiCode(beanType.getSimpleName(), method.getName()));

        // 解析鉴权配置（方法级别 > 类级别）
        boolean needAuth = !(methodAuthIgnore != null || classAuthIgnore != null);
        meta.setAuthFlag(needAuth ? 1 : 0);

        // 解析加密配置（方法级别 > 类级别）
        boolean needEncrypt = methodDecrypt != null || methodEncrypt != null || classDecrypt != null || classEncrypt != null;
        meta.setEncryptFlag(needEncrypt ? 1 : 0);

        // 解析租户配置（方法级别 > 类级别）
        boolean needTenant = !(methodIgnoreTenant != null || classIgnoreTenant != null);
        meta.setTenantFlag(needTenant ? 1 : 0);

        // 默认值
        meta.setApiVersion("v1.0.0");
        meta.setLimitFlag(0);
        meta.setStatus(1);

        return meta;
    }

    /**
     * 从包名提取模块编码
     */
    private String extractModuleCode(String packageName,OperationLog operationLog) {
        if (operationLog != null && !operationLog.module().isEmpty()) {
            return operationLog.module();
        }
        // 例如: com.mdframe.forge.plugin.system.controller -> system
        String[] parts = packageName.split("\\.");
        if (parts.length >= 4) {
            return parts[3];
        }
        return "default";
    }

    /**
     * 构建接口名称
     */
    private String buildApiName(String className, String methodName, OperationLog operationLog) {
        if (operationLog != null && !operationLog.desc().isEmpty()) {
            return operationLog.desc();
        }
        // 从类名和方法名推断
        String simpleClassName = className.replace("Controller", "").replace("Sys", "");
        return simpleClassName + "-" + methodName;
    }

    /**
     * 构建接口编码
     */
    private String buildApiCode(String className, String methodName) {
        // 例如: UserController.getUser -> user:getUser
        String simpleClassName = className.replace("Controller", "").toLowerCase();
        return simpleClassName + ":" + methodName;
    }

    /**
     * API配置元数据
     */
    @Data
    public static class ApiConfigMeta {
        /**
         * 接口名称
         */
        private String apiName;

        /**
         * 接口编码
         */
        private String apiCode;

        /**
         * 请求方式
         */
        private String reqMethod;

        /**
         * 接口请求路径
         */
        private String urlPath;

        /**
         * 接口版本号
         */
        private String apiVersion;

        /**
         * 所属业务模块
         */
        private String moduleCode;

        /**
         * 所属微服务ID
         */
        private String serviceId;

        /**
         * 是否需认证/鉴权
         */
        private Integer authFlag;

        /**
         * 是否需报文加解密
         */
        private Integer encryptFlag;

        /**
         * 是否启用租户隔离
         */
        private Integer tenantFlag;

        /**
         * 是否开启限流
         */
        private Integer limitFlag;

        /**
         * 状态
         */
        private Integer status;

        /**
         * Bean类型
         */
        private String beanType;

        /**
         * 方法名
         */
        private String methodName;

        /**
         * 转换为实体对象
         */
        public SysApiConfig toEntity() {
            SysApiConfig entity = new SysApiConfig();
            entity.setApiName(apiName);
            entity.setApiCode(apiCode);
            entity.setReqMethod(reqMethod);
            entity.setUrlPath(urlPath);
            entity.setApiVersion(apiVersion);
            entity.setModuleCode(moduleCode);
            entity.setServiceId(serviceId);
            entity.setAuthFlag(authFlag);
            entity.setEncryptFlag(encryptFlag);
            entity.setTenantFlag(tenantFlag);
            entity.setLimitFlag(limitFlag);
            entity.setStatus(status);
            return entity;
        }
    }
}
