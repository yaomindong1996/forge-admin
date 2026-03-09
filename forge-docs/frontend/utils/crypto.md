# 加密工具

提供 MD5、Base64、SM4、AES、RSA 等加密方法。

## 基础加密

```js
import { crypto, md5Encrypt, base64Encode, base64Decode, randomString } from '@/utils/crypto'

// MD5 加密
const hash = md5Encrypt('password123')

// Base64 编解码
const encoded = base64Encode('hello')
const decoded = base64Decode(encoded)

// 随机字符串
const str = randomString(16)
```

## 对称加密

### SM4 加密

```js
import { sm4Encrypt, sm4Decrypt } from '@/utils/crypto'

const encrypted = sm4Encrypt('敏感数据', 'secretKey')
const decrypted = sm4Decrypt(encrypted, 'secretKey')
```

### AES 加密

```js
import { aesEncrypt, aesDecrypt } from '@/utils/crypto'

const encrypted = aesEncrypt('敏感数据', 'secretKey')
const decrypted = aesDecrypt(encrypted, 'secretKey')
```

## 非对称加密

```js
import { rsaEncrypt, rsaDecrypt } from '@/utils/crypto'

// RSA 加密（公钥）
const encrypted = rsaEncrypt('敏感数据', publicKey)

// RSA 解密（私钥）
const decrypted = rsaDecrypt(encrypted, privateKey)
```

## 密钥交换

```js
import { 
  initKeyExchange, 
  getSessionKey, 
  isKeyExchanged,
  resetKeyExchange 
} from '@/utils/crypto'

// 初始化密钥交换
await initKeyExchange()

// 获取会话密钥
const sessionKey = getSessionKey()

// 检查是否已交换
if (isKeyExchanged()) {
  // 可以进行加密通信
}

// 重置密钥交换
resetKeyExchange()
```

## 加密请求

```js
import { encryptRequest, decryptResponse, shouldEncrypt } from '@/utils/crypto'

// 检查是否需要加密
if (shouldEncrypt(url)) {
  const encrypted = encryptRequest(data)
}

// 解密响应
const decrypted = decryptResponse(response)
```

## API 参考

| 函数 | 参数 | 说明 |
|------|------|------|
| md5Encrypt | data | MD5 加密 |
| base64Encode | data | Base64 编码 |
| base64Decode | data | Base64 解码 |
| randomString | length | 生成随机字符串 |
| sm4Encrypt | data, key | SM4 加密 |
| sm4Decrypt | data, key | SM4 解密 |
| aesEncrypt | data, key | AES 加密 |
| aesDecrypt | data, key | AES 解密 |
| rsaEncrypt | data, publicKey | RSA 加密 |
| rsaDecr`ypt | data, privateKey | RSA 解密 |
