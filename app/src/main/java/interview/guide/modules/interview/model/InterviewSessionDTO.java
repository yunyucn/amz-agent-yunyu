package interview.guide.modules.interview.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 面试会话DTO
 */
@Schema(description = "面试会话信息")
public record InterviewSessionDTO(
    @Schema(description = "会话ID", example = "sess_20260414_001")
    String sessionId,
    @Schema(description = "简历文本", example = "5年Java开发经验...")
    String resumeText,
    @Schema(description = "总题目数", example = "5")
    int totalQuestions,
    @Schema(description = "当前题目索引", example = "1")
    int currentQuestionIndex,
    @Schema(description = "题目列表")
    List<InterviewQuestionDTO> questions,
    @Schema(description = "会话状态", example = "IN_PROGRESS")
    SessionStatus status
) {
    public enum SessionStatus {
        CREATED,      // 会话已创建
        IN_PROGRESS,  // 面试进行中
        COMPLETED,    // 面试已完成
        EVALUATED     // 已生成评估报告
    }
}
