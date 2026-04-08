# 演示环境控制功能 Spec

## 变更概述

**变更名称**: demo-environment-control  
**变更类型**: 新功能开发  
**优先级**: 高  
**影响范围**: 全局API拦截、系统配置、前端交互  
**预计工作量**: 1-2人日  

---

## 一、背景与动机

### 1.1 业务背景

Forge Admin系统需要对外提供演示环境，供潜在客户或用户试用体验。演示环境需要：

- **保护数据安全**：防止恶意修改、删除系统数据
- **保证体验完整**：允许用户查看所有功能和数据
- **降低维护成本**：自动限制写操作，无需人工干预

### 1.2 当前问题

| 问题类别 | 具体问题 | 业务影响 |
|---------|---------|---------|
| **数据安全** | 演示环境无保护，用户可随意修改/删除数据 | 演示环境数据混乱，影响其他用户体验 |
| **维护成本** | 需要定期手动恢复数据库 | 浪费人力，维护成本高 |
| **无法区分** | 系统无法识别是否为演示环境 | 无法实现环境级别的差异化控制 |
| **提示缺失** | 用户不知道是演示环境，操作失败无提示 | 用户体验差，误以为系统故障 |

### 1.3 优化目标

1. **环境识别**：系统配置区分演示环境和生产环境
2. **API拦截**：演示环境自动拦截POST/PUT/DELETE请求
3. **友好提示**：前端明确提示用户当前为演示环境
4. **白名单机制**：支持部分接口豁免（如登录、查询）
5. **动态开关**：无需重启即可切换演示模式

---

## 二、需求规格

### 2.1 核心功能需求

#### 功能1：演示环境配置

**需求描述**：提供全局配置项，标识当前是否为演示环境。

**业务价值**：
- 环境隔离：区分演示环境和生产环境
- 灵活控制：可通过配置动态切换
- 集中管理：统一的环境配置中心

**功能规格**：
- 配置项：`forge.demo.enabled`（默认false）
- 配置项：`forge.demo.message`（自定义提示消息）
- 配置项：`forge.demo.whitelist-urls`（白名单URL列表）
- 支持配置中心动态刷新（@RefreshScope）

#### 功能2：API请求拦截

**需求描述**：演示环境下拦截POST/PUT/DELETE请求，返回友好提示。

**业务价值**：
- 数据保护：自动拦截所有写操作
- 安全防护：防止恶意破坏
- 一致性：统一的拦截逻辑

**功能规格**：
- 拦截方法：POST、PUT、DELETE、PATCH
- 拦截范围：所有业务接口（除白名单外）
- 白名单接口：
  - `/auth/login`（登录）
  - `/auth/logout`（登出）
  - `/auth/online/**`（在线用户查询）
  - 所有GET请求
- 返回格式：
  ```json
  {
    "code": 403,
    "msg": "演示环境禁止修改操作",
    "data": null
  }
  ```

#### 功能3：前端提示

**需求描述**：前端在演示环境下显示明显的环境标识和提示。

**业务价值**：
- 用户知情：用户明确知道是演示环境
- 减少误操作：提前告知不可修改
- 体验提升：友好的提示信息

**功能规格**：
- 页面顶部显示："当前为演示环境，部分功能受限"
- 操作按钮置灰或隐藏（新增、编辑、删除按钮）
- Toast提示：点击修改按钮时弹出提示
- 环境标识：页面Header显示"演示环境"标签

#### 功能4：接口白名单

**需求描述**：支持配置接口白名单，部分接口在演示环境也可写操作。

**业务价值**：
- 灵活性：特殊场景需要写操作
- 用户体验：登录、个人设置等不影响体验

**功能规格**：
- 白名单配置：支持通配符
- 默认白名单：
  - `/auth/login`
  - `/auth/logout`
  - `/system/user/profile`（个人设置）
  - `/system/user/updatePassword`（修改密码）

---

## 三、技术设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│     Demo Environment Control Architecture                │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ DemoConfig   │  │ DemoIntercep-│  │ DemoAspect   │  │
│  │ (配置类)     │  │ tor(拦截器)  │  │ (切面)       │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ DemoAnnotat- │  │ DemoResponse │  │ Frontend     │  │
│  │ ion(注解)    │  │ Handler      │  │ Indicator    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### 3.2 配置模型设计

#### DemoProperties配置类

```java
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

### 3.3 拦截器设计

#### DemoInterceptor

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoInterceptor implements HandlerInterceptor {
    
    private final DemoProperties demoProperties;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        // 未启用演示模式，放行
        if (!demoProperties.getEnabled()) {
            return true;
        }
        
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        
        // GET请求放行
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }
        
        // 白名单URL放行
        if (isWhitelisted(requestURI)) {
            return true;
        }
        
        // 拦截写操作
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
        return demoProperties.getWhitelistUrls().stream()
            .anyMatch(pattern -> match(pattern, requestURI));
    }
}
```

### 3.4 注解设计（可选扩展）

#### @IgnoreDemo注解

用于标记某个接口在演示环境也可访问：

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreDemo {
    /**
     * 是否忽略演示环境限制
     */
    boolean value() default true;
}
```

### 3.5 配置文件示例

```yaml
# application-demo.yml
forge:
  demo:
    # 启用演示模式
    enabled: true
    
    # 提示消息
    message: "演示环境禁止修改操作，如需完整体验请联系管理员"
    
    # 白名单URL（支持通配符）
    whitelist-urls:
      - /auth/login
      - /auth/logout
      - /auth/captcha/**
      - /system/user/profile
      - /system/user/updatePassword
      - /system/config/getByKey/**
    
    # 拦截的HTTP方法
    blocked-methods:
      - POST
      - PUT
      - DELETE
      - PATCH
```

---

## 四、数据模型变更

### 4.1 系统配置表

无需新增表，使用现有的 `sys_config` 表存储演示环境配置：

```sql
-- 演示环境配置
INSERT INTO sys_config (config_key, config_value, config_name, remark) VALUES
('demo.enabled', 'false', '演示环境开关', '是否启用演示环境模式'),
('demo.message', '演示环境禁止修改操作', '演示环境提示', '演示环境拦截提示消息'),
('demo.whitelist', '/auth/login,/auth/logout', '演示环境白名单', '演示环境白名单URL列表');
```

---

## 五、接口变更

### 5.1 新增接口

#### API1：获取演示环境状态

```java
@GetMapping("/api/system/demo/status")
@SaIgnore
public RespInfo<Map<String, Object>> getDemoStatus() {
    Map<String, Object> result = new HashMap<>();
    result.put("enabled", demoProperties.getEnabled());
    result.put("message", demoProperties.getMessage());
    return RespInfo.success(result);
}
```

**前端调用示例**：

```javascript
// 获取演示环境状态
const { data } = await request.get('/system/demo/status')

if (data.enabled) {
  // 显示演示环境标识
  showDemoIndicator(data.message)
  
  // 禁用写操作按钮
  disableWriteButtons()
}
```

---

## 六、前端改造

### 6.1 环境检测

在 `App.vue` 或 `layout` 中检测演示环境：

```javascript
// stores/demo.js
export const useDemoStore = defineStore('demo', {
  state: () => ({
    enabled: false,
    message: ''
  }),
  
  actions: {
    async loadDemoStatus() {
      try {
        const { data } = await request.get('/system/demo/status')
        this.enabled = data.enabled
        this.message = data.message
      } catch (error) {
        console.error('获取演示环境状态失败', error)
      }
    }
  }
})
```

### 6.2 UI适配

#### 方案1：全局提示条

```vue
<!-- Layout.vue -->
<template>
  <div class="layout">
    <!-- 演示环境提示条 -->
    <n-alert 
      v-if="demoStore.enabled"
      type="warning"
      closable
      class="demo-banner"
    >
      <template #header>
        <n-icon><WarningOutlined /></n-icon>
        演示环境
      </template>
      {{ demoStore.message }}
    </n-alert>
    
    <!-- 主内容 -->
    <router-view />
  </div>
</template>

<script setup>
import { useDemoStore } from '@/stores/demo'

const demoStore = useDemoStore()

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
}
</style>
```

#### 方案2：按钮禁用

```vue
<!-- 使用自定义指令 -->
<template>
  <n-button 
    v-demo-disabled
    type="primary" 
    @click="handleAdd"
  >
    新增
  </n-button>
</template>

<script setup>
// 自定义指令：演示环境禁用按钮
const vDemoDisabled = {
  mounted(el, binding) {
    const demoStore = useDemoStore()
    if (demoStore.enabled) {
      el.disabled = true
      el.classList.add('demo-disabled')
      el.title = demoStore.message
    }
  }
}
</script>
```

### 6.3 请求拦截

```javascript
// utils/request.js
request.interceptors.response.use(
  response => {
    const { data } = response
    
    // 演示环境拦截提示
    if (data.code === 403 && data.msg?.includes('演示环境')) {
      window.$message.warning(data.msg, { duration: 5000 })
    }
    
    return data
  },
  error => {
    // ...
  }
)
```

---

## 七、风险评估

### 7.1 技术风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| 拦截器性能 | 低 | 每个请求都需要判断 | 简单判断逻辑，性能影响小 |
| 白名单配置错误 | 中 | 部分接口无法访问 | 提供默认白名单，文档说明 |
| 前端缓存 | 低 | 环境状态缓存导致不一致 | 定期刷新状态或监听配置变更 |

### 7.2 业务风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| 误拦截正常请求 | 中 | 影响用户体验 | 完善白名单配置，提供豁免机制 |
| 用户不知道是演示环境 | 低 | 用户困惑 | 前端明显标识，友好提示 |
| 配置泄露 | 中 | 生产环境误启用 | 配置项默认false，加强权限控制 |

### 7.3 兼容性风险

| 风险项 | 风险等级 | 影响分析 | 缓解措施 |
|-------|---------|---------|---------|
| 现有接口无影响 | 低 | 仅新增功能 | 向后兼容 |
| 配置项迁移 | 低 | 需要配置演示环境 | 提供配置示例 |

---

## 八、测试策略

### 8.1 单元测试

**测试用例清单**：

| 测试类 | 核心测试方法 | 验证点 |
|-------|-------------|--------|
| `DemoInterceptorTest` | `testBlockPostInDemoMode` | 演示模式拦截POST请求 |
| `DemoInterceptorTest` | `testAllowGetInDemoMode` | 演示模式允许GET请求 |
| `DemoInterceptorTest` | `testWhitelistInDemoMode` | 白名单URL放行 |
| `DemoInterceptorTest` | `testDisabledDemoMode` | 关闭演示模式全放行 |
| `DemoPropertiesTest` | `testDefaultConfig` | 默认配置验证 |

### 8.2 集成测试

**测试场景**：

1. **演示环境拦截测试**
   - 启用演示模式
   - 访问POST接口 → 403错误
   - 访问GET接口 → 正常返回
   - 访问白名单接口 → 正常返回

2. **生产环境放行测试**
   - 关闭演示模式
   - 所有接口正常访问

3. **前端提示测试**
   - 启用演示模式
   - 检查提示条是否显示
   - 检查按钮是否禁用

---

## 九、实施计划

### 9.1 开发任务拆解

| 任务ID | 任务描述 | 预估工时 | 优先级 | 涉及文件 |
|-------|---------|---------|-------|---------|
| T1 | 创建DemoProperties配置类 | 0.5小时 | P0 | DemoProperties.java |
| T2 | 实现DemoInterceptor拦截器 | 1小时 | P0 | DemoInterceptor.java |
| T3 | 注册拦截器到WebMvcConfigurer | 0.5小时 | P0 | WebMvcConfig.java |
| T4 | 创建@IgnoreDemo注解（可选） | 0.5小时 | P1 | IgnoreDemo.java |
| T5 | 实现前端环境检测逻辑 | 1小时 | P0 | demo.js, Layout.vue |
| T6 | 实现前端UI提示组件 | 1小时 | P0 | DemoBanner.vue |
| T7 | 实现请求拦截和提示 | 0.5小时 | P0 | request.js |
| T8 | 编写单元测试 | 1小时 | P1 | 测试类 |
| T9 | 编写集成测试 | 0.5小时 | P2 | 测试类 |
| T10 | 编写配置文档 | 0.5小时 | P1 | README.md |

**总工时**: 7小时（约1人日）  
**建议团队**: 1人  

---

## 十、待澄清问题

### 10.1 技术决策待确认

| 问题ID | 问题描述 | 影响范围 | 建议方案 |
|-------|---------|---------|---------|
| Q1 | 白名单配置放在配置文件还是数据库？ | 灵活性 | 建议配置文件，支持动态刷新 |
| Q2 | 是否需要@IgnoreDemo注解？ | 扩展性 | 建议实现，提高灵活性 |
| Q3 | 前端提示方式：全局提示条还是对话框？ | 用户体验 | 建议全局提示条，不打断用户 |
| Q4 | 是否需要记录拦截日志？ | 审计 | 建议记录，方便排查问题 |
| Q5 | 是否需要动态切换演示模式？ | 运维 | 建议支持，通过配置中心 |

### 10.2 业务规则待确认

| 问题ID | 问题描述 | 影响范围 | 建议方案 |
|-------|---------|---------|---------|
| Q6 | 是否限制文件上传？ | 安全性 | 建议拦截POST，自然限制上传 |
| Q7 | 是否限制导出功能？ | 数据安全 | 建议不限制（GET请求） |
| Q8 | 个人设置是否允许修改？ | 用户体验 | 建议加入白名单 |
| Q9 | 是否显示被拦截的次数统计？ | 运营分析 | 可作为后续优化项 |

---

## 十一、验收标准

### 11.1 功能验收标准

| 验收项 | 验收标准 | 验证方法 |
|-------|---------|---------|
| 配置管理 | 演示模式开关正常 | 修改配置，验证生效 |
| GET请求 | 演示环境GET请求正常 | 访问查询接口 |
| POST拦截 | 演示环境POST请求被拦截 | 访问新增接口 |
| 白名单 | 白名单接口正常访问 | 访问登录接口 |
| 前端提示 | 显示演示环境标识 | 检查页面显示 |
| 友好提示 | 操作失败提示友好 | 触发拦截操作 |

---

## 十二、参考资料

- Spring Boot Interceptor官方文档
- Vue3状态管理最佳实践
- 演示环境安全控制方案

---

**文档版本**: v1.0  
**编写日期**: 2026-04-08  
**编写人**: AI Assistant  
**审核状态**: 待用户审核