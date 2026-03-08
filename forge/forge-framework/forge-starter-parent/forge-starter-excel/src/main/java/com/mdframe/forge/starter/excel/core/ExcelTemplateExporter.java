package com.mdframe.forge.starter.excel.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Excel模板导出器
 * 基于模板填充数据
 */
@Slf4j
@Component
public class ExcelTemplateExporter {

    /**
     * 模板导出（单个对象）
     */
    public void exportByTemplate(HttpServletResponse response, Resource templateResource, 
                                  Map<String, Object> data, String fileName) {
        try (InputStream templateStream = templateResource.getInputStream()) {
            setResponseHeaders(response, fileName);
            EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateStream)
                    .sheet()
                    .doFill(data);
        } catch (IOException e) {
            log.error("模板导出失败", e);
            throw new RuntimeException("模板导出失败", e);
        }
    }

    /**
     * 模板导出（列表）
     */
    public void exportByTemplate(HttpServletResponse response, Resource templateResource,
                                  List<Map<String, Object>> dataList, String fileName) {
        try (InputStream templateStream = templateResource.getInputStream()) {
            setResponseHeaders(response, fileName);
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
            EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateStream)
                    .sheet()
                    .doFill(dataList);
        } catch (IOException e) {
            log.error("模板导出失败", e);
            throw new RuntimeException("模板导出失败", e);
        }
    }

    /**
     * 模板导出（复杂场景：单个对象 + 列表）
     */
    public void exportByTemplate(HttpServletResponse response, Resource templateResource,
                                  Map<String, Object> singleData, List<Map<String, Object>> dataList,
                                  String fileName) {
        try (InputStream templateStream = templateResource.getInputStream()) {
            setResponseHeaders(response, fileName);
            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
                    .withTemplate(templateStream)
                    .build();
            WriteSheet writeSheet = EasyExcel.writerSheet().build();
            FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();

            // 先填充单个对象
            if (singleData != null && !singleData.isEmpty()) {
                excelWriter.fill(singleData, writeSheet);
            }

            // 再填充列表
            if (dataList != null && !dataList.isEmpty()) {
                excelWriter.fill(dataList, fillConfig, writeSheet);
            }

            excelWriter.finish();
        } catch (IOException e) {
            log.error("模板导出失败", e);
            throw new RuntimeException("模板导出失败", e);
        }
    }

    private void setResponseHeaders(HttpServletResponse response, String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            fileName = "export_" + System.currentTimeMillis() + ".xlsx";
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
    }
}
