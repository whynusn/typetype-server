package com.typetype.auth.service;

import com.typetype.auth.config.JwtProperties;
import com.typetype.auth.dto.JwtPayloadDTO;
import com.typetype.auth.dto.LoginDTO;
import com.typetype.auth.dto.RegisterDTO;
import com.typetype.auth.dto.TokenVO;
import com.typetype.common.exception.BusinessException;
import com.typetype.common.result.ResultCode;
import com.typetype.user.converter.UserConverter;
import com.typetype.user.dto.UserVO;
import com.typetype.user.entity.User;
import com.typetype.user.service.UserService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.Mockito.lenient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试。
 *
 * 🎓 面试知识点：Mockito 的核心概念
 *
 * 1. @Mock — 创建一个假对象，所有方法默认返回 null/0/false
 *    - 用于隔离被测类的依赖，只关注当前类的逻辑
 *
 * 2. @InjectMocks — 创建被测类的真实实例，自动注入 @Mock
 *    - Mockito 通过构造器注入（因为 AuthService 用 @RequiredArgsConstructor）
 *
 * 3. when(...).thenReturn(...) — 定义 mock 的行为
 *    - "当调用 mock 的某个方法时，返回指定值"
 *
 * 4. verify(...) — 验证 mock 的方法是否被调用
 *    - 确认代码执行了预期的交互，而不仅仅是返回值正确
 *
 * 💡 为什么不用 @SpringBootTest？
 * - @SpringBootTest 会启动整个 Spring 容器（几百毫秒到几秒）
 * - 单元测试只测一个类的逻辑，不需要容器（毫秒级）
 * - 单元测试是测试金字塔的底层，应该占测试总量的 70%+
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private UserConverter userConverter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private UserVO mockUserVO;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
            .id(1L)
            .username("testuser")
            .password("$2a$10$encodedPassword")
            .nickname("Test")
            .role("USER")
            .build();

        mockUserVO = UserVO.builder()
            .id(1L)
            .username("testuser")
            .nickname("Test")
            .build();

        // Mock RedisTemplate 行为（lenient: 部分测试不需要此 mock）
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 注册测试 ====================

    /**
     * 测试正常注册流程。
     *
     * 验证点：
     * - 密码一致性检查通过
     * - 用户名不存在
     * - 密码被加密
     * - 用户被创建
     * - 返回 UserVO
     */
    @Test
    void testRegister_Success() {
        // Given
        RegisterDTO dto = RegisterDTO.builder()
            .username("testuser")
            .password("password123")
            .confirmPassword("password123")
            .nickname("Test")
            .build();

        when(userService.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userService.createUser("testuser", "$2a$10$encodedPassword", "Test")).thenReturn(mockUser);
        when(userConverter.toVO(mockUser)).thenReturn(mockUserVO);

        // When
        UserVO result = authService.register(dto);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(passwordEncoder).encode("password123");
        verify(userService).createUser("testuser", "$2a$10$encodedPassword", "Test");
    }

    /**
     * 测试两次密码不一致时抛出异常。
     *
     * 🎓 面试考点：防御性编程
     * - 注册时强制校验两次密码，防止用户输错密码
     * - 错误码 PARAM_ERROR (10002) 表示参数校验失败
     */
    @Test
    void testRegister_PasswordMismatch() {
        // Given
        RegisterDTO dto = RegisterDTO.builder()
            .username("testuser")
            .password("password123")
            .confirmPassword("differentPassword")
            .build();

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.register(dto)
        );
        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());

        // 验证：密码不一致时，不应该调用后续方法
        verify(userService, never()).existsByUsername(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    /**
     * 测试用户名已存在时抛出异常。
     *
     * 🎓 面试考点：用户体验 vs 安全性
     * - 注册时告诉用户"用户名已存在"是合理的（用户主动注册场景）
     * - 但登录时不应该区分"用户不存在"和"密码错误"（防止账号枚举）
     * - 所以登录失败统一返回 LOGIN_FAILED (20006)
     */
    @Test
    void testRegister_UsernameExists() {
        // Given
        RegisterDTO dto = RegisterDTO.builder()
            .username("existinguser")
            .password("password123")
            .confirmPassword("password123")
            .build();

        when(userService.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.register(dto)
        );
        assertEquals(ResultCode.USER_EXISTS.getCode(), exception.getCode());

        verify(passwordEncoder, never()).encode(anyString());
    }

    // ==================== 登录测试 ====================

    /**
     * 测试正常登录流程。
     *
     * 验证点：
     * - 凭证验证通过
     * - 生成了 access token 和 refresh token
     * - 返回 TokenVO 包含用户信息
     */
    @Test
    void testLogin_Success() {
        // Given
        LoginDTO dto = LoginDTO.builder()
            .username("testuser")
            .password("password123")
            .build();

        when(userService.validateCredentials("testuser", "password123")).thenReturn(mockUser);
        when(jwtService.generateAccessToken(1L, "testuser", "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(1L, "testuser", "USER")).thenReturn("refresh-token");
        when(jwtProperties.getAccessTokenExpire()).thenReturn(900L);
        when(userConverter.toVO(mockUser)).thenReturn(mockUserVO);

        // When
        TokenVO result = authService.login(dto);

        // Then
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(900L, result.getExpiresIn());
        assertEquals("testuser", result.getUser().getUsername());
    }

    /**
     * 测试登录凭证错误时抛出异常。
     *
     * 🎓 面试考点：UserService.validateCredentials 的异常传播
     * - 当用户名不存在或密码错误时，validateCredentials 抛出 BusinessException
     * - AuthService 不需要 catch 这个异常，让它自然传播到 GlobalExceptionHandler
     * - 这就是"异常冒泡"模式 — 只在需要恢复的地方 catch
     */
    @Test
    void testLogin_InvalidCredentials() {
        // Given
        LoginDTO dto = LoginDTO.builder()
            .username("testuser")
            .password("wrongpassword")
            .build();

        when(userService.validateCredentials("testuser", "wrongpassword"))
            .thenThrow(new BusinessException(ResultCode.LOGIN_FAILED));

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.login(dto)
        );
        assertEquals(ResultCode.LOGIN_FAILED.getCode(), exception.getCode());

        // 验证：凭证错误时，不应该生成 token
        verify(jwtService, never()).generateAccessToken(anyLong(), anyString(), anyString());
    }

    // ==================== 刷新 Token 测试 ====================

    /**
     * 测试正常刷新 Token 流程。
     *
     * 🎓 面试考点：Token Rotation（令牌轮换）
     * - 刷新时不仅生成新的 access token，还生成新的 refresh token
     * - 旧的 refresh token 立即失效
     * - 防止 token 被盗用后无限期使用
     */
    @Test
    void testRefreshToken_Success() {
        // Given
        JwtPayloadDTO payload = JwtPayloadDTO.builder()
            .userId(1L)
            .username("testuser")
            .tokenType("refresh")
            .build();

        when(jwtService.verifyToken("valid-refresh-token")).thenReturn(payload);
        when(userService.getUserEntityById(1L)).thenReturn(mockUser);
        when(jwtService.generateAccessToken(1L, "testuser", "USER")).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(1L, "testuser", "USER")).thenReturn("new-refresh-token");
        when(jwtProperties.getTokenPrefix()).thenReturn("Bearer ");
        when(jwtProperties.getAccessTokenExpire()).thenReturn(900L);
        when(userConverter.toVO(mockUser)).thenReturn(mockUserVO);
        // Mock Redis: 存储的 token 与传入的一致
        when(valueOperations.get("refresh_token:1")).thenReturn("valid-refresh-token");

        // When
        TokenVO result = authService.refreshToken("Bearer valid-refresh-token");

        // Then
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
    }

    /**
     * 测试无效 Token 刷新时抛出异常。
     *
     * 🎓 面试考点：异常处理策略
     * - jwtService.verifyToken 抛出 JwtException（jjwt 库的异常）
     * - AuthService 捕获 JwtException，转为业务异常 BusinessException
     * - 这是"异常转换"模式 — 将技术异常转为业务异常，统一错误码
     */
    @Test
    void testRefreshToken_InvalidToken() {
        // Given
        when(jwtProperties.getTokenPrefix()).thenReturn("Bearer ");
        when(jwtService.verifyToken("invalid-token"))
            .thenThrow(new JwtException("Token expired"));

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.refreshToken("Bearer invalid-token")
        );
        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
    }

    /**
     * 测试使用 Access Token 刷新时抛出异常。
     *
     * 🎓 面试考点：Token 类型校验
     * - refresh 接口只接受 refresh token
     * - 如果传入 access token，必须拒绝
     * - 防止 token 类型混淆导致的安全问题
     */
    @Test
    void testRefreshToken_WrongType() {
        // Given
        JwtPayloadDTO payload = JwtPayloadDTO.builder()
            .userId(1L)
            .username("testuser")
            .tokenType("access")  // 错误类型：access 而非 refresh
            .build();

        when(jwtProperties.getTokenPrefix()).thenReturn("Bearer ");
        when(jwtService.verifyToken("access-token")).thenReturn(payload);

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.refreshToken("Bearer access-token")
        );
        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
    }

    // ==================== 登出测试 ====================

    /**
     * 测试空 Token 登出（客户端已删除本地 Token）。
     *
     * 🎓 面试考点：JWT 登出的局限性
     * - JWT 是无状态的，服务端无法主动失效
     * - 所以空 Token 也允许登出 — 客户端只需删除本地存储的 token
     * - 生产环境需要 Redis 存储 refresh token 来实现真正的撤销
     */
    @Test
    void testLogout_EmptyHeader() {
        // When & Then — 不应该抛出异常
        assertDoesNotThrow(() -> authService.logout(null));
        assertDoesNotThrow(() -> authService.logout(""));

        // 验证：空 Token 时不应该调用 jwtService
        verify(jwtService, never()).verifyToken(anyString());
    }

    /**
     * 测试正常登出流程。
     *
     * 验证点：
     * - Token 被验证
     * - 日志记录登出成功
     */
    @Test
    void testLogout_ValidToken() {
        // Given
        JwtPayloadDTO payload = JwtPayloadDTO.builder()
            .userId(1L)
            .username("testuser")
            .tokenType("access")
            .build();

        when(jwtProperties.getTokenPrefix()).thenReturn("Bearer ");
        when(jwtService.verifyToken("valid-token")).thenReturn(payload);

        // When
        authService.logout("Bearer valid-token");

        // Then
        verify(jwtService).verifyToken("valid-token");
    }
}
