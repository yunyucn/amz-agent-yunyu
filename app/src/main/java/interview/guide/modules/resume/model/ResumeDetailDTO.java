package interview.guide.modules.resume.model;

import interview.guide.common.model.AsyncTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历详情DTO
 */
@Schema(description = "简历详情")
public record ResumeDetailDTO(
    @Schema(description = "简历ID", example = "101")
    Long id,
    @Schema(description = "文件名", example = "zhangsan_resume.pdf")
    String filename,
    Long fileSize,
    String contentType,
    String storageUrl,
    LocalDateTime uploadedAt,
    Integer accessCount,
    String resumeText,
    AsyncTaskStatus analyzeStatus,
    String analyzeError,
    @Schema(description = "分析历史")
    List<AnalysisHistoryDTO> analyses,
    @Schema(description = "关联面试记录")
    List<Object> interviews  // 面试历史由InterviewHistoryService提供
) {
    /**
     * 分析历史DTO
     */
    public record AnalysisHistoryDTO(
        Long id,
        Integer overallScore,
        Integer contentScore,
        Integer structureScore,
        Integer skillMatchScore,
        Integer expressionScore,
        Integer projectScore,
        String summary,
        LocalDateTime analyzedAt,
        List<String> strengths,
        List<Object> suggestions
    ) {}
}

