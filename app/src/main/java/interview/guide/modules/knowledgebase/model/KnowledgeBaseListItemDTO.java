package interview.guide.modules.knowledgebase.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 知识库列表项DTO
 * 使用MapStruct进行转换，见KnowledgeBaseMapper
 */
@Schema(description = "知识库列表项")
public record KnowledgeBaseListItemDTO(
    @Schema(description = "知识库ID", example = "201")
    Long id,
    @Schema(description = "知识库名称", example = "MySQL面试题库")
    String name,
    @Schema(description = "分类", example = "数据库")
    String category,
    @Schema(description = "原始文件名", example = "mysql-guide.pdf")
    String originalFilename,
    @Schema(description = "文件大小(字节)", example = "102400")
    Long fileSize,
    @Schema(description = "内容类型", example = "application/pdf")
    String contentType,
    @Schema(description = "上传时间", example = "2026-04-14T08:00:00")
    LocalDateTime uploadedAt,
    @Schema(description = "最近访问时间", example = "2026-04-14T10:20:00")
    LocalDateTime lastAccessedAt,
    @Schema(description = "访问次数", example = "16")
    Integer accessCount,
    @Schema(description = "问答次数", example = "9")
    Integer questionCount,
    @Schema(description = "向量化状态", example = "COMPLETED")
    VectorStatus vectorStatus,
    @Schema(description = "向量化错误信息", example = "")
    String vectorError,
    @Schema(description = "文档切片数量", example = "128")
    Integer chunkCount
) {
}

