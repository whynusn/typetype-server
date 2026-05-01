package com.typetype.user.controller;

import com.typetype.common.result.Result;
import com.typetype.common.util.SecurityUtils;
import com.typetype.user.dto.UserVO;
import com.typetype.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 *
 * 🎓 学习点：
 * - @PathVariable：路径变量绑定（/users/{id} 中的 id）
 * - SecurityUtils：从 SecurityContext 获取当前用户 ID
 * - 区分 AuthController 和 UserController：
 *   - AuthController：登录/注册（认证相关）
 *   - UserController：用户信息查询/更新（资源相关）
 *
 * 💡 接口设计规范：
 * - 资源 ID 放在路径中：/api/v1/users/1
 * - 条件参数放在查询字符串：/api/v1/users?page=1&size=10
 * - 请求体放在 POST/PUT 中
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "用户信息查询")
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户信息
     *
     * 🎓 获取当前用户：
     * - 使用 SecurityUtils.getCurrentUserId()
     * - 从 JWT Token 中解析的用户 ID
     *
     * @return 用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "当前用户信息", description = "获取当前已认证用户的信息")
    public Result<UserVO> getCurrentUser() {
        // 从 JWT Token 中获取 userId
        Long userId = SecurityUtils.getCurrentUserId();

        UserVO userVO = userService.getUserById(userId);
        return Result.success(userVO);
    }

    /**
     * 根据 ID 获取用户信息
     *
     * 🎓 @PathVariable：
     * - 将 URL 中的 {id} 绑定到方法参数
     * - 示例：GET /api/v1/users/123 → id = 123L
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "用户详情", description = "通过 ID 获取用户信息（需要 ADMIN 权限）")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO userVO = userService.getUserById(id);
        return Result.success(userVO);
    }
}
