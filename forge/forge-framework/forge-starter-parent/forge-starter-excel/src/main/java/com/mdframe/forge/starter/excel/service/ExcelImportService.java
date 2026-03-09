package com.mdframe.forge.starter.excel.service;

import com.mdframe.forge.starter.excel.model.ImportResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Map;

/**
 * Excel 导入服务接口
 */
public interface ExcelImportService {
    
    /**
     * 下载导入模板
     *
     * @param configKey 配置键
     * @return 模板文件字节数组
     */
    byte[] downloadTemplate(String configKey);
    
    /**
     * 导入 Excel 数据
     *
     * @param file      上传的文件
     * @param configKey 配置键
     * @param clazz     目标类型
     * @param <T>       数据类型
     * @return 导入结果
     */
    <T> ImportResult<T> importData(MultipartFile file, String configKey, Class<T> clazz);
    
    /**
     * 导入 Excel 数据（从输入流）
     *
     * @param inputStream 输入流
     * @param configKey   配置键
     * @param clazz       目标类型
     * @param <T>         数据类型
     * @return 导入结果
     */
    <T> ImportResult<T> importData(InputStream inputStream, String configKey, Class<T> clazz);
    
    /**
     * 下载错误报告
     *
     * @param taskId 导入任务 ID
     * @return 错误报告文件字节数组
     */
    byte[] downloadErrorReport(String taskId);
}
