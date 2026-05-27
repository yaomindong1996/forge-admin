# forge-starter-file 模块深度分析

> 分析时间：2026-05-27  
> 模块路径：`forge/forge-framework/forge-starter-parent/forge-starter-file`  
> 博客目标：B05《文件访问不能只返回 URL：Forge Admin 鉴权图片和文件存储设计》

---

## 一、模块整体架构

```
forge-starter-file/
├── config/
│   ├── FileAutoConfiguration.java   # 自动配置入口
│   └── FileStorageProperties.java   # 配置属性 (forge.file.*)
├── core/
│   └── FileManager.java             # 核心管理器（统一调度）
├── controller/
│   └── FileController.java          # 通用文件 REST API
├── enums/
│   └── StorageType.java            # 存储类型枚举
├── model/
│   ├── FileMetadata.java            # 文件元数据模型
│   └── StorageConfig.java          # 存储配置模型
├── spi/
│   ├── FileMetadataPersistence.java # 元数据持久化 SPI
│   └── StorageConfigProvider.java  # 存储配置提供者 SPI
├── storage/
│   ├── FileStorage.java            # 存储策略接口（SPI）
│   └── impl/
│       ├── LocalFileStorage.java    # 本地文件系统实现
│       ├── TencentCosFileStorage.java # 腾讯云 COS 实现
│       └── RustfsFileStorage.java  # RustFS(S3兼容) 实现
└── util/
    └── FileUtil.java               # 文件工具类
```

**设计思想**：策略模式 + SPI 插件化。存储实现可插拔，业务系统通过 SPI 接口（`FileMetadataPersistence`、`StorageConfigProvider`）接入自身的数据持久化。

---

## 二、支持的存储方式及切换机制

### 2.1 已支持的存储类型

| 存储类型 | code | 实现类 | 协议/方式 |
|---------|------|--------|-----------|
| 本地文件系统 | `local` | `LocalFileStorage` | 本地磁盘 IO |
| 腾讯云 COS | `tencent` | `TencentCosFileStorage` | COS SDK |
| RustFS | `rustfs` | `RustfsFileStorage` | AWS S3 SDK v2 (S3 兼容) |
| MinIO | `minio` | — | 预留枚举，未实现 |
| 阿里云 OSS | `aliyun` | — | 预留枚举，未实现 |
| 七牛云 | `qiniu` | — | 预留枚举，未实现 |

### 2.2 存储切换方式

**方式一：通过配置文件切换默认存储**

```yaml
# application.yml
forge:
  file:
    default-storage-type: tencent   # local / tencent / rustfs
```

`FileAutoConfiguration` 启动时调用 `refreshConfiguredStorages()`，从数据库加载所有 `enabled=1` 的配置记录，逐一调用 `storage.init(config)` 初始化。

**方式二：上传时指定存储类型**

```java
// Controller 层
@PostMapping("/upload")
public RespInfo<FileMetadata> upload(
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "storageType", required = false) String storageType,
    ...
) {
    FileMetadata metadata = fileManager.upload(file, businessType, businessId, storageType, isPrivate);
    return RespInfo.success(metadata);
}
```

前端通过 `storageType` 参数指定，后端 `FileManager` 根据 `storageType` 从 `storageMap` 路由到对应实现。

**方式三：数据库动态配置（推荐）**

`sys_file_storage_config` 表可配置多条存储记录，通过 `is_default` 字段标记默认策略。`SystemStorageConfigProvider` 实现从数据库读取配置。

---

## 三、文件上传完整流程

### 3.1 时序图

```
[前端] --(MultipartFile + token 请求头)--> [FileController.upload()]
    |
    v
[FileManager.upload()]
    |
    +-- 1. 确定存储类型（参数 → 默认配置 → 异常）
    +-- 2. 验证文件（大小、类型，通过 StorageConfigProvider 获取配置）
    +-- 3. 秒传检查（计算 MD5，查库是否存在相同文件 + 相同 businessType/businessId）
    +-- 4. 调用 FileStorage.upload() 执行实际上传
    +-- 5. 构建 FileMetadata（fileId、路径、大小等）
    +-- 6. 持久化元数据（FileMetadataPersistence.save()）
    |
    v
[返回 FileMetadata JSON 给前端]
```

### 3.2 关键代码路径

**Controller** (`FileController.java`)：
```java
@PostMapping("/upload")
public RespInfo<FileMetadata> upload(...) {
    // 非私有文件只有管理员可上传
    if (!isPrivate && !StpUtil.hasPermission("*:*:*")) {
        return RespInfo.error("只有管理员才能上传公共素材");
    }
    FileMetadata metadata = fileManager.upload(file, businessType, businessId, storageType, isPrivate);
    return RespInfo.success(metadata);
}
```

**FileManager** (`FileManager.java`)：
- 秒传逻辑：`metadataPersistence.getByMd5(md5)` 且 `businessType/businessId` 一致时才复用
- 文件大小/类型校验：从 `StorageConfigProvider` 获取当前存储类型的 `maxFileSize` 和 `allowedTypes`

**存储实现**（`LocalFileStorage.java` 示例）：
```java
public FileMetadata upload(MultipartFile file, String businessType, String businessId) {
    String storageName = generateStorageName(fileName);      // UUID + 扩展名
    String relativePath = generateRelativePath(businessType); // {businessType}/yyyy/MM/dd/
    Files.copy(inputStream, targetFile.toPath());
    return FileMetadata.builder().fileId(UUID).originalName(...).build();
}
```

---

## 四、AuthImage 鉴权图片实现

### 4.1 问题背景

如果后端直接返回文件的真实 URL（如 `https://cos.xxx.com/avatar/xxx.jpg`），前端 `<img src="...">` 请求该 URL 时**不会携带认证 Token**，导致私有文件无法访问或需要公开 bucket，存在安全隐患。

### 4.2 AuthImage 解决方案

`AuthImage.vue` 是一个封装过的 `<img>` 组件，**核心思路：不直接使用外链 URL，而是通过后端接口代理访问**。

**组件核心逻辑** (`AuthImage.vue`)：

```vue
<script setup>
import { resolveRenderableFileUrl } from '@/utils/file.js'

async function loadImage() {
  // 1. 调用 resolveRenderableFileUrl 解析文件地址
  const url = await resolveRenderableFileUrl(props.src, props.expires)
  // 2. 如果是内部文件接口，fetch 时手动带 Authorization 头，拿到 blob 后转成 blob URL
  // 3. 如果是外部直链，直接赋值
  imageSrc.value = url
}
</script>

<template>
  <img :src="imageSrc" @error="handleError" />
</template>
```

**使用方式**：
```vue
<!-- 传入 fileId，AuthImage 会自动解析成可访问的地址 -->
<AuthImage :src="fileId" :fallback="/default-avatar.png" />
```

### 4.3 `resolveRenderableFileUrl` 详解

这是整个鉴权访问的核心工具函数 (`file.js`)：

```js
// 1. 如果是外部直链 / data: / blob: ，直接返回
if (isDirectFilePath(rawValue)) {
  return normalizeFileAccessUrl(rawValue)
}

// 2. 否则认为是 fileId，调用后端 /api/file/url/{fileId} 获取带签名的临时 URL
const result = await request({
  url: `/api/file/url/${rawValue}`,
  method: 'get',
  params: { expires },  // 默认 43200 秒（12小时）
  needTip: false,
})

// 3. 如果是内部接口返回的相对路径（如 /api/file/download/{fileId}），
//    则通过 fetch + 手动携带 Authorization 头获取文件流，转成 blob URL
if (isInternalFileUrl(url)) {
  const response = await fetch(url, { headers: getAuthHeaders() })
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}
```

**鉴权请求头构造**：
```js
function getAuthHeaders() {
  const authStore = useAuthStore()
  return {
    'X-Timestamp': Date.now().toString(),
    'X-Nonce': generateUUID(),
    'Authorization': `Bearer ${authStore.accessToken}`,
  }
}
```

### 4.4 缓存机制

为避免每次渲染都请求后端签URL接口，实现了**两级缓存**：

| 缓存层 | 实现 | 有效期 |
|--------|------|--------|
| 内存缓存 | `fileUrlMemoryCache` (Map) | 同一次页面会话 |
| localStorage 缓存 | `FILE_URL_CACHE_KEY` | 12小时（可配置） |

缓存 key 为 `fileId`，value 包含 `url` 和 `expiresAt`。过期后自动清除并重新请求。

---

## 五、getFileUrl 的实现

### 5.1 后端：`FileManager.getAccessUrl()`

```java
public String getAccessUrl(String fileId, Integer expires) {
    FileMetadata metadata = metadataPersistence.getById(fileId);
    FileStorage storage = getStorage(metadata.getStorageType());
    // 委托给具体存储实现生成访问 URL
    return storage.getAccessUrl(fileId, expires);
}
```

### 5.2 各存储实现的 URL 生成策略

**本地存储** (`LocalFileStorage`)：
```java
public String getAccessUrl(String fileId, Integer expires) {
    // 本地文件无法通过预签名 URL 访问，返回后端下载接口地址
    return "/api/file/download/" + fileId;
    // 如果配置了 domain，则返回: domain + "/api/file/download/" + fileId
}
```
> 本地存储的文件必须通过 `FileController.download()` 接口访问，由后端读取文件流并写入 response。

**腾讯云 COS** (`TencentCosFileStorage`)：
```java
private String buildAccessUrl(String key, Integer expires) {
    // 如果配置了自定义域名，直接返回 CDN 地址
    if (config.getDomain() != null) {
        return config.getDomain() + "/" + key;
    }
    // 否则生成预签名 URL（带过期时间）
    Date expiration = new Date(System.currentTimeMillis() + (expires != null ? expires : 3600) * 1000L);
    return cosClient.generatePresignedUrl(bucket, key, expiration, HttpMethodName.GET).toString();
}
```

**RustFS** (`RustfsFileStorage`)：
```java
public String getAccessUrl(String fileId, Integer expires) {
    // 使用 AWS S3 Presigner 生成预签名 URL
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .getObjectRequest(b -> b.bucket(bucket).key(key))
        .signatureDuration(Duration.ofSeconds(expires != null ? expires : 3600))
        .build();
    return presigner.presignGetObject(presignRequest).url().toString();
}
```

### 5.3 前端：`getFileUrl()` 工具函数

```js
export function getFileUrl(fileData) {
  if (typeof fileData === 'object') {
    // 对象格式：优先使用 accessUrl，其次 fileId
    if (fileData.accessUrl) return normalizeFileAccessUrl(fileData.accessUrl)
    if (fileData.fileId) return getFileDownloadUrl(fileData.fileId)
  }
  // 字符串格式：统一走下载接口
  return `${prefix}/api/file/download/${fileData}`
}
```

---

## 六、私有文件鉴权机制

### 6.1 上传时标记私有性

```java
// FileController.upload()
@RequestParam(value = "isPrivate", required = false, defaultValue = "true") Boolean isPrivate
```
- `isPrivate=true`（默认）：只有上传者本人可访问
- `isPrivate=false`：需要管理员权限才能上传，所有人可访问

### 6.2 权限校验

`SystemFileMetadataPersistence.checkPermission()`：

```java
public boolean checkPermission(String fileId, Long userId) {
    SysFileMetadata entity = metadataMapper.selectOne(...);
    
    // 公开文件：所有人可访问
    if (!Boolean.TRUE.equals(entity.getIsPrivate())) {
        return true;
    }
    
    // 私有文件：检查是否是上传者
    if (userId == null) {
        userId = StpUtil.getLoginIdAsLong();  // Sa-Token 获取当前登录用户
    }
    return entity.getUploaderId() != null && entity.getUploaderId().equals(userId);
}
```

### 6.3 下载接口鉴权

`FileController.download()` 标注了 `@ApiPermissionIgnore`（跳过接口级权限），但**文件级权限在 `FileManager.download()` 中校验**：

```java
public void download(String fileId, HttpServletResponse response) {
    FileMetadata metadata = metadataPersistence.getById(fileId);
    // 这里可以加入权限校验
    // if (!metadataPersistence.checkPermission(fileId, currentUserId)) { throw ... }
    
    try (InputStream inputStream = storage.download(fileId);
         OutputStream outputStream = response.getOutputStream()) {
        response.setContentType(metadata.getMimeType());
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(...));
        inputStream.transferTo(outputStream);
    }
}
```

> **注意**：当前代码中 `download()` 方法未显式调用 `checkPermission()`，这是一个可以改进的安全点。预签名 URL 本身带有有效期，但对于本地存储场景，需要在下载接口中补充权限校验。

---

## 七、文件信息表结构

### 7.1 `sys_file_metadata`（文件元数据表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT (ASSIGN_ID) | 主键 |
| `file_id` | VARCHAR | 文件唯一 ID（UUID，对外暴露的标识） |
| `original_name` | VARCHAR | 原始文件名 |
| `storage_name` | VARCHAR | 存储文件名（UUID + 扩展名） |
| `file_path` | VARCHAR | 文件存储路径（含业务目录和日期目录） |
| `file_size` | BIGINT | 文件大小（字节） |
| `mime_type` | VARCHAR | MIME 类型 |
| `extension` | VARCHAR | 文件扩展名 |
| `md5` | VARCHAR | 文件 MD5（用于秒传） |
| `storage_type` | VARCHAR | 存储类型（local/tencent/rustfs） |
| `bucket` | VARCHAR | 存储桶名称（对象存储时使用） |
| `access_url` | VARCHAR | 访问 URL |
| `thumbnail_url` | VARCHAR | 缩略图 URL |
| `business_type` | VARCHAR | 业务类型（avatar/notice/image 等） |
| `business_id` | VARCHAR | 业务 ID |
| `group_id` | BIGINT | 文件分组 ID |
| `uploader_id` | BIGINT | 上传者用户 ID |
| `upload_time` | DATETIME | 上传时间 |
| `expire_time` | DATETIME | 过期时间 |
| `is_private` | TINYINT(1) | 是否私有 |
| `download_count` | INT | 下载次数 |
| `status` | INT | 状态（1-正常，0-删除） |

**逻辑删除**：通过 `status` 字段实现，`delete()` 时 `set status=0`，不物理删除记录。

### 7.2 `sys_file_storage_config`（存储配置表）

| 字段 | 说明 |
|------|------|
| `id` | 主键 |
| `config_name` | 配置名称（如"本地存储""腾讯云COS"） |
| `storage_type` | 存储类型 code |
| `is_default` | 是否默认策略 |
| `enabled` | 是否启用 |
| `endpoint` | 访问端点（对象存储） |
| `access_key` / `secret_key` | 访问密钥 |
| `bucket_name` | 存储桶 |
| `region` | 区域 |
| `base_path` | 基础路径（本地存储的磁盘路径） |
| `domain` | 自定义访问域名（CDN） |
| `use_https` | 是否使用 HTTPS |
| `max_file_size` | 最大文件大小（MB） |
| `allowed_types` | 允许的文件类型（逗号分隔） |
| `order_num` | 排序 |
| `extra_config` | 扩展配置（JSON） |

### 7.3 `sys_file_group`（文件分组表）

| 字段 | 说明 |
|------|------|
| `id` | 主键 |
| `group_name` | 分组名称 |
| `group_code` | 分组编码 |
| `group_type` | 分组类型（document/image/video/audio/archive/default） |
| `parent_id` | 父分组 ID |
| `sort` | 排序 |
| `icon` | 图标 |
| `status` | 状态 |

---

## 八、前端上传组件和鉴权展示逻辑

### 8.1 `file-upload` 组件

通用文件上传组件（支持任意文件类型）。

**核心特性**：
- 基于 Naive UI `NUpload` 封装，自定义文件列表卡片展示
- 上传时自动携带 `Authorization`、`X-Timestamp`、`X-Nonce` 请求头
- 上传完成后，通过 `resolveRenderableFileUrl()` 将 `fileId` 解析为可渲染的 blob URL
- 支持 `v-model` 双向绑定，返回值可以是 `string`（逗号分隔）、`array` 或 `object`

**上传请求头**：
```js
const headers = computed(() => {
  const token = authStore.accessToken
  return {
    'Authorization': token ? `Bearer ${token}` : '',
    'X-Timestamp': Date.now().toString(),
    'X-Nonce': generateUUID(),
  }
})
```

### 8.2 `image-upload` 组件

图片专用上传组件（限制 `accept="image/*"`）。

**与 `file-upload` 的差异**：
- 展示为图片缩略图网格，而非文件列表
- 支持图片预览（Modal 弹窗）
- 支持图片重命名
- 懒加载优化（`IntersectionObserver`，`rootMargin: '220px'`）

### 8.3 鉴权展示的完整流程

```
用户访问页面
    │
    ▼
AuthImage 组件 mounted
    │
    ▼
loadImage()
    │
    ├── props.src 是完整 HTTP URL？
    │   └── 直接设置 imageSrc = url（浏览器直接请求，无鉴权）
    │
    ├── props.src 是 fileId（不含 /）？
    │   └── resolveRenderableFileUrl(fileId)
    │       │
    │       ├── 检查内存缓存 → localStorage 缓存
    │       │   └── 命中且未过期 → 直接使用缓存 URL
    │       │
    │       └── 未命中 → GET /api/file/url/{fileId}
    │           │
    │           ├── 后端返回预签名 URL（COS/RustFS）
    │           │   └── 图片 src 直接设为预签名 URL（带临时 token，有效期 12h）
    │           │
    │           └── 后端返回 /api/file/download/{fileId}（本地存储）
    │               └── fetch(URL, { headers: { Authorization: Bearer xxx } })
    │                   └── 获取 blob → URL.createObjectURL(blob) → 设置 imageSrc
    │
    ▼
<img :src="blobUrl 或 预签名URL" />
```

---

## 九、配置类和自动配置

### 9.1 `FileStorageProperties`（配置属性）

```java
@ConfigurationProperties(prefix = "forge.file")
public class FileStorageProperties {
    private Boolean enableGenericApi = true;  // 是否启用通用文件 API
    private String defaultStorageType = "local"; // 默认存储类型
}
```

### 9.2 `FileAutoConfiguration`（自动配置）

```java
@Configuration
@ComponentScan("com.mdframe.forge.starter.file")
public class FileAutoConfiguration implements InitializingBean {

    @Bean
    @ConditionalOnMissingBean
    public FileManager fileManager() {
        return new FileManager();
    }

    @Bean
    @ConditionalOnMissingBean(LocalFileStorage.class)
    public LocalFileStorage localFileStorage() {
        return new LocalFileStorage();  // 本地存储作为默认实现注册
    }

    @Override
    public void afterPropertiesSet() {
        // 注册所有 FileStorage Bean
        if (storageList != null) {
            for (FileStorage storage : storageList) {
                fileManager.registerStorage(storage);
            }
        }
        // 从数据库加载存储配置并初始化
        if (configProvider != null) {
            fileManager.refreshConfiguredStorages();
        }
    }
}
```

**自动配置生效条件**：Spring Boot 项目引入 `forge-starter-file` 依赖后，通过 Spring Boot 自动配置机制加载。

---

## 十、设计亮点与改进建议

### 10.1 设计亮点

1. **策略模式 + SPI 双扩展点**：存储实现可插拔，持久化层由业务系统自定义实现
2. **秒传支持**：基于 MD5 去重，相同文件只存一份
3. **预签名 URL**：对象存储场景使用预签名 URL，避免流量经后端转发
4. **AuthImage 组件**：优雅解决私有图片前端展示问题，对开发者透明
5. **两级缓存**：内存 + localStorage，减少预签名 URL 请求次数

### 10.2 改进建议

1. **下载接口补充权限校验**：当前 `FileManager.download()` 未调用 `checkPermission()`，建议补充
2. **预签名 URL 有效期优化**：当前默认 3600 秒，前端缓存 12 小时，可能出现缓存未过期但 URL 已失效的情况，建议前端在图片加载失败时自动刷新 URL（已有 `handleError` 中的重试逻辑，但可进一步优化）
3. **分片上传的并发控制**：当前分片上传为顺序处理，可优化为并发上传
4. **文件过期清理**：`expire_time` 字段已定义，但未实现定时清理过期文件的逻辑

---

## 十一、关键代码引用索引

| 功能 | 文件 |
|------|------|
| 存储策略接口 | `storage/FileStorage.java` |
| 本地存储实现 | `storage/impl/LocalFileStorage.java` |
| 腾讯云 COS 实现 | `storage/impl/TencentCosFileStorage.java` |
| RustFS 实现 | `storage/impl/RustfsFileStorage.java` |
| 文件管理器 | `core/FileManager.java` |
| REST API | `controller/FileController.java` |
| 元数据持久化 SPI | `spi/FileMetadataPersistence.java` |
| 配置提供者 SPI | `spi/StorageConfigProvider.java` |
| 前端 AuthImage 组件 | `forge-admin-ui/src/components/common/AuthImage.vue` |
| 前端文件工具函数 | `forge-admin-ui/src/utils/file.js` |
| 前端文件上传组件 | `forge-admin-ui/src/components/file-upload/index.vue` |
| 前端图片上传组件 | `forge-admin-ui/src/components/image-upload/index.vue` |
| 元数据实体 | `SysFileMetadata.java` |
| 存储配置实体 | `SysFileStorageConfig.java` |
| 持久化实现 | `SystemFileMetadataPersistence.java` |
| 配置提供者实现 | `SystemStorageConfigProvider.java` |
