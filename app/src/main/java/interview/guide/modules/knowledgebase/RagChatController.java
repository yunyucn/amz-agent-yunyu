package interview.guide.modules.knowledgebase;

import interview.guide.common.result.Result;
import interview.guide.modules.knowledgebase.model.RagChatDTO.*;
import interview.guide.modules.knowledgebase.service.RagChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * RAG 聊天控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "RAG聊天", description = "RAG会话管理与流式消息接口")
public class RagChatController {
    // RAG 会话服务，负责会话管理与流式问答编排
    private final RagChatSessionService sessionService;

    /**
     * 创建新会话
     */
    @PostMapping("/api/rag-chat/sessions")
    @Operation(summary = "创建RAG会话", description = "指定知识库列表并创建聊天会话，可选设置会话标题。")
    public Result<SessionDTO> createSession(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "创建会话请求体",
                content = @Content(
                    schema = @Schema(implementation = CreateSessionRequest.class),
                    examples = @ExampleObject(value = "{\"knowledgeBaseIds\":[201,202],\"title\":\"Redis复习\"}")
                )
            )
            @Valid @RequestBody CreateSessionRequest request) {
        return Result.success(sessionService.createSession(request));
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/api/rag-chat/sessions")
    @Operation(summary = "查询会话列表", description = "返回会话概要信息列表，包含置顶状态和消息数。")
    public Result<List<SessionListItemDTO>> listSessions() {
        return Result.success(sessionService.listSessions());
    }

    /**
     * 获取会话详情（包含消息历史）
     * GET /api/rag-chat/sessions/{sessionId}
     */
    @GetMapping("/api/rag-chat/sessions/{sessionId}")
    @Operation(summary = "查询会话详情", description = "根据会话ID查询详情与历史消息。")
    public Result<SessionDetailDTO> getSessionDetail(
            @Parameter(description = "会话ID", required = true, example = "301")
            @PathVariable Long sessionId) {
        return Result.success(sessionService.getSessionDetail(sessionId));
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/api/rag-chat/sessions/{sessionId}/title")
    @Operation(summary = "更新会话标题", description = "修改指定会话的标题。")
    public Result<Void> updateSessionTitle(
            @Parameter(description = "会话ID", required = true, example = "301")
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\"title\":\"Java并发复习\"}"))
            )
            @Valid @RequestBody UpdateTitleRequest request) {
        sessionService.updateSessionTitle(sessionId, request.title());
        return Result.success(null);
    }

    /**
     * 切换会话置顶状态
     * PUT /api/rag-chat/sessions/{sessionId}/pin
     */
    @PutMapping("/api/rag-chat/sessions/{sessionId}/pin")
    @Operation(summary = "切换会话置顶", description = "将会话在置顶/非置顶状态之间切换。")
    public Result<Void> togglePin(
            @Parameter(description = "会话ID", required = true, example = "301")
            @PathVariable Long sessionId) {
        sessionService.togglePin(sessionId);
        return Result.success(null);
    }

    /**
     * 更新会话知识库
     */
    @PutMapping("/api/rag-chat/sessions/{sessionId}/knowledge-bases")
    @Operation(summary = "更新会话知识库范围", description = "修改当前会话关联的知识库ID列表。")
    public Result<Void> updateSessionKnowledgeBases(
            @Parameter(description = "会话ID", required = true, example = "301")
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\"knowledgeBaseIds\":[201,205]}"))
            )
            @Valid @RequestBody UpdateKnowledgeBasesRequest request) {
        sessionService.updateSessionKnowledgeBases(sessionId, request.knowledgeBaseIds());
        return Result.success(null);
    }

    /**
     * 删除会话
     * DELETE /api/rag-chat/sessions/{sessionId}
     */
    @DeleteMapping("/api/rag-chat/sessions/{sessionId}")
    @Operation(summary = "删除会话", description = "删除会话及其历史消息。")
    public Result<Void> deleteSession(
            @Parameter(description = "会话ID", required = true, example = "301")
            @PathVariable Long sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.success(null);
    }

    /**
     * 发送消息（流式SSE）
     * 流式响应设计：
     * 1. 先同步保存用户消息和创建 AI 消息占位
     * 2. 返回流式响应
     * 3. 流式完成后通过回调更新消息
     */
    @PostMapping(value = "/api/rag-chat/sessions/{sessionId}/messages/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息并流式接收回复", description = "通过 SSE 连续返回 AI 回复分片，最终落库完整消息。")
    public Flux<ServerSentEvent<String>> sendMessageStream(
            @Parameter(description = "会话ID", required = true, example = "301")
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\"question\":\"解释一下Redis跳表\"}"))
            )
            @Valid @RequestBody SendMessageRequest request) {

        log.info("收到 RAG 聊天流式请求: sessionId={}, question={}, 线程: {} (虚拟线程: {})",
            sessionId, request.question(), Thread.currentThread(), Thread.currentThread().isVirtual());

        // 1. 准备消息（保存用户消息，创建 AI 消息占位）
        Long messageId = sessionService.prepareStreamMessage(sessionId, request.question());

        // 2. 获取流式响应
        StringBuilder fullContent = new StringBuilder();

        return sessionService.getStreamAnswer(sessionId, request.question())
            .doOnNext(fullContent::append)
            // 使用 ServerSentEvent 包装，转义换行符避免破坏 SSE 格式
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk.replace("\n", "\\n").replace("\r", "\\r"))
                .build())
            .doOnComplete(() -> {
                // 3. 流式完成后更新消息内容
                sessionService.completeStreamMessage(messageId, fullContent.toString());
                log.info("RAG 聊天流式完成: sessionId={}, messageId={}", sessionId, messageId);
            })
            .doOnError(e -> {
                // 错误时也保存已接收的内容
                String content = !fullContent.isEmpty()
                    ? fullContent.toString()
                    : "【错误】回答生成失败：" + e.getMessage();
                sessionService.completeStreamMessage(messageId, content);
                log.error("RAG 聊天流式错误: sessionId={}", sessionId, e);
            });
    }
}
