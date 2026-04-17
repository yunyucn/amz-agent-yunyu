package interview.guide.modules.knowledgebase.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG聊天相关DTO
 */
public class RagChatDTO {

    // ========== 请求 DTO ==========

    /**
     * 创建会话请求
     */
    public record CreateSessionRequest(
        @Schema(description = "关联知识库ID列表", example = "[201,205]")
        @NotEmpty(message = "至少选择一个知识库")
        List<Long> knowledgeBaseIds,

        @Schema(description = "会话标题，可为空自动生成", example = "Redis专项复习")
        String title  // 可选，为空则自动生成
    ) {}

    /**
     * 发送消息请求
     */
    public record SendMessageRequest(
        @Schema(description = "用户输入问题", example = "请解释Redis持久化AOF和RDB的区别")
        @NotBlank(message = "问题不能为空")
        String question
    ) {}

    /**
     * 更新标题请求
     */
    public record UpdateTitleRequest(
        @Schema(description = "新标题", example = "Java并发专项")
        @NotBlank(message = "标题不能为空")
        String title
    ) {}

    /**
     * 更新知识库请求
     */
    public record UpdateKnowledgeBasesRequest(
        @Schema(description = "新的知识库ID列表", example = "[201,202]")
        @NotEmpty(message = "至少选择一个知识库")
        List<Long> knowledgeBaseIds
    ) {}

    // ========== 响应 DTO ==========

    /**
     * 会话基础信息
     */
    public record SessionDTO(
        @Schema(description = "会话ID", example = "301")
        Long id,
        @Schema(description = "会话标题", example = "Redis专项复习")
        String title,
        @Schema(description = "关联知识库ID列表", example = "[201,205]")
        List<Long> knowledgeBaseIds,
        @Schema(description = "创建时间", example = "2026-04-14T10:00:00")
        LocalDateTime createdAt
    ) {}

    /**
     * 会话列表项
     */
    public record SessionListItemDTO(
        @Schema(description = "会话ID", example = "301")
        Long id,
        @Schema(description = "会话标题", example = "Redis专项复习")
        String title,
        @Schema(description = "消息数", example = "12")
        Integer messageCount,
        @Schema(description = "知识库名称列表", example = "[\"Redis题库\",\"缓存设计题库\"]")
        List<String> knowledgeBaseNames,
        @Schema(description = "更新时间", example = "2026-04-14T10:32:00")
        LocalDateTime updatedAt,
        @Schema(description = "是否置顶", example = "true")
        Boolean isPinned
    ) {}

    /**
     * 会话详情（含消息）
     */
    public record SessionDetailDTO(
        @Schema(description = "会话ID", example = "301")
        Long id,
        @Schema(description = "会话标题", example = "Redis专项复习")
        String title,
        @Schema(description = "关联知识库详情")
        List<KnowledgeBaseListItemDTO> knowledgeBases,
        @Schema(description = "消息列表")
        List<MessageDTO> messages,
        @Schema(description = "创建时间", example = "2026-04-14T10:00:00")
        LocalDateTime createdAt,
        @Schema(description = "更新时间", example = "2026-04-14T10:32:00")
        LocalDateTime updatedAt
    ) {}

    /**
     * 消息 DTO
     */
    public record MessageDTO(
        @Schema(description = "消息ID", example = "9001")
        Long id,
        @Schema(description = "消息类型: user/assistant", example = "assistant")
        String type,  // "user" | "assistant"
        @Schema(description = "消息内容", example = "RDB 是快照持久化，AOF 是日志追加...")
        String content,
        @Schema(description = "创建时间", example = "2026-04-14T10:30:00")
        LocalDateTime createdAt
    ) {}
}
