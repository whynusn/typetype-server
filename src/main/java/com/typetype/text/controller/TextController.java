package com.typetype.text.controller;

import com.typetype.common.result.Result;
import com.typetype.text.dto.UploadTextDTO;
import com.typetype.text.entity.Text;
import com.typetype.text.entity.TextSource;
import com.typetype.text.service.TextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/texts")
@RequiredArgsConstructor
@Tag(name = "文本模块", description = "文本管理、来源目录、随机文本")
public class TextController {

    private final TextService textService;

    @GetMapping("/catalog")
    @Operation(summary = "文本来源目录", description = "获取所有已启用的文本来源")
    public Result<List<TextSource>> getCatalog() {
        List<TextSource> sources = textService.getActiveSources();
        return Result.success(sources);
    }

    @GetMapping("/source/{sourceKey}")
    @Operation(summary = "随机文本", description = "获取指定来源的随机文本（offset 策略）")
    public Result<Text> getRandomTextBySourceKey(@PathVariable String sourceKey) {
        Text text = textService.getRandomTextBySourceKey(sourceKey);
        return Result.success(text);
    }

    @GetMapping("/latest/{sourceKey}")
    @Operation(summary = "最新文本", description = "获取指定来源的最新文本")
    public Result<Text> getLatestTextBySourceKey(@PathVariable String sourceKey) {
        Text text = textService.getLatestTextBySourceKey(sourceKey);
        return Result.success(text);
    }

    @GetMapping("/{id}")
    @Operation(summary = "文本详情", description = "通过服务端 ID 获取文本")
    public Result<Text> getTextById(@PathVariable Long id) {
        Text data = textService.getTextEntityById(id);
        return Result.success(data);
    }

    @GetMapping("/by-source/{sourceKey}")
    @Operation(summary = "按来源查询", description = "获取指定来源下的所有文本")
    public Result<List<Text>> getTextsBySource(@PathVariable String sourceKey) {
        List<Text> texts = textService.getTextSummariesBySourceKey(sourceKey);
        return Result.success(texts);
    }

    @GetMapping("/by-client-text-id/{clientTextId}")
    @Operation(summary = "客户端文本查询", description = "通过客户端哈希 ID 查找文本")
    public Result<Text> getTextByClientTextId(@PathVariable Long clientTextId) {
        Text text = textService.getByClientTextId(clientTextId);
        return Result.success(text);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/upload")
    @Operation(summary = "上传文本", description = "管理员上传新文本（需要 ADMIN 权限）")
    public Result<Text> uploadText(@RequestBody UploadTextDTO dto) {
        Text text = textService.uploadText(dto);
        return Result.success(text);
    }
}
