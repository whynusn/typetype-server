package com.typetype.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求 DTO
 *
 * 🎓 学习点：
 * - jakarta.validation.constraints.*：参数校验注解
 * - Controller 中使用 @Valid 时，框架自动校验这些约束
 *
 * 💡 常用校验注解：
 * - @NotBlank：不能为 null 或空字符串
 * - @Size：字符串长度范围
 * - @Pattern：正则表达式匹配
 * - @Email：邮箱格式
 *
 * 💡 校验失败时：
 * - GlobalExceptionHandler 捕获 MethodArgumentNotValidException
 * - 返回统一的错误格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {

    /**
     * 用户名
     *
     * 🎓 校验规则：
     * - 不能为空
     * - 长度 3-20 字符
     * - 只允许字母、数字、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /**
     * 密码（明文，Service 层会加密）
     *
     * 🎓 校验规则：
     * - 不能为空
     * - 长度 6-30 字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度必须在 6-30 之间")
    private String password;

    /**
     * 确认密码（前端校验用，后端可选）
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 昵称（可选）
     */
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;
}
