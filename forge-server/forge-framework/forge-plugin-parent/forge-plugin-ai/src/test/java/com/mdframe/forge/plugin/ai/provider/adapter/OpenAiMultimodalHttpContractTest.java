package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.multimodal.image.adapter.OpenAiCompatibleImageModelAdapter;
import com.mdframe.forge.plugin.ai.multimodal.voice.adapter.OpenAiCompatibleAsrModelAdapter;
import com.mdframe.forge.plugin.ai.multimodal.voice.adapter.OpenAiCompatibleTtsModelAdapter;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 实际迁移适配器的图像/ASR/TTS 协议测试，合成媒体不写文件也不访问外部地址。 */
@Timeout(10)
class OpenAiMultimodalHttpContractTest {

    private AiLoopbackFixture fixture;
    private static final String KEY = "t1-fixture-key";

    @BeforeEach
    void start() throws IOException {
        fixture = new AiLoopbackFixture();
    }

    @AfterEach
    void stop() {
        fixture.close();
    }

    @Test
    void generatesImageWithConfiguredSizeAndWithoutFetchingResult() {
        var result = new OpenAiCompatibleImageModelAdapter().generate(fixture.baseUrl(), KEY,
                "dall-e-3", "fixture prompt", null, "1024x1024");
        assertThat(result).isEqualTo("https://fixture.invalid/image.png");
        var request = fixture.request();
        assertThat(request.path()).isEqualTo("/v1/images/generations");
        assertThat(request.authorization()).isEqualTo("Bearer " + KEY);
        assertThat(request.body().path("size").asText()).isEqualTo("1024x1024");
        assertThat(request.body().path("prompt").asText()).isEqualTo("fixture prompt");
        assertThat(fixture.request()).isNull();
    }

    @Test
    void transcribesUsingMultipartWithTheRequiredFilename() {
        var result = new OpenAiCompatibleAsrModelAdapter().transcribe(fixture.baseUrl() + "/v1", KEY,
                "whisper-1", "fixture audio".getBytes(StandardCharsets.UTF_8), "audio/wav");
        assertThat(result).isEqualTo("fixture transcription");
        var request = fixture.request();
        assertThat(request.path()).isEqualTo("/v1/audio/transcriptions");
        assertThat(request.authorization()).isEqualTo("Bearer " + KEY);
        assertThat(request.body().path("rawPayload").asText())
                .contains("filename=\"audio.wav\"", "whisper-1", "fixture audio");
    }

    @Test
    void synthesizesBinaryAudioThroughOfficialSdk() {
        var result = new OpenAiCompatibleTtsModelAdapter().synthesize(fixture.baseUrl(), KEY,
                "tts-1", "fixture text");
        assertThat(result).isEqualTo("fixture audio".getBytes(StandardCharsets.UTF_8));
        var request = fixture.request();
        assertThat(request.path()).isEqualTo("/v1/audio/speech");
        assertThat(request.body().path("input").asText()).isEqualTo("fixture text");
        assertThat(request.body().path("model").asText()).isEqualTo("tts-1");
    }

    @Test
    void doesNotExposeVendorErrorBodiesFromMultimodalAdapters() {
        fixture.respondOnce(503, "{\"error\":{\"message\":\"t1-fixture-key private prompt\"}}", false);
        assertThatThrownBy(() -> new OpenAiCompatibleImageModelAdapter().generate(fixture.baseUrl(), KEY,
                "dall-e-3", "fixture prompt", null, null))
                .isInstanceOf(BusinessException.class).hasMessage("图片生成失败，请稍后重试");
        assertThat(fixture.request()).isNotNull();
        assertThat(fixture.request()).isNull();
    }
}
