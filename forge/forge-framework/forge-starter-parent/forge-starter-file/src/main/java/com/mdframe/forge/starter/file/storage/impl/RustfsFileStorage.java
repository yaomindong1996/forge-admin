package com.mdframe.forge.starter.file.storage.impl;

import cn.hutool.core.util.IdUtil;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.storage.FileStorage;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RustfsFileStorage implements FileStorage {
    
    private static final String STORAGE_TYPE = "rustfs";
    
    private StorageConfig config;
    private S3Client s3Client;
    private String defaultBucket;
    
    @Autowired(required = false)
    private FileMetadataPersistence metadataPersistence;
    
    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }
    
    @Override
    public void init(StorageConfig config) {
        this.config = config;
        this.defaultBucket = config.getBucketName();
        
        try {
            String endpoint = config.getEndpoint();
            String accessKey = config.getAccessKey();
            String secretKey = config.getSecretKey();
            
            if (endpoint == null || endpoint.isEmpty()) {
                throw new IllegalArgumentException("RustFS endpoint 不能为空");
            }
            
            if (accessKey == null || accessKey.isEmpty()) {
                throw new IllegalArgumentException("RustFS accessKey 不能为空");
            }
            
            if (secretKey == null || secretKey.isEmpty()) {
                throw new IllegalArgumentException("RustFS secretKey 不能为空");
            }
            
            s3Client = S3Client.builder()
                    .endpointOverride(java.net.URI.create(endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKey, secretKey)
                            )
                    )
                    .forcePathStyle(true)
                    .build();
            
            log.info("RustFS 存储初始化完成, endpoint: {}, bucket: {}", endpoint, defaultBucket);
        } catch (Exception e) {
            log.error("RustFS 存储初始化失败", e);
            throw new RuntimeException("RustFS 存储初始化失败", e);
        }
    }
    
    @Override
    public FileMetadata upload(org.springframework.web.multipart.MultipartFile file, String businessType, String businessId) {
        try {
            return upload(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                businessType,
                businessId
            );
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    @Override
    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId) {
        try {
            String bucket = defaultBucket;
            String key = generateObjectKey(fileName, businessType, businessId);
            
            try (InputStream stream = inputStream) {
                byte[] bytes = stream.readAllBytes();
                long fileSize = bytes.length;
                
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(fileSize)
                        .build();
                
                s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
                
                log.info("文件上传成功: bucket={}, key={}, size={}", bucket, key, fileSize);
                
                return FileMetadata.builder()
                        .fileId(IdUtil.fastSimpleUUID())
                        .originalName(fileName)
                        .storageName(key)
                        .filePath(key)
                        .fileSize(fileSize)
                        .mimeType(contentType)
                        .extension(getExtension(fileName))
                        .storageType(STORAGE_TYPE)
                        .bucket(bucket)
                        .businessType(businessType)
                        .businessId(businessId)
                        .uploadTime(LocalDateTime.now())
                        .isPrivate(false)
                        .downloadCount(0)
                        .build();
            }
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    @Override
    public String initMultipartUpload(String fileName, String businessType, String businessId) {
        String bucket = defaultBucket;
        String key = generateObjectKey(fileName, businessType, businessId);
        
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);
        
        log.info("初始化分片上传: bucket={}, key={}, uploadId={}", bucket, key, response.uploadId());
        
        return response.uploadId() + "|" + bucket + "|" + key;
    }
    
    @Override
    public String uploadPart(String uploadId, int partNumber, InputStream inputStream) {
        try {
            String[] parts = uploadId.split("\\|");
            if (parts.length != 3) {
                throw new IllegalArgumentException("无效的 uploadId 格式");
            }
            
            String actualUploadId = parts[0];
            String bucket = parts[1];
            String key = parts[2];
            
            byte[] bytes = inputStream.readAllBytes();
            
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(actualUploadId)
                    .partNumber(partNumber)
                    .contentLength((long) bytes.length)
                    .build();
            
            UploadPartResponse response = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(bytes));
            
            log.debug("上传分片成功: bucket={}, key={}, partNumber={}, eTag={}", bucket, key, partNumber, response.eTag());
            
            return response.eTag();
        } catch (IOException e) {
            log.error("分片上传失败", e);
            throw new RuntimeException("分片上传失败", e);
        }
    }
    
    @Override
    public FileMetadata completeMultipartUpload(String uploadId, List<String> partETags) {
        try {
            String[] parts = uploadId.split("\\|");
            if (parts.length != 3) {
                throw new IllegalArgumentException("无效的 uploadId 格式");
            }
            
            String actualUploadId = parts[0];
            String bucket = parts[1];
            String key = parts[2];
            
            List<CompletedPart> completedParts = new ArrayList<>();
            for (int i = 0; i < partETags.size(); i++) {
                completedParts.add(CompletedPart.builder()
                        .partNumber(i + 1)
                        .eTag(partETags.get(i))
                        .build());
            }
            
            CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();
            
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(actualUploadId)
                    .multipartUpload(completedUpload)
                    .build();
            
            s3Client.completeMultipartUpload(completeRequest);
            
            HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            
            log.info("完成分片上传: bucket={}, key={}, size={}", bucket, key, headResponse.contentLength());
            
            return FileMetadata.builder()
                    .fileId(IdUtil.fastSimpleUUID())
                    .originalName(key.substring(key.lastIndexOf("/") + 1))
                    .storageName(key)
                    .filePath(key)
                    .fileSize(headResponse.contentLength())
                    .mimeType(headResponse.contentType())
                    .extension(getExtension(key))
                    .storageType(STORAGE_TYPE)
                    .bucket(bucket)
                    .uploadTime(LocalDateTime.now())
                    .isPrivate(false)
                    .downloadCount(0)
                    .build();
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            throw new RuntimeException("完成分片上传失败", e);
        }
    }
    
    @Override
    public InputStream download(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }
        
        try {
            String bucket = metadata.getBucket();
            String key = metadata.getStorageName();
            
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            return new ByteArrayInputStream(response.readAllBytes());
        } catch (IOException e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败", e);
        }
    }
    
    @Override
    public String getAccessUrl(String fileId, Integer expires) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            return null;
        }
        
        String bucket = metadata.getBucket();
        String key = metadata.getStorageName();
        
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(b -> b.bucket(bucket).key(key))
                .signatureDuration(java.time.Duration.ofSeconds(expires != null ? expires : 3600))
                .build();
        
        software.amazon.awssdk.services.s3.presigner.S3Presigner presigner = software.amazon.awssdk.services.s3.presigner.S3Presigner.builder()
                .endpointOverride(java.net.URI.create(config.getEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(config.getAccessKey(), config.getSecretKey())
                        )
                )
                .build();
        
        try {
            software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest presignedRequest =
                    presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } finally {
            presigner.close();
        }
    }
    
    @Override
    public boolean delete(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            return false;
        }
        
        try {
            String bucket = metadata.getBucket();
            String key = metadata.getStorageName();
            
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(request);
            
            log.info("删除文件成功: bucket={}, key={}", bucket, key);
            return true;
        } catch (Exception e) {
            log.error("删除文件失败: {}", fileId, e);
            return false;
        }
    }
    
    @Override
    public boolean exists(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            return false;
        }
        
        try {
            String bucket = metadata.getBucket();
            String key = metadata.getStorageName();
            
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("检查文件存在性失败: {}", fileId, e);
            return false;
        }
    }
    
    private String generateObjectKey(String fileName, String businessType, String businessId) {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = getExtension(fileName);
        String uuid = IdUtil.fastSimpleUUID();
        String storageName = extension.isEmpty() ? uuid : uuid + "." + extension;
        
        return (businessType != null ? businessType : "common") + "/" + date + "/" + storageName;
    }
    
    private String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        
        return fileName.substring(lastDot + 1);
    }
    
    private FileMetadata getFileMetadata(String fileId) {
        if (metadataPersistence == null) {
            log.warn("FileMetadataPersistence未配置，无法获取文件元数据");
            return null;
        }
        return metadataPersistence.getById(fileId);
    }
}
