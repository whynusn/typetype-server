# TypeType 后端学习指南（第二版）

> 这是一个完整的 Spring Boot 后端学习路径，从基础到进阶，覆盖后端面试核心考点。

---

## 📚 学习路径概览

```
第一阶段：项目基础（当前已完成）✅
├── 1.1 项目架构理解
├── 1.2 基础设施学习
├── 1.3 用户模块基础（注册/登录）
└── 1.4 数据库基础

第二阶段：JWT 认证模块（重点）🔥
├── 2.1 JWT 原理与实现
├── 2.2 双 Token 机制
├── 2.3 Token 刷新与轮换
└── 2.4 Spring Security 集成

第三阶段：文本管理模块
├── 3.1 文本 CRUD
├── 3.2 随机文本获取（性能优化）
└── 3.3 文本来源管理

第四阶段：成绩管理模块（核心业务）
├── 4.1 成绩提交与校验
├── 4.2 历史记录查询（分页）
├── 4.3 排行榜（Redis ZSET）
└── 4.4 个人统计概览

第五阶段：性能与优化（面试亮点）⭐
├── 5.1 Redis 缓存策略
├── 5.2 SQL 索引优化
├── 5.3 防作弊机制
└── 5.4 并发控制
```

---

## 🎯 第一阶段：项目基础（已掌握检查）

### 1.1 项目架构理解

**学习目标**：
- 理解 Spring Boot 分层架构
- 掌握依赖注入和 Spring 组件

**关键知识点**：
```
┌─────────────────────────────────────┐
│         Controller 层               │  ← @RestController, 处理 HTTP 请求
├─────────────────────────────────────┤
│         Service 层                  │  ← @Service, 业务逻辑
├─────────────────────────────────────┤
│         Mapper 层                   │  ← @Mapper, 数据访问 (MyBatis)
├─────────────────────────────────────┤
│         Entity/DTO/VO               │  ← 数据模型
└─────────────────────────────────────┘
```

**面试问题**：
- Q: 为什么需要分层架构？
  A: 职责分离、易于测试、便于维护

- Q: DTO 和 VO 有什么区别？
  A: DTO (Data Transfer Object) 接收请求；VO (View Object) 返回响应

### 1.2 基础设施学习

**关键类**：
- `Result<T>`: 统一响应格式
- `ResultCode`: 错误码枚举
- `BusinessException`: 业务异常
- `GlobalExceptionHandler`: 全局异常处理

**学习要点**：
1. **泛型的使用**：`Result<T>` 可以包装任意类型
2. **枚举的设计**：错误码分类（10xxx 系统、20xxx 认证...）
3. **异常处理**：`@RestControllerAdvice` + `@ExceptionHandler`
4. **Lombok 注解**：`@Data`、`@Builder`、`@RequiredArgsConstructor`

**练习任务**：
```
✅ 已完成：Result.java
✅ 已完成：ResultCode.java
✅ 已完成：BusinessException.java
✅ 已完成：GlobalExceptionHandler.java
```

### 1.3 用户模块基础

**关键类**：
- `User`: 用户实体
- `UserVO`: 用户视图对象（不含密码）
- `UserMapper`: MyBatis 数据访问接口
- `UserService`: 用户服务
- `AuthController`: 认证控制器
- `UserController`: 用户控制器

**学习要点**：
1. **MyBatis 注解方式**：`@Select`、`@Insert`、`@Update`
2. **密码加密**：`BCryptPasswordEncoder`
3. **参数校验**：`@Valid` + `@NotBlank`、`@Size`
4. **转换器模式**：`UserConverter`（Entity ↔ VO）

**面试问题**：
- Q: BCrypt 和 MD5 有什么区别？
  A: BCrypt 自动加盐、可调节强度、单向不可逆

- Q: `@Options(useGeneratedKeys = true)` 的作用？
  A: 插入后回填自增主键

### 1.4 数据库基础

**关键文件**：
- `V1__create_tables.sql`: Flyway 迁移脚本

**学习要点**：
1. **数据库设计**：
   - `t_user`: 用户表
   - `t_text_source`: 文本来源表
   - `t_text`: 文本表
   - `t_score`: 成绩表

2. **索引设计**：
   - `idx_username`: 用户名唯一索引
   - `idx_user_created`: 复合索引（用户+时间）
   - `idx_speed DESC`: 降序索引（排行榜）

3. **外键约束**：
   - `t_score.user_id → t_user.id`
   - `t_text.source_id → t_text_source.id`

**练习任务**：
```
✅ 已完成：数据库表创建
✅ 已完成：示例数据插入
```

---

## 🔥 第二阶段：JWT 认证模块（重点）

### 2.1 JWT 原理与实现

**学习目标**：
- 理解 JWT 的组成和工作原理
- 实现 JWT Token 的生成和验证

**JWT 结构**：
```
Header.Payload.Signature

Header: { "alg": "HS256", "typ": "JWT" }
Payload: { "sub": "123", "iat": 1516239022, "exp": 1516242622 }
Signature: HMACSHA256(base64Url(Header) + "." + base64Url(Payload), secret)
```

**需要创建的类**：
```
jwt/
├── JwtService.java          # JWT 服务（生成、验证、解析）
├── JwtProperties.java       # JWT 配置（密钥、过期时间）
├── filter/
│   └── JwtAuthenticationFilter.java  # JWT 认证过滤器
└── dto/
    └── JwtPayloadDTO.java   # JWT 载荷数据
```

**JwtService 核心方法**：
```java
public class JwtService {
    // 生成 Access Token
    public String generateAccessToken(Long userId, String username);

    // 生成 Refresh Token
    public String generateRefreshToken(Long userId, String username);

    // 验证 Token 并返回 Payload
    public JwtPayloadDTO verifyToken(String token);

    // 从 Token 中提取用户 ID
    public Long getUserIdFromToken(String token);
}
```

**依赖**：
```xml
<!-- 已在 pom.xml 中配置 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
</dependency>
```

**面试问题**：
- Q: JWT 和 Session 有什么区别？
  A: JWT 无状态、可跨域、无需服务端存储；Session 需要服务端维护

- Q: JWT 的缺点是什么？
  A: 无法主动失效、Payload 不可存储敏感信息、Token 较大

**练习任务**：
```
⬜ TODO: 创建 JwtProperties 配置类
⬜ TODO: 实现 JwtService.generateAccessToken()
⬜ TODO: 实现 JwtService.verifyToken()
⬜ TODO: 测试 JWT 生成和解析
```

### 2.2 双 Token 机制

**学习目标**：
- 理解 Access Token 和 Refresh Token 的作用
- 实现双 Token 生成和登录接口

**双 Token 设计**：
```
Access Token:
  - 有效期：15 分钟
  - 用途：访问受保护的 API
  - 存储：前端 localStorage / memory

Refresh Token:
  - 有效期：7 天
  - 用途：刷新 Access Token
  - 存储：前端 httpOnly Cookie（更安全）
```

**实现步骤**：
1. 修改 `UserService.login()` 方法，调用 `JwtService`
2. 更新 `TokenVO` 返回真实的 JWT
3. 添加 `POST /api/v1/auth/refresh` 接口

**AuthService 新增方法**：
```java
@Service
public class AuthService {
    public TokenVO login(LoginDTO loginDTO) {
        // 1. 验证用户名密码
        // 2. 生成 Access Token
        // 3. (可选) 生成 Refresh Token 并存入 Redis
        // 4. 返回 TokenVO
    }

    public TokenVO refreshToken(String refreshToken) {
        // 1. 验证 Refresh Token
        // 2. 检查 Redis 中是否存在
        // 3. 生成新的 Access Token
        // 4. Token Rotation：生成新的 Refresh Token
        // 5. 返回新的 TokenVO
    }
}
```

**面试问题**：
- Q: 为什么要用双 Token？
  A: Access Token 短期有效降低被盗风险；Refresh Token 长期有效提升用户体验

- Q: Token Rotation 是什么？
  A: 刷新 Token 时生成新的 Refresh Token，旧 Token 立即失效，防止重放攻击

**练习任务**：
```
⬜ TODO: 创建 AuthService
⬜ TODO: 修改 UserService.login() 使用 JwtService
⬜ TODO: 实现 AuthService.refreshToken()
⬜ TODO: 添加 POST /api/v1/auth/refresh 接口
```

### 2.3 Token 刷新与轮换

**学习目标**：
- 实现 Token 刷新接口
- 理解 Token Rotation 安全机制

**Token Rotation 流程**：
```
1. 客户端提交 Refresh Token
2. 服务端验证 Token 有效性
3. 服务端检查 Redis 中是否存在该 Token
4. 生成新的 Access Token
5. 生成新的 Refresh Token
6. 删除旧的 Refresh Token（Redis）
7. 保存新的 Refresh Token（Redis）
8. 返回新的 Token 对
```

**Redis 存储结构**：
```
Key: refresh_token:{userId}
Value: {refreshToken}
TTL: 7 天
```

**AuthController 新增接口**：
```java
@PostMapping("/refresh")
public Result<TokenVO> refreshToken(
    @RequestHeader("Authorization") String authHeader
) {
    // 提取 Refresh Token
    // 调用 AuthService.refreshToken()
    // 返回新的 TokenVO
}
```

**面试问题**：
- Q: Refresh Token 应该存在哪里？
  A: 推荐 httpOnly Cookie，避免 XSS 攻击；Redis 用于服务端验证和撤销

- Q: 如何实现 Token 撤销？
  A: 将 Refresh Token 存入 Redis，撤销时删除对应 Key

**练习任务**：
```
⬜ TODO: 添加 Redis 依赖到 pom.xml
⬜ TODO: 配置 Redis 连接
⬜ TODO: 实现 Refresh Token 存储
⬜ TODO: 实现刷新接口
```

### 2.4 Spring Security 集成

**学习目标**：
- 配置 Spring Security 白名单
- 实现 JWT 认证过滤器
- 理解 Security 过滤链

**Security 过滤链**：
```
Client Request
  ↓
JwtAuthenticationFilter (提取 JWT，验证，设置 SecurityContext)
  ↓
SecurityContextHolder (获取当前用户)
  ↓
Controller
```

**JwtAuthenticationFilter 实现**：
```java
@Component
public class JwtAuthenticationFilter
    extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        // 1. 从 Header 提取 Token
        // 2. 验证 Token
        // 3. 解析用户信息
        // 4. 创建 UsernamePasswordAuthenticationToken
        // 5. 设置到 SecurityContext
        // 6. 继续过滤链
    }
}
```

**修改 SecurityConfig**：
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .csrf(csrf -> csrf.disable())
        .addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .anyRequest().authenticated()
        );
    return http.build();
}
```

**获取当前用户**：
```java
@GetMapping("/me")
public Result<UserVO> getCurrentUser() {
    // 方式 1: 从 SecurityContext 获取
    Authentication auth = SecurityContextHolder.getContext()
        .getAuthentication();
    Long userId = (Long) auth.getPrincipal();

    // 方式 2: 使用注解（需要自定义 UserDetailsService）
    // @AuthenticationPrincipal Long userId

    UserVO userVO = userService.getUserById(userId);
    return Result.success(userVO);
}
```

**面试问题**：
- Q: Spring Security 的核心组件有哪些？
  A: SecurityContext、Authentication、FilterChain、UserDetailsService

- Q: CSRF 是什么？为什么 API 要禁用？
  A: 跨站请求伪造保护；JWT 不依赖 Session Cookie，无需 CSRF

**练习任务**：
```
⬜ TODO: 实现 JwtAuthenticationFilter
⬜ TODO: 修改 SecurityConfig 启用认证
⬜ TODO: 修改 UserController.getCurrentUser() 从 Token 获取 userId
⬜ TODO: 测试认证接口
```

---

## 📖 第三阶段：文本管理模块

### 3.1 文本 CRUD

**学习目标**：
- 实现文本的增删改查
- 理解 MyBatis 动态 SQL

**需要创建的类**：
```
text/
├── TextController.java
├── TextService.java
├── TextMapper.java
├── entity/
│   ├── Text.java
│   └── TextSource.java
└── dto/
    ├── TextVO.java
    ├── TextSourceVO.java
    ├── TextQueryDTO.java
    └── TextCreateDTO.java
```

**TextMapper 关键方法**：
```java
@Mapper
public interface TextMapper {
    // 随机获取一篇文本（按来源和难度）
    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} " +
            "AND (#{difficulty} IS NULL OR difficulty = #{difficulty}) " +
            "ORDER BY RAND() LIMIT 1")
    Text findRandomText(@Param("sourceId") Long sourceId,
                      @Param("difficulty") Integer difficulty);

    // 分页查询文本
    @Select("SELECT * FROM t_text WHERE source_id = #{sourceId} " +
            "LIMIT #{offset}, #{size}")
    List<Text> findByPage(@Param("sourceId") Long sourceId,
                          @Param("offset") Long offset,
                          @Param("size") Long size);

    // 统计总数
    @Select("SELECT COUNT(*) FROM t_text WHERE source_id = #{sourceId}")
    Long countBySource(@Param("sourceId") Long sourceId);
}
```

**TextController 接口**：
```java
@RestController
@RequestMapping("/api/v1/texts")
public class TextController {

    // 随机获取文本
    @GetMapping("/random")
    public Result<TextVO> getRandomText(
        @RequestParam String sourceKey,
        @RequestParam(required = false) Integer difficulty
    );

    // 分页查询文本
    @GetMapping
    public Result<PageResult<TextVO>> getTexts(
        @RequestParam String sourceKey,
        @RequestParam(defaultValue = "1") Long page,
        @RequestParam(defaultValue = "20") Long size
    );

    // 获取文本来源列表
    @GetMapping("/sources")
    public Result<List<TextSourceVO>> getTextSources();
}
```

**面试问题**：
- Q: `ORDER BY RAND()` 有什么性能问题？
  A: 全表扫描，大数据量下性能差；优化方案：预加载 ID 池到 Redis

- Q: MyBatis 的 `#{}` 和 `${}` 有什么区别？
  A: `#{}` 预编译，防止 SQL 注入；`${}` 直接拼接，不安全

**练习任务**：
```
⬜ TODO: 创建 Text 实体和 TextSource 实体
⬜ TODO: 创建 TextMapper 接口
⬜ TODO: 实现 TextService
⬜ TODO: 创建 TextController
⬜ TODO: 测试文本查询接口
```

### 3.2 随机文本获取（性能优化）

**学习目标**：
- 使用 Redis 预加载文本 ID 池
- 理解缓存预热策略

**优化方案**：
```
1. 应用启动时，从 DB 加载所有文本 ID
2. 按来源分组存储到 Redis SET
3. 定时刷新（5 分钟）
4. 随机文本时，从 Redis SRANDMEMBER 获取 ID
5. 根据 ID 查询 DB（或缓存）
```

**Redis 数据结构**：
```
Key: text:ids:{sourceKey}  (SET 类型)
Value: {textId1, textId2, textId3, ...}
TTL: 永久（定时刷新）

示例:
text:ids:cet4 → {1, 5, 9, 13, ...}
text:ids:cet6 → {2, 6, 10, 14, ...}
```

**TextService 实现**：
```java
@Service
public class TextService {

    // 预加载 ID 池到 Redis
    @PostConstruct
    public void preloadTextIds() {
        List<TextSource> sources = textSourceMapper.findAll();
        for (TextSource source : sources) {
            List<Long> ids = textMapper.findIdsBySource(
                source.getId()
            );
            String key = "text:ids:" + source.getSourceKey();
            redisTemplate.delete(key);
            redisTemplate.opsForSet().add(
                key,
                ids.toArray(new Long[0])
            );
        }
    }

    // 随机获取文本
    public TextVO getRandomText(String sourceKey, Integer difficulty) {
        String key = "text:ids:" + sourceKey;
        Long textId = redisTemplate.opsForSet().randomMember(key);
        Text text = textMapper.findById(textId);
        return textConverter.toVO(text);
    }
}
```

**面试问题**：
- Q: 为什么用 Redis SET 存储文本 ID？
  A: SRANDMEMBER 时间复杂度 O(1)，无需全表扫描

- Q: 如何保证 Redis 中的数据与 DB 同步？
  A: 定时刷新 + DB 变更时主动更新

**练习任务**：
```
⬜ TODO: 启用 Redis 依赖（pom.xml）
⬜ TODO: 配置 Redis 连接
⬜ TODO: 实现 ID 池预加载
⬜ TODO: 实现随机文本获取
⬜ TODO: 添加定时任务刷新 ID 池
```

### 3.3 文本来源管理

**学习目标**：
- 实现文本来源的 CRUD
- 理解分类管理

**TextSourceMapper**：
```java
@Mapper
public interface TextSourceMapper {

    @Select("SELECT * FROM t_text_source")
    List<TextSource> findAll();

    @Select("SELECT * FROM t_text_source WHERE source_key = #{key}")
    TextSource findByKey(String key);

    @Select("SELECT * FROM t_text_source WHERE is_active = 1")
    List<TextSource> findActive();

    @Insert("INSERT INTO t_text_source (source_key, label, category, is_active) " +
            "VALUES (#{sourceKey}, #{label}, #{category}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TextSource textSource);

    @Update("UPDATE t_text_source SET label = #{label}, category = #{category}, " +
            "is_active = #{isActive} WHERE id = #{id}")
    void update(TextSource textSource);

    @Delete("DELETE FROM t_text_source WHERE id = #{id}")
    void delete(Long id);
}
```

**TextController 接口**：
```java
// 获取所有文本来源
@GetMapping@GetMapping("/sources")
public Result<List<TextSourceVO>> getTextSources();

// 获取启用的文本来源
@GetMapping("/sources/active")
public Result<List<TextSourceVO>> getActiveTextSources();

// 创建文本来源（管理员权限）
@PostMapping("/sources")
@PreAuthorize("hasRole('ADMIN')")
public Result<TextSourceVO> createTextSource(
    @Valid @RequestBody TextSourceCreateDTO dto
);

// 更新文本来源（管理员权限）
@PutMapping("/sources/{id}")
@PreAuthorize("hasRole('ADMIN')")
public Result<TextSourceVO> updateTextSource(
    @PathVariable Long id,
    @Valid @RequestBody TextSourceUpdateDTO dto
);

// 删除文本来源（管理员权限）
@DeleteMapping("/sources/{id}")
@PreAuthorize("hasRole('ADMIN')")
public Result<Void> deleteTextSource(@PathVariable Long id);
```

**面试问题**：
- Q: `@PreAuthorize` 的作用？
  A: 方法级别的权限控制，检查用户角色

- Q: 如何实现软删除？
  A: 添加 is_deleted 字段，查询时过滤，更新状态而非物理删除

**练习任务**：
```
⬜ TODO: 创建 TextSourceMapper
⬜ TODO: 实现 TextSourceService
⬜ TODO: 添加文本来源管理接口
⬜ TODO: （可选）实现权限控制
```

---

## 📊 第四阶段：成绩管理模块（核心业务）

### 4.1 成绩提交与校验

**学习目标**：
- 实现成绩提交接口
- 实现数据校验和防作弊机制

**需要创建的类**：
```
score/
├── ScoreController.java
├── ScoreService.java
├── ScoreMapper.java
├── entity/
│   └── Score.java
└── dto/
    ├── ScoreSubmitDTO.java
    ├── ScoreVO.java
    └── ScoreHistoryVO.java
```

**ScoreSubmitDTO 校验（V2）**：
```java
@Data
public class ScoreSubmitDTO {

    @NotNull(message = "文本 ID 不能为空")
    private Long textId;

    @NotNull(message = "速度不能为空")
    @DecimalMax(value = "300", message = "速度异常")
    private BigDecimal speed;

    @NotNull(message = "字符数不能为空")
    @Min(value = 1, message = "字符数至少为 1")
    private Integer charCount;

    @NotNull(message = "键准不能为空")
    @DecimalMin(value = "0", message = "键准不能小于 0")
    @DecimalMax(value = "100", message = "键准不能大于 100")
    private BigDecimal keyAccuracy;

    @NotNull(message = "时长不能为空")
    @Min(value = 1, message = "时长至少为 1 秒")
    private BigDecimal time;

    // ... 其他字段：keyStroke, codeLength, wrongCharCount, backspaceCount, correctionCount
}
```

**ScoreService 业务逻辑（V2）**：
```java
@Service
public class ScoreService {

    public ScoreVO submitScore(ScoreSubmitDTO dto, Long userId) {
        // 1. 基本校验（已在 DTO 中完成）

        // 2. 交叉校验：speed ≈ charCount * 60 / time
        BigDecimal expectedSpeed = dto.getCharCount()
            .multiply(new BigDecimal("60"))
            .divide(dto.getTime(), 2, RoundingMode.HALF_UP);

        if (expectedSpeed.subtract(dto.getSpeed()).abs()
                .compareTo(new BigDecimal("5")) > 0) {
            throw new BusinessException(ResultCode.SCORE_DATA_INVALID,
                "速度数据与时长/字符数不匹配");
        }

        // 3. 频率限制：同一用户 5 秒内只能提交一次
        String lockKey = "score:submit:" + userId;
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (!locked) {
            throw new BusinessException(ResultCode.SCORE_SUBMIT_TOO_FREQUENT);
        }

        // 4. 保存成绩
        Score score = Score.builder()
            .userId(userId)
            .textId(dto.getTextId())
            .speed(dto.getSpeed())
            .keyStroke(dto.getKeyStroke())
            .codeLength(dto.getCodeLength())
            .charCount(dto.getCharCount())
            .wrongCharCount(dto.getWrongCharCount())
            .keyAccuracy(dto.getKeyAccuracy())
            .backspaceCount(dto.getBackspaceCount())
            .correctionCount(dto.getCorrectionCount())
            .time(dto.getTime())
            .build();
        scoreMapper.insert(score);

        // 5. 更新排行榜（Redis ZSET）
        rankingService.updateRanking(userId, dto.getSpeed());

        return scoreConverter.toVO(score);
    }
}
```

**ScoreController 接口**：
```java
@RestController
@RequestMapping("/api/v1/scores")
public class ScoreController {

    // 提交成绩（需要认证）
    @PostMapping
    public Result<ScoreVO> submitScore(
        @Valid @RequestBody ScoreSubmitDTO dto
    ) {
        Long userId = getCurrentUserId(); // 从 JWT 获取
        ScoreVO scoreVO = scoreService.submitScore(dto, userId);
        return Result.success("成绩提交成功", scoreVO);
    }
}
```

**面试问题**：
- Q: 如何防止成绩作弊？
  A: 交叉校验、频率限制、异常值检测、客户端签名

- Q: 为什么用 Redis SETNX 做频率限制？
  A: 原子操作、无需同步锁、自动过期

**练习任务**：
```
⬜ TODO: 创建 Score 实体
⬜ TODO: 创建 ScoreSubmitDTO（带校验）
⬜ TODO: 实现 ScoreMapper
⬜ TODO: 实现 ScoreService（含校验逻辑）
⬜ TODO: 创建 ScoreController
⬜ TODO: 测试成绩提交接口
```

### 4.2 历史记录查询（分页）

**学习目标**：
- 实现分页查询
- 理解 MyBatis 分页插件

**ScoreMapper 分页查询**：
```java
@Mapper
public interface ScoreMapper {

    // 查询用户的历史记录（按时间倒序）
    @Select("SELECT * FROM t_score WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{size}")
    List<Score> findByUserId(
        @Param("userId") Long userId,
        @Param("offset") Long offset,
        @Param("size") Long size
    );

    // 统计用户记录总数
    @Select("SELECT COUNT(*) FROM t_score WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);

    // 按时间范围查询
    @Select("SELECT * FROM t_score WHERE user_id = #{userId} " +
            "AND created_at >= #{startDate} " +
            "AND created_at <= #{endDate} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{size}")
    List<Score> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("offset") Long offset,
        @Param("size") Long size
    );
}
```

**ScoreService 分页逻辑**：
```java
@Service
public class ScoreService {

    public PageResult<ScoreVO> getMyHistory(
        Long userId, Long page, Long size
    ) {
        long offset = (page - 1) * size;

        List<Score> scores = scoreMapper.findByUserId(
            userId, offset, size
        );
        long total = scoreMapper.countByUserId(userId);

        List<ScoreVO> scoreVOs = scores.stream()
            .map(scoreConverter::toVO)
            .collect(Collectors.toList());

        return PageResult.of(scoreVOs, total, page, size);
    }

    public PageResult<ScoreVO> getMyHistoryByDateRange(
        Long userId, LocalDateTime startDate,
        LocalDateTime endDate, Long page, Long size
    ) {
        long offset = (page - 1) * size;

        List<Score> scores = scoreMapper.findByUserIdAndDateRange(
            userId, startDate, endDate, offset, size
        );
        long total = scoreMapper.countByUserIdAndDateRange(
            userId, startDate, endDate
        );

        List<ScoreVO> scoreVOs = scores.stream()
            .map(scoreConverter::toVO)
            .collect(Collectors.toList());

        return PageResult.of(scoreVOs, total, page, size);
    }
}
```

**ScoreController 接口**：
```java
// 查询我的历史记录
@GetMapping("/me")
public Result<PageResult<ScoreVO>> getMyHistory(
    @RequestParam(defaultValue = "1") Long page,
    @RequestParam(defaultValue = "20") Long size
) {
    Long userId = getCurrentUserId();
    PageResult<ScoreVO> result = scoreService.getMyHistory(
        userId, page, size
    );
    return Result.success(result);
}

// 按日期范围查询
@GetMapping("/me/range")
public Result<PageResult<ScoreVO>> getMyHistoryByDateRange(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate startDate,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate endDate,
    @RequestParam(defaultValue = "1") Long page,
    @RequestParam(defaultValue = "20") Long size
) {
    Long userId = getCurrentUserId();
    PageResult<ScoreVO> result = scoreService.getMyHistoryByDateRange(
        userId,
        startDate.atStartOfDay(),
        endDate.atTime(23, 59, 59),
        page, size
    );
    return Result.success(result);
}
```

**面试问题**：
- Q: `PageResult.of()` 方法做了什么？
  A: 封装分页数据，自动计算总页数

- Q: 如何优化分页查询的 OFFSET 问题？
  A: 使用游标分页（WHERE id > lastId LIMIT N）代替 OFFSET

**练习任务**：
```
⬜ TODO: 实现分页查询方法
⬜ TODO: 实现日期范围查询
⬜ TODO: 添加历史记录接口
⬜ TODO: 测试分页查询
```

### 4.3 排行榜（Redis ZSET）

**学习目标**：
- 使用 Redis ZSET 实现排行榜
- 理解 Redis 数据结构

**需要创建的类**：
```
score/
└── service/
    └── RankingService.java
```

**Redis 数据结构**：
```
Key: ranking:daily:2026-03-08  (ZSET 类型)
Key: ranking:weekly:2026-W10     (ZSET 类型)
Key: ranking:all_time            (ZSET 类型)

Value: {userId: score}

示例:
ranking:all_time → {1: 145.5, 2: 138.2, 3: 125.8, ...}
```

**RankingService 实现**：
```java
@Service
public class RankingService {

    // 更新排行榜
    public void updateRanking(Long userId, Double speed) {
        // 日榜
        String dailyKey = "ranking:daily:" +
            LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        redisTemplate.opsForZSet().add(dailyKey,
            userId.toString(), speed);
        redisTemplate.expire(dailyKey, Duration.ofDays(2));

        // 周榜
        String weeklyKey = "ranking:weekly:" + getWeekKey();
        redisTemplate.opsForZSet().add(weeklyKey,
            userId.toString(), speed);
        redisTemplate.expire(weeklyKey, Duration.ofDays(8));

        // 总榜
        String allTimeKey = "ranking:all_time";
        redisTemplate.opsForZSet().add(allTimeKey,
            userId.toString(), speed);
    }

    // 获取排行榜 Top N
    public List<RankingVO> getTopN(String type, int n) {
        String key = "ranking:" + type + ":" + getKeySuffix(type);

        // ZREVRANGE key 0 (n-1) WITHSCORES
        Set<ZSetOperations.TypedTuple<String>> tuples =
            redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, n - 1);

        List<Long> userIds = tuples.stream()
            .map(t -> Long.valueOf(t.getValue()))
            .collect(Collectors.toList());

        // 批量查询用户信息
        List<User> users = userService.batchGetUsers(userIds);

        // 组装返回
        List<RankingVO> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long userId = Long.valueOf(tuple.getValue());
            Double score = tuple.getScore();
            User user = findUserById(users, userId);

            result.add(RankingVO.builder()
                .rank(rank++)
                .userId(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .speed(score)
                .build());
        }

        return result;
    }

    // 获取用户排名
    public Long getUserRank(String type, Long userId) {
        String key = "ranking:" + type + ":" + getKeySuffix(type);

        // ZREVRANK key member (返回从 0 开始的排名)
        Long rank = redisTemplate.opsForZSet()
            .reverseRank(key, userId.toString());

        return rank != null ? rank + 1 : null; // 转换为从 1 开始
    }

    private String getWeekKey() {
        // 获取当前周 key（如 2026-W10）
        YearWeek yearWeek = YearWeek.of(
            LocalDate.now().get(ChronoField.YEAR),
            LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_YEAR)
        );
        return yearWeek.toString();
    }

    private String getKeySuffix(String type) {
        switch (type) {
            case "daily":
                return LocalDate.now().format(
                    DateTimeFormatter.ISO_DATE);
            case "weekly":
                return getWeekKey();
            case "all_time":
                return "";
            default:
                throw new IllegalArgumentException("Invalid type");
        }
    }
}
```

**ScoreController 接口**：
```java
// 查询排行榜
@GetMapping("/ranking")
public Result<List<RankingVO>> getRanking(
    @RequestParam(defaultValue = "all_time") String type,
    @RequestParam(defaultValue = "50") int n
) {
    List<RankingVO> ranking = rankingService.getTopN(type, n);
    return Result.success(ranking);
}

// 查询我的排名
@GetMapping("/my-rank")
public Result<Long> getMyRank(
    @RequestParam(defaultValue = "all_time") String type
) {
    Long userId = getCurrentUserId();
    Long rank = rankingService.getUserRank(type, userId);
    return Result.success(rank);
}
```

**RankingVO**：
```java
@Data
@Builder
public class RankingVO {
    private Long rank;       // 排名
    private Long userId;     // 用户 ID
    private String username;  // 用户名
    private String nickname;  // 昵称
    private Double speed;     // 速度
}
```

**面试问题**：
- Q: Redis ZSET 的时间复杂度是多少？
  A: ZADD O(logN)、ZRANGE O(logN+M)、ZRANK O(logN)

- Q: 为什么用 ZSET 而不是 SQL ORDER BY？
  A: 性能更好（O(logN) vs O(N logN)），支持实时更新

**练习任务**：
```
⬜ TODO: 创建 RankingService
⬜ TODO: 实现更新排行榜方法
⬜ TODO: 实现查询排行榜 Top N
⬜ TODO: 实现查询用户排名
⬜ TODO: 添加排行榜接口
⬜ TODO: 测试排行榜功能
```

### 4.4 个人统计概览

**学习目标**：
- 实现个人统计数据查询
- 理解 SQL 聚合函数

**需要创建的类**：
```
user/
├── service/
│   └── UserStatsService.java
└── dto/
    └── UserStatsVO.java
```

**UserStatsVO**：
```java
@Data
@Builder
public class UserStatsVO {
    private Long totalPractices;      // 总练习次数
    private Double avgSpeed;         // 平均速度
    private Double maxSpeed;         // 最高速度
    private Double avgAccuracy;      // 平均准确率
    private Double totalDuration;     // 总时长（秒）
    private List<TrendVO> recentTrend; // 近期趋势
}

@Data
@Builder
public class TrendVO {
    private String date;      // 日期
    private Double avgSpeed;  // 平均速度
    private Double maxSpeed;  // 最高速度
}
```

**ScoreMapper 统计方法**：
```java
// 用户统计概览
@Select("SELECT " +
        "COUNT(*) as total_practices, " +
        "AVG(speed) as avg_speed, " +
        "MAX(speed) as max_speed, " +
        "AVG(accuracy_rate) as avg_accuracy, " +
        "SUM(duration) as total_duration " +
        "FROM t_score WHERE user_id = #{userId}")
@MapKey("totalPractices")
Map<String, Object> getUserStats(@Param("userId") Long userId);

// 近期趋势（按天聚合）
@Select("SELECT " +
        "DATE(created_at) as date, " +
        "AVG(speed) as avg_speed, " +
        "MAX(speed) as max_speed " +
        "FROM t_score " +
        "WHERE user_id = #{userId} " +
        "AND created_at >= #{startDate} " +
        "GROUP BY DATE(created_at) " +
        "ORDER BY date")
List<Map<String, Object>> getUserTrend(
    @Param("userId") Long userId,
    @Param("startDate") LocalDateTime startDate
);
```

**UserStatsService 实现**：
```java
@Service
public class UserStatsService {

    public UserStatsVO getUserStats(Long userId) {
        // 1. 查询总体统计
        Map<String, Object> stats = scoreMapper.getUserStats(userId);

        // 2. 查询近期趋势（最近 30 天）
        LocalDateTime startDate = LocalDateTime.now()
            .minusDays(30);
        List<Map<String, Object>> trendData =
            scoreMapper.getUserTrend(userId, startDate);

        // 3. 组装返回
        List<TrendVO> trends = trendData.stream()
            .map(row -> TrendVO.builder()
                .date(row.getDate("date").toString())
                .avgSpeed((Double) row.get("avg_speed"))
                .maxSpeed((Double) row.get("max_speed"))
                .build())
            .collect(Collectors.toList());

        return UserStatsVO.builder()
            .totalPractices(((Number) stats.get("total_practices"))
                .longValue())
            .avgSpeed((Double) stats.get("avg_speed"))
            .maxSpeed((Double) stats.get("max_speed"))
            .avgAccuracy((Double) stats.get("avg_accuracy"))
            .totalDuration((Double) stats.get("total_duration"))
            .recentTrend(trends)
            .build();
    }
}
```

**UserController 接口**：
```java
// 获取个人统计概览
@GetMapping("/me/stats")
public Result<UserStatsVO> getMyStats() {
    Long userId = getCurrentUserId();
    UserStatsVO stats = userStatsService.getUserStats(userId);
    return Result.success(stats);
}
```

**面试问题**：
- Q: GROUP BY 的性能如何优化？
  A: 添加复合索引、减少查询范围、使用物化视图

- Q: 为什么不用 Java 做聚合？
  A: 数据库聚合更快、减少网络传输

**练习任务**：
```
⬜ TODO: 创建 UserStatsVO 和 TrendVO
⬜ TODO: 实现 ScoreMapper 统计方法
⬜ TODO: 实现 UserStatsService
⬜ TODO: 添加个人统计接口
⬜ TODO: 测试统计功能
```

---

## ⚡ 第五阶段：性能与优化（面试亮点）

### 5.1 Redis 缓存策略

**学习目标**：
- 理解常见缓存模式
- 实现缓存击穿/穿透/雪崩防护

**缓存模式**：
```
1. Cache-Aside（旁路缓存）：
   - 读：先查缓存 → 缓存 miss 再查 DB → 写入缓存
   - 写：先更新 DB → 删除缓存

2. Write-Through（直写）：
   - 写：同时更新缓存和 DB

3. Write-Behind（回写）：
   - 写：先更新缓存，异步写 DB
```

**实现示例**：
```java
@Service
public class TextService {

    // Cache-Aside 模式
    @Cacheable(value = "text", key = "#id")
    public TextVO getTextById(Long id) {
        Text text = textMapper.findById(id);
        return textConverter.toVO(text);
    }

    // 更新时删除缓存
    @CacheEvict(value = "text", key = "#id")
    public void updateText(Long id, TextUpdateDTO dto) {
        textMapper.update(id, dto);
    }
}
```

**缓存防护**：
```java
// 防止缓存穿透（查询不存在的数据）
@Cacheable(value = "text", key = "#id", unless = "#result == null")
public TextVO getTextById(Long id) {
    Text text = textMapper.findById(id);
    return text != null ? textConverter.toVO(text) : null;
}

// 防止缓存击穿（热门数据过期时大量请求）
@Cacheable(value = "text", key = "#id",
           sync = true)  // 同步加锁
public TextVO getTextById(Long id) {
    // ...
}

// 防止缓存雪崩（大量 key 同时过期）
@Cacheable(value = "text", key = "#id",
           cacheResolver = "randomTtlCacheResolver")  // 随机 TTL
public TextVO getTextById(Long id) {
    // ...
}
```

**面试问题**：
- Q: 缓存穿透、击穿、雪崩的区别？
  A:
    - 穿透：查询不存在的数据绕过缓存直接打 DB
    - 击穿：热点 key 过期瞬间大量请求打 DB
    - 雪崩：大量 key 同时过期导致 DB 压力骤增

- Q: 如何解决缓存穿透？
  A: 缓存空值、布隆过滤器

**练习任务**：
```
⬜ TODO: 启用 Spring Cache
⬜ TODO: 配置 Redis Cache
⬜ TODO: 实现文本缓存
⬜ TODO: （可选）实现缓存防护
```

### 5.2 SQL 索引优化

**学习目标**：
- 理解索引类型和选择
- 使用 EXPLAIN 分析查询

**索引类型**：
```
- B-Tree 索引（默认）：=、>、<、BETWEEN、LIKE
- 哈希索引：=、IN、<=>（不支持范围查询）
- 全文索引：全文搜索
- 空间索引：地理坐标
```

**EXPLAIN 分析**：
```sql
EXPLAIN SELECT * FROM t_score WHERE user_id = 1 ORDER BY created_at DESC LIMIT 20;

-- 输出：
-- type: ref（使用了索引）
-- key: idx_user_created（使用的索引名）
-- rows: 100（预估扫描行数）
-- Extra: Using where; Using filesort（出现了文件排序）
```

**优化案例**：
```sql
-- 优化前：全表扫描
SELECT * FROM t_score ORDER BY speed DESC LIMIT 50;

-- 优化后：使用索引
-- 已经有 idx_speed DESC

-- 优化前：覆盖索引失效
SELECT * FROM t_user WHERE username LIKE '%wang%';

-- 优化后：无法使用索引，改用全文索引或 ES
```

**面试问题**：
- Q: 索引的优缺点？
  A:
    - 优点：查询快、排序快
    - 缺点：占用空间、写入慢、维护成本

- Q: 什么情况下索引失效？
  A: 使用函数、LIKE 前缀通配符、OR 条件、类型转换

**练习任务**：
```
⬜ TODO: 使用 EXPLAIN 分析关键查询
⬜ TODO: （可选）添加缺失的索引
⬜ TODO: （可选）优化慢查询
```

### 5.3 防作弊机制

**学习目标**：
- 实现多维度数据校验
- 理解常见作弊手段

**作弊手段与防护**：
```
1. 直接修改 API 请求：
   - 防护：服务器端校验、交叉验证

2. 重放攻击：
   - 防护：请求签名、时间戳、nonce

3. 速度异常：
   - 防护：阈值检测、趋势分析

4. 频率攻击：
   - 防护：限流、黑名单
```

**实现示例**：
```java
@Service
public class ScoreService {

    public ScoreVO submitScore(ScoreSubmitDTO dto, Long userId) {
        // 1. 基本校验
        if (dto.getSpeed().compareTo(new BigDecimal("300")) > 0) {
            throw new BusinessException(ResultCode.SCORE_DATA_INVALID);
        }

        // 2. 交叉校验
        BigDecimal expectedSpeed = calculateExpectedSpeed(dto);
        if (Math.abs(expectedSpeed.subtract(dto.getSpeed())
                .doubleValue()) > 5.0) {
            throw new BusinessException(ResultCode.SCORE_DATA_INVALID);
        }

        // 3. 趋势分析（检测异常突增）
        if (detectAbnormalSpike(userId, dto.getSpeed())) {
            throw new BusinessException(ResultCode.SCORE_DATA_INVALID);
        }

        // 4. 频率限制
        checkSubmitFrequency(userId);

        // 5. 保存成绩
        Score score = saveScore(dto, userId);

        return scoreConverter.toVO(score);
    }

    private boolean detectAbnormalSpike(Long userId, BigDecimal speed) {
        // 查询用户最近 10 次成绩
        List<Score> recentScores = scoreMapper
            .findRecentByUser(userId, 10);

        if (recentScores.size() < 5) {
            return false; // 数据不足，无法判断
        }

        // 计算平均速度
        BigDecimal avgSpeed = recentScores.stream()
            .map(Score::getSpeed)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(recentScores.size()),
                2, RoundingMode.HALF_UP);

        // 当前速度超过平均速度 2 倍
        return speed.compareTo(avgSpeed.multiply(new BigDecimal("2"))) > 0;
    }
}
```

**面试问题**：
- Q: 如何防止 API 篡改？
  A: HTTPS、请求签名、服务器校验

- Q: 如何检测作弊？
  A: 异常值检测、趋势分析、机器学习

**练习任务**：
```
⬜ TODO: 实现交叉校验（已完成）
⬜ TODO: 实现趋势分析
⬜ TODO: （可选）添加作弊日志
```

### 5.4 并发控制

**学习目标**：
- 理解并发问题
- 实现分布式锁

**并发问题**：
```
1. 数据竞争：多线程修改同一数据
2. 脏读：读到未提交的数据
3. 不可重复读：同一事务中两次读取结果不同
4. 幻读：事务中范围查询结果变化
```

**分布式锁（Redis）**：
```java
@Service
public class DistributedLockService {

    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        String value = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(key, value, timeout, unit);

        return locked != null && locked;
    }

    public void unlock(String key, String value) {
        // Lua 脚本保证原子性
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                       "then return redis.call('del', KEYS[1]) " +
                       "else return 0 end";

        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(key), value);
    }
}
```

**使用示例**：
```java
@Service
public class ScoreService {

    public ScoreVO submitScore(ScoreSubmitDTO dto, Long userId) {
        String lockKey = "score:submit:" + userId;
        String lockValue = UUID.randomUUID().toString();

        try {
            // 尝试获取锁
            boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, 5, TimeUnit.SECONDS);

            if (!locked) {
                throw new BusinessException(
                    ResultCode.SCORE_SUBMIT_TOO_FREQUENT);
            }

            // 业务逻辑
            return saveScore(dto, userId);

        } finally {
            // 释放锁（使用 Lua 脚本保证原子性）
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                           "then return redis.call('del', KEYS[1]) " +
                           "else return 0 end";
            redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(lockKey), lockValue);
        }
    }
}
```

**面试问题**：
- Q: 分布式锁的实现方式？
  A: Redis SETNX、Zookeeper、数据库唯一索引

- Q: Redis 分布式锁的问题？
  A: 主从切换时锁丢失、客户端宕机锁无法释放

**练习任务**：
```
⬜ TODO: 实现分布式锁服务
⬜ TODO: 使用分布式锁保护成绩提交
⬜ TODO: （可选）实现可重入锁
```

---

## 🎓 面试核心知识点总结

### 1. Spring Boot 基础
- 自动配置原理
- 启动流程
- 常用注解（`@RestController`、`@Service`、`@Mapper`）
- 依赖注入方式（构造器注入 vs 字段注入）

### 2. Spring Security
- 认证 vs 授权
- JWT 原理
- Security 过滤链
- CSRF 防护

### 3. MyBatis
- 注解 vs XML 方式
- `#{}` vs `${}`
- 动态 SQL
- 分页插件

### 4. Redis
- 数据结构（String、Hash、List、SET、ZSET）
- 缓存模式（Cache-Aside、Write-Through）
- 缓存问题（穿透、击穿、雪崩）
- 分布式锁

### 5. 数据库
- 索引设计（B-Tree、复合索引）
- 慢查询优化（EXPLAIN）
- 事务隔离级别
- 外键约束

### 6. 并发与安全
- 线程池
- 限流（Guava RateLimiter、Redis）
- 防作弊机制
- SQL 注入防护

### 7. 设计模式
- 分层架构
- 转换器模式
- 策略模式
- 模板方法模式

---

## 📝 学习建议

1. **循序渐进**：按阶段逐步完成，不要跳过基础
2. **动手实践**：每个模块都要亲自编写代码
3. **测试驱动**：先写测试用例，再写实现
4. **思考原理**：理解"为什么这样设计"
5. **模拟面试**：每完成一个模块，对照面试问题自测

---

## 🚀 下一步行动

立即开始第二阶段：JWT 认证模块

```
第一步：创建 JwtProperties 配置类
第二步：实现 JwtService.generateAccessToken()
第三步：实现 JwtService.verifyToken()
第四步：测试 JWT 生成和解析
```

准备好了吗？让我们开始吧！
