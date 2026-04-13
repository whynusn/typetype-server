package com.typetype.text.service;

import com.typetype.text.dto.FetchedTextDTO;
import com.typetype.text.entity.Text;
import com.typetype.text.mapper.TextMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public Text fetchAndSave(Long sourceId, String sourceKey) {
        FetchedTextDTO fetched = fetchTextFromSaiWen();
        if (fetched == null) {
            return null;
        }
        return saveText(sourceId, sourceKey, fetched);
    }

    public Text saveText(Long sourceId, String sourceKey, FetchedTextDTO fetched) {
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

        // 计算 clientTextId，算法和前端保持一致
        long clientTextId = calculateClientTextId(sourceKey, fetched.getContent());
        text.setClientTextId(clientTextId);

        textMapper.insert(text);
        log.info("Saved new text from SaiWen: id={}, clientTextId={}, title='{}', charCount={}",
                text.getId(), clientTextId, title, fetched.getContent().length());
        return text;
    }

    /**
     * 计算 clientTextId，算法和前端保持一致
     * SHA256(sourceKey + ":" + content) → 取前8个字符 → 转成十进制 Long → 取模 10^9
     */
    public static long calculateClientTextId(String sourceKey, String content) {
        try {
            String combined = sourceKey + ":" + content;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // 转十六进制字符串，每字节保证输出两位
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            // 取前8个字符，转成十进制 Long，最后取模 10^9
            String hex8 = hexString.toString().substring(0, 8);
            return new BigInteger(hex8, 16).longValue() % 1_000_000_000L;
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 should always be available
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
