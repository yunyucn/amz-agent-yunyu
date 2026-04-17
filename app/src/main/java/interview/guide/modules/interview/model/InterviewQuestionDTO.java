package interview.guide.modules.interview.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 面试问题DTO
 */
@Schema(description = "面试问题与作答信息")
public record InterviewQuestionDTO(
    @Schema(description = "问题索引", example = "0")
    int questionIndex,
    @Schema(description = "问题内容", example = "请介绍一下你做过最复杂的项目。")
    String question,
    @Schema(description = "题目类型", example = "PROJECT")
    QuestionType type,
    @Schema(description = "题目分类", example = "项目经历")
    String category,      // 问题类别：项目经历、Java基础、集合、并发、MySQL、Redis、Spring、SpringBoot
    @Schema(description = "用户回答", example = "我主导了订单中心重构...")
    String userAnswer,    // 用户回答
    @Schema(description = "单题得分", example = "82")
    Integer score,        // 单题得分 (0-100)
    @Schema(description = "单题反馈", example = "回答结构完整，可补充量化指标。")
    String feedback,      // 单题反馈
    @Schema(description = "是否追问", example = "false")
    boolean isFollowUp,   // 是否为追问
    @Schema(description = "关联主问题索引", example = "0")
    Integer parentQuestionIndex // 追问关联的主问题索引
) {
    public enum QuestionType {
        PROJECT,          // 项目经历
        JAVA_BASIC,       // Java基础
        JAVA_COLLECTION,  // Java集合
        JAVA_CONCURRENT,  // Java并发
        MYSQL,            // MySQL
        REDIS,            // Redis
        SPRING,           // Spring
        SPRING_BOOT       // Spring Boot
    }
    
    /**
     * 创建新问题（未回答状态）
     */
    public static InterviewQuestionDTO create(int index, String question, QuestionType type, String category) {
        return new InterviewQuestionDTO(index, question, type, category, null, null, null, false, null);
    }

    /**
     * 创建新问题（支持追问标记）
     */
    public static InterviewQuestionDTO create(
            int index,
            String question,
            QuestionType type,
            String category,
            boolean isFollowUp,
            Integer parentQuestionIndex) {
        return new InterviewQuestionDTO(index, question, type, category, null, null, null, isFollowUp, parentQuestionIndex);
    }
    
    /**
     * 添加用户回答
     */
    public InterviewQuestionDTO withAnswer(String answer) {
        return new InterviewQuestionDTO(
            questionIndex, question, type, category, answer, score, feedback, isFollowUp, parentQuestionIndex);
    }
    
    /**
     * 添加评分和反馈
     */
    public InterviewQuestionDTO withEvaluation(int score, String feedback) {
        return new InterviewQuestionDTO(
            questionIndex, question, type, category, userAnswer, score, feedback, isFollowUp, parentQuestionIndex);
    }
}
