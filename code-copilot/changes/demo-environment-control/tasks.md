# 任务拆分 — 演示环境控制功能

> 拆分顺序：配置模型 → 拦截器 → 前端集成 → 测试验证
> 每个任务 = 可独立提交的原子变更

---

## Task 1: 创建演示环境配置类

- **目标**: 创建DemoProperties配置类，支持演示环境配置项
- **涉及文件**:
  - `forge/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/context/DemoProperties.java` — 新增

- **关键签名**:
  ```java
  @Data
  @Component
  @ConfigurationProperties(prefix = "forge.demo")
  @RefreshScope
  public class DemoProperties {
      private Boolean enabled = false;
      private String message = "演示环境禁止修改操作";
      private List<String> whitelistUrls = new ArrayList<>();
      private List<String> blockedMethods = Arrays.asList("POST", "PUT", "DELETE", "PATCH");
  }
  ```

- [ ] **Step 1: 创建DemoProperties类**

```java
package com.mdframe.forge.starter.core.context;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "forge.demo")
@RefreshScope
public class DemoProperties {
    
    /**
     * 是否启用演示模式
     */
    private Boolean enabled = false;
    
    /**
     * 演示环境提示消息
     */
    private String message = "演示环境禁止修改操作";
    
    /**
     * 白名单URL列表（支持通配符）
     */
    private List<String> whitelistUrls = new ArrayList<>();
    
    /**
     * 拦截的HTTP方法
     */
    private List<String> blockedMethods = Arrays.asList(
        "POST", "PUT", "DELETE", "PATCH"
    );
}
```

- [ ] **Step 2: 创建配置文件示例**

`application-demo.yml`:

```yaml
forge:
  demo:
    enabled: true
    message: "演示环境禁止修改操作，如需完整体验请联系管理员"
    whitelist-urls:
      - /auth/login
      - /auth/logout
      - /auth/captcha/**
      - /system/user/profile
    blocked-methods:
      - POST
      - PUT
      - DELETE
      - PATCH
```

- [ ] **Step 3: 编译验证**

```bash
cd forge && mvn clean compile -DskipTests
```

- [ ] **Step 4: Commit**

```bash
git add forge/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/context/DemoProperties.java
git commit -m "feat(demo): add demo environment configuration properties"
```

---

## Task 2: 实现演示环境拦截器

- **目标**: 实现DemoInterceptor，拦截写操作
- **涉及文件**:
  - `forge/forge-framework/forge-starter-parent/forge-starter-web/src/main/java/com/mdframe/forge/starter/web/interceptor/DemoInterceptor.java` — 新增

- **关键签名**:
  ```java
  @Slf4j
  @Component
  @RequiredArgsConstructor
  public class DemoInterceptor implements HandlerInterceptor {
      @Override
      public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler);
  }
  ```

- [ ] **Step 1: 创建DemoInterceptor类**

```java
package com.mdframe.forge.starter.web.interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.starter.core.context.DemoProperties;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoInterceptor implements HandlerInterceptor {
    
    private final DemoProperties demoProperties;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        if (!demoProperties.getEnabled()) {
            return true;
        }
        
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        if (isWhitelisted(requestURI)) {
            log.debug("演示环境白名单放行: {} {}", method, requestURI);
            return true;
        }
        
        if (demoProperties.getBlockedMethods().contains(method.toUpperCase())) {
            log.warn("演示环境拦截写操作: {} {}", method, requestURI);
            
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            
            RespInfo<Object> result = RespInfo.error(
                HttpServletResponse.SC_FORBIDDEN,
                demoProperties.getMessage()
            );
            
            response.getWriter().write(JSON.toJSONString(result));
            return false;
        }
        
        return true;
    }
    
    private boolean isWhitelisted(String requestURI) {
        if (demoProperties.getWhitelistUrls() == null || demoProperties.getWhitelistUrls().isEmpty()) {
            return false;
        }
        
        return demoProperties.getWhitelistUrls().stream()
            .anyMatch(pattern -> match(pattern, requestURI));
    }
    
    private boolean match(String pattern, String path) {
        if (StrUtil.isBlank(pattern) || StrUtil.isBlank(path)) {
            return false;
        }
        
        pattern = pattern.trim();
        path = path.trim();
        
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (!path.startsWith(prefix)) {
                return false;
            }
            String suffix = path.substring(prefix.length());
            return !suffix.contains("/");
        }
        
        return pattern.equals(path);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add forge/forge-framework/forge-starter-parent/forge-starter-web/src/main/java/com/mdframe/forge/starter/web/interceptor/DemoInterceptor.java
git commit -m "feat(demo): implement demo environment interceptor"
```

---

## Task 3: 注册拦截器

- **目标**: 在WebMvcConfigurer中注册DemoInterceptor
- **涉及文件**:
  - `forge/forge-framework/forge-starter-parent/forge-starter-web/src/main/java/com/mdframe/forge/starter/web/config/WebMvcConfig.java` — 修改

- [ ] **Step 1: 修改WebMvcConfig类**

在现有的WebMvcConfig类中添加拦截器注册：

```java
@Autowired
private DemoInterceptor demoInterceptor;

@Override
public void addInterceptors(InterceptorRegistry registry) {
    // 演示环境拦截器（优先级最高）
    registry.addInterceptor(demoInterceptor)
        .addPathPatterns("/**")
        .order(0);
    
    // 其他拦截器...
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn clean compile -DskipTests
```

- [ ] **Step 3: Commit**

```bash
git add forge/forge-framework/forge-starter-parent/forge-starter-web/src/main/java/com/mdframe/forge/starter/web/config/WebMvcConfig.java
git commit -m "feat(demo): register demo interceptor in WebMvcConfig"
```

---

## Task 4: 创建演示环境状态查询接口

- **目标**: 提供API接口供前端查询演示环境状态
- **涉及文件**:
  - `forge/forge-framework/forge-starter-parent/forge-starter-web/src/main/java/com/mdframe/forge/starter/web/controller/DemoController.java` — 新增

- [ ] **Step 1: 创建DemoController**

```java
package com.mdframe.forge.starter.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.context.DemoProperties;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system/demo")
@RequiredArgsConstructor
public class DemoController {
    
    private final DemoProperties demoProperties;
    
    @GetMapping("/status")
    @SaIgnore
    @IgnoreTenant
    public RespInfo<Map<String, Object>> getDemoStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", demoProperties.getEnabled());
        result.put("message", demoProperties.getMessage());
        return RespInfo.success(result);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add forge/forge-framework/forge-starter-parent/forge-starter-web/src/main/java/com/mdframe/forge/starter/web/controller/DemoController.java
git commit -m "feat(demo): add demo status query API"
```

---

## Task 5: 前端环境检测Store

- **目标**: 创建前端演示环境状态管理
- **涉及文件**:
  - `forge-admin-ui/src/stores/demo.js` — 新增

- [ ] **Step 1: 创建demo store**

```javascript
import { defineStore } from 'pinia'
import { request } from '@/utils'

export const useDemoStore = defineStore('demo', {
  state: () => ({
    enabled: false,
    message: '',
    loaded: false
  }),
  
  getters: {
    isDemo: (state) => state.enabled,
    demoMessage: (state) => state.message
  },
  
  actions: {
    async loadDemoStatus() {
      if (this.loaded) return
      
      try {
        const { data } = await request.get('/system/demo/status')
        this.enabled = data.enabled || false
        this.message = data.message || ''
        this.loaded = true
      } catch (error) {
        console.error('获取演示环境状态失败', error)
        this.enabled = false
        this.loaded = true
      }
    },
    
    clearStatus() {
      this.enabled = false
      this.message = ''
      this.loaded = false
    }
  }
})
```

- [ ] **Step 2: Commit**

```bash
git add forge-admin-ui/src/stores/demo.js
git commit -m "feat(ui): add demo environment store"
```

---

## Task 6: 前端演示环境提示组件

- **目标**: 创建全局演示环境提示条组件
- **涉及文件**:
  - `forge-admin-ui/src/components/DemoBanner.vue` — 新增

- [ ] **Step 1: 创建DemoBanner组件**

```vue
<template>
  <n-alert
    v-if="demoStore.isDemo"
    type="warning"
    closable
    class="demo-banner"
    @close="handleClose"
  >
    <template #header>
      <div class="flex items-center gap-4">
        <n-icon size="18">
          <WarningOutlined />
        </n-icon>
        <span class="font-medium">演示环境</span>
      </div>
    </template>
    {{ demoStore.demoMessage }}
  </n-alert>
</template>

<script setup>
import { WarningOutlined } from '@vicons/ionicons5'
import { useDemoStore } from '@/stores/demo'

const demoStore = useDemoStore()

const handleClose = () => {
  // 关闭提示条（可选：记住用户选择）
  localStorage.setItem('demo-banner-closed', 'true')
}

onMounted(() => {
  demoStore.loadDemoStatus()
})
</script>

<style scoped>
.demo-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  text-align: center;
  background: #FFF7E6;
  border-bottom: 1px solid #FFD591;
  border-radius: 0;
}

.demo-banner :deep(.n-alert-body) {
  padding: 8px 16px;
}
</style>
```

- [ ] **Step 2: 在Layout中集成**

修改主布局文件：

```vue
<template>
  <div class="layout-container">
    <DemoBanner />
    <router-view />
  </div>
</template>

<script setup>
import DemoBanner from '@/components/DemoBanner.vue'
</script>

<style scoped>
.layout-container {
  padding-top: 0;
}

.layout-container:has(.demo-banner) {
  padding-top: 48px;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add forge-admin-ui/src/components/DemoBanner.vue
git commit -m "feat(ui): add demo environment banner component"
```

---

## Task 7: 前端请求拦截增强

- **目标**: 在请求拦截器中处理演示环境错误提示
- **涉及文件**:
  - `forge-admin-ui/src/utils/request.js` — 修改

- [ ] **Step 1: 修改响应拦截器**

```javascript
// 在响应拦截器中添加演示环境错误处理
request.interceptors.response.use(
  response => {
    const { data } = response
    
    // 演示环境拦截提示
    if (data.code === 403 && data.msg?.includes('演示环境')) {
      window.$message.warning(data.msg, { 
        duration: 5000,
        keepAliveOnHover: true
      })
    }
    
    return data
  },
  error => {
    const { response } = error
    
    if (response?.status === 403) {
      const data = response.data
      if (data?.msg?.includes('演示环境')) {
        window.$message.warning(data.msg, { 
          duration: 5000,
          keepAliveOnHover: true
        })
        return Promise.reject(error)
      }
    }
    
    // 其他错误处理...
    return Promise.reject(error)
  }
)
```

- [ ] **Step 2: Commit**

```bash
git add forge-admin-ui/src/utils/request.js
git commit -m "feat(ui): add demo environment error handler"
```

---

## Task 8: 编写单元测试

- **目标**: 编写拦截器单元测试
- **涉及文件**:
  - `forge/forge-framework/forge-starter-parent/forge-starter-web/src/test/java/com/mdframe/forge/starter/web/interceptor/DemoInterceptorTest.java` — 新增

- [ ] **Step 1: 创建测试类**

```java
package com.mdframe.forge.starter.web.interceptor;

import com.mdframe.forge.starter.core.context.DemoProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoInterceptorTest {
    
    @Mock
    private DemoProperties demoProperties;
    
    private DemoInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        interceptor = new DemoInterceptor(demoProperties);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }
    
    @Test
    void testDisabledDemoMode() throws Exception {
        when(demoProperties.getEnabled()).thenReturn(false);
        
        request.setMethod("POST");
        request.setRequestURI("/api/system/user");
        
        boolean result = interceptor.preHandle(request, response, null);
        
        assertTrue(result);
        assertEquals(200, response.getStatus());
    }
    
    @Test
    void testAllowGetInDemoMode() throws Exception {
        when(demoProperties.getEnabled()).thenReturn(true);
        
        request.setMethod("GET");
        request.setRequestURI("/api/system/user/page");
        
        boolean result = interceptor.preHandle(request, response, null);
        
        assertTrue(result);
    }
    
    @Test
    void testBlockPostInDemoMode() throws Exception {
        when(demoProperties.getEnabled()).thenReturn(true);
        when(demoProperties.getBlockedMethods()).thenReturn(Arrays.asList("POST", "PUT", "DELETE"));
        when(demoProperties.getMessage()).thenReturn("演示环境禁止修改操作");
        when(demoProperties.getWhitelistUrls()).thenReturn(Arrays.asList());
        
        request.setMethod("POST");
        request.setRequestURI("/api/system/user");
        
        boolean result = interceptor.preHandle(request, response, null);
        
        assertFalse(result);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("演示环境禁止修改操作"));
    }
    
    @Test
    void testWhitelistInDemoMode() throws Exception {
        when(demoProperties.getEnabled()).thenReturn(true);
        when(demoProperties.getBlockedMethods()).thenReturn(Arrays.asList("POST"));
        when(demoProperties.getWhitelistUrls()).thenReturn(Arrays.asList("/auth/login"));
        
        request.setMethod("POST");
        request.setRequestURI("/auth/login");
        
        boolean result = interceptor.preHandle(request, response, null);
        
        assertTrue(result);
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd forge && mvn test -Dtest=DemoInterceptorTest
```

- [ ] **Step 3: Commit**

```bash
git add forge/forge-framework/forge-starter-parent/forge-starter-web/src/test/java/com/mdframe/forge/starter/web/interceptor/DemoInterceptorTest.java
git commit -m "test(demo): add demo interceptor unit tests"
```

---

## Task 9: 配置文档和示例

- **目标**: 编写配置使用文档
- **涉及文件**:
  - `code-copilot/changes/demo-environment-control/usage-guide.md` — 新增

- [ ] **Step 1: 创建使用文档**

参考之前的文档模板，编写演示环境配置指南。

- [ ] **Step 2: Commit**

```bash
git add code-copilot/changes/demo-environment-control/usage-guide.md
git commit -m "docs(demo): add demo environment usage guide"
```

---

## Task 10: 整体测试验证

- **目标**: 整体功能测试
- **涉及文件**: 无新增

- [ ] **Step 1: 启动后端服务**

```bash
cd forge/forge-admin && mvn spring-boot:run
```

- [ ] **Step 2: 修改配置启用演示模式**

在 `application.yml` 中：

```yaml
forge:
  demo:
    enabled: true
```

- [ ] **Step 3: 启动前端服务**

```bash
cd forge-admin-ui && pnpm dev
```

- [ ] **Step 4: 功能测试**

1. 访问任意页面，检查顶部是否显示演示环境提示条
2. 尝试新增数据，检查是否提示"演示环境禁止修改操作"
3. 尝试查询数据，检查是否正常返回
4. 检查白名单接口（登录）是否正常

- [ ] **Step 5: Final Commit**

```bash
git add -A
git commit -m "feat(demo): complete demo environment control implementation"
```

---

## 执行完成标志

- [ ] 所有单元测试通过
- [ ] 演示模式拦截正常
- [ ] 白名单放行正常
- [ ] 前端提示正常
- [ ] 文档完整