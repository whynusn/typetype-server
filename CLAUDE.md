# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 命令

- 构建/运行: `mvn spring-boot:run` 或 `./start.sh`（自动生成 JWT 密钥）
- 运行测试: `mvn test`
- 仅编译: `mvn compile`
- 打包可执行 jar: `mvn package`
- 运行单个测试: `mvn test -Dtest=ClassNameTest`
- Flyway 迁移（应用启动时自动执行）: `mvn flyway:migrate`

## 文档

- [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md) — 接口契约（请求/响应 JSON 结构）
- [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) — 数据库 schema 与迁移历史
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — 模块结构与数据流

## 技术栈

Spring Boot 3.2.5 + Java 21 + Spring Security + MyBatis 3.0.3 + MySQL + Flyway + JWT (jjwt 0.12.6) + Bucket4j + Lombok

## 项目结构

feature-first 包结构：`auth`（认证）、`user`（用户）、`text`（文本）、`score`（成绩）、`common`（公共基础设施）
