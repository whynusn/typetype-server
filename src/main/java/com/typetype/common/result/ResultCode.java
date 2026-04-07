package com.typetype.common.result;

import lombok.Getter;

/**
 * 业务状态码枚举
 *
 * 🎓 学习点：
 * - enum：枚举类型，定义常量集合
 * - 枚举构造器必须私有（可以省略 private）
 * - 枚举常量必须在所有字段/方法之前定义
 *
 * 💡 错误码分类：
 * - 10xxx：系统错误
 * - 20xxx：认证错误
 * - 30xxx：文本业务错误
 * - 40xxx：成绩业务错误
 */
@Getter
public enum ResultCode {

    // ==================== 成功 ====================
    SUCCESS(200, "操作成功"),

    // ==================== 系统错误 (10xxx) ====================
    SYSTEM_ERROR(10001, "系统内部异常"),
    PARAM_ERROR(10002, "参数校验失败"),
    NOT_FOUND(10003, "资源不存在"),

    // ==================== 认证错误 (20xxx) ====================
    TOKEN_EXPIRED(20001, "Token 已过期"),
    TOKEN_INVALID(20002, "Token 无效"),
    PASSWORD_ERROR(20003, "密码错误"),
    USER_NOT_FOUND(20004, "用户名不存在"),
    USER_EXISTS(20005, "用户名已存在"),

    // ==================== 文本业务 (30xxx) ====================
    TEXT_SOURCE_NOT_FOUND(30001, "文本来源不存在"),
    TEXT_NOT_FOUND(30002, "无可用文本"),

    // ==================== 成绩业务 (40xxx) ====================
    SCORE_DATA_INVALID(40001, "成绩数据异常"),
    SCORE_SUBMIT_TOO_FREQUENT(40002, "提交过于频繁");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态消息
     */
    private final String message;

    /**
     * 枚举构造器（必须是私有的）
     *
     * @param code 状态码
     * @param message 状态消息
     */
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
