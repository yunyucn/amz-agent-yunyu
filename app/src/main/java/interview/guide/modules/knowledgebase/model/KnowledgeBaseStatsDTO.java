package interview.guide.modules.knowledgebase.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识库统计信息DTO
 */
@Schema(description = "知识库统计信息")
public record KnowledgeBaseStatsDTO(
    @Schema(description = "知识库总数", example = "24")
    long totalCount,           // 知识库总数
    @Schema(description = "总提问次数", example = "186")
    long totalQuestionCount,   // 总提问次数
    @Schema(description = "总访问次数", example = "420")
    long totalAccessCount,     // 总访问次数
    @Schema(description = "已完成向量化数量", example = "20")
    long completedCount,       // 已完成向量化数量
    @Schema(description = "处理中数量", example = "2")
    long processingCount       // 处理中数量
) {
}
