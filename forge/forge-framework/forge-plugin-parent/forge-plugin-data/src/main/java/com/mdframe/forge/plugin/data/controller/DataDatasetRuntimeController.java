package com.mdframe.forge.plugin.data.controller;

import com.mdframe.forge.plugin.data.dto.DataDatasetQueryDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import com.mdframe.forge.plugin.data.enums.DataDatasetAccessLevelEnum;
import com.mdframe.forge.plugin.data.enums.DatasetPublishStatusEnum;
import com.mdframe.forge.plugin.data.service.DataConnectionService;
import com.mdframe.forge.plugin.data.service.DataDatasetAccessService;
import com.mdframe.forge.plugin.data.service.DataDatasetFieldService;
import com.mdframe.forge.plugin.data.service.DataDatasetService;
import com.mdframe.forge.plugin.data.service.DataQueryExecutor;
import com.mdframe.forge.plugin.data.support.DataDatasetFieldViewAssembler;
import com.mdframe.forge.plugin.data.vo.DataDatasetFieldVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetMetadataVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetQueryResultVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/data/dataset/runtime")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DataDatasetRuntimeController {

    private final DataDatasetService datasetService;
    private final DataDatasetAccessService datasetAccessService;
    private final DataConnectionService connectionService;
    private final DataDatasetFieldService fieldService;
    private final DataQueryExecutor queryExecutor;
    private final DataDatasetFieldViewAssembler fieldViewAssembler;

    @PostMapping("/query")
    public RespInfo<DataDatasetQueryResultVO> query(@RequestBody DataDatasetQueryDTO dto) {
        DataDataset dataset = datasetService.getById(dto.getDatasetId());
        if (dataset == null) {
            throw new BusinessException("数据集不存在或已删除");
        }
        if (!DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
            throw new BusinessException("数据集未发布，暂不可使用");
        }
        if (dataset.getStatus() != 1) {
            throw new BusinessException("数据集已禁用");
        }
        datasetAccessService.requireAccess(dataset, DataDatasetAccessLevelEnum.QUERY);
        DataConnection connection = connectionService.getById(dataset.getConnectionId());
        if (connection == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        if (connection.getStatus() != 1) {
            throw new BusinessException("数据连接已禁用");
        }
        List<DataDatasetField> fields = fieldService.listByDatasetId(dto.getDatasetId());
        DataDatasetQueryResultVO result = queryExecutor.execute(dataset, connection, fields, dto);
        return RespInfo.success(result);
    }

    @GetMapping("/{id}/metadata")
    public RespInfo<DataDatasetMetadataVO> getMetadata(@PathVariable Long id) {
        DataDataset dataset = datasetService.getById(id);
        if (dataset == null) {
            throw new BusinessException("数据集不存在或已删除");
        }
        if (!DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
            throw new BusinessException("数据集未发布，暂不可使用");
        }
        if (dataset.getStatus() != 1) {
            throw new BusinessException("数据集已禁用");
        }
        datasetAccessService.requireAccess(dataset, DataDatasetAccessLevelEnum.VIEW);
        List<DataDatasetField> fields = fieldService.listByDatasetId(id);
        DataDatasetMetadataVO metadata = new DataDatasetMetadataVO();
        metadata.setDatasetId(dataset.getId());
        metadata.setDatasetCode(dataset.getDatasetCode());
        metadata.setDatasetName(dataset.getDatasetName());
        metadata.setDatasetType(dataset.getDatasetType());
        metadata.setFields(convertToFieldVOList(fields));
        metadata.setParamSchemaJson(dataset.getParamSchemaJson());
        return RespInfo.success(metadata);
    }

    private List<DataDatasetFieldVO> convertToFieldVOList(List<DataDatasetField> fields) {
        return fieldViewAssembler.toVOList(fields);
    }
}
