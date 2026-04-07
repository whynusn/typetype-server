package com.typetype.common.exception;

import com.typetype.common.result.ResultCode;
import lombok.Getter;

/**
 * 自定义业务异常
 *
 * 🎓 学习点：
 * - 继承 RuntimeException：运行时异常，不需要 try-catch 声明
 * - 携带 ResultCode：统一错误码管理
 *
 * 💡 使用方式：
 * - throw new BusinessException(ResultCode.USER_NOT_FOUND)
 * - throw new BusinessException(ResultCode.PARAM_ERROR, "用户名不能为空")
 *
 * 💡 为什么继承 RuntimeException？
 * - 受检异常（checked exception）需要声明 throws，代码冗余
 * - 运行时异常（unchecked exception）无需声明，代码简洁
 * - 业务异常通常是"预期内的"，应该让调用方自己决定是否处理
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造器（使用 ResultCode）
     *
     * @param resultCode 结果码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造器（使用 ResultCode + 自定义消息）
     *
     * @param resultCode 结果码
     * @param message 错误消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    /**
     * 构造器（直接指定码和消息）
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
