package interview.guide.modules.resume.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 简历列表项DTO
 */
@Schema(description = "简历列表项")
public record ResumeListItemDTO(
    @Schema(description = "简历ID", example = "101")
    Long id,
    @Schema(description = "文件名", example = "zhangsan_resume.pdf")
    String filename,
    @Schema(description = "文件大小(字节)", example = "245760")
    Long fileSize,
    @Schema(description = "上传时间", example = "2026-04-14T09:30:00")
    LocalDateTime uploadedAt,
    @Schema(description = "访问次数", example = "3")
    Integer accessCount,
    @Schema(description = "最新综合评分", example = "86")
    Integer latestScore,
    @Schema(description = "最近分析时间", example = "2026-04-14T09:35:00")
    LocalDateTime lastAnalyzedAt,
    @Schema(description = "关联面试次数", example = "2")
    Integer interviewCount
) {}

