# ==================== Stage 1: Build ====================
# 构建阶段：编译源码、打包 jar
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 先复制 pom.xml，利用 Docker 层缓存
# 只有 pom.xml 变化时才重新下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源码并编译
COPY src ./src
RUN mvn package -DskipTests -B

# ==================== Stage 2: Runtime ====================
# 运行阶段：只包含 JRE 和 jar 包
FROM eclipse-temurin:21-jre
WORKDIR /app

# 从构建阶段复制 jar
COPY --from=build /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
