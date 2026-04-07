package com.typetype.user.converter;

import com.typetype.user.dto.UserVO;
import com.typetype.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * 用户实体与 DTO 转换器
 *
 * 🎓 学习点：
 * - @Component：标记为 Spring 组件，可被注入
 * - Converter 模式：专门负责 Entity ↔ DTO 转换
 *
 * 💡 为什么需要转换器？
 * 1. 安全：过滤敏感字段（如 password）
 * 2. 解耦：数据库结构变更不影响接口定义
 * 3. 灵活：不同场景返回不同字段组合
 *
 * 💡 其他转换方式对比：
 * - BeanUtils.copyProperties（Spring）：速度快，但字段名必须完全匹配
 * - MapStruct：编译时生成转换代码，性能最佳
 * - 手动转换：最灵活，可自定义逻辑
 */
@Component
public class UserConverter {

    /**
     * Entity → VO 转换
     *
     * @param user 用户实体
     * @return 用户 VO（不包含 password）
     */
    public UserVO toVO(User user) {
        if (user == null) {
            return null;
        }

        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
