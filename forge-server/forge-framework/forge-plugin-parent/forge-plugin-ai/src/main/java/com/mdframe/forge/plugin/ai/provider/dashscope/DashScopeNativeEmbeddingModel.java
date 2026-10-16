package com.mdframe.forge.plugin.ai.provider.dashscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 原生文本嵌入按保守的 10 条批量限制发送，按 text_index 还原顺序。 */
public final class DashScopeNativeEmbeddingModel extends AbstractEmbeddingModel {

    private static final int BATCH_SIZE = 10;
    private final DashScopeNativeClient client;
    private final String model;

    public DashScopeNativeEmbeddingModel(String baseUrl, String apiKey, String model) {
        this.client = new DashScopeNativeClient(baseUrl, apiKey);
        this.model = model;
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        String selected = request.getOptions() != null && request.getOptions().getModel() != null
                ? request.getOptions().getModel() : model;
        List<Embedding> results = new ArrayList<>();
        int tokens = 0;
        for (int offset = 0; offset < request.getInstructions().size(); offset += BATCH_SIZE) {
            List<String> texts = request.getInstructions()
                    .subList(offset, Math.min(offset + BATCH_SIZE, request.getInstructions().size()));
            var body = DashScopeNativeClient.JSON.createObjectNode().put("model", selected);
            body.putObject("input").set("texts", DashScopeNativeClient.JSON.valueToTree(texts));
            var parameters = body.putObject("parameters").put("text_type", "document");
            if (request.getOptions() != null && request.getOptions().getDimensions() != null) {
                parameters.put("dimension", request.getOptions().getDimensions());
            }
            JsonNode response = client.post("/services/embeddings/text-embedding/text-embedding", body);
            results.addAll(embeddings(response, offset, texts.size()));
            tokens += response.path("usage").path("total_tokens").asInt();
        }
        return new EmbeddingResponse(results,
                new EmbeddingResponseMetadata(selected, new DefaultUsage(tokens, 0, tokens)));
    }

    private List<Embedding> embeddings(JsonNode response, int offset, int expected) {
        List<Embedding> results = new ArrayList<>();
        for (JsonNode item : response.path("output").path("embeddings")) {
            JsonNode values = item.path("embedding");
            if (!values.isArray() || values.isEmpty()) {
                throw new BusinessException("通义原生嵌入返回空向量");
            }
            float[] vector = new float[values.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) values.get(i).asDouble();
            }
            results.add(new Embedding(vector, offset + item.path("text_index").asInt(-1)));
        }
        results.sort(Comparator.comparingInt(Embedding::getIndex));
        if (results.size() != expected) {
            throw new BusinessException("通义原生嵌入返回条数不一致");
        }
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getIndex() != offset + i) {
                throw new BusinessException("通义原生嵌入返回索引不一致");
            }
        }
        return results;
    }
}
