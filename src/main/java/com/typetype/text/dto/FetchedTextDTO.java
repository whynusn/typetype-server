package com.typetype.text.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for holding fetched text result from external API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchedTextDTO {

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章内容
     */
    private String content;
}
