# 中后台系统的 5 个安全裸奔点，你中了几个？

> 一次安全审计暴露的 5 类敏感信息泄露，以及我们的修复方案。附可复用的脱敏工具类和 Flyway 迁移脚本。

## 一、一次安全审计，5 个裸奔点

上个月，安全团队对我们基于 Spring Boot + Vue3 的中后台管理框架做了一次渗透测试。结果不算太惨，但也不好看——5 类敏感信息在接口层面直接暴露。

不是什么高深的攻击手法，就是普通的 API 调用，连 SQL 注入都不需要。

以下是 5 个裸奔点的完整清单，每个都包含：**问题现象 → 攻击场景 → 代码修复**。如果你的系统也有类似问题，可以直接抄作业。

---

## 二、裸奔点 1：系统配置接口返回密码明文

### 问题现象

`GET /system/config/page` 返回系统参数列表时，`sys.user.initPassword` 的值是明文：

```json
{
  "configKey": "sys.user.initPassword",
  "configValue": "123456",     // ← 明文暴露
  "configName": "用户初始密码"
}
```

### 攻击场景

- 内部管理员通过接口获取默认密码，用于撞库攻击
- 接口被爬虫抓取，密码泄露到公网
- 新员工入职，系统分配的初始密码就是这个，所有人都能查到

### 修复方案

**核心思路**：识别敏感配置键，返回时用 `******` 替换。

**Step 1：敏感键判断工具类**

```java
public class SensitiveDataUtil {

    private static final List<String> SENSITIVE_KEYWORDS = List.of(
        "password", "secret", "token", "key", "credential",
        "ak", "sk", "accesskey", "secretkey"
    );

    /**
     * 判断配置键是否包含敏感关键字
     */
    public static boolean isSensitiveKey(String configKey) {
        if (configKey == null) return false;
        String lower = configKey.toLowerCase();
        return SENSITIVE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * 对敏感配置值脱敏
     */
    public static String maskValue(String configKey, String configValue) {
        if (!isSensitiveKey(configKey) || configValue == null) {
            return configValue;
        }
        return "******";
    }
}
```

**Step 2：Controller 层脱敏拷贝**

```java
@GetMapping("/page")
public RespInfo<SysConfigVO> page(SysConfigQueryDTO query) {
    IPage<SysConfigVO> page = configService.page(query);

    // 对敏感配置值脱敏
    page.getRecords().forEach(vo -> {
        vo.setConfigValue(SensitiveDataUtil.maskValue(
            vo.getConfigKey(), vo.getConfigValue()
        ));
    });

    return RespInfo.success(page);
}
```

**Step 3：保存时跳过脱敏占位值**

防止前端把 `******` 当作真实值保存到数据库：

```java
public void updateConfig(SysConfig config) {
    // 如果前端提交的是脱敏占位值，不更新原值
    if ("******".equals(config.getConfigValue()) && SensitiveDataUtil.isSensitiveKey(config.getConfigKey())) {
        config.setConfigValue(null); // 不更新 configValue
    }
    // 只更新其他字段
    updateById(config);
}
```

---

## 三、裸奔点 2：用户接口暴露密码哈希和 PII

### 问题现象

`GET /system/user/page` 返回用户列表时，包含了密码哈希、盐值、手机号明文、身份证明文：

```json
{
  "username": "zhangsan",
  "password": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi",
  "salt": "abc123",
  "phone": "13812345678",
  "idCard": "110101199001011234",
  "email": "zhangsan@example.com"
}
```

### 攻击场景

- **彩虹表破解**：拿到密码哈希 + 盐值，离线暴力破解
- **PII 泄露**：手机号、身份证号直接暴露，隐私合规风险（GDPR/个人信息保护法）
- **社工攻击**：手机号 + 身份证号 = 可以做很多事

### 修复方案

**Step 1：@JsonIgnore 排除认证字段**

在实体类上直接排除 `password` 和 `salt`：

```java
public class SysUser {
    private Long id;
    private String username;

    @JsonIgnore  // 所有 JSON 响应都不包含此字段
    private String password;

    @JsonIgnore
    private String salt;

    private String phone;
    private String idCard;
    private String email;
}
```

**Step 2：PII 字段脱敏**

```java
public class SensitiveDataUtil {

    /**
     * 手机号脱敏：13812345678 → 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证脱敏：110101199001011234 → 110101****1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 6) + "****" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 邮箱脱敏：zhangsan@example.com → zha****@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, Math.min(3, atIndex));
        return prefix + "****" + email.substring(atIndex);
    }
}
```

**Step 3：列表接口脱敏**

```java
@GetMapping("/page")
public RespInfo<SysUserVO> page(SysUserQueryDTO query) {
    IPage<SysUserVO> page = userService.page(query);

    page.getRecords().forEach(vo -> {
        vo.setPhone(SensitiveDataUtil.maskPhone(vo.getPhone()));
        vo.setIdCard(SensitiveDataUtil.maskIdCard(vo.getIdCard()));
        vo.setEmail(SensitiveDataUtil.maskEmail(vo.getEmail()));
    });

    return RespInfo.success(page);
}
```

**设计决策**：列表脱敏，详情不脱敏（但清空认证字段）。因为详情页用户可能需要编辑手机号/邮箱，脱敏后无法回显。详情接口的做法是清空 `password` 和 `salt`，保留原始 `phone` / `email`。

---

## 四、裸奔点 3：缓存管理可枚举敏感键

### 问题现象

`GET /system/cache/page` 返回 Redis 缓存键列表时，包含了：

```json
{
  "key": "satoken:login:token:abc123def456",
  "valuePreview": "{\"userId\":1,\"loginTime\":\"2026-06-15\"}"
}
```

以及 `crypto:session:*`、`Authorization:login:token:*` 等敏感键。

### 攻击场景

- 管理员账号被盗 → 通过缓存接口枚举所有 Token → 伪造会话
- 读取加密会话密钥 → 解密用户数据
- 删除敏感缓存 → 导致用户大面积掉线

### 修复方案

**核心思路**：定义敏感缓存键前缀，列表过滤 + 操作拦截。

```java
@RestController
@RequestMapping("/system/cache")
public class SysCacheController {

    // 敏感缓存键前缀
    private static final List<String> SENSITIVE_KEY_PREFIXES = List.of(
        "crypto:session:",
        "Authorization:login:token:",
        "Authorization:login:token-session:",
        "Authorization:login:last-active:",
        "satoken:",
        "auth:sso:"
    );

    /**
     * 判断是否为敏感缓存键
     */
    private boolean isSensitiveKey(String key) {
        return SENSITIVE_KEY_PREFIXES.stream().anyMatch(key::startsWith);
    }

    @GetMapping("/page")
    public RespInfo page(CacheQueryDTO query) {
        List<CacheVO> allKeys = redisService.scanKeys(query.getPattern());

        // 过滤敏感键
        List<CacheVO> safeKeys = allKeys.stream()
            .filter(vo -> !isSensitiveKey(vo.getKey()))
            .collect(Collectors.toList());

        return RespInfo.success(PageUtils.paginate(safeKeys, query));
    }

    @GetMapping("/getInfo")
    public RespInfo getInfo(String key) {
        if (isSensitiveKey(key)) {
            return RespInfo.error("该缓存键不允许查看");
        }
        return RespInfo.success(redisService.getCacheInfo(key));
    }

    @DeleteMapping("/remove")
    public RespInfo remove(String key) {
        if (isSensitiveKey(key)) {
            return RespInfo.error("该缓存键不允许删除");
        }
        redisService.delete(key);
        return RespInfo.success();
    }
}
```

---

## 五、裸奔点 4：文件存储配置暴露 AK/SK

### 问题现象

`GET /system/storage/config/default` 返回默认存储配置时，包含了完整的云存储凭据：

```json
{
  "storageType": "ALI_OSS",
  "endpoint": "oss-cn-hangzhou.aliyuncs.com",
  "accessKey": "LTAI5tFakeKey123456",     // ← 明文
  "secretKey": "FakeSecretKey7890123456",  // ← 明文
  "bucket": "my-bucket"
}
```

### 攻击场景

- 接口泄露 → 拿到 AK/SK → 直接操作云存储
- 上传恶意文件、删除业务数据、产生高额流量费用

### 修复方案

**Step 1：默认接口只返回安全元数据**

```java
@GetMapping("/config/default")
public RespInfo getDefaultConfig() {
    SysFileStorageConfig config = storageConfigService.getDefaultConfig();

    // 只返回上传组件需要的安全字段
    SafeStorageConfigVO safe = new SafeStorageConfigVO();
    safe.setId(config.getId());
    safe.setConfigName(config.getConfigName());
    safe.setStorageType(config.getStorageType());
    safe.setBucket(config.getBucket());
    safe.setBasePath(config.getBasePath());
    // 不返回 endpoint、accessKey、secretKey、domain

    return RespInfo.success(safe);
}
```

**Step 2：管理接口 AK/SK 脱敏**

```java
@GetMapping("/config/page")
public RespInfo page(StorageConfigQueryDTO query) {
    IPage<SysFileStorageConfigVO> page = storageConfigService.page(query);

    page.getRecords().forEach(vo -> {
        vo.setAccessKey(SensitiveDataUtil.maskAccessKey(vo.getAccessKey()));
        vo.setSecretKey(SensitiveDataUtil.maskSecretKey(vo.getSecretKey()));
    });

    return RespInfo.success(page);
}
```

**Step 3：保存时脱敏占位值不覆盖**

```java
public void updateConfig(SysFileStorageConfig config) {
    if ("******".equals(config.getAccessKey())) {
        config.setAccessKey(null); // 不更新
    }
    if ("******".equals(config.getSecretKey())) {
        config.setSecretKey(null); // 不更新
    }
    updateById(config);
}
```

---

## 六、裸奔点 5：文件上传无类型校验

### 问题现象

`POST /api/file/upload` 允许上传任意文件类型，包括 `.jsp`、`.php`、`.html`、`.js`：

```bash
curl -X POST http://localhost:8580/api/file/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@shell.jsp"
# → 上传成功，返回文件 URL
```

### 攻击场景

- 上传 Webshell（`.jsp`、`.php`）→ 服务器被控制
- 上传 HTML 页面 → 钓鱼攻击
- 上传 `.js` 文件 → XSS 攻击

### 修复方案

在 `FileManager` 中统一拦截：

```java
public class FileManager {

    // 高风险扩展名黑名单
    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
        "jsp", "jspx", "php", "asp", "aspx",
        "html", "htm", "js", "mjs", "ts", "vue",
        "sh", "bat", "cmd", "exe", "dll", "jar", "war", "ear", "sql"
    );

    // 默认安全白名单（当存储配置未配置 allowedTypes 时使用）
    private static final Set<String> DEFAULT_SAFE_TYPES = Set.of(
        "jpg", "jpeg", "png", "gif", "webp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "csv", "zip", "rar", "mp4", "mp3"
    );

    /**
     * 校验文件安全性
     */
    public void validateFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        String extension = getExtension(originalFilename).toLowerCase();

        // 1. 黑名单检查
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不允许上传 ." + extension + " 类型文件");
        }

        // 2. 白名单检查（如果配置了 allowedTypes）
        Set<String> allowedTypes = getAllowedTypes();
        if (!allowedTypes.isEmpty() && !allowedTypes.contains(extension)) {
            throw new BusinessException("文件类型 ." + extension + " 不在允许列表中");
        }

        // 3. MIME 类型基础匹配
        String contentType = file.getContentType();
        if (contentType != null && isDangerousMime(contentType)) {
            throw new BusinessException("文件 MIME 类型不安全: " + contentType);
        }
    }

    private boolean isDangerousMime(String contentType) {
        return contentType.startsWith("text/html")
            || contentType.startsWith("application/x-javascript")
            || contentType.startsWith("application/x-httpd-php")
            || contentType.startsWith("application/jsp");
    }
}
```

**Flyway 迁移脚本**：回填默认安全白名单到存储配置表：

```sql
-- V1.0.68__harden_file_upload_defaults.sql
UPDATE sys_file_storage_config
SET allowed_types = 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3'
WHERE allowed_types IS NULL OR allowed_types = '';
```

---

## 七、附录：强制改密机制

除了上面 5 个接口层面的裸奔点，我们还加了一个**业务层面**的安全机制：强制改密。

### 场景

- 用户使用初始密码 `123456` 登录
- 管理员重置了用户密码
- 安全审计发现弱密码账号

这些场景下，用户下次登录应该**强制修改密码**，否则不能访问系统。

### 实现

**Step 1：数据库加字段**

```sql
ALTER TABLE sys_user ADD COLUMN force_password_change TINYINT(1) NOT NULL DEFAULT 0;
```

**Step 2：登录时标记**

```java
// 新增用户时
user.setForcePasswordChange(1);

// 管理员重置密码时
user.setPassword(newPassword);
user.setForcePasswordChange(1);

// 用户自行改密成功后
user.setForcePasswordChange(0);
```

**Step 3：API 拦截**

在 `ApiPermissionInterceptor` 中，登录后先检查强制改密标记：

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    LoginUser loginUser = StpUtil.getLoginUser(LoginUser.class);

    if (loginUser.getForcePasswordChange()) {
        String path = request.getRequestURI();

        // 只允许访问这几个接口
        Set<String> allowedPaths = Set.of(
            "/auth/userInfo",
            "/auth/changePassword",
            "/auth/logout"
        );

        if (!allowedPaths.contains(path)) {
            throw new BusinessException("请先修改初始密码");
        }
    }

    return true;
}
```

**Step 4：前端路由守卫**

```javascript
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (userStore.forcePasswordChange && to.path !== '/profile') {
    // 强制跳转到个人中心改密
    ElMessage.warning('请先修改初始密码')
    next('/profile')
  } else {
    next()
  }
})
```

---

## 八、总结：中后台安全基线清单

以下 checklist 可直接用于代码审查：

```
接口安全审查清单
───────────────────────────────────────────

□ 系统配置接口
  └ 是否返回 password/secret/token/key 明文？

□ 用户接口
  └ password/salt 是否用 @JsonIgnore 排除？
  └ 手机号/身份证号/邮箱是否脱敏？

□ 缓存管理接口
  └ 是否枚举敏感缓存键？
  └ 是否允许读取/删除 Token 和会话密钥？

□ 文件存储配置接口
  └ AK/SK 是否脱敏？
  └ 默认接口是否只返回安全元数据？

□ 文件上传接口
  └ 是否校验文件扩展名？
  └ 是否拒绝 .jsp/.php/.html/.js 等高风险类型？
  └ 是否有默认安全白名单？

□ 密码安全
  └ 初始密码/重置密码后是否标记强制改密？
  └ 强制改密期间是否限制 API 访问范围？

□ 数据库迁移
  └ 敏感配置是否清空默认值？
  └ 上传白名单是否回填？
```

---

## 写在最后

安全这件事，最怕的不是被黑，而是**不知道自己在裸奔**。

上面 5 个裸奔点，没有一个需要高深的攻击手法。一个有权限的内部用户，甚至一个被泄露的管理员账号，就能通过普通 API 调用拿到系统里所有的密码、Token 和云存储凭据。

好消息是，修复成本不高。一个 `SensitiveDataUtil` 工具类 + 几个 Controller 层的脱敏调用 + 一个 Flyway 迁移脚本，就能把这 5 个洞堵上。

如果你的系统也有类似问题，希望这篇文章能帮你快速修复。代码已开源在 [Forge Admin](https://github.com/mdframe/forge-project)，欢迎参考。
