package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SysPluginUploadDTO {
    @NotNull
    private MultipartFile file;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")
    private String requestId;
}
