# 加解密模块

提供 RSA、AES、SM4 等加解密能力。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-crypto</artifactId>
</dependency>
```

## 配置

```yaml
forge:
  crypto:
    enabled: true
    algorithm: SM4    # RSA | AES | SM4
    secret: your-secret-key
```

## 使用示例

### 对称加密

```java
@Autowired
private CryptoService cryptoService;

// 加密
String encrypted = cryptoService.encrypt("敏感数据");

// 解密
String decrypted = cryptoService.decrypt(encrypted);
```

### RSA 非对称加密

```java
// 生成密钥对
KeyPair keyPair = cryptoService.generateKeyPair();
String publicKey = cryptoService.getPublicKey(keyPair);
String privateKey = cryptoService.getPrivateKey(keyPair);

// 公钥加密
String encrypted = cryptoService.encryptByPublicKey("数据", publicKey);

// 私钥解密
String decrypted = cryptoService.decryptByPrivateKey(encrypted, privateKey);
```

### 签名验证

```java
// 签名
String signature = cryptoService.sign("数据", privateKey);

// 验签
boolean valid = cryptoService.verify("数据", signature, publicKey);
```

## 加密算法

| 算法 | 类型 | 说明 |
|------|------|------|
| RSA | 非对称 | 适合小数据加密、签名 |
| AES | 对称 | 高效，适合大数据加密 |
| SM4 | 对称 | 国密算法，合规场景 |
| MD5 | 哈希 | 不可逆，适合摘要 |
| SHA256 | 哈希 | 不可逆，安全性更高 |

## 请求加密

```java
@PostMapping("/secure")
public Result secureRequest(@RequestBody EncryptedRequest request) {
    // 自动解密请求体
    String data = cryptoService.decrypt(request.getData());
    
    // 处理业务
    String result = process(data);
    
    // 加密响应
    String encrypted = cryptoService.encrypt(result);
    return Result.success(encrypted);
}
```

## 数据库字段加密

```java
@Entity
public class User {
    
    @Convert(converter = EncryptedStringConverter.class)
    private String idCard;  // 自动加解密
}
```

## 注意事项

1. 密钥不要硬编码在代码中
2. RSA 加密数据长度有限制
3. 敏感数据建议使用国密算法
4. 密钥定期更换