package com.typetype.text.service;

import com.typetype.text.dto.FetchedTextDTO;
import com.typetype.text.entity.Text;
import com.typetype.text.mapper.TextMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextFetchService {

    private final SaiWenTextFetcher saiWenTextFetcher;
    private final TextMapper textMapper;

    public FetchedTextDTO fetchTextFromSaiWen() {
        FetchedTextDTO fetched = saiWenTextFetcher.fetchText();
        if (fetched == null || fetched.getContent() == null || fetched.getContent().isBlank()) {
            log.warn("Failed to fetch text from SaiWen API");
            return null;
        }
        return fetched;
    }

    public Text saveText(Long sourceId, FetchedTextDTO fetched) {
        // Check if already exists by sourceId + title
        String title = fetched.getTitle() != null ? fetched.getTitle() : "极速杯 - 自动抓取";
        Text existing = textMapper.findBySourceIdAndTitle(sourceId, title);
        if (existing != null) {
            log.info("Text already exists with title '{}', returning existing id={}",
                    title, existing.getId());
            return existing;
        }

        Text text = new Text();
        text.setSourceId(sourceId);
        text.setTitle(title);
        text.setContent(fetched.getContent());
        text.setCharCount(fetched.getContent().length());
        text.setDifficulty(0);

        textMapper.insert(text);
        log.info("Saved new text from SaiWen: id={}, title='{}', charCount={}",
                text.getId(), title, fetched.getContent().length());
        return text;
    }

    public Text fetchAndSave(Long sourceId) {
        FetchedTextDTO fetched = fetchTextFromSaiWen();
        if (fetched == null) {
            return null;
        }
        return saveText(sourceId, fetched);
    }
}
