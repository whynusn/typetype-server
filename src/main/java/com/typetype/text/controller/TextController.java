package com.typetype.text.controller;

import com.typetype.common.result.Result;
import com.typetype.text.dto.UploadTextDTO;
import com.typetype.text.entity.Text;
import com.typetype.text.entity.TextSource;
import com.typetype.text.service.TextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/texts")
@RequiredArgsConstructor
public class TextController {

    private final TextService textService;

    @GetMapping("/catalog")
    public Result<List<TextSource>> getCatalog() {
        List<TextSource> sources = textService.getActiveSources();
        return Result.success(sources);
    }

    @GetMapping("/source/{sourceKey}")
    public Result<Text> getRandomTextBySourceKey(@PathVariable String sourceKey) {
        Text text = textService.getRandomTextBySourceKey(sourceKey);
        return Result.success(text);
    }

    @GetMapping("/latest/{sourceKey}")
    public Result<Text> getLatestTextBySourceKey(@PathVariable String sourceKey) {
        Text text = textService.getLatestTextBySourceKey(sourceKey);
        return Result.success(text);
    }

    @GetMapping("/{id}")
    public Result<Text> getTextById(@PathVariable Long id) {
        Text data = textService.getTextEntityById(id);
        return Result.success(data);
    }

    @GetMapping("/by-client-text-id/{clientTextId}")
    public Result<Text> getTextByClientTextId(@PathVariable Long clientTextId) {
        Text text = textService.getByClientTextId(clientTextId);
        return Result.success(text);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    public Result<Text> uploadText(@RequestBody UploadTextDTO dto) {
        Text text = textService.uploadText(dto);
        return Result.success(text);
    }
}
