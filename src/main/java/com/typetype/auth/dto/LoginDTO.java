package com.typetype.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求 DTO
 *
 * 🎓 学习点：
 * - 登录只需要用户名和密码
 * - 密码在 Controller 中接收，Service 层与数据库加密密码比对
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码（明文）
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
