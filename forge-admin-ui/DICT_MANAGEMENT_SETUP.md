# 字典管理功能设置指南

## 已创建的文件

1. **字典类型管理页面**: `src/views/system/dict-type.vue`
2. **字典数据管理页面**: `src/views/system/dict-data.vue`

## 功能说明

### 字典类型管理 (dict-type.vue)
- 字典类型的增删改查
- 支持搜索：字典名称、字典类型、状态
- 支持查看和管理字典数据（点击"字典数据"按钮跳转到字典数据管理页面）
- 字段：
  - 字典ID (dictId)
  - 字典名称 (dictName)
  - 字典类型 (dictType) - 租户内唯一
  - 字典状态 (dictStatus) - 0禁用，1正常
  - 备注 (remark)

### 字典数据管理 (dict-data.vue)
- 字典数据的增删改查
- 支持按字典类型筛选
- 支持搜索：字典标签、字典键值、状态
- 字段：
  - 字典编码 (dictCode)
  - 字典标签 (dictLabel)
  - 字典键值 (dictValue)
  - 字典类型 (dictType)
  - 字典排序 (dictSort)
  - 样式属性 (cssClass)
  - 表格样式 (listClass)
  - 是否默认 (isDefault) - Y是，N否
  - 字典状态 (dictStatus) - 0禁用，1正常
  - 备注 (remark)

## 菜单配置

需要在系统的菜单管理中添加以下菜单项：

### 1. 字典类型管理菜单

在菜单管理页面添加：

- **上级资源**: 系统管理（或其他合适的父菜单）
- **资源类型**: 菜单
- **资源名称**: 字典类型
- **路由地址**: `/system/dict-type`
- **组件路径**: `system/dict-type`
- **图标**: 可选择合适的图标，如 `ionicons5:BookOutline` 或 `local:i-material-symbols:book-outline`
- **排序**: 根据需要设置
- **显示状态**: 显示
- **菜单状态**: 显示

### 2. 字典数据管理菜单

在菜单管理页面添加：

- **上级资源**: 系统管理（或其他合适的父菜单）
- **资源类型**: 菜单
- **资源名称**: 字典数据
- **路由地址**: `/system/dict-data`
- **组件路径**: `system/dict-data`
- **图标**: 可选择合适的图标，如 `ionicons5:ListOutline` 或 `local:i-material-symbols:list`
- **排序**: 根据需要设置
- **显示状态**: 隐藏（因为通常从字典类型页面跳转进入）
- **菜单状态**: 显示

## 使用流程

1. 进入"字典类型"菜单
2. 点击"新增字典类型"创建字典类型（如：用户性别 sys_user_sex）
3. 在列表中找到创建的字典类型，点击"字典数据"按钮
4. 在字典数据页面添加具体的字典项（如：男、女、未知）
5. 字典数据可以在其他业务模块中使用

## API 接口

### 字典类型接口
- 分页查询: `GET /system/dict/type/page`
- 列表查询: `GET /system/dict/type/list`
- 详情查询: `POST /system/dict/type/getById?dictId={id}`
- 新增: `POST /system/dict/type/add`
- 修改: `POST /system/dict/type/edit`
- 删除: `POST /system/dict/type/remove?dictId={id}`
- 批量删除: `POST /system/dict/type/removeBatch`

### 字典数据接口
- 分页查询: `GET /system/dict/data/page`
- 列表查询: `GET /system/dict/data/list`
- 按类型查询: `GET /system/dict/data/type/{dictType}`
- 详情查询: `POST /system/dict/data/getById?dictCode={id}`
- 新增: `POST /system/dict/data/add`
- 修改: `POST /system/dict/data/edit`
- 删除: `POST /system/dict/data/remove?dictCode={id}`
- 批量删除: `POST /system/dict/data/removeBatch`

## 注意事项

1. 字典类型的 `dictType` 字段在租户内必须唯一
2. 字典数据页面支持从字典类型页面带参数跳转，也支持独立访问
3. 字典数据的 `dictType` 字段关联到字典类型
4. 删除字典类型前，建议先删除该类型下的所有字典数据
