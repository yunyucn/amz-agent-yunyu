package interview.guide.common.ai;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 统一封装结构化输出调用与重试策略。
 */
@Component
public class StructuredOutputInvoker {
    // 结构化输出强约束指令，确保模型只返回可解析 JSON
    private static final String STRICT_JSON_INSTRUCTION = """
请仅返回可被 JSON 解析器直接解析的 JSON 对象，并严格满足字段结构要求：
1) 不要输出 Markdown 代码块（如 ```json）。
2) 不要输出任何解释文字、前后缀、注释。
3) 所有字符串内引号必须正确转义。
""";

    // 结构化输出最大重试次数（最小为 1）
    private final int maxAttempts;
    // 重试时是否注入上次错误信息，帮助模型纠正输出格式
    private final boolean includeLastErrorInRetryPrompt;

    public StructuredOutputInvoker(
        @Value("${app.ai.structured-max-attempts:2}") int maxAttempts,
        @Value("${app.ai.structured-include-last-error:true}") boolean includeLastErrorInRetryPrompt
    ) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.includeLastErrorInRetryPrompt = includeLastErrorInRetryPrompt;
    }

    /**
     * 调用模型并将结果解析为结构化对象，失败时按策略重试。
     */
    public <T> T invoke(
        ChatClient chatClient,
        String systemPromptWithFormat,
        String userPrompt,
        BeanOutputConverter<T> outputConverter,
        ErrorCode errorCode,
        String errorPrefix,
        String logContext,
        Logger log
    ) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String attemptSystemPrompt = attempt == 1
                ? systemPromptWithFormat
                : buildRetrySystemPrompt(systemPromptWithFormat, lastError);
            try {
                return chatClient.prompt()
                    .system(attemptSystemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(outputConverter);
            } catch (Exception e) {
                lastError = e;
                log.warn("{}结构化解析失败，准备重试: attempt={}, error={}", logContext, attempt, e.getMessage());
            }
        }

        throw new BusinessException(
            errorCode,
            errorPrefix + (lastError != null ? lastError.getMessage() : "unknown")
        );
    }

    /**
     * 构造重试轮次的系统提示词，附加严格 JSON 约束与可选错误上下文。
     */
    private String buildRetrySystemPrompt(String systemPromptWithFormat, Exception lastError) {
        StringBuilder prompt = new StringBuilder(systemPromptWithFormat)
            .append("\n\n")
            .append(STRICT_JSON_INSTRUCTION)
            .append("\n上次输出解析失败，请仅返回合法 JSON。");

        if (includeLastErrorInRetryPrompt && lastError != null && lastError.getMessage() != null) {
            prompt.append("\n上次失败原因：")
                .append(sanitizeErrorMessage(lastError.getMessage()));
        }
        return prompt.toString();
    }

    /**
     * 清洗错误信息，避免将多行/超长文本注入到提示词中。
     */
    private String sanitizeErrorMessage(String message) {
        String oneLine = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (oneLine.length() > 200) {
            return oneLine.substring(0, 200) + "...";
        }
        return oneLine;
    }
}
