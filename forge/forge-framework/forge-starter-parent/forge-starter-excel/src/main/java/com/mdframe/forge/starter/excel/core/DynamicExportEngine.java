package com.mdframe.forge.starter.excel.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;
import com.mdframe.forge.starter.excel.model.ExcelExportMetadata;
import com.mdframe.forge.starter.excel.spi.ExcelConfigProvider;
import com.mdframe.forge.starter.excel.spi.ExcelMetadataProvider;
import com.mdframe.forge.starter.trans.manager.TransManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态导出引擎
 * 通过数据库配置驱动，无需编写代码即可实现导出
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicExportEngine {

    private final ApplicationContext applicationContext;

    @Autowired(required = false)
    private ExcelMetadataProvider metadataProvider;

    @Autowired(required = false)
    private ExcelConfigProvider configProvider;

    @Autowired(required = false)
    private TransManager transManager;

    /**
     * 动态导出核心方法
     *
     * @param response    响应对象
     * @param configKey   配置键
     * @param queryParams 查询参数（Map形式）
     */
    public void export(HttpServletResponse response, String configKey, Map<String, Object> queryParams) {
        try {
            // 1. 加载导出元数据
            ExcelExportMetadata metadata = loadMetadata(configKey);
            if (metadata == null || metadata.getStatus() == 0) {
                throw new RuntimeException("导出配置不存在或已禁用: " + configKey);
            }

            // 2. 加载列配置
            List<ExcelColumnConfig> columnConfigs = loadColumnConfigs(configKey);
            if (columnConfigs == null || columnConfigs.isEmpty()) {
                throw new RuntimeException("未配置导出列: " + configKey);
            }

            // 3. 查询数据
            List<?> dataList = queryData(metadata, queryParams);
            if (dataList == null || dataList.isEmpty()) {
                log.warn("导出数据为空: {}", configKey);
                dataList = Collections.emptyList();
            }

            // 4. 字典翻译
            if (Boolean.TRUE.equals(metadata.getAutoTrans()) && transManager != null) {
                translateData(dataList);
            }

            // 5. 限制导出条数
            if (metadata.getMaxRows() != null && dataList.size() > metadata.getMaxRows()) {
                dataList = dataList.subList(0, metadata.getMaxRows());
                log.warn("导出数据超过最大限制，已截断至{}条", metadata.getMaxRows());
            }

            // 6. 构建Excel并导出
            exportToResponse(response, metadata, columnConfigs, dataList);

        } catch (Exception e) {
            log.error("动态导出失败: {}", configKey, e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加载元数据
     */
    private ExcelExportMetadata loadMetadata(String configKey) {
        if (metadataProvider == null) {
            throw new RuntimeException("未配置ExcelMetadataProvider");
        }
        return metadataProvider.getMetadata(configKey);
    }

    /**
     * 加载列配置
     */
    private List<ExcelColumnConfig> loadColumnConfigs(String configKey) {
        if (configProvider == null) {
            throw new RuntimeException("未配置ExcelConfigProvider");
        }
        List<ExcelColumnConfig> configs = configProvider.getColumnConfigs(configKey);
        if (configs != null) {
            // 按orderNum排序，过滤未启用的列
            configs = configs.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getExport()))
                    .sorted(Comparator.comparingInt(c -> c.getOrderNum() != null ? c.getOrderNum() : Integer.MAX_VALUE))
                    .collect(Collectors.toList());
        }
        return configs;
    }

    /**
     * 查询数据（反射调用Service方法）
     */
    private List<?> queryData(ExcelExportMetadata metadata, Map<String, Object> queryParams) {
        try {
            // 获取Service Bean
            Object serviceBean = applicationContext.getBean(metadata.getDataSourceBean());
            Class<?> serviceClass = serviceBean.getClass();

            // 查找方法
            Method queryMethod = findQueryMethod(serviceClass, metadata.getQueryMethod());
            if (queryMethod == null) {
                throw new RuntimeException("未找到查询方法: " + metadata.getQueryMethod());
            }

            // 构建方法参数
            Object[] args = buildMethodArgs(queryMethod, queryParams);

            // 反射调用
            Object result = queryMethod.invoke(serviceBean, args);

            // 处理返回值
            return extractDataList(result, metadata.getPageable());

        } catch (Exception e) {
            log.error("查询数据失败", e);
            throw new RuntimeException("查询数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找查询方法
     */
    private Method findQueryMethod(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 构建方法参数（支持多种参数类型）
     * 支持场景：
     * 1. 无参方法：list()
     * 2. Map参数：queryList(Map<String, Object> params)
     * 3. 单个实体参数：query(UserQuery query)
     * 4. 多个基本类型参数：query(String keyword, Integer status)
     */
    private Object[] buildMethodArgs(Method method, Map<String, Object> queryParams) {
        Class<?>[] paramTypes = method.getParameterTypes();
        
        // 无参方法
        if (paramTypes.length == 0) {
            return new Object[0];
        }
        
        // 单参数方法
        if (paramTypes.length == 1) {
            Class<?> paramType = paramTypes[0];
            
            // 1. Map类型参数
            if (Map.class.isAssignableFrom(paramType)) {
                return new Object[]{queryParams != null ? queryParams : new HashMap<>()};
            }
            
            // 2. 基本类型及其包装类
            if (isPrimitiveOrWrapper(paramType)) {
                Object value = queryParams != null && queryParams.size() == 1 
                    ? queryParams.values().iterator().next() 
                    : null;
                return new Object[]{convertToType(value, paramType)};
            }
            
            // 3. String类型
            if (String.class.isAssignableFrom(paramType)) {
                Object value = queryParams != null && queryParams.size() == 1 
                    ? queryParams.values().iterator().next() 
                    : null;
                return new Object[]{value != null ? String.valueOf(value) : null};
            }
            
            // 4. 自定义实体对象（通过反射从Map构建）
            return new Object[]{mapToObject(queryParams, paramType)};
        }
        
        // 多参数方法：尝试按参数名匹配
        try {
            return buildMultiArgs(method, queryParams);
        } catch (Exception e) {
            log.warn("无法构建多参数方法参数，将使用null填充: {}", method.getName(), e);
            Object[] args = new Object[paramTypes.length];
            Arrays.fill(args, null);
            return args;
        }
    }
    
    /**
     * 构建多参数方法的参数
     */
    private Object[] buildMultiArgs(Method method, Map<String, Object> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return new Object[method.getParameterCount()];
        }
        
        Class<?>[] paramTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] args = new Object[paramTypes.length];
        
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Class<?> paramType = paramTypes[i];
            
            // 从queryParams中按参数名获取值
            Object value = queryParams.get(paramName);
            
            if (value == null) {
                args[i] = null;
            } else if (paramType.isAssignableFrom(value.getClass())) {
                args[i] = value;
            } else {
                args[i] = convertToType(value, paramType);
            }
        }
        
        return args;
    }
    
    /**
     * 判断是否为基本类型或其包装类
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() 
            || clazz == Integer.class || clazz == Long.class 
            || clazz == Double.class || clazz == Float.class 
            || clazz == Boolean.class || clazz == Byte.class 
            || clazz == Short.class || clazz == Character.class;
    }
    
    /**
     * 类型转换
     */
    private Object convertToType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        String strValue = String.valueOf(value);
        
        try {
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(strValue);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(strValue);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.parseDouble(strValue);
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.parseFloat(strValue);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(strValue);
            } else if (targetType == String.class) {
                return strValue;
            }
        } catch (Exception e) {
            log.warn("类型转换失败: {} -> {}", value, targetType.getSimpleName());
        }
        
        return value;
    }
    
    /**
     * 将Map转换为对象（通过反射设置字段）
     */
    private Object mapToObject(Map<String, Object> map, Class<?> targetClass) {
        try {
            // 先创建对象实例
            Object instance = targetClass.getDeclaredConstructor().newInstance();
            
            // 如果map为空或无数据,返回空对象
            if (map == null || map.isEmpty()) {
                return instance;
            }
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                
                try {
                    // 尝试通过setter方法设置
                    String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    Method setter = findSetter(targetClass, setterName, value);
                    if (setter != null) {
                        Object convertedValue = convertToType(value, setter.getParameterTypes()[0]);
                        setter.invoke(instance, convertedValue);
                        continue;
                    }
                    
                    // 回退到直接字段访问
                    java.lang.reflect.Field field = findField(targetClass, fieldName);
                    if (field != null) {
                        field.setAccessible(true);
                        Object convertedValue = convertToType(value, field.getType());
                        field.set(instance, convertedValue);
                    }
                } catch (Exception e) {
                    log.debug("设置字段失败: {}.{}", targetClass.getSimpleName(), fieldName);
                }
            }
            
            return instance;
        } catch (Exception e) {
            log.error("Map转对象失败: {}", targetClass.getSimpleName(), e);
            return null;
        }
    }
    
    /**
     * 查找setter方法
     */
    private Method findSetter(Class<?> clazz, String setterName, Object value) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }
    
    /**
     * 查找字段（支持父类）
     */
    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 提取数据列表（支持分页对象）
     */
    @SuppressWarnings("unchecked")
    private List<?> extractDataList(Object result, Boolean pageable) {
        if (result == null) {
            return Collections.emptyList();
        }
        if (result instanceof List) {
            return (List<?>) result;
        }
        // 如果是分页对象，尝试反射获取records字段
        if (Boolean.TRUE.equals(pageable)) {
            try {
                Method getRecords = result.getClass().getMethod("getRecords");
                Object records = getRecords.invoke(result);
                if (records instanceof List) {
                    return (List<?>) records;
                }
            } catch (Exception e) {
                log.warn("无法从分页对象提取records", e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * 字典翻译
     */
    private void translateData(List<?> dataList) {
        try {
            if (transManager != null && !dataList.isEmpty()) {
                transManager.translate(dataList);
            }
        } catch (Exception e) {
            log.warn("字典翻译失败", e);
        }
    }

    /**
     * 导出到响应流
     */
    private void exportToResponse(HttpServletResponse response, ExcelExportMetadata metadata,
                                   List<ExcelColumnConfig> columnConfigs, List<?> dataList) throws IOException {
        // 设置响应头
        String fileName = buildFileName(metadata.getFileNameTemplate(), metadata.getExportName());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);

        // 构建表头
        List<List<String>> headers = columnConfigs.stream()
                .map(c -> Collections.singletonList(c.getColumnName()))
                .collect(Collectors.toList());

        // 映射数据为List<List<Object>>格式（与表头列顺序对应）
        List<List<Object>> mappedData = mapDataToList(dataList, columnConfigs);

        // 写入Excel
        String sheetName = metadata.getSheetName() != null ? metadata.getSheetName() : "Sheet1";
        EasyExcel.write(response.getOutputStream())
                .head(headers)
                .sheet(sheetName)
                .doWrite(mappedData);
    }

    /**
     * 构建文件名
     */
    private String buildFileName(String template, String defaultName) {
        if (template == null || template.isEmpty()) {
            template = defaultName + "_{date}.xlsx";
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return template.replace("{date}", date).replace("{time}", time);
    }

    /**
     * 映射数据到列表格式（与表头顺序一致）
     */
    private List<List<Object>> mapDataToList(List<?> dataList, List<ExcelColumnConfig> columnConfigs) {
        List<List<Object>> result = new ArrayList<>();
        for (Object obj : dataList) {
            List<Object> row = new ArrayList<>();
            for (ExcelColumnConfig config : columnConfigs) {
                Object value = getFieldValue(obj, config.getFieldName());
                row.add(value);
            }
            result.add(row);
        }
        log.debug("映射数据: {} 条记录，{} 列", result.size(), columnConfigs.size());
        return result;
    }

    /**
     * 映射数据到列（反射读取字段值）
     * @deprecated 使用 mapDataToList 替代
     */
    @Deprecated
    private List<Map<String, Object>> mapDataToColumns(List<?> dataList, List<ExcelColumnConfig> columnConfigs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object obj : dataList) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (ExcelColumnConfig config : columnConfigs) {
                Object value = getFieldValue(obj, config.getFieldName());
                row.put(config.getFieldName(), value);
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 反射获取字段值
     */
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            // 优先尝试getter方法
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = obj.getClass().getMethod(getterName);
            return getter.invoke(obj);
        } catch (Exception e) {
            // 回退到直接字段访问
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception ex) {
                log.warn("无法获取字段值: {}.{}", obj.getClass().getSimpleName(), fieldName);
                return null;
            }
        }
    }

    /**
     * 导出到输出流（用于异步导出）
     *
     * @param outputStream 输出流
     * @param configKey    配置键
     * @param queryParams  查询参数
     */
    public void exportToStream(java.io.OutputStream outputStream, String configKey, Map<String, Object> queryParams) {
        try {
            ExcelExportMetadata metadata = loadMetadata(configKey);
            if (metadata == null || metadata.getStatus() == 0) {
                throw new RuntimeException("导出配置不存在或已禁用：" + configKey);
            }

            List<ExcelColumnConfig> columnConfigs = loadColumnConfigs(configKey);
            if (columnConfigs == null || columnConfigs.isEmpty()) {
                throw new RuntimeException("未配置导出列：" + configKey);
            }

            List<?> dataList = queryData(metadata, queryParams);
            if (dataList == null || dataList.isEmpty()) {
                log.warn("导出数据为空：{}", configKey);
                dataList = Collections.emptyList();
            }

            if (Boolean.TRUE.equals(metadata.getAutoTrans()) && transManager != null) {
                translateData(dataList);
            }

            if (metadata.getMaxRows() != null && dataList.size() > metadata.getMaxRows()) {
                dataList = dataList.subList(0, metadata.getMaxRows());
                log.warn("导出数据超过最大限制，已截断至{}条", metadata.getMaxRows());
            }

            exportToStreamInternal(outputStream, metadata, columnConfigs, dataList);

        } catch (Exception e) {
            log.error("动态导出到流失败：{}", configKey, e);
            throw new RuntimeException("导出失败：" + e.getMessage(), e);
        }
    }

    /**
     * 导出到输出流内部方法
     */
    private void exportToStreamInternal(java.io.OutputStream outputStream, ExcelExportMetadata metadata,
                                         List<ExcelColumnConfig> columnConfigs, List<?> dataList) throws IOException {
        List<List<String>> headers = columnConfigs.stream()
                .map(c -> Collections.singletonList(c.getColumnName()))
                .collect(Collectors.toList());

        List<List<Object>> mappedData = mapDataToList(dataList, columnConfigs);

        String sheetName = metadata.getSheetName() != null ? metadata.getSheetName() : "Sheet1";
        EasyExcel.write(outputStream)
                .head(headers)
                .sheet(sheetName)
                .doWrite(mappedData);
    }
}
