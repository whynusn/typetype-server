package com.typetype.score.service;

import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import com.typetype.common.util.SecurityUtils;
import com.typetype.score.dto.SubmitScoreDTO;
import com.typetype.score.entity.Score;
import com.typetype.score.mapper.ScoreMapper;
import com.typetype.score.service.ScoreService;
import com.typetype.text.entity.Text;
import com.typetype.text.mapper.TextMapper;
import com.typetype.text.service.TextService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ScoreService 测试，验证 V2 纯原始字段合约。
 */
@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreMapper scoreMapper;

    @Mock
    private TextMapper textMapper;

    @Mock
    private TextService textService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ScoreService scoreService;

    private Text mockText;
    private MockedStatic<SecurityUtils> mockedSecurityUtils;

    @BeforeEach
    void setUp() {
        // 准备模拟文本
        mockText = Text.builder()
            .id(1L)
            .title("Test Text")
            .content("Test content")
            .build();

        // 模拟 SecurityUtils.getCurrentUserId()
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

        // Mock RedisTemplate 行为（避免 NullPointerException）
        @SuppressWarnings("unchecked")
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityUtils.close();
    }

    /**
     * 测试 V2 纯原始字段合约。
     *
     * 客户端只传原始字段：charCount, wrongCharCount, backspaceCount,
     * correctionCount, keyStrokeCount, time。
     * 派生指标（speed, keyStroke, codeLength, keyAccuracy）
     * 由服务端实体 getter 统一计算。
     */
    @Test
    void testSubmitScore_V2PureRawContract() {
        // Given
        when(textMapper.findById(1L)).thenReturn(mockText);
        when(scoreMapper.findLastSubmitTime(any())).thenReturn(null);

        SubmitScoreDTO dto = new SubmitScoreDTO();
        dto.setTextId(1L);
        dto.setCharCount(300);
        dto.setWrongCharCount(5);
        dto.setBackspaceCount(10);
        dto.setCorrectionCount(3);
        dto.setKeyStrokeCount(750);  // 原始字段：总按键次数
        dto.setTime(new BigDecimal("120.0"));

        // When
        scoreService.submitScore(dto);

        // Then
        verify(scoreMapper, times(1)).insert(any(Score.class));
        verify(scoreMapper).insert(argThat(score ->
            // 验证原始字段存储正确
            score.getCharCount() == 300 &&
            score.getWrongCharCount() == 5 &&
            score.getBackspaceCount() == 10 &&
            score.getCorrectionCount() == 3 &&
            score.getKeyStrokeCount() == 750 &&
            score.getTime().compareTo(new BigDecimal("120.0")) == 0 &&
            // 验证派生字段计算正确：300*60/120 = 150 字/分
            score.getSpeed().compareTo(new BigDecimal("150.00")) == 0 &&
            // 验证派生字段：750/120 = 6.25 击/秒
            score.getKeyStroke().compareTo(new BigDecimal("6.25")) == 0 &&
            // 验证派生字段：750/300 = 2.5 击/字
            score.getCodeLength().compareTo(new BigDecimal("2.500")) == 0
        ));
    }

    /**
     * 测试派生字段计算：effectiveSpeed = correctChars * 60 / time
     */
    @Test
    void testSubmitScore_DeriveEffectiveSpeed() {
        // Given
        when(textMapper.findById(1L)).thenReturn(mockText);
        when(scoreMapper.findLastSubmitTime(any())).thenReturn(null);

        SubmitScoreDTO dto = new SubmitScoreDTO();
        dto.setTextId(1L);
        dto.setCharCount(300);
        dto.setWrongCharCount(5);
        dto.setBackspaceCount(10);
        dto.setCorrectionCount(3);
        dto.setKeyStrokeCount(750);
        dto.setTime(new BigDecimal("120.0"));

        // When
        scoreService.submitScore(dto);

        // Then
        verify(scoreMapper).insert(argThat(score ->
            // (300-5)*60/120 = 147.5
            score.getEffectiveSpeed().compareTo(new BigDecimal("147.50")) == 0
        ));
    }

    /**
     * 测试派生字段计算：keyAccuracy 复杂计算。
     *
     * keyAccuracy = (keyStrokeCount - wrongKeys) / keyStrokeCount * 100
     * wrongKeys = backspaceCount + correctionCount * codeLength
     */
    @Test
    void testSubmitScore_DeriveKeyAccuracy() {
        // Given
        when(textMapper.findById(1L)).thenReturn(mockText);
        when(scoreMapper.findLastSubmitTime(any())).thenReturn(null);

        SubmitScoreDTO dto = new SubmitScoreDTO();
        dto.setTextId(1L);
        dto.setCharCount(300);
        dto.setWrongCharCount(0);
        dto.setBackspaceCount(0);
        dto.setCorrectionCount(0);
        dto.setKeyStrokeCount(750);  // codeLength = 750/300 = 2.5
        dto.setTime(new BigDecimal("120.0"));

        // When
        scoreService.submitScore(dto);

        // Then
        verify(scoreMapper).insert(argThat(score ->
            // 无退格无回改，键准 100%
            score.getKeyAccuracy().compareTo(new BigDecimal("100.0")) == 0
        ));
    }

    /**
     * 测试提交频率限制。
     */
    @Test
    void testSubmitScore_TooFrequent() {
        // Given
        when(textMapper.findById(1L)).thenReturn(mockText);
        // 模拟最近一次提交时间在 2 秒前
        when(scoreMapper.findLastSubmitTime(any())).thenReturn(
            System.currentTimeMillis() / 1000 - 2L
        );

        SubmitScoreDTO dto = new SubmitScoreDTO();
        dto.setTextId(1L);
        dto.setCharCount(300);
        dto.setWrongCharCount(5);
        dto.setBackspaceCount(10);
        dto.setCorrectionCount(3);
        dto.setKeyStrokeCount(750);
        dto.setTime(new BigDecimal("120.0"));

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> scoreService.submitScore(dto)
        );
        assertEquals(ResultCode.SCORE_SUBMIT_TOO_FREQUENT.getCode(), exception.getCode());

        verify(scoreMapper, never()).insert(any(Score.class));
    }

    /**
     * 测试文本不存在时抛出异常。
     */
    @Test
    void testSubmitScore_TextNotFound() {
        // Given
        when(textMapper.findById(1L)).thenReturn(null);

        SubmitScoreDTO dto = new SubmitScoreDTO();
        dto.setTextId(1L);
        dto.setCharCount(300);
        dto.setWrongCharCount(5);
        dto.setBackspaceCount(10);
        dto.setCorrectionCount(3);
        dto.setKeyStrokeCount(750);
        dto.setTime(new BigDecimal("120.0"));

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> scoreService.submitScore(dto)
        );
        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());

        verify(scoreMapper, never()).insert(any(Score.class));
    }
}