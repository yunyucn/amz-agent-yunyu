package interview.guide.modules.knowledgebase.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识库查询响应
 */
@Schema(description = "知识库问答响应")
public record QueryResponse(
    @Schema(description = "回答内容", example = "索引下推是把过滤条件尽量下沉到存储层执行...")
    String answer,
    @Schema(description = "命中的主知识库ID", example = "201")
    Long knowledgeBaseId,
    @Schema(description = "命中的主知识库名称", example = "MySQL面试题库")
    String knowledgeBaseName
) {}

