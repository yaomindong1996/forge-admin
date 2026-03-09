# 前端工具函数概览

## 工具分类

| 模块 | 说明 |
|------|------|
| [common](./common.md) | 通用工具函数 |
| [http](./http.md) | HTTP 请求封装 |
| [file](./file.md) | 文件处理工具 |
| [storage](./storage.md) | 本地存储封装 |
| [crypto](./crypto.md) | 加密工具 |
| [dict](./dict.md) | 字典管理 |

## 使用方式

```js
// 按需导入
import { formatDateTime, debounce } from '@/utils/common'
import { request } from '@/utils/http'
import { getFileUrl } from '@/utils/file'

// 或统一导入
import { formatDateTime, request, getFileUrl } from '@/utils'
```