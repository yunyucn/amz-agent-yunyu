package interview.guide.modules.interview.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 面试评估报告
 */
@Schema(description = "面试评估报告")
public record InterviewReportDTO(
    @Schema(description = "会话ID", example = "sess_20260414_001")
    String sessionId,
    @Schema(description = "总题目数", example = "5")
    int totalQuestions,
    @Schema(description = "总分(0-100)", example = "84")
    int overallScore,                          // 总分 (0-100)
    List<CategoryScore> categoryScores,        // 各类别得分
    List<QuestionEvaluation> questionDetails,  // 每题详情
    @Schema(description = "总体评价", example = "基础扎实，表达清晰，系统设计可进一步加强。")
    String overallFeedback,                    // 总体评价
    List<String> strengths,                    // 优势
    List<String> improvements,                 // 改进建议
    List<ReferenceAnswer> referenceAnswers     // 参考答案
) {
    /**
     * 类别得分
     */
    public record CategoryScore(
        String category,
        int score,
        int questionCount
    ) {}
    
    /**
     * 问题评估详情
     */
    public record QuestionEvaluation(
        int questionIndex,
        String question,
        String category,
        String userAnswer,
        int score,
        String feedback
    ) {}
    
    /**
     * 参考答案
     */
    public record ReferenceAnswer(
        int questionIndex,
        String question,
        String referenceAnswer,
        List<String> keyPoints
    ) {}
}
