# Excel 导出模块

基于 EasyExcel 的 Excel 导出模块，支持注解配置和动态列。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-excel</artifactId>
</dependency>
```

## 注解配置

### @ExcelExport 类注解

```java
@ExcelExport(sheetName = "用户列表", autoTrans = true)
public class UserVO {
    // ...
}
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| sheetName | "Sheet1" | Sheet 名称 |
| autoTrans | true | 自动翻译字典 |
| filterNull | false | 过滤 null 值 |

### @ExcelColumn 字段注解

```java
@ExcelColumn(value = "用户名", width = 20, order = 1)
private String username;

@ExcelColumn(value = "状态", dictType = "sys_status")
private Integer status;

@ExcelColumn(value = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
private Date createTime;

@ExcelColumn(value = "金额", numberFormat = "#,##0.00")
private BigDecimal amount;

@ExcelColumn(export = false)
private String password;  // 不导出
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| value | 字段名 | 列名 |
| width | 20 | 列宽 |
| order | Integer.MAX_VALUE | 排序 |
| dateFormat | - | 日期格式 |
| numberFormat | - | 数字格式 |
| dictType | - | 字典类型 |
| export | true | 是否导出 |

## 使用示例

### 基础导出

```java
@Autowired
private ExcelExporter excelExporter;

public void export(HttpServletResponse response, List<UserVO> users) {
    ExcelExportConfig config = new ExcelExportConfig();
    config.setFileName("用户列表.xlsx");
    config.setSheetName("用户");
    
    excelExporter.export(response, users, UserVO.class, config);
}
```

### 动态列导出

```java
@Autowired
private DynamicExportEngine dynamicExportEngine;

public void exportDynamic(HttpServletResponse response, String configKey, List<Map<String, Object>> data) {
    dynamicExportEngine.export(response, configKey, data);
}
```

### 配合字典翻译

```java
// 实体类添加 @DictTrans 注解
@DictTrans
@ExcelExport(autoTrans = true)
public class UserVO {
    @ExcelColumn(value = "状态", dictType = "sys_status")
    @TransField(dictType = "sys_status")
    private Integer status;
}
```

## SPI 扩展

### ExcelConfigProvider

从数据库加载列配置：

```java
@Component
public class MyExcelConfigProvider implements ExcelConfigProvider {
    @Override
    public List<ExcelColumnConfig> getColumnConfigs(String configKey) {
        // 从数据库查询列配置
        return columnConfigMapper.selectByConfigKey(configKey);
    }
}
```

### ExcelMetadataProvider

提供导出元数据：

```java
@Component
public class MyExcelMetadataProvider implements ExcelMetadataProvider {
    @Override
    public ExcelExportMetadata getMetadata(String configKey) {
        // 返回导出元数据
        return metadataMapper.selectByConfigKey(configKey);
    }
}
```