package com.typetype.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typetype.IntegrationTestBase;
import com.typetype.auth.dto.LoginDTO;
import com.typetype.auth.dto.RegisterDTO;
import com.typetype.auth.dto.TokenVO;
import com.typetype.common.result.Result;
import com.typetype.user.dto.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试。
 *
 * 🎓 面试知识点：集成测试的设计原则
 *
 * 1. 测试完整的请求-响应链路：
 *    HTTP Request → Controller → Service → Mapper → MySQL → Response
 *
 * 2. 使用真实数据库（Testcontainers MySQL）：
 *    - 验证 SQL 语法正确性
 *    - 验证 MyBatis 映射正确性
 *    - 验证 Flyway 迁移正确性（如果启用）
 *
 * 3. Mock 只 Mock 外部系统：
 *    - SaiWenTextFetcher 调用第三方 API → @MockBean
 *    - AuthService/UserService/Mapper 使用真实实现
 *
 * 4. 测试场景覆盖：
 *    - 正常路径（Happy Path）
 *    - 异常路径（错误输入、重复数据等）
 *    - 边界条件（空值、超长字符串等）
 *
 * 💡 MockMvc 的工作原理：
 * - 不启动真实 HTTP 服务器
 * - 直接在内存中模拟 HTTP 请求
 * - 返回 MvcResult 对象，包含状态码、响应体、响应头
 */
@DisplayName("认证模块集成测试")
class AuthControllerIT extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterDTO registerDTO;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        registerDTO = RegisterDTO.builder()
            .username("testuser_" + System.nanoTime())
            .password("password123")
            .confirmPassword("password123")
            .nickname("Test User")
            .build();

        loginDTO = LoginDTO.builder()
            .username(registerDTO.getUsername())
            .password(registerDTO.getPassword())
            .build();
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册成功 - 返回用户信息")
    void testRegister_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("注册成功"))
            .andExpect(jsonPath("$.data.username").value(registerDTO.getUsername()))
            .andExpect(jsonPath("$.data.nickname").value("Test User"))
            .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("注册失败 - 重复用户名")
    void testRegister_DuplicateUsername() throws Exception {
        // Given：先注册一次
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk());

        // When & Then：再次注册同一用户名
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(20005));  // USER_EXISTS
    }

    @Test
    @DisplayName("注册失败 - 密码不一致")
    void testRegister_PasswordMismatch() throws Exception {
        // Given
        RegisterDTO dto = RegisterDTO.builder()
            .username("testuser_" + System.nanoTime())
            .password("password123")
            .confirmPassword("differentPassword")
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(10002));  // PARAM_ERROR
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录成功 - 返回 Token 和用户信息")
    void testLogin_Success() throws Exception {
        // Given：先注册用户
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk());

        // When & Then：登录
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.data.user.username").value(registerDTO.getUsername()));
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_WrongPassword() throws Exception {
        // Given：先注册用户
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk());

        // When
        LoginDTO wrongLoginDTO = LoginDTO.builder()
            .username(registerDTO.getUsername())
            .password("wrongpassword")
            .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongLoginDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(20006));  // LOGIN_FAILED
    }

    // ==================== Token 刷新测试 ====================

    @Test
    @DisplayName("刷新 Token 成功")
    void testRefreshToken_Success() throws Exception {
        // Given：注册 + 登录，获取 refresh token
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
            .andExpect(status().isOk())
            .andReturn();

        Result<TokenVO> loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(Result.class, TokenVO.class)
        );
        String refreshToken = loginResponse.getData().getRefreshToken();

        // When & Then：使用 refresh token 刷新
        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    // ==================== 登出测试 ====================

    @Test
    @DisplayName("登出成功")
    void testLogout_Success() throws Exception {
        // Given：注册 + 登录
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
            .andExpect(status().isOk())
            .andReturn();

        Result<TokenVO> loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(),
            objectMapper.getTypeFactory().constructParametricType(Result.class, TokenVO.class)
        );
        String accessToken = loginResponse.getData().getAccessToken();

        // When & Then：登出
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("登出成功"));
    }
}
