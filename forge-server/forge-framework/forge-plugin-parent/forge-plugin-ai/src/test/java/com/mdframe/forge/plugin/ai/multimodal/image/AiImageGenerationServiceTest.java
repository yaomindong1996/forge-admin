package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiImageGenerationServiceTest {
    @AfterEach
    void clear() {
        TenantContextHolder.clear();
        assertThat(LoginUserContextHolder.current()).isNull();
    }

    @Test
    void should_returnTaskIdBeforeCompletion_then_storeValidatedImageFromTrustedWorker() throws Exception {
        try (var fixture = new AiImageGenerationFixture(); var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var identity = new AtomicReference<String>();
            when(fixture.adapter.generate(anyString(), anyString(), anyString(), anyString(),
                    nullable(String.class), anyString())).thenAnswer(call -> {
                        identity.set(SessionHelper.getUserId() + ":" + TenantContextHolder.getTenantId()
                                + ":" + Thread.currentThread().getName());
                        started.countDown();
                        if (!release.await(3, TimeUnit.SECONDS)) {
                            throw new AssertionError("fixture image gate timed out");
                        }
                        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(
                                AiGeneratedImageStoreTest.image("png", 2, 2));
                    });
            Long id = fixture.service.generate(request());
            try {
                assertThat(id).isEqualTo(101L);
                assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(fixture.service.getResult(id).getStatus()).isEqualTo("generating");
                assertThat(fixture.service.getResult(id).getResultFileId()).isNull();
                assertThat(identity.get()).startsWith("31:7:forge-ai-image-");
            } finally {
                release.countDown();
            }
            assertThat(fixture.completed.await(3, TimeUnit.SECONDS)).isTrue();
            fixture.pool.submit(() -> { }).get(3, TimeUnit.SECONDS);
            assertThat(fixture.service.getResult(id).getResultFileId()).isEqualTo(501L);
            assertThat(fixture.states).containsExactly("pending", "generating", "success");
            try (var foreign = LoginUserContextHolder.open(user(99L, 7L))) {
                assertThatThrownBy(() -> fixture.service.getResult(id)).hasMessageContaining("不可访问");
            }
            try (var foreign = LoginUserContextHolder.open(user(31L, 8L))) {
                assertThatThrownBy(() -> fixture.service.getResult(id)).hasMessageContaining("不可访问");
            }
        }
    }

    @Test
    void should_rebuildAuthoritativeRecord_and_notPersistInjectedIdentityStateOrResult() throws Exception {
        try (var fixture = new AiImageGenerationFixture(); var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var request = request();
            request.setId(999L);
            request.setTenantId(9L);
            request.setUserId(99L);
            request.setProviderId(999L);
            request.setStatus("success");
            request.setErrorMsg("injected");
            request.setResultFileId(999L);
            request.setDelFlag(999L);
            Long id = fixture.service(mock(AiImageTaskExecutor.class)).generate(request);
            var stored = fixture.rows.get(id);
            assertThat(stored.getTenantId()).isEqualTo(7L);
            assertThat(stored.getUserId()).isEqualTo(31L);
            assertThat(stored.getProviderId()).isEqualTo(81L);
            assertThat(stored.getStatus()).isEqualTo("pending");
            assertThat(stored.getErrorMsg()).isNull();
            assertThat(stored.getResultFileId()).isNull();
            assertThat(stored.getDelFlag()).isZero();
            assertThat(stored.getSize()).isEqualTo("1024x1024");
            assertThat(request.getId()).isEqualTo(999L);
        }
    }

    @Test
    void should_recheckDisabledModelAfterQueueing_without_supplierOrStorageCall() throws Exception {
        try (var fixture = new AiImageGenerationFixture(); var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var release = blockPool(fixture);
            Long id = fixture.service.generate(request());
            assertThat(fixture.service.getResult(id).getStatus()).isEqualTo("pending");
            when(fixture.models.selectEnabledById(89L)).thenReturn(null);
            release.countDown();
            assertThat(fixture.completed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(fixture.service.getResult(id).getStatus()).isEqualTo("failed");
            assertThat(fixture.states).containsExactly("pending", "generating", "failed");
            verify(fixture.adapter, never()).generate(anyString(), anyString(), anyString(), anyString(),
                    nullable(String.class), anyString());
            verifyNoInteractions(fixture.files, fixture.crypto);
        }
    }

    @Test
    void should_persistRejectedTaskAsFailed_when_poolIsSaturated() throws Exception {
        try (var fixture = new AiImageGenerationFixture(); var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var release = blockPool(fixture);
            fixture.pool.execute(() -> { });
            try {
                assertThatThrownBy(() -> fixture.service.generate(request())).hasMessageContaining("繁忙");
                assertThat(fixture.rows.get(101L).getStatus()).isEqualTo("failed");
                assertThat(fixture.states).containsExactly("pending", "failed");
                verifyNoInteractions(fixture.files);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void should_notLeakProviderDiagnostics_and_notRegenerateTerminalRecord() throws Exception {
        try (var fixture = new AiImageGenerationFixture(); var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            when(fixture.adapter.generate(anyString(), anyString(), anyString(), anyString(),
                    nullable(String.class), anyString())).thenThrow(
                            new IllegalStateException("fixture-private-credential "
                                    + "https://cdn.invalid/?signature=private"));
            Long id = fixture.service.generate(request());
            assertThat(fixture.completed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(fixture.service.getResult(id).getErrorMsg()).contains("图片生成或存储失败")
                    .doesNotContain("credential", "signature", "cdn.invalid");
            fixture.worker.run(id);
            verify(fixture.adapter).generate(anyString(), anyString(), anyString(), anyString(),
                    nullable(String.class), anyString());
            assertThat(fixture.states).containsExactly("pending", "generating", "failed");
            verifyNoInteractions(fixture.files);
        }
    }

    @Test
    void should_rejectInvalidInputAndForeignModel_without_creatingTask() throws Exception {
        try (var fixture = new AiImageGenerationFixture()) {
            assertThatThrownBy(() -> fixture.service.generate(request())).hasMessageContaining("登录");
            assertThatThrownBy(() -> fixture.service.getResult(101L)).hasMessageContaining("登录");
            assertThatThrownBy(() -> fixture.worker.run(101L)).hasMessageContaining("登录");
            verify(fixture.records, never()).selectById(org.mockito.ArgumentMatchers.anyLong());
            try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
                var invalid = request();
                invalid.setPrompt(" ");
                assertThatThrownBy(() -> fixture.service.generate(invalid)).hasMessageContaining("提示词");
                invalid.setPrompt("x".repeat(6001));
                assertThatThrownBy(() -> fixture.service.generate(invalid)).hasMessageContaining("6000");
                invalid.setPrompt("fixture");
                invalid.setSize("bogus");
                assertThatThrownBy(() -> fixture.service.generate(invalid)).hasMessageContaining("尺寸");
                fixture.model.setTenantId(8L);
                assertThatThrownBy(() -> fixture.service.generate(request())).hasMessageContaining("不可访问");
                fixture.model.setTenantId(7L);
                fixture.model.setModelType("chat");
                assertThatThrownBy(() -> fixture.service.generate(request())).hasMessageContaining("图像模型");
                fixture.model.setModelType("image_generation");
                fixture.provider.setTenantId(8L);
                assertThatThrownBy(() -> fixture.service.generate(request())).hasMessageContaining("供应商");
                assertThat(fixture.rows).isEmpty();
            }
        }
    }

    private CountDownLatch blockPool(AiImageGenerationFixture fixture) throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        fixture.pool.execute(() -> {
            started.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
        return release;
    }

    private AiImageGenerateRecord request() {
        var request = new AiImageGenerateRecord();
        request.setModelId(89L);
        request.setPrompt("fixture");
        return request;
    }

    private LoginUser user(Long id, Long tenant) {
        var user = new LoginUser();
        user.setUserId(id);
        user.setTenantId(tenant);
        return user;
    }
}
