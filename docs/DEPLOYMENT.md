# 部署指南

## 环境变量配置

所有配置集中在 `.env` 文件中，**不要提交到 Git**。

```bash
# .env（开发环境默认值）
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=typetype
REDIS_PASSWORD=
JWT_SECRET_KEY=typetype-dev-secret-key-change-in-production
APP_PORT=8080
```

`docker-compose.yml` 通过 `${变量名}` 引用这些配置。

---

## 开发环境

### 一键启动（推荐）

```bash
docker compose up -d
```

自动读取 `.env` 文件，启动 MySQL 8.0 + Redis 7 + App。

### 本地开发（不用 Docker）

```bash
# 确保本地 MySQL 和 Redis 已启动
mvn spring-boot:run
```

### 常用命令

```bash
# 重启（代码更新后）
docker compose up -d --build

# 停止
docker compose down

# 停止并清空数据（慎用）
docker compose down -v

# 查看日志
docker compose logs -f app
```

---

## 生产部署

### 流程概览

```
1. 修改 .env（生产密码）
2. 构建镜像 → 推送仓库
3. 生产服务器：同步 .env → 拉取镜像 → 启动
```

### 1. 修改 .env（生产环境）

```bash
# 在生产服务器上修改 .env
MYSQL_ROOT_PASSWORD=your-strong-password-here
JWT_SECRET_KEY=your-random-64-char-secret-key-here
REDIS_PASSWORD=your-redis-password
APP_PORT=80
```

### 2. 构建并推送镜像（开发机）

```bash
# 打包 jar
mvn clean package -DskipTests

# 构建镜像
docker build -t typetype-server:v1.0 .

# 推送到镜像仓库（以阿里云为例）
docker login registry.cn-hangzhou.aliyuncs.com
docker tag typetype-server:v1.0 registry.cn-hangzhou.aliyuncs.com/your-namespace/typetype-server:v1.0
docker push registry.cn-hangzhou.aliyuncs.com/your-namespace/typetype-server:v1.0
```

### 3. 生产服务器部署

```bash
# 拉取镜像
docker pull registry.cn-hangzhou.aliyuncs.com/your-namespace/typetype-server:v1.0

# 启动（自动读取 .env）
docker compose up -d
```

---

## 常用镜像仓库

| 仓库 | 地址 | 适用场景 |
|------|------|---------|
| Docker Hub | hub.docker.com | 公开项目，国外访问快 |
| 阿里云 | registry.cn-hangzhou.aliyuncs.com | 国内项目，免费 |
| 腾讯云 | ccr.ccs.tencentyun.com | 国内项目，免费 |
| GitHub | ghcr.io | 和 GitHub Actions 集成 |

---

## 注意事项

- `.env` 包含敏感信息，**不要提交到 Git**（已在 .gitignore 中）
- `docker compose down` 不删数据，`docker compose down -v` 会删数据
- 更新后端版本只需重建 app 容器，MySQL 数据不受影响
- 数据卷（mysql_data）持久化存储，容器销毁后数据仍在
- 开发和生产用同一个 `docker-compose.yml`，通过 `.env` 区分配置
