# HTTP 请求封装

基于 Axios 封装的 HTTP 请求工具，支持拦截器、错误处理。

## 基础用法

```js
import { request } from '@/utils/http'

// GET 请求
const res = await request.get('/api/user/list', {
  params: { page: 1, size: 10 }
})

// POST 请求
const res = await request.post('/api/user', {
  name: '张三',
  age: 25
})

// PUT 请求
const res = await request.put('/api/user/1', data)

// DELETE 请求
const res = await request.delete('/api/user/1')
```

## 完整配置

```js
const res = await request({
  url: '/api/user',
  method: 'post',
  data: { name: '张三' },
  params: { type: 1 },    // URL 参数
  headers: {},            // 自定义请求头
  timeout: 10000          // 超时时间
})
```

## 创建自定义实例

```js
import { createAxios } from '@/utils/http'

const customRequest = createAxios({
  baseURL: 'https://api.example.com',
  timeout: 5000
})
```

## 内置实例

| 实例 | 说明 |
|------|------|
| request | 默认实例，带 VITE_REQUEST_PREFIX 前缀 |
| noPrefixRequest | 无前缀实例 |
| mockRequest | Mock 数据实例 |

## 拦截器

请求拦截器自动添加 Token：

```js
// 自动添加 Authorization 头
headers: {
  Authorization: 'Bearer xxx'
}
```

响应拦截器自动处理：

- 统一错误提示
- Token 过期自动刷新
- 请求加密/解密

## 错误处理

```js
try {
  const res = await request.get('/api/user')
} catch (error) {
  if (error.response) {
    // 服务器返回错误
    console.log(error.response.status)
    console.log(error.response.data)
  } else if (error.request) {
    // 请求发送但无响应
    console.log('网络错误')
  } else {
    // 请求配置错误
    console.log(error.message)
  }
}
```

## 加密请求

```js
import { postEncrypt } from '@/utils/encrypt-request'

// 使用加密 POST 请求
const res = await postEncrypt('/api/user', data)
```