package com.typetype.score.controller;

import com.typetype.score.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 排行榜 SSE 实时推送控制器
 *
 * 🎓 面试考点：SSE（Server-Sent Events）原理
 *
 * 1. 基于 HTTP 长连接，服务器单向推送
 * 2. Content-Type: text/event-stream
 * 3. 数据格式：
 *    event: leaderboard-update
 *    data: {"textId":1,"timestamp":1234567890}
 *
 * 4. 客户端使用 EventSource API 接收：
 *    const source = new EventSource('/api/v1/texts/1/leaderboard/stream');
 *    source.addEventListener('leaderboard-update', (e) => {
 *        const data = JSON.parse(e.data);
 *        // 重新获取排行榜数据
 *        fetchLeaderboard(data.textId);
 *    });
 *
 * 💡 SSE vs WebSocket vs Long Polling：
 *
 * | 特性        | SSE           | WebSocket     | Long Polling  |
 * |-----------|---------------|---------------|---------------|
 * | 方向       | 服务器→客户端   | 双向           | 客户端→服务器   |
 * | 协议       | HTTP          | WS            | HTTP          |
 * | 自动重连    | ✅ 内置        | ❌ 需手动实现    | ❌ 需手动实现    |
 * | 数据格式    | 文本           | 文本/二进制      | 任意           |
 * | 浏览器支持  | 所有现代浏览器    | 所有现代浏览器    | 所有浏览器       |
 * | 适用场景    | 推送通知/实时数据 | 聊天/游戏/协同编辑 | 低频更新        |
 *
 * 💡 排行榜为什么选 SSE？
 * - 只需要服务器推送（用户提交成绩后通知其他人）
 * - 不需要双向通信
 * - SSE 自动重连，比 WebSocket 简单
 * - HTTP 协议，更容易穿透代理/防火墙
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "排行榜 SSE", description = "实时排行榜推送")
public class LeaderboardSseController {

    private final ScoreService scoreService;

    /**
     * 订阅排行榜更新（SSE 端点）
     *
     * 🎓 SSE 连接流程：
     * 1. 客户端发起 GET 请求，Accept: text/event-stream
     * 2. 服务器返回 SseEmitter，保持连接不断开
     * 3. 有新成绩提交时，服务器推送 event 到所有监听者
     * 4. 客户端收到 event 后重新请求排行榜数据
     *
     * 💡 为什么不在 SSE 中直接返回排行榜数据？
     * - 排行榜数据可能很大（100+ 用户），每次推送消耗带宽
     * - 推送"有更新"的通知（几十字节），客户端按需请求（更高效）
     * - 支持分页查询（客户端可以控制每页大小）
     *
     * @param textId 文本ID
     * @return SseEmitter 实例
     */
    @GetMapping(value = "/texts/{textId}/leaderboard/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅排行榜更新", description = "SSE 端点，实时接收指定文本的排行榜更新通知")
    public SseEmitter subscribeLeaderboard(@PathVariable Long textId) {
        return scoreService.subscribeLeaderboard(textId);
    }
}
