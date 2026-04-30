package com.typetype.text.task;

import com.typetype.text.dto.FetchedTextDTO;
import com.typetype.text.entity.TextSource;
import com.typetype.text.mapper.TextSourceMapper;
import com.typetype.text.service.TextFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 每日定时获取"极速杯"文章
 *
 * 客户端已不再调用 GET /api/v1/texts/latest/jisubei 触发爬取，
 * 改用此定时任务在服务端自动完成，避免文章"断供"。
 *
 * 设计参考 ncat-bot 每日打卡任务的锁+标志变量模式：
 *  - 用 lastSuccessDate 记录"今日已成功"状态（等效于 today_had_done_clock）
 *  - 用 isFetching 防止并发执行（等效于 clock_task_is_locking）
 *  - 失败后等待下一个周期重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyJisubeiFetchTask {

    private static final String JISUBEI_SOURCE_KEY = "jisubei";
    private static final long FIXED_DELAY_MS = 10 * 60 * 1000; // 10 分钟
    private static final LocalTime WINDOW_START = LocalTime.of(6, 0);
    private static final LocalTime WINDOW_END = LocalTime.of(23, 59);
    private static final int MIN_CONTENT_LENGTH = 10;

    private final TextFetchService textFetchService;
    private final TextSourceMapper textSourceMapper;

    /** 防止并发执行 */
    private volatile boolean isFetching = false;

    /** 最近一次成功获取的日期，用于判断"今日是否已完成" */
    private volatile LocalDate lastSuccessDate = null;

    /**
     * 每 10 分钟执行一次，在 6:00-23:59 时间段内尝试获取今日极速杯文章。
     * 已获取成功则跳过后续周期，跨日后自动重置。
     */
    @Scheduled(fixedDelay = FIXED_DELAY_MS)
    public void fetchJisubei() {
        // 今日已成功获取，跳过
        if (lastSuccessDate != null && lastSuccessDate.equals(LocalDate.now())) {
            return;
        }

        // 时间窗口检查
        LocalTime now = LocalTime.now();
        if (now.isBefore(WINDOW_START) || now.isAfter(WINDOW_END)) {
            return;
        }

        // 防并发
        if (isFetching) {
            log.debug("上一次极速杯获取任务仍在执行中，跳过本次");
            return;
        }

        // 校验来源存在且启用
        TextSource source = textSourceMapper.findBySourceKey(JISUBEI_SOURCE_KEY);
        if (source == null || !Boolean.TRUE.equals(source.getIsActive())) {
            return;
        }

        isFetching = true;
        try {
            FetchedTextDTO fetched = textFetchService.fetchTextFromSaiWen();
            if (fetched == null || fetched.getContent() == null || fetched.getContent().isBlank()) {
                log.info("极速杯文章获取失败，将在下一个周期重试");
                return;
            }

            if (fetched.getContent().length() < MIN_CONTENT_LENGTH) {
                log.warn("极速杯文章内容过短 ({} 字符)，不保存，将在下一个周期重试",
                        fetched.getContent().length());
                return;
            }

            textFetchService.saveText(source.getId(), JISUBEI_SOURCE_KEY, fetched);
            lastSuccessDate = LocalDate.now();
            log.info("今日极速杯文章获取成功: title='{}', charCount={}",
                    fetched.getTitle(), fetched.getContent().length());
        } catch (Exception e) {
            log.error("极速杯文章获取异常，将在下一个周期重试", e);
        } finally {
            isFetching = false;
        }
    }
}
