package com.typetype.common.exception;

import com.typetype.common.result.Result;
import com.typetype.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 🎓 学习点：
 * - @RestControllerAdvice：全局异常处理（组合了 @ControllerAdvice + @ResponseBody）
 * - @ExceptionHandler：指定要处理的异常类型
 * - @ResponseStatus：指定 HTTP 状态码
 *
 * 💡 工作流程：
 * 1. Controller 抛出异常
 * 2. GlobalExceptionHandler 捕获
 * 3. 转换为 Result<T>
 * 4. 返回 JSON
 *
 * 💡 为什么用 Lombok 的 @Slf4j？
 * - 自动生成 private static final Logger log = LoggerFactory.getLogger(XXX.class);
 * - 不需要手动创建 Logger
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    /**
     * 处理业务异常（BusinessException）
     *
     * @param e 业务异常
     * @return Result<Void>
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验异常 ====================

    /**
     * 处理 @Valid 校验失败异常（MethodArgumentNotValidException）
     *
     * @param e 参数校验异常
     * @return Result<Void>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理 BindException（GET 请求参数绑定失败）
     *
     * @param e 参数绑定异常
     * @return Result<Void>
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    // ==================== 运行时异常 ====================

    /**
     * 处理其他运行时异常（RuntimeException）
     *
     * @param e 运行时异常
     * @return Result<Void>
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("系统异常: ", e);
        return Result.error(ResultCode.SYSTEM_ERROR, e.getMessage());
    }

    /**
     * 处理所有异常（兜底）
     *
     * @param e 异常
     * @return Result<Void>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("未知异常: ", e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }
}
