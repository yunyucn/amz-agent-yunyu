package interview.guide.modules.knowledgebase.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 知识库查询请求
 */
@Schema(description = "知识库问答请求")
public record QueryRequest(
    @Schema(description = "知识库ID列表", example = "[201,202]")
    @NotEmpty(message = "至少选择一个知识库")
    List<Long> knowledgeBaseIds,  // 支持多个知识库
    
    @Schema(description = "用户问题", example = "什么是数据库索引下推？")
    @NotBlank(message = "问题不能为空")
    String question
) {
    /**
     * 兼容单知识库查询（向后兼容）
     */
    public QueryRequest(Long knowledgeBaseId, String question) {
        this(List.of(knowledgeBaseId), question);
    }
}

