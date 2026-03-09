# 文件存储模块

统一的文件存储模块，支持本地、MinIO、OSS 等多种存储策略。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-file</artifactId>
</dependency>
```

## 配置

```yaml
forge:
  file:
    default-storage: local
    storages:
      local:
        path: /data/uploads
      minio:
        endpoint: http://localhost:9000
        access-key: minioadmin
        secret-key: minioadmin
        bucket: forge
```

## 使用示例

### 基础上传

```java
@Autowired
private FileManager fileManager;

public FileMetadata upload(MultipartFile file) {
    return fileManager.upload(file, "avatar", "user_001");
}
```

### 指定存储策略

```java
public FileMetadata uploadToMinio(MultipartFile file) {
    return fileManager.upload(file, "document", "order_001", "minio");
}
```

### 下载文件

```java
public void download(String fileId, HttpServletResponse response) {
    fileManager.download(fileId, response);
}
```

### 获取访问 URL

```java
// 获取临时访问 URL（有效期 1 小时）
String url = fileManager.getAccessUrl(fileId, 3600);
```

### 删除文件

```java
boolean success = fileManager.delete(fileId);
```

### 分片上传

```java
// 初始化分片上传
String uploadId = fileManager.initMultipartUpload(
    "large_file.zip", "document", "order_001", "minio"
);

// 上传分片
String eTag = fileManager.uploadPart(uploadId, 1, inputStream, "minio");

// 完成分片上传
FileMetadata metadata = fileManager.completeMultipartUpload(
    uploadId, Arrays.asList(eTag1, eTag2, eTag3), "minio"
);
```

## SPI 扩展

### StorageConfigProvider

动态获取存储配置：

```java
@Component
public class MyStorageConfigProvider implements StorageConfigProvider {
    @Override
    public StorageConfig getDefaultConfig() {
        // 返回默认存储配置
    }
    
    @Override
    public StorageConfig getConfigByType(String storageType) {
        // 返回指定类型的存储配置
    }
}
```

### FileMetadataPersistence

持久化文件元数据：

```java
@Component
public class MyFileMetadataPersistence implements FileMetadataPersistence {
    @Override
    public void save(FileMetadata metadata) {
        // 保存文件元数据
    }
    
    @Override
    public FileMetadata getById(String fileId) {
        // 根据 ID 查询
    }
    
    @Override
    public FileMetadata getByMd5(String md5) {
        // 根据 MD5 查询（用于秒传）
    }
    
    @Override
    public void delete(String fileId) {
        // 删除元数据
    }
    
    @Override
    public void incrementDownloadCount(String fileId) {
        // 增加下载次数
    }
}
```

## 自定义存储策略

```java
@Component
public class MyFileStorage implements FileStorage {
    
    @Override
    public String getStorageType() {
        return "my-storage";
    }
    
    @Override
    public FileMetadata upload(MultipartFile file, String businessType, String businessId) {
        // 实现上传逻辑
    }
    
    @Override
    public InputStream download(String fileId) {
        // 实现下载逻辑
    }
    
    @Override
    public void delete(String fileId) {
        // 实现删除逻辑
    }
    
    @Override
    public String getAccessUrl(String fileId, Integer expires) {
        // 返回访问 URL
    }
}
```

## FileMetadata 结构

```java
public class FileMetadata {
    private String fileId;          // 文件 ID
    private String fileName;        // 文件名
    private String originalName;    // 原始文件名
    private String filePath;        // 文件路径
    private String mimeType;        // MIME 类型
    private Long fileSize;          // 文件大小
    private String md5;             // MD5 值
    private String storageType;     // 存储类型
    private String businessType;    // 业务类型
    private String businessId;      // 业务 ID
    private Integer downloadCount;  // 下载次数
}
```