package com.typetype.user.constant;

/**
 * 用户角色枚举
 *
 * 🎓 面试讲法：
 * - 角色统一管理，避免字符串散落
 * - 数据库存 USER/ADMIN，授权时加 ROLE_ 前缀
 */
public enum UserRole {
    USER,
    ADMIN
}
