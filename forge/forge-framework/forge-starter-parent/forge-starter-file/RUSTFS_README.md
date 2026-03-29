# RustFS 文件存储集成

## 概述

RustFS 是一款兼容 S3 协议的对象存储系统。本集成允许您在 forge-starter-file 模块中使用 RustFS 作为文件存储后端。

## 依赖

已添加 AWS S3 SDK 依赖：

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.27</version>
</dependency>
```

## 配置步骤

### 1. 初始化 RustFS

确保 RustFS 服务已启动并可访问。创建存储桶：

```bash
# 使用 mc 客户端或直接通过管理界面创建存储桶
mc alias set rustfs http://192.168.1.100:9000 rustfsadmin rustfssecret
mc mb rustfs/forge-files
```

### 2. 导入数据库配置

执行 SQL 文件添加 RustFS 存储配置：

```sql
-- forge/forge-framework/forge-starter-parent/forge-starter-file-file/sql/rustfs_config.sql
```

修改配置中的以下参数：
- `endpoint`: RustFS 服务地址
- `access_key`: 访问密钥ID
- `secret_key`: 访问密钥Secret
- `bucket_name`: 存储桶名称

### 3. 启用 RustFS 存储

在系统管理界面的"存储配置"中：
1. 找到"RustFS存储"配置项
2. 设置为默认存储（可选）
3. 启用该存储策略

## 使用示例

### 1. 上传文件

```java
@Autowired
private FileStorageManager fileStorageManager;

@RestController
public class FileController {
    
    @PostMapping("/upload")
    public FileMetadata upload(@RequestParam("file") MultipartFile file) {
        // 使用RustFS上传
        return fileStorageManager.upload(
            file, 
            "avatar",  // businessType
            "user-123" // businessId
        );
    }
}
```

### 2. 指定存储类型上传

```java
@PostMapping("/upload/rustfs")
public FileMetadata uploadToRustfs(@RequestParam("file") MultipartFile file) {
    return fileStorageManager.upload(
        file, 
        "avatar", 
        "user-123",
        "rustfs"  // 指定使用RustFS存储
    );
}
```

### 3. 下载文件

```java
@GetMapping("/download/{fileId}")
public void download(@PathVariable String fileId, HttpServletResponse response) {
    InputStream inputStream = fileStorageManager.download(fileId);
    // 处理文件下载
}
```

### 4. 获取访问URL

```java
@GetMapping("/url/{fileId}")
public String getAccessUrl(@PathVariable String fileId) {
    // 获取1小时有效期的预签名URL
    return fileStorageManager.getAccessUrl(fileId, 3600);
}
```

### 5. 分片上传

```java
@PostMapping("/upload/multipart/init")
public String initMultipartUpload(
    @RequestParam("fileName") String fileName,
    @RequestParam("businessType") String businessType,
    @RequestParam("businessId") String businessId
) {
    return fileStorageManager.initMultipartUpload(fileName, businessType, businessId);
}

@PostMapping("/upload/multipart/part")
public String uploadPart(
    @RequestParam("uploadId") String uploadId,
    @RequestParam("partNumber") int partNumber,
    @RequestBody byte[] data
) {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
    return fileStorageManager.uploadPart(uploadId, partNumber, inputStream);
}

@PostMapping("/upload/multipart/complete")
public FileMetadata completeMultipartUpload(
    @RequestParam("uploadId") String uploadId,
    @RequestBody List<String> partETags
) {
    return fileStorageManager.completeMultipartUpload(uploadId, partETags);
}
```

## RustfsFileStorage 实现说明

`RustfsFileStorage` 类实现了 `FileStorage` 接口，提供以下功能：

- **upload()**: 上传文件到 RustFS
- **initMultipartUpload()**: 初始化分片上传
- **uploadPart()**: 上传分片
- **completeMultipartUpload()**: 完成分片上传
- **download()**: 下载文件
- **getAccessUrl()**: 生成预签名访问URL
- **delete()**: 删除文件
- **exists()**: 检查文件是否存在

### 关键配置

- 使用 AWS S3 SDK v2.25.27
- 强制使用 Path-Style 访问模式 (`.forcePathStyle(true)`)
- 支持 HTTP/HTTPS 协议
- 支持预签名 URL 生成
- 支持分片上传大文件

## 常见问题

### 1. 连接失败

```
ConnectException: Connection refused
```

**解决方法**: 检查 RustFS 服务是否启动，端点和端口是否正确。

### 2. 301 Moved Permanently

```
S3Exception: 301 Moved Permanently
```

**解决方法**: 已在代码中设置 `.forcePathStyle(true)`，确保 region 设置正确。

### 3. 403 Forbidden

**解决方法**: 检查 `access_key` 和 `secret_key` 是否正确。

### 4. 上传失败

**解决方法**: 确保：
- 使用 HTTP 协议（或配置正确的 HTTPS 证书）
- 存储桶已创建
- 文件大小在允许范围内

## 配置项说明

| 配置项 | 说明 | 示例 |
|--------|------|------|
| endpoint | RustFS 服务地址 | http://192.168.1.100:9000 |
| access_key | 访问密钥ID | rustfsadmin |
| secret_key | 访问密钥Secret | rustfssecret |
| bucket_name | 存储桶名称 | forge-files |
| region | 区域（可选） | us-east-1 |
| max_file_size | 最大文件大小(MB) | 500 |
| allowed_types | 允许的文件类型 | jpg,png,pdf,doc,docx |

## 多存储切换

系统支持多种存储策略并存，可通过配置动态切换：

```java
// 使用本地存储
fileStorageManager.upload(file, businessType, businessId, "local");

// 使用RustFS存储
fileStorageManager.upload(file, businessType, businessId, "rustfs");

// 使用默认存储
fileStorageManager.upload(file, businessType, businessId);
```

## 性能优化建议

1. **大文件上传**: 使用分片上传功能，支持断点续传
2. **并发上传**: 利用 RustFS 的分布式特性进行并发上传
3. **预签名URL**: 对于公开访问的文件，使用预签名URL减少服务器压力
4. **CDN集成**: 可将 RustFS 与 CDN 结合使用加速文件访问

## 监控和日志

日志输出示例：

```
RustFS 存储初始化完成, endpoint: http://192.168.1.100:9000, bucket: forge-files
文件上传成功: bucket=forge-files, key=avatar/2025/03/28/abc123.jpg, size=102400
初始化分片上传: bucket=forge-files, key=bigfile.zip, uploadId=xyz789
上传分片成功: bucket=forge-files, key=bigfile.zip, partNumber=1, eTag="abc..."
完成分片上传: bucket=forge-files, key=bigfile.zip, size=104857600
```

## 参考链接

- [RustFS 官方文档](https://docs.rustfs.com.cn)
- [AWS S3 SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [S3 协议兼容性](https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html)
