package com.typetype.text.service;

import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import com.typetype.text.dto.FetchedTextDTO;
import com.typetype.text.dto.UploadTextDTO;
import com.typetype.text.entity.Text;
import com.typetype.text.entity.TextSource;
import com.typetype.text.mapper.TextMapper;
import com.typetype.text.mapper.TextSourceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TextService 单元测试。
 *
 * 🎓 面试知识点：边界条件覆盖
 *
 * TextService 有多个守卫条件（guard clause），每个都可能抛出异常：
 * - validateSource：来源不存在 / 来源已禁用
 * - getRandomTextBySourceKey：该来源下无文本
 * - getTextEntityById：文本不存在
 * - getByClientTextId：文本不存在
 *
 * 好的测试会系统性地覆盖每一个分支，而不是只测"正常路径"。
 * 这就是"边界条件测试" — 测试代码在边界处的行为。
 */
@ExtendWith(MockitoExtension.class)
class TextServiceTest {

    @Mock
    private TextMapper textMapper;

    @Mock
    private TextSourceMapper textSourceMapper;

    @Mock
    private TextFetchService textFetchService;

    @InjectMocks
    private TextService textService;

    private TextSource activeSource;
    private Text mockText;

    @BeforeEach
    void setUp() {
        activeSource = TextSource.builder()
            .id(1L)
            .sourceKey("cet4")
            .label("CET-4 词汇")
            .category("vocabulary")
            .isActive(true)
            .build();

        mockText = Text.builder()
            .id(10L)
            .sourceId(1L)
            .title("abandon")
            .content("to give up completely")
            .charCount(22)
            .difficulty(0)
            .clientTextId(123456789L)
            .build();
    }

    // ==================== getTextEntityById 测试 ====================

    @Test
    void testGetTextEntityById_Success() {
        // Given
        when(textMapper.findById(10L)).thenReturn(mockText);

        // When
        Text result = textService.getTextEntityById(10L);

        // Then
        assertNotNull(result);
        assertEquals("abandon", result.getTitle());
        assertEquals(10L, result.getId());
    }

    /**
     * 测试文本不存在时抛出异常。
     */
    @Test
    void testGetTextEntityById_NotFound() {
        // Given
        when(textMapper.findById(999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> textService.getTextEntityById(999L)
        );
        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== getActiveSources 测试 ====================

    @Test
    void testGetActiveSources_ReturnsList() {
        // Given
        List<TextSource> sources = List.of(activeSource);
        when(textSourceMapper.findAllActive()).thenReturn(sources);

        // When
        List<TextSource> result = textService.getActiveSources();

        // Then
        assertEquals(1, result.size());
        assertEquals("cet4", result.get(0).getSourceKey());
    }

    // ==================== getRandomTextBySourceKey 测试 ====================

    /**
     * 测试来源不存在时抛出异常。
     *
     * 🎓 面试考点：防御性编程
     * - 每个公开方法的入口处都校验前置条件
     * - 如果前置条件不满足，立即抛出有意义的异常
     * - 这就是"快速失败"（Fail Fast）原则
     */
    @Test
    void testGetRandomTextBySourceKey_SourceNotFound() {
        // Given
        when(textSourceMapper.findBySourceKey("nonexistent")).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> textService.getRandomTextBySourceKey("nonexistent")
        );
        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
    }

    /**
     * 测试来源已禁用时抛出异常。
     */
    @Test
    void testGetRandomTextBySourceKey_SourceDisabled() {
        // Given
        TextSource disabledSource = TextSource.builder()
            .id(2L)
            .sourceKey("old_source")
            .isActive(false)
            .build();
        when(textSourceMapper.findBySourceKey("old_source")).thenReturn(disabledSource);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> textService.getRandomTextBySourceKey("old_source")
        );
        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
    }

    /**
     * 测试来源下无文本时抛出异常。
     */
    @Test
    void testGetRandomTextBySourceKey_NoTexts() {
        // Given
        when(textSourceMapper.findBySourceKey("cet4")).thenReturn(activeSource);
        when(textMapper.countBySourceId(1L)).thenReturn(0);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> textService.getRandomTextBySourceKey("cet4")
        );
        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== getByClientTextId 测试 ====================

    @Test
    void testGetByClientTextId_Success() {
        // Given
        when(textMapper.findByClientTextId(123456789L)).thenReturn(mockText);

        // When
        Text result = textService.getByClientTextId(123456789L);

        // Then
        assertNotNull(result);
        assertEquals("abandon", result.getTitle());
    }

    @Test
    void testGetByClientTextId_NotFound() {
        // Given
        when(textMapper.findByClientTextId(999999999L)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> textService.getByClientTextId(999999999L)
        );
        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
    }

    // ==================== uploadText 测试 ====================

    /**
     * 测试上传新文本（无重复）。
     *
     * 🎓 面试考点：幂等性
     * - uploadText 按 clientTextId 去重
     * - 相同内容的文本不会重复插入
     * - 这是幂等性设计 — 多次调用效果相同
     */
    @Test
    void testUploadText_NewText() {
        // Given
        UploadTextDTO dto = new UploadTextDTO();
        dto.setTitle("My Text");
        dto.setContent("Hello World");
        dto.setSourceKey("custom");

        TextSource customSource = TextSource.builder()
            .id(6L)
            .sourceKey("custom")
            .isActive(true)
            .build();

        when(textSourceMapper.findCustomSource()).thenReturn(customSource);
        when(textMapper.findByClientTextId(anyLong())).thenReturn(null);
        doAnswer(invocation -> {
            Text t = invocation.getArgument(0);
            t.setId(100L);
            return null;
        }).when(textMapper).insert(any(Text.class));

        // When
        Text result = textService.uploadText(dto);

        // Then
        assertNotNull(result);
        assertEquals("My Text", result.getTitle());
        assertEquals("Hello World", result.getContent());
        verify(textMapper).insert(any(Text.class));
    }

    /**
     * 测试上传重复文本时返回已有文本。
     */
    @Test
    void testUploadText_DuplicateText() {
        // Given
        UploadTextDTO dto = new UploadTextDTO();
        dto.setContent("Hello World");

        TextSource customSource = TextSource.builder()
            .id(6L)
            .sourceKey("custom")
            .isActive(true)
            .build();

        when(textSourceMapper.findCustomSource()).thenReturn(customSource);
        when(textMapper.findByClientTextId(anyLong())).thenReturn(mockText);

        // When
        Text result = textService.uploadText(dto);

        // Then
        // 返回已有文本，不插入新记录
        assertEquals(10L, result.getId());
        assertEquals("abandon", result.getTitle());
        verify(textMapper, never()).insert(any(Text.class));
    }
}
