package com.typetype.score.mapper;

import com.typetype.score.dto.LeaderboardVO;
import com.typetype.score.entity.Score;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 成绩数据访问接口
 *
 * 🎓 学习点：
 * - 排行榜查询使用子查询获取每用户最佳成绩
 * - 分页查询使用 LIMIT offset, limit
 * - 复杂查询使用 @Select 注解的多行字符串
 */
@Mapper
public interface ScoreMapper {

    /**
     * 插入成绩记录
     *
     * @param score 成绩实体
     */
    @Insert("""
        INSERT INTO t_score (user_id, text_id, speed, effective_speed,
            key_stroke, code_length, accuracy_rate, char_count,
            wrong_char_count, duration)
        VALUES (#{userId}, #{textId}, #{speed}, #{effectiveSpeed},
            #{keyStroke}, #{codeLength}, #{accuracyRate}, #{charCount},
            #{wrongCharCount}, #{duration})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Score score);

    /**
     * 查询用户历史成绩
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 限制条数
     * @return 成绩列表
     */
    @Select("""
        SELECT s.*, t.title as text_title
        FROM t_score s
        LEFT JOIN t_text t ON s.text_id = t.id
        WHERE s.user_id = #{userId}
        ORDER BY s.created_at DESC
        LIMIT #{offset}, #{limit}
        """)
    List<Score> findByUserId(@Param("userId") Long userId,
                             @Param("offset") long offset,
                             @Param("limit") long limit);

    /**
     * 统计用户成绩总数
     *
     * @param userId 用户ID
     * @return 成绩总数
     */
    @Select("SELECT COUNT(*) FROM t_score WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    /**
     * 查询文本排行榜
     *
     * 💡 查询逻辑：
     * 1. 子查询找出每用户在该文本的最佳成绩（MAX speed）
     * 2. 关联用户表获取用户信息
     * 3. 使用变量计算排名（避免应用层计算）
     *
     * @param textId 文本ID
     * @param offset 偏移量
     * @param limit 限制条数
     * @return 排行榜列表
     */
    @Select("""
        SELECT
            ranked.rank,
            ranked.userId,
            ranked.username,
            ranked.nickname,
            ranked.avatarUrl,
            ranked.speed,
            ranked.effectiveSpeed,
            ranked.keyStroke,
            ranked.codeLength,
            ranked.accuracyRate,
            ranked.charCount,
            ranked.wrongCharCount,
            ranked.duration,
            ranked.createdAt
        FROM (
            SELECT
                @rank := @rank + 1 AS rank,
                best.user_id AS userId,
                u.username,
                u.nickname,
                u.avatar_url AS avatarUrl,
                s.speed,
                s.effective_speed AS effectiveSpeed,
                s.key_stroke AS keyStroke,
                s.code_length AS codeLength,
                s.accuracy_rate AS accuracyRate,
                s.char_count AS charCount,
                s.wrong_char_count AS wrongCharCount,
                s.duration,
                s.created_at AS createdAt
            FROM (
                SELECT user_id, MAX(speed) AS max_speed
                FROM t_score
                WHERE text_id = #{textId}
                GROUP BY user_id
                ORDER BY max_speed DESC
                LIMIT #{offset}, #{limit}
            ) best
            INNER JOIN t_score s ON s.user_id = best.user_id
                AND s.text_id = #{textId}
                AND s.speed = best.max_speed
            INNER JOIN t_user u ON best.user_id = u.id
            CROSS JOIN (SELECT @rank := #{offset}) r
            ORDER BY s.speed DESC
        ) ranked
        """)
    List<LeaderboardVO> findLeaderboardByTextId(@Param("textId") Long textId,
                                                @Param("offset") long offset,
                                                @Param("limit") long limit);

    /**
     * 统计排行榜人数（有成绩的用户数）
     *
     * @param textId 文本ID
     * @return 用户数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM t_score WHERE text_id = #{textId}")
    long countLeaderboardByTextId(Long textId);

    /**
     * 查询用户在指定文本的最佳成绩
     *
     * @param userId 用户ID
     * @param textId 文本ID
     * @return 最佳成绩，不存在返回 null
     */
    @Select("""
        SELECT * FROM t_score
        WHERE user_id = #{userId} AND text_id = #{textId}
        ORDER BY speed DESC
        LIMIT 1
        """)
    Score findBestScore(@Param("userId") Long userId, @Param("textId") Long textId);

    /**
     * 查询用户在指定文本的成绩列表
     *
     * @param userId 用户ID
     * @param textId 文本ID
     * @param offset 偏移量
     * @param limit 限制条数
     * @return 成绩列表
     */
    @Select("""
        SELECT * FROM t_score
        WHERE user_id = #{userId} AND text_id = #{textId}
        ORDER BY created_at DESC
        LIMIT #{offset}, #{limit}
        """)
    List<Score> findByUserIdAndTextId(@Param("userId") Long userId,
                                      @Param("textId") Long textId,
                                      @Param("offset") long offset,
                                      @Param("limit") long limit);

    /**
     * 统计用户在指定文本的成绩数
     *
     * @param userId 用户ID
     * @param textId 文本ID
     * @return 成绩数
     */
    @Select("SELECT COUNT(*) FROM t_score WHERE user_id = #{userId} AND text_id = #{textId}")
    long countByUserIdAndTextId(@Param("userId") Long userId, @Param("textId") Long textId);
}
