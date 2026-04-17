package interview.guide.modules.interview;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.interview.model.*;
import interview.guide.modules.interview.service.InterviewHistoryService;
import interview.guide.modules.interview.service.InterviewPersistenceService;
import interview.guide.modules.interview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 面试控制器
 * 提供模拟面试相关的API接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "模拟面试", description = "模拟面试会话管理、答题、报告与导出接口")
public class InterviewController {
    // 会话服务（创建会话、答题流转、报告生成）
    private final InterviewSessionService sessionService;
    // 历史服务（详情查询与 PDF 导出）
    private final InterviewHistoryService historyService;
    // 持久化服务（会话删除等管理操作）
    private final InterviewPersistenceService persistenceService;
    
    /**
     * 创建面试会话
     */
    @PostMapping("/api/interview/sessions")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    @Operation(summary = "创建面试会话", description = "根据简历内容生成面试问题并创建会话。")
    public Result<InterviewSessionDTO> createSession(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(examples = @ExampleObject(
                    value = "{\"resumeText\":\"5年Java开发经验...\",\"questionCount\":5,\"resumeId\":101,\"forceCreate\":false}"
                ))
            )
            @RequestBody CreateInterviewRequest request) {
        log.info("创建面试会话，题目数量: {}", request.questionCount());
        InterviewSessionDTO session = sessionService.createSession(request);
        return Result.success(session);
    }
    
    /**
     * 获取会话信息
     */
    @GetMapping("/api/interview/sessions/{sessionId}")
    @Operation(summary = "查询会话信息", description = "按会话ID获取当前会话完整状态。")
    public Result<InterviewSessionDTO> getSession(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId) {
        InterviewSessionDTO session = sessionService.getSession(sessionId);
        return Result.success(session);
    }
    
    /**
     * 获取当前问题
     */
    @GetMapping("/api/interview/sessions/{sessionId}/question")
    @Operation(summary = "获取当前题目", description = "返回当前题目索引和题干等信息。")
    public Result<Map<String, Object>> getCurrentQuestion(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId) {
        return Result.success(sessionService.getCurrentQuestionResponse(sessionId));
    }
    
    /**
     * 提交答案
     */
    @PostMapping("/api/interview/sessions/{sessionId}/answers")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL}, count = 10)
    @Operation(summary = "提交答案并进入下一题", description = "提交当前题答案，返回下一题或结束状态。")
    public Result<SubmitAnswerResponse> submitAnswer(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\"questionIndex\":0,\"answer\":\"我会先做索引优化...\"}"))
            )
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("提交答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        SubmitAnswerResponse response = sessionService.submitAnswer(request);
        return Result.success(response);
    }
    
    /**
     * 生成面试报告
     */
    @GetMapping("/api/interview/sessions/{sessionId}/report")
    @Operation(summary = "生成/获取面试报告", description = "为指定会话生成并返回结构化评估报告。")
    public Result<InterviewReportDTO> getReport(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId) {
        log.info("生成面试报告: {}", sessionId);
        InterviewReportDTO report = sessionService.generateReport(sessionId);
        return Result.success(report);
    }
    
    /**
     * 查找未完成的面试会话
     * GET /api/interview/sessions/unfinished/{resumeId}
     */
    @GetMapping("/api/interview/sessions/unfinished/{resumeId}")
    @Operation(summary = "查询未完成会话", description = "根据简历ID查询最近一个未完成的面试会话。")
    public Result<InterviewSessionDTO> findUnfinishedSession(
            @Parameter(description = "简历ID", required = true, example = "101")
            @PathVariable Long resumeId) {
        return Result.success(sessionService.findUnfinishedSessionOrThrow(resumeId));
    }
    
    /**
     * 暂存答案（不进入下一题）
     */
    @PutMapping("/api/interview/sessions/{sessionId}/answers")
    @Operation(summary = "暂存答案", description = "保存当前答案草稿，不推进题目进度。")
    public Result<Void> saveAnswer(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(examples = @ExampleObject(value = "{\"questionIndex\":0,\"answer\":\"草稿答案\"}"))
            )
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("暂存答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        sessionService.saveAnswer(request);
        return Result.success(null);
    }
    
    /**
     * 提前交卷
     */
    @PostMapping("/api/interview/sessions/{sessionId}/complete")
    @Operation(summary = "提前交卷", description = "手动结束当前面试会话并标记为完成。")
    public Result<Void> completeInterview(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId) {
        log.info("提前交卷: {}", sessionId);
        sessionService.completeInterview(sessionId);
        return Result.success(null);
    }
    
    /**
     * 获取面试会话详情
     * GET /api/interview/sessions/{sessionId}/details
     */
    @GetMapping("/api/interview/sessions/{sessionId}/details")
    @Operation(summary = "查询面试详情", description = "获取面试会话详情、作答明细和评估结果。")
    public Result<InterviewDetailDTO> getInterviewDetail(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId) {
        InterviewDetailDTO detail = historyService.getInterviewDetail(sessionId);
        return Result.success(detail);
    }
    
    /**
     * 导出面试报告为PDF
     */
    @GetMapping("/api/interview/sessions/{sessionId}/export")
    @Operation(summary = "导出面试PDF报告", description = "导出指定会话的 PDF 报告文件。")
    public ResponseEntity<byte[]> exportInterviewPdf(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId) {
        try {
            byte[] pdfBytes = historyService.exportInterviewPdf(sessionId);
            String filename = URLEncoder.encode("模拟面试报告_" + sessionId + ".pdf", 
                StandardCharsets.UTF_8);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除面试会话
     */
    @DeleteMapping("/api/interview/sessions/{sessionId}")
    @Operation(summary = "删除面试会话", description = "删除会话及关联答题数据。")
    public Result<Void> deleteInterview(
            @Parameter(description = "会话ID", required = true, example = "sess_20260414_001")
            @PathVariable String sessionId) {
        log.info("删除面试会话: {}", sessionId);
        persistenceService.deleteSessionBySessionId(sessionId);
        return Result.success(null);
    }
}
