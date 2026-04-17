package interview.guide.modules.interview.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 提交答案响应
 */
@Schema(description = "提交答案后的响应")
public record SubmitAnswerResponse(
    @Schema(description = "是否有下一题", example = "true")
    boolean hasNextQuestion,
    @Schema(description = "下一题内容，没有则为null")
    InterviewQuestionDTO nextQuestion,
    @Schema(description = "当前题目索引", example = "1")
    int currentIndex,
    @Schema(description = "总题目数", example = "5")
    int totalQuestions
) {}
