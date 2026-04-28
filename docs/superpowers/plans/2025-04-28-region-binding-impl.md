# 行政区划绑定功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现行政区划绑定功能，支持组织、用户绑定行政区划，登录Session包含完整行政区划信息。

**Architecture:** 采用完整缓存方案，LoginUser存储完整行政区划信息，登录时一次性加载，用户组织变更时同步更新regionCode。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Vue 3 + Naive UI + UnoCSS

---

## 文件结构

### 后端新增文件
- `forge-plugin-system/entity/SysRegion.java` - 行政区划实体
- `forge-plugin-system/mapper/SysRegionMapper.java` - Mapper接口
- `forge-plugin-system/service/ISysRegionService.java` - Service接口
- `forge-plugin-system/service/impl/SysRegionServiceImpl.java` - Service实现
- `forge-plugin-system/controller/SysRegionController.java` - Controller
- `forge-plugin-system/dto/SysRegionDTO.java` - DTO
- `forge-plugin-system/vo/SysRegionTreeVO.java` - 树形VO

### 后端修改文件
- `forge-plugin-system/entity/SysUser.java` - 新增regionCode字段
- `forge-starter-core/session/LoginUser.java` - 新增5个行政区划字段
- `forge-plugin-system/service/impl/UserLoadServiceImpl.java` - 加载行政区划
- `forge-plugin-system/service/impl/SysUserOrgServiceImpl.java` - 组织变更同步

### 前端新增文件
- `forge-admin-ui/src/api/system/region.js` - API
- `forge-admin-ui/src/views/system/region.vue` - 行政区划管理页面

### 前端修改文件
- `forge-admin-ui/src/views/system/org.vue` - 新增行政区划选择器
- `forge-admin-ui/src/views/system/user.vue` - 新增行政区划选择器

---

## Task 1: 数据库变更

**Files:**
- Modify: `forge-admin-server/sql/初始化脚本.sql`

- [ ] **Step 1: 在初始化脚本末尾添加sys_user表字段变更SQL**

在文件末尾添加以下SQL：

```sql
-- sys_user表新增region_code字段
ALTER TABLE sys_user ADD COLUMN region_code VARCHAR(10) DEFAULT NULL COMMENT '行政区划编码' AFTER avatar;
ALTER TABLE sys_user ADD INDEX idx_region_code (region_code);
```

- [ ] **Step 2: 执行SQL变更**

在MySQL数据库中执行上述SQL，验证字段添加成功：

```sql
DESC sys_user;
-- 应看到region_code字段
```

---

## Task 2: 后端Entity - SysRegion

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysRegion.java`

- [ ] **Step 1: 创建SysRegion实体类**

```java
package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 行政区划表实体类
 */
@Data
@TableName("sys_region_code")
public class SysRegion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行政区划代码（主键）
     */
    private String code;

    /**
     * 行政区划名称
     */
    private String name;

    /**
     * 行政级别(1-省,2-市,3-区/县,4-街道)
     */
    private Integer level;

    /**
     * 父级代码
     */
    private String parentCode;

    /**
     * 全名
     */
    private String fullName;

    /**
     * 地市编码
     */
    private String cityCode;

    /**
     * 子节点列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysRegion> children;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 3: 后端Entity - SysUser新增字段

**Files:**
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/entity/SysUser.java`

- [ ] **Step 1: 在SysUser类中添加regionCode字段**

在avatar字段后添加：

```java
    /**
     * 行政区划编码
     */
    private String regionCode;
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 4: 后端LoginUser新增字段

**Files:**
- Modify: `forge/forge-framework/forge-starter-parent/forge-starter-core/src/main/java/com/mdframe/forge/starter/core/session/LoginUser.java`

- [ ] **Step 1: 在LoginUser类末尾添加行政区划字段**

在userClient字段后添加：

```java
    /**
     * 行政区划编码
     */
    private String regionCode;

    /**
     * 行政区划名称
     */
    private String regionName;

    /**
     * 行政级别(1-省,2-市,3-区/县,4-街道)
     */
    private Integer regionLevel;

    /**
     * 行政区划全名
     */
    private String regionFullName;

    /**
     * 行政区划祖级编码（如：110000,110100,110101）
     */
    private String regionAncestors;
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-starter-parent/forge-starter-core -am
```

---

## Task 5: 后端Mapper - SysRegionMapper

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/mapper/SysRegionMapper.java`

- [ ] **Step 1: 创建SysRegionMapper接口**

```java
package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行政区划Mapper接口
 */
@Mapper
public interface SysRegionMapper extends BaseMapper<SysRegion> {

}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 6: 后端DTO - SysRegionDTO

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/dto/SysRegionDTO.java`

- [ ] **Step 1: 创建SysRegionDTO类**

```java
package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 行政区划新增/修改DTO
 */
@Data
public class SysRegionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行政区划代码（主键）
     */
    private String code;

    /**
     * 行政区划名称
     */
    private String name;

    /**
     * 行政级别(1-省,2-市,3-区/县,4-街道)
     */
    private Integer level;

    /**
     * 父级代码
     */
    private String parentCode;

    /**
     * 全名
     */
    private String fullName;

    /**
     * 地市编码
     */
    private String cityCode;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 7: 后端VO - SysRegionTreeVO

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/vo/SysRegionTreeVO.java`

- [ ] **Step 1: 创建SysRegionTreeVO类**

```java
package com.mdframe.forge.plugin.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 行政区划树形VO
 */
@Data
public class SysRegionTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行政区划代码
     */
    private String code;

    /**
     * 行政区划名称
     */
    private String name;

    /**
     * 行政级别(1-省,2-市,3-区/县,4-街道)
     */
    private Integer level;

    /**
     * 父级代码
     */
    private String parentCode;

    /**
     * 全名
     */
    private String fullName;

    /**
     * 地市编码
     */
    private String cityCode;

    /**
     * 子节点列表
     */
    private List<SysRegionTreeVO> children;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 8: 后端Service接口 - ISysRegionService

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/ISysRegionService.java`

- [ ] **Step 1: 创建ISysRegionService接口**

```java
package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.dto.SysRegionDTO;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.vo.SysRegionTreeVO;

import java.util.List;

/**
 * 行政区划Service接口
 */
public interface ISysRegionService extends IService<SysRegion> {

    /**
     * 获取行政区划树形结构
     */
    List<SysRegionTreeVO> selectRegionTree();

    /**
     * 根据code获取详情
     */
    SysRegion selectRegionByCode(String code);

    /**
     * 新增行政区划
     */
    boolean insertRegion(SysRegionDTO dto);

    /**
     * 更新行政区划
     */
    boolean updateRegion(SysRegionDTO dto);

    /**
     * 删除行政区划
     */
    boolean deleteRegionByCode(String code);

    /**
     * 获取子级列表
     */
    List<SysRegion> selectChildrenByParentCode(String parentCode);

    /**
     * 搜索行政区划
     */
    List<SysRegion> searchRegionByName(String name);

    /**
     * 构建祖级编码
     */
    String buildAncestors(String code);
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 9: 后端Service实现 - SysRegionServiceImpl

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysRegionServiceImpl.java`

- [ ] **Step 1: 创建SysRegionServiceImpl实现类**

```java
package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysRegionDTO;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.mapper.SysRegionMapper;
import com.mdframe.forge.plugin.system.service.ISysRegionService;
import com.mdframe.forge.plugin.system.vo.SysRegionTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 行政区划Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysRegionServiceImpl extends ServiceImpl<SysRegionMapper, SysRegion> implements ISysRegionService {

    private final SysRegionMapper regionMapper;

    @Override
    public List<SysRegionTreeVO> selectRegionTree() {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> allRegions = regionMapper.selectList(wrapper);
        return buildTreeVO(allRegions, null);
    }

    @Override
    public SysRegion selectRegionByCode(String code) {
        return regionMapper.selectById(code);
    }

    @Override
    public boolean insertRegion(SysRegionDTO dto) {
        SysRegion region = new SysRegion();
        BeanUtil.copyProperties(dto, region);
        return regionMapper.insert(region) > 0;
    }

    @Override
    public boolean updateRegion(SysRegionDTO dto) {
        SysRegion region = new SysRegion();
        BeanUtil.copyProperties(dto, region);
        return regionMapper.updateById(region) > 0;
    }

    @Override
    public boolean deleteRegionByCode(String code) {
        return regionMapper.deleteById(code) > 0;
    }

    @Override
    public List<SysRegion> selectChildrenByParentCode(String parentCode) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, parentCode)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        return regionMapper.selectList(wrapper);
    }

    @Override
    public List<SysRegion> searchRegionByName(String name) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SysRegion::getName, name)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        return regionMapper.selectList(wrapper);
    }

    @Override
    public String buildAncestors(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        StringBuilder ancestors = new StringBuilder();
        String currentCode = code;
        while (StrUtil.isNotBlank(currentCode)) {
            SysRegion region = regionMapper.selectById(currentCode);
            if (region == null) {
                break;
            }
            if (ancestors.length() > 0) {
                ancestors.insert(0, ",");
            }
            ancestors.insert(0, currentCode);
            currentCode = region.getParentCode();
        }
        return ancestors.toString();
    }

    private List<SysRegionTreeVO> buildTreeVO(List<SysRegion> allRegions, String parentCode) {
        List<SysRegionTreeVO> treeList = new ArrayList<>();
        for (SysRegion region : allRegions) {
            if (StrUtil.isBlank(parentCode) && StrUtil.isBlank(region.getParentCode)) {
                SysRegionTreeVO vo = new SysRegionTreeVO();
                BeanUtil.copyProperties(region, vo);
                vo.setChildren(buildTreeVO(allRegions, region.getCode()));
                treeList.add(vo);
            } else if (StrUtil.isNotBlank(parentCode) && parentCode.equals(region.getParentCode())) {
                SysRegionTreeVO vo = new SysRegionTreeVO();
                BeanUtil.copyProperties(region, vo);
                vo.setChildren(buildTreeVO(allRegions, region.getCode()));
                treeList.add(vo);
            }
        }
        return treeList.isEmpty() ? null : treeList;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 10: 后端Controller - SysRegionController

**Files:**
- Create: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/SysRegionController.java`

- [ ] **Step 1: 创建SysRegionController类**

```java
package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.dto.SysRegionDTO;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.service.ISysRegionService;
import com.mdframe.forge.plugin.system.vo.SysRegionTreeVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行政区划管理Controller
 */
@RestController
@RequestMapping("/system/region")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysRegionController {

    private final ISysRegionService regionService;

    /**
     * 获取行政区划树形结构
     */
    @GetMapping("/tree")
    public RespInfo<List<SysRegionTreeVO>> tree() {
        List<SysRegionTreeVO> tree = regionService.selectRegionTree();
        return RespInfo.success(tree);
    }

    /**
     * 根据code获取详情
     */
    @GetMapping("/{code}")
    public RespInfo<SysRegion> getByCode(@PathVariable String code) {
        SysRegion region = regionService.selectRegionByCode(code);
        return RespInfo.success(region);
    }

    /**
     * 新增行政区划
     */
    @PostMapping("/")
    public RespInfo<Void> add(@RequestBody SysRegionDTO dto) {
        boolean result = regionService.insertRegion(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 更新行政区划
     */
    @PutMapping("/")
    public RespInfo<Void> edit(@RequestBody SysRegionDTO dto) {
        boolean result = regionService.updateRegion(dto);
        return result ? RespInfo.success() : RespInfo.error("更新失败");
    }

    /**
     * 删除行政区划
     */
    @DeleteMapping("/{code}")
    public RespInfo<Void> remove(@PathVariable String code) {
        boolean result = regionService.deleteRegionByCode(code);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 获取子级列表
     */
    @GetMapping("/children/{parentCode}")
    public RespInfo<List<SysRegion>> getChildren(@PathVariable String parentCode) {
        List<SysRegion> children = regionService.selectChildrenByParentCode(parentCode);
        return RespInfo.success(children);
    }

    /**
     * 搜索行政区划
     */
    @GetMapping("/search")
    public RespInfo<List<SysRegion>> search(@RequestParam String name) {
        List<SysRegion> regions = regionService.searchRegionByName(name);
        return RespInfo.success(regions);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 11: 后端UserLoadServiceImpl修改 - 加载行政区划

**Files:**
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java`

- [ ] **Step 1: 注入SysRegionMapper**

在类顶部添加注入：

```java
    private final SysRegionMapper regionMapper;
```

- [ ] **Step 2: 在buildLoginUser方法中调用loadUserRegion**

在`loadApiPermissions(loginUser);`后添加：

```java
        // 6. 加载用户行政区划
        loadUserRegion(loginUser);
```

- [ ] **Step 3: 添加loadUserRegion方法**

在类的末尾添加方法：

```java
    /**
     * 加载用户行政区划信息
     * 优先级：用户有组织且组织有regionCode -> 取组织的regionCode
     * 用户有组织但组织无regionCode -> 取用户的regionCode（fallback）
     * 用户无组织 -> 取用户的regionCode
     */
    private void loadUserRegion(LoginUser loginUser) {
        String regionCode = null;
        
        // 优先从主组织获取
        if (loginUser.getMainOrgId() != null) {
            SysOrg org = sysOrgMapper.selectById(loginUser.getMainOrgId());
            if (org != null && StrUtil.isNotBlank(org.getRegionCode())) {
                regionCode = org.getRegionCode();
            }
        }
        
        // 组织无regionCode或无组织，从用户自身获取
        if (StrUtil.isBlank(regionCode)) {
            SysUser user = userMapper.selectById(loginUser.getUserId());
            if (user != null && StrUtil.isNotBlank(user.getRegionCode())) {
                regionCode = user.getRegionCode();
            }
        }
        
        // 根据regionCode查询完整信息
        if (StrUtil.isNotBlank(regionCode)) {
            SysRegion region = regionMapper.selectById(regionCode);
            if (region != null) {
                loginUser.setRegionCode(region.getCode());
                loginUser.setRegionName(region.getName());
                loginUser.setRegionLevel(region.getLevel());
                loginUser.setRegionFullName(region.getFullName());
                loginUser.setRegionAncestors(buildRegionAncestors(region));
                
                log.debug("加载用户行政区划: userId={}, regionCode={}, regionName={}",
                        loginUser.getUserId(), regionCode, region.getName());
            }
        }
    }

    /**
     * 构建行政区划祖级编码
     */
    private String buildRegionAncestors(SysRegion region) {
        StringBuilder ancestors = new StringBuilder();
        String currentCode = region.getCode();
        while (StrUtil.isNotBlank(currentCode)) {
            SysRegion currentRegion = regionMapper.selectById(currentCode);
            if (currentRegion == null) {
                break;
            }
            if (ancestors.length() > 0) {
                ancestors.insert(0, ",");
            }
            ancestors.insert(0, currentCode);
            currentCode = currentRegion.getParentCode();
        }
        return ancestors.toString();
    }
```

- [ ] **Step 4: 添加SysRegion导入**

在import部分添加：

```java
import com.mdframe.forge.plugin.system.entity.SysRegion;
```

- [ ] **Step 5: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 12: 后端SysUserOrgServiceImpl修改 - 组织变更同步

**Files:**
- Modify: `forge/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/SysUserOrgServiceImpl.java`

- [ ] **Step 1: 注入需要的Mapper**

添加注入：

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class SysUserOrgServiceImpl extends ServiceImpl<SysUserOrgMapper, SysUserOrg> implements ISysUserOrgService {

    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
```

- [ ] **Step 2: 添加同步用户regionCode的方法**

```java
    /**
     * 绑定用户组织关系（主组织变更时同步regionCode）
     */
    public boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setUserId(userId);
        userOrg.setOrgId(orgId);
        userOrg.setIsMain(isMain);
        
        boolean result = this.save(userOrg);
        
        // 如果是主组织，同步更新用户的regionCode
        if (result && isMain != null && isMain == 1) {
            syncUserRegionCode(userId, orgId);
        }
        
        return result;
    }

    /**
     * 更新用户主组织（同步regionCode）
     */
    public boolean updateUserMainOrg(Long userId, Long newOrgId) {
        // 1. 清除旧的主组织标记
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getIsMain, 1);
        SysUserOrg oldMainOrg = this.selectOne(wrapper);
        if (oldMainOrg != null) {
            oldMainOrg.setIsMain(0);
            this.updateById(oldMainOrg);
        }
        
        // 2. 设置新的主组织
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getOrgId, newOrgId);
        SysUserOrg newOrg = this.selectOne(wrapper);
        if (newOrg != null) {
            newOrg.setIsMain(1);
            this.updateById(newOrg);
        } else {
            newOrg = new SysUserOrg();
            newOrg.setUserId(userId);
            newOrg.setOrgId(newOrgId);
            newOrg.setIsMain(1);
            this.save(newOrg);
        }
        
        // 3. 同步regionCode
        syncUserRegionCode(userId, newOrgId);
        
        return true;
    }

    /**
     * 同步用户regionCode
     */
    private void syncUserRegionCode(Long userId, Long orgId) {
        SysOrg org = orgMapper.selectById(orgId);
        if (org != null && StrUtil.isNotBlank(org.getRegionCode())) {
            SysUser user = userMapper.selectById(userId);
            if (user != null) {
                user.setRegionCode(org.getRegionCode());
                userMapper.updateById(user);
                log.info("同步用户regionCode: userId={}, orgId={}, regionCode={}",
                        userId, orgId, org.getRegionCode());
            }
        }
    }
```

- [ ] **Step 3: 编译验证**

```bash
cd forge && mvn compile -pl forge-framework/forge-plugin-parent/forge-plugin-system -am
```

---

## Task 13: 前端API - region.js

**Files:**
- Create: `forge-admin-ui/src/api/system/region.js`

- [ ] **Step 1: 创建region.js API文件**

```javascript
import { request } from '@/utils/request'

/**
 * 获取行政区划树形结构
 */
export function getRegionTree() {
  return request.get('/system/region/tree')
}

/**
 * 根据code获取详情
 */
export function getRegionByCode(code) {
  return request.get(`/system/region/${code}`)
}

/**
 * 新增行政区划
 */
export function addRegion(data) {
  return request.post('/system/region/', data)
}

/**
 * 更新行政区划
 */
export function updateRegion(data) {
  return request.put('/system/region/', data)
}

/**
 * 删除行政区划
 */
export function deleteRegion(code) {
  return request.delete(`/system/region/${code}`)
}

/**
 * 获取子级列表
 */
export function getRegionChildren(parentCode) {
  return request.get(`/system/region/children/${parentCode}`)
}

/**
 * 搜索行政区划
 */
export function searchRegion(name) {
  return request.get('/system/region/search', { params: { name } })
}
```

---

## Task 14: 前端页面 - region.vue

**Files:**
- Create: `forge-admin-ui/src/views/system/region.vue`

- [ ] **Step 1: 创建region.vue页面**

```vue
<template>
  <div class="system-region-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/region"
      :api-config="{
        list: 'get@/system/region/tree',
        detail: 'get@/system/region/{code}',
        add: 'post@/system/region/',
        update: 'put@/system/region/',
        delete: 'delete@/system/region/{code}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-render-list="beforeRenderList"
      row-key="code"
      :show-pagination="false"
      :lazy="false"
      add-button-text="新增行政区划"
      :table-props="{
        expandedRowKeys: expandedKeys,
        onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
      }"
    >
      <!-- 自定义工具栏 -->
      <template #toolbar-end>
        <n-button @click="toggleExpandAll">
          <template #icon>
            <i :class="expandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
          </template>
          {{ expandAll ? '折叠全部' : '展开全部' }}
        </n-button>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemRegion' })

const crudRef = ref(null)
const expandAll = ref(true)
const expandedKeys = ref([])
const parentRegionOptions = ref([{ label: '顶级区域', value: '', key: '' }])

const levelOptions = [
  { label: '省', value: 1 },
  { label: '市', value: 2 },
  { label: '区/县', value: 3 },
  { label: '街道', value: 4 },
]

const searchSchema = [
  {
    field: 'name',
    label: '名称',
    type: 'input',
    props: {
      placeholder: '请输入行政区划名称',
    },
  },
]

const tableColumns = computed(() => [
  {
    prop: 'name',
    label: '名称',
    width: 200,
  },
  {
    prop: 'code',
    label: '编码',
    width: 120,
  },
  {
    prop: 'level',
    label: '级别',
    width: 100,
    render: (row) => {
      const levelMap = {
        1: { text: '省', type: 'success' },
        2: { text: '市', type: 'info' },
        3: { text: '区/县', type: 'warning' },
        4: { text: '街道', type: 'default' },
      }
      const config = levelMap[row.level] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'fullName',
    label: '全名',
    minWidth: 200,
  },
  {
    prop: 'cityCode',
    label: '地市编码',
    width: 120,
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '新增', key: 'add', type: 'primary', onClick: handleAdd },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

const editSchema = ref([
  {
    field: 'parentCode',
    label: '上级区域',
    type: 'treeSelect',
    defaultValue: '',
    props: {
      placeholder: '请选择上级区域',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => parentRegionOptions.value,
  },
  {
    field: 'code',
    label: '编码',
    type: 'input',
    rules: [{ required: true, message: '请输入行政区划编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入行政区划编码',
    },
  },
  {
    field: 'name',
    label: '名称',
    type: 'input',
    rules: [{ required: true, message: '请输入行政区划名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入行政区划名称',
    },
  },
  {
    field: 'level',
    label: '级别',
    type: 'select',
    defaultValue: 1,
    rules: [{ required: true, type: 'number', message: '请选择行政级别', trigger: 'change' }],
    props: {
      placeholder: '请选择行政级别',
      options: levelOptions,
    },
  },
  {
    field: 'fullName',
    label: '全名',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入行政区划全名',
    },
  },
  {
    field: 'cityCode',
    label: '地市编码',
    type: 'input',
    props: {
      placeholder: '请输入地市编码',
    },
  },
])

async function loadParentRegionOptions() {
  try {
    const res = await request.get('/system/region/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.name,
          value: item.code,
          key: item.code,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      parentRegionOptions.value = [
        { label: '顶级区域', value: '', key: '' },
        ...convertToTreeSelect(res.data || []),
      ]
    }
  }
  catch (error) {
    console.error('加载上级区域选项失败:', error)
  }
}

function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.code)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

function beforeRenderList(list) {
  if (expandAll.value) {
    expandedKeys.value = getAllKeys(list)
  }
  return list
}

function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys
  const tableData = crudRef.value?.getTableData() || []
  const allKeys = getAllKeys(tableData)
  expandAll.value = keys.length === allKeys.length
}

function toggleExpandAll() {
  expandAll.value = !expandAll.value
  if (expandAll.value) {
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = getAllKeys(tableData)
  }
  else {
    expandedKeys.value = []
  }
}

async function handleAdd(row) {
  await loadParentRegionOptions()
  const parentCodeField = editSchema.value.find(f => f.field === 'parentCode')
  if (row && parentCodeField) {
    parentCodeField.defaultValue = row.code
  }
  crudRef.value?.showAdd()
}

async function handleEdit(row) {
  await loadParentRegionOptions()
  crudRef.value?.showEdit(row)
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除"${row.name}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.delete(`/system/region/${row.code}`)
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('删除失败')
      }
    },
  })
}

onMounted(() => {
  loadParentRegionOptions()
})
</script>

<style scoped>
.system-region-page {
  height: 100%;
}
</style>
```

- [ ] **Step 2: 路由自动生成**

本项目使用 `unplugin-vue-router` 自动生成路由。将 `region.vue` 文件放置在 `src/views/system/` 目录下，路由会自动生成为 `/system/region`，无需手动配置路由。

前端菜单需要在后台管理系统的 `sys_resource` 表中配置，添加以下菜单数据：

```sql
-- 行政区划管理菜单
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, path, component, menu_status, visible, icon)
VALUES (XXX, 0, '行政区划管理', [系统管理父菜单ID], 2, 10, '/system/region', '/views/system/region.vue', 1, 1, 'i-material-symbols:location-city');

-- 注意：XXX需要替换为实际的ID值，[系统管理父菜单ID]需要替换为系统管理菜单的parent_id
```

---

## Task 15: 前端修改 - org.vue新增行政区划选择

**Files:**
- Modify: `forge-admin-ui/src/views/system/org.vue`

- [ ] **Step 1: 在editSchema中添加行政区划选择器字段**

在address字段后添加：

```javascript
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => regionOptions.value,
  },
```

- [ ] **Step 2: 添加regionOptions变量**

在script setup中添加：

```javascript
const regionOptions = ref([{ label: '请选择', value: '', key: '' }])
```

- [ ] **Step 3: 添加加载行政区划选项的方法**

```javascript
async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.name,
          value: item.code,
          key: item.code,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      regionOptions.value = [
        { label: '请选择', value: '', key: '' },
        ...convertToTreeSelect(res.data || []),
      ]
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}
```

- [ ] **Step 4: 在loadParentOrgOptions方法中调用loadRegionOptions**

修改loadParentOrgOptions方法：

```javascript
async function loadParentOrgOptions() {
  try {
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.orgName,
          value: item.id,
          key: item.id,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      parentOrgOptions.value = [
        { label: '顶级组织', value: 0, key: 0 },
        ...convertToTreeSelect(res.data || []),
      ]
      
      // 同时加载行政区划选项
      await loadRegionOptions()
    }
  }
  catch (error) {
    console.error('加载上级组织选项失败:', error)
  }
}
```

- [ ] **Step 5: 在表格列中添加行政区划列**

在tableColumns中添加：

```javascript
  {
    prop: 'regionCode',
    label: '行政区划',
    width: 150,
    render: (row) => {
      // 这里可以查询regionName显示，简化版本直接显示code
      return row.regionCode || '-'
    },
  },
```

---

## Task 16: 前端修改 - user.vue新增行政区划选择

**Files:**
- Modify: `forge-admin-ui/src/views/system/user.vue`

- [ ] **Step 1: 在editSchema中添加行政区划选择器字段**

在user.vue的editSchema数组中，在"状态配置"divider（Line 559）之前添加行政区划字段。具体位置是在gender字段（Line 550-557）之后，添加新的divider和regionCode字段：

```javascript
  {
    type: 'divider',
    label: '行政区划',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => regionOptions.value,
  },
```

- [ ] **Step 2: 添加regionOptions变量和hasMainOrg变量**

```javascript
const regionOptions = ref([{ label: '请选择', value: '', key: '' }])
const hasMainOrg = ref(false)
```

- [ ] **Step 3: 添加加载行政区划选项的方法**

```javascript
async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.name,
          value: item.code,
          key: item.code,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      regionOptions.value = [
        { label: '请选择', value: '', key: '' },
        ...convertToTreeSelect(res.data || []),
      ]
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}
```

- [ ] **Step 4: 在编辑时检查是否有主组织**

在handleEdit或showEdit方法中添加判断：

```javascript
// 检查是否有主组织
hasMainOrg.value = formData.mainOrgId != null
```

- [ ] **Step 5: 在表格列中添加行政区划列**

在tableColumns中添加：

```javascript
  {
    prop: 'regionCode',
    label: '行政区划',
    width: 150,
    render: (row) => {
      return row.regionCode || '-'
    },
  },
```

---

## Task 17: 最终编译验证

- [ ] **Step 1: 后端完整编译**

```bash
cd forge && mvn clean compile -DskipTests
```

- [ ] **Step 2: 前端编译检查**

```bash
cd forge-admin-ui && pnpm build
```

- [ ] **Step 3: 提交代码**

```bash
git add .
git commit -m "feat: 实现行政区划绑定功能
- 新增行政区划管理模块(SysRegion)
- SysUser新增regionCode字段
- LoginUser新增行政区划信息
- 登录时加载用户行政区划信息
- 组织变更时同步用户regionCode
- 前端新增行政区划管理页面
- 组织/用户管理支持行政区划选择"
```