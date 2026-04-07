package com.typetype.user.mapper;

import com.typetype.user.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户数据访问接口
 *
 * 🎓 学习点：
 * - @Mapper：标记为 MyBatis Mapper，Spring 自动扫描并创建代理实现
 * - @Select/@Insert/@Update/@Delete：SQL 注解，替代 XML 配置
 * - @Options(useGeneratedKeys = true, keyProperty = "id")：插入后回填自增 ID
 * - #{username}：参数占位符，防止 SQL 注入
 *
 * 💡 SQL 注入防护：
 * - ❌ 错误：SELECT * FROM t_user WHERE username = '" + username + "'"
 * - ✅ 正确：SELECT * FROM t_user WHERE username = #{username}
 *
 * 💡 命名映射：
 * - application.yml 中 map-underscore-to-camel-case: true
 * - 数据库 user_name → Java userName
 *
 * 💡 @Options 的正确使用：
 * - 仅在 INSERT 操作中使用，用于回填自增主键
 * - UPDATE/DELETE 操作不需要这个注解
 */
@Mapper
public interface UserMapper {

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户ID
     * @return 用户实体，不存在返回 null
     */
    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User findById(Long id);

    /**
     * 根据用户名查询用户（登录登录）
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    @Select("SELECT * FROM t_user WHERE username = #{username}")
    User findByUsername(String username);

    /**
     * 检查用户名是否存在（注册时校验）
     *
     * @param username 用户名
     * @return 用户名是否已存在
     */
    @Select("SELECT COUNT(*) FROM t_user WHERE username = #{username}")
    boolean existsByUsername(String username);

    /**
     * 插入新用户（注册）
     *
     * @param user 用户实体（包含 username、password 等字段）
     * 🎓 useGeneratedKeys = true：插入后获取自增 ID 并回填到 user.id
     * 🎓 keyProperty = "id"：指定回填的字段名
     */
    @Insert("INSERT INTO t_user (username, password, nickname, avatar_url, role) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{avatarUrl}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    /**
     * 更新用户基本信息
     *
     * @param user 用户实体（必须包含 id）
     */
    @Update("UPDATE t_user SET nickname = #{nickname}, avatar_url = #{avatarUrl} " +
            "WHERE id = #{id}")
    void update(User user);

    /**
     * 更新用户角色
     *
     * @param id 用户ID
     * @param role 用户新角色
     */
    @Update("UPDATE t_user SET role = #{role} WHERE id = #{id}")
    boolean updateRole(Long id, String role);

    /**
     * 更新密码（密码重置功能）
     *
     * @param id 用户ID
     * @param newPassword 加密后的新密码
     *
     * 💡 注意：UPDATE 操作不需要 @Options 注解
     * - UPDATE 不会生成新主键，不需要回填任何值
     */
    @Update("UPDATE t_user SET password = #{password} WHERE id = #{id}")
    void updatePassword(@Param("id") Long id, @Param("password") String newPassword);
}
