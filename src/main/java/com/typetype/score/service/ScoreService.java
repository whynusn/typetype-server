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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 成绩服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    /** 成绩提交最短间隔（秒），防止刷榜 */
    private static final long MIN_SUBMIT_INTERVAL_SECONDS = 5;

    private final ScoreMapper scoreMapper;
    private final TextMapper textMapper;
    private final TextService textService;

    /**
     * 提交成绩
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
            .speed(dto.getSpeed())
            .effectiveSpeed(dto.getEffectiveSpeed())
            .keyStroke(dto.getKeyStroke())
            .codeLength(dto.getCodeLength())
            .accuracyRate(dto.getAccuracyRate())
            .charCount(dto.getCharCount())
            .wrongCharCount(dto.getWrongCharCount())
            .duration(dto.getDuration())
            .build();

        scoreMapper.insert(score);
        log.info("用户 {} 提交成绩成功，文本ID: {}, 速度: {}", userId, textId, dto.getSpeed());
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
     */
    private ScoreVO toScoreVO(Score score) {
        return ScoreVO.builder()
            .id(score.getId())
            .textId(score.getTextId())
            .textTitle(score.getTextTitle())
            .speed(score.getSpeed())
            .effectiveSpeed(score.getEffectiveSpeed())
            .keyStroke(score.getKeyStroke())
            .codeLength(score.getCodeLength())
            .accuracyRate(score.getAccuracyRate())
            .charCount(score.getCharCount())
            .wrongCharCount(score.getWrongCharCount())
            .duration(score.getDuration())
            .createdAt(score.getCreatedAt())
            .build();
    }
}
