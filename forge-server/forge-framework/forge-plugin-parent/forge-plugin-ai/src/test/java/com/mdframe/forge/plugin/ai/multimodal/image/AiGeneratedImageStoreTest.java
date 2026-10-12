package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiGeneratedImageStoreTest {
    private final SecureOutboundClient outbound = mock(SecureOutboundClient.class);
    private final FileManager files = mock(FileManager.class);
    private final AiImageGenerationProperties properties = new AiImageGenerationProperties();
    private final AiGeneratedImageStore store = new AiGeneratedImageStore(outbound, files, properties,
            new AiImageContentValidator(properties));

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg"})
    void should_storeOnlyValidatedInlineImage_with_correctMimeAndExtension(String format) throws Exception {
        String mime = "image/" + format;
        byte[] bytes = image(format, 2, 2);
        when(files.upload(any(InputStream.class), eq("ai-image" + ("png".equals(format) ? ".png" : ".jpg")),
                eq(mime), eq("ai_image_generate"), isNull(), isNull(), eq(true), anyLong())).thenAnswer(call -> {
                    assertThat(((InputStream) call.getArgument(0)).readAllBytes()).isEqualTo(bytes);
                    assertThat((Long) call.getArgument(7)).isEqualTo((long) bytes.length);
                    return FileMetadata.builder().fileId("101").build();
                });
        assertThat(store.store("data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes)))
                .isEqualTo(101L);
        verifyNoInteractions(outbound);
    }

    @Test
    void should_downloadUsingExistingSceneWithoutCredentials_and_threeSecondBounds() throws Exception {
        byte[] bytes = image("png", 2, 2);
        when(outbound.execute(any())).thenReturn(new OutboundResponse(200,
                Map.of("Content-Type", List.of("image/png; charset=binary")), bytes));
        when(files.upload(any(InputStream.class), eq("ai-image.png"), eq("image/png"),
                eq("ai_image_generate"), isNull(), isNull(), eq(true), anyLong()))
                .thenReturn(FileMetadata.builder().fileId("101").build());
        assertThat(store.store("https://cdn.example.invalid/image?signature=fixture-private" )).isEqualTo(101L);
        var request = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(outbound).execute(request.capture());
        assertThat(request.getValue().getScene()).isEqualTo(OutboundScenes.EXTERNAL_CONNECTOR);
        assertThat(request.getValue().getMethod()).isEqualTo("GET");
        assertThat(request.getValue().getHeaders()).isEmpty();
        assertThat(request.getValue().getBody()).isEmpty();
        assertThat(request.getValue().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(request.getValue().getReadTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(request.getValue().getCallTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"data:image/svg+xml;base64,PHN2Zy8+", "data:text/html;base64,Zm9v",
            "data:image/png,abc", "data:image/png;base64,@@@", "data:image/png;base64,SGVsbG8="})
    void should_rejectUnsupportedEncodingAndFakeImage_before_storage(String source) {
        assertThatThrownBy(() -> store.store(source)).isInstanceOf(
                com.mdframe.forge.starter.core.exception.BusinessException.class);
        verifyNoInteractions(files, outbound);
    }

    @Test
    void should_limitBeforeBase64Decoding_and_validateDecodedSizeTypeAndPixels() throws Exception {
        properties.setMaxImageBytes(32);
        assertThatThrownBy(() -> store.store("data:image/png;base64," + "A".repeat(128)))
                .hasMessageContaining("上限");
        properties.setMaxImageBytes(1024);
        properties.setMaxImagePixels(3);
        assertThatThrownBy(() -> store.store("data:image/png;base64," +
                Base64.getEncoder().encodeToString(image("png", 2, 2)))).hasMessageContaining("像素");
        properties.setMaxImagePixels(16);
        assertThatThrownBy(() -> store.store("data:image/jpeg;base64," +
                Base64.getEncoder().encodeToString(image("png", 2, 2)))).hasMessageContaining("类型");
        verifyNoInteractions(files, outbound);
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 403, 404, 500})
    void should_rejectNonSuccessResponse_without_storingErrorBody(int status) {
        when(outbound.execute(any())).thenReturn(new OutboundResponse(status,
                Map.of("Content-Type", List.of("text/html")), "fixture error".getBytes()));
        assertThatThrownBy(() -> store.store("https://cdn.example.invalid/image"))
                .hasMessageContaining("成功状态");
        verifyNoInteractions(files);
    }

    @Test
    void should_rejectWrongMimeAndOverLimitHttpBody_without_storage() throws Exception {
        byte[] image = image("png", 2, 2);
        when(outbound.execute(any())).thenReturn(new OutboundResponse(200,
                Map.of("Content-Type", List.of("text/html")), image));
        assertThatThrownBy(() -> store.store("https://cdn.example.invalid/image")).hasMessageContaining("格式");
        properties.setMaxImageBytes(1);
        when(outbound.execute(any())).thenReturn(new OutboundResponse(200,
                Map.of("Content-Type", List.of("image/png")), image));
        assertThatThrownBy(() -> store.store("https://cdn.example.invalid/image")).hasMessageContaining("上限");
        verifyNoInteractions(files);
    }

    @Test
    void should_notMakeNetworkRequest_when_sourceIsEmptyOrTooLong() {
        assertThatThrownBy(() -> store.store(null)).hasMessageContaining("没有返回");
        assertThatThrownBy(() -> store.store("https://cdn.example.invalid/" + "a".repeat(8192)))
                .hasMessageContaining("过长");
        verifyNoInteractions(files, outbound);
    }

    static byte[] image(String format, int width, int height) throws Exception {
        var output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output))
                .isTrue();
        return output.toByteArray();
    }
}
