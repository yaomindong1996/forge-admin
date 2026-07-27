package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.dto.VersionCompareDTO;
import com.mdframe.forge.starter.flow.dto.VersionRevertDTO;
import com.mdframe.forge.starter.flow.dto.VersionTagUpdateDTO;
import com.mdframe.forge.starter.flow.entity.FlowModelVersion;
import com.mdframe.forge.starter.flow.service.FlowModelVersionService;
import com.mdframe.forge.starter.flow.vo.VersionCompareVO;
import com.mdframe.forge.starter.flow.vo.VersionDetailVO;
import com.mdframe.forge.starter.flow.vo.VersionRevertVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/flow/model/version")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowModelVersionController {

    private final FlowModelVersionService flowModelVersionService;

    @GetMapping("/list")
    public RespInfo<IPage<FlowModelVersion>> pageVersionList(
            @RequestParam String modelId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<FlowModelVersion> page = new Page<>(pageNum, pageSize);
        return RespInfo.success(flowModelVersionService.pageVersionList(page, modelId));
    }

    @GetMapping("/{versionId}")
    public RespInfo<VersionDetailVO> getVersionDetail(@PathVariable String versionId) {
        return RespInfo.success(flowModelVersionService.getVersionDetail(versionId));
    }

    @PostMapping("/compare")
    public RespInfo<VersionCompareVO> compareVersions(@RequestBody VersionCompareDTO dto) {
        return RespInfo.success(flowModelVersionService.compareVersions(dto));
    }

    @PostMapping("/revert")
    public RespInfo<VersionRevertVO> revertVersion(@RequestBody VersionRevertDTO dto) {
        VersionRevertVO vo = flowModelVersionService.revertVersion(dto);
        return RespInfo.success("回退成功，正在运行的 " + vo.getRunningInstances() + " 个实例将继续按旧版本执行", vo);
    }

    @PutMapping("/{versionId}/tag")
    public RespInfo<Void> updateVersionTag(@PathVariable String versionId, @RequestBody VersionTagUpdateDTO dto) {
        flowModelVersionService.updateVersionTag(versionId, dto.getVersionTag());
        return RespInfo.success();
    }

    @DeleteMapping("/{versionId}")
    public RespInfo<Void> deleteVersion(@PathVariable String versionId) {
        flowModelVersionService.deleteVersion(versionId);
        return RespInfo.success();
    }

    @GetMapping("/download/{versionId}")
    public ResponseEntity<byte[]> downloadVersion(@PathVariable String versionId) {
        VersionDetailVO version = flowModelVersionService.getVersionDetail(versionId);
        if (version.getBpmnXml() == null || version.getBpmnXml().isBlank()) {
            throw new BusinessException("该版本没有 BPMN XML，无法下载");
        }

        String filename = (version.getVersionName() == null || version.getVersionName().isBlank())
                ? "version-" + version.getVersion()
                : version.getVersionName();
        byte[] content = version.getBpmnXml().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/xml;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename + ".bpmn20.xml", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(content.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }
}
