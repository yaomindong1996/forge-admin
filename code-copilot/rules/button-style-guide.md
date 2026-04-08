# 操作按钮样式规范

## UnoCSS颜色类说明

项目使用UnoCSS提供的语义化颜色类，用于操作按钮样式：

| 样式类 | 颜色 | 适用场景 | 使用频率 |
|--------|------|---------|---------|
| `text-primary` | 蓝色 | 主要操作按钮 | 高 |
| `text-info` | 灰蓝色 | 信息查看、次要操作 | 中 |
| `text-success` | 绿色 | 成功、启用操作 | 低 |
| `text-warning` | 黄色/橙色 | 警告、中等操作 | 中 |
| `text-error` | 红色 | 危险操作、删除 | 中 |

## 使用规范

### 1. 主要操作（蓝色 `text-primary`）

**适用场景**：编辑、查看、授权、详情等常规操作

```vue
<a class="text-primary cursor-pointer hover:text-primary-hover">编辑</a>
<a class="text-primary cursor-pointer hover:text-primary-hover">查看</a>
<a class="text-primary cursor-pointer hover:text-primary-hover">授权</a>
```

### 2. 信息操作（灰蓝色 `text-info`）

**适用场景**：查看详情、在线用户、统计信息等

```vue
<a class="text-info cursor-pointer hover:text-info-hover">在线用户</a>
<a class="text-info cursor-pointer hover:text-info-hover">详情</a>
<a class="text-info cursor-pointer hover:text-info-hover">统计</a>
```

### 3. 警告操作（黄色 `text-warning`）

**适用场景**：刷新缓存、重置、封禁等中等操作

```vue
<a class="text-warning cursor-pointer hover:text-warning-hover">刷新缓存</a>
<a class="text-warning cursor-pointer hover:text-warning-hover">重置</a>
<a class="text-warning cursor-pointer hover:text-warning-hover">封禁</a>
```

### 4. 危险操作（红色 `text-error`）

**适用场景**：删除、强制下线、禁用等危险操作

```vue
<a class="text-error cursor-pointer hover:text-error-hover">删除</a>
<a class="text-error cursor-pointer hover:text-error-hover">强制下线</a>
<a class="text-error cursor-pointer hover:text-error-hover">禁用</a>
```

### 5. 成功操作（绿色 `text-success`）

**适用场景**：启用、发布、通过等成功操作

```vue
<a class="text-success cursor-pointer hover:text-success-hover">启用</a>
<a class="text-success cursor-pointer hover:text-success-hover">发布</a>
<a class="text-success cursor-pointer hover:text-success-hover">通过</a>
```

## 实际案例对比

### ❌ 错误示例（不明显）

```vue
<template #table-action="{ row }">
  <a class="text-primary">编辑</a>
  <a class="text-primary">在线用户</a>  <!-- 和编辑一样都是蓝色 -->
  <a class="text-warning">刷新缓存</a>
  <a class="text-error">删除</a>
</template>
```

**问题**：编辑和在线用户都是蓝色，区分不明显

### ✅ 正确示例（明显区分）

```vue
<template #table-action="{ row }">
  <a class="text-primary">编辑</a>        <!-- 蓝色 -->
  <a class="text-info">在线用户</a>      <!-- 灰蓝色 -->
  <a class="text-warning">刷新缓存</a>   <!-- 黄色 -->
  <a class="text-error">删除</a>         <!-- 红色 -->
</template>
```

**效果**：四种操作，四种颜色，一眼就能区分

## 按钮颜色优先级

当一行有多个操作按钮时，按以下优先级分配颜色：

1. **主操作** → `text-primary`（蓝色）
2. **信息操作** → `text-info`（灰蓝色）
3. **警告操作** → `text-warning`（黄色）
4. **危险操作** → `text-error`（红色）

**示例**：

```vue
<!-- 用户管理页面 -->
<a class="text-primary">编辑</a>
<a class="text-primary">授权</a>
<a class="text-primary">组织</a>
<a class="text-warning">重置密码</a>
<a class="text-error">删除</a>

<!-- 客户端管理页面 -->
<a class="text-primary">编辑</a>
<a class="text-info">在线用户</a>
<a class="text-warning">刷新缓存</a>
<a class="text-error">删除</a>

<!-- 在线用户管理页面 -->
<a class="text-info">详情</a>
<a class="text-error">强制下线</a>
<a class="text-warning">封禁</a>
<a class="text-success">解封</a>
```

## Hover效果

所有按钮都应该添加hover效果：

```vue
<!-- 基础写法 -->
<a class="text-primary cursor-pointer hover:text-primary-hover">编辑</a>

<!-- 完整写法（推荐） -->
<a 
  class="text-primary cursor-pointer hover:text-primary-hover"
  @click="handleEdit(row)"
>
  编辑
</a>
```

**Hover类说明**：
- `cursor-pointer` - 鼠标悬停显示手型
- `hover:text-primary-hover` - 鼠标悬停时颜色加深

## 特殊场景

### 1. 条件显示

```vue
<a
  v-if="row.status === 1"
  class="text-warning cursor-pointer hover:text-warning-hover"
>
  禁用
</a>
<a
  v-else
  class="text-success cursor-pointer hover:text-success-hover"
>
  启用
</a>
```

### 2. 权限控制

```vue
<a
  v-if="hasPermission('system:user:edit')"
  class="text-primary cursor-pointer hover:text-primary-hover"
>
  编辑
</a>
```

### 3. 系统保护

```vue
<!-- 超级管理员不能被删除 -->
<a
  v-if="row.id !== 1"
  class="text-error cursor-pointer hover:text-error-hover"
>
  删除
</a>
```

## 颜色对比度建议

根据WCAG无障碍标准，建议：
- 主要操作（蓝色）：`#1890ff`
- 信息操作（灰蓝色）：`#909399`
- 警告操作（黄色）：`#faad14`
- 错误操作（红色）：`#ff4d4f`
- 成功操作（绿色）：`#52c41a`

## 最佳实践总结

✅ **推荐做法**：
- 同一行操作使用不同颜色区分
- 危险操作（删除）使用红色
- 主要操作使用蓝色
- 信息查看使用灰蓝色
- 警告操作使用黄色

❌ **不推荐做法**：
- 所有按钮都用同一种颜色
- 编辑、删除都用蓝色（不区分）
- 颜色使用混乱，没有规律

---

**文档版本**: v1.0  
**更新日期**: 2026-04-08  
**适用项目**: Forge Admin