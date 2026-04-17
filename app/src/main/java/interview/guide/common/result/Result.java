package interview.guide.common.result;

import interview.guide.common.constant.CommonConstants;
import interview.guide.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 统一响应结果
 */
@Getter
@Schema(description = "统一响应体")
public class Result<T> {
    
    @Schema(description = "业务状态码，200表示成功", example = "200")
    private final Integer code;
    @Schema(description = "响应消息", example = "success")
    private final String message;
    @Schema(description = "响应数据，不同接口类型不同")
    private final T data;
    
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // ========== 成功响应 ==========
    
    public static <T> Result<T> success() {
        return new Result<>(CommonConstants.StatusCode.SUCCESS, "success", null);
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(CommonConstants.StatusCode.SUCCESS, "success", data);
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(CommonConstants.StatusCode.SUCCESS, message, data);
    }
    
    // ========== 失败响应 ==========
    
    public static <T> Result<T> error(String message) {
        return new Result<>(CommonConstants.StatusCode.SERVER_ERROR, message, null);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
    
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
    
    // ========== 辅助方法 ==========
    
    public boolean isSuccess() {
        return CommonConstants.StatusCode.SUCCESS == this.code;
    }
}
