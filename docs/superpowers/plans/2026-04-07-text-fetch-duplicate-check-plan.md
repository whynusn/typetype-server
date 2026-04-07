# 解决文章重复抓取与保存耦合问题 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 解决 `getLatestTextBySourceKey` 每次都重复保存文章的问题，并解耦抓取和保存逻辑，利用第三方API返回的title做唯一性校验。

**Architecture:** 新建 `FetchedText` DTO 承载抓取结果（title+content），修改 `SaiWenTextFetcher` 返回完整结果，在保存前通过 `sourceId + title` 查询数据库判断是否已存在，不存在才插入。

**Tech Stack:** Spring Boot 3.2.5, MyBatis, Lombok

---

### Task 1: 创建 FetchedText DTO

**Files:**
- Create: `src/main/java/com/typetype/text/dto/FetchedText.java`

- [ ] **Step 1: Create the DTO file**

```java
package com.typetype.text.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for holding fetched text result from external API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchedText {
    private String title;
    private String content;
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl . -am`
Expected: Compilation succeeds

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/typetype/text/dto/FetchedText.java
git commit -m "feat: add FetchedText DTO for title+content result"
```

---

### Task 2: 修改 SaiWenTextFetcher 返回 FetchedText

**Files:**
- Modify: `src/main/java/com/typetype/text/service/SaiWenTextFetcher.java`

- [ ] **Step 1: Update imports and method signature**

Change return type from `String` to `FetchedText`:

```java
package com.typetype.text.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typetype.text.dto.FetchedText;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// ... (keep existing imports)

@Slf4j
@Service
public class SaiWenTextFetcher {

    // ... (keep existing constants and fields)

    public FetchedText fetchText() {
        return fetchText(DEFAULT_URL);
    }

    public FetchedText fetchText(String url) {
        try {
            String encryptedPayload = buildEncryptedPayload();
            Map<String, String> payload = Map.of("0", encryptedPayload.substring(1));

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("SaiWen API returned status: {}", response.statusCode());
                return null;
            }

            return extractText(response.body());
        } catch (Exception e) {
            log.error("Failed to fetch text from SaiWen API", e);
            return null;
        }
    }

    // ... (keep existing buildEncryptedPayload, encrypt, zeroPadding unchanged)

    @SuppressWarnings("unchecked")
    private FetchedText extractText(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            Object msg = response.get("msg");

            if (msg instanceof Map) {
                Map<String, Object> msgMap = (Map<String, Object>) msg;
                String content = null;
                String title = null;

                if (msgMap.containsKey("0")) {
                    content = String.valueOf(msgMap.get("0"));
                }
                if (msgMap.containsKey("a_name")) {
                    title = String.valueOf(msgMap.get("a_name"));
                }
                // Fallback: if a_name not available, use "SaiWen - fetched" as title
                if (title == null || title.isBlank()) {
                    title = "极速杯 - 自动抓取";
                }

                return new FetchedText(title, content);
            }

            log.warn("Unexpected response format from SaiWen API");
            return null;
        } catch (Exception e) {
            log.error("Failed to extract text from response", e);
            return null;
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl . -am`
Expected: Compilation succeeds

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/typetype/text/service/SaiWenTextFetcher.java
git commit -m "refactor: SaiWenTextFetcher returns FetchedText with title"
```

---

### Task 3: 在 TextMapper 新增 findBySourceIdAndTitle 方法

**Files:**
- Modify: `src/main/java/com/typetype/text/mapper/TextMapper.java`

- [ ] **Step 1: Add the new query method**

Add after existing methods:

```java
@Mapper
public interface TextMapper {

    // ... (keep existing methods unchanged)

    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} AND title = #{title} LIMIT 1")
    Text findBySourceIdAndTitle(Long sourceId, String title);
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl . -am`
Expected: Compilation succeeds

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/typetype/text/mapper/TextMapper.java
git commit -m "feat: add findBySourceIdAndTitle to TextMapper"
```

---

### Task 4: 重构 TextFetchService 适配新接口并增加去重检查

**Files:**
- Modify: `src/main/java/com/typetype/text/service/TextFetchService.java`

- [ ] **Step 1: Update method signatures and add duplicate check**

Full updated code:

```java
package com.typetype.text.service;

import com.typetype.text.dto.FetchedText;
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

    public FetchedText fetchTextFromSaiWen() {
        FetchedText fetched = saiWenTextFetcher.fetchText();
        if (fetched == null || fetched.getContent() == null || fetched.getContent().isBlank()) {
            log.warn("Failed to fetch text from SaiWen API");
            return null;
        }
        return fetched;
    }

    public Text saveText(Long sourceId, FetchedText fetched) {
        // Check if already exists by sourceId + title
        Text existing = textMapper.findBySourceIdAndTitle(sourceId, fetched.getTitle());
        if (existing != null) {
            log.info("Text already exists with title '{}', returning existing id={}",
                    fetched.getTitle(), existing.getId());
            return existing;
        }

        Text text = new Text();
        text.setSourceId(sourceId);
        text.setTitle(fetched.getTitle());
        text.setContent(fetched.getContent());
        text.setCharCount(fetched.getContent().length());
        text.setDifficulty(0);

        textMapper.insert(text);
        log.info("Saved new text from SaiWen: id={}, title='{}', charCount={}",
                text.getId(), fetched.getTitle(), fetched.getContent().length());
        return text;
    }

    public Text fetchAndSave(Long sourceId) {
        FetchedText fetched = fetchTextFromSaiWen();
        if (fetched == null) {
            return null;
        }
        return saveText(sourceId, fetched);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl . -am`
Expected: Compilation succeeds

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/typetype/text/service/TextFetchService.java
git commit -m "refactor: TextFetchService adds duplicate check by title"
```

---

### Task 5: 更新 TextService.getLatestTextBySourceKey 适配新接口

**Files:**
- Modify: `src/main/java/com/typetype/text/service/TextService.java`

- [ ] **Step 1: Update method implementation**

Update the `getLatestTextBySourceKey` method:

```java
public Text getLatestTextBySourceKey(String sourceKey) {
    TextSource source = validateSource(sourceKey);
    Text text = textMapper.findLatestBySourceId(source.getId());

    if (text != null && isToday(text.getCreatedAt())) {
        return text;
    }

    FetchedText fetched = textFetchService.fetchTextFromSaiWen();
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
```

- [ ] **Step 2: Add missing import**

Add import at the top:

```java
import com.typetype.text.dto.FetchedText;
```

- [ ] **Step 3: Verify compilation**

Run: `mvn compile -pl . -am`
Expected: Compilation succeeds

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/typetype/text/service/TextService.java
git commit -m "refactor: TextService adapts to FetchedText with title duplicate check"
```

---

## Summary

All tasks complete the implementation:
1. ✅ New `FetchedText` DTO holds both title and content
2. ✅ `SaiWenTextFetcher` extracts title from `a_name` field
3. ✅ `TextMapper` adds query by `sourceId + title`
4. ✅ `TextFetchService` checks before saving - only insert if not exists
5. ✅ `TextService` updates to use new interface

**End result:** Same title articles won't be inserted multiple times, fetching and saving are decoupled.
