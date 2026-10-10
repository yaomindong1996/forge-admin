package com.mdframe.forge.plugin.system.service.plugin;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.mdframe.forge.plugin.system.dto.PluginArtifactMetadataDTO;
import com.mdframe.forge.plugin.system.dto.PluginArtifactRegisterDTO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import com.mdframe.forge.starter.plugin.version.SemanticVersion;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** 严格导入协议独立于全局宽松JSON设置；只保存校验后的规范元数据。 */
@Component
@RequiredArgsConstructor
public class PluginArtifactCodec {
    private final ObjectMapper json;
    private final Validator validator;

    public PluginArtifactMetadataDTO parse(String text) {
        require(text != null && text.getBytes(StandardCharsets.UTF_8).length <= 65536, "登记元数据不得超过64 KiB");
        try {
            var strict = json.copy().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
            strict.getFactory().setStreamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(16).build());
            var reader = strict
                    .readerFor(PluginArtifactMetadataDTO.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            PluginArtifactMetadataDTO value = reader.readValue(text);
            require(value != null && validator.validate(value).isEmpty(), "登记元数据字段不完整或格式错误");
            require(value.getReleaseId().equals("rel-" + value.getManifestSha256()), "候选编号与清单摘要不一致");
            SemanticVersion.parse(value.getPluginVersion());
            SemanticVersion.parse(value.getCoreVersion());
            var report = value.getResult();
            require(Boolean.TRUE.equals(report.getSuccess()) && report.getFailureCode() == null
                    && report.getSourceSha256() != null && report.getArtifactManifestSha256() != null
                    && report.getArtifactCount() != null && report.getArtifactBytes() != null,
                    "仅成功且有完整产物报告的候选可登记");
            return value;
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            throw new BusinessException(400, "登记元数据JSON或版本格式错误，禁止未知/重复字段");
        }
    }

    public void normalize(PluginArtifactRegisterDTO command, PluginArtifactMetadataDTO metadata) {
        require(validator.validate(command).isEmpty(), "登记请求字段不完整或格式错误");
        require(Boolean.TRUE.equals(command.getLocalVerified()) && Boolean.TRUE.equals(command.getNotDeployed()),
                "请人工确认本地制品已复验且尚未部署");
        command.setNote(command.getNote().strip());
        require(command.getNote().length() >= 10, "请填写10到1000字核查说明，不要包含凭证");
        command.setMetadataJson(encode(metadata));
    }

    public String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("登记元数据无法序列化", failure);
        }
    }

    public String digest(PluginArtifactRegisterDTO command) {
        return PackageDigests.sha256(encode(command).getBytes(StandardCharsets.UTF_8));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(400, message);
        }
    }
}
