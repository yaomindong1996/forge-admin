# 外部接口代理转发功能设计

> 版本: 1.0  
> 日期: 2026-05-10  
> 作者: OpenCode

---

## 1. 需求背景

### 1.1 当前问题

Forge Report UI 在设计报表时，动态请求配置存在以下问题：

1. **跨域问题**：调用外部系统接口时，前端直接发起请求会遇到 CORS 限制
2. **认证方式固定**：当前仅支持 Bearer Token，无法适配外部系统的多种认证方式
3. **数据格式不一致**：外部接口返回数据格式与前端期望格式不匹配
4. **接口配置无持久化**：每次设计报表都需要重新输入完整的接口地址

### 1.2 优化目标

1. 支持内部接口（report-server）和外部系统接口的区分管理
2. 外部接口通过 server 代理转发，解决跨域和认证问题
3. 认证方式采用策略模式，便于扩展
4. 数据适配采用混合模式（JSON Path + 脚本化）
5. 外部系统和接口配置持久化管理，方便用户选择

---

## 2. 整体架构

### 2.1 模块划分

```
forge-plugin-external/                   # 新插件模块
├── controller/
│   ├── ExternalSystemController.java    # 外部系统管理 CRUD
│   ├── ExternalApiController.java       # 外部接口管理 CRUD
│   └── ExternalProxyController.java     # 代理转发入口
├── service/
│   ├── ExternalSystemService.java       # 外部系统服务接口
│   ├── ExternalApiService.java          # 外部接口服务接口
│   ├── ExternalProxyService.java        # 代理转发核心接口
│   └── impl/
│       ├── ExternalSystemServiceImpl.java
│       ├── ExternalApiServiceImpl.java
│       └── ExternalProxyServiceImpl.java
├── strategy/
│   ├── AuthStrategy.java                # 认证策略接口
│   ├── impl/
│   │   ├── NoneAuthStrategy.java        # 无认证实现
│   │   ├── BearerTokenAuthStrategy.java # Bearer Token 实现
│   └── AuthStrategyFactory.java         # 策略工厂
├── adapter/
│   ├── DataAdapter.java                 # 数据适配接口
│   ├── impl/
│   │   ├── NoneAdapter.java             # 无适配实现
│   │   ├── JsonPathAdapter.java         # JSON Path 适配
│   │   ├── ScriptAdapter.java           # 脚本适配
│   └── DataAdapterFactory.java          # 适配器工厂
├── entity/
│   ├── ExternalSystem.java              # 外部系统实体
│   ├── ExternalApi.java                 # 外部接口实体
├── mapper/
│   ├── ExternalSystemMapper.java
│   ├── ExternalApiMapper.java
│   └── ExternalSystemMapper.xml
│   └── ExternalApiMapper.xml
├── dto/
│   ├── ExternalSystemDTO.java
│   ├── ExternalApiDTO.java
│   ├── ExternalApiVO.java               # 包含系统名称的接口VO
└── enums/
    ├── AuthTypeEnum.java                # 认证类型枚举
    ├── AdapterTypeEnum.java             # 适配类型枚举
```

### 2.2 技术选型

| 技术 | 用途 | 版本 |
|------|------|------|
| HttpClient (Java 11+) | HTTP 请求发送 | 内置 |
| fastjson | JSON 解析 | 2.0 |
| Nashorn | JavaScript 脚本执行 | Java 8+ 内置 |

---

## 3. 数据库设计

### 3.1 外部系统配置表 (sys_external_system)

```sql
CREATE TABLE sys_external_system (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    system_name     VARCHAR(100) NOT NULL COMMENT '系统名称',
    system_code     VARCHAR(50) NOT NULL COMMENT '系统编码（唯一标识）',
    base_url        VARCHAR(255) NOT NULL COMMENT '基础URL',
    auth_type       VARCHAR(20) NOT NULL COMMENT '认证类型',
    auth_config     TEXT COMMENT '认证配置（JSON格式）',
    description     VARCHAR(500) COMMENT '系统描述',
    status          CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_by       VARCHAR(64) COMMENT '创建者',
    create_time     DATETIME COMMENT '创建时间',
    create_dept     BIGINT COMMENT '创建部门',
    update_by       VARCHAR(64) COMMENT '更新者',
    update_time     DATETIME COMMENT '更新时间',
    INDEX idx_tenant_system_code (tenant_id, system_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部系统配置表';
```

**auth_config JSON 结构示例（Bearer Token）**：
```json
{
    "token": "your-access-token",
    "tokenHeader": "Authorization",
    "tokenPrefix": "Bearer"
}
```

### 3.2 外部接口配置表 (sys_external_api)

```sql
CREATE TABLE sys_external_api (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id       BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    system_id       BIGINT NOT NULL COMMENT '所属系统ID',
    api_name        VARCHAR(100) NOT NULL COMMENT '接口名称',
    api_code        VARCHAR(50) NOT NULL COMMENT '接口编码',
    api_path        VARCHAR(255) NOT NULL COMMENT '接口路径（相对路径）',
    method          VARCHAR(10) NOT NULL COMMENT '请求方法',
    description     VARCHAR(500) COMMENT '接口描述',
    adapter_type    VARCHAR(20) COMMENT '适配类型',
    adapter_config  TEXT COMMENT '适配配置（JSON Path 或脚本）',
    status          CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
    create_by       VARCHAR(64) COMMENT '创建者',
    create_time     DATETIME COMMENT '创建时间',
    create_dept     BIGINT COMMENT '创建部门',
    update_by       VARCHAR(64) COMMENT '更新者',
    update_time     DATETIME COMMENT '更新时间',
    INDEX idx_tenant_system_id (tenant_id, system_id),
    INDEX idx_tenant_api_code (tenant_id, api_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部接口配置表';
```

**adapter_config JSON 结构示例（JsonPath）**：
```json
{
    "sourcePath": "data.list",
    "targetPath": "data",
    "fieldMapping": {
        "name": "userName",
        "value": "amount"
    }
}
```

**adapter_config 脚本示例（Script）**：
```javascript
var result = {
    code: 0,
    data: response.result.items.map(function(item) {
        return {
            name: item.user_name,
            value: item.amount
        };
    })
};
```

---

## 4. 认证策略模式

### 4.1 策略接口定义

```java
package com.mdframe.forge.plugin.external.strategy;

public interface AuthStrategy {
    
    /**
     * 获取认证类型标识
     */
    String getAuthType();
    
    /**
     * 为请求添加认证信息
     * @param requestBuilder HTTP请求构建器
     * @param authConfig 认证配置（JSON字符串）
     */
    void applyAuth(HttpRequest.Builder requestBuilder, String authConfig);
    
    /**
     * 解析并验证认证配置
     * @param authConfig 认证配置（JSON字符串）
     * @return 配置是否有效
     */
    boolean validateConfig(String authConfig);
}
```

### 4.2 Bearer Token 策略实现

```java
package com.mdframe.forge.plugin.external.strategy.impl;

@Component
public class BearerTokenAuthStrategy implements AuthStrategy {
    
    @Override
    public String getAuthType() {
        return "BearerToken";
    }
    
    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        String token = config.getString("token");
        String header = config.getString("tokenHeader");
        String prefix = config.getString("tokenPrefix");
        
        requestBuilder.header(header, prefix + " " + token);
    }
    
    @Override
    public boolean validateConfig(String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        return config.containsKey("token");
    }
}
```

### 4.3 策略工厂

```java
package com.mdframe.forge.plugin.external.strategy;

@Component
public class AuthStrategyFactory {
    
    private final Map<String, AuthStrategy> strategies;
    
    public AuthStrategyFactory(List<AuthStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(AuthStrategy::getAuthType, Function.identity()));
    }
    
    public AuthStrategy getStrategy(String authType) {
        return strategies.getOrDefault(authType, strategies.get("None"));
    }
    
    public List<String> getSupportedTypes() {
        return new ArrayList<>(strategies.keySet());
    }
}
```

---

## 5. 数据适配器设计

### 5.1 适配器接口定义

```java
package com.mdframe.forge.plugin.external.adapter;

public interface DataAdapter {
    
    /**
     * 获取适配类型标识
     */
    String getAdapterType();
    
    /**
     * 转换外部接口返回数据
     * @param originalData 外部接口原始返回数据
     * @param adapterConfig 适配配置
     * @return 转换后的数据（符合前端期望格式）
     */
    Object transform(Object originalData, String adapterConfig);
    
    /**
     * 验证适配配置是否有效
     */
    boolean validateConfig(String adapterConfig);
}
```

### 5.2 JSON Path 适配器实现

```java
package com.mdframe.forge.plugin.external.adapter.impl;

@Component
public class JsonPathAdapter implements DataAdapter {
    
    @Override
    public String getAdapterType() {
        return "JsonPath";
    }
    
    @Override
    public Object transform(Object originalData, String adapterConfig) {
        JSONObject config = JSON.parseObject(adapterConfig);
        JSONObject sourceData = (JSONObject) originalData;
        
        // 1. 提取源路径数据
        String sourcePath = config.getString("sourcePath");
        Object extractedData = extractByPath(sourceData, sourcePath);
        
        // 2. 字段映射转换
        JSONObject fieldMapping = config.getJSONObject("fieldMapping");
        Object transformedData = mapFields(extractedData, fieldMapping);
        
        // 3. 构建目标结构
        String targetPath = config.getString("targetPath");
        return buildTargetStructure(transformedData, targetPath);
    }
    
    private Object extractByPath(JSONObject source, String path) {
        if (path == null || path.isEmpty()) {
            return source;
        }
        String[] parts = path.split(".");
        Object current = source;
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            }
        }
        return current;
    }
    
    private Object mapFields(Object data, JSONObject mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return data;
        }
        if (data instanceof JSONArray) {
            JSONArray result = new JSONArray();
            for (Object item : (JSONArray) data) {
                if (item instanceof JSONObject) {
                    result.add(mapSingleObject((JSONObject) item, mapping));
                }
            }
            return result;
        }
        if (data instanceof JSONObject) {
            return mapSingleObject((JSONObject) data, mapping);
        }
        return data;
    }
    
    private JSONObject mapSingleObject(JSONObject source, JSONObject mapping) {
        JSONObject result = new JSONObject();
        for (String targetField : mapping.keySet()) {
            String sourceField = mapping.getString(targetField);
            result.put(targetField, source.get(sourceField));
        }
        return result;
    }
    
    private Object buildTargetStructure(Object data, String targetPath) {
        JSONObject result = new JSONObject();
        result.put("code", 0);
        
        if (targetPath == null || targetPath.isEmpty()) {
            result.put("data", data);
        } else {
            JSONObject targetContainer = new JSONObject();
            targetContainer.put(targetPath, data);
            result.put("data", targetContainer);
        }
        return result;
    }
    
    @Override
    public boolean validateConfig(String adapterConfig) {
        try {
            JSONObject config = JSON.parseObject(adapterConfig);
            return config != null;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 5.3 脚本适配器实现

```java
package com.mdframe.forge.plugin.external.adapter.impl;

@Component
public class ScriptAdapter implements DataAdapter {
    
    private final ScriptEngineManager scriptEngineManager;
    
    public ScriptAdapter() {
        this.scriptEngineManager = new ScriptEngineManager();
    }
    
    @Override
    public String getAdapterType() {
        return "Script";
    }
    
    @Override
    public Object transform(Object originalData, String adapterConfig) {
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
        
        // 注入原始数据
        engine.put("response", originalData);
        
        // 执行转换脚本
        try {
            engine.eval(adapterConfig);
            return engine.get("result");
        } catch (ScriptException e) {
            throw new RuntimeException("脚本执行失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateConfig(String adapterConfig) {
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
        try {
            engine.eval(adapterConfig);
            return true;
        } catch (ScriptException e) {
            return false;
        }
    }
}
```

### 5.4 适配器工厂

```java
package com.mdframe.forge.plugin.external.adapter;

@Component
public class DataAdapterFactory {
    
    private final Map<String, DataAdapter> adapters;
    
    public DataAdapterFactory(List<DataAdapter> adapterList) {
        this.adapters = adapterList.stream()
            .collect(Collectors.toMap(DataAdapter::getAdapterType, Function.identity()));
    }
    
    public DataAdapter getAdapter(String adapterType) {
        return adapters.getOrDefault(adapterType, adapters.get("None"));
    }
    
    public List<String> getSupportedTypes() {
        return new ArrayList<>(adapters.keySet());
    }
}
```

---

## 6. 代理转发服务

### 6.1 代理转发核心服务

```java
package com.mdframe.forge.plugin.external.service.impl;

@Service
@RequiredArgsConstructor
public class ExternalProxyServiceImpl implements ExternalProxyService {
    
    private final ExternalApiService apiService;
    private final ExternalSystemService systemService;
    private final AuthStrategyFactory authFactory;
    private final DataAdapterFactory adapterFactory;
    
    @Override
    public Object proxyRequest(Long apiId, Map<String, Object> params) {
        // 1. 获取接口配置
        ExternalApi api = apiService.getById(apiId);
        if (api == null || !"0".equals(api.getStatus())) {
            throw new BusinessException("接口不存在或已停用");
        }
        
        // 2. 获取系统配置
        ExternalSystem system = systemService.getById(api.getSystemId());
        if (system == null || !"0".equals(system.getStatus())) {
            throw new BusinessException("系统不存在或已停用");
        }
        
        // 3. 构建完整请求URL
        String fullUrl = buildFullUrl(system.getBaseUrl(), api.getApiPath(), params);
        
        // 4. 构建HTTP请求
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(fullUrl))
            .timeout(Duration.ofSeconds(30));
        
        // 5. 应用认证策略
        AuthStrategy authStrategy = authFactory.getStrategy(system.getAuthType());
        authStrategy.applyAuth(requestBuilder, system.getAuthConfig());
        
        // 6. 设置请求方法和参数
        applyRequestMethod(requestBuilder, api.getMethod(), params);
        
        // 7. 执行请求
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = sendRequest(client, requestBuilder.build());
        
        // 8. 解析原始响应
        Object originalData = JSON.parse(response.body());
        
        // 9. 应用数据适配
        if (api.getAdapterType() != null && !api.getAdapterType().isEmpty()) {
            DataAdapter adapter = adapterFactory.getAdapter(api.getAdapterType());
            return adapter.transform(originalData, api.getAdapterConfig());
        }
        
        // 10. 无适配则直接返回
        return originalData;
    }
    
    private String buildFullUrl(String baseUrl, String apiPath, Map<String, Object> params) {
        String url = baseUrl;
        if (!baseUrl.endsWith("/")) {
            url += "/";
        }
        url += apiPath.startsWith("/") ? apiPath.substring(1) : apiPath;
        return url;
    }
    
    private void applyRequestMethod(HttpRequest.Builder builder, String method, Map<String, Object> params) {
        switch (method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(params)));
                builder.header("Content-Type", "application/json");
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(params)));
                builder.header("Content-Type", "application/json");
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                builder.GET();
        }
    }
    
    private HttpResponse<String> sendRequest(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new BusinessException("请求外部接口失败: " + e.getMessage());
        }
    }
}
```

### 6.2 代理转发 Controller

```java
package com.mdframe.forge.plugin.external.controller;

@RestController
@RequestMapping("/external/proxy")
@RequiredArgsConstructor
public class ExternalProxyController {
    
    private final ExternalProxyService proxyService;
    
    /**
     * 代理转发请求（POST方式，参数在body中）
     */
    @PostMapping("/{apiId}")
    public RespInfo<Object> proxyPost(
            @PathVariable Long apiId,
            @RequestBody(required = false) Map<String, Object> params) {
        
        Object result = proxyService.proxyRequest(apiId, params);
        return RespInfo.success(result);
    }
    
    /**
     * 代理转发请求（GET方式）
     */
    @GetMapping("/{apiId}")
    public RespInfo<Object> proxyGet(
            @PathVariable Long apiId,
            @RequestParam(required = false) Map<String, Object> params) {
        
        Object result = proxyService.proxyRequest(apiId, params);
        return RespInfo.success(result);
    }
}
```

---

## 7. 前端改造方案

### 7.1 新增外部系统管理页面

**页面路径**：`forge-report-ui/src/views/external/system/index.vue`

**功能**：
- 外部系统列表（CRUD）
- 认证类型选择（下拉）
- 认证配置（根据类型动态显示表单）

**认证配置表单（Bearer Token）**：
```vue
<template>
  <n-card title="认证配置">
    <n-form-item label="Token">
      <n-input v-model:value="formData.authConfig.token" type="password" show-password-on="click" />
    </n-form-item>
    <n-form-item label="Token Header">
      <n-input v-model:value="formData.authConfig.tokenHeader" placeholder="默认: Authorization" />
    </n-form-item>
    <n-form-item label="Token Prefix">
      <n-input v-model:value="formData.authConfig.tokenPrefix" placeholder="默认: Bearer" />
    </n-form-item>
  </n-card>
</template>
```

### 7.2 新增外部接口管理页面

**页面路径**：`forge-report-ui/src/views/external/api/index.vue`

**功能**：
- 外部接口列表（CRUD）
- 所属系统选择（下拉，带搜索）
- 适配类型选择（None / JsonPath / Script）
- 适配配置（根据类型动态显示）

**适配配置表单（JsonPath）**：
```vue
<template>
  <n-card title="数据适配配置">
    <n-form-item label="源数据路径">
      <n-input v-model:value="formData.adapterConfig.sourcePath" placeholder="如: data.list" />
    </n-form-item>
    <n-form-item label="目标数据路径">
      <n-input v-model:value="formData.adapterConfig.targetPath" placeholder="如: data" />
    </n-form-item>
    <n-form-item label="字段映射">
      <n-dynamic-input
        v-model:value="fieldMappings"
        :on-create="() => ({ sourceField: '', targetField: '' })"
      >
        <template #default="{ value }">
          <div style="display: flex; gap: 8px; align-items: center;">
            <n-input v-model:value="value.sourceField" placeholder="源字段" />
            <n-input v-model:value="value.targetField" placeholder="目标字段" />
          </div>
        </template>
      </n-dynamic-input>
    </n-form-item>
  </n-card>
</template>
```

**适配配置表单（Script）**：
```vue
<template>
  <n-card title="脚本适配配置">
    <n-form-item label="转换脚本">
      <n-input
        v-model:value="formData.adapterConfig"
        type="textarea"
        :rows="10"
        placeholder="请输入转换脚本"
      />
    </n-form-item>
    <n-alert type="info" title="提示">
      使用 response 变量访问原始数据，将转换结果赋值给 result 变量。
      <br />
      示例: var result = { code: 0, data: response.list };
    </n-alert>
  </n-card>
</template>
```

### 7.3 改造动态请求配置页面

**改造文件**：`forge-report-ui/src/views/chart/ContentConfigurations/components/ChartData/components/ChartDataRequest/components/RequestTargetConfig/index.vue`

**新增内容**：

```vue
<template>
  <setting-item-box name="地址">
    <!-- 新增：接口来源选择 -->
    <setting-item name="接口来源">
      <n-radio-group v-model:value="targetDataRequest.requestSource">
        <n-radio-button value="internal">内部接口</n-radio-button>
        <n-radio-button value="external">外部接口</n-radio-button>
      </n-radio-group>
    </setting-item>
    
    <!-- 内部接口：保持原有逻辑 -->
    <setting-item name="请求方式 & URL 地址" v-if="targetDataRequest.requestSource === 'internal'">
      <n-input-group>
        <n-select v-model:value="targetDataRequest.requestHttpType" :options="selectTypeOptions" />
        <n-input v-model:value.trim="targetDataRequest.requestUrl" placeholder="请输入地址（去除前置URL）">
          <template #prefix>
            <n-text>{{ requestOriginUrl }}</n-text>
            <n-divider vertical />
          </template>
        </n-input>
      </n-input-group>
    </setting-item>
    
    <!-- 外部接口：新增接口选择器 -->
    <setting-item name="选择接口" v-if="targetDataRequest.requestSource === 'external'">
      <n-select
        v-model:value="targetDataRequest.externalApiId"
        :options="externalApiOptions"
        placeholder="请选择外部接口"
        filterable
        clearable
      />
    </setting-item>
    
    <!-- 外部接口参数配置 -->
    <setting-item name="请求参数" v-if="targetDataRequest.requestSource === 'external' && targetDataRequest.externalApiId">
      <n-dynamic-input
        v-model:value="externalParamList"
        :on-create="() => ({ key: '', value: '' })"
      >
        <template #default="{ value }">
          <div style="display: flex; gap: 8px; align-items: center;">
            <n-input v-model:value="value.key" placeholder="参数名" />
            <n-input v-model:value="value.value" placeholder="参数值" />
          </div>
        </template>
      </n-dynamic-input>
    </setting-item>
  </setting-item-box>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { getExternalApiList } from '@/api/external/api'

const externalApiList = ref<ExternalApiVO[]>([])
const externalParamList = ref<Array<{ key: string; value: string }>>([])

// 外部接口选项
const externalApiOptions = computed(() => {
  return externalApiList.value.map(api => ({
    label: `${api.systemName} - ${api.apiName} (${api.method} ${api.apiPath})`,
    value: api.id,
    disabled: api.status !== '0'
  }))
})

// 加载外部接口列表
onMounted(async () => {
  const res = await getExternalApiList()
  externalApiList.value = res.data || []
})

// 监听参数列表变化，同步到 requestParams
watch(externalParamList, (newVal) => {
  const params: Record<string, string> = {}
  newVal.forEach(item => {
    if (item.key && item.value) {
      params[item.key] = item.value
    }
  })
  targetDataRequest.value.externalRequestParams = params
}, { deep: true })
</script>
```

### 7.4 数据结构调整

**扩展 RequestConfigType**（`chartEditStore.d.ts`）：

```typescript
export interface RequestConfigType {
  requestHttpType: RequestHttpEnum
  requestUrl: string
  requestContentType: RequestContentTypeEnum
  requestDataType: RequestDataTypeEnum
  requestParamsBodyType: RequestBodyEnum
  requestSQLContent: object
  requestParams: RequestParams
  requestInterval: number
  requestIntervalUnit: RequestHttpIntervalEnum
  
  // 新增字段
  requestSource: 'internal' | 'external'  // 接口来源
  externalApiId: number | null            // 外部接口ID
  externalRequestParams: Record<string, string>  // 外部接口参数
}
```

**默认值设置**（`publicConfig.ts`）：

```typescript
requestSource: 'internal',
externalApiId: null,
externalRequestParams: {}
```

### 7.5 请求发送逻辑改造

**改造文件**：`forge-report-ui/src/api/http.ts`

**customizeHttp 函数改造**：

```typescript
export const customizeHttp = (targetParams: RequestConfigType, globalParams: RequestGlobalConfigType) => {
  if (!targetParams || !globalParams) return
  
  const {
    requestDataType,
    requestSource,
    requestUrl,
    requestHttpType,
    requestParams,
    externalApiId,
    externalRequestParams
  } = targetParams
  
  // 静态数据直接返回
  if (requestDataType === RequestDataTypeEnum.STATIC) return
  
  // 内部接口：保持原有逻辑
  if (requestSource === 'internal') {
    if (!requestUrl) return
    
    const url = (new Function("return `" + `${globalParams.requestOriginUrl}${requestUrl}`.trim() + "`"))()
    
    // 处理请求参数（保持原有逻辑）
    let headers: RequestParamsObjType = { ...globalParams.requestParams.Header, ...requestParams.Header }
    let data: RequestParamsObjType | FormData | string = {}
    let params: RequestParamsObjType = { ...requestParams.Params }
    
    // ... 原有的 requestParamsBodyType 处理逻辑 ...
    
    return axiosInstance({
      url,
      method: requestHttpType,
      data,
      params,
      headers
    })
  }
  
  // 外部接口：调用代理转发
  if (requestSource === 'external' && externalApiId) {
    const url = `${globalParams.requestOriginUrl}/external/proxy/${externalApiId}`
    
    return axiosInstance({
      url,
      method: RequestHttpEnum.POST,
      data: externalRequestParams
    })
  }
}
```

---

## 8. API 规范

### 8.1 外部系统管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/external/system/page` | 分页查询外部系统 |
| GET | `/external/system/{id}` | 查询外部系统详情 |
| POST | `/external/system` | 新增外部系统 |
| PUT | `/external/system` | 修改外部系统 |
| DELETE | `/external/system/{id}` | 删除外部系统 |
| GET | `/external/system/list` | 查询外部系统列表（不分页） |

### 8.2 外部接口管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/external/api/page` | 分页查询外部接口 |
| GET | `/external/api/{id}` | 查询外部接口详情 |
| POST | `/external/api` | 新增外部接口 |
| PUT | `/external/api` | 修改外部接口 |
| DELETE | `/external/api/{id}` | 删除外部接口 |
| GET | `/external/api/list` | 查询外部接口列表（不分页，含系统名称） |

### 8.3 代理转发 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/external/proxy/{apiId}` | 代理转发请求（POST方式） |
| GET | `/external/proxy/{apiId}` | 代理转发请求（GET方式） |

---

## 9. 扩展指南

### 9.1 新增认证策略

1. 实现 `AuthStrategy` 接口
2. 添加 `@Component` 注解（自动注册到工厂）
3. 定义 `auth_config` JSON 结构

**示例：Basic Auth 策略**

```java
@Component
public class BasicAuthStrategy implements AuthStrategy {
    
    @Override
    public String getAuthType() {
        return "BasicAuth";
    }
    
    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        String username = config.getString("username");
        String password = config.getString("password");
        String encoded = Base64.getEncoder().encodeToString(
            (username + ":" + password).getBytes()
        );
        requestBuilder.header("Authorization", "Basic " + encoded);
    }
    
    @Override
    public boolean validateConfig(String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        return config.containsKey("username") && config.containsKey("password");
    }
}
```

### 9.2 新增数据适配器

1. 实现 `DataAdapter` 接口
2. 添加 `@Component` 注解（自动注册到工厂）
3. 定义 `adapter_config` 格式

---

## 10. 测试要点

### 10.1 单元测试

| 测试项 | 测试内容 |
|--------|---------|
| BearerTokenAuthStrategy | Token 配置验证、Header 添加正确性 |
| JsonPathAdapter | JSON Path 提取、字段映射、目标结构构建 |
| ScriptAdapter | 脚本执行、异常处理 |
| ExternalProxyService | 正常请求流程、异常情况处理 |

### 10.2 集成测试

| 测试项 | 测试内容 |
|--------|---------|
| 外部系统 CRUD | 创建、查询、更新、删除功能 |
| 外部接口 CRUD | 创建、查询、更新、删除功能 |
| 代理转发 | 内部接口调用、外部接口代理转发 |
| 数据适配 | JSON Path 适配、脚本适配 |

### 10.3 前端测试

| 测试项 | 测试内容 |
|--------|---------|
| 外部系统管理页面 | 表单提交、认证配置动态显示 |
| 外部接口管理页面 | 系统选择、适配配置动态显示 |
| 动态请求配置 | 内部/外部切换、接口选择、参数配置 |

---

## 11. 部署说明

### 11.1 模块集成

**forge-admin-server pom.xml 添加依赖**：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-plugin-external</artifactId>
</dependency>
```

### 11.2 数据库脚本

- 初始化脚本：`forge/forge-admin-server/src/main/resources/sql/forge.sql`
- 新增表：`sys_external_system`、`sys_external_api`

### 11.3 菜单配置

**新增菜单**：
- 外部系统管理：`/external/system`
- 外部接口管理：`/external/api`

---

## 12. 注意事项

### 12.1 安全考虑

1. **脚本执行安全**：ScriptAdapter 使用 Nashorn 执行脚本，需限制脚本权限
2. **Token 存储**：auth_config 中的敏感信息建议加密存储
3. **请求超时**：外部请求默认超时 30 秒，防止长时间阻塞
4. **租户隔离**：外部系统和接口配置按租户隔离

### 12.2 性能考虑

1. **HttpClient 复用**：使用单例 HttpClient，避免频繁创建
2. **缓存策略**：外部接口列表可考虑前端缓存
3. **并发限制**：代理转发接口需考虑并发请求限制

### 12.3 错误处理

1. **外部系统不可用**：返回友好错误提示，记录日志
2. **认证失败**：明确提示认证失败原因
3. **数据适配失败**：返回原始数据或错误提示

---

## 附录：实现顺序

**Phase 1：基础设施**
1. 数据库表创建
2. Entity、Mapper、DTO 创建
3. Service 接口定义

**Phase 2：核心能力**
1. AuthStrategy 接口 + BearerTokenAuthStrategy 实现
2. DataAdapter 接口 + JsonPathAdapter/ScriptAdapter 实现
3. AuthStrategyFactory / DataAdapterFactory 实现

**Phase 3：代理转发**
1. ExternalProxyService 实现
2. ExternalProxyController 实现
3. 单元测试

**Phase 4：管理功能**
1. ExternalSystemService 实现
2. ExternalApiService 实现
3. Controller CRUD 实现
4. 集成测试

**Phase 5：前端实现**
1. 外部系统管理页面
2. 外部接口管理页面
3. 动态请求配置改造
4. API 类型定义

**Phase 6：集成验证**
1. forge-admin-server 集成
2. 全链路测试
3. 文档完善