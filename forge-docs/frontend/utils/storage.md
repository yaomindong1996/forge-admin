# 本地存储封装

支持过期时间的本地存储封装。

## 基础用法

```js
import { createStorage } from '@/utils/storage'

// 创建 localStorage 实例
const localStore = createStorage({
  prefixKey: 'app_',
  storage: localStorage
})

// 创建 sessionStorage 实例
const sessionStore = createStorage({
  prefixKey: 'app_',
  storage: sessionStorage
})
```

## API 参考

### set(key, value, expire?)

存储数据，支持设置过期时间（秒）。

```js
// 永久存储
localStore.set('token', 'xxx')

// 设置过期时间（秒）
localStore.set('userInfo', { name: '张三' }, 3600)  // 1 小时后过期
```

### get(key)

获取数据，自动处理过期。

```js
const token = localStore.get('token')
```

### getItem(key)

获取完整数据（包含时间信息）。

```js
const { value, time } = localStore.getItem('userInfo')
```

### remove(key)

删除数据。

```js
localStore.remove('token')
```

### clear()

清空所有数据。

```js
localStore.clear()
```

## 示例

```js
// 封装 Token 存储
const tokenStore = createStorage({
  prefixKey: 'auth_',
  storage: localStorage
})

// 存储 Token
export function setToken(token) {
  tokenStore.set('token', token, 86400)  // 1 天过期
}

// 获取 Token
export function getToken() {
  return tokenStore.get('token')
}

// 清除 Token
export function clearToken() {
  tokenStore.remove('token')
}
```