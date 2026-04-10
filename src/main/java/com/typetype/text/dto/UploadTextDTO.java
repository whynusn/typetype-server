package com.typetype.text.dto;

import lombok.Data;

@Data
public class UploadTextDTO {
    private Long clientTextId;
    private String title;
    private String content;
}
