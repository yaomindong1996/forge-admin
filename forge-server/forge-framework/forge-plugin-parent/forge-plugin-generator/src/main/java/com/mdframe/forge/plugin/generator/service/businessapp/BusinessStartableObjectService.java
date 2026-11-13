package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.businessprocess.schema.BusinessProcessSchema;
import com.mdframe.forge.plugin.generator.mapper.BusinessStartableObjectMapper;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessRuntimeActionCompiler;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableProcessRow;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 移动端发起审批目录。只返回元数据，新建与提交仍由各自接口做权限校验。
 * 目录 = 绑定对象主流程的单据 + 含手动开始节点的已发布应用业务流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessStartableObjectService {

    private final BusinessStartableObjectMapper startableObjectMapper;
    private final BusinessProcessRuntimeActionCompiler actionCompiler;
    private final ObjectMapper objectMapper;

    public List<BusinessStartableObjectVO> listStartableObjects() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null) {
            return List.of();
        }
        List<BusinessStartableObjectVO> catalog = new ArrayList<>(
                safeList(startableObjectMapper.selectStartableObjects(tenantId)));
        for (BusinessStartableProcessRow row : safeList(startableObjectMapper.selectStartableProcesses(tenantId))) {
            String label = manualStartLabel(row);
            if (label != null) {
                catalog.add(toCatalogItem(row, label));
            }
        }
        return catalog;
    }

    /** 无手动开始节点（如仅事件触发）的流程不能由用户发起，返回 null。 */
    private String manualStartLabel(BusinessStartableProcessRow row) {
        BusinessProcessSchema schema = readSchema(row);
        if (schema == null) {
            return null;
        }
        List<Map<String, Object>> actions = actionCompiler.compileSchema(
                schema, row.getApplicationCode(), row.getProcessCode(), row.getProcessName());
        if (actions.isEmpty()) {
            return null;
        }
        return StringUtils.defaultIfBlank(String.valueOf(actions.get(0).get("label")), row.getProcessName());
    }

    private BusinessProcessSchema readSchema(BusinessStartableProcessRow row) {
        if (StringUtils.isBlank(row.getSchemaJson())) {
            return null;
        }
        try {
            Map<String, Object> json = objectMapper.readValue(row.getSchemaJson(), new TypeReference<>() { });
            return actionCompiler.schemaFromJson(json);
        } catch (Exception e) {
            log.warn("发起审批目录跳过协议损坏的业务流程: {}", row.getProcessCode());
            return null;
        }
    }

    private BusinessStartableObjectVO toCatalogItem(BusinessStartableProcessRow row, String label) {
        BusinessStartableObjectVO item = new BusinessStartableObjectVO();
        item.setObjectCode(row.getObjectCode());
        item.setObjectName(row.getObjectName());
        item.setObjectIcon(row.getObjectIcon());
        item.setConfigKey(row.getConfigKey());
        item.setApplicationId(row.getApplicationId());
        item.setApplicationName(row.getApplicationName());
        item.setApplicationIcon(row.getApplicationIcon());
        item.setSortOrder(row.getSortOrder());
        item.setProcessCode(row.getProcessCode());
        item.setProcessName(label);
        return item;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
