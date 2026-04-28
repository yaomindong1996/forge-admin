# 行政区划绑定功能设计文档

## 1. 背景

项目中新增了行政区划表（sys_region_code），需要在组织管理和人员管理中支持行政区划绑定功能，同时在登录Session中提供用户的行政区划信息。

## 2. 需求分析

### 2.1 核心需求

1. **组织管理支持行政区划绑定**：组织信息可以绑定行政区划，树形结构展示行政区划选择器
2. **用户管理支持行政区划绑定**：用户信息可以绑定行政区划编码
3. **行政区划优先级规则**：
   - 用户有组织 → 以组织的行政区划为主
   - 用户无组织 → 以用户自身的行政区划为主
4. **同步更新机制**：更新用户归属组织时，用户的行政区划信息也要同步更新
5. **Session信息**：登录Session可获取当前用户的行政区划信息（编码、名称、行政级别等）

### 2.2 行政区划数据管理

需要完整的行政区划管理模块，包括：
- CRUD操作
- 树形展示
- 搜索功能
- 懒加载子节点

## 3. 技术方案

### 3.1 方案选择

采用**完整缓存方案**：
- LoginUser存储完整行政区划信息（code、name、level、fullName、ancestors）
- 登录时一次性加载到Session
- 用户组织变更时同步更新regionCode
- 性能好、信息完整、与现有LoginUser架构一致

## 4. 模块结构

### 4.1 后端新增模块

```
forge-plugin-system下新增：
├── entity/SysRegion.java          - 行政区划实体
├── mapper/SysRegionMapper.java    - Mapper接口
├── service/ISysRegionService.java - Service接口
├── service/impl/SysRegionServiceImpl.java - Service实现
├── controller/SysRegionController.java - Controller
├── dto/SysRegionDTO.java          - DTO传输对象
├── vo/SysRegionTreeVO.java        - 树形VO对象
```

### 4.2 后端修改模块

```
├── entity/SysUser.java            - 新增regionCode字段
├── session/LoginUser.java         - 新增5个行政区划字段
├── service/impl/UserLoadServiceImpl.java - 加载行政区划信息
├── service/impl/SysUserOrgServiceImpl.java - 组织变更时同步regionCode
```

### 4.3 前端新增

```
├── views/system/region.vue        - 行政区划管理页面
├── api/system/region.js           - 行政区划API
├── components/RegionSelector.vue  - 行政区划选择器组件
```

### 4.4 前端修改

```
├── views/system/org.vue           - 组织管理页面新增行政区划选择器
├── views/system/user.vue          - 用户管理页面新增行政区划选择器
```

## 5. 数据库设计

### 5.1 sys_user表变更

```sql
ALTER TABLE sys_user ADD COLUMN region_code VARCHAR(10) DEFAULT NULL COMMENT '行政区划编码' AFTER avatar;
ALTER TABLE sys_user ADD INDEX idx_region_code (region_code);
```

### 5.2 sys_region_code表（已存在）

字段：code（主键）、name、level、parent_code、full_name、city_code

## 6. 后端核心实现

### 6.1 SysRegion实体类

```java
@TableName("sys_region_code")
public class SysRegion {
    private String code;        // 行政区划代码（主键）
    private String name;        // 行政区划名称
    private Integer level;      // 行政级别(1-省,2-市,3-区/县,4-街道)
    private String parentCode;  // 父级代码
    private String fullName;    // 全名
    private String cityCode;    // 地市编码
}
```

### 6.2 LoginUser新增字段

```java
private String regionCode;       // 行政区划编码
private String regionName;       // 行政区划名称
private Integer regionLevel;     // 行政级别(1-省,2-市,3-区/县,4-街道)
private String regionFullName;   // 行政区划全名
private String regionAncestors;  // 行政区划祖级编码（如：110000,110100,110101）
```

### 6.3 UserLoadServiceImpl加载逻辑

在`buildLoginUser`方法中新增`loadUserRegion`方法：

**优先级规则**：
- 用户有组织且组织有regionCode → 取组织的regionCode
- 用户有组织但组织无regionCode → 取用户的regionCode（fallback）
- 用户无组织 → 取用户的regionCode

**实现逻辑**：
1. 获取用户主组织ID（mainOrgId）
2. 若有主组织，查询组织的regionCode
3. 若组织的regionCode为空或无主组织，查询用户的regionCode
4. 根据regionCode查询SysRegion获取完整信息
5. 构建祖级编码（通过parentCode递归查询）

### 6.4 SysUserOrgServiceImpl同步逻辑

当用户绑定/更换主组织时：
1. 查询新组织的regionCode
2. 更新用户的regionCode字段
3. 调用userMapper.updateById保存

## 7. 前端实现

### 7.1 行政区划管理页面

- 使用Naive UI的NTree组件展示树形结构
- 支持搜索过滤（按名称搜索）
- CRUD操作：新增、编辑、删除
- 表单字段：code、name、level、parentCode、fullName、cityCode

### 7.2 行政区划选择器组件

- 支持多级联动选择（省→市→区/县→街道）
- 搜索功能
- 选中后返回完整信息

### 7.3 组织管理页面修改

- 新增行政区划选择器字段
- 编辑时可选择行政区划
- 显示行政区划名称

### 7.4 用户管理页面修改

- 新增行政区划选择器字段
- 无组织时允许手动选择
- 有组织时自动显示组织行政区划，禁止编辑

## 8. API接口设计

### 8.1 行政区划接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/system/region/tree` | GET | 获取行政区划树形结构 |
| `/system/region/{code}` | GET | 根据code获取详情 |
| `/system/region/` | POST | 新增行政区划 |
| `/system/region/` | PUT | 更新行政区划 |
| `/system/region/{code}` | DELETE | 删除行政区划 |
| `/system/region/children/{parentCode}` | GET | 获取子级列表 |
| `/system/region/search?name={name}` | GET | 搜索行政区划 |

### 8.2 Session信息获取

用户登录后通过`/auth/userInfo`接口获取LoginUser，包含完整行政区划信息。

## 9. 风险评估

### 9.1 数据一致性风险

- 组织regionCode变更时，不会自动更新已绑定用户的regionCode
- 需要考虑是否提供批量同步功能（后续可扩展）

### 9.2 性能风险

- 祖级编码构建需要递归查询，可能影响登录性能
- 建议后续优化：在SysRegion表增加ancestors字段预存储

## 10. 扩展考虑

### 10.1 数据导入

行政区划数据量大（全国约4级、数万条），建议提供Excel导入功能。

### 10.2 缓存优化

行政区划数据变化频率低，可考虑Redis缓存树形结构数据。