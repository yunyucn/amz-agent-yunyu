package interview.guide.modules.resume;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.resume.model.ResumeDetailDTO;
import interview.guide.modules.resume.model.ResumeListItemDTO;
import interview.guide.modules.resume.service.ResumeDeleteService;
import interview.guide.modules.resume.service.ResumeHistoryService;
import interview.guide.modules.resume.service.ResumeUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 简历控制器
 * Resume Controller for upload and analysis
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "简历管理", description = "简历上传、查询、导出与重分析接口")
public class ResumeController {
    // 上传服务（简历上传、异步分析、重分析）
    private final ResumeUploadService uploadService;
    // 删除服务（清理简历与关联数据）
    private final ResumeDeleteService deleteService;
    // 历史服务（列表、详情与导出）
    private final ResumeHistoryService historyService;

    /**
     * 上传简历并获取分析结果
     *
     * @param file 简历文件（支持PDF、DOCX、DOC、TXT、MD等）
     * @return 简历分析结果，data 中包含 duplicate、resumeId、overallScore、suggestions 等字段
     */
    @PostMapping(value = "/api/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    @Operation(
        summary = "上传简历并分析",
        description = "上传单个简历文件并触发解析与评估。若检测到重复简历，将返回历史分析结果。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传并分析成功",
            content = @Content(schema = @Schema(implementation = Result.class),
                examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":{\"duplicate\":false,\"resumeId\":101,\"overallScore\":85}}"))),
        @ApiResponse(responseCode = "400", description = "文件格式不支持或文件为空"),
        @ApiResponse(responseCode = "429", description = "触发限流")
    })
    public Result<Map<String, Object>> uploadAndAnalyze(
        @Parameter(description = "简历文件", required = true, example = "zhangsan_resume.pdf")
        @RequestParam("file") MultipartFile file) {
        Map<String, Object> result = uploadService.uploadAndAnalyze(file);
        boolean isDuplicate = (Boolean) result.get("duplicate");
        if (isDuplicate) {
            return Result.success("检测到相同简历，已返回历史分析结果", result);
        }
        return Result.success(result);
    }

    /**
     * 获取所有简历列表
     *
     * @return 简历列表，按最近上传/访问时间排序
     */
    @GetMapping("/api/resumes")
    @Operation(summary = "查询简历列表", description = "获取系统中所有简历的概要信息。")
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResumeListItemDTO.class))))
    public Result<List<ResumeListItemDTO>> getAllResumes() {
        List<ResumeListItemDTO> resumes = historyService.getAllResumes();
        return Result.success(resumes);
    }

    /**
     * 获取简历详情（包含分析历史）
     *
     * @param id 简历ID，示例值：101
     * @return 简历详情与分析历史
     */
    @GetMapping("/api/resumes/{id}/detail")
    @Operation(summary = "查询简历详情", description = "根据简历ID获取完整详情，包含分析记录和面试关联信息。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = ResumeDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "简历不存在")
    })
    public Result<ResumeDetailDTO> getResumeDetail(
        @Parameter(description = "简历ID", required = true, example = "101")
        @PathVariable Long id) {
        ResumeDetailDTO detail = historyService.getResumeDetail(id);
        return Result.success(detail);
    }

    /**
     * 导出简历分析报告为PDF
     *
     * @param id 简历ID，示例值：101
     * @return 二进制 PDF 文件流
     */
    @GetMapping("/api/resumes/{id}/export")
    @Operation(summary = "导出简历分析PDF", description = "导出指定简历最近一次分析结果的 PDF 报告。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "导出成功，返回 application/pdf"),
        @ApiResponse(responseCode = "500", description = "导出失败")
    })
    public ResponseEntity<byte[]> exportAnalysisPdf(
        @Parameter(description = "简历ID", required = true, example = "101")
        @PathVariable Long id) {
        try {
            var result = historyService.exportAnalysisPdf(id);
            String filename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.pdfBytes());
        } catch (Exception e) {
            log.error("导出PDF失败: resumeId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除简历
     *
     * @param id 简历ID
     * @return 删除结果
     */
    @DeleteMapping("/api/resumes/{id}")
    @Operation(summary = "删除简历", description = "删除简历文件、文本与关联分析记录。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功",
            content = @Content(examples = @ExampleObject(value = "{\"code\":200,\"message\":\"success\",\"data\":null}"))),
        @ApiResponse(responseCode = "404", description = "简历不存在")
    })
    public Result<Void> deleteResume(
        @Parameter(description = "简历ID", required = true, example = "101")
        @PathVariable Long id) {
        deleteService.deleteResume(id);
        return Result.success(null);
    }

    /**
     * 重新分析简历（手动重试）
     * 用于分析失败后的重试
     *
     * @param id 简历ID
     * @return 结果
     */
    @PostMapping("/api/resumes/{id}/reanalyze")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    @Operation(summary = "重新分析简历", description = "手动触发指定简历重新分析，通常用于失败重试。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重分析任务触发成功"),
        @ApiResponse(responseCode = "429", description = "触发限流")
    })
    public Result<Void> reanalyze(
        @Parameter(description = "简历ID", required = true, example = "101")
        @PathVariable Long id) {
        uploadService.reanalyze(id);
        return Result.success(null);
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态，示例：{\"status\":\"UP\",\"service\":\"AI Interview Platform - Resume Service\"}
     */
    @GetMapping("/api/resumes/health")
    @Operation(summary = "简历模块健康检查", description = "返回简历服务健康状态，用于监控与探活。")
    @ApiResponse(responseCode = "200", description = "健康检查通过")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
            "status", "UP",
            "service", "AI Interview Platform - Resume Service"
        ));
    }

}
