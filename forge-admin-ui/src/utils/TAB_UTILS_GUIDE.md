# Tab 工具函数使用指南

## 概述

`tab.js` 提供了一套完整的标签页管理工具函数，用于处理页面标签的打开、关闭、刷新等操作。这些函数会自动管理 keep-alive 缓存，确保页面状态正确。

## 导入方式

```javascript
import {
  closeAllPages,
  closeAndOpen,
  closeLeftPages,
  closeOtherPages,
  closePage,
  closePages,
  closeRightPages,
  reloadPage
} from '@/utils'
```

## API 文档

### 1. closePage(target)

关闭指定的 tab 页签，会自动清除对应的 keep-alive 缓存。

**参数：**

- `target` (Object|String|undefined) - 要关闭的页面对象或路径
  - 如果是对象：`{ path: '/system/user', name: 'User' }`
  - 如果是字符串：`'/system/user'`
  - 如果不传：关闭当前页面

**返回：** Promise

**示例：**

```javascript
// 关闭指定路径的 tab
closePage({ path: '/system/dictData' })

// 关闭指定路径（字符串形式）
closePage('/system/dictData')

// 关闭当前 tab
closePage()

// 关闭后执行回调
closePage({ path: '/system/dictData' }).then(() => {
  console.log('页面已关闭')
})
```

### 2. closePages(targets)

批量关闭多个 tab 页签。

**参数：**

- `targets` (Array) - 要关闭的页面数组

**返回：** Promise

**示例：**

```javascript
// 关闭多个页面
closePages([
  { path: '/system/dictData' },
  { path: '/system/user' },
  '/system/role'
]).then(() => {
  console.log('所有页面已关闭')
})
```

### 3. closeAndOpen(closeTarget, openTarget)

关闭指定 tab 并跳转到新页面。这是最常用的方法，用于避免 keep-alive 缓存问题。

**参数：**

- `closeTarget` (Object|String) - 要关闭的页面
- `openTarget` (Object|String) - 要打开的页面

**返回：** Promise

**示例：**

```javascript
// 关闭旧页面并打开新页面（带 query 参数）
closeAndOpen(
  '/system/dictData',
  {
    path: '/system/dictData',
    query: { dictType: 'new_type', dictName: '新类型' }
  }
)

// 简单跳转
closeAndOpen('/old/path', '/new/path')

// 关闭后跳转并执行回调
closeAndOpen('/system/dictData', '/system/dictType').then(() => {
  console.log('跳转完成')
})
```

### 4. reloadPage(path)

刷新指定的 tab 页签。

**参数：**

- `path` (String|undefined) - 页面路径，不传则刷新当前页面

**返回：** Promise

**示例：**

```javascript
// 刷新当前页面
reloadPage()

// 刷新指定页面
reloadPage('/system/user')
```

### 5. closeOtherPages(path)

关闭其他 tab 页签，保留指定页面。

**参数：**

- `path` (String|undefined) - 要保留的页面路径，不传则保留当前页面

**返回：** Promise

**示例：**

```javascript
// 关闭除当前页面外的所有 tab
closeOtherPages()

// 关闭除指定页面外的所有 tab
closeOtherPages('/system/user')
```

### 6. closeLeftPages(path)

关闭左侧的 tab 页签。

**参数：**

- `path` (String|undefined) - 基准页面路径，不传则以当前页面为基准

**返回：** Promise

**示例：**

```javascript
// 关闭当前页面左侧的所有 tab
closeLeftPages()

// 关闭指定页面左侧的所有 tab
closeLeftPages('/system/user')
```

### 7. closeRightPages(path)

关闭右侧的 tab 页签。

**参数：**

- `path` (String|undefined) - 基准页面路径，不传则以当前页面为基准

**返回：** Promise

**示例：**

```javascript
// 关闭当前页面右侧的所有 tab
closeRightPages()

// 关闭指定页面右侧的所有 tab
closeRightPages('/system/user')
```

### 8. closeAllPages()

关闭所有 tab 页签并跳转到首页。

**返回：** Promise

**示例：**

```javascript
// 关闭所有页面
closeAllPages().then(() => {
  console.log('已返回首页')
})
```

## 实际应用场景

### 场景 1：字典管理 - 避免缓存问题

当从字典类型列表点击"字典数据"按钮时，需要关闭之前打开的字典数据页面，避免显示旧数据：

```javascript
// dictType.vue
function handleManageData(row) {
  closeAndOpen(
    '/system/dictData',
    {
      path: '/system/dictData',
      query: {
        dictType: row.dictType,
        dictName: row.dictName
      }
    }
  )
}
```

### 场景 2：表单提交后跳转

提交表单后关闭当前页面并返回列表：

```javascript
async function handleSubmit() {
  await submitForm()
  closePage().then(() => {
    router.push('/list')
  })
}
```

### 场景 3：级联关闭

关闭多个相关页面后跳转：

```javascript
function handleBackToList() {
  closePages([
    '/case/caseCheck',
    '/case/caseDetail'
  ]).then(() => {
    router.push('/case/caseManageList')
  })
}
```

### 场景 4：刷新页面

点击刷新按钮时：

```javascript
function handleRefresh() {
  reloadPage()
}
```

## 注意事项

1. **路径匹配**：`closePage` 支持前缀匹配，可以关闭所有以指定路径开头的 tab
2. **缓存清理**：所有关闭操作都会自动清除 keep-alive 缓存
3. **异步操作**：所有方法都返回 Promise，可以使用 `.then()` 或 `async/await`
4. **当前路由**：关闭当前页面时会自动跳转到相邻的 tab

## 与旧版本对比

### 旧版本（Vue 2 + Vuex）

```javascript
// 旧版本
this.$tab.closePage(obj).then(() => {
  this.$router.push('/new/path')
})
```

### 新版本（Vue 3 + Pinia）

```javascript
// 新版本
import { closePage } from '@/utils'

closePage(obj).then(() => {
  router.push('/new/path')
})

// 或者使用更简洁的 closeAndOpen
closeAndOpen(obj, '/new/path')
```

## 技术实现

这些工具函数基于：

- **Pinia Store**：`useTabStore()` 管理标签状态
- **Vue Router**：`useRouter()` 处理路由跳转
- **Keep-Alive**：通过 `cacheViews` 数组管理缓存

当关闭 tab 时，会同时：

1. 从 `tabs` 数组中移除标签
2. 从 `cacheViews` 数组中移除缓存名称
3. 更新 sessionStorage
4. 如果关闭的是当前页面，自动跳转到相邻页面
