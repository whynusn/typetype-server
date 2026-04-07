# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build/run: `mvn spring-boot:run` or `./start.sh` (generates random JWT secret)
- Run tests: `mvn test`
- Compile only: `mvn compile`
- Package executable jar: `mvn package`
- Run single test: `mvn test -Dtest=ClassNameTest`
- Flyway migrate (auto-runs on app start): `mvn flyway:migrate`

## Architecture

This is a Spring Boot 3.2.5 backend for the TypeType typing practice application.

### Project Structure

Organized by **feature-first** package structure:
- `com.typetype.auth` - Authentication (JWT login/register)
- `com.typetype.user` - User management
- `com.typetype.text` - Text management for typing practice
- `com.typetype.score` - User score/race records
- `com.typetype.common` - Shared utilities (exception handling, results, security utils)

### Technology Stack

- **Framework**: Spring Boot 3.2.5 + Spring Security + Spring MVC
- **Authentication**: JWT (io.jsonwebtoken 0.12.6) with Bearer token authentication
- **Database**: MySQL with MyBatis 3.0.3 for ORM, Flyway for database migrations
- **Build**: Maven, Java 21
- **Other**: Lombok for code simplification

### Key Components

- `JwtAuthenticationFilter` - Validates JWT tokens from request headers
- `SecurityConfig` - Spring Security configuration defining protected/public endpoints
- `GlobalExceptionHandler` - Global exception handling returning standardized Result responses
- `Result`/`ResultCode` - Standard API response format

### Database

- Flyway manages migrations in `src/main/resources/db/migration/`
- Migrations run automatically on application startup
- MyBatis mapper XML files expected in `classpath:mapper/*.xml`
- Local MySQL database expected at `localhost:3306/typetype`

### Configuration

- `application.yml` - Main configuration
- `application-dev.yml` - Development-specific overrides
- JWT secret key read from `JWT_SECRET_KEY` environment variable
- Server runs on port 8080 by default
