package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.plugin.ai.agent.engine.context.AgentExecutionTaskDecorator;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.multimodal.image.adapter.AiImageModelAdapter;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.plugin.ai.multimodal.image.mapper.AiImageGenerateRecordMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 实体副本 + 条件状态更新，避免 Mock 共享可变实体把 CAS 竞争变成假通过。 */
final class AiImageGenerationFixture implements AutoCloseable {
    final AiImageGenerateRecordMapper records = mock(AiImageGenerateRecordMapper.class);
    final AiModelMapper models = mock(AiModelMapper.class);
    final AiProviderMapper providers = mock(AiProviderMapper.class);
    final AiImageModelAdapter adapter = mock(AiImageModelAdapter.class);
    final FileManager files = mock(FileManager.class);
    final AiSecretCrypto crypto = mock(AiSecretCrypto.class);
    final Map<Long, AiImageGenerateRecord> rows = new ConcurrentHashMap<>();
    final List<String> states = new CopyOnWriteArrayList<>();
    final CountDownLatch completed = new CountDownLatch(1);
    final ThreadPoolTaskExecutor pool = AiImageExecutorConfigurationTest.pool();
    final AiImageModelResolver resolver = new AiImageModelResolver(models, providers);
    final AiImageGenerationWorker worker;
    final AiImageGenerationService service;
    final AiModel model = new AiModel();
    final AiProvider provider = new AiProvider();
    private final AtomicLong sequence = new AtomicLong(100);

    AiImageGenerationFixture() throws Exception {
        model.setId(89L);
        model.setTenantId(7L);
        model.setProviderId(81L);
        model.setModelType("image_generation");
        model.setModelId("fixture-image");
        provider.setId(81L);
        provider.setTenantId(7L);
        provider.setBaseUrl("https://provider.example.invalid/v1");
        provider.setApiKey("fixture-private-credential");
        when(crypto.decrypt("fixture-private-credential")).thenReturn("fixture-decrypted-value");
        when(models.selectEnabledById(89L)).thenReturn(model);
        when(providers.selectEnabledById(81L)).thenReturn(provider);
        when(adapter.supports("fixture-image")).thenReturn(true);
        byte[] png = AiGeneratedImageStoreTest.image("png", 2, 2);
        when(adapter.generate(anyString(), anyString(), anyString(), anyString(), nullable(String.class), anyString()))
                .thenReturn("data:image/png;base64," + Base64.getEncoder().encodeToString(png));
        when(files.upload(any(InputStream.class), eq("ai-image.png"), eq("image/png"),
                eq("ai_image_generate"), isNull(), isNull(), eq(true), anyLong()))
                .thenReturn(FileMetadata.builder().fileId("501").build());
        when(records.insert(any(AiImageGenerateRecord.class))).thenAnswer(call -> {
            AiImageGenerateRecord row = call.getArgument(0);
            row.setId(sequence.incrementAndGet());
            rows.put(row.getId(), copy(row));
            states.add(row.getStatus());
            return 1;
        });
        when(records.selectById(anyLong())).thenAnswer(call -> copy(rows.get(call.getArgument(0))));
        when(records.transition(any(), anyString())).thenAnswer(call -> transition(call.getArgument(0),
                call.getArgument(1)));
        var properties = new AiImageGenerationProperties();
        var store = new AiGeneratedImageStore(mock(SecureOutboundClient.class), files, properties,
                new AiImageContentValidator(properties));
        var adapters = new AiModelAdapterRegistry(List.of(), List.of(), List.of(adapter), List.of(), List.of());
        worker = new AiImageGenerationWorker(records, resolver, adapters, crypto, store);
        service = service(new AiImageTaskExecutor(pool, new AgentExecutionTaskDecorator()));
    }

    AiImageGenerationService service(AiImageTaskExecutor executor) {
        return new AiImageGenerationService(records, resolver, worker, executor);
    }

    private synchronized int transition(AiImageGenerateRecord record, String expected) {
        AiImageGenerateRecord stored = rows.get(record.getId());
        if (stored == null || !expected.equals(stored.getStatus())
                || !stored.getTenantId().equals(record.getTenantId())
                || !stored.getUserId().equals(record.getUserId())) {
            return 0;
        }
        rows.put(record.getId(), copy(record));
        states.add(record.getStatus());
        if ("success".equals(record.getStatus()) || "failed".equals(record.getStatus())) {
            completed.countDown();
        }
        return 1;
    }

    static AiImageGenerateRecord copy(AiImageGenerateRecord row) {
        if (row == null) {
            return null;
        }
        var copy = new AiImageGenerateRecord();
        BeanUtils.copyProperties(row, copy);
        return copy;
    }

    @Override
    public void close() {
        pool.shutdown();
    }
}
