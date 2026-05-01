package com.typetype.score.controller;

import com.typetype.common.result.PageResult;
import com.typetype.common.result.Result;
import com.typetype.common.util.SecurityUtils;
import com.typetype.score.dto.LeaderboardVO;
import com.typetype.score.dto.ScoreVO;
import com.typetype.score.dto.SubmitScoreDTO;
import com.typetype.score.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 成绩控制器
 *
 * 🎓 学习点：
 * - RESTful API 设计：资源名词复数形式
 * - 排行榜作为文本的子资源：/texts/{id}/leaderboard
 *
 * 💡 API 设计：
 * - POST /scores           提交成绩
 * - GET /scores/history    用户历史成绩
 * - GET /texts/{id}/leaderboard  文本排行榜
 * - GET /texts/{id}/best   用户在文本的最佳成绩
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "成绩模块", description = "成绩提交、排行榜、历史记录")
public class ScoreController {

    private final ScoreService scoreService;

    /**
     * 提交成绩
     *
     * @param dto 成绩数据
     * @return 成功响应
     */
    @PostMapping("/scores")
    @Operation(summary = "提交成绩", description = "提交打字测试成绩，服务端计算所有派生指标")
    public Result<Void> submitScore(@Valid @RequestBody SubmitScoreDTO dto) {
        scoreService.submitScore(dto);
        return Result.success("成绩提交成功");
    }

    /**
     * 当前用户历史成绩
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/scores/history")
    @Operation(summary = "用户历史成绩", description = "查询当前用户的所有成绩记录，按时间降序")
    public Result<PageResult<ScoreVO>> getUserHistory(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Long userId = SecurityUtils.getCurrentUserId();
        PageResult<ScoreVO> result = scoreService.getUserHistory(userId, page, size);
        return Result.success(result);
    }

    /**
     * 当前用户在指定文本的历史成绩
     *
     * @param textId 文本ID
     * @param page   页码
     * @param size   每页大小
     * @return 分页结果
     */
    @GetMapping("/texts/{textId}/scores")
    @Operation(summary = "用户文本历史", description = "查询当前用户在指定文本下的成绩历史")
    public Result<PageResult<ScoreVO>> getUserTextHistory(
            @PathVariable Long textId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        PageResult<ScoreVO> result = scoreService.getUserTextHistory(textId, page, size);
        return Result.success(result);
    }

    /**
     * 获取文本排行榜
     *
     * @param textId 文本ID
     * @param page   页码
     * @param size   每页大小
     * @return 排行榜分页结果
     */
    @GetMapping("/texts/{textId}/leaderboard")
    @Operation(summary = "文本排行榜", description = "查询指定文本的排行榜，每个用户仅保留最高速度")
    public Result<PageResult<LeaderboardVO>> getLeaderboard(
            @PathVariable Long textId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "50") long size) {
        PageResult<LeaderboardVO> result = scoreService.getLeaderboard(textId, page, size);
        return Result.success(result);
    }

    /**
     * 获取当前用户在指定文本的最佳成绩
     *
     * @param textId 文本ID
     * @return 最佳成绩，不存在返回 null
     */
    @GetMapping("/texts/{textId}/best")
    @Operation(summary = "最佳成绩", description = "查询当前用户在指定文本下的最佳成绩")
    public Result<ScoreVO> getBestScore(@PathVariable Long textId) {
        ScoreVO best = scoreService.getBestScore(textId);
        return Result.success(best);
    }
}
