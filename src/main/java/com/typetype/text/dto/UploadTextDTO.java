package com.typetype.text.dto;

import lombok.Data;

@Data
public class UploadTextDTO {
    private Long textId;
    private String title;
    private String content;
}