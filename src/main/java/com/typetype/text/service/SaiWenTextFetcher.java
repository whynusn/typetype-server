package com.typetype.text.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typetype.text.dto.FetchedTextDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class SaiWenTextFetcher {

    private static final String KEY = "c9ec834c80f77237";
    private static final String IV = "db4d6bfde3057dca";
    private static final String DEFAULT_URL = "https://www.jsxiaoshi.com/index.php/Api/Text/getContent";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SaiWenTextFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public FetchedTextDTO fetchText() {
        return fetchText(DEFAULT_URL);
    }

    public FetchedTextDTO fetchText(String url) {
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

    private String buildEncryptedPayload() throws Exception {
        // Manually build JSON to match Python json.dumps output format exactly
        // This guarantees the same byte sequence for encryption
        long timestamp = System.currentTimeMillis() / 1000;
        String json = "{" +
                "\"competitionType\": 0," +
                " \"snumflag\": \"1\"," +
                " \"from\": \"web\"," +
                " \"timestamp\": " + timestamp + "," +
                " \"version\": \"v2.1.5\"," +
                " \"subversions\": 17108" +
                "}";
        return encrypt(json);
    }

    private String encrypt(String plainText) throws Exception {
        byte[] keyBytes = KEY.getBytes(StandardCharsets.ISO_8859_1);
        byte[] ivBytes = IV.getBytes(StandardCharsets.ISO_8859_1);
        byte[] dataBytes = plainText.getBytes(StandardCharsets.ISO_8859_1);

        byte[] padded = zeroPadding(dataBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(padded);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private byte[] zeroPadding(byte[] data) {
        int blockSize = 16;
        int paddingLen = blockSize - (data.length % blockSize);
        if (paddingLen == blockSize) {
            paddingLen = 0;
        }

        byte[] padded = new byte[data.length + paddingLen];
        System.arraycopy(data, 0, padded, 0, data.length);
        return padded;
    }

    @SuppressWarnings("unchecked")
    private FetchedTextDTO extractText(String responseBody) {
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

                return new FetchedTextDTO(title, content);
            }

            log.warn("Unexpected response format from SaiWen API");
            return null;
        } catch (Exception e) {
            log.error("Failed to extract text from response", e);
            return null;
        }
    }
}
