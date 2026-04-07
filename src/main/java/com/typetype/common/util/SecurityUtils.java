package com.typetype.common.util;

import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 *
 * 🎓 学习点：
 * - SecurityContextHolder：Spring Security 上下文持有者
 * - 用于获取当前登录用户的信息
 *
 * 💡 为什么需要工具类？
 * - 避免在每个 Controller/Service 中重复写获取逻辑
 * - 统一异常处理
 *
 * 💡 使用方式：
 * - Long userId = SecurityUtils.getCurrentUserId();
 * - Long userId = SecurityUtils.getCurrentUserIdOrNull();
 *
 * 💡 当前用户信息存储位置：
 * - JwtAuthenticationFilter 将用户 ID 设置到 SecurityContext
 * - 这里从 SecurityContext 读取
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户 ID
     *
     * 🎓 执行流程：
     * 1. 从 SecurityContextHolder 获取 Authentication
     * 2. 检查 Authentication 是否为空
     * 3. 检查 principal 类型是否正确
     * 4. 返回用户 ID
     *
     * @return 用户 ID
     * @throws BusinessException 未登录或认证信息无效时抛出异常
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext()
            .getAuthentication();

        if (auth == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID,
                "未登录或认证信息无效");
        }

        Object principal = auth.getPrincipal();

        if (!(principal instanceof Long)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID,
                "认证信息格式错误");
        }

        return (Long) principal;
    }

    /**
     * 获取当前登录用户 ID（可能为 null）
     *
     * 🎓 使用场景：
     * - 某些接口允许未登录访问
     * - 需要判断用户是否登录时
     *
     * @return 用户 ID，未登录返回 null
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (BusinessException e) {
            return null;
        }
    }

    /**
     * 检查是否已登录
     *
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

    /**
     * 获取当前 Authentication 对象
     *
     * 🎓 使用场景：
     * - 需要获取更详细的认证信息时
     * - 例如：权限、角色等
     *
     * @return Authentication 对象，未登录返回 null
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
