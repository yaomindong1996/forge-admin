package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.dto.PluginArtifactMetadataDTO;
import com.mdframe.forge.plugin.system.dto.PluginArtifactRegisterDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.service.plugin.*;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/** 独占实际XML/H2/Spring事务；真实MySQL/登录仍需目标环境验收。 */
final class PluginArtifactTestDatabase implements AutoCloseable {
    final String id = "00000000-0000-4000-8000-00000000000a";
    final PluginReviewTestDatabase db = new PluginReviewTestDatabase();
    final PluginTaskActor actor = new PluginTaskActor(1L, 9L, 1L);
    final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    final PluginArtifactCodec codec = new PluginArtifactCodec(db.json, validators.getValidator());
    final PluginReleaseApprovalValidator approvals = new PluginReleaseApprovalValidator(db.json);
    final PluginArtifactRegistrationService registrations;
    final PluginArtifactViews views = new PluginArtifactViews(db.artifacts, db.reviews, codec, approvals);
    final PluginArtifactMetadataDTO metadata;

    PluginArtifactTestDatabase() throws Exception {
        db.execute("UPDATE sys_plugin_task SET id = '" + id + "'");
        db.execute("UPDATE sys_plugin_build SET id = '" + id + "'");
        var result = approve();
        metadata = metadata(result);
        var target = new PluginArtifactRegistrationService(db.builds, db.tasks, db.reviews,
                db.artifacts, approvals, codec);
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(db.source),
                new AnnotationTransactionAttributeSource()));
        registrations = (PluginArtifactRegistrationService) proxy.getProxy();
    }

    PluginArtifactRegisterDTO command() {
        var command = new PluginArtifactRegisterDTO();
        command.setRequestId(UUID.randomUUID().toString());
        command.setMetadataJson(codec.encode(metadata));
        command.setLocalVerified(true);
        command.setNotDeployed(true);
        command.setNote("已逐文件复验本次候选，核对构建结果且尚未部署");
        return command;
    }

    void closeTask() {
        var close = db.close();
        close.setRevision(3);
        close.setResultSha256(metadata.getServerResultSha256());
        db.service.review(id, close, actor);
    }

    private PluginBuildResultDTO approve() throws Exception {
        var build = db.builds.selectBuild(1L, id);
        var report = new PluginBuildResultDTO();
        report.setSuccess(true);
        report.setJobId("job-test");
        report.setPackageSha256("a".repeat(64));
        report.setSourceCommit(build.getSourceCommit());
        report.setImage(build.getImage());
        report.setPhase("artifact_verification");
        report.setSourceSha256("e".repeat(64));
        report.setArtifactCount(1);
        report.setArtifactBytes(100L);
        report.setArtifactManifestSha256("f".repeat(64));
        String encoded = db.json.writeValueAsString(report);
        build.setResultJson(encoded);
        build.setResultSha256(PackageDigests.sha256(encoded.getBytes(StandardCharsets.UTF_8)));
        build.setFinishedTime(LocalDateTime.now());
        build.setPhase("artifact_verification");
        db.builds.updateById(build);
        db.execute("UPDATE sys_plugin_task SET task_status = 'built', preview_json = '{\"coreVersion\":\""
                + ForgeVersion.CURRENT + "\"}'");
        var review = db.close();
        review.setDecision("approve_build");
        review.setResultSha256(build.getResultSha256());
        review.setArtifactsReviewed(true);
        review.setMigrationsReviewed(true);
        db.service.review(id, review, actor);
        return report;
    }

    private PluginArtifactMetadataDTO metadata(PluginBuildResultDTO report) {
        var value = new PluginArtifactMetadataDTO();
        value.setProtocolVersion(1);
        value.setTaskId(id);
        value.setReviewId(db.reviews.selectRecent(1L, id).get(0).getId());
        value.setRevision(3);
        value.setServerResultSha256(db.builds.selectBuild(1L, id).getResultSha256());
        value.setReleaseId("rel-" + "b".repeat(64));
        value.setManifestSha256("b".repeat(64));
        value.setRepositoryId("local-test");
        value.setResultSha256("c".repeat(64));
        value.setPluginId("demo");
        value.setPluginVersion("1.0.0");
        value.setCoreVersion(ForgeVersion.CURRENT);
        value.setOperation("install");
        value.setResult(report);
        return value;
    }

    @Override
    public void close() { validators.close(); }
}
