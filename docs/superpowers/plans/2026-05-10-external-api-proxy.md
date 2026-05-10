# 外部接口代理转发功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现外部接口代理转发功能，支持报表设计器调用外部系统接口，解决跨域和认证问题。

**Architecture:** 新建 forge-plugin-external 插件模块，采用策略模式实现认证、适配器模式实现数据转换，report-server 作为代理转发层，前端改造动态请求配置支持接口选择。

**Tech Stack:** Spring Boot 3.2 + MyBatis-Plus 3.5 + HttpClient (Java 11+) + fastjson 2.0 + Nashorn (JavaScript) + Vue 3 + Naive UI

---

## 文件结构规划

### 后端新建文件

```
forge/forge-framework/forge-plugin-parent/forge-plugin-external/
├── pom.xml
├── src/main/java/com/mdframe/forge/plugin/external/
│   ├── controller/
│   │   ├── ExternalSystemController.java      # 外部系统管理
│   │   ├── ExternalApiController.java         # 外部接口管理
│   │   └── ExternalProxyController.java       # 代理转发入口
│   ├── service/
│   │   ├── ExternalSystemService.java         # 服务接口
│   │   ├── ExternalApiService.java
│   │   ├── ExternalProxyService.java
│   │   └── impl/
│   │       ├── ExternalSystemServiceImpl.java
│   │       ├── ExternalApiServiceImpl.java
│   │       └── ExternalProxyServiceImpl.java
│   ├── strategy/
│   │   ├── AuthStrategy.java                  # 认证策略接口
│   │   ├── AuthStrategyFactory.java
│   │   └── impl/
│   │       ├── NoneAuthStrategy.java
│   │       └── BearerTokenAuthStrategy.java
│   ├── adapter/
│   │   ├── DataAdapter.java                   # 数据适配接口
│   │   ├── DataAdapterFactory.java
│   │   └── impl/
│   │       ├── NoneAdapter.java
│   │       ├── JsonPathAdapter.java
│   │       └── ScriptAdapter.java
│   ├── entity/
│   │   ├── ExternalSystem.java
│   │   └── ExternalApi.java
│   ├── mapper/
│   │   ├── ExternalSystemMapper.java
│   │   ├── ExternalApiMapper.java
│   ├── dto/
│   │   ├── ExternalSystemDTO.java
│   │   ├── ExternalApiDTO.java
│   │   └── ExternalApiVO.java                 # 含系统名称
│   └── enums/
│       ├── AuthTypeEnum.java
│       └── AdapterTypeEnum.java
├── src/main/resources/mapper/
│   ├── ExternalSystemMapper.xml
│   └── ExternalApiMapper.xml
```

### 前端新建文件

```
forge-report-ui/src/
├── api/external/
│   ├── system.ts                              # 外部系统 API
│   └── api.ts                                 # 外部接口 API
├── views/external/
│   ├── system/
│   │   └── index.vue                          # 外部系统管理页面
│   └── api/
│   │   └── index.vue                          # 外部接口管理页面
```

### 前端修改文件

```
forge-report-ui/src/
├── api/http.ts                                # 改造 customizeHttp 函数
├── store/modules/chartEditStore/chartEditStore.d.ts  # 扩展 RequestConfigType
├── views/chart/ContentConfigurations/components/ChartData/components/ChartDataRequest/components/RequestTargetConfig/index.vue  # 改造接口选择
├── packages/public/publicConfig.ts            # 增加默认值
```

### 数据库文件

```
forge/forge-admin-server/src/main/resources/sql/forge.sql  # 添加新表DDL
```

---

## Phase 1: 基础设施搭建

### Task 1.1: 创建数据库表

**Files:**
- Modify: `forge/forge-admin-server/src/main/resources/sql/forge.sql`

- [ ] **Step 1: 添加 sys_external_system 表 DDL**

在 forge.sql 文件末尾添加：

```sql
-- ----------------------------
-- 外部系统配置表
-- ----------------------------
DROP TABLE IF EXISTS sys_external_system;
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

-- ----------------------------
-- 外部接口配置表
-- ----------------------------
DROP TABLE IF EXISTS sys_external_api;
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

- [ ] **Step 2: 执行数据库脚本**

```bash
mysql -u root -p forge < forge/forge-admin-server/src/main/resources/sql/forge.sql
```

- [ ] **Step 3: 验证表创建**

```bash
mysql -u root -p forge -e "SHOW CREATE TABLE sys_external_system;"
mysql -u root -p forge -e "SHOW CREATE TABLE sys_external_api;"
```

---

### Task 1.2: 创建插件模块基础结构

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/pom.xml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.mdframe.forge</groupId>
        <artifactId>forge-plugin-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>forge-plugin-external</artifactId>
    <packaging>jar</packaging>
    <name>Forge Plugin External</name>
    <description>外部接口代理转发插件</description>

    <dependencies>
        <dependency>
            <groupId>com.mdframe.forge</groupId>
            <artifactId>forge-starter-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mdframe.forge</groupId>
            <artifactId>forge-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mdframe.forge</groupId>
            <artifactId>forge-starter-orm</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 在父 pom 中添加模块**

修改 `forge/forge-framework/forge-plugin-parent/pom.xml`，在 `<modules>` 中添加：

```xml
<module>forge-plugin-external</module>
```

- [ ] **Step 3: 创建目录结构**

```bash
cd forge/forge-framework/forge-plugin-parent/forge-plugin-external
mkdir -p src/main/java/com/mdframe/forge/plugin/external/{controller,service/impl,strategy/impl,adapter/impl,entity,mapper,dto,enums}
mkdir -p src/main/resources/mapper
```

---

### Task 1.3: 创建实体类

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/entity/ExternalSystem.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/entity/ExternalApi.java`

- [ ] **Step 1: 创建 ExternalSystem.java**

```java
package com.mdframe.forge.plugin.external.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_external_system")
public class ExternalSystem extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String systemName;

    private String systemCode;

    private String baseUrl;

    private String authType;

    private String authConfig;

    private String description;

    private String status;
}
```

- [ ] **Step 2: 创建 ExternalApi.java**

```java
package com.mdframe.forge.plugin.external.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_external_api")
public class ExternalApi extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long systemId;

    private String apiName;

    private String apiCode;

    private String apiPath;

    private String method;

    private String description;

    private String adapterType;

    private String adapterConfig;

    private String status;
}
```

- [ ] **Step 3: 编译验证**

```bash
cd forge && mvn clean compile -pl forge-framework/forge-plugin-parent/forge-plugin-external -am
```

---

### Task 1.4: 创建枚举类

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/enums/AuthTypeEnum.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/enums/AdapterTypeEnum.java`

- [ ] **Step 1: 创建 AuthTypeEnum.java**

```java
package com.mdframe.forge.plugin.external.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthTypeEnum {

    NONE("None", "无认证"),
    BEARER_TOKEN("BearerToken", "Bearer Token");

    private final String code;
    private final String desc;

    public static AuthTypeEnum getByCode(String code) {
        for (AuthTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return NONE;
    }
}
```

- [ ] **Step 2: 创建 AdapterTypeEnum.java**

```java
package com.mdframe.forge.plugin.external.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdapterTypeEnum {

    NONE("None", "无适配"),
    JSON_PATH("JsonPath", "JSON Path 映射"),
    SCRIPT("Script", "脚本转换");

    private final String code;
    private final String desc;

    public static AdapterTypeEnum getByCode(String code) {
        for (AdapterTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return NONE;
    }
}
```

---

### Task 1.5: 创建 Mapper 接口

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/mapper/ExternalSystemMapper.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/mapper/ExternalApiMapper.java`

- [ ] **Step 1: 创建 ExternalSystemMapper.java**

```java
package com.mdframe.forge.plugin.external.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExternalSystemMapper extends BaseMapper<ExternalSystem> {
}
```

- [ ] **Step 2: 创建 ExternalApiMapper.java**

```java
package com.mdframe.forge.plugin.external.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.dto.ExternalApiVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExternalApiMapper extends BaseMapper<ExternalApi> {

    List<ExternalApiVO> selectApiVOList(@Param("tenantId") Long tenantId);
}
```

---

### Task 1.6: 创建 Mapper XML

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/resources/mapper/ExternalSystemMapper.xml`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/resources/mapper/ExternalApiMapper.xml`

- [ ] **Step 1: 创建 ExternalSystemMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mdframe.forge.plugin.external.mapper.ExternalSystemMapper">

</mapper>
```

- [ ] **Step 2: 创建 ExternalApiMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mdframe.forge.plugin.external.mapper.ExternalApiMapper">

    <resultMap id="ApiVOResultMap" type="com.mdframe.forge.plugin.external.dto.ExternalApiVO">
        <id property="id" column="id"/>
        <result property="tenantId" column="tenant_id"/>
        <result property="systemId" column="system_id"/>
        <result property="systemName" column="system_name"/>
        <result property="apiName" column="api_name"/>
        <result property="apiCode" column="api_code"/>
        <result property="apiPath" column="api_path"/>
        <result property="method" column="method"/>
        <result property="description" column="description"/>
        <result property="adapterType" column="adapter_type"/>
        <result property="adapterConfig" column="adapter_config"/>
        <result property="status" column="status"/>
        <result property="createTime" column="create_time"/>
        <result property="updateTime" column="update_time"/>
    </resultMap>

    <select id="selectApiVOList" resultMap="ApiVOResultMap">
        SELECT
            a.id,
            a.tenant_id,
            a.system_id,
            s.system_name,
            a.api_name,
            a.api_code,
            a.api_path,
            a.method,
            a.description,
            a.adapter_type,
            a.adapter_config,
            a.status,
            a.create_time,
            a.update_time
        FROM sys_external_api a
        LEFT JOIN sys_external_system s ON a.system_id = s.id AND a.tenant_id = s.tenant_id
        WHERE a.tenant_id = #{tenantId}
        ORDER BY a.create_time DESC
    </select>

</mapper>
```

---

### Task 1.7: 创建 DTO 类

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/dto/ExternalSystemDTO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/dto/ExternalApiDTO.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/dto/ExternalApiVO.java`

- [ ] **Step 1: 创建 ExternalSystemDTO.java**

```java
package com.mdframe.forge.plugin.external.dto;

import lombok.Data;

@Data
public class ExternalSystemDTO {

    private Long id;

    private String systemName;

    private String systemCode;

    private String baseUrl;

    private String authType;

    private String authConfig;

    private String description;

    private String status;
}
```

- [ ] **Step 2: 创建 ExternalApiDTO.java**

```java
package com.mdframe.forge.plugin.external.dto;

import lombok.Data;

@Data
public class ExternalApiDTO {

    private Long id;

    private Long systemId;

    private String apiName;

    private String apiCode;

    private String apiPath;

    private String method;

    private String description;

    private String adapterType;

    private String adapterConfig;

    private String status;
}
```

- [ ] **Step 3: 创建 ExternalApiVO.java**

```java
package com.mdframe.forge.plugin.external.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExternalApiVO {

    private Long id;

    private Long tenantId;

    private Long systemId;

    private String systemName;

    private String apiName;

    private String apiCode;

    private String apiPath;

    private String method;

    private String description;

    private String adapterType;

    private String adapterConfig;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

---

### Task 1.8: 创建 Service 接口

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/service/ExternalSystemService.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/service/ExternalApiService.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/service/ExternalProxyService.java`

- [ ] **Step 1: 创建 ExternalSystemService.java**

```java
package com.mdframe.forge.plugin.external.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;

import java.util.List;

public interface ExternalSystemService extends IService<ExternalSystem> {

    Page<ExternalSystem> page(Integer pageNum, Integer pageSize, String systemName);

    List<ExternalSystem> listAll();
}
```

- [ ] **Step 2: 创建 ExternalApiService.java**

```java
package com.mdframe.forge.plugin.external.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.external.dto.ExternalApiVO;
import com.mdframe.forge.plugin.external.entity.ExternalApi;

import java.util.List;

public interface ExternalApiService extends IService<ExternalApi> {

    Page<ExternalApi> page(Integer pageNum, Integer pageSize, Long systemId, String apiName);

    List<ExternalApiVO> listApiVO();
}
```

- [ ] **Step 3: 创建 ExternalProxyService.java**

```java
package com.mdframe.forge.plugin.external.service;

import java.util.Map;

public interface ExternalProxyService {

    Object proxyRequest(Long apiId, Map<String, Object> params);
}
```

---

### Task 1.9: 创建 Service 实现类（基础 CRUD）

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/service/impl/ExternalSystemServiceImpl.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/service/impl/ExternalApiServiceImpl.java`

- [ ] **Step 1: 创建 ExternalSystemServiceImpl.java**

```java
package com.mdframe.forge.plugin.external.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.mapper.ExternalSystemMapper;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalSystemServiceImpl extends ServiceImpl<ExternalSystemMapper, ExternalSystem>
        implements ExternalSystemService {

    @Override
    public Page<ExternalSystem> page(Integer pageNum, Integer pageSize, String systemName) {
        LambdaQueryWrapper<ExternalSystem> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(systemName != null, ExternalSystem::getSystemName, systemName)
               .orderByDesc(ExternalSystem::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<ExternalSystem> listAll() {
        LambdaQueryWrapper<ExternalSystem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExternalSystem::getStatus, "0")
               .orderByDesc(ExternalSystem::getCreateTime);
        return list(wrapper);
    }
}
```

- [ ] **Step 2: 创建 ExternalApiServiceImpl.java**

```java
package com.mdframe.forge.plugin.external.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.external.dto.ExternalApiVO;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.mapper.ExternalApiMapper;
import com.mdframe.forge.plugin.external.service.ExternalApiService;
import com.mdframe.forge.starter.core.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalApiServiceImpl extends ServiceImpl<ExternalApiMapper, ExternalApi>
        implements ExternalApiService {

    private final ExternalApiMapper apiMapper;

    @Override
    public Page<ExternalApi> page(Integer pageNum, Integer pageSize, Long systemId, String apiName) {
        LambdaQueryWrapper<ExternalApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(systemId != null, ExternalApi::getSystemId, systemId)
               .like(apiName != null, ExternalApi::getApiName, apiName)
               .orderByDesc(ExternalApi::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<ExternalApiVO> listApiVO() {
        Long tenantId = SecurityUtils.getTenantId();
        return apiMapper.selectApiVOList(tenantId);
    }
}
```

---

### Task 1.10: 创建 Controller（基础 CRUD）

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/controller/ExternalSystemController.java`
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/controller/ExternalApiController.java`

- [ ] **Step 1: 创建 ExternalSystemController.java**

```java
package com.mdframe.forge.plugin.external.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/external/system")
@RequiredArgsConstructor
public class ExternalSystemController {

    private final ExternalSystemService systemService;

    @GetMapping("/page")
    public RespInfo<Page<ExternalSystem>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String systemName) {
        return RespInfo.success(systemService.page(pageNum, pageSize, systemName));
    }

    @GetMapping("/{id}")
    public RespInfo<ExternalSystem> getById(@PathVariable Long id) {
        return RespInfo.success(systemService.getById(id));
    }

    @PostMapping
    public RespInfo<Void> add(@RequestBody ExternalSystem system) {
        systemService.save(system);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> edit(@RequestBody ExternalSystem system) {
        systemService.updateById(system);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> remove(@PathVariable Long id) {
        systemService.removeById(id);
        return RespInfo.success();
    }

    @GetMapping("/list")
    public RespInfo<List<ExternalSystem>> list() {
        return RespInfo.success(systemService.listAll());
    }
}
```

- [ ] **Step 2: 创建 ExternalApiController.java**

```java
package com.mdframe.forge.plugin.external.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.external.dto.ExternalApiVO;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.service.ExternalApiService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/external/api")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService apiService;

    @GetMapping("/page")
    public RespInfo<Page<ExternalApi>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long systemId,
            @RequestParam(required = false) String apiName) {
        return RespInfo.success(apiService.page(pageNum, pageSize, systemId, apiName));
    }

    @GetMapping("/{id}")
    public RespInfo<ExternalApi> getById(@PathVariable Long id) {
        return RespInfo.success(apiService.getById(id));
    }

    @PostMapping
    public RespInfo<Void> add(@RequestBody ExternalApi api) {
        apiService.save(api);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> edit(@RequestBody ExternalApi api) {
        apiService.updateById(api);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> remove(@PathVariable Long id) {
        apiService.removeById(id);
        return RespInfo.success();
    }

    @GetMapping("/list")
    public RespInfo<List<ExternalApiVO>> list() {
        return RespInfo.success(apiService.listApiVO());
    }
}
```

---

### Task 1.11: 编译验证 Phase 1

- [ ] **Step 1: 编译插件模块**

```bash
cd forge && mvn clean compile -pl forge-framework/forge-plugin-parent/forge-plugin-external -am
```

- [ ] **Step 2: 添加插件依赖到 admin-server**

修改 `forge/forge-admin-server/pom.xml`，添加：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-plugin-external</artifactId>
</dependency>
```

- [ ] **Step 3: 全量编译**

```bash
cd forge && mvn clean install -DskipTests
```

---

## Phase 2: 认证策略模式实现

### Task 2.1: 创建认证策略接口

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/strategy/AuthStrategy.java`

- [ ] **Step 1: 创建 AuthStrategy.java**

```java
package com.mdframe.forge.plugin.external.strategy;

import java.net.http.HttpRequest;

public interface AuthStrategy {

    String getAuthType();

    void applyAuth(HttpRequest.Builder requestBuilder, String authConfig);

    boolean validateConfig(String authConfig);
}
```

---

### Task 2.2: 创建无认证策略

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/strategy/impl/NoneAuthStrategy.java`

- [ ] **Step 1: 创建 NoneAuthStrategy.java**

```java
package com.mdframe.forge.plugin.external.strategy.impl;

import com.mdframe.forge.plugin.external.strategy.AuthStrategy;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;

@Component
public class NoneAuthStrategy implements AuthStrategy {

    @Override
    public String getAuthType() {
        return "None";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
    }

    @Override
    public boolean validateConfig(String authConfig) {
        return true;
    }
}
```

---

### Task 2.3: 创建 Bearer Token 认证策略

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/strategy/impl/BearerTokenAuthStrategy.java`

- [ ] **Step 1: 创建 BearerTokenAuthStrategy.java**

```java
package com.mdframe.forge.plugin.external.strategy.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.AuthStrategy;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;

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

        if (header == null || header.isEmpty()) {
            header = "Authorization";
        }
        if (prefix == null || prefix.isEmpty()) {
            prefix = "Bearer";
        }

        requestBuilder.header(header, prefix + " " + token);
    }

    @Override
    public boolean validateConfig(String authConfig) {
        if (authConfig == null || authConfig.isEmpty()) {
            return false;
        }
        try {
            JSONObject config = JSON.parseObject(authConfig);
            return config.containsKey("token") && config.getString("token") != null;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

### Task 2.4: 创建认证策略工厂

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/strategy/AuthStrategyFactory.java`

- [ ] **Step 1: 创建 AuthStrategyFactory.java**

```java
package com.mdframe.forge.plugin.external.strategy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

### Task 2.5: 编译验证 Phase 2

- [ ] **Step 1: 编译**

```bash
cd forge && mvn clean compile -pl forge-framework/forge-plugin-parent/forge-plugin-external -am
```

---

## Phase 3: 数据适配器模式实现

### Task 3.1: 创建数据适配接口

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/adapter/DataAdapter.java`

- [ ] **Step 1: 创建 DataAdapter.java**

```java
package com.mdframe.forge.plugin.external.adapter;

public interface DataAdapter {

    String getAdapterType();

    Object transform(Object originalData, String adapterConfig);

    boolean validateConfig(String adapterConfig);
}
```

---

### Task 3.2: 创建无适配器

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/adapter/impl/NoneAdapter.java`

- [ ] **Step 1: 创建 NoneAdapter.java**

```java
package com.mdframe.forge.plugin.external.adapter.impl;

import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import org.springframework.stereotype.Component;

@Component
public class NoneAdapter implements DataAdapter {

    @Override
    public String getAdapterType() {
        return "None";
    }

    @Override
    public Object transform(Object originalData, String adapterConfig) {
        return originalData;
    }

    @Override
    public boolean validateConfig(String adapterConfig) {
        return true;
    }
}
```

---

### Task 3.3: 创建 JSON Path 适配器

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/adapter/impl/JsonPathAdapter.java`

- [ ] **Step 1: 创建 JsonPathAdapter.java**

```java
package com.mdframe.forge.plugin.external.adapter.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import org.springframework.stereotype.Component;

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

        String sourcePath = config.getString("sourcePath");
        Object extractedData = extractByPath(sourceData, sourcePath);

        JSONObject fieldMapping = config.getJSONObject("fieldMapping");
        Object transformedData = mapFields(extractedData, fieldMapping);

        String targetPath = config.getString("targetPath");
        return buildTargetStructure(transformedData, targetPath);
    }

    private Object extractByPath(JSONObject source, String path) {
        if (path == null || path.isEmpty()) {
            return source;
        }
        String[] parts = path.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            } else if (current instanceof JSONArray) {
                JSONArray arr = (JSONArray) current;
                current = arr;
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
                } else {
                    result.add(item);
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
            JSONObject container = new JSONObject();
            container.put(targetPath, data);
            result.put("data", container);
        }
        return result;
    }

    @Override
    public boolean validateConfig(String adapterConfig) {
        if (adapterConfig == null || adapterConfig.isEmpty()) {
            return false;
        }
        try {
            JSONObject config = JSON.parseObject(adapterConfig);
            return config != null;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

### Task 3.4: 创建脚本适配器

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/adapter/impl/ScriptAdapter.java`

- [ ] **Step 1: 创建 ScriptAdapter.java**

```java
package com.mdframe.forge.plugin.external.adapter.impl;

import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
        engine.put("response", originalData);

        try {
            engine.eval(adapterConfig);
            return engine.get("result");
        } catch (ScriptException e) {
            throw new RuntimeException("脚本执行失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validateConfig(String adapterConfig) {
        if (adapterConfig == null || adapterConfig.isEmpty()) {
            return false;
        }
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

---

### Task 3.5: 创建适配器工厂

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/adapter/DataAdapterFactory.java`

- [ ] **Step 1: 创建 DataAdapterFactory.java**

```java
package com.mdframe.forge.plugin.external.adapter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

### Task 3.6: 编译验证 Phase 3

- [ ] **Step 1: 编译**

```bash
cd forge && mvn clean compile -pl forge-framework/forge-plugin-parent/forge-plugin-external -am
```

---

## Phase 4: 代理转发服务实现

### Task 4.1: 创建代理转发服务实现

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/service/impl/ExternalProxyServiceImpl.java`

- [ ] **Step 1: 创建 ExternalProxyServiceImpl.java**

```java
package com.mdframe.forge.plugin.external.service.impl;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import com.mdframe.forge.plugin.external.adapter.DataAdapterFactory;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.service.ExternalApiService;
import com.mdframe.forge.plugin.external.service.ExternalProxyService;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.plugin.external.strategy.AuthStrategy;
import com.mdframe.forge.plugin.external.strategy.AuthStrategyFactory;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalProxyServiceImpl implements ExternalProxyService {

    private final ExternalApiService apiService;
    private final ExternalSystemService systemService;
    private final AuthStrategyFactory authFactory;
    private final DataAdapterFactory adapterFactory;

    @Override
    public Object proxyRequest(Long apiId, Map<String, Object> params) {
        ExternalApi api = apiService.getById(apiId);
        if (api == null || !"0".equals(api.getStatus())) {
            throw new BusinessException("接口不存在或已停用");
        }

        ExternalSystem system = systemService.getById(api.getSystemId());
        if (system == null || !"0".equals(system.getStatus())) {
            throw new BusinessException("系统不存在或已停用");
        }

        String fullUrl = buildFullUrl(system.getBaseUrl(), api.getApiPath());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(30));

        AuthStrategy authStrategy = authFactory.getStrategy(system.getAuthType());
        authStrategy.applyAuth(requestBuilder, system.getAuthConfig());

        applyRequestMethod(requestBuilder, api.getMethod(), params);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = sendRequest(client, requestBuilder.build());

        Object originalData = JSON.parse(response.body());

        if (api.getAdapterType() != null && !api.getAdapterType().isEmpty()) {
            DataAdapter adapter = adapterFactory.getAdapter(api.getAdapterType());
            return adapter.transform(originalData, api.getAdapterConfig());
        }

        return originalData;
    }

    private String buildFullUrl(String baseUrl, String apiPath) {
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

---

### Task 4.2: 创建代理转发 Controller

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-external/src/main/java/com/mdframe/forge/plugin/external/controller/ExternalProxyController.java`

- [ ] **Step 1: 创建 ExternalProxyController.java**

```java
package com.mdframe.forge.plugin.external.controller;

import com.mdframe.forge.plugin.external.service.ExternalProxyService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/external/proxy")
@RequiredArgsConstructor
public class ExternalProxyController {

    private final ExternalProxyService proxyService;

    @PostMapping("/{apiId}")
    public RespInfo<Object> proxyPost(
            @PathVariable Long apiId,
            @RequestBody(required = false) Map<String, Object> params) {
        Object result = proxyService.proxyRequest(apiId, params);
        return RespInfo.success(result);
    }

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

### Task 4.3: 编译验证 Phase 4

- [ ] **Step 1: 编译**

```bash
cd forge && mvn clean compile -pl forge-framework/forge-plugin-parent/forge-plugin-external -am
```

---

## Phase 5: 前端实现

### Task 5.1: 创建前端 API 接口文件

**Files:**
- Create: `forge-report-ui/src/api/external/system.ts`
- Create: `forge-report-ui/src/api/external/api.ts`

- [ ] **Step 1: 创建 system.ts**

```typescript
import { request } from '@/api/http'

export interface ExternalSystem {
  id: number
  systemName: string
  systemCode: string
  baseUrl: string
  authType: string
  authConfig: string
  description: string
  status: string
  createTime?: string
  updateTime?: string
}

export function getSystemPage(params: { pageNum: number; pageSize: number; systemName?: string }) {
  return request.get('/external/system/page', { params })
}

export function getSystemById(id: number) {
  return request.get(`/external/system/${id}`)
}

export function addSystem(data: ExternalSystem) {
  return request.post('/external/system', data)
}

export function editSystem(data: ExternalSystem) {
  return request.put('/external/system', data)
}

export function removeSystem(id: number) {
  return request.delete(`/external/system/${id}`)
}

export function getSystemList() {
  return request.get('/external/system/list')
}
```

- [ ] **Step 2: 创建 api.ts**

```typescript
import { request } from '@/api/http'

export interface ExternalApi {
  id: number
  systemId: number
  apiName: string
  apiCode: string
  apiPath: string
  method: string
  description: string
  adapterType: string
  adapterConfig: string
  status: string
  createTime?: string
  updateTime?: string
}

export interface ExternalApiVO extends ExternalApi {
  systemName: string
}

export function getApiPage(params: { pageNum: number; pageSize: number; systemId?: number; apiName?: string }) {
  return request.get('/external/api/page', { params })
}

export function getApiById(id: number) {
  return request.get(`/external/api/${id}`)
}

export function addApi(data: ExternalApi) {
  return request.post('/external/api', data)
}

export function editApi(data: ExternalApi) {
  return request.put('/external/api', data)
}

export function removeApi(id: number) {
  return request.delete(`/external/api/${id}`)
}

export function getApiVOList() {
  return request.get('/external/api/list')
}
```

---

### Task 5.2: 创建外部系统管理页面

**Files:**
- Create: `forge-report-ui/src/views/external/system/index.vue`

- [ ] **Step 1: 创建 index.vue（简化版 CRUD 页面）**

```vue
<template>
  <div class="go-external-system">
    <n-card title="外部系统管理">
      <n-space vertical>
        <n-space justify="space-between">
          <n-space>
            <n-input v-model:value="searchName" placeholder="系统名称" clearable />
            <n-button type="primary" @click="handleSearch">查询</n-button>
          </n-space>
          <n-button type="primary" @click="handleAdd">新增</n-button>
        </n-space>

        <n-data-table :columns="columns" :data="tableData" :loading="loading" />

        <n-pagination
          v-model:page="pageNum"
          v-model:page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
        />
      </n-space>
    </n-card>

    <n-modal v-model:show="modalShow" preset="card" title="外部系统配置" style="width: 600px">
      <n-form ref="formRef" :model="formData" label-placement="left" label-width="100">
        <n-form-item label="系统名称" path="systemName">
          <n-input v-model:value="formData.systemName" />
        </n-form-item>
        <n-form-item label="系统编码" path="systemCode">
          <n-input v-model:value="formData.systemCode" />
        </n-form-item>
        <n-form-item label="基础URL" path="baseUrl">
          <n-input v-model:value="formData.baseUrl" placeholder="http://127.0.0.1:8080" />
        </n-form-item>
        <n-form-item label="认证类型" path="authType">
          <n-select v-model:value="formData.authType" :options="authTypeOptions" />
        </n-form-item>
        <n-form-item v-if="formData.authType === 'BearerToken'" label="Token">
          <n-input v-model:value="authConfig.token" type="password" show-password-on="click" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="formData.status">
            <n-radio-button value="0">正常</n-radio-button>
            <n-radio-button value="1">停用</n-radio-button>
          </n-radio-group>
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="modalShow = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSystemPage, addSystem, editSystem, removeSystem, ExternalSystem } from '@/api/external/system'
import { NButton, NSpace, NTag } from 'naive-ui'

const searchName = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<ExternalSystem[]>([])
const loading = ref(false)
const modalShow = ref(false)
const isEdit = ref(false)

const formData = reactive<ExternalSystem>({
  id: 0,
  systemName: '',
  systemCode: '',
  baseUrl: '',
  authType: 'None',
  authConfig: '',
  description: '',
  status: '0'
})

const authConfig = reactive({
  token: '',
  tokenHeader: 'Authorization',
  tokenPrefix: 'Bearer'
})

const authTypeOptions = [
  { label: '无认证', value: 'None' },
  { label: 'Bearer Token', value: 'BearerToken' }
]

const columns = [
  { title: '系统名称', key: 'systemName' },
  { title: '系统编码', key: 'systemCode' },
  { title: '基础URL', key: 'baseUrl' },
  { title: '认证类型', key: 'authType' },
  {
    title: '状态',
    key: 'status',
    render: (row: ExternalSystem) => {
      return row.status === '0' ? <NTag type="success">正常</NTag> : <NTag type="error">停用</NTag>
    }
  },
  {
    title: '操作',
    key: 'actions',
    render: (row: ExternalSystem) => {
      return (
        <NSpace>
          <NButton size="small" onClick={() => handleEdit(row)}>编辑</NButton>
          <NButton size="small" type="error" onClick={() => handleDelete(row.id)}>删除</NButton>
        </NSpace>
      )
    }
  }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getSystemPage({ pageNum: pageNum.value, pageSize: pageSize.value, systemName: searchName.value })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, { id: 0, systemName: '', systemCode: '', baseUrl: '', authType: 'None', authConfig: '', description: '', status: '0' })
  Object.assign(authConfig, { token: '', tokenHeader: 'Authorization', tokenPrefix: 'Bearer' })
  modalShow.value = true
}

const handleEdit = (row: ExternalSystem) => {
  isEdit.value = true
  Object.assign(formData, row)
  if (row.authType === 'BearerToken' && row.authConfig) {
    const config = JSON.parse(row.authConfig)
    Object.assign(authConfig, config)
  }
  modalShow.value = true
}

const handleDelete = async (id: number) => {
  await removeSystem(id)
  fetchData()
}

const handleSubmit = async () => {
  if (formData.authType === 'BearerToken') {
    formData.authConfig = JSON.stringify(authConfig)
  } else {
    formData.authConfig = ''
  }

  if (isEdit.value) {
    await editSystem(formData)
  } else {
    await addSystem(formData)
  }
  modalShow.value = false
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.go-external-system {
  padding: 20px;
}
</style>
```

---

### Task 5.3: 创建外部接口管理页面

**Files:**
- Create: `forge-report-ui/src/views/external/api/index.vue`

- [ ] **Step 1: 创建 index.vue**

```vue
<template>
  <div class="go-external-api">
    <n-card title="外部接口管理">
      <n-space vertical>
        <n-space justify="space-between">
          <n-space>
            <n-select v-model:value="searchSystemId" :options="systemOptions" placeholder="所属系统" clearable />
            <n-input v-model:value="searchApiName" placeholder="接口名称" clearable />
            <n-button type="primary" @click="handleSearch">查询</n-button>
          </n-space>
          <n-button type="primary" @click="handleAdd">新增</n-button>
        </n-space>

        <n-data-table :columns="columns" :data="tableData" :loading="loading" />

        <n-pagination
          v-model:page="pageNum"
          v-model:page-size="pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
        />
      </n-space>
    </n-card>

    <n-modal v-model:show="modalShow" preset="card" title="外部接口配置" style="width: 800px">
      <n-form ref="formRef" :model="formData" label-placement="left" label-width="100">
        <n-form-item label="所属系统" path="systemId">
          <n-select v-model:value="formData.systemId" :options="systemOptions" />
        </n-form-item>
        <n-form-item label="接口名称" path="apiName">
          <n-input v-model:value="formData.apiName" />
        </n-form-item>
        <n-form-item label="接口编码" path="apiCode">
          <n-input v-model:value="formData.apiCode" />
        </n-form-item>
        <n-form-item label="接口路径" path="apiPath">
          <n-input v-model:value="formData.apiPath" placeholder="/api/data" />
        </n-form-item>
        <n-form-item label="请求方法" path="method">
          <n-select v-model:value="formData.method" :options="methodOptions" />
        </n-form-item>
        <n-form-item label="适配类型" path="adapterType">
          <n-select v-model:value="formData.adapterType" :options="adapterTypeOptions" />
        </n-form-item>

        <n-card v-if="formData.adapterType === 'JsonPath'" title="JSON Path 配置">
          <n-form-item label="源数据路径">
            <n-input v-model:value="adapterConfig.sourcePath" placeholder="data.list" />
          </n-form-item>
          <n-form-item label="目标数据路径">
            <n-input v-model:value="adapterConfig.targetPath" placeholder="data" />
          </n-form-item>
          <n-form-item label="字段映射">
            <n-dynamic-input v-model:value="fieldMappings" :on-create="() => ({ sourceField: '', targetField: '' })">
              <template #default="{ value }">
                <n-input v-model:value="value.sourceField" placeholder="源字段" style="width: 45%" />
                <n-input v-model:value="value.targetField" placeholder="目标字段" style="width: 45%" />
              </template>
            </n-dynamic-input>
          </n-form-item>
        </n-card>

        <n-card v-if="formData.adapterType === 'Script'" title="脚本配置">
          <n-input
            v-model:value="formData.adapterConfig"
            type="textarea"
            :rows="10"
            placeholder="var result = { code: 0, data: response.list };"
          />
          <n-alert type="info" style="margin-top: 10px">
            使用 response 变量访问原始数据，将转换结果赋值给 result 变量
          </n-alert>
        </n-card>

        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="formData.status">
            <n-radio-button value="0">正常</n-radio-button>
            <n-radio-button value="1">停用</n-radio-button>
          </n-radio-group>
        </n-form-item>
      </n-form>
      <template #action>
        <n-space justify="end">
          <n-button @click="modalShow = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getApiPage, addApi, editApi, removeApi, ExternalApi, getApiVOList } from '@/api/external/api'
import { getSystemList, ExternalSystem } from '@/api/external/system'
import { NButton, NSpace, NTag } from 'naive-ui'

const searchSystemId = ref<number | null>(null)
const searchApiName = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<ExternalApi[]>([])
const systemList = ref<ExternalSystem[]>([])
const loading = ref(false)
const modalShow = ref(false)
const isEdit = ref(false)

const formData = reactive<ExternalApi>({
  id: 0,
  systemId: 0,
  apiName: '',
  apiCode: '',
  apiPath: '',
  method: 'GET',
  description: '',
  adapterType: 'None',
  adapterConfig: '',
  status: '0'
})

const adapterConfig = reactive({
  sourcePath: '',
  targetPath: '',
  fieldMapping: {}
})

const fieldMappings = ref<Array<{ sourceField: string; targetField: string }>>([])

const systemOptions = computed(() => {
  return systemList.value.map(s => ({ label: s.systemName, value: s.id }))
})

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

const adapterTypeOptions = [
  { label: '无适配', value: 'None' },
  { label: 'JSON Path', value: 'JsonPath' },
  { label: '脚本转换', value: 'Script' }
]

const columns = [
  { title: '接口名称', key: 'apiName' },
  { title: '接口编码', key: 'apiCode' },
  { title: '接口路径', key: 'apiPath' },
  { title: '请求方法', key: 'method' },
  { title: '适配类型', key: 'adapterType' },
  {
    title: '状态',
    key: 'status',
    render: (row: ExternalApi) => {
      return row.status === '0' ? <NTag type="success">正常</NTag> : <NTag type="error">停用</NTag>
    }
  },
  {
    title: '操作',
    key: 'actions',
    render: (row: ExternalApi) => {
      return (
        <NSpace>
          <NButton size="small" onClick={() => handleEdit(row)}>编辑</NButton>
          <NButton size="small" type="error" onClick={() => handleDelete(row.id)}>删除</NButton>
        </NSpace>
      )
    }
  }
]

const fetchSystemList = async () => {
  const res = await getSystemList()
  systemList.value = res.data || []
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getApiPage({ pageNum: pageNum.value, pageSize: pageSize.value, systemId: searchSystemId.value, apiName: searchApiName.value })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formData, { id: 0, systemId: 0, apiName: '', apiCode: '', apiPath: '', method: 'GET', description: '', adapterType: 'None', adapterConfig: '', status: '0' })
  Object.assign(adapterConfig, { sourcePath: '', targetPath: '', fieldMapping: {} })
  fieldMappings.value = []
  modalShow.value = true
}

const handleEdit = (row: ExternalApi) => {
  isEdit.value = true
  Object.assign(formData, row)
  if (row.adapterType === 'JsonPath' && row.adapterConfig) {
    const config = JSON.parse(row.adapterConfig)
    adapterConfig.sourcePath = config.sourcePath || ''
    adapterConfig.targetPath = config.targetPath || ''
    if (config.fieldMapping) {
      fieldMappings.value = Object.entries(config.fieldMapping).map(([target, source]) => ({
        sourceField: source as string,
        targetField: target
      }))
    }
  }
  modalShow.value = true
}

const handleDelete = async (id: number) => {
  await removeApi(id)
  fetchData()
}

const handleSubmit = async () => {
  if (formData.adapterType === 'JsonPath') {
    const mapping: Record<string, string> = {}
    fieldMappings.value.forEach(item => {
      if (item.sourceField && item.targetField) {
        mapping[item.targetField] = item.sourceField
      }
    })
    formData.adapterConfig = JSON.stringify({
      sourcePath: adapterConfig.sourcePath,
      targetPath: adapterConfig.targetPath,
      fieldMapping: mapping
    })
  }

  if (isEdit.value) {
    await editApi(formData)
  } else {
    await addApi(formData)
  }
  modalShow.value = false
  fetchData()
}

onMounted(() => {
  fetchSystemList()
  fetchData()
})
</script>

<style lang="scss" scoped>
.go-external-api {
  padding: 20px;
}
</style>
```

---

### Task 5.4: 扩展前端数据结构

**Files:**
- Modify: `forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts`

- [ ] **Step 1: 扩展 RequestConfigType**

在 RequestConfigType 接口中添加：

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
  
  requestSource: 'internal' | 'external'
  externalApiId: number | null
  externalRequestParams: Record<string, string>
}
```

---

### Task 5.5: 设置默认值

**Files:**
- Modify: `forge-report-ui/src/packages/public/publicConfig.ts`

- [ ] **Step 1: 添加新字段默认值**

找到 request 相关配置，添加：

```typescript
requestSource: 'internal',
externalApiId: null,
externalRequestParams: {}
```

---

### Task 5.6: 改造请求配置页面

**Files:**
- Modify: `forge-report-ui/src/views/chart/ContentConfigurations/components/ChartData/components/ChartDataRequest/components/RequestTargetConfig/index.vue`

- [ ] **Step 1: 修改接口来源选择部分**

替换现有的地址配置部分为：

```vue
<template>
  <setting-item-box name="地址">
    <setting-item name="接口来源">
      <n-radio-group v-model:value="targetDataRequest.requestSource">
        <n-radio-button value="internal">内部接口</n-radio-button>
        <n-radio-button value="external">外部接口</n-radio-button>
      </n-radio-group>
    </setting-item>
    
    <setting-item name="请求方式 & URL 地址" v-if="targetDataRequest.requestSource === 'internal'">
      <n-input-group>
        <n-select class="select-type-options" v-model:value="targetDataRequest.requestHttpType" :options="selectTypeOptions" />
        <n-input v-model:value.trim="targetDataRequest.requestUrl" :min="1" placeholder="请输入地址（去除前置URL）">
          <template #prefix>
            <n-text>{{ requestOriginUrl }}</n-text>
            <n-divider vertical />
          </template>
        </n-input>
      </n-input-group>
    </setting-item>
    
    <setting-item name="选择接口" v-if="targetDataRequest.requestSource === 'external'">
      <n-select
        v-model:value="targetDataRequest.externalApiId"
        :options="externalApiOptions"
        placeholder="请选择外部接口"
        filterable
        clearable
      />
    </setting-item>
    
    <setting-item name="请求参数" v-if="targetDataRequest.requestSource === 'external' && targetDataRequest.externalApiId">
      <n-dynamic-input
        v-model:value="externalParamList"
        :on-create="() => ({ key: '', value: '' })"
      >
        <template #default="{ value }">
          <div style="display: flex; gap: 8px; width: 100%">
            <n-input v-model:value="value.key" placeholder="参数名" />
            <n-input v-model:value="value.value" placeholder="参数值" />
          </div>
        </template>
      </n-dynamic-input>
    </setting-item>
    
    <setting-item name="更新间隔，为 0 只会初始化">
      <n-input-group>
        <n-input-number
          v-model:value.trim="targetDataRequest.requestInterval"
          class="select-time-number"
          min="0"
          :show-button="false"
          placeholder="默认使用全局数据"
        />
        <n-select class="select-time-options" v-model:value="targetDataRequest.requestIntervalUnit" :options="selectTimeOptions" />
      </n-input-group>
    </setting-item>
  </setting-item-box>
  <setting-item-box name="选择方式" class="go-mt-0" v-if="targetDataRequest.requestSource === 'internal'">
    <request-header :targetDataRequest="targetDataRequest"></request-header>
  </setting-item-box>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, toRefs, PropType } from 'vue'
import { SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { useTargetData } from '@/views/chart/ContentConfigurations/components/hooks/useTargetData.hook'
import { selectTypeOptions, selectTimeOptions } from '@/views/chart/ContentConfigurations/components/ChartData/index.d'
import { RequestConfigType } from '@/store/modules/chartEditStore/chartEditStore.d'
import { RequestHeader } from '../RequestHeader'
import { getApiVOList, ExternalApiVO } from '@/api/external/api'

const props = defineProps({
  targetDataRequest: Object as PropType<RequestConfigType>
})

const { chartEditStore } = useTargetData()
const { requestOriginUrl } = toRefs(chartEditStore.getRequestGlobalConfig)

const externalApiList = ref<ExternalApiVO[]>([])
const externalParamList = ref<Array<{ key: string; value: string }>>([])

const externalApiOptions = computed(() => {
  return externalApiList.value.map(api => ({
    label: `${api.systemName} - ${api.apiName} (${api.method} ${api.apiPath})`,
    value: api.id,
    disabled: api.status !== '0'
  }))
})

onMounted(async () => {
  try {
    const res = await getApiVOList()
    externalApiList.value = res.data || []
  } catch (e) {
    console.log('加载外部接口列表失败', e)
  }
})

watch(externalParamList, (newVal) => {
  const params: Record<string, string> = {}
  newVal.forEach(item => {
    if (item.key && item.value) {
      params[item.key] = item.value
    }
  })
  if (props.targetDataRequest) {
    props.targetDataRequest.externalRequestParams = params
  }
}, { deep: true })

watch(() => props.targetDataRequest?.externalRequestParams, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    externalParamList.value = Object.entries(newVal).map(([key, value]) => ({ key, value }))
  }
}, { immediate: true })
</script>
```

---

### Task 5.7: 改造请求发送逻辑

**Files:**
- Modify: `forge-report-ui/src/api/http.ts`

- [ ] **Step 1: 修改 customizeHttp 函数**

找到 customizeHttp 函数，修改为：

```typescript
export const customizeHttp = (targetParams: RequestConfigType, globalParams: RequestGlobalConfigType) => {
  if (!targetParams || !globalParams) {
    return
  }

  const {
    requestDataType,
    requestSource,
    requestUrl,
    requestHttpType,
    requestParams,
    requestParamsBodyType,
    requestSQLContent,
    requestContentType,
    externalApiId,
    externalRequestParams
  } = targetParams

  if (requestDataType === RequestDataTypeEnum.STATIC) return

  if (requestSource === 'internal') {
    if (!requestUrl) {
      return
    }

    let headers: RequestParamsObjType = {
      ...globalParams.requestParams.Header,
      ...requestParams.Header
    }
    headers = translateStr(headers)

    let data: RequestParamsObjType | FormData | string = {}
    let params: RequestParamsObjType = { ...requestParams.Params }
    params = translateStr(params)
    let formData: FormData = new FormData()

    switch (requestParamsBodyType) {
      case RequestBodyEnum.NONE:
        break
      case RequestBodyEnum.JSON:
        headers['Content-Type'] = ContentTypeEnum.JSON
        data = translateStr(requestParams.Body['json'])
        if (typeof data === 'string') data = JSON.parse(data)
        break
      case RequestBodyEnum.XML:
        headers['Content-Type'] = ContentTypeEnum.XML
        data = translateStr(requestParams.Body['xml'])
        break
      case RequestBodyEnum.X_WWW_FORM_URLENCODED: {
        headers['Content-Type'] = ContentTypeEnum.FORM_URLENCODED
        const bodyFormData = requestParams.Body['x-www-form-urlencoded']
        for (const i in bodyFormData) formData.set(i, translateStr(bodyFormData[i]))
        data = formData
        break
      }
      case RequestBodyEnum.FORM_DATA: {
        headers['Content-Type'] = ContentTypeEnum.FORM_DATA
        const bodyFormUrlencoded = requestParams.Body['form-data']
        for (const i in bodyFormUrlencoded) {
          formData.set(i, translateStr(bodyFormUrlencoded[i]))
        }
        data = formData
        break
      }
    }

    if (requestContentType === RequestContentTypeEnum.SQL) {
      headers['Content-Type'] = ContentTypeEnum.JSON
      data = requestSQLContent
    }

    try {
      const url = (new Function("return `" + `${globalParams.requestOriginUrl}${requestUrl}`.trim() + "`"))();
      return axiosInstance({
        url,
        method: requestHttpType,
        data,
        params,
        headers
      })
    } catch (error) {
      console.log(error)
      window['$message'].error('URL地址格式有误！')
    }
  }

  if (requestSource === 'external' && externalApiId) {
    const url = `${globalParams.requestOriginUrl}/external/proxy/${externalApiId}`
    return axiosInstance({
      url,
      method: RequestHttpEnum.POST,
      data: externalRequestParams || {}
    })
  }
}
```

---

### Task 5.8: 编译验证前端

- [ ] **Step 1: 前端编译**

```bash
cd forge-report-ui && pnpm build
```

---

## Phase 6: 集成验证

### Task 6.1: 后端全量编译

- [ ] **Step 1: 编译**

```bash
cd forge && mvn clean install -DskipTests
```

---

### Task 6.2: 启动验证

- [ ] **Step 1: 启动后端**

```bash
cd forge/forge-admin-server && mvn spring-boot:run
```

- [ ] **Step 2: 启动前端**

```bash
cd forge-report-ui && pnpm dev
```

---

### Task 6.3: 功能验证

- [ ] **Step 1: 验证外部系统管理**

访问：`http://localhost:5173/external/system`

测试：
1. 新增外部系统（Bearer Token 类型）
2. 编辑外部系统
3. 删除外部系统

- [ ] **Step 2: 验证外部接口管理**

访问：`http://localhost:5173/external/api`

测试：
1. 新增外部接口（JsonPath 适配类型）
2. 新增外部接口（Script 适配类型）
3. 编辑、删除操作

- [ ] **Step 3: 验证代理转发**

使用 curl 或浏览器测试：

```bash
curl -X POST http://localhost:8580/external/proxy/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"param1": "value1"}'
```

- [ ] **Step 4: 验证报表设计器**

访问报表设计器，测试动态请求配置：
1. 切换到"外部接口"
2. 选择已配置的外部接口
3. 配置请求参数
4. 发送请求验证数据返回

---

### Task 6.4: 提交代码

- [ ] **Step 1: 提交后端代码**

```bash
git add forge/forge-framework/forge-plugin-parent/forge-plugin-external
git add forge/forge-framework/forge-plugin-parent/pom.xml
git add forge/forge-admin-server/pom.xml
git add forge/forge-admin-server/src/main/resources/sql/forge.sql
git commit -m "feat(external): 新增外部接口代理转发插件模块"
```

- [ ] **Step 2: 提交前端代码**

```bash
git add forge-report-ui/src/api/external
git add forge-report-ui/src/views/external
git add forge-report-ui/src/store/modules/chartEditStore/chartEditStore.d.ts
git add forge-report-ui/src/packages/public/publicConfig.ts
git add forge-report-ui/src/views/chart/ContentConfigurations/components/ChartData/components/ChartDataRequest/components/RequestTargetConfig/index.vue
git add forge-report-ui/src/api/http.ts
git commit -m "feat(report-ui): 改造动态请求配置支持外部接口选择"
```

---

## 执行完成标志

完成所有 Phase 后，项目应具备以下能力：

1. ✅ 外部系统管理（CRUD + 认证配置）
2. ✅ 外部接口管理（CRUD + 数据适配配置）
3. ✅ 代理转发服务（解决跨域 + 认证）
4. ✅ 报表设计器支持外部接口选择
5. ✅ 数据适配能力（JSON Path + Script）