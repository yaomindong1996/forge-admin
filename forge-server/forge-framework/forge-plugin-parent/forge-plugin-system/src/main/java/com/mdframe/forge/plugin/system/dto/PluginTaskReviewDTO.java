package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** 人工核查声明，不接受部署地址、命令、租户或用户。 */
@Data
public class PluginTaskReviewDTO {
    @NotNull
    @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    private String requestId;
    @NotNull
    @Min(0)
    private Integer revision;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{64}")
    private String sha256;
    @Pattern(regexp = "[a-f0-9]{64}")
    private String resultSha256;
    @NotNull
    @Pattern(regexp = "approve_build|close_task")
    private String decision;
    @NotNull
    @AssertTrue
    private Boolean executorStopped;
    @NotNull
    @AssertTrue
    private Boolean notDeployed;
    private Boolean artifactsReviewed;
    private Boolean migrationsReviewed;
    @NotBlank
    @Size(min = 10, max = 1000)
    @ToString.Exclude
    private String note;
}
