# 加密请求（encrypt-request）

加密 API 请求工具，对请求体进行 AES + RSA 加密传输。

## 使用方式

```js
import { postEncrypt } from '@/utils/encrypt-request'

// 加密 POST 请求
postEncrypt('/api/user/sensitive', {
  phone: '13800138000',
  idCard: '110101199001011234'
}).then(res => {
  console.log(res.data)
})
```

## 加密流程

1. 前端生成随机 AES Key
2. 使用 AES Key 加密请求体
3. 使用公钥加密 AES Key
4. 将加密后的数据和 AES Key 一起发送到后端
5. 后端使用私钥解密 AES Key，再用 AES Key 解密请求体
