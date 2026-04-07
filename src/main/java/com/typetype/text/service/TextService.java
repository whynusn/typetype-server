package com.typetype.text.service;

import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import com.typetype.text.dto.FetchedTextDTO;
import com.typetype.text.entity.Text;
import com.typetype.text.entity.TextSource;
import com.typetype.text.mapper.TextMapper;
import com.typetype.text.mapper.TextSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextService {

    private static final int MIN_FETCH_LENGTH = 10;

    private final TextMapper textMapper;
    private final TextSourceMapper textSourceMapper;
    private final TextFetchService textFetchService;

    public Text getTextEntityById(Long id) {
        Text text = textMapper.findById(id);
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本不存在");
        }
        return text;
    }

    public List<TextSource> getActiveSources() {
        return textSourceMapper.findAllActive();
    }

    public Text getRandomTextBySourceKey(String sourceKey) {
        TextSource source = validateSource(sourceKey);
        Text text = textMapper.findRandomBySourceId(source.getId());
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该来源暂无文本: " + sourceKey);
        }
        return text;
    }

    public Text getLatestTextBySourceKey(String sourceKey) {
        TextSource source = validateSource(sourceKey);
        Text text = textMapper.findLatestBySourceId(source.getId());

        if (text != null && isToday(text.getCreatedAt())) {
            return text;
        }

        FetchedTextDTO fetched = textFetchService.fetchTextFromSaiWen();
        if (fetched == null || fetched.getContent() == null || fetched.getContent().isBlank()) {
            if (text != null) {
                return text;
            }
            throw new BusinessException(ResultCode.NOT_FOUND, "抓取文本失败: " + sourceKey);
        }

        if (fetched.getContent().length() < MIN_FETCH_LENGTH) {
            log.warn("Fetched text too short ({} chars), returning without insert", fetched.getContent().length());
            Text result = new Text();
            result.setSourceId(source.getId());
            result.setTitle(fetched.getTitle() != null ? fetched.getTitle() : "极速杯 - 待入库");
            result.setContent(fetched.getContent());
            result.setCharCount(fetched.getContent().length());
            result.setDifficulty(0);
            return result;
        }

        Text newText = textFetchService.saveText(source.getId(), fetched);
        return newText != null ? newText : text;
    }

    private TextSource validateSource(String sourceKey) {
        TextSource source = textSourceMapper.findBySourceKey(sourceKey);
        if (source == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本来源不存在: " + sourceKey);
        }
        if (!Boolean.TRUE.equals(source.getIsActive())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文本来源已禁用: " + sourceKey);
        }
        return source;
    }

    private boolean isToday(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now());
    }
}
