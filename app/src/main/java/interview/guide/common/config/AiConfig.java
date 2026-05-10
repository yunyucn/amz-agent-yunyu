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
 * 手动创建 EmbeddingModel Bean 并指定阿里云百炼 URL，
 * 绕开 Spring AI 2.0.0-M1 自动配置 Bug（OpenAiEmbeddingModel 忽略
 * spring.ai.openai.embedding.base-url，始终回退到全局 base-url DeepSeek，
 * 导致 /v1/embeddings 返回 404）。
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
                        .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                        .apiKey(embeddingApiKey)
                        .build()
        );
    }
}
