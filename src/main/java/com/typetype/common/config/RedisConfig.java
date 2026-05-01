package com.typetype.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类。
 *
 * 🎓 面试知识点：Redis 序列化策略
 *
 * 1. 默认序列化（JdkSerializationRedisSerializer）：
 *    - 存储的是 Java 序列化后的二进制数据
 *    - 问题：Redis CLI 中看到的是乱码，难以调试
 *
 * 2. Jackson2JsonRedisSerializer（本项目使用）：
 *    - 存储的是 JSON 格式
 *    - 优点：人类可读，便于调试
 *    - 优点：跨语言兼容（其他语言也能解析）
 *
 * 3. StringRedisSerializer：
 *    - Key 使用 String 序列化
 *    - 保证 key 的可读性
 *
 * 💡 面试考点：
 * Q: 为什么不用 JDK 序列化？
 * A: JDK 序列化产出二进制数据，Redis CLI 中是乱码，难以调试和排查问题。
 *    而且 JDK 序列化有安全漏洞（反序列化攻击），生产环境不推荐。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 使用 String 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 Jackson JSON 序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jsonSerializer =
            new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
