package com.mdframe.forge.starter.file.util;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件工具类
 */
@Slf4j
public class FileUtil {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * 获取文件扩展名
     */
    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int index = fileName.lastIndexOf(".");
        return index >= 0 ? fileName.substring(index + 1) : "";
    }
    
    /**
     * 获取文件名（不含扩展名）
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int index = fileName.lastIndexOf(".");
        return index >= 0 ? fileName.substring(0, index) : fileName;
    }
    
    /**
     * 生成存储文件名
     */
    public static String generateStorageName(String originalFileName) {
        String extension = getExtension(originalFileName);
        String uuid = IdUtil.fastSimpleUUID();
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }
    
    /**
     * 生成文件路径（按日期分组）
     */
    public static String generateFilePath(String businessType) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        return businessType + "/" + date + "/";
    }
    
    /**
     * 计算文件MD5
     */
    public static String calculateMd5(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return calculateMd5(inputStream);
        } catch (Exception e) {
            log.error("计算文件MD5失败", e);
            return null;
        }
    }
    
    /**
     * 计算流MD5
     */
    public static String calculateMd5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算MD5失败", e);
            return null;
        }
    }
    
    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * 检测MIME类型
     */
    public static String detectMimeType(String fileName) {
        String extension = getExtension(fileName).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            default -> "application/octet-stream";
        };
    }
}
