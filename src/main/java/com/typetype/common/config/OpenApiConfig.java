package com.typetype.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类。
 *
 * 🎓 面试知识点：OpenAPI 3.0 规范
 *
 * OpenAPI 是 API 描述的标准格式，主要包含：
 * - info：API 基本信息（标题、描述、版本）
 * - paths：所有端点定义
 * - components：数据模型定义（Schema）
 * - security：认证方式定义
 *
 * springdoc-openapi 自动从以下来源生成 OpenAPI 文档：
 * - @Tag：Controller 类级别的分组
 * - @Operation：方法级别的接口描述
 * - @Schema：DTO 字段描述
 * - @ApiResponse：响应描述
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI typetypeOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TypeType Server API")
                .description("打字练习平台后端 API — 支持用户认证、文本管理、成绩提交与排行榜查询")
                .version("1.0.0"));
    }
}
