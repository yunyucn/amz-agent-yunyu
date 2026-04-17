package interview.guide.modules.interview.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建面试会话请求
 */
@Schema(description = "创建面试会话请求")
public record CreateInterviewRequest(
    @Schema(description = "简历文本内容", example = "5年Java开发经验，负责电商交易系统...")
    @NotBlank(message = "简历文本不能为空")
    String resumeText,      // 简历文本内容
    
    @Schema(description = "题目数量，范围3~20", example = "5")
    @Min(value = 3, message = "题目数量最少3题")
    @Max(value = 20, message = "题目数量最多20题")
    int questionCount,      // 面试题目数量 (3-20)
    
    @Schema(description = "简历ID", example = "101")
    @NotNull(message = "简历ID不能为空")
    Long resumeId,          // 简历ID（用于持久化关联）
    
    @Schema(description = "是否强制新建会话，true 时忽略未完成会话", example = "false")
    Boolean forceCreate     // 是否强制创建新会话（忽略未完成的会话），默认为 false
) {}
