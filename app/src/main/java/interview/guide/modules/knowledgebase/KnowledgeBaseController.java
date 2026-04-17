package interview.guide.modules.knowledgebase;

import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.modules.knowledgebase.model.KnowledgeBaseListItemDTO;
import interview.guide.modules.knowledgebase.model.KnowledgeBaseStatsDTO;
import interview.guide.modules.knowledgebase.model.QueryRequest;
import interview.guide.modules.knowledgebase.model.QueryResponse;
import interview.guide.modules.knowledgebase.model.VectorStatus;
import interview.guide.modules.knowledgebase.service.KnowledgeBaseDeleteService;
import interview.guide.modules.knowledgebase.service.KnowledgeBaseListService;
import interview.guide.modules.knowledgebase.service.KnowledgeBaseQueryService;
import interview.guide.modules.knowledgebase.service.KnowledgeBaseUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 知识库控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库上传、检索、分类、统计与向量化管理接口")
public class KnowledgeBaseController {
    // 上传服务（文件校验、存储与向量化任务投递）
    private final KnowledgeBaseUploadService uploadService;
    // 查询服务（RAG 检索与问答）
    private final KnowledgeBaseQueryService queryService;
    // 列表服务（知识库列表、统计、下载、分类）
    private final KnowledgeBaseListService listService;
    // 删除服务（删除元数据、文件和向量）
    private final KnowledgeBaseDeleteService deleteService;

    /**
     * 获取所有知识库列表
     */
    @GetMapping("/api/knowledgebase/list")
    @Operation(summary = "查询知识库列表", description = "按可选排序字段和向量化状态过滤知识库列表。")
    @ApiResponse(responseCode = "200", description = "查询成功",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = KnowledgeBaseListItemDTO.class))))
    public Result<List<KnowledgeBaseListItemDTO>> getAllKnowledgeBases(
            @Parameter(description = "排序字段，示例：uploadedAt/lastAccessedAt/accessCount", example = "uploadedAt")
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @Parameter(description = "向量化状态，示例：PENDING/PROCESSING/COMPLETED/FAILED", example = "COMPLETED")
            @RequestParam(value = "vectorStatus", required = false) String vectorStatus) {
        
        VectorStatus status = null;
        if (vectorStatus != null && !vectorStatus.isBlank()) {
            try {
                status = VectorStatus.valueOf(vectorStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.error("无效的向量化状态: " + vectorStatus);
            }
        }
        
        return Result.success(listService.listKnowledgeBases(status, sortBy));
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/api/knowledgebase/{id}")
    @Operation(summary = "查询知识库详情", description = "根据知识库ID查询单条知识库详情信息。")
    public Result<KnowledgeBaseListItemDTO> getKnowledgeBase(
            @Parameter(description = "知识库ID", required = true, example = "201")
            @PathVariable Long id) {
        return listService.getKnowledgeBase(id)
                .map(Result::success)
                .orElse(Result.error("知识库不存在"));
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/api/knowledgebase/{id}")
    @Operation(summary = "删除知识库", description = "删除知识库元数据、文件和向量数据。")
    public Result<Void> deleteKnowledgeBase(
            @Parameter(description = "知识库ID", required = true, example = "201")
            @PathVariable Long id) {
        deleteService.deleteKnowledgeBase(id);
        return Result.success(null);
    }

    /**
     * 基于知识库回答问题（支持多知识库）
     */
    @PostMapping("/api/knowledgebase/query")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 10)
    @Operation(summary = "知识库问答", description = "输入问题和知识库ID列表，返回聚合后的答案。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "回答成功",
            content = @Content(schema = @Schema(implementation = QueryResponse.class))),
        @ApiResponse(responseCode = "400", description = "请求参数不合法"),
        @ApiResponse(responseCode = "429", description = "触发限流")
    })
    public Result<QueryResponse> queryKnowledgeBase(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "问答请求体",
                content = @Content(
                    schema = @Schema(implementation = QueryRequest.class),
                    examples = @ExampleObject(value = "{\"knowledgeBaseIds\":[201,202],\"question\":\"什么是索引下推？\"}")
                )
            )
            @Valid @RequestBody QueryRequest request) {
        return Result.success(queryService.queryKnowledgeBase(request));
    }

    /**
     * 基于知识库回答问题（流式SSE，支持多知识库）
     */
    @PostMapping(value = "/api/knowledgebase/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    @Operation(summary = "知识库问答流式返回", description = "使用 SSE 流式返回生成文本，适用于打字机效果。")
    public Flux<String> queryKnowledgeBaseStream(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "流式问答请求体",
                content = @Content(schema = @Schema(implementation = QueryRequest.class))
            )
            @Valid @RequestBody QueryRequest request) {
        log.debug("收到知识库流式查询请求: kbIds={}, question={}, 线程: {} (虚拟线程: {})",
            request.knowledgeBaseIds(), request.question(), Thread.currentThread(), Thread.currentThread().isVirtual());
        return queryService.answerQuestionStream(request.knowledgeBaseIds(), request.question());
    }

    // ========== 分类管理 API ==========

    /**
     * 获取所有分类
     */
    @GetMapping("/api/knowledgebase/categories")
    @Operation(summary = "查询所有分类", description = "返回系统中已存在的知识库分类列表。")
    public Result<List<String>> getAllCategories() {
        return Result.success(listService.getAllCategories());
    }

    /**
     * 根据分类获取知识库列表
     */
    @GetMapping("/api/knowledgebase/category/{category}")
    @Operation(summary = "按分类查询知识库", description = "根据分类名筛选知识库。")
    public Result<List<KnowledgeBaseListItemDTO>> getByCategory(
            @Parameter(description = "分类名称", required = true, example = "Java后端")
            @PathVariable String category) {
        return Result.success(listService.listByCategory(category));
    }

    /**
     * 获取未分类的知识库
     */
    @GetMapping("/api/knowledgebase/uncategorized")
    @Operation(summary = "查询未分类知识库", description = "返回 category 为空的知识库列表。")
    public Result<List<KnowledgeBaseListItemDTO>> getUncategorized() {
        return Result.success(listService.listByCategory(null));
    }

    /**
     * 更新知识库分类
     */
    @PutMapping("/api/knowledgebase/{id}/category")
    @Operation(summary = "更新知识库分类", description = "更新指定知识库的分类字段。")
    public Result<Void> updateCategory(
            @Parameter(description = "知识库ID", required = true, example = "201")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "分类更新请求体",
                content = @Content(examples = @ExampleObject(value = "{\"category\":\"系统设计\"}"))
            )
            @RequestBody Map<String, String> body) {
        listService.updateCategory(id, body.get("category"));
        return Result.success(null);
    }

    // ========== 上传下载 API ==========

    /**
     * 上传知识库文件
     */
    @PostMapping(value = "/api/knowledgebase/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 3)
    @Operation(summary = "上传知识库文件", description = "上传知识库文档并触发解析与向量化。")
    public Result<Map<String, Object>> uploadKnowledgeBase(
            @Parameter(description = "上传文件", required = true, example = "redis-guide.pdf")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "知识库名称，默认使用文件名", example = "Redis实战")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "分类名称，可为空", example = "中间件")
            @RequestParam(value = "category", required = false) String category) {
        return Result.success(uploadService.uploadKnowledgeBase(file, name, category));
    }

    /**
     * 下载知识库文件
     */
    @GetMapping("/api/knowledgebase/{id}/download")
    @Operation(summary = "下载知识库文件", description = "根据知识库ID下载原始上传文件。")
    public ResponseEntity<byte[]> downloadKnowledgeBase(
            @Parameter(description = "知识库ID", required = true, example = "201")
            @PathVariable Long id) {
        var entity = listService.getEntityForDownload(id);
        byte[] fileContent = listService.downloadFile(id);

        String filename = entity.getOriginalFilename();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_TYPE,
                        entity.getContentType() != null ? entity.getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(fileContent);
    }

    // ========== 搜索 API ==========

    /**
     * 搜索知识库
     */
    @GetMapping("/api/knowledgebase/search")
    @Operation(summary = "关键字搜索知识库", description = "按知识库名称、分类、文件名进行模糊搜索。")
    public Result<List<KnowledgeBaseListItemDTO>> search(
            @Parameter(description = "搜索关键字", required = true, example = "Java")
            @RequestParam("keyword") String keyword) {
        return Result.success(listService.search(keyword));
    }

    // ========== 统计 API ==========

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/api/knowledgebase/stats")
    @Operation(summary = "查询知识库统计", description = "返回总量、访问量、提问量和向量化状态统计。")
    public Result<KnowledgeBaseStatsDTO> getStatistics() {
        return Result.success(listService.getStatistics());
    }

    // ========== 向量化管理 API ==========

    /**
     * 重新向量化知识库（手动重试）
     * 用于向量化失败后的重试
     */
    @PostMapping("/api/knowledgebase/{id}/revectorize")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 2)
    @Operation(summary = "重试向量化", description = "手动重试指定知识库的向量化任务。")
    public Result<Void> revectorize(
            @Parameter(description = "知识库ID", required = true, example = "201")
            @PathVariable Long id) {
        uploadService.revectorize(id);
        return Result.success(null);
    }

}
