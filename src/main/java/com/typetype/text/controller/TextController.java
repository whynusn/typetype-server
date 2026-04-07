package com.typetype.text.controller;

import com.typetype.common.result.Result;
import com.typetype.text.entity.Text;
import com.typetype.text.entity.TextSource;
import com.typetype.text.service.TextService;
import lombok.RequiredArgsConstructor;
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
}
