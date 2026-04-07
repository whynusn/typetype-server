package com.typetype.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应包装类
 *
 * 🎓 学习点：
 * - 泛型 <T>：包装任意类型的数据
 * - @Data：Lombok 自动生成 getter/setter/toString/equals/hashCode
 * - @Builder: Lombok 生成链式构造器
 * - @AllArgsConstructor: Lombok 自动生成全参构造器
 * - @NoArgsConstructor：Lombok 自动生成无参构造器
 * - @JsonInclude：序列化时忽略 null 值
 *
 * 💡 使用方式：
 * - 成功：Result.success(data)
 * - 失败：Result.error(ResultCode.PARAM_ERROR)
 * - 自定义消息：Result.error(ResultCode.PARAM_ERROR, "用户名不能为空")
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;

    // ==================== 成功响应 ====================

    /**
     * 成功响应（带数据）
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return Result<T>
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
            .code(ResultCode.SUCCESS.getCode())
            .message(ResultCode.SUCCESS.getMessage())
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return Result<T>
     */
    public static <T> Result<T> success() {
        return Result.<T>success(null);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 消息
     * @param <T> 数据类型
     * @return Result<T>
     */
    public static <T> Result<T> success(String message) {
        return Result.<T>builder()
            .code(ResultCode.SUCCESS.getCode())
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 成功响应（自定义消息，带数据）
     *
     * @param message 消息
     * @param data 数据
     * @param <T> 数据类型
     * @return Result<T>
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
            .code(ResultCode.SUCCESS.getCode())
            .message(message)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    // ==================== 失败响应 ====================

    /**
     * 失败响应（使用 ResultCode）
     *
     * @param resultCode 结果码
     * @param <T> 数据类型
     * @return Result<T>
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return Result.<T>builder()
            .code(resultCode.getCode())
            .message(resultCode.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 失败响应（使用 ResultCode + 自定义消息）
     *
     * @param resultCode 结果码
     * @param message 消息
     * @param <T> 数据类型
     * @return Result<T>
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return Result.<T>builder()
            .code(resultCode.getCode())
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 失败响应（直接指定码和消息）
     *
     * @param code 错误码
     * @param message 消息
     * @param <T> 数据类型
     * @return Result<T>
     */
    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
            .code(code)
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
