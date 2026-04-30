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
     * 插入成绩记录（V2 纯原始字段）
     *
     * @param score 成绩实体
     */
    @Insert("""
        INSERT INTO t_score (user_id, text_id, char_count,
            wrong_char_count, backspace_count, correction_count,
            key_stroke_count, time)
        VALUES (#{userId}, #{textId}, #{charCount},
            #{wrongCharCount}, #{backspaceCount}, #{correctionCount},
            #{keyStrokeCount}, #{time})
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
        SELECT s.id, s.user_id, s.text_id, s.char_count, s.wrong_char_count,
               s.backspace_count, s.correction_count, s.key_stroke_count,
               s.time, s.created_at, t.title as text_title
        FROM t_score s
        LEFT JOIN t_text t ON s.text_id = t.id
        WHERE s.user_id = #{userId}
        ORDER BY s.created_at DESC
        LIMIT #{offset}, #{limit}
        """)
    @Results(id = "scoreResult", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "textId", column = "text_id"),
        @Result(property = "charCount", column = "char_count"),
        @Result(property = "wrongCharCount", column = "wrong_char_count"),
        @Result(property = "backspaceCount", column = "backspace_count"),
        @Result(property = "correctionCount", column = "correction_count"),
        @Result(property = "keyStrokeCount", column = "key_stroke_count"),
        @Result(property = "time", column = "time"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "textTitle", column = "text_title")
    })
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
            ranked.charCount,
            ranked.wrongCharCount,
            ranked.keyAccuracy,
            ranked.backspaceCount,
            ranked.correctionCount,
            ranked.time,
            ranked.createdAt
        FROM (
            SELECT
                @rank := @rank + 1 AS rank,
                best.user_id AS userId,
                u.username,
                u.nickname,
                u.avatar_url AS avatarUrl,
                -- 派生字段：speed = char_count * 60 / time
                ROUND(s.char_count * 60.0 / s.time, 2) AS speed,
                -- 派生字段：effectiveSpeed = correctChars * 60 / time
                ROUND((s.char_count - s.wrong_char_count) * 60.0 / s.time, 2) AS effectiveSpeed,
                -- 派生字段：keyStroke = key_stroke_count / time
                ROUND(s.key_stroke_count / s.time, 2) AS keyStroke,
                -- 派生字段：codeLength = key_stroke_count / char_count
                ROUND(s.key_stroke_count / s.char_count, 3) AS codeLength,
                s.char_count AS charCount,
                s.wrong_char_count AS wrongCharCount,
                -- 派生字段：keyAccuracy = (key_stroke_count - wrongKeys) / key_stroke_count * 100
                ROUND(
                    (s.key_stroke_count - (s.backspace_count + s.correction_count * (s.key_stroke_count / s.char_count)))
                    / s.key_stroke_count * 100,
                2) AS keyAccuracy,
                s.backspace_count AS backspaceCount,
                s.correction_count AS correctionCount,
                s.time,
                s.created_at AS createdAt
            FROM (
                SELECT user_id, ROUND(MAX(s.char_count * 60.0 / s.time), 2) AS max_speed
                FROM t_score s
                WHERE text_id = #{textId}
                GROUP BY user_id
                ORDER BY max_speed DESC
                LIMIT #{offset}, #{limit}
            ) best
            INNER JOIN t_score s ON s.id = (
                SELECT MAX(s2.id) FROM t_score s2
                WHERE s2.user_id = best.user_id
                    AND s2.text_id = #{textId}
                    AND ROUND(s2.char_count * 60.0 / s2.time, 2) = best.max_speed
            )
            INNER JOIN t_user u ON best.user_id = u.id
            CROSS JOIN (SELECT @rank := #{offset}) r
            ORDER BY ROUND(s.char_count * 60.0 / s.time, 2) DESC
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
        SELECT id, user_id, text_id, char_count, wrong_char_count,
               backspace_count, correction_count, key_stroke_count,
               time, created_at
        FROM t_score
        WHERE user_id = #{userId} AND text_id = #{textId}
        ORDER BY char_count * 60.0 / time DESC
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
        SELECT id, user_id, text_id, char_count, wrong_char_count,
               backspace_count, correction_count, key_stroke_count,
               time, created_at
        FROM t_score
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

    /**
     * 查询用户最近一次提交成绩的时间（用于频率限制）
     *
     * @param userId 用户ID
     * @return 最近提交时间（秒级时间戳），无记录返回 null
     */
    @Select("SELECT UNIX_TIMESTAMP(MAX(created_at)) FROM t_score WHERE user_id = #{userId}")
    Long findLastSubmitTime(Long userId);
}
