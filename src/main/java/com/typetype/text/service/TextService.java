package com.typetype.text.service;

import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import com.typetype.text.dto.FetchedTextDTO;
import com.typetype.text.dto.UploadTextDTO;
import com.typetype.text.entity.Text;
import com.typetype.text.entity.TextSource;
import com.typetype.text.mapper.TextMapper;
import com.typetype.text.mapper.TextSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
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
    private final java.util.Random random = new java.util.Random();

    @Cacheable(cacheNames = "textById", key = "#id")
    public Text getTextEntityById(Long id) {
        Text text = textMapper.findById(id);
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本不存在");
        }
        return text;
    }

    @Cacheable(cacheNames = "textCatalog", key = "'activeSources'")
    public List<TextSource> getActiveSources() {
        return textSourceMapper.findAllActive();
    }

    public Text getRandomTextBySourceKey(String sourceKey) {
        TextSource source = validateSource(sourceKey);
        int count = textMapper.countBySourceId(source.getId());
        if (count == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该来源暂无文本: " + sourceKey);
        }
        int offset = random.nextInt(count);
        Text text = textMapper.findRandomBySourceIdWithOffset(source.getId(), offset);
        if (text == null) {
            // fallback 到原方法
            text = textMapper.findRandomBySourceId(source.getId());
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
            // 计算 clientTextId
            long clientTextId = TextFetchService.calculateClientTextId(sourceKey, fetched.getContent());
            result.setClientTextId(clientTextId);
            return result;
        }

        Text newText = textFetchService.saveText(source.getId(), sourceKey, fetched);
        return newText != null ? newText : text;
    }

    public List<Text> getTextSummariesBySourceKey(String sourceKey) {
        TextSource source = validateSource(sourceKey);
        return textMapper.findBySourceIdSummary(source.getId());
    }

    public Text getByClientTextId(Long clientTextId) {
        Text text = textMapper.findByClientTextId(clientTextId);
        if (text == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文本不存在");
        }
        return text;
    }

    @CacheEvict(cacheNames = "textCatalog", allEntries = true)
    public Text uploadText(UploadTextDTO dto) {
        // 根据 sourceKey 确定来源
        TextSource source;
        String sourceKey = dto.getSourceKey();
        boolean hasValidSpecifiedSource = sourceKey != null && !sourceKey.isBlank() && !"custom".equals(sourceKey);

        if (hasValidSpecifiedSource) {
            // 使用客户端指定的来源
            source = textSourceMapper.findBySourceKey(sourceKey);
            if (source == null || !Boolean.TRUE.equals(source.getIsActive())) {
                // 指定来源不存在或已禁用，回退到 custom
                log.warn("指定来源不存在或已禁用: sourceKey={}, 回退到 custom", sourceKey);
                source = getOrCreateCustomSource();
                sourceKey = "custom";
            }
        } else {
            // 未指定有效来源，使用 custom
            source = getOrCreateCustomSource();
            sourceKey = "custom";
        }

        // 服务端统一计算 clientTextId（算法与客户端一致）
        long clientTextId = TextFetchService.calculateClientTextId(sourceKey, dto.getContent());

        // 按 clientTextId 去重
        Text existing = textMapper.findByClientTextId(clientTextId);
        if (existing != null) {
            log.info("文本已存在: clientTextId={}, id={}", clientTextId, existing.getId());
            return existing;
        }

        Text text = new Text();
        text.setSourceId(source.getId());
        text.setTitle(dto.getTitle() != null ? dto.getTitle() : "自定义文本");
        text.setContent(dto.getContent());
        text.setCharCount(dto.getContent() != null ? dto.getContent().length() : 0);
        text.setDifficulty(0);
        text.setClientTextId(clientTextId);
        textMapper.insert(text);

        log.info("上传文本成功: id={}, clientTextId={}, sourceKey={}", text.getId(), clientTextId, sourceKey);
        return text;
    }

    /**
     * 查找或创建文本来源。
     * 未知来源回退到 custom。
     */
    private TextSource getOrCreateSource(String sourceKey) {
        if ("custom".equals(sourceKey)) {
            return getOrCreateCustomSource();
        }
        TextSource source = textSourceMapper.findBySourceKey(sourceKey);
        if (source != null && Boolean.TRUE.equals(source.getIsActive())) {
            return source;
        }
        // 未知来源回退到 custom
        return getOrCreateCustomSource();
    }

    /**
     * 获取或创建 custom 来源
     */
    private TextSource getOrCreateCustomSource() {
        TextSource source = textSourceMapper.findCustomSource();
        if (source == null) {
            try {
                source = new TextSource();
                source.setSourceKey("custom");
                source.setLabel("自定义文本");
                source.setCategory("custom");
                source.setIsActive(true);
                textSourceMapper.insert(source);
            } catch (DuplicateKeyException e) {
                // 并发请求导致重复插入，重新查询
                source = textSourceMapper.findCustomSource();
            }
        }
        return source;
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
