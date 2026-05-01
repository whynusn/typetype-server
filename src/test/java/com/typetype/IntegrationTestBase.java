package com.typetype;

import com.typetype.text.service.SaiWenTextFetcher;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 集成测试基类。
 *
 * 🎓 面试知识点：Testcontainers 的工作原理
 *
 * 1. @Testcontainers — 启用 Testcontainers 支持
 * 2. @Container — 标记 MySQL 容器，JUnit 5 自动管理生命周期
 *    - @BeforeAll：启动 Docker 容器（首次运行需要拉取镜像）
 *    - @AfterAll：停止并删除容器
 *
 * 3. @DynamicPropertySource — 动态覆盖 Spring 配置
 *    - 容器启动后才能获取到动态端口
 *    - 通过这个注解将容器的 JDBC URL 注入到 Spring 配置中
 *
 * 4. @SpringBootTest(webEnvironment = RANDOM_PORT) — 完整启动 Spring 容器
 *    - 加载所有 Bean（Controller、Service、Mapper 等）
 *    - 启动内嵌 Tomcat，监听随机端口
 *
 * 5. @AutoConfigureMockMvc — 自动配置 MockMvc
 *    - 无需启动真实 HTTP 服务器
 *    - 直接在内存中模拟 HTTP 请求
 *
 * 6. @MockBean — 替换 Spring 容器中的 Bean
 *    - SaiWenTextFetcher 调用外部 API，测试时必须 Mock
 *    - 其他组件（AuthService、UserService、Mapper）使用真实实现
 *
 * 💡 为什么 Mock 只 Mock SaiWenTextFetcher？
 * - 集成测试的原则：只 Mock 外部系统，不 Mock 内部组件
 * - SaiWenTextFetcher 调用第三方 API（外部系统）
 * - AuthService → UserService → UserMapper → MySQL 是内部链路，应该用真实实现
 * - 这样才能验证组件间的真实交互
 */
@Disabled("需要 Docker 环境且 Docker API >= 1.40。请在配置好 Docker 的环境中运行。")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTestBase {

    /**
     * MySQL 容器。
     *
     * 🎓 面试考点：为什么用 Testcontainers 而不是 H2？
     * - 你的 ScoreMapper 使用了 MySQL 变量语法 @rank := @rank + 1
     * - H2 不支持这种语法，测试会报错
     * - Testcontainers 启动真实 MySQL，测试环境和生产环境完全一致
     * - 代价是需要 Docker 环境，测试速度稍慢（秒级）
     */
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("typetype")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("schema-test.sql");  // 初始化 schema

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        // 测试环境禁用 Flyway（使用 schema-test.sql 初始化）
        registry.add("spring.flyway.enabled", () -> "false");
    }

    /**
     * Mock 外部 API 调用。
     *
     * SaiWenTextFetcher 调用第三方赛文 API，测试时不应该真正发起网络请求。
     * @MockBean 会替换 Spring 容器中的真实 Bean。
     */
    @MockBean
    protected SaiWenTextFetcher saiWenTextFetcher;

    @Autowired
    protected MockMvc mockMvc;
}
