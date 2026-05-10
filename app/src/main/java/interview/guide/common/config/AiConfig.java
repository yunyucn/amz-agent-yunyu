package interview.guide.common.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI 模型配置
 * <p>
 * 手动创建 EmbeddingModel Bean 并指定阿里云百炼 URL。
 * OpenAiApi 在 baseUrl 后固定追加 /v1/embeddings，因此 baseUrl 末尾不含 /v1，
 * 避免双 /v1 导致 404。
 */
@Configuration
public class AiConfig {

    @Value("${AI_EMBEDDING_API_KEY}")
    private String embeddingApiKey;

    @Bean
    @Primary
    public OpenAiEmbeddingModel embeddingModel() {
        return new OpenAiEmbeddingModel(
                OpenAiApi.builder()
                        .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                        .apiKey(embeddingApiKey)
                        .build()
        );
    }
}
