package com.typetype.score.service;

import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.PageResult;
import com.typetype.common.result.ResultCode;
import com.typetype.common.util.SecurityUtils;
import com.typetype.score.dto.LeaderboardVO;
import com.typetype.score.dto.ScoreVO;
import com.typetype.score.dto.SubmitScoreDTO;
import com.typetype.score.entity.Score;
import com.typetype.score.mapper.ScoreMapper;
import com.typetype.text.entity.Text;
import com.typetype.text.mapper.TextMapper;
import com.typetype.text.service.TextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 成绩服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    /** 成绩提交最短间隔（秒），防止刷榜 */
    private static final long MIN_SUBMIT_INTERVAL_SECONDS = 5;

    /** 排行榜 Redis Key 前缀 */
    private static final String LEADERBOARD_KEY_PREFIX = "leaderboard:text:";

    /** SSE 连接超时（毫秒）：30 分钟 */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final ScoreMapper scoreMapper;
    private final TextMapper textMapper;
    private final TextService textService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * SSE 连接池：textId → 该文本的所有监听者
     *
     * 🎓 面试考点：SSE vs WebSocket
     * - SSE：服务器单向推送，基于 HTTP，简单可靠，支持自动重连
     * - WebSocket：双向通信，需要单独协议，适合聊天/游戏
     * - 排行榜只需要服务器推 → SSE 更合适
     *
     * 💡 为什么用 ConcurrentHashMap？
     * - 多线程安全（一个用户提交成绩，多个监听者收到通知）
     * - key 是 textId，value 是该文本的所有 SseEmitter
     */
    private final Map<Long, List<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();

    /**
     * 提交成绩（V2 纯原始字段合约）
     *
     * 客户端只传原始字段，所有派生指标（speed, keyStroke, codeLength, accuracyRate, keyAccuracy）
     * 由服务端实体 getter 统一计算，确保全栈指标一致。
     *
     * @param dto 成绩数据
     */
    public void submitScore(SubmitScoreDTO dto) {
        // 只有服务端存在的文本才能提交成绩
        if (dto.getTextId() == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本ID不能为空");
        }
        Text text = textMapper.findById(dto.getTextId());
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本不存在");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        Long textId = text.getId();

        // 频率限制：同一用户两次提交间隔不得小于 5 秒
        Long lastSubmitTime = scoreMapper.findLastSubmitTime(userId);
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (lastSubmitTime != null && (nowSeconds - lastSubmitTime) < MIN_SUBMIT_INTERVAL_SECONDS) {
            throw new BusinessException(ResultCode.SCORE_SUBMIT_TOO_FREQUENT);
        }

        Score score = Score.builder()
            .userId(userId)
            .textId(textId)
            .charCount(dto.getCharCount())
            .wrongCharCount(dto.getWrongCharCount())
            .backspaceCount(dto.getBackspaceCount())
            .correctionCount(dto.getCorrectionCount())
            .keyStrokeCount(dto.getKeyStrokeCount())
            .time(dto.getTime())
            .build();

        scoreMapper.insert(score);
        log.info("用户 {} 提交成绩成功，文本ID: {}, 字符数: {}", userId, textId, dto.getCharCount());

        // 更新 Redis 排行榜（Sorted Set）
        updateLeaderboardCache(textId, userId, score);
    }

    /**
     * 查询用户历史成绩
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    public PageResult<ScoreVO> getUserHistory(Long userId, long page, long size) {
        long offset = (page - 1) * size;
        List<Score> scores = scoreMapper.findByUserId(userId, offset, size);
        long total = scoreMapper.countByUserId(userId);

        List<ScoreVO> records = scores.stream()
            .map(this::toScoreVO)
            .toList();

        return PageResult.of(records, total, page, size);
    }

    /**
     * 查询当前用户在指定文本的历史成绩
     *
     * @param textId 文本ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    public PageResult<ScoreVO> getUserTextHistory(Long textId, long page, long size) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 校验文本存在
        Text text = textMapper.findById(textId);
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本不存在");
        }

        long offset = (page - 1) * size;
        List<Score> scores = scoreMapper.findByUserIdAndTextId(userId, textId, offset, size);
        long total = scoreMapper.countByUserIdAndTextId(userId, textId);

        List<ScoreVO> records = scores.stream()
            .map(s -> {
                ScoreVO vo = toScoreVO(s);
                vo.setTextTitle(text.getTitle());
                return vo;
            })
            .toList();

        return PageResult.of(records, total, page, size);
    }

    /**
     * 获取文本排行榜
     *
     * @param textId 文本ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    public PageResult<LeaderboardVO> getLeaderboard(Long textId, long page, long size) {
        // 校验文本存在
        Text text = textMapper.findById(textId);
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本不存在");
        }

        long offset = (page - 1) * size;
        List<LeaderboardVO> records = scoreMapper.findLeaderboardByTextId(textId, offset, size);
        long total = scoreMapper.countLeaderboardByTextId(textId);

        return PageResult.of(records, total, page, size);
    }

    /**
     * 获取用户在指定文本的最佳成绩
     *
     * @param textId 文本ID
     * @return 最佳成绩，不存在返回 null
     */
    public ScoreVO getBestScore(Long textId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 校验文本存在
        Text text = textMapper.findById(textId);
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本不存在");
        }

        Score score = scoreMapper.findBestScore(userId, textId);
        if (score == null) {
            return null;
        }

        ScoreVO vo = toScoreVO(score);
        vo.setTextTitle(text.getTitle());
        return vo;
    }

    /**
     * 转换为 ScoreVO
     *
     * 派生字段直接调用 Score 实体的 getter 方法，
     * 确保计算逻辑在整个服务层的单一数据源。
     */
    private ScoreVO toScoreVO(Score score) {
        return ScoreVO.builder()
            .id(score.getId())
            .textId(score.getTextId())
            .textTitle(score.getTextTitle())
            .speed(score.getSpeed())
            .keyStroke(score.getKeyStroke())
            .codeLength(score.getCodeLength())
            .charCount(score.getCharCount())
            .wrongCharCount(score.getWrongCharCount())
            .keyAccuracy(score.getKeyAccuracy())
            .backspaceCount(score.getBackspaceCount())
            .correctionCount(score.getCorrectionCount())
            .time(score.getTime())
            .effectiveSpeed(score.getEffectiveSpeed())
            .createdAt(score.getCreatedAt())
            .build();
    }

    /**
     * 更新 Redis 排行榜缓存。
     *
     * 🎓 面试知识点：Redis Sorted Set 用于排行榜
     *
     * - Key: leaderboard:text:{textId}
     * - Member: userId（字符串）
     * - Score: speed（字/分），用于排序
     *
     * ZADD 命令：如果 member 已存在，更新 score
     * 这意味着每个用户只保留最高速度，符合排行榜需求
     *
     * 💡 面试考点：
     * Q: 为什么用 Sorted Set 而不是 List？
     * A: List 不支持按 score 排序，需要每次查询时排序。
     *    Sorted Set 天然按 score 排序，插入和查询都是 O(log N)。
     */
    private void updateLeaderboardCache(Long textId, Long userId, Score score) {
        try {
            String key = LEADERBOARD_KEY_PREFIX + textId;
            String member = userId.toString();
            double speedScore = score.getSpeed().doubleValue();

            // ZADD: 如果已存在，更新 score（保留最高速度）
            redisTemplate.opsForZSet().add(key, member, speedScore);
            log.debug("更新排行榜缓存: textId={}, userId={}, speed={}", textId, userId, speedScore);

            // 广播排行榜更新给所有监听者
            broadcastLeaderboardUpdate(textId);
        } catch (Exception e) {
            // Redis 不可用时不影响主流程，降级到 MySQL
            log.warn("更新排行榜缓存失败，降级到 MySQL: {}", e.getMessage());
        }
    }

    /**
     * 注册 SSE 监听器（订阅指定文本的排行榜更新）
     *
     * 🎓 SSE 注册流程：
     * 1. 创建 SseEmitter（设置超时时间）
     * 2. 注册 completion/error 回调（清理过期连接）
     * 3. 加入 ConcurrentHashMap
     * 4. 返回 SseEmitter 给 Controller
     *
     * 💡 面试考点：SSE 连接管理
     * - 客户端断开 → 触发 completion 回调 → 从 Map 中移除
     * - 超时 → 触发 timeout → 自动关闭连接
     * - 服务器重启 → 所有连接丢失 → 客户端自动重连（SSE 内置机制）
     *
     * @param textId 文本ID
     * @return SseEmitter 实例
     */
    public SseEmitter subscribeLeaderboard(Long textId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        sseEmitters.computeIfAbsent(textId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
            .add(emitter);

        // 连接完成/超时/错误时清理
        emitter.onCompletion(() -> removeEmitter(textId, emitter));
        emitter.onTimeout(() -> removeEmitter(textId, emitter));
        emitter.onError(e -> removeEmitter(textId, emitter));

        log.debug("SSE 监听器注册: textId={}, 当前监听者数: {}", textId,
            sseEmitters.getOrDefault(textId, java.util.List.of()).size());

        return emitter;
    }

    /**
     * 移除 SSE 监听器
     */
    private void removeEmitter(Long textId, SseEmitter emitter) {
        sseEmitters.computeIfPresent(textId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }

    /**
     * 广播排行榜更新给所有监听指定文本的客户端
     *
     * 🎓 广播策略：
     * - 获取该文本的所有 SseEmitter
     * - 逐个发送事件（失败则移除）
     * - 使用 CompletableFuture 异步发送，避免阻塞
     *
     * 💡 SSE 事件格式：
     * - event: leaderboard-update
     * - data: JSON（textId + 时间戳）
     * - 客户端收到后自行调用 GET /texts/{id}/leaderboard 获取最新数据
     *
     * 💡 为什么只推通知而不推完整数据？
     * - 减少带宽（通知只有几十字节）
     * - 客户端可以按需请求（避免广播风暴）
     * - 排行榜数据可能很大（100+ 用户），不适合每次全量推送
     *
     * @param textId 文本ID
     */
    private void broadcastLeaderboardUpdate(Long textId) {
        List<SseEmitter> emitters = sseEmitters.get(textId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String eventJson = String.format(
            "{\"textId\":%d,\"timestamp\":%d}",
            textId, System.currentTimeMillis()
        );

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("leaderboard-update")
                    .data(eventJson));
            } catch (Exception e) {
                // 发送失败，移除该监听者
                log.debug("SSE 发送失败，移除监听者: textId={}, error={}", textId, e.getMessage());
                removeEmitter(textId, emitter);
            }
        });
    }
}
